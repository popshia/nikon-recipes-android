package com.noahlin.nikonpicturecontrol.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwipeRight
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.noahlin.nikonpicturecontrol.Recipe
import com.noahlin.nikonpicturecontrol.RecipeStore
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Swipeable pile of recipe cards: tap the top card to open it, swipe it away to skip. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RandomScreen(store: RecipeStore, nav: NavController) {
    // Keep 3 visible + 1 buffered card, all distinct random picks.
    var deck by remember { mutableStateOf(emptyList<Recipe>()) }

    fun refill() {
        val pool = store.recipes
        val ids = deck.map { it.id }.toMutableSet()
        val extras = pool.filter { it.id !in ids }.shuffled()
        var i = 0
        val out = deck.toMutableList()
        while (out.size < 4 && i < extras.size) out.add(extras[i++])
        deck = out
    }

    LaunchedEffect(store.recipes.size) { if (deck.isEmpty()) refill() }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Random Recipe") }) },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) {
            if (deck.isEmpty()) {
                Text("No recipes yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Box
            }
            Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
                val shown = deck.take(3)
                // Draw back-to-front so the top card (depth 0) is on top and takes touches.
                for (depth in shown.indices.reversed()) {
                    val recipe = shown[depth]
                    if (depth == 0) {
                        TopCard(
                            recipe = recipe,
                            onOpen = { nav.navigate("detail/${recipe.id}") },
                            onSkip = { deck = deck.drop(1); refill() },
                        )
                    } else {
                        Box(
                            Modifier.fillMaxWidth().graphicsLayer {
                                scaleX = 1f - depth * 0.05f
                                scaleY = 1f - depth * 0.05f
                                translationY = depth * 14.dp.toPx()
                            },
                        ) {
                            RecipeCard(recipe) {}
                        }
                    }
                }
            }
            Row(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Hint(Icons.Default.TouchApp, "Tap to open")
                Hint(Icons.Default.SwipeRight, "Swipe to skip")
            }
        }
    }
}

@Composable
private fun TopCard(recipe: Recipe, onOpen: () -> Unit, onSkip: () -> Unit) {
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val density = LocalDensity.current
    val thresholdPx = with(density) { 110.dp.toPx() }
    val flingPx = with(density) { (LocalConfiguration.current.screenWidthDp.dp.toPx()) * 1.3f }

    Box(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationX = offsetX.value
                translationY = offsetY.value
                rotationZ = offsetX.value / 18f
            }
            // Key by recipe id so the gesture detector resets when the top card changes.
            .pointerInput(recipe.id) {
                detectDragGestures(
                    onDrag = { change, drag ->
                        change.consume()
                        scope.launch { offsetX.snapTo(offsetX.value + drag.x) }
                        scope.launch { offsetY.snapTo(offsetY.value + drag.y) }
                    },
                    onDragEnd = {
                        // Decide inside a launch so the per-drag snapTo coroutines have applied
                        // first — reading offsetX.value synchronously here would be stale.
                        scope.launch {
                            if (abs(offsetX.value) > thresholdPx) {
                                val dir = if (offsetX.value > 0) 1f else -1f
                                offsetX.animateTo(dir * flingPx, tween(280))
                                onSkip()
                                offsetX.snapTo(0f); offsetY.snapTo(0f)
                            } else {
                                val back = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy)
                                launch { offsetX.animateTo(0f, back) }
                                launch { offsetY.animateTo(0f, back) }
                            }
                        }
                    },
                )
            },
    ) {
        RecipeCard(recipe, onClick = onOpen)
    }
}

@Composable
private fun Hint(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
