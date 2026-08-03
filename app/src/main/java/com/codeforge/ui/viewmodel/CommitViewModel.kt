package com.codeforge.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codeforge.auth.TokenStorage
import com.codeforge.data.Commit
import com.codeforge.data.CommitWithFiles
import com.codeforge.data.NetworkModule
import com.codeforge.data.RepoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CommitViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RepoRepository(NetworkModule.createGitHubApi(TokenStorage.getToken(application), false))

    private val _commits = MutableStateFlow<List<Commit>>(emptyList())
    val commits: StateFlow<List<Commit>> = _commits
    private val _selectedCommit = MutableStateFlow<CommitWithFiles?>(null)
    val selectedCommit: StateFlow<CommitWithFiles?> = _selectedCommit
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadCommits(owner: String, repo: String, branch: String?) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val response = repository.listCommits(owner, repo, branch?.ifBlank { null })
                if (response.isSuccessful) _commits.value = response.body().orEmpty()
                else _error.value = "${response.code()} ${response.message()}"
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    fun loadCommitDetail(owner: String, repo: String, sha: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val response = repository.getCommit(owner, repo, sha)
                if (response.isSuccessful) _selectedCommit.value = response.body()
                else _error.value = "${response.code()} ${response.message()}"
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    fun clearSelection() {
        _selectedCommit.value = null
    }
}
