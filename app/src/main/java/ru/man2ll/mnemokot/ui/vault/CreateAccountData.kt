package ru.man2ll.mnemokot.ui.vault

import ru.man2ll.mnemokot.core.crypto.domain.model.MnemonicMode

enum class AccountType {
    MNEMONIC,
    HYBRID,
    VAULT
}

data class CreateAccountData(
    val serviceName: String,
    val login: String,
    val password: CharArray,
    val accountType: AccountType,
    val mnemonicPhrase: CharArray?,
    val mode: MnemonicMode,
    val useLeetSpeak: Boolean,
    val passwordLength: Int,
    val phraseHint: String?,
    val salt: ByteArray
) {
    val isMnemonic: Boolean get() = accountType != AccountType.VAULT
    val isStoredInVault: Boolean get() = accountType != AccountType.MNEMONIC

    fun clearSensitiveData() {
        password.fill('\u0000')
        mnemonicPhrase?.fill('\u0000')
        salt.fill(0)
    }
}
