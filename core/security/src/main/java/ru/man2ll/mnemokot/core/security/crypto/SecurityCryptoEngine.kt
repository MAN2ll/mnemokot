package ru.man2ll.mnemokot.core.security.crypto

import android.util.Base64
import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityCryptoEngine @Inject constructor() {

    private val secureRandom = SecureRandom()

    // 1. Хэширование PIN-кода (PBKDF2, 100 000 итераций)
    fun hashPin(pin: CharArray, customSalt: ByteArray? = null): String {
        val salt = customSalt ?: ByteArray(16).also { secureRandom.nextBytes(it) }
        val spec: KeySpec = PBEKeySpec(pin, salt, 100000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        
        val result = "${Base64.encodeToString(salt, Base64.NO_WRAP)}:${Base64.encodeToString(hash, Base64.NO_WRAP)}"
        
        // Очистка памяти
        hash.fill(0)
        return result
    }

    fun verifyPin(pin: CharArray, storedHash: String): Boolean {
        val parts = storedHash.split(":")
        if (parts.size != 2) return false
        val salt = Base64.decode(parts[0], Base64.NO_WRAP)
        val expectedHash = parts[1]
        
        val spec: KeySpec = PBEKeySpec(pin, salt, 100000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val testHashBytes = factory.generateSecret(spec).encoded
        val testHash = Base64.encodeToString(testHashBytes, Base64.NO_WRAP)
        
        testHashBytes.fill(0)
        return testHash == expectedHash
    }

    // 2. Шифрование резервных копий Backup v3 (AES-256-GCM + PBKDF2 200 000 итераций)
    fun encryptBackup(rawData: String, password: CharArray): String {
        val salt = ByteArray(16).also { secureRandom.nextBytes(it) }
        val iv = ByteArray(12).also { secureRandom.nextBytes(it) } // 12 байт стандартно для GCM
        
        // Формируем ключ на основе пароля бэкапа
        val spec: KeySpec = PBEKeySpec(password, salt, 200000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val tmp = factory.generateSecret(spec)
        val secretKey = SecretKeySpec(tmp.encoded, "AES")
        
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        
        val encryptedBytes = cipher.doByteArray(rawData.toByteArray(Charsets.UTF_8))
        
        // Сборка монолитной строки бэкапа: SALT:IV:ENCRYPTED_DATA
        return "${Base64.encodeToString(salt, Base64.NO_WRAP)}:${Base64.encodeToString(iv, Base64.NO_WRAP)}:${Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)}"
    }

    fun decryptBackup(encryptedPayload: String, password: CharArray): String {
        val parts = encryptedPayload.split(":")
        if (parts.size != 3) throw IllegalArgumentException("Неверный формат резервной копии v3")
        
        val salt = Base64.decode(parts[0], Base64.NO_WRAP)
        val iv = Base64.decode(parts[1], Base64.NO_WRAP)
        val encryptedData = Base64.decode(parts[2], Base64.NO_WRAP)
        
        val spec: KeySpec = PBEKeySpec(password, salt, 200000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val tmp = factory.generateSecret(spec)
        val secretKey = SecretKeySpec(tmp.encoded, "AES")
        
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        
        val decryptedBytes = cipher.doByteArray(encryptedData)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    private fun Cipher.doByteArray(data: ByteArray): ByteArray = this.doFinal(data)
}
