package ru.man2ll.mnemokot.core.crypto.domain.generator

import ru.mnemokot.core.crypto.domain.model.RandomGeneratorConfig
import ru.mnemokot.core.crypto.domain.model.RandomGeneratorResult
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log2


@Singleton
class SecureRandomGenerator @Inject constructor() {

    private val secureRandom = SecureRandom()

    private val uppercaseSet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private val lowercaseSet = "abcdefghijklmnopqrstuvwxyz"
    private val digitsSet = "0123456789"
    private val symbolsSet = "!@#\$%^&*()-_=+[]{}|;:,.<>?"
    private val similarChars = setOf('l', 'I', '1', 'o', 'O', '0', '5', 'S', '2', 'Z', '8', 'B')

    fun generate(config: RandomGeneratorConfig): RandomGeneratorResult {
        val allowedPool = StringBuilder()
        val mandatoryPools = mutableListOf<String>()

        if (config.includeUppercase) {
            val set = if (config.excludeSimilar) uppercaseSet.filter { it !in similarChars } else uppercaseSet
            if (set.isNotEmpty()) { allowedPool.append(set); mandatoryPools.add(set) }
        }
        if (config.includeLowercase) {
            val set = if (config.excludeSimilar) lowercaseSet.filter { it !in similarChars } else lowercaseSet
            if (set.isNotEmpty()) { allowedPool.append(set); mandatoryPools.add(set) }
        }
        if (config.includeDigits) {
            val set = if (config.excludeSimilar) digitsSet.filter { it !in similarChars } else digitsSet
            if (set.isNotEmpty()) { allowedPool.append(set); mandatoryPools.add(set) }
        }
        if (config.includeSymbols) {
            val set = if (config.excludeSimilar) symbolsSet.filter { it !in similarChars } else symbolsSet
            if (set.isNotEmpty()) { allowedPool.append(set); mandatoryPools.add(set) }
        }

        if (allowedPool.isEmpty() || config.length < mandatoryPools.size) {
            return RandomGeneratorResult()
        }

        val passwordChars = CharArray(config.length)
        var charIndex = 0

        for (pool in mandatoryPools) {
            passwordChars[charIndex++] = pool[secureRandom.nextInt(pool.length)]
        }

        val poolString = allowedPool.toString()
        while (charIndex < config.length) {
            passwordChars[charIndex++] = poolString[secureRandom.nextInt(poolString.length)]
        }

        for (i in passwordChars.size - 1 downTo 1) {
            val j = secureRandom.nextInt(i + 1)
            val temp = passwordChars[i]
            passwordChars[i] = passwordChars[j]
            passwordChars[j] = temp
        }

        val finalPassword = String(passwordChars)
        
        var up = 0; var low = 0; var dig = 0; var sym = 0
        for (ch in passwordChars) {
            when {
                ch.isUpperCase() -> up++
                ch.isLowerCase() -> low++
                ch.isDigit() -> dig++
                ch in symbolsSet -> sym++
            }
        }

        val entropy = if (poolString.isNotEmpty()) {
            config.length * log2(poolString.length.toDouble())
        } else 0.0

        passwordChars.fill(' ')

        return RandomGeneratorResult(
            password = finalPassword,
            uppercaseCount = up,
            lowercaseCount = low,
            digitsCount = dig,
            symbolsCount = sym,
            entropyBits = entropy
        )
    }
}
