package ru.man2ll.mnemokot.core.database.data.mapper

import ru.man2ll.mnemokot.core.database.data.model.AccountEntity
import ru.man2ll.mnemokot.core.database.data.model.UserProfileEntity
import ru.man2ll.mnemokot.core.database.data.model.MasterSettingsEntity
import ru.man2ll.mnemokot.core.database.domain.model.Account
import ru.man2ll.mnemokot.core.database.domain.model.UserProfile
import ru.man2ll.mnemokot.core.database.domain.model.MasterSettings

fun AccountEntity.toDomain(): Account {
    return Account(
        id = id,
        profileId = profileId,
        serviceName = serviceName,
        login = login,
        isMnemonic = isMnemonic,
        passwordLength = passwordLength,
        salt = salt,
        rotationDate = rotationDate,
        explicitPassword = encryptedPassword,
        encryptedMnemonicPhrase = encryptedMnemonicPhrase, // ← МАППИНГ НОВОГО ПОЛЯ
        isDualUserMode = isDualUserMode,
        createdAt = createdAt,
        expiresAt = expiresAt,
        updatedAt = updatedAt
    )
}

fun Account.toEntity(): AccountEntity {
    return AccountEntity(
        id = id,
        profileId = profileId,
        serviceName = serviceName,
        login = login,
        isMnemonic = isMnemonic,
        passwordLength = passwordLength,
        salt = salt,
        rotationDate = rotationDate,
        encryptedPassword = explicitPassword,
        encryptedMnemonicPhrase = encryptedMnemonicPhrase, // ← МАППИНГ НОВОГО ПОЛЯ
        isDualUserMode = isDualUserMode,
        createdAt = createdAt,
        expiresAt = expiresAt,
        updatedAt = updatedAt
    )
}

fun UserProfileEntity.toDomain(): UserProfile {
    return UserProfile(
        id = id,
        profileName = profileName,
        pinHash = pinHash,
        pinSalt = pinSalt,
        failedAttempts = failedAttempts,
        maxAttemptsBeforeDelete = maxAttemptsBeforeDelete,
        createdAt = createdAt
    )
}

fun MasterSettingsEntity.toDomain(): MasterSettings {
    return MasterSettings(
        id = id,
        masterPasswordHash = masterPasswordHash,
        masterPasswordSalt = masterPasswordSalt,
        isBiometricEnabled = isBiometricEnabled,
        updatedAt = updatedAt
    )
}
