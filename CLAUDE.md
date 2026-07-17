# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Native Android / Jetpack Compose (Material 3) port of the iOS Nikon Flexible Picture Controls app.
Browse, search, filter, view samples + recommended settings, create your own recipes, and share
each `.NP3` file via the system share sheet. The community recipe library is **cloud-hosted on
Cloudflare R2** (not bundled) — see the Data section.

## Build & run

Open in Android Studio (Koala+/AGP 8.5) and Run — Studio generates the Gradle wrapper and
dependencies on first sync.

CLI (once a wrapper exists):
```
./gradlew installDebug
```
Requires JDK 17 and an Android device/emulator (minSdk 26 / Android 8.0, target/compile 36).

There is no test suite in this repo — verify changes by running the app.

## Data: the cloud-hosted recipe library

The app ships with **no recipe data** and streams the library from Cloudflare R2 — the same
bucket the iOS app uses (`Recipe.IMAGE_BASE_URL`). On first launch `RecipeStore.fetchLatest()`
downloads the index (`<base>/np3_list.json`) into internal storage, caches it, and prefetches
every list thumbnail (behind the `LibraryGate` in `MainActivity`). Full images and `.NP3` files
stream on demand; Coil disk-caches images, and cloud `.NP3`s download to `cache/NP3/<id>/` when
shared. Settings → **Fetch Latest Recipes** re-pulls the index, so new recipes appear with no app
rebuild. There is no data to copy from the iOS repo — both apps point at the same R2 URLs.

Index/asset schema: each recipe carries relative asset paths (`images`, `thumb`, `np3`); a remote
URL is `IMAGE_BASE_URL + path` with segments percent-encoded (paths contain spaces). `assetHash`
is reserved for future cache eviction (the index does not emit it yet).

## Architecture

- **`Recipe.kt`** — the data model (`@Serializable`, decodes `np3_list.json` directly) plus
  asset/file path resolution for Coil. `imageModel()` is the key indirection: a custom recipe's
  flat file in internal storage wins; otherwise it resolves to the remote R2 URL — so user-created
  and cloud recipes share one image-loading path. `thumbModel()`/`np3Model()` layer on top.
- **`RecipeStore.kt`** — the single `AndroidViewModel` and single source of truth. Loads the
  `library` from the cached index (empty on first launch) and refreshes it from R2 via
  `fetchLatest()` (+ `prefetchThumbnails()`); holds custom (user-created) recipes and per-recipe
  `overrides` as separate lists, merged on read (`merged()`): `library.map { overrides[it.id] ?: it } + custom`.
  Editing a library recipe never mutates the index — it creates an override entry instead.
  Favorites, notes, and extra categories/tags persist to `SharedPreferences` as JSON
  (kotlinx.serialization); custom recipe assets live in internal storage
  (`Context.recipeAssetsDir()`), not cache, so they survive.
- **Navigation** — single-Activity, Compose Navigation, all routes wired in `MainActivity.kt`'s
  `NavHost`: `library`, `detail/{id}`, `author/{name}`, `create`, `edit/{id}`, `settings`,
  `terms/{kind}`, `guide`.
- **`ui/`** — one file per screen (`LibraryScreen`, `DetailScreen`, `AuthorScreen`,
  `CreateRecipeScreen`, `SettingsScreen`), plus `Common.kt` for shared composables and `Theme.kt`
  for the Material 3 theme.

## Conventions

- Category/tag rename operations (`RecipeStore.renameCategory`/`renameTag`) rewrite every affected
  recipe via `update()` rather than storing categories/tags as a separate normalized entity —
  keep that pattern if extending recipe metadata.
- `String?.nilIfBlank()` (in `Recipe.kt`) is the standard way to treat blank user input as unset;
  mirrors the iOS app's `nilIfBlank`. Use it instead of ad hoc blank checks.
- All recipes, images, and `.NP3` files belong to their respective authors (credited per recipe).
  This app only repackages the public library — don't alter attribution fields casually.
