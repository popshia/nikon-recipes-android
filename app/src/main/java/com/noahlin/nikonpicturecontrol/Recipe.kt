package com.noahlin.nikonpicturecontrol

import android.content.Context
import android.net.Uri
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
    /** Sample-image paths (R2-relative, or flat custom names). */
    val images: List<String>? = null,
    /** Small list thumbnail path (R2-relative), if any. Falls back to the first image. */
    val thumb: String? = null,
    /** Content hash of the recipe's assets, set at import — reserved for cache eviction (see [RecipeStore]). */
    val assetHash: String? = null,
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

    companion object {
        /** Public base for recipe assets hosted on Cloudflare R2. Asset names are the relative
         *  paths stored in the index, so a remote URL is simply base + name (segments encoded). */
        const val IMAGE_BASE_URL = "https://pub-61aff30f035a49c391df4e7286a6369f.r2.dev"

        /** The index (np3_list.json) location on R2. */
        const val INDEX_URL = "$IMAGE_BASE_URL/np3_list.json"
    }
}

/** Where user-created recipes' image/NP3 files live. Internal storage, not cache, so it survives. */
fun Context.recipeAssetsDir(): File =
    File(filesDir, "RecipeLibrary").apply { mkdirs() }

/** Remote R2 URL for an asset name, with each path segment percent-encoded (names contain spaces). */
private fun remoteAssetUrl(name: String): String {
    val b = Uri.parse(Recipe.IMAGE_BASE_URL).buildUpon()
    name.split("/").forEach { if (it.isNotEmpty()) b.appendPath(it) }
    return b.build().toString()
}

/**
 * A Coil-loadable model for an asset path. A custom recipe's local file (in the internal assets
 * dir) wins; otherwise the asset streams from Cloudflare R2 (Coil disk-caches it). Returns a
 * [File] for local assets or a URL [String] for remote ones — both load through Coil.
 */
fun Context.imageModel(name: String?): Any? {
    if (name.isNullOrEmpty()) return null
    val custom = File(recipeAssetsDir(), name)
    if (custom.exists()) return custom
    return remoteAssetUrl(name)
}

fun Recipe.imageModels(context: Context): List<Any> =
    (images ?: emptyList()).mapNotNull { context.imageModel(it) }

fun Recipe.firstImageModel(context: Context): Any? = imageModels(context).firstOrNull()

/** Small list/rail thumbnail model (R2), falling back to the first full image if absent. */
fun Recipe.thumbModel(context: Context): Any? =
    context.imageModel(thumb) ?: firstImageModel(context)

/** Model for the `.NP3`: a local custom file, else its remote R2 URL string. */
fun Recipe.np3Model(context: Context): Any? = context.imageModel(np3)
