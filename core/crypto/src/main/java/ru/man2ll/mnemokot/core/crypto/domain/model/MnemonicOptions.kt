package ru.man2ll.mnemokot.core.crypto.domain.model

data class MnemonicOptions(
    val length: Int = 16,
    val salt: String = "",
    val serviceName: String = "",
    val rotationDate: String = "", // Формат: ММГГ, например "1026"
    val isTwoUserMode: Boolean = false,
    val customRules: String = ""
)
