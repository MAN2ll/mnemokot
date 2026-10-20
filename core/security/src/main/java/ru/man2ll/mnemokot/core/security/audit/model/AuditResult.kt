package ru.man2ll.mnemokot.core.security.audit.model

enum class WarningType {
    SHORT_PASSWORD,
    DUPLICATE_LOGIN,
    LOW_CHARACTER_DIVERSITY,
    REUSED_PASSWORD
}

data class AuditWarning(
    val accountId: Long,
    val serviceName: String,
    val login: String,
    val type: WarningType,
    val message: String
)

data class AuditResult(
    val totalAccountsChecked: Int,
    val duplicatePasswordsFound: Int,
    val weakPasswordsFound: Int,
    val warnings: List<AuditWarning>
)
