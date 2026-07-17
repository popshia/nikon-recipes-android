package com.noahlin.nikonpicturecontrol

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

/**
 * Holds the recipe library: the bundled `np3_list.json` plus any recipes the user creates in-app.
 * State is exposed as Compose state; user data persists in SharedPreferences.
 */
class RecipeStore(app: Application) : AndroidViewModel(app) {
    private val ctx: Context get() = getApplication()
    private val prefs = app.getSharedPreferences("recipes", Context.MODE_PRIVATE)

    private val bundled: List<Recipe> = loadBundled()
    private var custom: List<Recipe> = loadJsonList("custom")
    private var overrides: Map<String, Recipe> = loadJsonMap("overrides")

    var recipes by mutableStateOf(merged()); private set

    var favorites by mutableStateOf(prefs.getStringSet("favorites", emptySet())!!.toSet()); private set
    var userNotes by mutableStateOf(loadJsonMapString("userNotes")); private set
    var extraCategories by mutableStateOf(prefs.getStringSet("extraCategories", emptySet())!!.toSet()); private set
    var extraTags by mutableStateOf(prefs.getStringSet("extraTags", emptySet())!!.toSet()); private set

    var recentlySearchedIds by mutableStateOf(loadJsonStringList("recentlySearched")); private set

    var dynamicColorEnabled by mutableStateOf(prefs.getBoolean("dynamicColor", false)); private set

    fun setDynamicColor(enabled: Boolean) {
        dynamicColorEnabled = enabled
        prefs.edit().putBoolean("dynamicColor", enabled).apply()
    }

    private fun merged(): List<Recipe> =
        bundled.map { overrides[it.id] ?: it } + custom

    // MARK: Favorites

    fun isFavorite(id: String) = id in favorites

    fun toggleFavorite(id: String) {
        favorites = if (id in favorites) favorites - id else favorites + id
        prefs.edit().putStringSet("favorites", favorites).apply()
    }

    // MARK: Notes

    /** Effective note: the user's note if set, otherwise the bundled note. */
    fun note(recipe: Recipe): String = userNotes[recipe.id] ?: recipe.note ?: ""

    fun setNote(text: String, id: String) {
        val trimmed = text.nilIfBlank()
        userNotes = if (trimmed != null) userNotes + (id to trimmed) else userNotes - id
        prefs.edit().putString("userNotes", json.encodeToString(userNotes)).apply()
    }

    // MARK: Extra categories/tags

    fun addCategory(name: String) {
        val t = name.nilIfBlank() ?: return
        extraCategories = extraCategories + t
        prefs.edit().putStringSet("extraCategories", extraCategories).apply()
    }

    fun addTag(name: String) {
        val t = name.nilIfBlank() ?: return
        extraTags = extraTags + t
        prefs.edit().putStringSet("extraTags", extraTags).apply()
    }

    // MARK: Custom recipes + edits

    fun addCustom(recipe: Recipe) {
        custom = custom + recipe
        saveCustom()
        recipes = merged()
    }

    /** Save an edit matched by id: a custom recipe is replaced in place; a bundled one becomes an override. */
    fun update(recipe: Recipe) {
        val i = custom.indexOfFirst { it.id == recipe.id }
        if (i >= 0) {
            custom = custom.toMutableList().also { it[i] = recipe }
            saveCustom()
        } else {
            overrides = overrides + (recipe.id to recipe)
            saveOverrides()
        }
        recipes = merged()
    }

    fun recipe(id: String): Recipe? = recipes.firstOrNull { it.id == id }

    // MARK: Recently searched

    /** IDs of recipes recently opened from Search, resolved to live recipes, newest first. */
    val recentlySearched: List<Recipe>
        get() = recentlySearchedIds.mapNotNull { recipe(it) }

    fun addRecentlySearched(id: String) {
        recentlySearchedIds = (listOf(id) + recentlySearchedIds.filter { it != id }).take(10)
        prefs.edit().putString("recentlySearched", json.encodeToString(recentlySearchedIds)).apply()
    }

    fun clearRecentlySearched() {
        recentlySearchedIds = emptyList()
        prefs.edit().remove("recentlySearched").apply()
    }

    /** Rename a category across every recipe that has it, or clear it if [name] is blank. */
    fun renameCategory(old: String, name: String) {
        val new = name.nilIfBlank()
        recipes.filter { it.category == old }.forEach { update(it.copy(category = new)) }
        extraCategories = extraCategories - old + listOfNotNull(new)
        prefs.edit().putStringSet("extraCategories", extraCategories).apply()
    }

    /** Rename a tag across every recipe that has it, or remove it if [name] is blank. */
    fun renameTag(old: String, name: String) {
        val new = name.nilIfBlank()
        recipes.filter { old in it.tags }.forEach { r ->
            val tags = if (new != null) r.tags.map { if (it == old) new else it } else r.tags.filter { it != old }
            update(r.copy(tags = tags.distinct()))
        }
        extraTags = extraTags - old + listOfNotNull(new)
        prefs.edit().putStringSet("extraTags", extraTags).apply()
    }

    // MARK: Derived

    /** Distinct categories in source order, non-empty, plus user-added ones unused so far. */
    val categories: List<String>
        get() {
            val out = LinkedHashSet<String>()
            recipes.forEach { it.category?.takeIf(String::isNotEmpty)?.let(out::add) }
            out.addAll(extraCategories)
            return out.toList()
        }

    /** Distinct authors, sorted case-insensitively, non-empty. */
    val authors: List<String>
        get() = recipes.mapNotNull { it.author?.takeIf(String::isNotEmpty) }
            .distinct().sortedWith(String.CASE_INSENSITIVE_ORDER)

    /** Tags sorted by frequency then name, plus user-added ones unused so far. */
    val tags: List<String>
        get() {
            val counts = HashMap<String, Int>()
            recipes.forEach { r -> r.tags.forEach { counts[it] = (counts[it] ?: 0) + 1 } }
            extraTags.forEach { counts.putIfAbsent(it, 0) }
            return counts.keys.sortedWith(
                compareByDescending<String> { counts[it] }.thenBy { it }
            )
        }

    // MARK: Persistence

    private fun loadBundled(): List<Recipe> =
        runCatching {
            ctx.assets.open("Bundled/np3_list.json").use { it.readBytes().decodeToString() }
                .let { json.decodeFromString<List<Recipe>>(it) }
        }.getOrDefault(emptyList())

    private fun loadJsonList(key: String): List<Recipe> =
        prefs.getString(key, null)?.let { runCatching { json.decodeFromString<List<Recipe>>(it) }.getOrNull() }
            ?: emptyList()

    private fun loadJsonMap(key: String): Map<String, Recipe> =
        prefs.getString(key, null)?.let { runCatching { json.decodeFromString<Map<String, Recipe>>(it) }.getOrNull() }
            ?: emptyMap()

    private fun loadJsonStringList(key: String): List<String> =
        prefs.getString(key, null)?.let { runCatching { json.decodeFromString<List<String>>(it) }.getOrNull() }
            ?: emptyList()

    private fun loadJsonMapString(key: String): Map<String, String> =
        prefs.getString(key, null)?.let { runCatching { json.decodeFromString<Map<String, String>>(it) }.getOrNull() }
            ?: emptyMap()

    private fun saveCustom() = prefs.edit().putString("custom", json.encodeToString(custom)).apply()
    private fun saveOverrides() = prefs.edit().putString("overrides", json.encodeToString(overrides)).apply()
}
