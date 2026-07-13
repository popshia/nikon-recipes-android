package com.noahlin.nikonpicturecontrol

import android.content.Context
import kotlinx.serialization.Serializable
import java.io.File

/** Trim, or null if blank — treats blank user input as "not set" (mirrors iOS `nilIfBlank`). */
fun String?.nilIfBlank(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

/**
 * One Flexible Picture Control recipe. Bundled ones decode from `np3_list.json`;
 * user-created ones are added in-app (see [RecipeStore]).
 */
@Serializable
data class Recipe(
    val id: String,
    val name: String,
    val slug: String,
    val category: String? = null,
    val tags: List<String> = emptyList(),
    val description: String? = null,
    val author: String? = null,
    val authorLink: String? = null,
    val whiteBalance: String? = null,
    val whiteBalanceShift: String? = null,
    val activeDLighting: String? = null,
    val evComp: String? = null,
    val grain: String? = null,
    /** Default note bundled with the recipe (usually null); user notes live in [RecipeStore]. */
    val note: String? = null,
    /** Path of the `.NP3` file: bundle-relative under `Bundled/`, or a flat name in the custom dir. */
    val np3: String? = null,
    /** Sample-image paths (bundle-relative or flat custom names). */
    val images: List<String>? = null,
    /** External link to the recipe on the creator's site, if there is no bundled file. */
    val recipeLink: String? = null,
) {
    /** Settings rows that are actually filled in, for the detail view. */
    val settings: List<Pair<String, String>>
        get() = buildList {
            fun add(label: String, value: String?) { value.nilIfBlank()?.let { add(label to it) } }
            add("White Balance", whiteBalance)
            add("WB Shift", whiteBalanceShift)
            add("Active D-Lighting", activeDLighting)
            add("Exposure Comp.", evComp)
            add("Grain", grain)
        }

    val recipeUrl: String?
        get() = recipeLink.nilIfBlank()?.let { if (it.startsWith("http")) it else "https://$it" }

    val authorUrl: String?
        get() = authorLink.nilIfBlank()?.let { if (it.startsWith("http")) it else "https://$it" }
}

/** Where user-created recipes' image/NP3 files live. Internal storage, not cache, so it survives. */
fun Context.recipeAssetsDir(): File =
    File(filesDir, "RecipeLibrary").apply { mkdirs() }

/**
 * A Coil-loadable model for an asset path. A custom recipe's flat file (in the assets dir) wins;
 * otherwise it's a bundled asset under `assets/Bundled/`. Bundled paths are validated at build
 * time, so we hand back the asset URI without an existence check.
 */
fun Context.imageModel(name: String?): Any? {
    if (name.isNullOrEmpty()) return null
    val custom = File(recipeAssetsDir(), name)
    if (custom.exists()) return custom
    return "file:///android_asset/Bundled/$name"
}

fun Recipe.imageModels(context: Context): List<Any> =
    (images ?: emptyList()).mapNotNull { context.imageModel(it) }

fun Recipe.firstImageModel(context: Context): Any? = imageModels(context).firstOrNull()
