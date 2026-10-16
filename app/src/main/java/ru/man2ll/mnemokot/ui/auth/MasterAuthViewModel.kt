package ru.man2ll.mnemokot.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.man2ll.mnemokot.core.database.domain.repository.MnemokotRepository
import ru.man2ll.mnemokot.core.database.domain.session.SessionManager
import ru.man2ll.mnemokot.core.security.crypto.SecurityCryptoEngine
import javax.inject.Inject

sealed interface MasterUiState {
    data object Loading : MasterUiState
    data object SetupMaster : MasterUiState
    data class EnterMaster(val isBiometricAvailable: Boolean) : MasterUiState
    data object AccessGranted : MasterUiState
}

@HiltViewModel
class MasterAuthViewModel @Inject constructor(
    private val repository: MnemokotRepository,
    private val cryptoEngine: SecurityCryptoEngine,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<MasterUiState>(MasterUiState.Loading)
    val uiState: StateFlow<MasterUiState> = _uiState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        checkMasterStatus()
    }

    fun checkMasterStatus() {
        viewModelScope.launch {
            if (repository.isMasterPasswordSet()) {
                val bioEnabled = repository.isBiometricEnabled()
                _uiState.value = MasterUiState.EnterMaster(isBiometricAvailable = bioEnabled)
            } else {
                _uiState.value = MasterUiState.SetupMaster
            }
        }
    }

    fun onSetupMasterPassword(password: String) {
        if (password.length < 6) {
            _errorMessage.value = "Мастер-пароль должен быть не менее 6 символов"
            return
        }
        viewModelScope.launch {
            val passChars = password.toCharArray()
            val staticSalt = "MnemoKot_Master_Salt".toByteArray(Charsets.UTF_8)
            val securedHash = cryptoEngine.hashPin(passChars, staticSalt)
            passChars.fill(' ')
            
            repository.setupMasterPassword(securedHash)
            checkMasterStatus() // Перечитываем стейт для мгновенного пуска биометрии
        }
    }

    fun onVerifyMasterPassword(password: String): Boolean {
        _errorMessage.value = null
        var isValid = false
        val passChars = password.toCharArray()
        val staticSalt = "MnemoKot_Master_Salt".toByteArray(Charsets.UTF_8)
        val testHash = cryptoEngine.hashPin(passChars, staticSalt)
        passChars.fill(' ')

        // Выполняем блокирующе для синхронных проверок в диалогах UI
        run {
            val job = viewModelScope.launch {
                isValid = repository.verifyMasterPassword(testHash)
            }
            while (!job.isCompleted) { /* wait */ }
        }

        if (isValid) {
            _uiState.value = MasterUiState.AccessGranted
        } else {
            _errorMessage.value = "Неверный глобальный Мастер-пароль!"
        }
        return isValid
    }

    fun onBiometricSuccess() {
        _uiState.value = MasterUiState.AccessGranted
    }

    fun setBiometricPreference(enabled: Boolean) {
        // Блокирующий запуск для гарантированного сохранения перед вызовом установки пароля
        run {
            val job = viewModelScope.launch {
                repository.setBiometricEnabled(enabled)
            }
            while (!job.isCompleted) { /* wait */ }
        }
    }
}
