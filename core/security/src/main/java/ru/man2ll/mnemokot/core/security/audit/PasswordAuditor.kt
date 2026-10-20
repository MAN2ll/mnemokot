package ru.man2ll.mnemokot.core.security.audit

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import ru.man2ll.mnemokot.core.database.domain.repository.MnemokotRepository
import ru.man2ll.mnemokot.core.security.audit.model.AuditResult
import ru.man2ll.mnemokot.core.security.audit.model.AuditWarning
import ru.man2ll.mnemokot.core.security.audit.model.WarningType
import ru.man2ll.mnemokot.core.crypto.domain.model.MnemonicPhrase
import java.security.MessageDigest
import android.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PasswordAuditor @Inject constructor(
    private val repository: MnemokotRepository
) {

    suspend fun performShallowAudit(profileId: Long): AuditResult = withContext(Dispatchers.Default) {
        val accounts = repository.getAccounts(profileId).first()
        val warnings = mutableListOf<AuditWarning>()
        
        val loginGroup = accounts.groupBy { "${it.serviceName.lowercase()}:${it.login.lowercase()}" }
        
        for (account in accounts) {
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
            duplicatePasswordsFound = 0,
            weakPasswordsFound = warnings.filter { it.type == WarningType.SHORT_PASSWORD }.size,
            warnings = warnings
        )
    }

    suspend fun performDeepAudit(profileId: Long, masterPhrase: CharArray?): AuditResult = withContext(Dispatchers.Default) {
        val accounts = repository.getAccounts(profileId).first()
        val warnings = mutableListOf<AuditWarning>()
        
        val hashToAccountsMap = mutableMapOf<String, MutableList<Long>>()
        
        val shallowResult = performShallowAudit(profileId)
        warnings.addAll(shallowResult.warnings)

        val digest = MessageDigest.getInstance("SHA-256")

        for (account in accounts) {
            val mnemonicPhrase = masterPhrase?.let { MnemonicPhrase(it.clone()) }
            val generationResult = repository.getPasswordForAccount(account.id, mnemonicPhrase)
            val passwordChars = generationResult.password
            
            if (passwordChars.isEmpty()) {
                mnemonicPhrase?.close()
                generationResult.close()
                continue
            }
            
            val byteBuffer = java.nio.charset.StandardCharsets.UTF_8.encode(java.nio.CharBuffer.wrap(passwordChars))
            val passwordBytes = ByteArray(byteBuffer.remaining())
            byteBuffer.get(passwordBytes)
            
            val hashedBytes = digest.digest(passwordBytes)
            val passwordHashStr = Base64.encodeToString(hashedBytes, Base64.NO_WRAP)

            if (!hashToAccountsMap.containsKey(passwordHashStr)) {
                hashToAccountsMap[passwordHashStr] = mutableListOf()
            }
            hashToAccountsMap[passwordHashStr]?.add(account.id)

            val uniqueCharsCount = passwordChars.toSet().size
            if (uniqueCharsCount < (passwordChars.size / 2).coerceAtLeast(4)) {
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
            
            passwordChars.fill('\u0000')
            passwordBytes.fill(0)
            hashedBytes.fill(0)
            mnemonicPhrase?.close()
            generationResult.close()
        }

        var duplicateCount = 0
        for ((_, accountIds) in hashToAccountsMap) {
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
            warnings = warnings.distinctBy { "${it.accountId}:${it.type}" }
        )
    }
}
