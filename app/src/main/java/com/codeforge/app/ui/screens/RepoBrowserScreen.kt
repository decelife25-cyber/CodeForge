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
import com.codeforge.data.ContentItem
import com.codeforge.ui.viewmodel.RepoBrowserViewModel

@Composable
fun RepoBrowserScreen(owner: String, repo: String, onOpenFile: (path: String) -> Unit) {
    val context = LocalContext.current
    val factory = ViewModelProvider.AndroidViewModelFactory(context.applicationContext as Application)
    val vm: RepoBrowserViewModel = viewModel(factory = factory)

    val itemsState = vm.items.collectAsState()
    val loadingState = vm.loading.collectAsState()
    val errorState = vm.error.collectAsState()

    LaunchedEffect(owner, repo) {
        vm.loadRoot(owner, repo)
    }

    Scaffold(topBar = { SmallTopAppBar(title = { Text("Browser: $owner/$repo") }) }) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            when {
                loadingState.value -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                errorState.value != null -> {
                    Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Error: ${errorState.value}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { vm.loadRoot(owner, repo) }) {
                            Text("Retry")
                        }
                    }
                }
                itemsState.value.isEmpty() -> {
                    Text("No items in repository.", modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    RepoContentList(itemsState.value, onOpenFile)
                }
            }
        }
    }
}

@Composable
private fun RepoContentList(items: List<ContentItem>, onOpenFile: (path: String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(items) { item ->
            Card(modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .clickable(enabled = item.type == "file") { if (item.type == "file") onOpenFile(item.path) }) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(item.path, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(item.type, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
