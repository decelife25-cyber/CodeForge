package com.codeforge.app.ui

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.codeforge.app.ui.screens.*
import com.codeforge.auth.TokenStorage

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val startDestination = if (hasToken(context)) "repos" else "login"

    // Shared mutable state for branch context (shared across browser/search/branches screens)
    var sharedBranch by remember { mutableStateOf("") }

    NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
        composable("login") {
            LoginScreen(onLoginSuccess = {
                navController.navigate("repos") {
                    popUpTo("login") { inclusive = true }
                }
            })
        }

        composable("repos") {
            RepoListScreen(onOpenRepo = { owner, repo ->
                sharedBranch = ""
                navController.navigate("browser/$owner/$repo")
            })
        }

        composable(
            "browser/{owner}/{repo}",
            arguments = listOf(
                navArgument("owner") { type = NavType.StringType },
                navArgument("repo") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val owner = backStackEntry.arguments?.getString("owner") ?: ""
            val repo = backStackEntry.arguments?.getString("repo") ?: ""
            RepoBrowserScreen(
                owner = owner,
                repo = repo,
                externalBranch = sharedBranch,
                onBranchChanged = { sharedBranch = it },
                onOpenFile = { path ->
                    val encoded = java.net.URLEncoder.encode(path, "utf-8")
                    val branchParam = java.net.URLEncoder.encode(sharedBranch, "utf-8")
                    navController.navigate("editor/$owner/$repo/$encoded?branch=$branchParam")
                },
                onNavigateSearch = {
                    navController.navigate("search/$owner/$repo")
                },
                onNavigateBranches = {
                    navController.navigate("branches/$owner/$repo")
                },
                onNavigateCommits = {
                    val branchParam = java.net.URLEncoder.encode(sharedBranch, "utf-8")
                    navController.navigate("commits/$owner/$repo?branch=$branchParam")
                },
                onNavigatePRs = {
                    navController.navigate("pullrequests/$owner/$repo")
                }
            )
        }

        composable(
            "editor/{owner}/{repo}/{path}?branch={branch}",
            arguments = listOf(
                navArgument("owner") { type = NavType.StringType },
                navArgument("repo") { type = NavType.StringType },
                navArgument("path") { type = NavType.StringType },
                navArgument("branch") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val owner = backStackEntry.arguments?.getString("owner") ?: ""
            val repo = backStackEntry.arguments?.getString("repo") ?: ""
            val path = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("path") ?: "", "utf-8")
            val branch = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("branch") ?: "", "utf-8")
            EditorScreen(
                owner = owner,
                repo = repo,
                path = path,
                branch = branch,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            "search/{owner}/{repo}",
            arguments = listOf(
                navArgument("owner") { type = NavType.StringType },
                navArgument("repo") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val owner = backStackEntry.arguments?.getString("owner") ?: ""
            val repo = backStackEntry.arguments?.getString("repo") ?: ""
            SearchScreen(
                owner = owner,
                repo = repo,
                onOpenFile = { path ->
                    val encoded = java.net.URLEncoder.encode(path, "utf-8")
                    val branchParam = java.net.URLEncoder.encode(sharedBranch, "utf-8")
                    navController.navigate("editor/$owner/$repo/$encoded?branch=$branchParam")
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            "branches/{owner}/{repo}",
            arguments = listOf(
                navArgument("owner") { type = NavType.StringType },
                navArgument("repo") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val owner = backStackEntry.arguments?.getString("owner") ?: ""
            val repo = backStackEntry.arguments?.getString("repo") ?: ""
            BranchScreen(
                owner = owner,
                repo = repo,
                currentBranch = sharedBranch,
                onSelectBranch = { branch ->
                    sharedBranch = branch
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            "commits/{owner}/{repo}?branch={branch}",
            arguments = listOf(
                navArgument("owner") { type = NavType.StringType },
                navArgument("repo") { type = NavType.StringType },
                navArgument("branch") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val owner = backStackEntry.arguments?.getString("owner") ?: ""
            val repo = backStackEntry.arguments?.getString("repo") ?: ""
            val branchEncoded = backStackEntry.arguments?.getString("branch") ?: ""
            val branch = java.net.URLDecoder.decode(branchEncoded, "utf-8")
            CommitHistoryScreen(
                owner = owner,
                repo = repo,
                branch = branch,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            "pullrequests/{owner}/{repo}",
            arguments = listOf(
                navArgument("owner") { type = NavType.StringType },
                navArgument("repo") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val owner = backStackEntry.arguments?.getString("owner") ?: ""
            val repo = backStackEntry.arguments?.getString("repo") ?: ""
            PullRequestScreen(
                owner = owner,
                repo = repo,
                currentBranch = sharedBranch,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

private fun hasToken(context: Context): Boolean {
    return TokenStorage.getToken(context) != null
}
