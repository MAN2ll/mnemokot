package ru.man2ll.mnemokot.ui.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.man2ll.mnemokot.core.crypto.domain.generator.MnemonicPasswordGeneratorV2
import ru.man2ll.mnemokot.core.crypto.domain.model.MnemonicMode
import ru.man2ll.mnemokot.core.crypto.domain.model.MnemonicPhrase
import ru.man2ll.mnemokot.core.database.domain.model.Account
import ru.man2ll.mnemokot.core.database.domain.repository.MnemokotRepository
import ru.man2ll.mnemokot.core.database.domain.session.SessionManager
import ru.man2ll.mnemokot.core.security.crypto.SecurityCryptoEngine
import java.security.SecureRandom
import javax.inject.Inject

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val repository: MnemokotRepository,
    private val sessionManager: SessionManager,
    private val passwordGenerator: MnemonicPasswordGeneratorV2,
    private val cryptoEngine: SecurityCryptoEngine
) : ViewModel() {

    private val secureRandom = SecureRandom()

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    private val _calculatedPassword = MutableStateFlow<CharArray?>(null)
    val calculatedPassword: StateFlow<CharArray?> = _calculatedPassword.asStateFlow()

    private val _vaultError = MutableStateFlow<String?>(null)
    val vaultError: StateFlow<String?> = _vaultError.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _selectedAccountIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedAccountIds: StateFlow<Set<Long>> = _selectedAccountIds.asStateFlow()

    private var masterPasswordForEncryption: CharArray? = null

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

    fun generateSalt(): ByteArray {
        val salt = ByteArray(16)
        secureRandom.nextBytes(salt)
        return salt
    }

    fun generatePasswordPreview(
        phrase: CharArray,
        salt: ByteArray,
        mode: MnemonicMode,
        useLeetSpeak: Boolean,
        length: Int
    ): CharArray {
        val mnemonicPhrase = MnemonicPhrase(phrase.copyOf())
        val result = passwordGenerator.generatePassword(
            phrase = mnemonicPhrase,
            salt = salt,
            length = length,
            mode = mode,
            useLeetSpeak = useLeetSpeak
        )
        val preview = result.password.clone()
        mnemonicPhrase.close()
        result.close()
        return preview
    }

    fun generateRandomPassword(length: Int): CharArray {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+[]{}|;:,.<>?"
        val password = CharArray(length)
        for (i in 0 until length) {
            password[i] = chars[secureRandom.nextInt(chars.length)]
        }
        return password
    }

    fun setMasterPasswordForEncryption(masterPassword: CharArray) {
        masterPasswordForEncryption?.fill('\u0000')
        masterPasswordForEncryption = masterPassword
    }

    fun saveAccount(data: CreateAccountData) {
        val profileId = sessionManager.activeProfileId.value ?: return
        val masterPassword = masterPasswordForEncryption

        if (data.isStoredInVault && masterPassword == null) {
            _vaultError.value = "Требуется мастер-пароль для шифрования"
            return
        }

        viewModelScope.launch {
            val currentTime = System.currentTimeMillis()
            val saltString = data.salt.joinToString("") { "%02x".format(it) }

            var encryptedPassword: String? = null
            var encryptedPhrase: String? = null

            when (data.accountType) {
                AccountType.MNEMONIC -> {}

                AccountType.HYBRID -> {
                    val phrase = data.mnemonicPhrase ?: return@launch
                    val generatedPassword = generatePasswordPreview(
                        phrase, data.salt, data.mode, data.useLeetSpeak, data.passwordLength
                    )
                    encryptedPassword = cryptoEngine.encryptPassword(generatedPassword, masterPassword!!)
                    encryptedPhrase = cryptoEngine.encryptPassword(phrase, masterPassword)
                    generatedPassword.fill('\u0000')
                }

                AccountType.VAULT -> {
                    encryptedPassword = cryptoEngine.encryptPassword(data.password, masterPassword!!)
                }
            }

            val account = Account(
                id = 0,
                profileId = profileId,
                serviceName = data.serviceName,
                login = data.login,
                isMnemonic = data.isMnemonic,
                passwordLength = data.passwordLength,
                salt = saltString,
                rotationDate = "",
                explicitPassword = encryptedPassword,
                encryptedMnemonicPhrase = encryptedPhrase,
                isDualUserMode = false,
                createdAt = currentTime,
                expiresAt = 0L,
                updatedAt = currentTime
            )

            repository.saveAccount(account)
            data.clearSensitiveData()
            masterPasswordForEncryption?.fill('\u0000')
            masterPasswordForEncryption = null
            _vaultError.value = null
        }
    }

    fun verifyProfilePinForAction(pin: CharArray, onSuccessAction: () -> Unit) {
        val profileId = sessionManager.activeProfileId.value ?: return
        _vaultError.value = null
        viewModelScope.launch {
            val isValid = repository.verifyPin(profileId, pin)
            pin.fill('\u0000')
            if (isValid) {
                onSuccessAction()
            } else {
                _vaultError.value = "Неверный PIN-код профиля"
            }
        }
    }

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
            }
            _vaultError.value = reportMessage
        }
    }

    fun onCalculatePassword(accountId: Long, phrase: CharArray?) {
        if (phrase == null) return
        viewModelScope.launch {
            val mnemonicPhrase = MnemonicPhrase(phrase)
            val generationResult = repository.getPasswordForAccount(accountId, mnemonicPhrase)
            if (generationResult.password.isNotEmpty()) {
                _calculatedPassword.value = generationResult.password.clone()
            }
            mnemonicPhrase.close()
            generationResult.close()
        }
    }

    fun clearCalculatedPassword() {
        _calculatedPassword.value?.fill('\u0000')
        _calculatedPassword.value = null
    }

    fun clearVaultError() {
        _vaultError.value = null
    }

    fun onDeleteAccount(id: Long) {
        viewModelScope.launch {
            repository.deleteAccount(id)
        }
    }

    override fun onCleared() {
        super.onCleared()
        masterPasswordForEncryption?.fill('\u0000')
        masterPasswordForEncryption = null
        _calculatedPassword.value?.fill('\u0000')
    }
}
