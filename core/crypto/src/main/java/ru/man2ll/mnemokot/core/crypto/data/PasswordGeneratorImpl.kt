package ru.man2ll.mnemokot.core.crypto.data

import ru.man2ll.mnemokot.core.crypto.domain.PasswordGenerator
import ru.man2ll.mnemokot.core.crypto.domain.model.MnemonicOptions
import java.security.MessageDigest
import javax.inject.Inject

class PasswordGeneratorImpl @Inject constructor() : PasswordGenerator {

    // Таблица транслитерации ГОСТ 7.79-2000 (Система Б)
    // Поддерживает русский, украинский и белорусский языки
    private val gostTable = mapOf(
        'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'д' to "d", 'е' to "e", 'ё' to "yo",
        'ж' to "zh", 'з' to "z", 'и' to "i", 'й' to "j", 'к' to "k", 'л' to "l", 'м' to "m",
        'н' to "n", 'о' to "o", 'п' to "p", 'р' to "r", 'с' to "s", 'т' to "t", 'у' to "u",
        'ф' to "f", 'х' to "x", 'ц' to "cz", 'ч' to "ch", 'ш' to "sh", 'щ' to "shh",
        'ъ' to "``", 'ы' to "y", 'ь' to "`", 'э' to "e`", 'ю' to "yu", 'я' to "ya",
        // Украинские и белорусские буквы
        'ґ' to "g`", 'є' to "ye", 'і' to "i", 'ї' to "yi", 'ў' to "w"
    )

    override fun generateFromMnemonic(phrase: String, options: MnemonicOptions): CharArray {
        if (phrase.isBlank()) return charArrayOf()

        // 1. Нормализация и разбиение на слова
        val words = phrase.trim().lowercase().split(Regex("\\s+"))
        if (words.isEmpty()) return charArrayOf()

        // 2. Детерминированное усечение корней слов
        // Считаем доступный лимит длины для слов: общая длина минус длина соли, сервиса и даты
        val metaLength = options.salt.length + options.serviceName.length + options.rotationDate.length
        val targetWordsLength = (options.length - metaLength).coerceAtLeast(4)
        
        val charsPerWord = (targetWordsLength / words.size).coerceAtLeast(1)
        
        val processedPhrase = StringBuilder()
        for (word in words) {
        val transliterated = translit(word)
        // Берем N символов от каждого слова для сохранения семантики корня
        val truncated = if (transliterated.length > charsPerWord) {
        transliterated.substring(0, charsPerWord)
        } else {
        transliterated
        }
        
    // Делаем первую букву каждого слова заглавной (CamelCase)
    if (truncated.isNotEmpty()) {
        processedPhrase.append(truncated.replaceFirstChar { it.uppercase() })
    }
}

        // 3. Сборка сырого конвейера (Детерминированная склейка)
        val rawPassword = "${processedPhrase}${options.serviceName}${options.rotationDate}${options.salt}"

        // 4. Мутация символов (Leet-speak подстановки) и защита от повторов
        val finalPassword = StringBuilder()
        val usedChars = HashSet<Char>()

        for (ch in rawPassword) {
            val mutated = when (ch) {
                'o', 'O', 'о', 'О' -> '0'
                'i', 'I', 'и', 'И' -> '1'
                'e', 'E', 'е', 'Е' -> '3'
                'a', 'A', 'а', 'А' -> '@'
                's', 'S', 'с', 'С' -> '\$'
                't', 'T', 'т', 'Т' -> '7'
                else -> ch
            }

            // Проверка уникальности без учета регистра для AMPG v2
            val upperMutated = mutated.uppercaseChar()
            if (!usedChars.contains(upperMutated)) {
                finalPassword.append(mutated)
                usedChars.add(upperMutated)
            }
            
            if (finalPassword.length >= options.length) break
        }

        // 5. Если из-за уникальности букв не хватает до длины, добиваем детерминированным хэшем
        if (finalPassword.length < options.length) {
            val sha = MessageDigest.getInstance("SHA-256")
            val hashBytes = sha.digest(rawPassword.toByteArray(Charsets.UTF_8))
            val hashHex = hashBytes.joinToString("") { "%02x".format(it) }
            
            for (ch in hashHex) {
                val upperCh = ch.uppercaseChar()
                if (!usedChars.contains(upperCh)) {
                    finalPassword.append(ch)
                    usedChars.add(upperCh)
                }
                if (finalPassword.length >= options.length) break
            }
        }

        // Строго обрезаем под финальный размер
        val resultString = finalPassword.toString().substring(0, options.length)
        
        // 6. Очистка промежуточных данных из памяти
        usedChars.clear()
        
        return resultString.toCharArray()
    }

    private fun translit(word: String): String {
        val result = StringBuilder()
        for (ch in word) {
            val trans = gostTable[ch]
            if (trans != null) {
                result.append(trans)
            } else {
                result.append(ch) // Если символ латинский или знак, оставляем
            }
        }
        return result.toString()
    }
}
