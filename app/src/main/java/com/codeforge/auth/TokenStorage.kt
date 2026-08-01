package com.codeforge.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object TokenStorage {
    private const val PREFS_NAME = "codeforge_prefs"
    private const val KEY_TOKEN = "github_token"
    private const val KEY_CODE_VERIFIER = "pkce_code_verifier"

    private fun getEncryptedPrefs(context: Context) =
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

    fun saveToken(context: Context, token: String) {
        val prefs = getEncryptedPrefs(context)
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(context: Context): String? {
        val prefs = getEncryptedPrefs(context)
        return prefs.getString(KEY_TOKEN, null)
    }

    fun clearToken(context: Context) {
        val prefs = getEncryptedPrefs(context)
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    fun saveCodeVerifier(context: Context, verifier: String) {
        val prefs = getEncryptedPrefs(context)
        prefs.edit().putString(KEY_CODE_VERIFIER, verifier).apply()
    }

    fun getCodeVerifier(context: Context): String? {
        val prefs = getEncryptedPrefs(context)
        return prefs.getString(KEY_CODE_VERIFIER, null)
    }

    fun clearCodeVerifier(context: Context) {
        val prefs = getEncryptedPrefs(context)
        prefs.edit().remove(KEY_CODE_VERIFIER).apply()
    }
}
