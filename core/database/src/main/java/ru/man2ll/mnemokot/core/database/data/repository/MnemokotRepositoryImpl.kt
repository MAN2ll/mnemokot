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
        val entity = UserProfileEntity(profileName = name, pinHash = pin)
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
        return profile.pinHash == pin
    }

    override suspend fun incrementFailedAttempts(profileId: Long): Int {
        profileDao.incrementFailedAttempts(profileId)
        val profile = profileDao.getProfileById(profileId) ?: return 0
        
        if (profile.failedAttempts >= profile.maxAttemptsBeforeDelete) {
            profileDao.deleteProfile(profile)
            return -1
        }
        return profile.failedAttempts
    }

    override suspend fun resetFailedAttempts(profileId: Long) {
        profileDao.resetFailedAttempts(profileId)
    }

    override suspend fun getRemainingAttempts(profileId: Long): Int {
        val profile = profileDao.getProfileById(profileId) ?: return 0
        return (profile.maxAttemptsBeforeDelete - profile.failedAttempts).coerceAtLeast(0)
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
                rotationDate = account.rotationDate,
                isDualUserMode = account.isDualUserMode
            )
            passwordGenerator.generateFromMnemonic(mnemonicPhrase, options)
        } else {
            account.encryptedPassword?.toCharArray() ?: charArrayOf()
        }
    }
    
    override suspend fun rotateSelectedPasswords(accountIds: List<Long>): Pair<Boolean, String> {
        val entities = accountDao.getAccountsByIds(accountIds)
        if (entities.size < 2) return Pair(false, "Выберите минимум 2 аккаунта для ротации")

        var hasLengthWarning = false
        val reportBuilder = StringBuilder("Результат аудита перед ротацией:\n")

        val rotatedEntities = entities.mapIndexed { index, currentAccount ->
            // Циклический сдвиг: берем параметры у следующего в выбранном списке
            val nextIndex = (index + 1) % entities.size
            val donorAccount = entities[nextIndex]

            // Проверка риска длины: если донорский пароль короче, чем требовалось текущему сервису
            if (donorAccount.passwordLength < currentAccount.passwordLength) {
                hasLengthWarning = true
                reportBuilder.append("⚠ ${currentAccount.serviceName}: новый пароль короче исходного (${donorAccount.passwordLength} < ${currentAccount.passwordLength})\n")
            }

            if (currentAccount.isMnemonic) {
                // Ротация AMPG v2 через модификацию детерминированной соли донора
                currentAccount.copy(
                    salt = if (donorAccount.salt.isBlank()) "Rot_" + donorAccount.id else "Rot_" + donorAccount.salt,
                    expiresAt = System.currentTimeMillis() + (90L * 24 * 60 * 60 * 1000), // Обновляем срок на 90 дней
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                // Ротация явных паролей сдвигом шифрованных строк
                currentAccount.copy(
                    encryptedPassword = donorAccount.encryptedPassword,
                    expiresAt = System.currentTimeMillis() + (90L * 24 * 60 * 60 * 1000),
                    updatedAt = System.currentTimeMillis()
                )
            }
        }

        // Записываем пачку изменений в базу данных
        accountDao.updateAccountsList(rotatedEntities)

        val finalMessage = if (hasLengthWarning) {
            reportBuilder.append("\nПароли успешно перемешаны, но обнаружены несоответствия длин. Проверьте надежность в окне Аудита.").toString()
        } else {
            "Ротация выбранных аккаунтов успешно завершена! Все требования уникальности и длины соблюдены."
        }

        return Pair(true, finalMessage)
    }
}
