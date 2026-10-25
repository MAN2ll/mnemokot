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
import ru.man2ll.mnemokot.core.crypto.domain.model.MnemonicPhrase
import ru.man2ll.mnemokot.core.crypto.domain.model.MnemonicMode
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

    // Временное хранение мастер-пароля для шифрования (должен быть получен от пользователя)
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

    /**
     * Устанавливает мастер-пароль для шифрования новых записей.
     * Должен быть вызван перед saveAccount.
     */
    fun setMasterPasswordForEncryption(masterPassword: CharArray) {
        masterPasswordForEncryption = masterPassword
    }

    /**
     * Сохраняет новую карточку пароля.
     * Поддерживает три режима:
     * 1. Чистая мнемоника (isMnemonic=true, isStoredInVault=false)
     * 2. Гибрид (isMnemonic=true, isStoredInVault=true)
     * 3. Vault (isMnemonic=false)
     */
    fun saveAccount(data: CreateAccountData) {
        val profileId = sessionManager.activeProfileId.value ?: return
        val masterPassword = masterPasswordForEncryption

        if (data.isStoredInVault && masterPassword == null) {
            _vaultError.value = "Требуется мастер-пароль для шифрования"
            return
        }

        viewModelScope.launch {
            val currentTime = System.currentTimeMillis()
            
            // Генерируем уникальную соль для аккаунта
            val saltBytes = ByteArray(16)
            secureRandom.nextBytes(saltBytes)
            val saltString = saltBytes.joinToString("") { "%02x".format(it) }

            var encryptedPassword: String? = null
            var encryptedPhrase: String? = null

            when {
                // Режим 1: Чистая мнемоника — пароль не сохраняется
                data.isMnemonic && !data.isStoredInVault -> {
                    // Ничего не шифруем, пароль будет генерироваться на лету
                }

                // Режим 2: Гибрид — сохраняем и фразу, и сгенерированный пароль
                data.isMnemonic && data.isStoredInVault -> {
                    val phrase = data.mnemonicPhrase ?: run {
                        _vaultError.value = "Фраза обязательна для гибридного режима"
                        return@launch
                    }

                    // Генерируем пароль через движок
                    val mnemonicPhrase = MnemonicPhrase(phrase)
                    val generationResult = passwordGenerator.generatePassword(
                        phrase = mnemonicPhrase,
                        salt = saltBytes,
                        length = data.passwordLength,
                        mode = data.mode,
                        useLeetSpeak = true
                    )

                    // Шифруем и фразу, и пароль
                    encryptedPassword = cryptoEngine.encryptPassword(generationResult.password, masterPassword!!)
                    encryptedPhrase = cryptoEngine.encryptPassword(phrase, masterPassword)

                    // Крипто-гигиена
                    mnemonicPhrase.close()
                    generationResult.close()
                }

                // Режим 3: Vault — шифруем введённый пароль
                !data.isMnemonic -> {
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

            // Крипто-гигиена: затираем все секреты
            data.clearSensitiveData()
            masterPasswordForEncryption?.fill('\u0000')
            masterPasswordForEncryption = null
            saltBytes.fill(0)

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
                _vaultError.value = reportMessage
            } else {
                _vaultError.value = reportMessage
            }
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
        // затираем мастер-пароль при уничтожении ViewModel
        masterPasswordForEncryption?.fill('\u0000')
        masterPasswordForEncryption = null
    }
}
