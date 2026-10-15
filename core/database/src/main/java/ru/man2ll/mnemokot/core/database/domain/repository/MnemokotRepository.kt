package ru.man2ll.mnemokot.core.database.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.man2ll.mnemokot.core.database.domain.model.Account
import ru.man2ll.mnemokot.core.database.domain.model.UserProfile

interface MnemokotRepository {
    // Операции с профилями пользователей
    fun getProfiles(): Flow<List<UserProfile>>
    suspend fun createProfile(name: String, pin: String?): Long
    suspend fun deleteProfile(id: Long)
    suspend fun verifyPin(profileId: Long, pin: String): Boolean
    
    // Менеджмент Brute-force защиты профилей
    suspend fun incrementFailedAttempts(profileId: Long): Int
    suspend fun resetFailedAttempts(profileId: Long)
    suspend fun getRemainingAttempts(profileId: Long): Int

    // Операции с учетными записями (Журнал паролей)
    fun getAccounts(profileId: Long): Flow<List<Account>>
    suspend fun getAccountById(id: Long): Account?
    suspend fun saveAccount(account: Account)
    suspend fun deleteAccount(id: Long)
    suspend fun getPasswordForAccount(accountId: Long, mnemonicPhrase: String?): CharArray
    
    // Интеллектуальный модуль выборочной ротации
    suspend fun rotateSelectedPasswords(accountIds: List<Long>): Pair<Boolean, String>

    // ГЛОБАЛЬНЫЙ МАСТЕР-ПАРОЛЬ И БИОМЕТРИЯ
    suspend fun isMasterPasswordSet(): Boolean
    suspend fun setupMasterPassword(passwordHash: String): Boolean
    suspend fun verifyMasterPassword(passwordHash: String): Boolean
    suspend fun isBiometricEnabled(): Boolean
    suspend fun setBiometricEnabled(enabled: Boolean)

    // ЭКСТРЕННЫЙ ВЫГРУЗОЧНЫЙ КОНВЕРТ БЕЗОПАСНОСТИ (.TXT / .CSV)
    suspend fun generateSecurityEnvelopeText(accountIds: List<Long>): String
}
