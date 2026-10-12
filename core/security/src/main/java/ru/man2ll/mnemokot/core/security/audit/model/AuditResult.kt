package ru.man2ll.mnemokot.core.security.audit.model

data class AuditResult(
    val totalAccountsChecked: Int,
    val duplicatePasswordsFound: Int,
    val weakPasswordsFound: Int,
    val warnings: List<AuditWarning>
)

data class AuditWarning(
    val accountId: Long,
    val serviceName: String,
    val login: String,
    val type: WarningType,
    val message: String
)

enum class WarningType {
    REUSED_PASSWORD,      // Один и тот же пароль используется на разных сервисах
    SHORT_PASSWORD,       // Пароль слишком короткий (меньше 12 символов)
    DUPLICATE_LOGIN,      // Одинаковый логин на одном и том же сервисе
    LOW_CHARACTER_DIVERSITY // Недостаточно уникальных символов в пароле
}
