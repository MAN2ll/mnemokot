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

    suspend fun exportBackupV3(profileId: Long, password: CharArray): CharArray {
        val accountsList = repository.getAccounts(profileId).first()
        
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
        
        val jsonString = rootJson.toString()
        val jsonChars = jsonString.toCharArray()
        
        val encryptedBackup = cryptoEngine.encryptBackup(jsonChars, password)
        
        jsonChars.fill('\u0000')
        return encryptedBackup
    }

    suspend fun importBackupV3(profileId: Long, encryptedPayload: CharArray, password: CharArray): Boolean {
        return try {
            val decryptedChars = cryptoEngine.decryptBackup(encryptedPayload, password)
            val decryptedJsonString = String(decryptedChars)
            decryptedChars.fill('\u0000')
            
            val rootJson = JSONObject(decryptedJsonString)
            val version = rootJson.optInt("version", -1)
            if (version != 3) return false
            
            val accountsArray = rootJson.getJSONArray("accounts")
            val existingAccounts = repository.getAccounts(profileId).first()

            for (i in 0 until accountsArray.length()) {
                val accJson = accountsArray.getJSONObject(i)
                val serviceName = accJson.getString("serviceName")
                val login = accJson.getString("login")
                
                val isDuplicate = existingAccounts.any { 
                    it.serviceName.equals(serviceName, ignoreCase = true) && it.login.equals(login, ignoreCase = true) 
                }
                
                if (!isDuplicate) {
                    val account = Account(
                        id = 0,
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
            false
        }
    }
}
