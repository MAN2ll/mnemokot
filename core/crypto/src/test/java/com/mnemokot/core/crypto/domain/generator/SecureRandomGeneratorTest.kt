package com.mnemokot.core.crypto.domain.generator

import com.mnemokot.core.crypto.domain.model.RandomGeneratorConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SecureRandomGeneratorTest {

    private lateinit var generator: SecureRandomGenerator
    private val similarChars = setOf('l', 'I', '1', 'o', 'O', '0', '5', 'S', '2', 'Z', '8', 'B')

    @Before
    fun setUp() {
        generator = SecureRandomGenerator()
    }

    @Test
    fun testPasswordLengthBoundaries() {
        val lengths = listOf(8, 16, 32, 64)
        for (targetLength in lengths) {
            val config = RandomGeneratorConfig(length = targetLength)
            val result = generator.generate(config)
            assertEquals(targetLength, result.password.length)
        }
    }
    @Test
    fun testMandatoryCharacterPools() {
        val config = RandomGeneratorConfig(
            length = 8,
            includeUppercase = true,
            includeLowercase = true,
            includeDigits = true,
            includeSymbols = true,
            excludeSimilar = false
        )

        repeat(1000) {
            val result = generator.generate(config)
            assertTrue(result.uppercaseCount > 0)
            assertTrue(result.lowercaseCount > 0)
            assertTrue(result.digitsCount > 0)
            assertTrue(result.symbolsCount > 0)
        }
    }

    @Test
    fun testExclusionOfSimilarCharacters() {
        val config = RandomGeneratorConfig(
            length = 32,
            includeUppercase = true,
            includeLowercase = true,
            includeDigits = true,
            includeSymbols = false,
            excludeSimilar = true
        )

        repeat(500) {
            val result = generator.generate(config)
            for (ch in result.password) {
                assertTrue(ch !in similarChars)
            }
        }
    }
    @Test
    fun testEmptyPoolHandling() {
        val config = RandomGeneratorConfig(
            length = 16,
            includeUppercase = false,
            includeLowercase = false,
            includeDigits = false,
            includeSymbols = false
        )
        
        val result = generator.generate(config)
        assertEquals("", result.password)
        assertEquals(0.0, result.entropyBits, 0.001)
    }
}
