package com.codeforge.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RepoBrowserScreen(owner: String, repo: String, onOpenFile: (path: String) -> Unit) {
    // Placeholder static content list
    val items = listOf("README.md", "src/Main.kt", "app/build.gradle.kts")
    Column(modifier = Modifier.fillMaxSize()) {
        Text(text = "Repository: $owner/$repo", modifier = Modifier.padding(16.dp))
        LazyColumn {
            items(items) { item ->
                Card(modifier = Modifier
                    .padding(8.dp)
                    .clickable { onOpenFile(item) }
                ) {
                    Text(text = item, modifier = Modifier.padding(12.dp))
                }
            }
        }
    }
}
