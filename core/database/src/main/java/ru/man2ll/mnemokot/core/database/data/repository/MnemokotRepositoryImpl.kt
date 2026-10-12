package ru.man2ll.mnemokot.core.database.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.man2ll.mnemokot.core.crypto.domain.PasswordGenerator
import ru.man2ll.mnemokot.core.crypto.domain.model.MnemonicOptions
import ru.man2ll.mnemokot.core.database.data.dao.AccountDao
import ru.man2ll.mnemokot.core.database.data.dao.ProfileDao
import ru.man2ll.mnemokot.core.database.data.mapper.toDomain
import ru.man2ll.mnemokot.core.database.data.mapper.toEntity
import ru.man2ll.mnemokot.core.database.domain.model.Account
import ru.man2ll.mnemokot.core.database.domain.model.UserProfile
import ru.man2ll.mnemokot.core.database.domain.model.AccountEntity
import ru.man2ll.mnemokot.core.database.domain.model.UserProfileEntity
import ru.man2ll.mnemokot.core.database.domain.repository.MnemokotRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MnemokotRepositoryImpl @Inject constructor(
    private val profileDao: ProfileDao,
    private val accountDao: AccountDao,
    private val passwordGenerator: PasswordGenerator
) : MnemokotRepository {

    override fun getProfiles(): Flow<List<UserProfile>> {
        return profileDao.getAllProfilesFlow().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun createProfile(name: String, pin: String?): Long {
        val pinHash = pin?.hashCode()?.toString()
        val entity = UserProfileEntity(profileName = name, pinHash = pinHash)
        return profileDao.insertProfile(entity)
    }

    override suspend fun deleteProfile(id: Long) {
        val entity = profileDao.getProfileById(id)
        if (entity != null) {
            profileDao.deleteProfile(entity)
        }
    }

    override suspend fun verifyPin(profileId: Long, pin: String): Boolean {
        val profile = profileDao.getProfileById(profileId) ?: return false
        if (profile.pinHash == null) return true
        return profile.pinHash == pin.hashCode().toString()
    }

    override fun getAccounts(profileId: Long): Flow<List<Account>> {
        return accountDao.getAccountsByProfileFlow(profileId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getAccountById(id: Long): Account? {
        return accountDao.getAccountById(id)?.toDomain()
    }

    override suspend fun saveAccount(account: Account) {
        accountDao.insertAccount(account.toEntity())
    }

    override suspend fun deleteAccount(id: Long) {
        val entity = accountDao.getAccountById(id)
        if (entity != null) {
            accountDao.deleteAccount(entity)
        }
    }

    override suspend fun getPasswordForAccount(accountId: Long, mnemonicPhrase: String?): CharArray {
        val account = accountDao.getAccountById(accountId) ?: return charArrayOf()
        
        return if (account.isMnemonic) {
            if (mnemonicPhrase == null) return charArrayOf()
            
            val options = MnemonicOptions(
                length = account.passwordLength,
                salt = account.salt,
                serviceName = account.serviceName,
                rotationDate = account.rotationDate
            )
            passwordGenerator.generateFromMnemonic(mnemonicPhrase, options)
        } else {
            account.encryptedPassword?.toCharArray() ?: charArrayOf()
        }
    }
}
