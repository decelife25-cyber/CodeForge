package com.cartadigital.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.cartadigital.app.data.Config
import com.cartadigital.app.data.ConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeViewModel(
    private val configRepository: ConfigRepository = ConfigRepository()
) : ViewModel() {

    private val _configState = MutableStateFlow<Config?>(null)
    val configState: StateFlow<Config?> = _configState.asStateFlow()

    init {
        loadConfig()
    }

    private fun loadConfig() {
        _configState.value = configRepository.getConfig()
    }
}
