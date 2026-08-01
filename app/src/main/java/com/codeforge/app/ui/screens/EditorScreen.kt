package com.codeforge.app.ui.screens

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codeforge.ui.viewmodel.EditorViewModel

@Composable
fun EditorScreen(owner: String, repo: String, path: String, onSaved: () -> Unit) {
    val context = LocalContext.current
    val factory = ViewModelProvider.AndroidViewModelFactory(context.applicationContext as Application)
    val vm: EditorViewModel = viewModel(factory = factory)

    val contentState = vm.content.collectAsState()
    val loadingState = vm.loading.collectAsState()

    var content by remember { mutableStateOf(contentState.value ?: "") }
    var commitMessage by remember { mutableStateOf("Edit from CodeForge") }
    var saving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(owner, repo, path) {
        vm.loadFile(owner, repo, path)
    }

    // Keep local content in sync when loaded
    LaunchedEffect(contentState.value) {
        contentState.value?.let { content = it }
    }

    Scaffold(topBar = { SmallTopAppBar(title = { Text("Editor: ${path}") }) }) { padding ->
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
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    label = { Text("File content") }
                )

                OutlinedTextField(
                    value = commitMessage,
                    onValueChange = { commitMessage = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    label = { Text("Commit message") }
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (errorMsg != null) {
                    Text(text = "Error: $errorMsg", color = MaterialTheme.colorScheme.error)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(onClick = {
                        saving = true
                        errorMsg = null
                        vm.saveFile(owner, repo, path, content, commitMessage) { success, error ->
                            saving = false
                            if (success) {
                                onSaved()
                            } else {
                                errorMsg = error
                            }
                        }
                    }, enabled = !saving) {
                        Text(if (saving) "Saving..." else "Save")
                    }
                }
            }
        }
    }
}
