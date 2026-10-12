package ru.man2ll.mnemokot.core.security.backup

import org.json.JSONArray
import org.json.JSONObject
import ru.man2ll.mnemokot.core.database.domain.model.Account
import ru.man2ll.mnemokot.core.database.domain.repository.MnemokotRepository
import ru.man2ll.mnemokot.core.security.crypto.SecurityCryptoEngine
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class BackupManager @Inject constructor(
    private val repository: MnemokotRepository,
    private val cryptoEngine: SecurityCryptoEngine
) {

    // 1. Экспорт всех данных в зашифрованную строку Backup v3
    suspend fun exportBackupV3(profileId: Long, password: CharArray): String {
        val accountsList = repository.getAccounts(profileId).first()
        
        // Формируем внутреннюю структуру JSON
        val rootJson = JSONObject()
        rootJson.put("version", 3)
        rootJson.put("exportTimestamp", System.currentTimeMillis())
        
        val accountsArray = JSONArray()
        for (acc in accountsList) {
            val accJson = JSONObject().apply {
                put("serviceName", acc.serviceName)
                put("login", acc.login)
                put("isMnemonic", acc.isMnemonic)
                put("passwordLength", acc.passwordLength)
                put("salt", acc.salt)
                put("rotationDate", acc.rotationDate)
                put("explicitPassword", acc.explicitPassword ?: "")
            }
            accountsArray.put(accJson)
        }
        rootJson.put("accounts", accountsArray)
        
        // Шифруем полученный JSON-текст через PBKDF2 + AES-GCM-256
        return cryptoEngine.encryptBackup(rootJson.toString(), password)
    }

    // 2. Импорт зашифрованной резервной копии v3 в базу данных
    suspend fun importBackupV3(profileId: Long, encryptedPayload: String, password: CharArray): Boolean {
        return try {
            // Расшифровываем строку бэкапа
            val decryptedJsonString = cryptoEngine.decryptBackup(encryptedPayload, password)
            val rootJson = JSONObject(decryptedJsonString)
            
            // Проверяем версию формата резервной копии
            val version = rootJson.optInt("version", -1)
            if (version != 3) return false
            
            val accountsArray = rootJson.getJSONArray("accounts")
            
            // Читаем существующие аккаунты профиля, чтобы избежать жестких дубликатов
            val existingAccounts = repository.getAccounts(profileId).first()

            for (i in 0 until accountsArray.length()) {
                val accJson = accountsArray.getJSONObject(i)
                val serviceName = accJson.getString("serviceName")
                val login = accJson.getString("login")
                
                // Проверяем, существует ли уже такая учетная запись в профиле
                val isDuplicate = existingAccounts.any { 
                    it.serviceName.equals(serviceName, ignoreCase = true) && it.login.equals(login, ignoreCase = true) 
                }
                
                // Если это не дубликат, производим мягкое слияние (Добавление новых записей)
                if (!isDuplicate) {
                    val account = Account(
                        id = 0, // Указываем 0, чтобы Room сгенерировал ID автоматически
                        profileId = profileId,
                        serviceName = serviceName,
                        login = login,
                        isMnemonic = accJson.getBoolean("isMnemonic"),
                        passwordLength = accJson.getInt("passwordLength"),
                        salt = accJson.optString("salt", ""),
                        rotationDate = accJson.optString("rotationDate", ""),
                        explicitPassword = accJson.optString("explicitPassword", "").takeIf { it.isNotEmpty() }
                    )
                    repository.saveAccount(account)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false // Если неверный пароль или поврежден файл — возвращаем ошибку импорта
        }
    }
}
