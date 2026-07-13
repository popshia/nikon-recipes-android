package com.noahlin.nikonpicturecontrol.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.noahlin.nikonpicturecontrol.RecipeStore
import com.noahlin.nikonpicturecontrol.imageModels

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(id: String, store: RecipeStore, nav: NavController) {
    val ctx = LocalContext.current
    val recipe = store.recipe(id) ?: return
    var note by remember(id) { mutableStateOf(store.note(recipe)) }
    var fullScreenIndex by remember { mutableStateOf<Int?>(null) }
    val images = recipe.imageModels(ctx)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(recipe.name, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { nav.navigate("edit/$id") }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit recipe")
                    }
                    IconButton(onClick = { store.toggleFavorite(id) }) {
                        val fav = store.isFavorite(id)
                        Icon(
                            if (fav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (fav) "Remove from favorites" else "Add to favorites",
                            tint = if (fav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
    ) { pad ->
        Column(
            Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Image header: one fills width; several page horizontally. Tap to view fullscreen.
            if (images.size > 1) {
                val pager = rememberPagerState { images.size }
                HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth()) { page ->
                    SampleImage(images[page],
                        Modifier.fillMaxWidth().heightIn(max = 360.dp)
                            .clickable { fullScreenIndex = page },
                        contentScale = ContentScale.Fit)
                }
            } else if (images.size == 1) {
                SampleImage(images[0],
                    Modifier.fillMaxWidth().heightIn(max = 360.dp)
                        .clickable { fullScreenIndex = 0 },
                    contentScale = ContentScale.Fit)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(recipe.name, style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    recipe.category?.takeIf { it.isNotEmpty() }?.let { CategoryChip(it) }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    recipe.author?.takeIf { it.isNotEmpty() }?.let { a ->
                        Text(a, style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { nav.navigate("author/${Uri.encode(a)}") })
                    } ?: Spacer(Modifier.weight(1f))
                    if (recipe.tags.isNotEmpty()) {
                        Text(recipe.tags.joinToString(" · "), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }
            }

            // Download / share
            if (recipe.np3 != null) {
                OutlinedButton(onClick = { shareNp3(ctx, recipe) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(Modifier.size(8.dp)); Text("Save / Share .NP3 File")
                }
            } else recipe.recipeUrl?.let { url ->
                OutlinedButton(
                    onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                    Spacer(Modifier.size(8.dp)); Text("Download from Creator")
                }
            }

            recipe.description?.takeIf { it.isNotEmpty() }?.let {
                Section("About") { Text(it, style = MaterialTheme.typography.bodyLarge) }
            }

            if (recipe.settings.isNotEmpty()) {
                Section("Recommended Settings") {
                    Column {
                        recipe.settings.forEachIndexed { i, (label, value) ->
                            if (i > 0) Divider()
                            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                                Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f))
                                Text(value, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            Section("Notes") {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it; store.setNote(it, id) },
                    placeholder = { Text("Add a note…") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
                )
            }
        }
    }

    fullScreenIndex?.let { start ->
        FullScreenImageViewer(images, start) { fullScreenIndex = null }
    }
}

/** Black-backed, paged fullscreen image viewer opened by tapping a sample. */
@Composable
private fun FullScreenImageViewer(images: List<Any>, start: Int, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            val pager = rememberPagerState(initialPage = start) { images.size }
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
                SampleImage(images[page], Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp),
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) { content() }
        }
    }
}
