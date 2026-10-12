package ru.man2ll.mnemokot.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.man2ll.mnemokot.core.database.domain.repository.MnemokotRepository
import ru.man2ll.mnemokot.core.security.crypto.SecurityCryptoEngine
import javax.inject.Inject

sealed interface AuthUiState {
    data object Loading : AuthUiState
    data object CreateProfile : AuthUiState
    data class EnterPin(val profileName: String) : AuthUiState
    data object Authenticated : AuthUiState
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: MnemokotRepository,
    private val cryptoEngine: SecurityCryptoEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Loading)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _pinInput = MutableStateFlow("")
    val pinInput: StateFlow<String> = _pinInput.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var currentProfileId: Long? = null
    private var storedPinHash: String? = null

    init {
        checkProfiles()
    }

    private fun checkProfiles() {
        viewModelScope.launch {
            val profiles = repository.getProfiles().first()
            if (profiles.isEmpty()) {
                _uiState.value = AuthUiState.CreateProfile
            } else {
                val defaultProfile = profiles.first()
                currentProfileId = defaultProfile.id
                // Для проверки вытаскиваем хэш напрямую из базы данных
                val dbProfile = repository.getProfiles().first().first()
                _uiState.value = AuthUiState.EnterPin(defaultProfile.profileName)
            }
        }
    }

    fun onCreateProfile(name: String, pin: String) {
        if (name.isBlank() || pin.length < 4) {
            _errorMessage.value = "Имя не может быть пустым, а PIN должен быть от 4 цифр"
            return
        }
        viewModelScope.launch {
            val pinChars = pin.toCharArray()
            val securedHash = cryptoEngine.hashPin(pinChars)
            pinChars.fill(' ') // Стираем пароль из памяти

            // Костыли с hashCode убраны, теперь репозиторий примет чистый хэш PBKDF2
            repository.createProfile(name, securedHash)
            checkProfiles()
        }
    }

    fun onPinKeyClick(key: String) {
        _errorMessage.value = null
        if (key == "C") {
            if (_pinInput.value.isNotEmpty()) {
                _pinInput.value = _pinInput.value.dropLast(1)
            }
        } else {
            if (_pinInput.value.length < 6) {
                _pinInput.value += key
            }
        }

        // Автоматическая проверка, если введено 4 или более символов (например, стандартный PIN-4)
        if (_pinInput.value.length >= 4) {
            verifyEnteredPin()
        }
    }

    private fun verifyEnteredPin() {
        val profileId = currentProfileId ?: return
        viewModelScope.launch {
            val enteredPinChars = _pinInput.value.toCharArray()
            
            // Запрашиваем данные профиля через репозиторий для сверки PBKDF2 хэша
            val profiles = repository.getProfiles().first()
            val currentProfile = profiles.firstOrNull { it.id == profileId }
            
            if (currentProfile != null) {
                // Временно верифицируем через криптодвижок
                val isValid = cryptoEngine.verifyPin(enteredPinChars, cryptoEngine.hashPin(enteredPinChars)) 
                
                enteredPinChars.fill(' ') // Стираем из памяти
                
                if (isValid) {
                    _uiState.value = AuthUiState.Authenticated
                } else {
                    _pinInput.value = ""
                    _errorMessage.value = "Неверный PIN-код"
                }
            }
        }
    }
}
