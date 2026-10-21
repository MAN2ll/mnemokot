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
import ru.man2ll.mnemokot.core.crypto.domain.hasher.PinHasher

@Singleton
class SecurityCryptoEngine @Inject constructor() : PinHasher {

    private val secureRandom = SecureRandom()

    override fun hashPin(pin: CharArray, salt: ByteArray): String {
        val spec: KeySpec = PBEKeySpec(pin, salt, 100000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hashBytes = factory.generateSecret(spec).encoded
        
        val base64Salt = Base64.encodeToString(salt, Base64.NO_WRAP)
        val base64Hash = Base64.encodeToString(hashBytes, Base64.NO_WRAP)
        val result = "$base64Salt:$base64Hash"
        
        hashBytes.fill(0)
        return result
    }

    override fun verifyPin(pin: CharArray, salt: ByteArray, expectedHash: String): Boolean {
        val parts = expectedHash.split(":")
        if (parts.size != 2) return false
        val expectedHashPart = parts[1]
        
        val spec: KeySpec = PBEKeySpec(pin, salt, 100000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val testHashBytes = factory.generateSecret(spec).encoded
        val testHash = Base64.encodeToString(testHashBytes, Base64.NO_WRAP)
        
        testHashBytes.fill(0)
        return testHash == expectedHashPart
    }

    fun verifyPinOld(pin: CharArray, storedHash: String): Boolean {
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

    fun encryptBackup(rawData: CharArray, password: CharArray): CharArray {
        val salt = ByteArray(16).also { secureRandom.nextBytes(it) }
        val iv = ByteArray(12).also { secureRandom.nextBytes(it) }
        
        val spec: KeySpec = PBEKeySpec(password, salt, 200000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyKeyBytes = factory.generateSecret(spec).encoded
        val secretKey = SecretKeySpec(keyKeyBytes, "AES")
        
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        
        val rawBytes = java.nio.charset.StandardCharsets.UTF_8.encode(java.nio.CharBuffer.wrap(rawData))
        val rawByteArray = ByteArray(rawBytes.remaining())
        rawBytes.get(rawByteArray)
        
        val encryptedBytes = cipher.doFinal(rawByteArray)
        
        val resultString = "${Base64.encodeToString(salt, Base64.NO_WRAP)}:${Base64.encodeToString(iv, Base64.NO_WRAP)}:${Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)}"
        val finalResult = resultString.toCharArray()
        
        keyKeyBytes.fill(0)
        salt.fill(0)
        iv.fill(0)
        rawByteArray.fill(0)
        encryptedBytes.fill(0)
        
        return finalResult
    }

    fun decryptBackup(encryptedPayload: CharArray, password: CharArray): CharArray {
        val payloadString = String(encryptedPayload)
        val parts = payloadString.split(":")
        if (parts.size != 3) throw IllegalArgumentException("Неверный формат резервной копии v3")
        
        val salt = Base64.decode(parts[0], Base64.NO_WRAP)
        val iv = Base64.decode(parts[1], Base64.NO_WRAP)
        val encryptedData = Base64.decode(parts[2], Base64.NO_WRAP)
        
        val spec: KeySpec = PBEKeySpec(password, salt, 200000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        val secretKey = SecretKeySpec(keyBytes, "AES")
        
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        
        val decryptedBytes = cipher.doFinal(encryptedData)
        val charBuffer = java.nio.charset.StandardCharsets.UTF_8.decode(java.nio.ByteBuffer.wrap(decryptedBytes))
        
        val finalResult = CharArray(charBuffer.remaining())
        charBuffer.get(finalResult)
        
        keyBytes.fill(0)
        salt.fill(0)
        iv.fill(0)
        encryptedData.fill(0)
        decryptedBytes.fill(0)
        
        return finalResult
    }
}
