package com.codeforge.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codeforge.auth.TokenStorage
import com.codeforge.data.NetworkModule
import com.codeforge.data.PullRequest
import com.codeforge.data.RepoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PullRequestViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RepoRepository(NetworkModule.createGitHubApi(TokenStorage.getToken(application), false))

    private val _pullRequests = MutableStateFlow<List<PullRequest>>(emptyList())
    val pullRequests: StateFlow<List<PullRequest>> = _pullRequests
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadPullRequests(owner: String, repo: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val response = repository.listPullRequests(owner, repo)
                if (response.isSuccessful) _pullRequests.value = response.body().orEmpty()
                else _error.value = "${response.code()} ${response.message()}"
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    fun createPullRequest(owner: String, repo: String, title: String, body: String, head: String, base: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val response = repository.createPullRequest(owner, repo, title, body, head, base)
                if (response.isSuccessful) {
                    loadPullRequests(owner, repo)
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
}
