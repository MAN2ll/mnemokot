package ru.man2ll.mnemokot.core.crypto.data

import ru.man2ll.mnemokot.core.crypto.domain.generator.MnemonicPasswordGeneratorV2
import ru.man2ll.mnemokot.core.crypto.domain.model.GenerationResult
import ru.man2ll.mnemokot.core.crypto.domain.model.MnemonicMode
import ru.man2ll.mnemokot.core.crypto.domain.model.MnemonicPhrase
import java.security.MessageDigest
import javax.inject.Inject

class MnemonicPasswordGeneratorV2Impl @Inject constructor() : MnemonicPasswordGeneratorV2 {

    // Транслитерация кириллицы по ГОСТ 7.79-2000 (таблица Б)
    private val cyrillicToGost = mapOf(
        'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'д' to "d",
        'е' to "e", 'ё' to "yo", 'ж' to "zh", 'з' to "z", 'и' to "i",
        'й' to "j", 'к' to "k", 'л' to "l", 'м' to "m", 'н' to "n",
        'о' to "o", 'п' to "p", 'р' to "r", 'с' to "s", 'т' to "t",
        'у' to "u", 'ф' to "f", 'х' to "x", 'ц' to "cz", 'ч' to "ch",
        'ш' to "sh", 'щ' to "shh", 'ъ' to "``", 'ы' to "y`", 'ь' to "`",
        'э' to "e`", 'ю' to "yu", 'я' to "ya"
    )

    // Карта QWERTY-сдвига: русская раскладка -> английская (по физическим клавишам)
    private val ruToEnQwerty = mapOf(
        'й' to 'q', 'ц' to 'w', 'у' to 'e', 'к' to 'r', 'е' to 't', 'н' to 'y', 'г' to 'u',
        'ш' to 'i', 'щ' to 'o', 'з' to 'p', 'х' to '[', 'ъ' to ']',
        'ф' to 'a', 'ы' to 's', 'в' to 'd', 'а' to 'f', 'п' to 'g', 'р' to 'h', 'о' to 'j',
        'л' to 'k', 'д' to 'l', 'ж' to ';', 'э' to '\'',
        'я' to 'z', 'ч' to 'x', 'с' to 'c', 'м' to 'v', 'и' to 'b', 'т' to 'n', 'ь' to 'm',
        'б' to ',', 'ю' to '.', 'ё' to '`',
        'Й' to 'Q', 'Ц' to 'W', 'У' to 'E', 'К' to 'R', 'Е' to 'T', 'Н' to 'Y', 'Г' to 'U',
        'Ш' to 'I', 'Щ' to 'O', 'З' to 'P', 'Х' to '{', 'Ъ' to '}',
        'Ф' to 'A', 'Ы' to 'S', 'В' to 'D', 'А' to 'F', 'П' to 'G', 'Р' to 'H', 'О' to 'J',
        'Л' to 'K', 'Д' to 'L', 'Ж' to ':', 'Э' to '"',
        'Я' to 'Z', 'Ч' to 'X', 'С' to 'C', 'М' to 'V', 'И' to 'B', 'Т' to 'N', 'Ь' to 'M',
        'Б' to '<', 'Ю' to '>', 'Ё' to '~'
    )

    // Leet-карта
    private val leetSpeakMap = mapOf(
        'a' to '@', 'e' to '3', 'i' to '1', 'o' to '0', 's' to '$', 't' to '7'
    )

    override fun generatePassword(
        phrase: MnemonicPhrase,
        salt: ByteArray,
        length: Int,
        mode: MnemonicMode,
        useLeetSpeak: Boolean
    ): GenerationResult {
        val rawInput = String(phrase.rawValue)
        val password = when (mode) {
            MnemonicMode.AMPG_V2_GOST -> generateAmpgV2(rawInput, salt, length, useLeetSpeak)
            MnemonicMode.QWERTY_SHIFT -> generateQwertyShift(rawInput, length)
            MnemonicMode.PURE_LEET_SPEAK -> generatePureLeetSpeak(rawInput, length)
        }
        return GenerationResult(password)
    }

    private fun generateAmpgV2(input: String, salt: ByteArray, length: Int, useLeetSpeak: Boolean): CharArray {
        val processedBuilder = StringBuilder()
        val words = input.split(Regex("\\s+"))

        for (word in words) {
            if (word.isEmpty()) continue
            val lowerWord = word.lowercase()
            val transliterated = transliterateGost(lowerWord)
            val visualRoot = if (transliterated.length > 3) transliterated.take(3) else transliterated
            processedBuilder.append(visualRoot)
        }

        var intermediary = processedBuilder.toString()
        if (useLeetSpeak) {
            intermediary = applyLeetSpeak(intermediary)
        }

        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        val hashBytes = digest.digest(intermediary.toByteArray(Charsets.UTF_8))

        val baseChars = intermediary.toCharArray()
        val finalPassword = CharArray(length)

        for (i in 0 until length) {
            if (i < baseChars.size) {
                finalPassword[i] = baseChars[i]
            } else {
                val hashIndex = hashBytes[(i - baseChars.size) % hashBytes.size].toInt() and 0xFF
                finalPassword[i] = (33 + (hashIndex % 93)).toChar()
            }
        }

        baseChars.fill('\u0000')
        hashBytes.fill(0)
        return finalPassword
    }

    private fun generateQwertyShift(input: String, length: Int): CharArray {
        val builder = StringBuilder()
        for (char in input) {
            if (char.isWhitespace()) continue
            builder.append(ruToEnQwerty[char] ?: char)
        }
        return fitToLength(builder.toString(), length, '!')
    }

    private fun generatePureLeetSpeak(input: String, length: Int): CharArray {
        val noSpaces = input.replace(Regex("\\s+"), "")
        val builder = StringBuilder()
        for ((index, char) in noSpaces.withIndex()) {
            val baseChar = if (index % 2 == 0) char.uppercaseChar() else char.lowercaseChar()
            val replacement = leetSpeakMap[baseChar.lowercaseChar()]
            if (replacement != null && index % 3 == 0) {
                builder.append(replacement)
            } else {
                builder.append(baseChar)
            }
        }
        return fitToLength(builder.toString(), length, 'X')
    }

    private fun fitToLength(source: String, length: Int, filler: Char): CharArray {
        val result = CharArray(length) { filler }
        val copyLen = minOf(source.length, length)
        for (i in 0 until copyLen) {
            result[i] = source[i]
        }
        return result
    }

    private fun transliterateGost(input: String): String {
        val builder = StringBuilder()
        for (char in input) {
            builder.append(cyrillicToGost[char] ?: char)
        }
        return builder.toString()
    }

    private fun applyLeetSpeak(input: String): String {
        val builder = StringBuilder()
        for (char in input) {
            builder.append(leetSpeakMap[char] ?: char)
        }
        return builder.toString()
    }
}
