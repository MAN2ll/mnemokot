package ru.man2ll.mnemokot.core.crypto.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.man2ll.mnemokot.core.crypto.domain.model.MnemonicOptions

class PasswordGeneratorImplTest {

    private val generator = PasswordGeneratorImpl()

    @Test
    fun `generateFromMnemonic should produce deterministic results and respect length`() {
        val phrase = "фиолетовая корова"
        val options = MnemonicOptions(length = 16, salt = "!", serviceName = "Gmail", rotationDate = "1026")

        val firstRun = generator.generateFromMnemonic(phrase, options)
        val secondRun = generator.generateFromMnemonic(phrase, options)

        assertEquals(String(firstRun), String(secondRun))
        assertEquals(16, firstRun.size)
    }

    @Test
    fun `generateFromMnemonic should have unique characters case insensitive`() {
        val phrase = "абвгдежзийклмнопрстуфхцчшщъыьэюя"
        val options = MnemonicOptions(length = 20)

        val result = generator.generateFromMnemonic(phrase, options)
        val resultString = String(result)

        val checked = HashSet<Char>()
        for (ch in resultString) {
            val upper = ch.uppercaseChar()
            assertTrue("Duplicate character found: \$ch", !checked.contains(upper))
            checked.add(upper)
        }
    }
}
