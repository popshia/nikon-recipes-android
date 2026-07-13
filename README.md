# Nikon Recipes (Android)

Native Android / Jetpack Compose (Material 3) port of the iOS Nikon Flexible
Picture Controls app. Bundles the community recipe library offline: browse,
search, filter, view samples + recommended settings, create your own recipes,
and share each `.NP3` file via the system share sheet.

## Build

Open the folder in **Android Studio** (Koala+/AGP 8.5) and Run. Studio generates
the Gradle wrapper and downloads dependencies on first sync.

CLI (once a wrapper exists — `gradle wrapper` or Studio):

```
./gradlew installDebug
```

Requires JDK 17 and an Android device/emulator (minSdk 26 / Android 8.0).

## Data

The recipe library is the **same source data as the iOS app**, copied verbatim
into `app/src/main/assets/Bundled/` (`np3_list.json` + per-recipe folders). To
refresh it, re-run the iOS repo's `Scripts/build_np3_list.py`, then copy
`NikonPictureControl/Bundled/` over `app/src/main/assets/Bundled/`.

`np3_list.json` paths are relative to `Bundled/`, so `assets/Bundled/<path>`
resolves each `.NP3`/image — identical to the iOS bundle layout.

## Structure

- `Recipe.kt` — data model + asset/file path resolution (Coil models).
- `RecipeStore.kt` — the single `AndroidViewModel`: loads `np3_list.json`,
  holds bundled + custom recipes, favorites, notes, and overrides
  (persisted in `SharedPreferences`; custom assets in internal storage).
- `ui/LibraryScreen` — browse: search, filter sheet, card/list toggle, random.
- `ui/DetailScreen` — samples pager, settings, notes, `.NP3` share.
- `ui/AuthorScreen`, `ui/CreateRecipeScreen`, `ui/SettingsScreen`.

Attribution: all recipes, images, and `.NP3` files belong to their respective
authors (credited per recipe). This app only repackages the public library.
