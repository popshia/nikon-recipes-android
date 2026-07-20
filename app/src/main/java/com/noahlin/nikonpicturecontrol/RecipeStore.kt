package com.noahlin.nikonpicturecontrol

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

/**
 * Holds the recipe library: the index downloaded from Cloudflare R2 (cached on disk) plus any
 * recipes the user creates in-app. State is exposed as Compose state; user data persists in
 * SharedPreferences. The library is cloud-first — empty on first launch until [fetchLatest].
 */
class RecipeStore(app: Application) : AndroidViewModel(app) {
    private val ctx: Context get() = getApplication()
    private val prefs = app.getSharedPreferences("recipes", Context.MODE_PRIVATE)

    /** The main library, loaded from the locally-cached index and refreshed from R2. */
    private var library by mutableStateOf(loadIndexCache())
    private var custom: List<Recipe> = loadJsonList("custom")
    private var overrides: Map<String, Recipe> = loadJsonMap("overrides")

    var recipes by mutableStateOf(merged()); private set

    // Remote-sync state, observed by the UI.
    var isFetching by mutableStateOf(false); private set
    /** True while the first-launch fetch runs (no cached library yet) — drives the full-screen gate. */
    var isPreparingInitial by mutableStateOf(false); private set
    var fetchError by mutableStateOf<String?>(null); private set
    val hasLibrary: Boolean get() = library.isNotEmpty()

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
        library.map { overrides[it.id] ?: it } + custom

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

    // MARK: Remote sync (Cloudflare R2)

    /** Locally-cached copy of the index, refreshed by [fetchLatest]. */
    private fun indexCacheFile(): File = File(ctx.filesDir, "np3_list.json")

    private fun loadIndexCache(): List<Recipe> =
        indexCacheFile().takeIf { it.exists() }
            ?.let { runCatching { json.decodeFromString<List<Recipe>>(it.readText()) }.getOrNull() }
            ?: emptyList()

    /**
     * Download the latest index from R2, cache it, and refresh the library. The gate waits only on
     * the index — thumbnails and full images stream in on demand (Coil disk-caches each as it's
     * shown). Safe to call repeatedly (re-entrancy guarded).
     */
    fun fetchLatest() {
        if (isFetching) return
        isFetching = true
        isPreparingInitial = library.isEmpty()
        fetchError = null
        viewModelScope.launch {
            try {
                val body = withContext(Dispatchers.IO) { httpGet(Recipe.INDEX_URL) }
                val list = json.decodeFromString<List<Recipe>>(body)
                // ponytail: iOS evicts stale caches by comparing per-recipe assetHash, but the R2
                // index emits no assetHash yet, so eviction is inert — skip until hashes land.
                withContext(Dispatchers.IO) { indexCacheFile().writeText(body) }
                library = list
                recipes = merged()
            } catch (e: Exception) {
                fetchError = e.message ?: "Couldn't load recipes"
            } finally {
                isFetching = false
                isPreparingInitial = false
            }
        }
    }

    private fun httpGet(urlStr: String): String {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000; readTimeout = 20_000; requestMethod = "GET"
        }
        try {
            if (conn.responseCode >= 400) throw IOException("Server returned ${conn.responseCode}")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    // MARK: Persistence

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
