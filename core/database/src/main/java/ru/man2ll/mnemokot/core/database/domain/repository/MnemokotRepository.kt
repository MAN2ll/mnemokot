package ru.man2ll.mnemokot.core.database.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.man2ll.mnemokot.core.database.domain.model.Account
import ru.man2ll.mnemokot.core.database.domain.model.UserProfile

interface MnemokotRepository {
    // Операции с профилями
    fun getProfiles(): Flow<List<UserProfile>>
    suspend fun createProfile(name: String, pin: String?): Long
    suspend fun deleteProfile(id: Long)
    suspend fun verifyPin(profileId: Long, pin: String): Boolean
    
    // Новые методы Brute-force защиты
    suspend fun incrementFailedAttempts(profileId: Long): Int
    suspend fun resetFailedAttempts(profileId: Long)
    suspend fun getRemainingAttempts(profileId: Long): Int

    // Операции с аккаунтами
    fun getAccounts(profileId: Long): Flow<List<Account>>
    suspend fun getAccountById(id: Long): Account?
    suspend fun saveAccount(account: Account)
    suspend fun deleteAccount(id: Long)
    suspend fun getPasswordForAccount(accountId: Long, mnemonicPhrase: String?): CharArray
    suspend fun rotateSelectedPasswords(accountIds: List<Long>): Pair<Boolean, String>
}
