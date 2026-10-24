package ru.man2ll.mnemokot.core.crypto.domain.generator

import ru.man2ll.mnemokot.core.crypto.domain.model.MnemonicPhrase
import ru.man2ll.mnemokot.core.crypto.domain.model.GenerationResult
import ru.man2ll.mnemokot.core.crypto.domain.model.MnemonicMode

interface MnemonicPasswordGeneratorV2 {
    fun generatePassword(
        phrase: MnemonicPhrase,
        salt: ByteArray,
        length: Int,
        mode: MnemonicMode,
        useLeetSpeak: Boolean
    ): GenerationResult
}
