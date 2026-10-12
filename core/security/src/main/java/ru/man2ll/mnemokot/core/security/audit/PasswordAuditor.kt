package ru.man2ll.mnemokot.core.security.audit

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import ru.man2ll.mnemokot.core.database.domain.repository.MnemokotRepository
import ru.man2ll.mnemokot.core.security.audit.model.AuditResult
import ru.man2ll.mnemokot.core.security.audit.model.AuditWarning
import ru.man2ll.mnemokot.core.security.audit.model.WarningType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PasswordAuditor @Inject constructor(
    private val repository: MnemokotRepository
) {

    // 1. Поверхностный аудит (Быстрый, не требует мастер-фразы, проверяет только метаданные)
    suspend fun performShallowAudit(profileId: Long): AuditResult = withContext(Dispatchers.Default) {
        val accounts = repository.getAccounts(profileId).first()
        val warnings = mutableListOf<AuditWarning>()
        
        // Группируем по паре Сервис + Логин для поиска дубликатов учеток
        val loginGroup = accounts.groupBy { "${it.serviceName.lowercase()}:${it.login.lowercase()}" }
        
        for (account in accounts) {
            // Проверка на слишком короткий размер из метаданных
            if (account.passwordLength < 12) {
                warnings.add(
                    AuditWarning(
                        accountId = account.id,
                        serviceName = account.serviceName,
                        login = account.login,
                        type = WarningType.SHORT_PASSWORD,
                        message = "Длина пароля меньше рекомендуемых 12 символов."
                    )
                )
            }
            
            // Проверка на дублирование логинов на одном сервисе
            val key = "${account.serviceName.lowercase()}:${account.login.lowercase()}"
            if ((loginGroup[key]?.size ?: 0) > 1) {
                warnings.add(
                    AuditWarning(
                        accountId = account.id,
                        serviceName = account.serviceName,
                        login = account.login,
                        type = WarningType.DUPLICATE_LOGIN,
                        message = "Обнаружено несколько одинаковых логинов для этого сервиса."
                    )
                )
            }
        }

        return@withContext AuditResult(
            totalAccountsChecked = accounts.size,
            duplicatePasswordsFound = 0, // Невозможно определить без расшифровки
            weakPasswordsFound = warnings.filter { it.type == WarningType.SHORT_PASSWORD }.size,
            warnings = warnings
        )
    }

    // 2. Глубокий аудит (Требует мастер-фразу, расшифровывает или генерирует пароли в памяти для анализа)
    suspend fun performDeepAudit(profileId: Long, mnemonicPhrase: String?): AuditResult = withContext(Dispatchers.Default) {
        val accounts = repository.getAccounts(profileId).first()
        val warnings = mutableListOf<AuditWarning>()
        
        // Временная мапа в памяти для хранения сгенерированных/дешифрованных паролей ради поиска сквозных дубликатов
        // Ключ: Строка пароля, Значение: Список аккаунтов, использующих его
        val passwordToAccountsMap = mutableMapOf<String, MutableList<Long>>()
        
        // Сначала выполняем базовый поверхностный аудит, чтобы собрать первичные предупреждения
        val shallowResult = performShallowAudit(profileId)
        warnings.addAll(shallowResult.warnings)

        for (account in accounts) {
            // Генерируем или расшировываем пароль на лету через репозиторий
            val passwordChars = repository.getPasswordForAccount(account.id, mnemonicPhrase)
            if (passwordChars.isEmpty()) continue
            
            val passwordStr = String(passwordChars)
            
            // Заполняем карту перекрестных дубликатов
            if (!passwordToAccountsMap.containsKey(passwordStr)) {
                passwordToAccountsMap[passwordStr] = mutableListOf()
            }
            passwordToAccountsMap[passwordStr]?.add(account.id)

            // Проверка уникальности символов в пароле (критерий сложности для AMPG v2)
            val uniqueCharsCount = passwordStr.toSet().size
            if (uniqueCharsCount < (passwordStr.length / 2).coerceAtLeast(4)) {
                warnings.add(
                    AuditWarning(
                        accountId = account.id,
                        serviceName = account.serviceName,
                        login = account.login,
                        type = WarningType.LOW_CHARACTER_DIVERSITY,
                        message = "Низкое разнообразие символов. Пароль может быть предсказуем."
                    )
                )
            }
            
            // Незамедлительно стираем строку из памяти
            passwordChars.fill(' ')
        }

        // Выявляем перекрестные дубликаты (использование одинаковых паролей на разных сервисах)
        var duplicateCount = 0
        for ((_, accountIds) in passwordToAccountsMap) {
            if (accountIds.size > 1) {
                duplicateCount += accountIds.size
                for (id in accountIds) {
                    val targetAcc = accounts.firstOrNull { it.id == id }
                    if (targetAcc != null) {
                        warnings.add(
                            AuditWarning(
                                accountId = targetAcc.id,
                                serviceName = targetAcc.serviceName,
                                login = targetAcc.login,
                                type = WarningType.REUSED_PASSWORD,
                                message = "Внимание! Этот пароль повторно используется на других учетных записях."
                            )
                        )
                    }
                }
            }
        }

        return@withContext AuditResult(
            totalAccountsChecked = accounts.size,
            duplicatePasswordsFound = duplicateCount,
            weakPasswordsFound = warnings.count { it.type == WarningType.SHORT_PASSWORD || it.type == WarningType.LOW_CHARACTER_DIVERSITY },
            warnings = warnings.distinctBy { "${it.accountId}:${it.type}" } // Исключаем дубли предупреждений
        )
    }
}
