package ru.man2ll.mnemokot.core.database.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.man2ll.mnemokot.core.database.domain.model.Account
import ru.man2ll.mnemokot.core.database.domain.model.UserProfile
import ru.man2ll.mnemokot.core.crypto.domain.model.MnemonicPhrase
import ru.man2ll.mnemokot.core.crypto.domain.model.GenerationResult

interface MnemokotRepository {
    fun getProfiles(): Flow<List<UserProfile>>
    suspend fun createProfile(name: String, pin: String?): Long
    suspend fun createProfile(name: String, pin: CharArray?): Long
    suspend fun deleteProfile(id: Long)
    suspend fun verifyPin(profileId: Long, pin: String): Boolean
    suspend fun verifyPin(profileId: Long, pin: CharArray): Boolean
    
    suspend fun incrementFailedAttempts(profileId: Long): Int
    suspend fun resetFailedAttempts(profileId: Long)
    suspend fun getRemainingAttempts(profileId: Long): Int

    fun getAccounts(profileId: Long): Flow<List<Account>>
    suspend fun getAccountById(id: Long): Account?
    suspend fun saveAccount(account: Account)
    suspend fun deleteAccount(id: Long)
    suspend fun getPasswordForAccount(accountId: Long, mnemonicPhrase: MnemonicPhrase?): GenerationResult
    
    suspend fun rotateSelectedPasswords(accountIds: List<Long>): Pair<Boolean, String>

    suspend fun isMasterPasswordSet(): Boolean
    suspend fun setupMasterPassword(passwordHash: String): Boolean
    suspend fun verifyMasterPassword(passwordHash: String): Boolean
    suspend fun isBiometricEnabled(): Boolean
    suspend fun setBiometricEnabled(enabled: Boolean)
}
