package com.codeforge.app.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.codeforge.app.ui.screens.EditorScreen
import com.codeforge.app.ui.screens.LoginScreen
import com.codeforge.app.ui.screens.RepoBrowserScreen
import com.codeforge.app.ui.screens.RepoListScreen
import com.codeforge.auth.TokenStorage

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val startDestination = if (hasToken(context)) "repos" else "login"

    NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
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

private fun hasToken(context: Context): Boolean {
    return TokenStorage.getToken(context) != null
}
