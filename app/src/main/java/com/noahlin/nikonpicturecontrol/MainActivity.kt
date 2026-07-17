package com.noahlin.nikonpicturecontrol

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.noahlin.nikonpicturecontrol.ui.AuthorScreen
import com.noahlin.nikonpicturecontrol.ui.BrowseScreen
import com.noahlin.nikonpicturecontrol.ui.CreateRecipeScreen
import com.noahlin.nikonpicturecontrol.ui.DetailScreen
import com.noahlin.nikonpicturecontrol.ui.EditTermsScreen
import com.noahlin.nikonpicturecontrol.ui.LibraryScreen
import com.noahlin.nikonpicturecontrol.ui.NikonTheme
import com.noahlin.nikonpicturecontrol.ui.Np3GuideScreen
import com.noahlin.nikonpicturecontrol.ui.RandomScreen
import com.noahlin.nikonpicturecontrol.ui.SearchScreen
import com.noahlin.nikonpicturecontrol.ui.SettingsScreen

/** Top-level destinations shown in the bottom navigation bar. */
private enum class TopDest(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val icon: ImageVector,
) {
    Library("library", "Library", Icons.Filled.PhotoLibrary, Icons.Outlined.PhotoLibrary),
    Saved("saved", "Favorites", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
    Random("random", "Random", Icons.Filled.Shuffle, Icons.Outlined.Shuffle),
    Search("search", "Search", Icons.Filled.Search, Icons.Outlined.Search),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val store: RecipeStore = viewModel()
            NikonTheme(dynamicColor = store.dynamicColorEnabled) {
                Surface(Modifier) {
                    val nav = rememberNavController()
                    // First launch (no cached index) fetches from R2; the gate below covers the UI
                    // until the index + thumbnails are ready, so browsing never shows placeholders.
                    LaunchedEffect(Unit) { if (!store.hasLibrary) store.fetchLatest() }
                    Box(Modifier.fillMaxSize()) {
                    Scaffold(
                        bottomBar = { BottomBar(nav) },
                    ) { pad ->
                        NavHost(
                            nav,
                            startDestination = TopDest.Library.route,
                            // Pad for the bottom bar, then consume so inner screens' own
                            // Scaffolds don't re-apply the status-bar inset (avoids a double top gap).
                            modifier = Modifier.padding(pad).consumeWindowInsets(pad),
                        ) {
                            composable(TopDest.Library.route) { LibraryScreen(store, nav) }
                            composable(TopDest.Saved.route) { LibraryScreen(store, nav, savedTab = true) }
                            composable(TopDest.Random.route) { RandomScreen(store, nav) }
                            composable(TopDest.Search.route) { SearchScreen(store, nav) }
                            composable("settings") { SettingsScreen(store, nav) }

                            composable(
                                "browse/{kind}/{value}",
                                arguments = listOf(
                                    navArgument("kind") { type = NavType.StringType },
                                    navArgument("value") { type = NavType.StringType },
                                ),
                            ) {
                                BrowseScreen(
                                    it.arguments!!.getString("kind")!!,
                                    Uri.decode(it.arguments!!.getString("value")),
                                    store, nav,
                                )
                            }

                            composable(
                                "detail/{id}",
                                arguments = listOf(navArgument("id") { type = NavType.StringType }),
                            ) {
                                DetailScreen(it.arguments!!.getString("id")!!, store, nav)
                            }

                            composable(
                                "author/{name}",
                                arguments = listOf(navArgument("name") { type = NavType.StringType }),
                            ) {
                                val name = Uri.decode(it.arguments!!.getString("name"))
                                AuthorScreen(name, store, nav)
                            }

                            composable("create") { CreateRecipeScreen(null, store, nav) }
                            composable(
                                "edit/{id}",
                                arguments = listOf(navArgument("id") { type = NavType.StringType }),
                            ) {
                                CreateRecipeScreen(it.arguments!!.getString("id"), store, nav)
                            }

                            composable(
                                "terms/{kind}",
                                arguments = listOf(navArgument("kind") { type = NavType.StringType }),
                            ) {
                                EditTermsScreen(it.arguments!!.getString("kind")!!, store, nav)
                            }
                            composable("guide") { Np3GuideScreen(nav) }
                        }
                    }
                    if (!store.hasLibrary || store.isPreparingInitial) {
                        LibraryGate(store)
                    }
                    }
                }
            }
        }
    }
}

/**
 * Full-screen cover shown on first launch while the index + thumbnails download from R2, and on
 * failure (offline) with a retry — so the browse UI never appears empty or with placeholders.
 */
@Composable
private fun LibraryGate(store: RecipeStore) {
    Surface(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(40.dp),
            ) {
                if (store.isPreparingInitial) {
                    CircularProgressIndicator()
                    Text(
                        if (store.prefetchTotal > 0) "Loading previews… ${store.prefetchDone}/${store.prefetchTotal}"
                        else "Fetching recipes…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Icon(
                        Icons.Outlined.CloudOff, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp),
                    )
                    Text("Couldn't load recipes", style = MaterialTheme.typography.titleMedium)
                    store.fetchError?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center)
                    }
                    Button(onClick = { store.fetchLatest() }) { Text("Try Again") }
                }
            }
        }
    }
}

/** Bottom navigation bar, visible only while a top-level destination is showing. */
@Composable
private fun BottomBar(nav: NavHostController) {
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    if (currentRoute !in TopDest.entries.map { it.route }) return

    NavigationBar {
        TopDest.entries.forEach { dest ->
            val selected = currentRoute == dest.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    nav.navigate(dest.route) {
                        // One saved back stack entry per tab, à la Now in Android.
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(if (selected) dest.selectedIcon else dest.icon, contentDescription = dest.label) },
                label = { Text(dest.label) },
            )
        }
    }
}
