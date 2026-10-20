package ru.man2ll.mnemokot.ui.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.man2ll.mnemokot.core.database.domain.session.SessionManager
import ru.man2ll.mnemokot.core.security.backup.BackupManager
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupManager: BackupManager,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _backupPayload = MutableStateFlow<CharArray?>(null)
    val backupPayload: StateFlow<CharArray?> = _backupPayload.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun exportData(password: CharArray) {
        val profileId = sessionManager.activeProfileId.value ?: return
        if (password.isEmpty()) return
        
        viewModelScope.launch {
            val payload = backupManager.exportBackupV3(profileId, password)
            _backupPayload.value = payload
            _statusMessage.value = "Экспорт бэкапа v3 успешно выполнен!"
        }
    }

    fun importData(payload: CharArray, password: CharArray) {
        val profileId = sessionManager.activeProfileId.value ?: return
        if (payload.isEmpty() || password.isEmpty()) return
        
        viewModelScope.launch {
            val success = backupManager.importBackupV3(profileId, payload, password)
            if (success) {
                _statusMessage.value = "Импорт данных успешно завершен! Новые записи добавлены."
                _backupPayload.value = null
            } else {
                _statusMessage.value = "Ошибка импорта. Неверный пароль или поврежденная копия."
            }
        }
    }

    fun clearBackupData() {
        _backupPayload.value?.fill('\u0000')
        _backupPayload.value = null
    }

    override fun onCleared() {
        super.onCleared()
        clearBackupData()
    }
}
