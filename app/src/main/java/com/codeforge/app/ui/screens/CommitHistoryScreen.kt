package com.codeforge.app.ui.screens

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codeforge.data.Commit
import com.codeforge.data.CommitWithFiles
import com.codeforge.ui.viewmodel.CommitViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommitHistoryScreen(
    owner: String,
    repo: String,
    branch: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val factory = ViewModelProvider.AndroidViewModelFactory(context.applicationContext as Application)
    val vm: CommitViewModel = viewModel(factory = factory)

    val commits by vm.commits.collectAsState()
    val selectedCommit by vm.selectedCommit.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()

    LaunchedEffect(owner, repo, branch) {
        vm.loadCommits(owner, repo, branch.takeIf { it.isNotEmpty() })
    }

    BackHandler(enabled = selectedCommit != null) {
        vm.clearSelection()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selectedCommit != null) "Commit Diff" else "Commits") },
                navigationIcon = {
                    IconButton(onClick = { if (selectedCommit != null) vm.clearSelection() else onBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                error != null -> Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Error: $error")
                    Button(onClick = { vm.loadCommits(owner, repo, branch.takeIf { it.isNotEmpty() }) }) { Text("Retry") }
                }
                selectedCommit != null -> CommitDiffView(selectedCommit!!)
                else -> CommitList(commits) { commit ->
                    vm.loadCommitDetail(owner, repo, commit.sha)
                }
            }
        }
    }
}

@Composable
private fun CommitList(commits: List<Commit>, onSelect: (Commit) -> Unit) {
    if (commits.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No commits found")
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(commits, key = { it.sha }) { commit ->
            ListItem(
                headlineContent = { Text(commit.commit.message.lines().first(), maxLines = 1) },
                supportingContent = {
                    Column {
                        Text(commit.sha.take(7), style = MaterialTheme.typography.bodySmall)
                        val author = commit.commit.author
                        if (author != null) {
                            Text("${author.name ?: ""} • ${author.date ?: ""}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                modifier = Modifier.clickable { onSelect(commit) }
            )
            Divider()
        }
    }
}

@Composable
private fun CommitDiffView(commit: CommitWithFiles) {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp)) {
        Text(commit.commit.message, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(commit.sha, style = MaterialTheme.typography.bodySmall)
        val author = commit.commit.author
        if (author != null) {
            Text("${author.name ?: ""} • ${author.date ?: ""}", style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(16.dp))
        Text("Changed files (${commit.files.size})", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        commit.files.forEach { file ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(file.filename, style = MaterialTheme.typography.labelLarge)
                    Text("${file.status ?: ""} +${file.additions} -${file.deletions}", style = MaterialTheme.typography.bodySmall)
                    if (!file.patch.isNullOrEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            file.patch,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
