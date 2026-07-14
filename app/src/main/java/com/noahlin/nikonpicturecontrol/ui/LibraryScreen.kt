package com.noahlin.nikonpicturecontrol.ui

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.noahlin.nikonpicturecontrol.Recipe
import com.noahlin.nikonpicturecontrol.RecipeStore
import com.noahlin.nikonpicturecontrol.firstImageModel
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(store: RecipeStore, nav: NavController, savedTab: Boolean = false) {
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<String?>(null) }
    var tag by remember { mutableStateOf<String?>(null) }
    var author by remember { mutableStateOf<String?>(null) }
    var favoritesOnly by remember { mutableStateOf(false) }
    var asCards by remember { mutableStateOf(true) }
    var showFilters by remember { mutableStateOf(false) }
    var showRandom by remember { mutableStateOf(false) }
    var searchExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val hasFilter = category != null || tag != null || author != null || favoritesOnly

    val filtered = store.recipes.filter { r ->
        if ((favoritesOnly || savedTab) && !store.isFavorite(r.id)) return@filter false
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

    Box(Modifier.fillMaxSize()) {
    Scaffold(
        topBar = {
            // Saved tab gets a plain title bar; the Library tab uses a docked search bar (below).
            if (savedTab) {
                CenterAlignedTopAppBar(
                    title = { Text("Saved") },
                    actions = {
                        LibraryActions(
                            hasFilter = hasFilter, asCards = asCards,
                            onToggleView = { asCards = !asCards },
                            onFilter = { showFilters = true },
                            onCreate = { nav.navigate("create") },
                        )
                    },
                )
            }
        },
        floatingActionButton = {
            if (!showRandom && !searchExpanded) {
                FloatingActionButton(
                    onClick = {
                        val recipe = store.recipes.randomOrNull() ?: return@FloatingActionButton
                        showRandom = true
                        scope.launch {
                            delay(1500)
                            showRandom = false
                            nav.navigate("detail/${recipe.id}")
                        }
                    },
                ) {
                    Icon(Icons.Default.Casino, contentDescription = "Random recipe")
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                if (!savedTab) {
                    // Docked M3 search bar: tapping the field opens the full-screen search view;
                    // filter + overflow live inside the bar (Play Store / Photos pattern).
                    Surface(
                        onClick = { searchExpanded = true },
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .height(56.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(Modifier.size(16.dp))
                            Icon(
                                Icons.Default.Search, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.size(16.dp))
                            Text(
                                "Search ${store.recipes.size} recipes",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            LibraryActions(
                                hasFilter = hasFilter, asCards = asCards,
                                onToggleView = { asCards = !asCards },
                                onFilter = { showFilters = true },
                                onCreate = { nav.navigate("create") },
                            )
                        }
                    }
                }
                Text(
                    "${filtered.size} of ${store.recipes.size} recipes",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
                )
                if (filtered.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            if (savedTab && search.isEmpty() && !hasFilter)
                                "No saved recipes yet. Tap ♥ on a recipe to save it here."
                            else "No recipes — try a different search or filter.",
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
            AnimatedVisibility(
                visible = showRandom,
                enter = fadeIn() + scaleIn(initialScale = 0.87f),
                exit = fadeOut(),
            ) {
                RandomRecipeOverlay()
            }
        }
    }
    // Fullscreen search, opened from the top-bar search icon.
    if (searchExpanded) {
        // Auto-focus the field so the keyboard shows the moment the search view opens.
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
        SearchBar(
            modifier = Modifier.align(Alignment.TopStart).fillMaxWidth(),
            expanded = true,
            onExpandedChange = { searchExpanded = it },
            inputField = {
                SearchBarDefaults.InputField(
                    modifier = Modifier.focusRequester(focusRequester),
                    query = search,
                    onQueryChange = { search = it },
                    onSearch = {},
                    expanded = true,
                    onExpandedChange = { searchExpanded = it },
                    placeholder = { Text("Search ${store.recipes.size} recipes") },
                    leadingIcon = {
                        IconButton(onClick = { searchExpanded = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    trailingIcon = {
                        if (search.isNotEmpty()) {
                            IconButton(onClick = { search = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                )
            },
        ) {
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                items(filtered, key = { it.id }) { recipe ->
                    RecipeRow(recipe) { searchExpanded = false; nav.navigate("detail/${recipe.id}") }
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

/** Filter icon + overflow menu, shared by the docked search bar (Library) and the Saved title bar. */
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

/** Full-screen "rolling the dice" overlay shown briefly before opening a random recipe. */
@Composable
private fun RandomRecipeOverlay() {
    val transition = rememberInfiniteTransition(label = "dice")
    val angle by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "spin",
    )
    val pulse by transition.animateFloat(
        initialValue = 1f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "pulse",
    )
    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                Icons.Default.Casino,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(96.dp).graphicsLayer {
                    rotationZ = angle; scaleX = pulse; scaleY = pulse
                },
            )
            Text(
                "Feeling the mood for today...",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RecipeCard(recipe: Recipe, onClick: () -> Unit) {
    val ctx = LocalContext.current
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
