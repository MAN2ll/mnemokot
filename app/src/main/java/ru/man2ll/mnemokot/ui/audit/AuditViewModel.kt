package ru.man2ll.mnemokot.ui.audit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.man2ll.mnemokot.core.database.domain.session.SessionManager
import ru.man2ll.mnemokot.core.security.audit.PasswordAuditor
import ru.man2ll.mnemokot.core.security.audit.model.AuditResult
import javax.inject.Inject

@HiltViewModel
class AuditViewModel @Inject constructor(
    private val passwordAuditor: PasswordAuditor,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _auditResult = MutableStateFlow<AuditResult?>(null)
    val auditResult: StateFlow<AuditResult?> = _auditResult.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun runShallowAudit() {
        val profileId = sessionManager.activeProfileId.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _auditResult.value = passwordAuditor.performShallowAudit(profileId)
            _isLoading.value = false
        }
    }

    fun runDeepAudit(mnemonicPhrase: String) {
        val profileId = sessionManager.activeProfileId.value ?: return
        if (mnemonicPhrase.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            _auditResult.value = passwordAuditor.performDeepAudit(profileId, mnemonicPhrase)
            _isLoading.value = false
        }
    }
}
