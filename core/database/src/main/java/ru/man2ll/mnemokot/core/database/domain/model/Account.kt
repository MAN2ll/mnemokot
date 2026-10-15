package ru.man2ll.mnemokot.core.database.domain.model

data class Account(
    val id: Long,
    val profileId: Long,
    val serviceName: String,
    val login: String,
    val isMnemonic: Boolean,
    val passwordLength: Int,
    val salt: String,
    val rotationDate: String,
    val explicitPassword: String?,
    val isDualUserMode: Boolean = false,
    // Добавляем новые метрики на уровень Domain
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = 0L
)
