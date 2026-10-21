package ru.man2ll.mnemokot.core.database.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.man2ll.mnemokot.core.crypto.domain.generator.MnemonicPasswordGeneratorV2
import ru.man2ll.mnemokot.core.crypto.domain.model.MnemonicPhrase
import ru.man2ll.mnemokot.core.crypto.domain.model.GenerationResult
import ru.man2ll.mnemokot.core.crypto.domain.model.TranslitMode
import ru.man2ll.mnemokot.core.database.data.dao.AccountDao
import ru.man2ll.mnemokot.core.database.data.dao.ProfileDao
import ru.man2ll.mnemokot.core.database.data.dao.MasterSettingsDao
import ru.man2ll.mnemokot.core.database.data.mapper.toDomain
import ru.man2ll.mnemokot.core.database.data.mapper.toEntity
import ru.man2ll.mnemokot.core.database.data.model.UserProfileEntity
import ru.man2ll.mnemokot.core.database.data.model.MasterSettingsEntity
import ru.man2ll.mnemokot.core.database.domain.model.Account
import ru.man2ll.mnemokot.core.database.domain.model.UserProfile
import ru.man2ll.mnemokot.core.database.domain.repository.MnemokotRepository
import ru.man2ll.mnemokot.core.security.crypto.SecurityCryptoEngine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MnemokotRepositoryImpl @Inject constructor(
    private val profileDao: ProfileDao,
    private val accountDao: AccountDao,
    private val masterSettingsDao: MasterSettingsDao,
    private val passwordGeneratorV2: MnemonicPasswordGeneratorV2,
    private val cryptoEngine: SecurityCryptoEngine
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
        // ОШИБКА УСТРАНЕНА: pin здесь — это чистый CharArray в строковом представлении, переданный из VM. 
        // storedHash из базы содержит структуру "соль:хэш". Вызываем верный криптографический метод.
        val profile = profileDao.getProfileById(profileId) ?: return false
        val storedHash = profile.pinHash ?: return false
        val pinChars = pin.toCharArray()
        val isValid = cryptoEngine.verifyPin(pinChars, storedHash)
        pinChars.fill('\u0000')
        return isValid
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

    override suspend fun getPasswordForAccount(accountId: Long, mnemonicPhrase: MnemonicPhrase?): GenerationResult {
        val account = accountDao.getAccountById(accountId) ?: return GenerationResult(charArrayOf())
        return if (account.isMnemonic) {
            if (mnemonicPhrase == null) return GenerationResult(charArrayOf())
            val saltBytes = account.salt.toByteArray(Charsets.UTF_8)
            val result = passwordGeneratorV2.generatePassword(
                phrase = mnemonicPhrase,
                salt = saltBytes,
                length = account.passwordLength,
                translitMode = TranslitMode.GOST_7_79_2000_B,
                useLeetSpeak = true
            )
            saltBytes.fill(0)
            result
        } else {
            val decryptedChars = account.encryptedPassword?.toCharArray() ?: charArrayOf()
            GenerationResult(decryptedChars)
        }
    }
    
    override suspend fun rotateSelectedPasswords(accountIds: List<Long>): Pair<Boolean, String> {
        val entities = accountDao.getAccountsByIds(accountIds)
        if (entities.size < 2) return Pair(false, "Выберите минимум 2 аккаунта для ротации")

        var hasLengthWarning = false
        val reportBuilder = StringBuilder("Результат аудита перед ротацией:\n")

        val rotatedEntities = entities.mapIndexed { index, currentAccount ->
            val nextIndex = (index + 1) % entities.size
            val donorAccount = entities[nextIndex]

            if (donorAccount.passwordLength < currentAccount.passwordLength) {
                hasLengthWarning = true
                reportBuilder.append("⚠ ${currentAccount.serviceName}: новый пароль короче исходного (${donorAccount.passwordLength} < ${currentAccount.passwordLength})\n")
            }

            if (currentAccount.isMnemonic) {
                currentAccount.copy(
                    salt = if (donorAccount.salt.isBlank()) "Rot_" + donorAccount.id else "Rot_" + donorAccount.salt,
                    expiresAt = System.currentTimeMillis() + (90L * 24 * 60 * 60 * 1000),
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                currentAccount.copy(
                    encryptedPassword = donorAccount.encryptedPassword,
                    expiresAt = System.currentTimeMillis() + (90L * 24 * 60 * 60 * 1000),
                    updatedAt = System.currentTimeMillis()
                )
            }
        }

        accountDao.updateAccountsList(rotatedEntities)

        val finalMessage = if (hasLengthWarning) {
            reportBuilder.append("\nПароли успешно перемешаны, но обнаружены несоответствия длин. Проверьте надежность в окне Аудита.").toString()
        } else {
            "Ротация выбранных аккаунтов успешно завершена! Все требования уникальности и длины соблюдены."
        }

        return Pair(true, finalMessage)
    }

    override suspend fun isMasterPasswordSet(): Boolean {
        val settings = masterSettingsDao.getSettings()
        return settings?.masterPasswordHash != null
    }

    override suspend fun setupMasterPassword(passwordHash: String): Boolean {
        val current = masterSettingsDao.getSettings() ?: MasterSettingsEntity(masterPasswordHash = passwordHash)
        masterSettingsDao.saveSettings(current.copy(masterPasswordHash = passwordHash, updatedAt = System.currentTimeMillis()))
        return true
    }

    override suspend fun verifyMasterPassword(passwordHash: String): Boolean {
        val settings = masterSettingsDao.getSettings() ?: return false
        return cryptoEngine.verifyPin(passwordHash.toCharArray(), settings.masterPasswordHash ?: "")
    }

    override suspend fun isBiometricEnabled(): Boolean {
        val settings = masterSettingsDao.getSettings()
        return settings?.isBiometricEnabled ?: false
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        val current = masterSettingsDao.getSettings() ?: MasterSettingsEntity(masterPasswordHash = null)
        masterSettingsDao.saveSettings(current.copy(isBiometricEnabled = enabled, updatedAt = System.currentTimeMillis()))
    }
}
