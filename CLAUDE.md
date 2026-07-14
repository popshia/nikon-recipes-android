# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Native Android / Jetpack Compose (Material 3) port of the iOS Nikon Flexible Picture Controls app.
Bundles the community recipe library offline: browse, search, filter, view samples + recommended
settings, create your own recipes, and share each `.NP3` file via the system share sheet.

## Build & run

Open in Android Studio (Koala+/AGP 8.5) and Run — Studio generates the Gradle wrapper and
dependencies on first sync.

CLI (once a wrapper exists):
```
./gradlew installDebug
```
Requires JDK 17 and an Android device/emulator (minSdk 26 / Android 8.0, target/compile 36).

There is no test suite in this repo — verify changes by running the app.

## Data: the bundled recipe library

The recipe library is the **same source data as the iOS app**, copied verbatim into
`app/src/main/assets/Bundled/` (`np3_list.json` + per-recipe folders, each with a `.jpg`, `.json`,
and `.NP3`). To refresh it, re-run the iOS repo's `Scripts/build_np3_list.py`, then copy
`NikonPictureControl/Bundled/` over `app/src/main/assets/Bundled/`.

`np3_list.json` paths are relative to `Bundled/`, so `assets/Bundled/<path>` resolves each
`.NP3`/image — identical to the iOS bundle layout. `.NP3` files are excluded from asset
compression (`androidResources.noCompress`) since they're opened straight from assets.

## Architecture

- **`Recipe.kt`** — the data model (`@Serializable`, decodes `np3_list.json` directly) plus
  asset/file path resolution for Coil. `imageModel()` is the key indirection: a custom recipe's
  flat file in internal storage wins over the bundled asset path — this is what lets user-created
  and bundled recipes share the same image-loading code path.
- **`RecipeStore.kt`** — the single `AndroidViewModel` and single source of truth. Loads bundled
  recipes from `np3_list.json` once; holds custom (user-created) recipes and per-recipe
  `overrides` as separate lists, merged on read (`merged()`): `bundled.map { overrides[it.id] ?: it } + custom`.
  Editing a bundled recipe never mutates the bundled JSON — it creates an override entry instead.
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
