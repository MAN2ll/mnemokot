package ru.man2ll.mnemokot.core.crypto.data

import ru.man2ll.mnemokot.core.crypto.domain.PasswordGenerator
import ru.man2ll.mnemokot.core.crypto.domain.model.MnemonicOptions
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PasswordGeneratorImpl @Inject constructor() : PasswordGenerator {

    // ГОСТ 7.79-2000 (Система Б) жесткие таблицы подстановок кириллицы
    private val gostTable = mapOf(
        'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'д' to "d",
        'е' to "e", 'ё' to "yo", 'ж' to "zh", 'з' to "z", 'и' to "i",
        'й' to "j", 'к' to "k", 'л' to "l", 'м' to "m", 'н' to "n",
        'о' to "o", 'п' to "p", 'р' to "r", 'с' to "s", 'т' to "t",
        'у' to "u", 'ф' to "f", 'х' to "kh", 'ц' to "cz", 'ч' to "ch",
        'ш' to "sh", 'щ' to "shh", 'ъ' to "", 'ы' to "y", 'ь' to "",
        'э' to "e", 'ю' to "yu", 'я' to "ya"
    )

    // Карта мутаций Leet-speak
    private val leetTable = mapOf(
        'o' to '0', 'O' to '0',
        'e' to '3', 'E' to '3',
        'a' to '@', 'A' to '@',
        's' to '$', 'S' to '$',
        'i' to '1', 'I' to '1',
        't' to '7', 'T' to '7'
    )

    override fun generateFromMnemonic(phrase: String, options: MnemonicOptions): CharArray {
        if (phrase.isBlank()) return charArrayOf()

        // 1. Валидация длины под Режим двух пользователей (16, 18 или 20 символов)
        val targetLength = if (options.isDualUserMode) {
            when {
                options.length <= 16 -> 16
                options.length <= 18 -> 18
                else -> 20
            }
        } else {
            options.length.coerceIn(8, 64)
        }

        val words = phrase.trim().split("\\s+".toRegex())
        val totalWords = words.size
        
        // Вычисляем сколько символов брать от каждого корня слова
        val charsPerWord = (targetLength / totalWords).coerceAtLeast(2)
        val processedPhrase = StringBuilder()

        // 2. Логика усечения корней слов и транслитерации по ГОСТу
        for (word in words) {
            val transliterated = translit(word)
            val truncated = if (transliterated.length > charsPerWord) {
                transliterated.substring(0, charsPerWord)
            } else {
                transliterated
            }
            if (truncated.isNotEmpty()) {
                processedPhrase.append(truncated.replaceFirstChar { it.uppercase() })
            }
        }

        // Подмешиваем соль, имя сервиса и зафиксированную дату ротации
        val rawPassword = "${processedPhrase}${options.serviceName}${options.rotationDate}${options.salt}"
        
        // 3. Leet-speak мутация знаков и фильтрация повторов символов (case-insensitive)
        val uniqueChars = mutableListOf<Char>()
        val seenCharsLowerCase = mutableSetOf<Char>()

        for (ch in rawPassword) {
            val mutated = leetTable[ch] ?: ch
            val lowerMutated = mutated.lowercaseChar()
            
            // Жесткое требование оригинального проекта: никаких повторов символов без учета регистра
            if (!seenCharsLowerCase.contains(lowerMutated) && !mutated.isWhitespace()) {
                uniqueChars.add(mutated)
                seenCharsLowerCase.add(lowerMutated)
            }
        }

        // 4. SHA-256 Добивка, если уникальных символов от корней не хватило до целевой длины
        if (uniqueChars.size < targetLength) {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(rawPassword.toByteArray(Charsets.UTF_8))
            val hashString = hashBytes.joinToString("") { "%02x".format(it) }

            for (ch in hashString) {
                val upperCh = if (ch.isLetter()) ch.uppercaseChar() else ch
                val lowerCh = upperCh.lowercaseChar()
                if (!seenCharsLowerCase.contains(lowerCh)) {
                    uniqueChars.add(upperCh)
                    seenCharsLowerCase.add(lowerCh)
                    if (uniqueChars.size >= targetLength) break
                }
            }
        }

        // Возвращаем строго усеченный до целевой длины массив символов CharArray
        val finalPassword = uniqueChars.take(targetLength).toCharArray()
        
        // Затираем промежуточные списки в памяти ради безопасности
        uniqueChars.fill(' ')
        
        return finalPassword
    }

    private fun translit(word: String): String {
        val result = StringBuilder()
        for (ch in word) {
            val lowerCh = ch.lowercaseChar()
            val replacement = gostTable[lowerCh]
            if (replacement != null) {
                // Сохраняем исходный регистр первой буквы
                if (ch.isUpperCase() && replacement.isNotEmpty()) {
                    result.append(replacement.replaceFirstChar { it.uppercase() })
                } else {
                    result.append(replacement)
                }
            } else if (ch.isLetterOrDigit()) {
                result.append(ch)
            }
        }
        return result.toString()
    }
}
