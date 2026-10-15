package ru.man2ll.mnemokot.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.man2ll.mnemokot.core.database.domain.model.UserProfile
import ru.man2ll.mnemokot.core.database.domain.repository.MnemokotRepository
import ru.man2ll.mnemokot.core.database.domain.session.SessionManager
import ru.man2ll.mnemokot.core.security.crypto.SecurityCryptoEngine
import javax.inject.Inject

sealed interface AuthUiState {
    data object Loading : AuthUiState
    data class ProfileSelection(val profiles: List<UserProfile>) : AuthUiState
    data object CreateProfile : AuthUiState
    data class EnterPin(val profileName: String, val remainingAttempts: Int) : AuthUiState
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: MnemokotRepository,
    private val cryptoEngine: SecurityCryptoEngine,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Loading)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _pinInput = MutableStateFlow("")
    val pinInput: StateFlow<String> = _pinInput.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        observeProfiles()
    }

    private fun observeProfiles() {
        viewModelScope.launch {
            repository.getProfiles().collectLatest { profiles ->
                val activeId = sessionManager.activeProfileId.value
                if (profiles.isEmpty()) {
                    _uiState.value = AuthUiState.CreateProfile
                } else if (activeId == null) {
                    _uiState.value = AuthUiState.ProfileSelection(profiles)
                } else {
                    val activeProfile = profiles.firstOrNull { it.id == activeId }
                    if (activeProfile != null) {
                        val remaining = repository.getRemainingAttempts(activeId)
                        _uiState.value = AuthUiState.EnterPin(activeProfile.profileName, remaining)
                    } else {
                        _uiState.value = AuthUiState.ProfileSelection(profiles)
                    }
                }
            }
        }
    }

    fun onSelectProfile(profile: UserProfile) {
        _pinInput.value = ""
        _errorMessage.value = null
        sessionManager.selectProfile(profile.id)
    }

    fun navigateToCreateProfile() {
        _pinInput.value = ""
        _errorMessage.value = null
        _uiState.value = AuthUiState.CreateProfile
    }

    fun cancelCreateProfile() {
        _pinInput.value = ""
        _errorMessage.value = null
        sessionManager.logout()
        // Принудительно заставляем перечитать базу, возвращаясь к списку
        viewModelScope.launch {
            val profiles = repository.getProfiles().first()
            if (profiles.isNotEmpty()) {
                _uiState.value = AuthUiState.ProfileSelection(profiles)
            } else {
                _uiState.value = AuthUiState.CreateProfile
            }
        }
    }

    fun onCreateProfile(name: String, pin: String) {
        if (name.isBlank() || pin.length < 4) {
            _errorMessage.value = "Заполните имя и PIN (мин. 4 цифры)"
            return
        }
        viewModelScope.launch {
            val pinChars = pin.toCharArray()
            val staticSalt = "MnemoKot_Pin_Salt".toByteArray(Charsets.UTF_8)
            val securedHash = cryptoEngine.hashPin(pinChars, staticSalt)
            pinChars.fill(' ')
            val newId = repository.createProfile(name, securedHash)
            sessionManager.selectProfile(newId)
        }
    }

    fun onPinKeyClick(key: String) {
        _errorMessage.value = null
        val currentPin = _pinInput.value
        if (key == "C") {
            if (currentPin.isNotEmpty()) _pinInput.value = currentPin.dropLast(1)
        } else {
            // Аппаратно поддерживаем ввод PIN-кода переменной длины до 6 символов
            if (currentPin.length < 6) _pinInput.value = currentPin + key
        }

        // Проверяем PIN, если введено от 4 до 6 символов и пользователь сделал паузу, 
        // либо если введено ровно 6 символов (максимальный лимит)
        if (_pinInput.value.length == 6) {
            verifyPin()
        }
    }
    
    fun triggerManualVerification() {
        if (_pinInput.value.length >= 4) {
            verifyPin()
        } else {
            _errorMessage.value = "PIN-код должен быть от 4 до 6 цифр"
        }
    }

    private fun verifyPin() {
        val profileId = sessionManager.activeProfileId.value ?: return
        viewModelScope.launch {
            val enteredPinChars = _pinInput.value.toCharArray()
            val profiles = repository.getProfiles().first()
            val activeProfile = profiles.firstOrNull { it.id == profileId }

            if (activeProfile != null) {
                val staticSalt = "MnemoKot_Pin_Salt".toByteArray(Charsets.UTF_8)
                val testHash = cryptoEngine.hashPin(enteredPinChars, staticSalt)
                enteredPinChars.fill(' ')

                val isValid = repository.verifyPin(profileId, testHash)

                if (isValid) {
                    repository.resetFailedAttempts(profileId)
                    _pinInput.value = ""
                    sessionManager.authorizeSuccess()
                } else {
                    _pinInput.value = ""
                    val result = repository.incrementFailedAttempts(profileId)
                    if (result == -1) {
                        sessionManager.logout()
                        _errorMessage.value = "Профиль уничтожен из-за брутфорса!"
                    } else {
                        val remaining = repository.getRemainingAttempts(profileId)
                        _errorMessage.value = "Неверный PIN. Осталось попыток: $remaining"
                    }
                }
            }
        }
    }
}
