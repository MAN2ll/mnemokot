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

    // Операции с аккаунтами
    fun getAccounts(profileId: Long): Flow<List<Account>>
    suspend fun getAccountById(id: Long): Account?
    suspend fun saveAccount(account: Account)
    suspend fun deleteAccount(id: Long)
    
    // Ключевой метод: получить готовый пароль (сгенерировать на лету или расшифровать)
    suspend fun getPasswordForAccount(accountId: Long, mnemonicPhrase: String?): CharArray
}
