package ru.man2ll.mnemokot.core.database.data.mapper

import ru.man2ll.mnemokot.core.database.domain.model.Account
import ru.man2ll.mnemokot.core.database.domain.model.UserProfile
import ru.man2ll.mnemokot.core.database.domain.model.AccountEntity
import ru.man2ll.mnemokot.core.database.domain.model.UserProfileEntity

fun UserProfileEntity.toDomain() = UserProfile(
    id = id,
    profileName = profileName,
    hasPin = pinHash != null,
    failedAttempts = failedAttempts,
    maxAttemptsBeforeDelete = maxAttemptsBeforeDelete
)

fun AccountEntity.toDomain() = Account(
    id = id,
    profileId = profileId,
    serviceName = serviceName,
    login = login,
    isMnemonic = isMnemonic,
    passwordLength = passwordLength,
    salt = salt,
    rotationDate = rotationDate,
    explicitPassword = encryptedPassword,
    isDualUserMode = isDualUserMode
)

fun Account.toEntity() = AccountEntity(
    id = id,
    profileId = profileId,
    serviceName = serviceName,
    login = login,
    isMnemonic = isMnemonic,
    passwordLength = passwordLength,
    salt = salt,
    rotationDate = rotationDate,
    encryptedPassword = explicitPassword,
    isDualUserMode = isDualUserMode
)
