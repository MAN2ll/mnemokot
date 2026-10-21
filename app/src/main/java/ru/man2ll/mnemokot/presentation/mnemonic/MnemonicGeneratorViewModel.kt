package ru.man2ll.mnemokot.presentation.mnemonic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.man2ll.mnemokot.core.crypto.domain.generator.MnemonicPasswordGeneratorV2
import ru.man2ll.mnemokot.core.crypto.domain.model.MnemonicPhrase
import ru.man2ll.mnemokot.core.crypto.domain.model.TranslitMode
import javax.inject.Inject

data class MnemonicGeneratorUiState(
    val passwordLength: Int = 16,
    val useLeetSpeak: Boolean = true,
    val isHelpVisible: Boolean = false,
    val calculatedPassword: CharArray? = null
)

@HiltViewModel
class MnemonicGeneratorViewModel @Inject constructor(
    private val passwordGenerator: MnemonicPasswordGeneratorV2
) : ViewModel() {

    private val _uiState = MutableStateFlow(MnemonicGeneratorUiState())
    val uiState: StateFlow<MnemonicGeneratorUiState> = _uiState.asStateFlow()

    private val inputPhraseBuffer = CharArray(256) { '\u0000' }
    private var phraseLength = 0

    fun onPhraseCharAdded(char: Char) {
        if (phraseLength < inputPhraseBuffer.size) {
            inputPhraseBuffer[phraseLength] = char
            phraseLength++
        }
    }

    fun onPhraseBackspace() {
        if (phraseLength > 0) {
            phraseLength--
            inputPhraseBuffer[phraseLength] = '\u0000'
        }
    }

    fun onTargetLengthChanged(length: Int) {
        _uiState.value = _uiState.value.copy(passwordLength = length)
    }

    fun onLeetSpeakToggled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(useLeetSpeak = enabled)
    }

    fun toggleHelpVisibility() {
        _uiState.value = _uiState.value.copy(isHelpVisible = !_uiState.value.isHelpVisible)
    }

    fun generate() {
        viewModelScope.launch {
            val cleanPhrase = CharArray(phraseLength) { inputPhraseBuffer[it] }
            val mnemonicPhrase = MnemonicPhrase(cleanPhrase)
            val staticSalt = "MnemoKot_Generation_Salt".toByteArray(Charsets.UTF_8)
            
            val state = _uiState.value
            val result = passwordGenerator.generatePassword(
                phrase = mnemonicPhrase,
                salt = staticSalt,
                length = state.passwordLength,
                translitMode = TranslitMode.GOST_7_79_2000_B,
                useLeetSpeak = state.useLeetSpeak
            )

            _uiState.value = state.copy(calculatedPassword = result.password.clone())
            
            mnemonicPhrase.close()
            result.close()
            staticSalt.fill(0)
        }
    }

    fun clearGeneratedPassword() {
        _uiState.value.calculatedPassword?.fill('\u0000')
        _uiState.value = _uiState.value.copy(calculatedPassword = null)
    }

    override fun onCleared() {
        super.onCleared()
        inputPhraseBuffer.fill('\u0000')
        clearGeneratedPassword()
    }
}
