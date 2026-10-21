package ru.man2ll.mnemokot.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import ru.man2ll.mnemokot.core.database.security.PassphraseProvider
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidPassphraseProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : PassphraseProvider {

    private val keyAlias = "MnemoKotDbKeyAlias"
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    init {
        if (!keyStore.containsAlias(keyAlias)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            keyGenerator.init(
                KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
            keyGenerator.generateKey()
        }
    }

    override fun getPassphrase(): ByteArray {
        val encryptedKeyFile = File(context.filesDir, "db_encrypted.key")
        val ivFile = File(context.filesDir, "db_iv.key")

        if (!encryptedKeyFile.exists() || !ivFile.exists()) {
            val rawKey = ByteArray(32)
            SecureRandom().nextBytes(rawKey)

            val secretKey = keyStore.getKey(keyAlias, null) as SecretKey
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            
            val encryptedKeyBytes = cipher.doFinal(rawKey)
            val ivBytes = cipher.iv

            encryptedKeyFile.writeText(Base64.encodeToString(encryptedKeyBytes, Base64.NO_WRAP))
            ivFile.writeText(Base64.encodeToString(ivBytes, Base64.NO_WRAP))
            
            return rawKey
        }

        val encryptedKeyBytes = Base64.decode(encryptedKeyFile.readText(), Base64.NO_WRAP)
        val ivBytes = Base64.decode(ivFile.readText(), Base64.NO_WRAP)

        val secretKey = keyStore.getKey(keyAlias, null) as SecretKey
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, ivBytes))
        
        return cipher.doFinal(encryptedKeyBytes)
    }
}
