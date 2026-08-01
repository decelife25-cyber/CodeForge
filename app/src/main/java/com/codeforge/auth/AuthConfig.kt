package com.codeforge.auth

object AuthConfig {
    // Replace with your GitHub OAuth App client ID when ready.
    const val CLIENT_ID = "YOUR_CLIENT_ID"
    // Redirect URI registered in the OAuth app. Example: codeforge://oauth
    const val REDIRECT_URI = "codeforge://oauth"
    const val SCOPE = "repo"
}
