package com.codeforge.app.ui.screens

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codeforge.data.ContentItem
import com.codeforge.ui.viewmodel.RepoBrowserViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoBrowserScreen(
    owner: String,
    repo: String,
    externalBranch: String = "",
    onBranchChanged: (String) -> Unit = {},
    onOpenFile: (path: String) -> Unit,
    onNavigateSearch: () -> Unit = {},
    onNavigateBranches: () -> Unit = {},
    onNavigateCommits: () -> Unit = {},
    onNavigatePRs: () -> Unit = {}
) {
    val context = LocalContext.current
    val factory = ViewModelProvider.AndroidViewModelFactory(context.applicationContext as Application)
    val vm: RepoBrowserViewModel = viewModel(factory = factory)

    val items by vm.items.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val pathStack by vm.pathStack.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showCreateFileDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf<ContentItem?>(null) }
    var showDeleteDialog by remember { mutableStateOf<ContentItem?>(null) }
    var dialogInput by remember { mutableStateOf("") }
    var operationLoading by remember { mutableStateOf(false) }
    var showFabMenu by remember { mutableStateOf(false) }

    val currentPath = vm.currentPath
    val isRoot = pathStack.size <= 1

    // Sync external branch into VM
    LaunchedEffect(externalBranch) {
        vm.setBranch(externalBranch)
    }

    LaunchedEffect(owner, repo) {
        vm.loadCurrent(owner, repo)
    }

    // When external branch changes, reload from root
    LaunchedEffect(externalBranch) {
        vm.resetToRoot(owner, repo)
    }

    BackHandler(enabled = !isRoot) {
        vm.navigateUp(owner, repo)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("$owner/$repo", style = MaterialTheme.typography.titleSmall)
                        Text(currentPath ?: "/", style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    if (!isRoot) {
                        IconButton(onClick = { vm.navigateUp(owner, repo) }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Up")
                        }
                    }
                },
                actions = {
                    val branchLabel = externalBranch.ifEmpty { "default" }
                    TextButton(onClick = onNavigateBranches) {
                        Icon(Icons.Default.AccountTree, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(branchLabel, style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = onNavigateSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = onNavigateCommits) {
                        Icon(Icons.Default.History, contentDescription = "Commits")
                    }
                    IconButton(onClick = onNavigatePRs) {
                        Icon(Icons.Default.MergeType, contentDescription = "Pull Requests")
                    }
                }
            )
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(onClick = { showFabMenu = !showFabMenu }) {
                    Icon(if (showFabMenu) Icons.Default.Close else Icons.Default.Add, contentDescription = "Add")
                }
                DropdownMenu(expanded = showFabMenu, onDismissRequest = { showFabMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("New File") },
                        leadingIcon = { Icon(Icons.Default.InsertDriveFile, null) },
                        onClick = { showFabMenu = false; dialogInput = ""; showCreateFileDialog = true }
                    )
                    DropdownMenuItem(
                        text = { Text("New Folder") },
                        leadingIcon = { Icon(Icons.Default.CreateNewFolder, null) },
                        onClick = { showFabMenu = false; dialogInput = ""; showCreateFolderDialog = true }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                loading || operationLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                error != null -> Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Error: $error")
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { vm.loadCurrent(owner, repo) }) { Text("Retry") }
                }
                items.isEmpty() -> Text("Empty directory", modifier = Modifier.align(Alignment.Center))
                else -> ContentList(
                    items = items,
                    onOpenFile = onOpenFile,
                    onOpenDir = { dir -> vm.navigateInto(dir.path, owner, repo) },
                    onRename = { item -> dialogInput = item.name; showRenameDialog = item },
                    onDelete = { item -> showDeleteDialog = item }
                )
            }
        }
    }

    if (showCreateFileDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFileDialog = false },
            title = { Text("New File") },
            text = {
                OutlinedTextField(
                    value = dialogInput,
                    onValueChange = { dialogInput = it },
                    label = { Text("File name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = dialogInput.trim()
                    if (name.isNotEmpty()) {
                        showCreateFileDialog = false
                        operationLoading = true
                        val filePath = if (currentPath != null) "$currentPath/$name" else name
                        val branch = vm.getBranch()
                        scope.launch {
                            val resp = vm.getRepository().createFile(owner, repo, filePath, ByteArray(0), "Create $name", branch)
                            operationLoading = false
                            if (resp.isSuccessful) {
                                vm.loadCurrent(owner, repo)
                                snackbarHostState.showSnackbar("File created")
                                onOpenFile(filePath)
                            } else {
                                snackbarHostState.showSnackbar("Failed: ${resp.code()} ${resp.message()}")
                            }
                        }
                    }
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreateFileDialog = false }) { Text("Cancel") } }
        )
    }

    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("New Folder") },
            text = {
                OutlinedTextField(
                    value = dialogInput,
                    onValueChange = { dialogInput = it },
                    label = { Text("Folder name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = dialogInput.trim()
                    if (name.isNotEmpty()) {
                        showCreateFolderDialog = false
                        operationLoading = true
                        val folderPath = if (currentPath != null) "$currentPath/$name" else name
                        val branch = vm.getBranch()
                        scope.launch {
                            val resp = vm.getRepository().createFolder(owner, repo, folderPath, "Create folder $name", branch)
                            operationLoading = false
                            if (resp.isSuccessful) {
                                vm.loadCurrent(owner, repo)
                                snackbarHostState.showSnackbar("Folder created")
                            } else {
                                snackbarHostState.showSnackbar("Failed: ${resp.code()} ${resp.message()}")
                            }
                        }
                    }
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreateFolderDialog = false }) { Text("Cancel") } }
        )
    }

    showRenameDialog?.let { item ->
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text("Rename") },
            text = {
                OutlinedTextField(
                    value = dialogInput,
                    onValueChange = { dialogInput = it },
                    label = { Text("New name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val newName = dialogInput.trim()
                    if (newName.isNotEmpty() && newName != item.name) {
                        showRenameDialog = null
                        operationLoading = true
                        val parentPath = currentPath
                        val toPath = if (parentPath != null) "$parentPath/$newName" else newName
                        val branch = vm.getBranch()
                        scope.launch {
                            val (ok, err) = vm.getRepository().renameFile(owner, repo, item.path, toPath, "Rename ${item.name} to $newName", branch)
                            operationLoading = false
                            if (ok) {
                                vm.loadCurrent(owner, repo)
                                snackbarHostState.showSnackbar("Renamed successfully")
                            } else {
                                snackbarHostState.showSnackbar("Rename failed: $err")
                            }
                        }
                    } else {
                        showRenameDialog = null
                    }
                }) { Text("Rename") }
            },
            dismissButton = { TextButton(onClick = { showRenameDialog = null }) { Text("Cancel") } }
        )
    }

    showDeleteDialog?.let { item ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete") },
            text = { Text("Delete '${item.name}'? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = null
                    if (item.type == "file" && item.sha != null) {
                        operationLoading = true
                        val branch = vm.getBranch()
                        scope.launch {
                            val resp = vm.getRepository().deleteFile(owner, repo, item.path, item.sha, "Delete ${item.name}", branch)
                            operationLoading = false
                            if (resp.isSuccessful) {
                                vm.loadCurrent(owner, repo)
                                snackbarHostState.showSnackbar("Deleted")
                            } else {
                                snackbarHostState.showSnackbar("Failed: ${resp.code()} ${resp.message()}")
                            }
                        }
                    }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ContentList(
    items: List<ContentItem>,
    onOpenFile: (path: String) -> Unit,
    onOpenDir: (ContentItem) -> Unit,
    onRename: (ContentItem) -> Unit,
    onDelete: (ContentItem) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(items, key = { it.path }) { item ->
            ContentItemRow(item, onOpenFile, onOpenDir, onRename, onDelete)
            HorizontalDivider()
        }
    }
}

@Composable
private fun ContentItemRow(
    item: ContentItem,
    onOpenFile: (path: String) -> Unit,
    onOpenDir: (ContentItem) -> Unit,
    onRename: (ContentItem) -> Unit,
    onDelete: (ContentItem) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                when (item.type) {
                    "file" -> onOpenFile(item.path)
                    "dir" -> onOpenDir(item)
                }
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (item.type == "dir") Icons.Default.Folder else Icons.Default.InsertDriveFile,
            contentDescription = null,
            tint = if (item.type == "dir") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.width(12.dp))
        Text(
            item.name,
            modifier = Modifier.weight(1f),
            fontWeight = if (item.type == "dir") FontWeight.SemiBold else FontWeight.Normal
        )
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More")
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                if (item.type == "file") {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, null) },
                        onClick = { showMenu = false; onRename(item) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Default.Delete, null) },
                        onClick = { showMenu = false; onDelete(item) }
                    )
                }
            }
        }
    }
}
