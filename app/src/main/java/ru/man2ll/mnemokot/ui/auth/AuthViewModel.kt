package ru.man2ll.mnemokot.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val pinBuffer = CharArray(6) { '\u0000' }
    private var pinLength = 0

    private val _pinLengthState = MutableStateFlow(0)
    val pinLengthState: StateFlow<Int> = _pinLengthState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadProfilesState()
    }

    fun loadProfilesState() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val profiles = repository.getProfiles().first()
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

    fun onSelectProfile(profile: UserProfile) {
        clearPinBuffer()
        _errorMessage.value = null
        sessionManager.selectProfile(profile.id)
        loadProfilesState()
    }

    fun navigateToCreateProfile() {
        clearPinBuffer()
        _errorMessage.value = null
        _uiState.value = AuthUiState.CreateProfile
    }

    fun cancelCreateProfile() {
        clearPinBuffer()
        _errorMessage.value = null
        sessionManager.logout()
        loadProfilesState()
    }

    fun onCreateProfile(name: String, pin: CharArray) {
        if (name.isBlank() || pin.size < 4) {
            _errorMessage.value = "Заполните имя и PIN (мин. 4 цифры)"
            return
        }
        viewModelScope.launch {
            val staticSalt = "MnemoKot_Pin_Salt".toByteArray(Charsets.UTF_8)
            val securedHash = cryptoEngine.hashPin(pin, staticSalt)
            pin.fill('\u0000')
            staticSalt.fill(0)
            
            val newId = repository.createProfile(name, securedHash)
            sessionManager.selectProfile(newId)
            loadProfilesState()
        }
    }

    fun onPinKeyClick(key: String) {
        _errorMessage.value = null
        if (key == "C") {
            if (pinLength > 0) {
                pinLength--
                pinBuffer[pinLength] = '\u0000'
                _pinLengthState.value = pinLength
            }
        } else {
            if (pinLength < 6 && key.length == 1 && key[0].isDigit()) {
                pinBuffer[pinLength] = key[0]
                pinLength++
                _pinLengthState.value = pinLength
                if (pinLength == 6) {
                    verifyPin()
                }
            }
        }
    }
    
    fun triggerManualVerification() {
        if (pinLength >= 4) {
            verifyPin()
        } else {
            _errorMessage.value = "PIN-код должен быть от 4 до 6 цифр"
        }
    }

    private fun verifyPin() {
        val profileId = sessionManager.activeProfileId.value ?: return
        viewModelScope.launch {
            val profiles = repository.getProfiles().first()
            val activeProfile = profiles.firstOrNull { it.id == profileId }

            if (activeProfile != null) {
                val staticSalt = "MnemoKot_Pin_Salt".toByteArray(Charsets.UTF_8)
                val enteredPin = CharArray(pinLength) { pinBuffer[it] }
                val testHash = cryptoEngine.hashPin(enteredPin, staticSalt)
                enteredPin.fill('\u0000')
                staticSalt.fill(0)

                val isValid = repository.verifyPin(profileId, testHash)
                clearPinBuffer()

                if (isValid) {
                    repository.resetFailedAttempts(profileId)
                    sessionManager.authorizeSuccess()
                } else {
                    val result = repository.incrementFailedAttempts(profileId)
                    if (result == -1) {
                        sessionManager.logout()
                        loadProfilesState()
                        _errorMessage.value = "Профиль уничтожен из-за брутфорса!"
                    } else {
                        val remaining = repository.getRemainingAttempts(profileId)
                        _errorMessage.value = "Неверный PIN. Осталось попыток: $remaining"
                    }
                }
            }
        }
    }

    private fun clearPinBuffer() {
        pinBuffer.fill('\u0000')
        pinLength = 0
        _pinLengthState.value = 0
    }

    override fun onCleared() {
        super.onCleared()
        clearPinBuffer()
    }
}
