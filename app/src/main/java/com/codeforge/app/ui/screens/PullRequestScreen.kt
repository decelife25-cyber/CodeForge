package com.codeforge.app.ui.screens

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codeforge.data.PullRequest
import com.codeforge.ui.viewmodel.PullRequestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullRequestScreen(
    owner: String,
    repo: String,
    currentBranch: String,
    defaultBranch: String = "main",
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val factory = ViewModelProvider.AndroidViewModelFactory(context.applicationContext as Application)
    val vm: PullRequestViewModel = viewModel(factory = factory)

    val pullRequests by vm.pullRequests.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showCreateDialog by remember { mutableStateOf(false) }
    var prTitle by remember { mutableStateOf("") }
    var prBody by remember { mutableStateOf("") }
    var prHead by remember { mutableStateOf(currentBranch) }
    var prBase by remember { mutableStateOf(defaultBranch) }
    var stateFilter by remember { mutableStateOf("open") }

    LaunchedEffect(owner, repo, stateFilter) {
        vm.loadPullRequests(owner, repo, stateFilter)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Pull Requests") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        prTitle = ""
                        prBody = ""
                        prHead = currentBranch.ifEmpty { "" }
                        showCreateDialog = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Create PR")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = if (stateFilter == "open") 0 else 1) {
                Tab(selected = stateFilter == "open", onClick = { stateFilter = "open" }, text = { Text("Open") })
                Tab(selected = stateFilter == "closed", onClick = { stateFilter = "closed" }, text = { Text("Closed") })
            }
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    error != null -> Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Error: $error")
                        Button(onClick = { vm.loadPullRequests(owner, repo, stateFilter) }) { Text("Retry") }
                    }
                    pullRequests.isEmpty() -> Text("No pull requests", modifier = Modifier.align(Alignment.Center))
                    else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(pullRequests) { pr ->
                            PrItem(pr) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(pr.html_url))
                                context.startActivity(intent)
                            }
                            Divider()
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Pull Request") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = prTitle, onValueChange = { prTitle = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = prBody, onValueChange = { prBody = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                    OutlinedTextField(value = prHead, onValueChange = { prHead = it }, label = { Text("Head branch (from)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = prBase, onValueChange = { prBase = it }, label = { Text("Base branch (into)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (prTitle.isNotBlank() && prHead.isNotBlank() && prBase.isNotBlank()) {
                        showCreateDialog = false
                        vm.createPullRequest(owner, repo, prTitle, prBody, prHead, prBase) { ok, err ->
                            if (!ok) {
                                // show via snackbar - needs scope
                            }
                        }
                    }
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun PrItem(pr: PullRequest, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text("#${pr.number} ${pr.title}") },
        supportingContent = {
            Column {
                Text("${pr.head.ref} → ${pr.base.ref}", style = MaterialTheme.typography.bodySmall)
                Text("by ${pr.user.login} • ${pr.state}", style = MaterialTheme.typography.bodySmall)
            }
        },
        modifier = Modifier.clickable { onClick() }
    )
}
