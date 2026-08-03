package com.codeforge.app.ui.screens

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codeforge.ui.viewmodel.BranchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchScreen(
    owner: String,
    repo: String,
    currentBranch: String,
    onSelectBranch: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val factory = ViewModelProvider.AndroidViewModelFactory(context.applicationContext as Application)
    val vm: BranchViewModel = viewModel(factory = factory)

    val branches by vm.branches.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newBranchName by remember { mutableStateOf("") }
    var fromBranch by remember { mutableStateOf(currentBranch) }

    LaunchedEffect(owner, repo) {
        vm.loadBranches(owner, repo)
        if (fromBranch.isEmpty() && branches.isNotEmpty()) {
            fromBranch = branches.first().name
        }
    }

    LaunchedEffect(branches) {
        if (fromBranch.isEmpty() && branches.isNotEmpty()) {
            fromBranch = branches.first().name
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Branches") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { newBranchName = ""; showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Create Branch")
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
                    Button(onClick = { vm.loadBranches(owner, repo) }) { Text("Retry") }
                }
                else -> LazyColumn {
                    items(branches) { branch ->
                        val isSelected = branch.name == currentBranch
                        ListItem(
                            headlineContent = { Text(branch.name) },
                            supportingContent = { Text(branch.commit.sha.take(7), style = MaterialTheme.typography.bodySmall) },
                            trailingContent = {
                                if (isSelected) Icon(Icons.Default.Check, contentDescription = "Active", tint = MaterialTheme.colorScheme.primary)
                            },
                            modifier = Modifier.clickable { onSelectBranch(branch.name) }
                        )
                        Divider()
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Branch") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newBranchName,
                        onValueChange = { newBranchName = it },
                        label = { Text("Branch name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("From:", style = MaterialTheme.typography.labelMedium)
                    if (branches.isNotEmpty()) {
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                            OutlinedTextField(
                                value = fromBranch,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Base branch") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                branches.forEach { b ->
                                    DropdownMenuItem(
                                        text = { Text(b.name) },
                                        onClick = { fromBranch = b.name; expanded = false }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = newBranchName.trim()
                    if (name.isNotEmpty() && fromBranch.isNotEmpty()) {
                        showCreateDialog = false
                        vm.createBranch(owner, repo, name, fromBranch) { ok, err ->
                            if (ok) {
                                // auto select the new branch
                                onSelectBranch(name)
                            } else {
                                // show error via snackbar - but we can't easily call scope.launch from here
                                // the error will show in VM
                            }
                        }
                    }
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") } }
        )
    }
}
