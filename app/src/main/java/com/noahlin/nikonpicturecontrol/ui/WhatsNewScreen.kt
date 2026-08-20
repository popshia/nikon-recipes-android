package com.noahlin.nikonpicturecontrol.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.noahlin.nikonpicturecontrol.Recipe
import com.noahlin.nikonpicturecontrol.RecipeStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Settings → What's New: every fetch batch that brought new recipes, newest first. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewScreen(store: RecipeStore, nav: NavController) {
    val fmt = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    // Resolve ids against the current library so recipes removed upstream drop out (not blank rows).
    val batches = store.fetchHistory
        .map { it.at to it.ids.mapNotNull(store::recipe) }
        .filter { it.second.isNotEmpty() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("What's New") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { pad ->
        if (batches.isEmpty()) {
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No new recipes yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                batches.forEach { (at, recipes) ->
                    item(key = "h$at") {
                        Text(fmt.format(Date(at)), style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                    }
                    items(recipes, key = { "$at-${it.id}" }) { recipe: Recipe ->
                        RecipeRow(recipe) { nav.navigate("detail/${recipe.id}") }
                    }
                }
            }
        }
    }
}
