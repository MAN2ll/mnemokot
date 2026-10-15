package ru.man2ll.mnemokot.core.database.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "master_settings")
data class MasterSettingsEntity(
    @PrimaryKey val id: Int = 1, // Синглтон-строка настроек
    val masterPasswordHash: String?,
    val isBiometricEnabled: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
