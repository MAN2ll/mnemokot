package ru.man2ll.mnemokot.ui.audit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.man2ll.mnemokot.core.security.audit.PasswordAuditor
import ru.man2ll.mnemokot.core.security.audit.model.AuditResult
import javax.inject.Inject

@HiltViewModel
class AuditViewModel @Inject constructor(
    private val passwordAuditor: PasswordAuditor
) : ViewModel() {

    private val _auditResult = MutableStateFlow<AuditResult?>(null)
    val auditResult: StateFlow<AuditResult?> = _auditResult.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val defaultProfileId = 1L

    fun runShallowAudit() {
        viewModelScope.launch {
            _isLoading.value = true
            _auditResult.value = passwordAuditor.performShallowAudit(defaultProfileId)
            _isLoading.value = false
        }
    }

    fun runDeepAudit(mnemonicPhrase: String) {
        if (mnemonicPhrase.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            _auditResult.value = passwordAuditor.performDeepAudit(defaultProfileId, mnemonicPhrase)
            _isLoading.value = false
        }
    }
}
