package ru.mnemokot.core.crypto.domain.model

data class RandomGeneratorConfig(
    val length: Int = 16,
    val includeUppercase: Boolean = true,
    val includeLowercase: Boolean = true,
    val includeDigits: Boolean = true,
    val includeSymbols: Boolean = true,
    val excludeSimilar: Boolean = true
)

data class RandomGeneratorResult(
    val password: String = "",
    val uppercaseCount: Int = 0,
    val lowercaseCount: Int = 0,
    val digitsCount: Int = 0,
    val symbolsCount: Int = 0,
    val entropyBits: Double = 0.0
)
