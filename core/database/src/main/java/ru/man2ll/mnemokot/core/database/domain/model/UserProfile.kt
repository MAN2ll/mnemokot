package ru.man2ll.mnemokot.core.database.domain.model

data class UserProfile(
    val id: Long,
    val profileName: String,
    val hasPin: Boolean,
    val failedAttempts: Int,
    val maxAttemptsBeforeDelete: Int
)
