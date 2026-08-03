package com.codeforge.app.ui.screens

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codeforge.ui.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    owner: String,
    repo: String,
    onOpenFile: (path: String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val factory = ViewModelProvider.AndroidViewModelFactory(context.applicationContext as Application)
    val vm: SearchViewModel = viewModel(factory = factory)

    val results by vm.results.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()

    var query by remember { mutableStateOf("") }
    var searchMode by remember { mutableStateOf(0) } // 0=filename, 1=code

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search in $repo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            TabRow(selectedTabIndex = searchMode) {
                Tab(selected = searchMode == 0, onClick = { searchMode = 0 }, text = { Text("File Name") })
                Tab(selected = searchMode == 1, onClick = { searchMode = 1 }, text = { Text("Code Content") })
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    label = { Text(if (searchMode == 0) "Filename..." else "Search text...") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            if (query.isNotBlank()) {
                                if (searchMode == 0) vm.searchFiles(owner, repo, query)
                                else vm.searchText(owner, repo, query)
                            }
                        }) { Icon(Icons.Default.Search, null) }
                    }
                )
            }
            Spacer(Modifier.height(12.dp))
            when {
                loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                error != null -> Text("Error: $error", color = MaterialTheme.colorScheme.error)
                results.isEmpty() && query.isNotEmpty() -> Text("No results", modifier = Modifier.align(Alignment.CenterHorizontally))
                else -> LazyColumn {
                    items(results) { item ->
                        ListItem(
                            headlineContent = { Text(item.name) },
                            supportingContent = { Text(item.path, style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.clickable { onOpenFile(item.path) }
                        )
                        Divider()
                    }
                }
            }
        }
    }
}
