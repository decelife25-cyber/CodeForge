package com.codeforge.app.ui.screens

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codeforge.ui.viewmodel.EditorViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(owner: String, repo: String, path: String, branch: String = "", onSaved: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val factory = ViewModelProvider.AndroidViewModelFactory(context.applicationContext as Application)
    val vm: EditorViewModel = viewModel(factory = factory)
    val contentState by vm.content.collectAsState()
    val loadingState by vm.loading.collectAsState()

    var content by remember { mutableStateOf("") }
    var originalContent by remember { mutableStateOf<String?>(null) }
    var commitMessage by remember { mutableStateOf("Edit from CodeForge") }
    var saving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showConfirmExit by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(owner, repo, path, branch) { vm.loadFile(owner, repo, path, branch.ifBlank { null }) }
    LaunchedEffect(contentState) {
        contentState?.let {
            content = it
            if (originalContent == null) originalContent = it
        }
    }

    val hasChanges = (originalContent ?: "") != content

    BackHandler { if (hasChanges) showConfirmExit = true else onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(path, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (branch.isNotBlank()) Text(branch, style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (hasChanges) showConfirmExit = true else onBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (loadingState) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    val verticalState = rememberScrollState()
                    val horizontalState = rememberScrollState()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .verticalScroll(verticalState)
                            .horizontalScroll(horizontalState)
                            .padding(8.dp)
                    ) {
                        BasicTextField(
                            value = content,
                            onValueChange = { content = it },
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (hasChanges) "Unsaved changes" else "No changes", modifier = Modifier.weight(1f))
                        OutlinedTextField(value = commitMessage, onValueChange = { commitMessage = it }, modifier = Modifier.weight(2f), label = { Text("Commit message") })
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(onClick = { if (hasChanges) showConfirmExit = true else onBack() }) { Text("Cancel") }
                        Spacer(modifier = Modifier.padding(4.dp))
                        Button(onClick = {
                            saving = true
                            vm.saveFile(owner, repo, path, content, commitMessage, branch.ifBlank { null }) { success, error ->
                                saving = false
                                if (success) {
                                    originalContent = content
                                    scope.launch { snackbarHostState.showSnackbar("Saved successfully") }
                                    onSaved()
                                } else {
                                    errorMsg = error
                                    scope.launch { snackbarHostState.showSnackbar("Save failed: ${error ?: "unknown"}") }
                                }
                            }
                        }, enabled = hasChanges && !saving) {
                            Text(if (saving) "Saving..." else "Save")
                        }
                    }
                    errorMsg?.let { Text("Error: $it", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
                }
            }
        }
    }

    if (showConfirmExit) {
        AlertDialog(
            onDismissRequest = { showConfirmExit = false },
            title = { Text("Discard changes?") },
            text = { Text("You have unsaved changes. Do you want to discard them and exit?") },
            confirmButton = { TextButton(onClick = { showConfirmExit = false; onBack() }) { Text("Discard") } },
            dismissButton = { TextButton(onClick = { showConfirmExit = false }) { Text("Cancel") } }
        )
    }
}
