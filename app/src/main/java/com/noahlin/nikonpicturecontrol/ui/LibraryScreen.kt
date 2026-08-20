package com.noahlin.nikonpicturecontrol.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.rememberScrollState
import com.noahlin.nikonpicturecontrol.LibrarySort
import com.noahlin.nikonpicturecontrol.sortedFor
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.noahlin.nikonpicturecontrol.RecipeStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(store: RecipeStore, nav: NavController, savedTab: Boolean = false) {
    var category by remember { mutableStateOf<String?>(null) }
    var tag by remember { mutableStateOf<String?>(null) }
    var author by remember { mutableStateOf<String?>(null) }
    var asCards by remember { mutableStateOf(true) }
    var showFilters by remember { mutableStateOf(false) }

    // Popup after a fetch: always on a user pull ("Up to Date" or new); on the silent launch
    // refresh only when there actually are new recipes.
    var showFetchResult by remember { mutableStateOf(false) }
    var pulled by remember { mutableStateOf(false) }
    var prevFetching by remember { mutableStateOf(store.isFetching) }
    LaunchedEffect(store.isFetching) {
        if (!savedTab && prevFetching && !store.isFetching && store.fetchError == null) {
            if (pulled || store.lastFetchedNames.isNotEmpty()) showFetchResult = true
        }
        if (prevFetching && !store.isFetching) pulled = false
        prevFetching = store.isFetching
    }

    val hasFilter = category != null || tag != null || author != null

    // Re-tapping the Library tab or changing the sort scrolls back to top (shared token in the store).
    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()
    LaunchedEffect(store.libraryScrollTopToken) {
        if (store.libraryScrollTopToken > 0) {
            gridState.animateScrollToItem(0)
            listState.animateScrollToItem(0)
        }
    }

    val filtered = store.recipes.filter { r ->
        if (savedTab && !store.isFavorite(r.id)) return@filter false
        category?.let { if (r.category != it) return@filter false }
        tag?.let { if (it !in r.tags) return@filter false }
        author?.let { if (r.author != it) return@filter false }
        true
    }.sortedFor(store.librarySort, store.librarySortDescending)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (savedTab) "Favorites" else "Library") },
                navigationIcon = {
                    IconButton(onClick = { nav.navigate("settings") }) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                },
                actions = {
                    LibraryActions(
                        hasFilter = hasFilter, asCards = asCards,
                        onToggleView = { asCards = !asCards },
                        onFilter = { showFilters = true },
                        onCreate = { nav.navigate("create") },
                    )
                },
            )
        },
    ) { pad ->
        val body = @Composable {
        Column(Modifier.fillMaxSize()) {
            Text(
                "${filtered.size} of ${store.recipes.size} recipes",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
            )
            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (savedTab && !hasFilter)
                            "No favorites yet. Tap ♥ on a recipe to save it here."
                        else "No recipes — try a different filter.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 32.dp),
                    )
                }
            } else if (asCards) {
                // Adaptive column count: one column on phones, reflowing to several on
                // tablets/foldables. minSize > half a phone's width keeps phones single-column.
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(minSize = 300.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(filtered, key = { it.id }) { recipe ->
                        RecipeCard(recipe, isNew = recipe.id in store.newRecipeIds) {
                            nav.navigate("detail/${recipe.id}")
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filtered, key = { it.id }) { recipe ->
                        RecipeRow(recipe, isNew = recipe.id in store.newRecipeIds) {
                            nav.navigate("detail/${recipe.id}")
                        }
                    }
                }
            }
        }
        }
        // Pull-to-refresh fetches the latest recipes on the Library tab (Favorites just lists).
        if (savedTab) {
            Box(Modifier.padding(pad).fillMaxSize()) { body() }
        } else {
            PullToRefreshBox(
                isRefreshing = store.isFetching,
                onRefresh = { pulled = true; store.fetchLatest() },
                modifier = Modifier.padding(pad).fillMaxSize(),
            ) { body() }
        }
    }

    if (showFetchResult) {
        val names = store.lastFetchedNames
        val dismiss = { showFetchResult = false; store.clearLastFetched() }
        AlertDialog(
            onDismissRequest = dismiss,
            title = { Text(if (names.isEmpty()) "Up to Date"
                           else "${names.size} New Recipe${if (names.size == 1) "" else "s"}") },
            text = { Text(if (names.isEmpty()) "You already have the latest recipes."
                          else names.joinToString("\n")) },
            confirmButton = { TextButton(onClick = dismiss) { Text("OK") } },
        )
    }

    if (showFilters) {
        FilterSheet(
            store = store,
            category = category, onCategory = { category = it },
            tag = tag, onTag = { tag = it },
            author = author, onAuthor = { author = it },
            hasFilter = hasFilter,
            onClear = { category = null; tag = null; author = null },
            onDismiss = { showFilters = false },
        )
    }
}

/** Filter icon + overflow menu (view toggle, create), shared by the Library and Saved title bars. */
@Composable
private fun LibraryActions(
    hasFilter: Boolean,
    asCards: Boolean,
    onToggleView: () -> Unit,
    onFilter: () -> Unit,
    onCreate: () -> Unit,
) {
    IconButton(onClick = onFilter) {
        Icon(
            Icons.Default.FilterList,
            contentDescription = "Filter",
            tint = if (hasFilter) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    var showMenu by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { showMenu = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More options")
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text(if (asCards) "List view" else "Card view") },
                leadingIcon = {
                    Icon(
                        if (asCards) Icons.AutoMirrored.Filled.List else Icons.Default.GridView,
                        contentDescription = null,
                    )
                },
                onClick = { onToggleView(); showMenu = false },
            )
            DropdownMenuItem(
                text = { Text("Create recipe") },
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                onClick = { showMenu = false; onCreate() },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(
    store: RecipeStore,
    category: String?, onCategory: (String?) -> Unit,
    tag: String?, onTag: (String?) -> Unit,
    author: String?, onAuthor: (String?) -> Unit,
    hasFilter: Boolean, onClear: () -> Unit, onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Sort", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LibrarySort.entries.forEach { s ->
                    FilterChip(
                        selected = store.librarySort == s,
                        onClick = { store.setLibrarySort(s, store.librarySortDescending) },
                        label = { Text(s.label) },
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !store.librarySortDescending,
                    onClick = { store.setLibrarySort(store.librarySort, false) },
                    label = { Text("Ascending") },
                )
                FilterChip(
                    selected = store.librarySortDescending,
                    onClick = { store.setLibrarySort(store.librarySort, true) },
                    label = { Text("Descending") },
                )
            }
            Text("Filter", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            LabeledDropdown("Category", "All Categories", store.categories, category, onCategory)
            LabeledDropdown("Tag", "All Tags", store.tags, tag, onTag)
            LabeledDropdown("Author", "All Authors", store.authors, author, onAuthor)
            if (hasFilter) {
                Text("Clear Filters",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth().clickable { onClear() }.padding(vertical = 8.dp))
            }
        }
    }
}
