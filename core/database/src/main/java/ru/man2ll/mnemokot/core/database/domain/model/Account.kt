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
    // Переносим флаг на уровень бизнес-логики
    val isDualUserMode: Boolean = false
)
