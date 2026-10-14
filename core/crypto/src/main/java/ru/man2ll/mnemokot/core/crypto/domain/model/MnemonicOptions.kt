package ru.man2ll.mnemokot.core.crypto.domain.model

data class MnemonicOptions(
    val length: Int = 16,
    val salt: String = "",
    val serviceName: String = "",
    val rotationDate: String = "",
    // Новая настройка для разделения пароля на две учетные записи
    val isDualUserMode: Boolean = false 
)
