package ru.man2ll.mnemokot.core.database.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "master_settings")
data class MasterSettingsEntity(
    @PrimaryKey val id: Long = 1,
    val masterPasswordHash: String?,
    val masterPasswordSalt: String?,
    val isBiometricEnabled: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
