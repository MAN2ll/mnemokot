package com.mnemokot.app.presentation.mnemonic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mnemokot.core.crypto.domain.generator.MnemonicPasswordGeneratorV2
import com.mnemokot.core.crypto.domain.model.MnemonicConfig
import com.mnemokot.core.crypto.domain.model.MnemonicGeneratorUiState
import com.mnemokot.core.crypto.domain.model.MnemonicMethod
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
class MnemonicGeneratorViewModel @Inject constructor(
    private val passwordGenerator: MnemonicPasswordGeneratorV2
) : ViewModel() {

    private val _config = MutableStateFlow(MnemonicConfig())
    private val _isHelpVisible = MutableStateFlow(false)

    val uiState: StateFlow<MnemonicGeneratorUiState> = combine(
        _config,
        _isHelpVisible
    ) { config, isHelpVisible ->
        val steps = passwordGenerator.generateMnemonicV2(config)
        MnemonicGeneratorUiState(
            config = config,
            steps = steps,
            isHelpVisible = isHelpVisible
        )
    }.flowOn(Dispatchers.Default)
     .stateIn(
         scope = viewModelScope,
         started = SharingStarted.WhileSubscribed(5000),
         initialValue = MnemonicGeneratorUiState()
     )

    fun onMethodChanged(method: MnemonicMethod) {
        _config.value = _config.value.copy(method = method)
    }

    fun onPhraseChanged(phrase: String) {
        _config.value = _config.value.copy(phrase = phrase)
    }

    fun onAnchorWordChanged(anchor: String) {
        _config.value = _config.value.copy(anchorWord = anchor)
    }

    fun onServiceNameChanged(service: String) {
        _config.value = _config.value.copy(serviceName = service)
    }

    fun onDateContextChanged(date: String) {
        if (date.length <= 4 && date.all { it.isDigit() }) {
            _config.value = _config.value.copy(dateContext = date)
        }
    }

    fun onTargetLengthChanged(length: Int) {
        _config.value = _config.value.copy(targetLength = length)
    }

    fun onKeepRootsChanged(keep: Boolean) {
        _config.value = _config.value.copy(keepRoots = keep)
    }

    fun onMutateRepeatsChanged(mutate: Boolean) {
        _config.value = _config.value.copy(mutateRepeats = mutate)
    }

    fun onIncludeAnchorChanged(include: Boolean) {
        _config.value = _config.value.copy(includeAnchor = include)
    }

    fun onIncludeServiceChanged(include: Boolean) {
        _config.value = _config.value.copy(includeService = include)
    }

    fun onIncludeDateChanged(include: Boolean) {
        _config.value = _config.value.copy(includeDate = include)
    }

    fun toggleHelpVisibility() {
        _isHelpVisible.value = !_isHelpVisible.value
    }
}
