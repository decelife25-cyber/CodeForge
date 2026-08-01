package com.codeforge.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codeforge.data.CreateUpdateFileRequest
import com.codeforge.data.ContentItem
import com.codeforge.data.NetworkModule
import com.codeforge.data.RepoRepository
import com.codeforge.auth.TokenStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.util.Base64

class EditorViewModel(application: Application) : AndroidViewModel(application) {
    private val _content = MutableStateFlow<String?>(null)
    val content: StateFlow<String?> = _content

    private val _sha = MutableStateFlow<String?>(null)
    val sha: StateFlow<String?> = _sha

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val repository: RepoRepository

    init {
        val token = TokenStorage.getToken(application)
        val api = NetworkModule.createGitHubApi(token, enableLogging = false)
        repository = RepoRepository(api)
    }

    fun loadFile(owner: String, repo: String, path: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val resp = repository.getFile(owner, repo, path, null)
                if (resp.isSuccessful) {
                    val item: ContentItem? = resp.body()
                    if (item != null && item.content != null) {
                        val bytes = Base64.decode(item.content, Base64.DEFAULT)
                        _content.value = String(bytes)
                        _sha.value = item.sha
                    } else {
                        _content.value = null
                        _sha.value = null
                    }
                } else {
                    _content.value = null
                    _sha.value = null
                }
            } catch (e: Exception) {
                _content.value = null
                _sha.value = null
            } finally {
                _loading.value = false
            }
        }
    }

    fun saveFile(owner: String, repo: String, path: String, newContent: String, message: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val encoded = Base64.encodeToString(newContent.toByteArray(), Base64.NO_WRAP)
                val req = CreateUpdateFileRequest(message = message, content = encoded, sha = _sha.value)
                val resp = repository.createOrUpdateFile(owner, repo, path, req)
                if (resp.isSuccessful) {
                    onResult(true, null)
                } else {
                    onResult(false, "${resp.code()} ${resp.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                onResult(false, e.message)
            } finally {
                _loading.value = false
            }
        }
    }
}
