package com.codeforge.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codeforge.auth.TokenStorage
import com.codeforge.data.ContentItem
import com.codeforge.data.NetworkModule
import com.codeforge.data.RepoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RepoBrowserViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RepoRepository(NetworkModule.createGitHubApi(TokenStorage.getToken(application), false))

    private val _items = MutableStateFlow<List<ContentItem>>(emptyList())
    val items: StateFlow<List<ContentItem>> = _items

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _pathStack = MutableStateFlow<List<String?>>(listOf(null))
    val pathStack: StateFlow<List<String?>> = _pathStack

    val currentBranch = MutableStateFlow("")

    private fun currentPath(): String? = _pathStack.value.lastOrNull()

    fun loadCurrent(owner: String, repo: String) {
        val path = currentPath()
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val branch = currentBranch.value.ifBlank { null }
                val response = if (path == null) repository.getContentsRoot(owner, repo, branch) else repository.getContents(owner, repo, path, branch)
                if (response.isSuccessful) {
                    _items.value = response.body().orEmpty().sortedWith(compareBy<ContentItem> { it.type != "dir" }.thenBy { it.name.lowercase() })
                } else {
                    handleError(response.code(), response.message())
                }
            } catch (e: Exception) {
                _items.value = emptyList()
                _error.value = e.message ?: "Unknown error"
            } finally {
                _loading.value = false
            }
        }
    }

    fun loadRoot(owner: String, repo: String) {
        _pathStack.value = listOf(null)
        loadCurrent(owner, repo)
    }

    fun navigateTo(owner: String, repo: String, path: String?) {
        _pathStack.value = _pathStack.value + path
        loadCurrent(owner, repo)
    }

    fun navigateBack(owner: String, repo: String): Boolean {
        if (_pathStack.value.size <= 1) return false
        _pathStack.value = _pathStack.value.dropLast(1)
        loadCurrent(owner, repo)
        return true
    }

    fun switchBranch(owner: String, repo: String, branch: String) {
        currentBranch.value = branch
        loadRoot(owner, repo)
    }

    fun createFile(owner: String, repo: String, path: String, message: String, content: String = "", onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val response = repository.createFile(owner, repo, path, content.toByteArray(), message, currentBranch.value.ifBlank { null })
                if (response.isSuccessful) {
                    loadCurrent(owner, repo)
                    onResult(true, null)
                } else {
                    onResult(false, "${response.code()} ${response.message()}")
                }
            } catch (e: Exception) {
                onResult(false, e.message)
            } finally {
                _loading.value = false
            }
        }
    }

    fun createFolder(owner: String, repo: String, path: String, message: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val response = repository.createFolder(owner, repo, path, message, currentBranch.value.ifBlank { null })
                if (response.isSuccessful) {
                    loadCurrent(owner, repo)
                    onResult(true, null)
                } else {
                    onResult(false, "${response.code()} ${response.message()}")
                }
            } catch (e: Exception) {
                onResult(false, e.message)
            } finally {
                _loading.value = false
            }
        }
    }

    fun deleteItem(owner: String, repo: String, item: ContentItem, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val branch = currentBranch.value.ifBlank { null }
                val result = if (item.type == "dir") {
                    repository.deleteFolderIfEmpty(owner, repo, item.path, "Delete folder ${item.name}", branch)
                } else {
                    val sha = item.sha ?: return@launch onResult(false, "Missing SHA")
                    val response = repository.deleteFile(owner, repo, item.path, sha, "Delete ${item.name}", branch)
                    if (response.isSuccessful) true to null else false to "${response.code()} ${response.message()}"
                }
                if (result.first) loadCurrent(owner, repo)
                onResult(result.first, result.second)
            } catch (e: Exception) {
                onResult(false, e.message)
            } finally {
                _loading.value = false
            }
        }
    }

    fun renameItem(owner: String, repo: String, item: ContentItem, newPath: String, onResult: (Boolean, String?) -> Unit) {
        if (item.type == "dir") {
            onResult(false, "Folder rename is not supported by GitHub contents API")
            return
        }
        viewModelScope.launch {
            _loading.value = true
            try {
                val result = repository.renameFile(owner, repo, item.path, newPath, "Rename ${item.name}", currentBranch.value.ifBlank { null })
                if (result.first) loadCurrent(owner, repo)
                onResult(result.first, result.second)
            } catch (e: Exception) {
                onResult(false, e.message)
            } finally {
                _loading.value = false
            }
        }
    }

    private fun handleError(code: Int, message: String) {
        _items.value = emptyList()
        if (code == 401) {
            TokenStorage.clearToken(getApplication())
            _error.value = "Authentication required. Please login again."
        } else {
            _error.value = "$code $message"
        }
    }
}
