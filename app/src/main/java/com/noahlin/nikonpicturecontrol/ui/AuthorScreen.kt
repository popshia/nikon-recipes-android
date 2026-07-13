package com.noahlin.nikonpicturecontrol.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.noahlin.nikonpicturecontrol.RecipeStore
import com.noahlin.nikonpicturecontrol.firstImageModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthorScreen(author: String, store: RecipeStore, nav: NavController) {
    val ctx = LocalContext.current
    val recipes = store.recipes.filter { it.author == author }
    val siteUrl = recipes.firstNotNullOfOrNull { it.authorUrl }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(author, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { pad ->
        LazyColumn(Modifier.padding(pad)) {
            siteUrl?.let { url ->
                item {
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                        Text("  Visit $author", color = MaterialTheme.colorScheme.primary)
                    }
                    Divider()
                }
            }
            item {
                Text(
                    "${recipes.size} Recipe${if (recipes.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp),
                )
            }
            items(recipes, key = { it.id }) { recipe ->
                Row(
                    Modifier.fillMaxWidth()
                        .clickable { nav.navigate("detail/${recipe.id}") }
                        .padding(16.dp, 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SampleImage(recipe.firstImageModel(ctx),
                        Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)))
                    Column {
                        Text(recipe.name, style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        recipe.category?.takeIf { it.isNotEmpty() }?.let {
                            Text(it, style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
