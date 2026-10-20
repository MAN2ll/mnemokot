package ru.man2ll.mnemokot.core.crypto.data

import ru.man2ll.mnemokot.core.crypto.domain.generator.MnemonicPasswordGeneratorV2
import ru.man2ll.mnemokot.core.crypto.domain.model.MnemonicPhrase
import ru.man2ll.mnemokot.core.crypto.domain.model.GenerationResult
import ru.man2ll.mnemokot.core.crypto.domain.model.TranslitMode
import java.security.MessageDigest
import javax.inject.Inject

class MnemonicPasswordGeneratorV2Impl @Inject constructor() : MnemonicPasswordGeneratorV2 {

    private val cyrillicToGost = mapOf(
        'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'д' to "d",
        'е' to "e", 'ё' to "yo", 'ж' to "zh", 'з' to "z", 'и' to "i",
        'й' to "j", 'к' to "k", 'л' to "l", 'м' to "m", 'н' to "n",
        'о' to "o", 'п' to "p", 'р' to "r", 'с' to "s", 'т' to "t",
        'у' to "u", 'ф' to "f", 'х' to "x", 'ц' to "cz", 'ч' to "ch",
        'ш' to "sh", 'щ' to "shh", 'ъ' to "``", 'ы' to "y`", 'ь' to "`",
        'э' to "e`", 'ю' to "yu", 'я' to "ya"
    )

    private val leetSpeakMap = mapOf(
        'a' to '@', 'e' to '3', 'i' to '1', 'o' to '0', 's' to '\$', 't' to '7'
    )

    override fun generatePassword(
        phrase: MnemonicPhrase,
        salt: ByteArray,
        length: Int,
        translitMode: TranslitMode,
        useLeetSpeak: Boolean
    ): GenerationResult {
        val processedBuilder = StringBuilder()
        val words = String(phrase.rawValue).split(Regex("\\s+"))

        for (word in words) {
            if (word.isEmpty()) continue
            val lowerWord = word.lowercase()
            val transliterated = if (translitMode == TranslitMode.GOST_7_79_2000_B) {
                transliterateGost(lowerWord)
            } else {
                lowerWord
            }
            val visualRoot = if (transliterated.length > 3) transliterated.take(3) else transliterated
            processedBuilder.append(visualRoot)
        }

        var intermediaryString = processedBuilder.toString()
        if (useLeetSpeak) {
            intermediaryString = applyLeetSpeak(intermediaryString)
        }

        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        val hashBytes = digest.digest(intermediaryString.toByteArray(Charsets.UTF_8))
        
        val finalPassword = CharArray(length)
        val baseChars = intermediaryString.toCharArray()
        
        for (i in 0 until length) {
            if (i < baseChars.size) {
                finalPassword[i] = baseChars[i]
            } else {
                val hashIndex = (hashBytes[(i - baseChars.size) % hashBytes.size].toInt() and 0xFF)
                val pseudoRandomChar = (33 + (hashIndex % 93)).toChar()
                finalPassword[i] = pseudoRandomChar
            }
        }

        baseChars.fill('\u0000')
        intermediaryString.toCharArray().fill('\u0000')
        
        return GenerationResult(finalPassword)
    }

    private fun transliterateGost(input: String): String {
        val builder = StringBuilder()
        for (char in input) {
            val substitution = cyrillicToGost[char]
            if (substitution != null) {
                builder.append(substitution)
            } else {
                builder.append(char)
            }
        }
        return builder.toString()
    }

    private fun applyLeetSpeak(input: String): String {
        val builder = StringBuilder()
        for (char in input) {
            val replacement = leetSpeakMap[char]
            if (replacement != null) {
                builder.append(replacement)
            } else {
                builder.append(char)
            }
        }
        return builder.toString()
    }
}
