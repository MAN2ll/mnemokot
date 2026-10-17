package ru.man2ll.mnemokot.presentation.random

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ru.man2ll.mnemokot.core.crypto.domain.generator.SecureRandomGenerator
import ru.man2ll.mnemokot.core.crypto.domain.model.RandomGeneratorConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class RandomGeneratorViewModel @Inject constructor(
    private val randomGenerator: SecureRandomGenerator
) : ViewModel() {

    private val _config = MutableStateFlow(RandomGeneratorConfig())
    private val _isHelpVisible = MutableStateFlow(false)
    private val _refreshTrigger = MutableStateFlow(0)

    val uiState: StateFlow<RandomGeneratorUiState> = combine(
        _config,
        _isHelpVisible,
        _refreshTrigger
    ) { config, isHelpVisible, _ ->
        val result = randomGenerator.generate(config)
        RandomGeneratorUiState(
            config = config,
            result = result,
            isHelpVisible = isHelpVisible
        )
    }.flowOn(Dispatchers.Default)
     .stateIn(
         scope = viewModelScope,
         started = SharingStarted.WhileSubscribed(5000),
         initialValue = RandomGeneratorUiState()
     )

    fun onLengthChanged(length: Int) {
        _config.value = _config.value.copy(length = length)
    }

    fun onIncludeUppercaseChanged(include: Boolean) {
        _config.value = _config.value.copy(includeUppercase = include)
    }

    fun onIncludeLowercaseChanged(include: Boolean) {
        _config.value = _config.value.copy(includeLowercase = include)
    }

    fun onIncludeDigitsChanged(include: Boolean) {
        _config.value = _config.value.copy(includeDigits = include)
    }

    fun onIncludeSymbolsChanged(include: Boolean) {
        _config.value = _config.value.copy(includeSymbols = include)
    }

    fun onExcludeSimilarChanged(exclude: Boolean) {
        _config.value = _config.value.copy(excludeSimilar = exclude)
    }

    fun toggleHelpVisibility() {
        _isHelpVisible.value = !_isHelpVisible.value
    }

    fun regeneratePassword() {
        _refreshTrigger.value = _refreshTrigger.value + 1
    }
}
