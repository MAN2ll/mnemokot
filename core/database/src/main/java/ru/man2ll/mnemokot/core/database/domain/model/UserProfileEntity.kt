package ru.man2ll.mnemokot.core.database.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileName: String,
    val pinHash: String?, // Хэш PIN-кода (если задан пользователем)
    val createdAt: Long = System.currentTimeMillis()
)
