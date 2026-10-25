package ru.man2ll.mnemokot.core.database.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "accounts",
    foreignKeys = [
        ForeignKey(
            entity = UserProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["profileId"])]
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val serviceName: String,
    val login: String,
    val isMnemonic: Boolean,
    val passwordLength: Int = 16,
    val salt: String = "",
    val rotationDate: String = "",
    val encryptedPassword: String? = null,
    val encryptedMnemonicPhrase: String? = null, // ← НОВОЕ ПОЛЕ для гибридного режима
    val isDualUserMode: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis()
)
