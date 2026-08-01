package com.codeforge.app.ui.screens

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codeforge.ui.viewmodel.EditorViewModel
import kotlinx.coroutines.launch

@Composable
fun EditorScreen(owner: String, repo: String, path: String, onSaved: () -> Unit) {
    val context = LocalContext.current
    val factory = ViewModelProvider.AndroidViewModelFactory(context.applicationContext as Application)
    val vm: EditorViewModel = viewModel(factory = factory)

    val contentState = vm.content.collectAsState()
    val loadingState = vm.loading.collectAsState()

    var content by remember { mutableStateOf(contentState.value ?: "") }
    var originalContent by remember { mutableStateOf<String?>(null) }
    var commitMessage by remember { mutableStateOf("Edit from CodeForge") }
    var saving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showConfirmExit by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(owner, repo, path) {
        vm.loadFile(owner, repo, path)
    }

    LaunchedEffect(contentState.value) {
        if (contentState.value != null) {
            content = contentState.value ?: ""
            if (originalContent == null) originalContent = content
        }
    }

    val hasChanges = remember(content, originalContent) { (originalContent ?: "") != content }

    BackHandler {
        if (hasChanges) showConfirmExit = true else onSaved()
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(title = { Text(path) })
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {

            if (loadingState.value) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                return@Box
            }

            Column(modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)) {

                // Editor area with both vertical and horizontal scrolling
                val verticalState = rememberScrollState()
                val horizontalState = rememberScrollState()

                Box(modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .verticalScroll(verticalState)
                    .horizontalScroll(horizontalState)
                    .padding(8.dp)) {

                    BasicTextField(
                        value = content,
                        onValueChange = { content = it },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    if (hasChanges) {
                        Text("Unsaved changes", color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.CenterVertically))
                    } else {
                        Text("No changes", modifier = Modifier.align(Alignment.CenterVertically))
                    }

                    OutlinedTextField(
                        value = commitMessage,
                        onValueChange = { commitMessage = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(onClick = {
                        showConfirmExit = true
                    }, colors = ButtonDefaults.outlinedButtonColors(), modifier = Modifier.padding(end = 8.dp)) {
                        Text("Cancel")
                    }

                    Button(onClick = {
                        saving = true
                        errorMsg = null
                        vm.saveFile(owner, repo, path, content, commitMessage) { success, error ->
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
                        if (saving) Text("Saving...") else Text("Save")
                    }
                }

                if (errorMsg != null) {
                    Text(text = "Error: $errorMsg", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                }
            }

            if (showConfirmExit) {
                AlertDialog(
                    onDismissRequest = { showConfirmExit = false },
                    title = { Text("Discard changes?") },
                    text = { Text("You have unsaved changes. Do you want to discard them and exit?") },
                    confirmButton = {
                        TextButton(onClick = {
                            showConfirmExit = false
                            onSaved()
                        }) { Text("Discard") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConfirmExit = false }) { Text("Cancel") }
                    }
                )
            }
        }
    }
}
