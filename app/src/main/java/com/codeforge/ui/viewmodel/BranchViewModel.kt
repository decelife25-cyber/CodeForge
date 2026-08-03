package com.codeforge.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codeforge.auth.TokenStorage
import com.codeforge.data.Branch
import com.codeforge.data.NetworkModule
import com.codeforge.data.RepoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BranchViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RepoRepository(NetworkModule.createGitHubApi(TokenStorage.getToken(application), false))

    private val _branches = MutableStateFlow<List<Branch>>(emptyList())
    val branches: StateFlow<List<Branch>> = _branches
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadBranches(owner: String, repo: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val response = repository.listBranches(owner, repo)
                if (response.isSuccessful) _branches.value = response.body().orEmpty()
                else _error.value = "${response.code()} ${response.message()}"
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    fun createBranch(owner: String, repo: String, newBranch: String, fromBranch: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val refResponse = repository.getBranchRef(owner, repo, fromBranch)
                if (!refResponse.isSuccessful) {
                    onResult(false, "${refResponse.code()} ${refResponse.message()}")
                    return@launch
                }
                val sha = refResponse.body()?.objectInfo?.sha ?: run {
                    onResult(false, "Missing base SHA")
                    return@launch
                }
                val createResponse = repository.createBranch(owner, repo, newBranch, sha)
                if (createResponse.isSuccessful) {
                    loadBranches(owner, repo)
                    onResult(true, null)
                } else {
                    onResult(false, "${createResponse.code()} ${createResponse.message()}")
                }
            } catch (e: Exception) {
                onResult(false, e.message)
            } finally {
                _loading.value = false
            }
        }
    }
}
