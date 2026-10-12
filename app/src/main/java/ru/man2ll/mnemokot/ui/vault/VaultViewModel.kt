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
import javax.inject.Inject

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val repository: MnemokotRepository
) : ViewModel() {

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    private val _calculatedPassword = MutableStateFlow<String?>(null)
    val calculatedPassword: StateFlow<String?> = _calculatedPassword.asStateFlow()

    // Исправлено: теперь используется строго корректный синтаксис суффикса Long в Kotlin
    private val defaultProfileId = 1L

    init {
        loadAccounts()
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            repository.getAccounts(defaultProfileId).collectLatest { list ->
                _accounts.value = list
            }
        }
    }

    fun onAddAccount(
        serviceName: String,
        login: String,
        isMnemonic: Boolean,
        length: Int,
        salt: String,
        rotationDate: String,
        explicitPass: String?
    ) {
        viewModelScope.launch {
            val account = Account(
                id = 0,
                profileId = defaultProfileId,
                serviceName = serviceName,
                login = login,
                isMnemonic = isMnemonic,
                passwordLength = length,
                salt = salt,
                rotationDate = rotationDate,
                explicitPassword = explicitPass
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
        viewModelScope.launch {
            repository.deleteAccount(id)
        }
    }
}
