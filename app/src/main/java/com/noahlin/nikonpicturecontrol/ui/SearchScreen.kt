package com.noahlin.nikonpicturecontrol.ui

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.noahlin.nikonpicturecontrol.Recipe
import com.noahlin.nikonpicturecontrol.RecipeStore

@Composable
fun SearchScreen(store: RecipeStore, nav: NavController) {
    var query by remember { mutableStateOf("") }
    val open = { id: String -> store.addRecentlySearched(id); nav.navigate("detail/$id") }

    Scaffold(
        topBar = {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text("Search recipes") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 4.dp),
            )
        },
    ) { pad ->
        if (query.isBlank()) {
            BrowseLanding(store, Modifier.padding(pad), open, nav)
        } else {
            val results = store.recipes
                .map { it to score(it, query) }
                .filter { it.second > 0 }
                .sortedWith(compareByDescending<Pair<Recipe, Int>> { it.second }.thenBy { it.first.name.lowercase() })
                .map { it.first }
            if (results.isEmpty()) {
                Column(Modifier.padding(pad).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.padding(top = 48.dp))
                    Text("No matches", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    Modifier.padding(pad),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    items(results, key = { it.id }) { RecipeRow(it) { open(it.id) } }
                }
            }
        }
    }
}

@Composable
private fun BrowseLanding(
    store: RecipeStore,
    modifier: Modifier,
    open: (String) -> Unit,
    nav: NavController,
) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
        // Recently Searched — animates out when cleared.
        item {
            val recent = store.recentlySearched
            AnimatedVisibility(visible = recent.isNotEmpty()) {
                Rail(
                    title = "Recently Searched",
                    recipes = recent,
                    open = open,
                    trailing = {
                        TextButton(onClick = { store.clearRecentlySearched() }) { Text("Clear") }
                    },
                )
            }
        }
        // Browse by category.
        items(store.categories, key = { "cat-$it" }) { cat ->
            val recipes = store.recipes.filter { it.category == cat }.take(12)
            if (recipes.isNotEmpty()) {
                Rail(
                    title = cat,
                    recipes = recipes,
                    open = open,
                    onHeader = { nav.navigate("browse/category/${Uri.encode(cat)}") },
                )
            }
        }
        // Browse by top tags.
        items(store.tags.take(8), key = { "tag-$it" }) { tag ->
            val recipes = store.recipes.filter { tag in it.tags }.take(12)
            if (recipes.isNotEmpty()) {
                Rail(
                    title = "#$tag",
                    recipes = recipes,
                    open = open,
                    onHeader = { nav.navigate("browse/tag/${Uri.encode(tag)}") },
                )
            }
        }
    }
}

@Composable
private fun Rail(
    title: String,
    recipes: List<Recipe>,
    open: (String) -> Unit,
    onHeader: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Row(
            Modifier.fillMaxWidth()
                .then(if (onHeader != null) Modifier.clickable(onClick = onHeader) else Modifier)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f))
            trailing?.invoke()
            if (onHeader != null) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(recipes, key = { it.id }) { RailCard(it) { open(it.id) } }
        }
    }
}

/** Relevance score mirroring the iOS Search ranking; 0 means no match. */
private fun score(r: Recipe, q: String): Int {
    val query = q.trim().lowercase()
    if (query.isEmpty()) return 0
    var s = 0
    val name = r.name.lowercase()
    when {
        name == query -> s += 100
        name.startsWith(query) -> s += 60
        name.contains(query) -> s += 40
    }
    if (r.author?.lowercase()?.contains(query) == true) s += 20
    if (r.tags.any { it.lowercase().contains(query) }) s += 15
    if (r.category?.lowercase()?.contains(query) == true) s += 8
    if (r.description?.lowercase()?.contains(query) == true) s += 4
    return s
}
