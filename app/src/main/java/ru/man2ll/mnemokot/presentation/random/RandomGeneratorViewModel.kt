package ru.man2ll.mnemokot.presentation.random

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.security.SecureRandom
import javax.inject.Inject

data class RandomConfig(
    val length: Int = 16,
    val includeUppercase: Boolean = true,
    val includeLowercase: Boolean = true,
    val includeDigits: Boolean = true,
    val includeSymbols: Boolean = true,
    val excludeSimilar: Boolean = false
)

data class RandomGeneratorUiState(
    val config: RandomConfig = RandomConfig(),
    val result: RandomResult = RandomResult(),
    val isHelpVisible: Boolean = false
)

data class RandomResult(
    val password: CharArray? = null,
    val entropyBits: Double = 0.0,
    val uppercaseCount: Int = 0,
    val lowercaseCount: Int = 0,
    val digitsCount: Int = 0,
    val symbolsCount: Int = 0
)

@HiltViewModel
class RandomGeneratorViewModel @Inject constructor() : ViewModel() {

    private val secureRandom = SecureRandom()

    private val _uiState = MutableStateFlow(RandomGeneratorUiState())
    val uiState: StateFlow<RandomGeneratorUiState> = _uiState.asStateFlow()

    init {
        regeneratePassword()
    }

    fun onLengthChanged(length: Int) {
        _uiState.value = _uiState.value.copy(config = _uiState.value.config.copy(length = length))
        regeneratePassword()
    }

    fun onIncludeUppercaseChanged(include: Boolean) {
        _uiState.value = _uiState.value.copy(config = _uiState.value.config.copy(includeUppercase = include))
        regeneratePassword()
    }
    fun onIncludeLowercaseChanged(include: Boolean) {
        _uiState.value = _uiState.value.copy(config = _uiState.value.config.copy(includeLowercase = include))
        regeneratePassword()
    }

    fun onIncludeDigitsChanged(include: Boolean) {
        _uiState.value = _uiState.value.copy(config = _uiState.value.config.copy(includeDigits = include))
        regeneratePassword()
    }

    fun onIncludeSymbolsChanged(include: Boolean) {
        _uiState.value = _uiState.value.copy(config = _uiState.value.config.copy(includeSymbols = include))
        regeneratePassword()
    }

    fun onExcludeSimilarChanged(exclude: Boolean) {
        _uiState.value = _uiState.value.copy(config = _uiState.value.config.copy(excludeSimilar = exclude))
        regeneratePassword()
    }

    fun toggleHelpVisibility() {
        _uiState.value = _uiState.value.copy(isHelpVisible = !_uiState.value.isHelpVisible)
    }

    fun regeneratePassword() {
        viewModelScope.launch {
            val config = _uiState.value.config
            val uppercaseChars = "ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray()
            val uppercaseSimilar = "IO".toCharArray()
            val lowercaseChars = "abcdefghijkmnopqrstuvwxyz".toCharArray()
            val lowercaseSimilar = "l".toCharArray()
            val digitChars = "23456789".toCharArray()
            val digitSimilar = "10".toCharArray()
            val symbolChars = "!@#\$%^&*()-_=+[]{}|;:,.<>?".toCharArray()

            var poolStr = ""
            if (config.includeUppercase) poolStr += String(uppercaseChars) + if (!config.excludeSimilar) String(uppercaseSimilar) else ""
            if (config.includeLowercase) poolStr += String(lowercaseChars) + if (!config.excludeSimilar) String(lowercaseSimilar) else ""
            if (config.includeDigits) poolStr += String(digitChars) + if (!config.excludeSimilar) String(digitSimilar) else ""
            if (config.includeSymbols) poolStr += String(symbolChars)

            if (poolStr.isEmpty()) {
                _uiState.value = _uiState.value.copy(result = RandomResult())
                return@launch
            }

            val pool = poolStr.toCharArray()
            val password = CharArray(config.length)
            
            var up = 0; var low = 0; var dig = 0; var sym = 0
            val fullUpper = String(uppercaseChars) + String(uppercaseSimilar)
            val fullLower = String(lowercaseChars) + String(lowercaseSimilar)
            val fullDigit = String(digitChars) + String(digitSimilar)

            for (i in 0 until config.length) {
                val nextChar = pool[secureRandom.nextInt(pool.size)]
                password[i] = nextChar
                when {
                    fullUpper.contains(nextChar) -> up++
                    fullLower.contains(nextChar) -> low++
                    fullDigit.contains(nextChar) -> dig++
                    String(symbolChars).contains(nextChar) -> sym++
                }
            }

            val entropy = config.length * (Math.log(pool.size.toDouble()) / Math.log(2.0))

            _uiState.value = _uiState.value.copy(
                result = RandomResult(
                    password = password,
                    entropyBits = entropy,
                    uppercaseCount = up,
                    lowercaseCount = low,
                    digitsCount = dig,
                    symbolsCount = sym
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        _uiState.value.result.password?.fill('\u0000')
    }
}
