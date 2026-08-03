package com.codeforge.app.ui.screens

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codeforge.data.Repo
import com.codeforge.ui.viewmodel.RepoListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoListScreen(onOpenRepo: (owner: String, repo: String) -> Unit) {
    val context = LocalContext.current
    val factory = ViewModelProvider.AndroidViewModelFactory(context.applicationContext as Application)
    val vm: RepoListViewModel = viewModel(factory = factory)
    val repos by vm.repos.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()

    LaunchedEffect(Unit) { vm.loadRepos() }

    Scaffold(topBar = { TopAppBar(title = { Text("Repositories") }) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                error != null -> Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Error: $error")
                    Button(onClick = { vm.loadRepos() }) { Text("Retry") }
                }
                repos.isEmpty() -> Text("No repositories found.", modifier = Modifier.align(Alignment.Center))
                else -> RepoList(repos, onOpenRepo)
            }
        }
    }
}

@Composable
private fun RepoList(repos: List<Repo>, onOpenRepo: (owner: String, repo: String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(repos, key = { it.id }) { repo ->
            androidx.compose.material3.Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).clickable { onOpenRepo(repo.owner.login, repo.name) }) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(repo.name, style = MaterialTheme.typography.titleMedium)
                    Text(repo.full_name, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
