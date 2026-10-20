package ru.man2ll.mnemokot.core.crypto.domain.model

enum class TranslitMode {
    GOST_7_79_2000_B,
    NONE
}

data class MnemonicPhrase(
    val rawValue: CharArray
) : AutoCloseable {
    override fun close() {
        rawValue.fill('\u0000')
    }
}

data class GenerationResult(
    val password: CharArray
) : AutoCloseable {
    override fun close() {
        password.fill('\u0000')
    }
}
