package ru.man2ll.mnemokot.core.database.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileName: String,
    val pinHash: String?,
    val pinSalt: String?,
    val failedAttempts: Int = 0,
    val maxAttemptsBeforeDelete: Int = 5,
    val createdAt: Long = System.currentTimeMillis()
)
