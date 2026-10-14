package ru.man2ll.mnemokot.core.database.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileName: String,
    val pinHash: String?,
    val failedAttempts: Int = 0,         // Счетчик неудачных вводов PIN
    val maxAttemptsBeforeDelete: Int = 5, // N попыток до полного уничтожения
    val createdAt: Long = System.currentTimeMillis()
)
