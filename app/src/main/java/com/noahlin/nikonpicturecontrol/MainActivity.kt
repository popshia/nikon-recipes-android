package com.noahlin.nikonpicturecontrol

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.noahlin.nikonpicturecontrol.ui.AuthorScreen
import com.noahlin.nikonpicturecontrol.ui.CreateRecipeScreen
import com.noahlin.nikonpicturecontrol.ui.DetailScreen
import com.noahlin.nikonpicturecontrol.ui.EditTermsScreen
import com.noahlin.nikonpicturecontrol.ui.LibraryScreen
import com.noahlin.nikonpicturecontrol.ui.Np3GuideScreen
import com.noahlin.nikonpicturecontrol.ui.NikonTheme
import com.noahlin.nikonpicturecontrol.ui.SettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NikonTheme {
                Surface(Modifier) {
                    val store: RecipeStore = viewModel()
                    val nav = rememberNavController()
                    NavHost(nav, startDestination = "library") {
                        composable("library") { LibraryScreen(store, nav) }

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

                        composable("settings") { SettingsScreen(nav) }
                        composable(
                            "terms/{kind}",
                            arguments = listOf(navArgument("kind") { type = NavType.StringType }),
                        ) {
                            EditTermsScreen(it.arguments!!.getString("kind")!!, store, nav)
                        }
                        composable("guide") { Np3GuideScreen(nav) }
                    }
                }
            }
        }
    }
}
