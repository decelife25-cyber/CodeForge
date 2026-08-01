package com.codeforge.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codeforge.data.NetworkModule
import com.codeforge.data.Repo
import com.codeforge.data.RepoRepository
import com.codeforge.auth.TokenStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RepoListViewModel(application: Application) : AndroidViewModel(application) {
    private val _repos = MutableStateFlow<List<Repo>>(emptyList())
    val repos: StateFlow<List<Repo>> = _repos

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val repository: RepoRepository

    init {
        val token = TokenStorage.getToken(application)
        val api = NetworkModule.createGitHubApi(token, enableLogging = false)
        repository = RepoRepository(api)
    }

    fun loadRepos() {
        viewModelScope.launch {
            _loading.value = true
            try {
                val resp = repository.listUserRepos()
                if (resp.isSuccessful) {
                    _repos.value = resp.body() ?: emptyList()
                } else {
                    _repos.value = emptyList()
                }
            } catch (e: Exception) {
                _repos.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }
}
