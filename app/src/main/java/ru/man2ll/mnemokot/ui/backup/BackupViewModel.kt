package ru.man2ll.mnemokot.ui.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.man2ll.mnemokot.core.security.backup.BackupManager
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupManager: BackupManager
) : ViewModel() {

    private val _backupPayload = MutableStateFlow<String?>(null)
    val backupPayload: StateFlow<String?> = _backupPayload.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val defaultProfileId = 1L

    fun exportData(password: String) {
        if (password.isBlank()) return
        viewModelScope.launch {
            val passChars = password.toCharArray()
            val payload = backupManager.exportBackupV3(defaultProfileId, passChars)
            passChars.fill(' ')
            _backupPayload.value = payload
            _statusMessage.value = "Экспорт бэкапа v3 успешно выполнен!"
        }
    }

    fun importData(payload: String, password: String) {
        if (payload.isBlank() || password.isBlank()) return
        viewModelScope.launch {
            val passChars = password.toCharArray()
            val success = backupManager.importBackupV3(defaultProfileId, payload, passChars)
            passChars.fill(' ')
            if (success) {
                _statusMessage.value = "Импорт данных успешно завершен! Новые записи добавлены."
                _backupPayload.value = null
            } else {
                _statusMessage.value = "Ошибка импорта. Неверный пароль или поврежденная копия."
            }
        }
    }
}
