package com.noahlin.nikonpicturecontrol.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    val hasFilter = category != null || tag != null || author != null

    val filtered = store.recipes.filter { r ->
        if (savedTab && !store.isFavorite(r.id)) return@filter false
        category?.let { if (r.category != it) return@filter false }
        tag?.let { if (it !in r.tags) return@filter false }
        author?.let { if (r.author != it) return@filter false }
        true
    }

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
        Column(Modifier.padding(pad).fillMaxSize()) {
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
                    columns = GridCells.Adaptive(minSize = 300.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(filtered, key = { it.id }) { recipe ->
                        RecipeCard(recipe) { nav.navigate("detail/${recipe.id}") }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filtered, key = { it.id }) { recipe ->
                        RecipeRow(recipe) { nav.navigate("detail/${recipe.id}") }
                    }
                }
            }
        }
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
