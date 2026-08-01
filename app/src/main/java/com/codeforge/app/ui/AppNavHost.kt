package com.codeforge.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.codeforge.app.ui.screens.EditorScreen
import com.codeforge.app.ui.screens.LoginScreen
import com.codeforge.app.ui.screens.RepoBrowserScreen
import com.codeforge.app.ui.screens.RepoListScreen

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "login", modifier = modifier) {
        composable("login") {
            LoginScreen(onLoginSuccess = {
                navController.navigate("repos")
            })
        }
        composable("repos") {
            RepoListScreen(onOpenRepo = { owner, repo ->
                navController.navigate("browser/${owner}/${repo}")
            })
        }
        composable("browser/{owner}/{repo}", arguments = listOf(
            navArgument("owner") { type = NavType.StringType },
            navArgument("repo") { type = NavType.StringType }
        )) { backStackEntry ->
            val owner = backStackEntry.arguments?.getString("owner") ?: ""
            val repo = backStackEntry.arguments?.getString("repo") ?: ""
            RepoBrowserScreen(owner = owner, repo = repo, onOpenFile = { path ->
                // encode path
                val encoded = java.net.URLEncoder.encode(path, "utf-8")
                navController.navigate("editor/${owner}/${repo}/${encoded}")
            })
        }
        composable("editor/{owner}/{repo}/{path}", arguments = listOf(
            navArgument("owner") { type = NavType.StringType },
            navArgument("repo") { type = NavType.StringType },
            navArgument("path") { type = NavType.StringType }
        )) { backStackEntry ->
            val owner = backStackEntry.arguments?.getString("owner") ?: ""
            val repo = backStackEntry.arguments?.getString("repo") ?: ""
            val path = backStackEntry.arguments?.getString("path") ?: ""
            val decoded = java.net.URLDecoder.decode(path, "utf-8")
            EditorScreen(owner = owner, repo = repo, path = decoded, onSaved = {
                navController.popBackStack()
            })
        }
    }
}
