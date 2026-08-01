package com.codeforge.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EditorScreen(owner: String, repo: String, path: String, onSaved: () -> Unit) {
    // Simple editor UI; saving functionality will be integrated later
    var content by remember { mutableStateOf("") }
    var commitMessage by remember { mutableStateOf("Edit from CodeForge") }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Text(text = "Editing: $path in $owner/$repo")
        OutlinedTextField(value = content, onValueChange = { content = it }, modifier = Modifier
            .weight(1f)
            .padding(top = 8.dp))
        OutlinedTextField(value = commitMessage, onValueChange = { commitMessage = it }, modifier = Modifier.padding(top = 8.dp))
        Button(onClick = { /* saving will be implemented later */ onSaved() }, modifier = Modifier.padding(top = 8.dp)) {
            Text("Save (placeholder)")
        }
    }
}
