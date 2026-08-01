package com.codeforge.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codeforge.data.ContentItem
import com.codeforge.data.NetworkModule
import com.codeforge.data.RepoRepository
import com.codeforge.auth.TokenStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RepoBrowserViewModel(application: Application) : AndroidViewModel(application) {
    private val _items = MutableStateFlow<List<ContentItem>>(emptyList())
    val items: StateFlow<List<ContentItem>> = _items

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val repository: RepoRepository

    init {
        val token = TokenStorage.getToken(application)
        val api = NetworkModule.createGitHubApi(token, enableLogging = false)
        repository = RepoRepository(api)
    }

    fun loadRoot(owner: String, repo: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val resp = repository.getContentsRoot(owner, repo)
                if (resp.isSuccessful) {
                    _items.value = resp.body() ?: emptyList()
                } else {
                    _items.value = emptyList()
                }
            } catch (e: Exception) {
                _items.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }

    fun loadPath(owner: String, repo: String, path: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val resp = repository.getContents(owner, repo, path)
                if (resp.isSuccessful) {
                    _items.value = resp.body() ?: emptyList()
                } else {
                    _items.value = emptyList()
                }
            } catch (e: Exception) {
                _items.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }
}
