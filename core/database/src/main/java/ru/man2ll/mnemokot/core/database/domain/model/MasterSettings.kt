package ru.man2ll.mnemokot.core.database.domain.model

data class MasterSettings(
    val id: Long = 1,
    val masterPasswordHash: String?,
    val masterPasswordSalt: String?,
    val isBiometricEnabled: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
