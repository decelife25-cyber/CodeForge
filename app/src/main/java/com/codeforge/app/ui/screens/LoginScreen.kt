package com.codeforge.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.codeforge.auth.AuthConfig
import com.codeforge.auth.AuthUtils
import com.codeforge.auth.TokenStorage
import java.util.UUID

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        if (TokenStorage.getToken(context) != null) {
            onLoginSuccess()
        }
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("CodeForge", style = MaterialTheme.typography.headlineMedium)
        Text("Login with GitHub using PKCE.", modifier = Modifier.padding(vertical = 16.dp))
        Button(onClick = { startOAuthFlow(context) }) { Text("Login with GitHub") }
    }
}

private fun startOAuthFlow(context: Context) {
    val verifier = AuthUtils.generateCodeVerifier()
    TokenStorage.saveCodeVerifier(context, verifier)
    val challenge = AuthUtils.codeChallengeFromVerifier(verifier)
    val state = UUID.randomUUID().toString()
    val url = Uri.Builder()
        .scheme("https")
        .authority("github.com")
        .appendPath("login")
        .appendPath("oauth")
        .appendPath("authorize")
        .appendQueryParameter("client_id", AuthConfig.CLIENT_ID)
        .appendQueryParameter("scope", AuthConfig.SCOPE)
        .appendQueryParameter("redirect_uri", AuthConfig.REDIRECT_URI)
        .appendQueryParameter("state", state)
        .appendQueryParameter("code_challenge", challenge)
        .appendQueryParameter("code_challenge_method", "S256")
        .build()
    context.startActivity(Intent(Intent.ACTION_VIEW, url))
}
