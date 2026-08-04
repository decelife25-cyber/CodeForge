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

    fun loadPullRequests(owner: String, repo: String, stateFilter: String = "open") {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                // The API call listPullRequests in RepoRepository may not take stateFilter.
                // Depending on its implementation, we can just fetch and filter locally, or pass it if possible.
                // Assuming it takes only owner and repo for now based on current interface.
                val response = repository.listPullRequests(owner, repo)
                if (response.isSuccessful) {
                    val allPrs = response.body().orEmpty()
                    _pullRequests.value = if (stateFilter == "all") allPrs else allPrs.filter { it.state == stateFilter }
                } else _error.value = "${response.code()} ${response.message()}"
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
