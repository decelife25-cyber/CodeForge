package com.codeforge.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

class AuthRedirectActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data: Uri? = intent?.data
        if (data == null) {
            finish()
            return
        }

        val code = data.getQueryParameter("code")
        if (code.isNullOrEmpty()) {
            finish()
            return
        }

        // Exchange code for token
        lifecycleScope.launch {
            val verifier = TokenStorage.getCodeVerifier(applicationContext)
            if (verifier.isNullOrEmpty()) {
                finish()
                return@launch
            }

            val token = exchangeCodeForToken(code, verifier)
            if (!token.isNullOrEmpty()) {
                TokenStorage.saveToken(applicationContext, token)
                // Launch MainActivity so that NavHost can pick up the token and navigate to repos
                val intent = Intent(this@AuthRedirectActivity, com.codeforge.app.MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
            finish()
        }
    }

    private suspend fun exchangeCodeForToken(code: String, verifier: String): String? = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val form = FormBody.Builder()
                .add("client_id", AuthConfig.CLIENT_ID)
                .add("code", code)
                .add("code_verifier", verifier)
                .add("redirect_uri", AuthConfig.REDIRECT_URI)
                .add("grant_type", "authorization_code")
                .build()

            val request = Request.Builder()
                .url("https://github.com/login/oauth/access_token")
                .post(form)
                .header("Accept", "application/json")
                .build()

            val resp = client.newCall(request).execute()
            val body = resp.body?.string()
            if (!resp.isSuccessful || body.isNullOrEmpty()) return@withContext null

            val moshi = Moshi.Builder().build()
            val adapter = moshi.adapter(Map::class.java)
            val map = adapter.fromJson(body)
            val token = map?.get("access_token") as? String
            return@withContext token
        } catch (e: Exception) {
            return@withContext null
        }
    }
}
