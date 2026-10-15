package ru.man2ll.mnemokot.ui.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
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
    private val cryptoEngine: SecurityCryptoEngine // Внедряем криптодвижок для проверки PIN
) : ViewModel() {

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    private val _calculatedPassword = MutableStateFlow<String?>(null)
    val calculatedPassword: StateFlow<String?> = _calculatedPassword.asStateFlow()

    private val _pinVerificationSuccess = MutableStateFlow(false)
    val pinVerificationSuccess: StateFlow<Boolean> = _pinVerificationSuccess.asStateFlow()

    private val _vaultError = MutableStateFlow<String?>(null)
    val vaultError: StateFlow<String?> = _vaultError.asStateFlow()

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
                }
            }
        }
    }

    // Метод проверки PIN-кода профиля перед показом пароля
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
                _pinVerificationSuccess.value = true
                onSuccessAction()
            } else {
                _pinVerificationSuccess.value = false
                _vaultError.value = "Неверный PIN-код профиля"
            }
        }
    }

    fun clearVaultError() {
        _vaultError.value = null
    }

    fun resetPinVerification() {
        _pinVerificationSuccess.value = false
    }

    fun onAddAccount(service: String, login: String, isMnemonic: Boolean, len: Int, salt: String, date: String, explicit: String?, isDual: Boolean) {
        val profileId = sessionManager.activeProfileId.value ?: return
        viewModelScope.launch {
            val account = Account(
                id = 0, profileId = profileId, serviceName = service, login = login,
                isMnemonic = isMnemonic, passwordLength = len, salt = salt, rotationDate = date, 
                explicitPassword = explicit, isDualUserMode = isDual
            )
            repository.saveAccount(account)
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

    fun clearCalculatedPassword() { 
        _calculatedPassword.value = null 
    }
    
    fun onDeleteAccount(id: Long) { 
        viewModelScope.launch { repository.deleteAccount(id) } 
    }
}
