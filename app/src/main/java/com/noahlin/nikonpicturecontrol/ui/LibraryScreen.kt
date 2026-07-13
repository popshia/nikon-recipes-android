package com.noahlin.nikonpicturecontrol.ui

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.noahlin.nikonpicturecontrol.Recipe
import com.noahlin.nikonpicturecontrol.RecipeStore
import com.noahlin.nikonpicturecontrol.firstImageModel
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(store: RecipeStore, nav: NavController) {
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<String?>(null) }
    var tag by remember { mutableStateOf<String?>(null) }
    var author by remember { mutableStateOf<String?>(null) }
    var favoritesOnly by remember { mutableStateOf(false) }
    var asCards by remember { mutableStateOf(true) }
    var showFilters by remember { mutableStateOf(false) }

    val hasFilter = category != null || tag != null || author != null || favoritesOnly

    val filtered = store.recipes.filter { r ->
        if (favoritesOnly && !store.isFavorite(r.id)) return@filter false
        category?.let { if (r.category != it) return@filter false }
        tag?.let { if (it !in r.tags) return@filter false }
        author?.let { if (r.author != it) return@filter false }
        if (search.isNotEmpty()) {
            val hay = listOf(r.name, r.author ?: "", r.description ?: "",
                r.tags.joinToString(" "), r.category ?: "").joinToString(" ").lowercase()
            if (!hay.contains(search.lowercase())) return@filter false
        }
        true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Nikon Recipes", fontWeight = FontWeight.Bold)
                        Text(
                            "${filtered.size} of ${store.recipes.size} recipes",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { asCards = !asCards }) {
                        Icon(
                            if (asCards) Icons.AutoMirrored.Filled.List else Icons.Default.GridView,
                            contentDescription = "Toggle view",
                        )
                    }
                    IconButton(onClick = { showFilters = true }) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = if (hasFilter) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(onClick = { nav.navigate("create") }) {
                        Icon(Icons.Default.Add, contentDescription = "Create recipe")
                    }
                    IconButton(onClick = { nav.navigate("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                store.recipes.randomOrNull()?.let { nav.navigate("detail/${it.id}") }
            }) {
                Icon(Icons.Default.Casino, contentDescription = "Random recipe")
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Search ${filtered.size} recipes") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            )
            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No recipes — try a different search or filter.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(if (asCards) 14.dp else 4.dp),
                ) {
                    items(filtered, key = { it.id }) { recipe ->
                        if (asCards) RecipeCard(recipe) { nav.navigate("detail/${recipe.id}") }
                        else RecipeRow(recipe) { nav.navigate("detail/${recipe.id}") }
                    }
                }
            }
        }
    }

    if (showFilters) {
        FilterSheet(
            store = store,
            favoritesOnly = favoritesOnly, onFavoritesOnly = { favoritesOnly = it },
            category = category, onCategory = { category = it },
            tag = tag, onTag = { tag = it },
            author = author, onAuthor = { author = it },
            hasFilter = hasFilter,
            onClear = { category = null; tag = null; author = null; favoritesOnly = false },
            onDismiss = { showFilters = false },
        )
    }
}

@Composable
private fun RecipeCard(recipe: Recipe, onClick: () -> Unit) {
    val ctx = LocalContext.current
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        SampleImage(
            recipe.firstImageModel(ctx),
            modifier = Modifier.fillMaxWidth().height(200.dp),
        )
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(recipe.name, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                recipe.category?.takeIf { it.isNotEmpty() }?.let {
                    Spacer(Modifier.size(8.dp)); CategoryChip(it)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                recipe.author?.takeIf { it.isNotEmpty() }?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                } ?: Spacer(Modifier.weight(1f))
                if (recipe.tags.isNotEmpty()) {
                    Text(recipe.tags.joinToString(" · "), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun RecipeRow(recipe: Recipe, onClick: () -> Unit) {
    val ctx = LocalContext.current
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SampleImage(
            recipe.firstImageModel(ctx),
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
        )
        Column {
            Text(recipe.name, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            recipe.author?.takeIf { it.isNotEmpty() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(
    store: RecipeStore,
    favoritesOnly: Boolean, onFavoritesOnly: (Boolean) -> Unit,
    category: String?, onCategory: (String?) -> Unit,
    tag: String?, onTag: (String?) -> Unit,
    author: String?, onAuthor: (String?) -> Unit,
    hasFilter: Boolean, onClear: () -> Unit, onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Filter", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Favorites Only", Modifier.weight(1f))
                Switch(checked = favoritesOnly, onCheckedChange = onFavoritesOnly)
            }
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
