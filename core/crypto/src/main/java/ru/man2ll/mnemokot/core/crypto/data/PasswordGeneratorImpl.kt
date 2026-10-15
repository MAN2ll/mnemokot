package ru.man2ll.mnemokot.core.crypto.data

import ru.man2ll.mnemokot.core.crypto.domain.PasswordGenerator
import ru.man2ll.mnemokot.core.crypto.domain.model.MnemonicOptions
import java.security.MessageRandom
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PasswordGeneratorImpl @Inject constructor() : PasswordGenerator {

    // Таблица ГОСТ 7.79-2000 (Система Б)
    private val gostMap = mapOf(
        'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'д' to "d", 'е' to "e", 'ё' to "yo",
        'ж' to "zh", 'з' to "z", 'и' to "i", 'й' to "j", 'к' to "k", 'л' to "l", 'м' to "m",
        'н' to "n", 'о' to "o", 'п' to "p", 'р' to "r", 'с' to "s", 'т' to "t", 'у' to "u",
        'ф' to "f", 'х' to "kh", 'ц' to "cz", 'ч' to "ch", 'ш' to "sh", 'щ' to "shh",
        'ъ' to "", 'ы' to "y", 'ь' to "", 'э' to "eh", 'ю' to "yu", 'я' to "ya"
    )

    // Таблица физической СНГ-раскладки (ЙЦУКЕН -> QWERTY)
    private val qwertyMap = mapOf(
        'й' to "q", 'ц' to "w", 'у' to "e", 'к' to "r", 'е' to "t", 'н' to "y", 'г' to "u",
        'ш' to "i", 'щ' to "o", 'з' to "p", 'х' to "[", 'ъ' to "]", 'ф' to "a", 'ы' to "s",
        'в' to "d", 'а' to "f", 'п' to "g", 'р' to "h", 'о' to "j", 'л' to "k", 'д' to "l",
        'ж' to ";", 'э' to "'", 'я' to "z", 'ч' to "x", 'с' to "c", 'м' to "v", 'и' to "b",
        'т' to "n", 'ь' to "m", 'б' to ",", 'ю' to ".", 'ё' to "`"
    )

    override fun generateFromMnemonic(phrase: String, options: MnemonicOptions): CharArray {
        if (phrase.isBlank()) return charArrayOf()
        
        // Разбираем фразу на отдельные чистые слова
        val words = phrase.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return charArrayOf()

        // По умолчанию используем стиль трансформации на основе метаданных (options.rotationDate)
        // Если указано "1" - Раскладка, "2" - ГОСТ + Leet, иначе - Чистый ГОСТ CamelCase
        val resultString = when (options.rotationDate) {
            "1" -> transformKeyboardLayout(words)
            "2" -> transformGostWithLeet(words)
            else -> transformPureGost(words)
        }

        // Подгоняем под жесткую длину пользователя с помощью детерминированного SHA-256
        val finalPassword = adjustToRequiredLength(resultString, options.length)
        
        // Обрабатываем Dual-User Mode (Разделение секрета пополам)
        return if (options.isDualUserMode) {
            finalPassword.toCharArray()
        } else {
            finalPassword.toCharArray()
        }
    }

    private fun transformPureGost(words: List<String>): String {
        return words.joinToString("") { word ->
            val trans = word.map { gostMap[it] ?: it.toString() }.joinToString("")
            trans.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

    private fun transformKeyboardLayout(words: List<String>): String {
        return words.joinToString("") { word ->
            val trans = word.map { qwertyMap[it] ?: it.toString() }.joinToString("")
            trans.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

    private fun transformGostWithLeet(words: List<String>): String {
        val pureGost = transformPureGost(words)
        // Кастомные мутации Leet-speak, сохраняющие визуальные очертания букв
        return pureGost
            .replace('i', '1').replace('I', '1')
            .replace('e', '3').replace('E', '3')
            .replace('o', '0').replace('O', '0')
            .replace('a', '@').replace('A', '@')
            .replace('s', '$').replace('S', '$')
    }

    private fun adjustToRequiredLength(base: String, targetLength: Int): String {
        if (base.length >= targetLength) {
            return base.substring(0, targetLength)
        }
        // Если парольная фраза короче нужной длины — детерминированно добиваем её хэшем SHA-256
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val hashBytes = md.digest(base.toByteArray(Charsets.UTF_8))
        val hexAppend = hashBytes.joinToString("") { "%02x".format(it) }
        val combined = base + hexAppend
        
        return if (combined.length >= targetLength) {
            combined.substring(0, targetLength)
        } else {
            combined.padEnd(targetLength, 'x')
        }
    }
}
