package ru.man2ll.mnemokot.core.crypto.domain

import ru.man2ll.mnemokot.core.crypto.domain.model.MnemonicOptions

interface PasswordGenerator {
    fun generateFromMnemonic(phrase: String, options: MnemonicOptions): CharArray
}
