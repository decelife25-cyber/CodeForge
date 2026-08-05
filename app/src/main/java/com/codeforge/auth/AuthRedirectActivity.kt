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
        android.util.Log.d("OAuth", "Received redirect with code: $code")
        if (code.isNullOrEmpty()) {
            android.util.Log.e("OAuth", "Code is null or empty, finishing activity")
            finish()
            return
        }

        // Exchange code for token
        lifecycleScope.launch {
            val verifier = TokenStorage.getCodeVerifier(applicationContext)
            android.util.Log.d("OAuth", "Retrieved code verifier: $verifier")
            if (verifier.isNullOrEmpty()) {
                android.util.Log.e("OAuth", "Verifier is null or empty, finishing activity")
                finish()
                return@launch
            }

            android.util.Log.d("OAuth", "Starting token exchange with code and verifier")
            val token = exchangeCodeForToken(code, verifier)
            android.util.Log.d("OAuth", "Token exchange returned token: $token")
            if (!token.isNullOrEmpty()) {
                android.util.Log.d("OAuth", "Saving token and navigating to MainActivity")
                TokenStorage.saveToken(applicationContext, token)
                // Launch MainActivity so that NavHost can pick up the token and navigate to repos
                val intent = Intent(this@AuthRedirectActivity, com.codeforge.app.MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            } else {
                android.util.Log.e("OAuth", "Token was null or empty after exchange")
            }
            finish()
        }
    }

    private suspend fun exchangeCodeForToken(code: String, verifier: String): String? = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("OAuth", "Preparing POST request to GitHub token endpoint")
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

            println("request parameters:")
            println("client_id: ${AuthConfig.CLIENT_ID}")
            println("code: $code")
            println("redirect_uri: ${AuthConfig.REDIRECT_URI}")
            println("grant_type: authorization_code")

            android.util.Log.d("OAuth", "Executing HTTP call")
            val resp = client.newCall(request).execute()
            val body = resp.body?.string()
            println("HTTP status: ${resp.code}")
            println("complete response body: $body")
            android.util.Log.d("OAuth", "Response HTTP Status: ${resp.code}")
            android.util.Log.d("OAuth", "Response Body: $body")

            if (!resp.isSuccessful || body.isNullOrEmpty()) {
                android.util.Log.e("OAuth", "Response unsuccessful or body empty. isSuccessful=${resp.isSuccessful}, bodyLength=${body?.length}")
                return@withContext null
            }

            android.util.Log.d("OAuth", "Parsing JSON response with Moshi")
            val moshi = Moshi.Builder().build()
            val adapter = moshi.adapter(Map::class.java)
            val map = adapter.fromJson(body)
            val token = map?.get("access_token") as? String
            android.util.Log.d("OAuth", "Parsed access_token is valid: ${token != null}")
            return@withContext token
        } catch (e: Exception) {
            android.util.Log.e("OAuth", "Exception during token exchange: ${e.message}", e)
            return@withContext null
        }
    }
}
