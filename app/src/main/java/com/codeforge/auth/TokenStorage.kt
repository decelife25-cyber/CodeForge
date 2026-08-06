package com.codeforge.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object TokenStorage {
    private const val PREFS_NAME = "codeforge_prefs"
    private const val KEY_TOKEN = "github_token"
    private const val KEY_CODE_VERIFIER = "pkce_code_verifier"

    @Volatile
    private var prefsInstance: SharedPreferences? = null

    private fun getEncryptedPrefs(context: Context): SharedPreferences {
        return prefsInstance ?: synchronized(this) {
            prefsInstance ?: EncryptedSharedPreferences.create(
                context.applicationContext,
                PREFS_NAME,
                MasterKey.Builder(context.applicationContext).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ).also { prefsInstance = it }
        }
    }

    fun saveToken(context: Context, token: String) {
        val prefs = getEncryptedPrefs(context)
        prefs.edit().putString(KEY_TOKEN, token).commit()
    }

    fun getToken(context: Context): String? {
        val prefs = getEncryptedPrefs(context)
        return prefs.getString(KEY_TOKEN, null)
    }

    fun clearToken(context: Context) {
        val prefs = getEncryptedPrefs(context)
        prefs.edit().remove(KEY_TOKEN).commit()
    }

    fun saveCodeVerifier(context: Context, verifier: String) {
        val prefs = getEncryptedPrefs(context)
        prefs.edit().putString(KEY_CODE_VERIFIER, verifier).commit()
    }

    fun getCodeVerifier(context: Context): String? {
        val prefs = getEncryptedPrefs(context)
        return prefs.getString(KEY_CODE_VERIFIER, null)
    }

    fun clearCodeVerifier(context: Context) {
        val prefs = getEncryptedPrefs(context)
        prefs.edit().remove(KEY_CODE_VERIFIER).commit()
    }
}
