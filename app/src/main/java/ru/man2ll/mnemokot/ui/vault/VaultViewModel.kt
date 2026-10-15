package ru.man2ll.mnemokot.ui.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.man2ll.mnemokot.core.database.domain.model.Account
import ru.man2ll.mnemokot.core.database.domain.repository.MnemokotRepository
import ru.man2ll.mnemokot.core.database.domain.session.SessionManager
import ru.man2ll.mnemokot.core.security.crypto.SecurityCryptoEngine
import javax.inject.Inject

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val repository: MnemokotRepository,
    private val sessionManager: SessionManager,
    private val cryptoEngine: SecurityCryptoEngine
) : ViewModel() {

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    private val _calculatedPassword = MutableStateFlow<String?>(null)
    val calculatedPassword: StateFlow<String?> = _calculatedPassword.asStateFlow()

    private val _vaultError = MutableStateFlow<String?>(null)
    val vaultError: StateFlow<String?> = _vaultError.asStateFlow()

    // СОСТОЯНИЯ ДЛЯ РЕЖИМА ВЫБОРОЧНОЙ РОТАЦИИ ПАРОЛЕЙ (ЧЕКБОКСЫ)
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _selectedAccountIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedAccountIds: StateFlow<Set<Long>> = _selectedAccountIds.asStateFlow()

    init {
        observeActiveProfile()
    }

    private fun observeActiveProfile() {
        viewModelScope.launch {
            sessionManager.activeProfileId.collectLatest { profileId ->
                if (profileId != null) {
                    repository.getAccounts(profileId).collectLatest { list ->
                        _accounts.value = list
                    }
                } else {
                    _accounts.value = emptyList()
                    cancelSelectionMode()
                }
            }
        }
    }

    // Безопасная проверка PIN-кода профиля перед раскрытием ("глаз")
    fun verifyProfilePinForAction(pin: String, onSuccessAction: () -> Unit) {
        val profileId = sessionManager.activeProfileId.value ?: return
        _vaultError.value = null
        
        viewModelScope.launch {
            val pinChars = pin.toCharArray()
            val staticSalt = "MnemoKot_Pin_Salt".toByteArray(Charsets.UTF_8)
            val testHash = cryptoEngine.hashPin(pinChars, staticSalt)
            pinChars.fill(' ')

            val isValid = repository.verifyPin(profileId, testHash)
            if (isValid) {
                onSuccessAction()
            } else {
                _vaultError.value = "Неверный PIN-код профиля"
            }
        }
    }

    // Сохранение нового аккаунта с расчетом жизненного цикла (expiresAt)
    fun onAddAccount(
        service: String, 
        login: String, 
        isMnemonic: Boolean, 
        len: Int, 
        salt: String, 
        daysToLive: Int, 
        explicit: String?, 
        isDual: Boolean
    ) {
        val profileId = sessionManager.activeProfileId.value ?: return
        viewModelScope.launch {
            val currentTime = System.currentTimeMillis()
            val expiresAtTime = if (daysToLive > 0) {
                currentTime + (daysToLive.toLong() * 24 * 60 * 60 * 1000)
            } else {
                0L
            }

            val account = Account(
                id = 0, 
                profileId = profileId, 
                serviceName = service, 
                login = login,
                isMnemonic = isMnemonic, 
                passwordLength = len, 
                salt = salt, 
                rotationDate = daysToLive.toString(), 
                explicitPassword = explicit, 
                isDualUserMode = isDual,
                createdAt = currentTime,
                expiresAt = expiresAtTime
            )
            repository.saveAccount(account)
        }
    }

    // Логика переключения чекбоксов в режиме ротации
    fun toggleAccountSelection(accountId: Long) {
        val currentSet = _selectedAccountIds.value
        _selectedAccountIds.value = if (currentSet.contains(accountId)) {
            currentSet - accountId
        } else {
            currentSet + accountId
        }
    }

    fun startSelectionMode() {
        _isSelectionMode.value = true
        _selectedAccountIds.value = emptySet()
        _vaultError.value = null
    }

    fun cancelSelectionMode() {
        _isSelectionMode.value = false
        _selectedAccountIds.value = emptySet()
        _vaultError.value = null
    }

    // Запуск перемешивания паролей для отмеченных чекбоксами записей
    fun runSelectedPasswordRotation() {
        val idsToRotate = _selectedAccountIds.value.toList()
        if (idsToRotate.size < 2) {
            _vaultError.value = "Отметьте минимум 2 аккаунта для перемешивания"
            return
        }
        
        viewModelScope.launch {
            val (success, reportMessage) = repository.rotateSelectedPasswords(idsToRotate)
            if (success) {
                cancelSelectionMode()
                _vaultError.value = reportMessage // Выводим диалог отчета или тост
            } else {
                _vaultError.value = reportMessage
            }
        }
    }

    fun onCalculatePassword(accountId: Long, phrase: String?) {
        viewModelScope.launch {
            val passChars = repository.getPasswordForAccount(accountId, phrase)
            if (passChars.isNotEmpty()) {
                _calculatedPassword.value = String(passChars)
                passChars.fill(' ')
            }
        }
    }

    fun clearCalculatedPassword() { _calculatedPassword.value = null }
    fun clearVaultError() { _vaultError.value = null }
    fun onDeleteAccount(id: Long) { viewModelScope.launch { repository.deleteAccount(id) } }
}
