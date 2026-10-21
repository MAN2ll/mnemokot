package ru.man2ll.mnemokot.core.crypto.domain.hasher

interface PinHasher {
    fun hashPin(pin: CharArray, salt: ByteArray): String
    fun verifyPin(pin: CharArray, salt: ByteArray, expectedHash: String): Boolean
}
