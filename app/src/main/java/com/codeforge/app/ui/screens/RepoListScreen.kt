package com.codeforge.app.ui.screens

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codeforge.data.Repo
import com.codeforge.ui.viewmodel.RepoListViewModel

@Composable
fun RepoListScreen(onOpenRepo: (owner: String, repo: String) -> Unit) {
    val context = LocalContext.current
    val factory = ViewModelProvider.AndroidViewModelFactory(context.applicationContext as Application)
    val vm: RepoListViewModel = viewModel(factory = factory)

    val reposState = vm.repos.collectAsState()
    val loadingState = vm.loading.collectAsState()
    val errorState = vm.error.collectAsState()

    LaunchedEffect(Unit) {
        vm.loadRepos()
    }

    Scaffold(topBar = { SmallTopAppBar(title = { Text("Repositories") }) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                loadingState.value -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                errorState.value != null -> {
                    Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Error: ${errorState.value}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { vm.loadRepos() }) {
                            Text("Retry")
                        }
                    }
                }
                reposState.value.isEmpty() -> {
                    Text("No repositories found.", modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    RepoList(reposState.value, onOpenRepo)
                }
            }
        }
    }
}

@Composable
private fun RepoList(repos: List<Repo>, onOpenRepo: (owner: String, repo: String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(repos) { repo ->
            Card(modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .clickable { onOpenRepo(repo.owner.login, repo.name) }) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(repo.name, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(repo.full_name, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
