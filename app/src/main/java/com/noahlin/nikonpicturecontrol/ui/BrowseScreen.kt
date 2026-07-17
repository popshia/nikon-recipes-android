package com.noahlin.nikonpicturecontrol.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.noahlin.nikonpicturecontrol.RecipeStore

/** Filtered recipe list reached from a Search browse-rail header (by category or tag). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(kind: String, value: String, store: RecipeStore, nav: NavController) {
    val recipes = store.recipes.filter {
        if (kind == "tag") value in it.tags else it.category == value
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(value) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            items(recipes, key = { it.id }) { RecipeRow(it) { nav.navigate("detail/${it.id}") } }
        }
    }
}
