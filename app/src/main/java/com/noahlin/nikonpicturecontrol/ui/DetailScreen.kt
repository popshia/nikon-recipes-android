package com.noahlin.nikonpicturecontrol.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.noahlin.nikonpicturecontrol.RecipeStore
import com.noahlin.nikonpicturecontrol.imageModels
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(id: String, store: RecipeStore, nav: NavController) {
    val ctx = LocalContext.current
    val recipe = store.recipe(id) ?: return
    var note by remember(id) { mutableStateOf(store.note(recipe)) }
    var fullScreenIndex by remember { mutableStateOf<Int?>(null) }
    var preparingNp3 by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val images = recipe.imageModels(ctx)
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    // Show the recipe name in the top bar only once the in-content headline scrolls under it,
    // so the title isn't duplicated on screen at rest.
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val showBarTitle by remember {
        derivedStateOf { scrollState.value > with(density) { 240.dp.roundToPx() } }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showBarTitle,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        Text(recipe.name, maxLines = 1)
                    }
                },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { pad ->
      Box(Modifier.padding(pad).fillMaxSize()) {
        Column(
            Modifier.verticalScroll(scrollState).padding(16.dp).padding(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Image header. One sample → a single rounded hero; several → an M3 multi-browse
            // carousel (large lead item with the next samples peeking). Tap any to view fullscreen.
            val heroHeight = 280.dp
            if (images.size == 1) {
                SampleImage(
                    images[0],
                    Modifier.fillMaxWidth().height(heroHeight)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .clickable { fullScreenIndex = 0 },
                    contentScale = ContentScale.Crop,
                )
            } else if (images.size > 1) {
                val carouselState = rememberCarouselState { images.size }
                HorizontalMultiBrowseCarousel(
                    state = carouselState,
                    preferredItemWidth = 320.dp,
                    itemSpacing = 8.dp,
                    modifier = Modifier.fillMaxWidth().height(heroHeight),
                ) { i ->
                    SampleImage(
                        images[i],
                        Modifier.fillMaxSize()
                            .maskClip(MaterialTheme.shapes.extraLarge)
                            .clickable { fullScreenIndex = i },
                        contentScale = ContentScale.Crop,
                    )
                }
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

            recipe.description?.takeIf { it.isNotEmpty() }?.let {
                Section("About") { Text(it, style = MaterialTheme.typography.bodyLarge) }
            }

            if (recipe.settings.isNotEmpty()) {
                Section("Recommended Settings") {
                    Column {
                        recipe.settings.forEachIndexed { i, (label, value) ->
                            if (i > 0) HorizontalDivider()
                            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
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
                    // No border — the section card already provides the container.
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                    ),
                )
            }
        }

        // Floating toolbar (hand-rolled on stable APIs): a rounded pill of secondary actions
        // (favourite + edit) beside a primary FAB (save/share the .NP3, or open the creator link).
        val url = recipe.recipeUrl
        val barHeight = 56.dp
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 3.dp,
                shadowElevation = 3.dp,
                modifier = Modifier.height(barHeight),
            ) {
                Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { store.toggleFavorite(id) }) {
                        val fav = store.isFavorite(id)
                        Icon(
                            if (fav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (fav) "Remove from favorites" else "Add to favorites",
                            tint = if (fav) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                        )
                    }
                    IconButton(onClick = { nav.navigate("edit/$id") }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit recipe")
                    }
                }
            }
            val fabModifier = Modifier.height(barHeight)
            when {
                recipe.np3 != null -> FloatingActionButton(
                    onClick = {
                        if (!preparingNp3) scope.launch {
                            preparingNp3 = true
                            shareNp3(ctx, recipe)
                            preparingNp3 = false
                        }
                    },
                    modifier = fabModifier,
                ) {
                    if (preparingNp3) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = LocalContentColor.current,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(Icons.Default.Download, contentDescription = "Save / share .NP3 file")
                    }
                }
                url != null -> FloatingActionButton(
                    onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
                    modifier = fabModifier,
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open in browser")
                }
            }
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
        // Primary-coloured header + extra-large surfaceContainer card, matching Settings & Library.
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp),
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) { content() }
        }
    }
}
