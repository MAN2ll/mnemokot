package ru.man2ll.mnemokot.core.crypto.domain.model

enum class MnemonicMode {
    AMPG_V2_GOST,      // Транслит ГОСТ + усечение корней + Leet + достройка SHA-256 с солью
    QWERTY_SHIFT,      // Сдвиг раскладки: кириллица -> латиница по физическим клавишам QWERTY
    PURE_LEET_SPEAK    // Посимвольный Leet Speak с чередованием регистра
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
