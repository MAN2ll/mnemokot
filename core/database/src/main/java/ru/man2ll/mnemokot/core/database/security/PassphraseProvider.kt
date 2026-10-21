package ru.man2ll.mnemokot.core.database.security

interface PassphraseProvider {
    fun getPassphrase(): ByteArray
}
