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
fun RepoListScreen(onOpenRepo: (owner: String, repo: String) -> Unit) {
    // Placeholder static list for now; will be replaced with real data later
    val repos = listOf("demo-repo-1", "demo-repo-2")
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(repos) { name ->
            Card(modifier = Modifier
                .padding(8.dp)
                .clickable { onOpenRepo("your-username", name) }
            ) {
                Text(text = name, modifier = Modifier.padding(16.dp))
            }
        }
    }
}
