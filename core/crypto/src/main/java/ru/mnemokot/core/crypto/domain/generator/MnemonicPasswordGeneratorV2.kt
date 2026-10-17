package ru.man2ll.mnemokot.core.crypto.domain.generator

import ru.man2ll.mnemokot.core.crypto.domain.model.MnemonicConfig
import ru.man2ll.mnemokot.core.crypto.domain.model.MnemonicGenerationSteps
import ru.man2ll.mnemokot.core.crypto.domain.model.MnemonicMethod
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MnemonicPasswordGeneratorV2 @Inject constructor() {

    private val gostTable = mapOf(
        'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'д' to "d", 'е' to "e", 'ё' to "yo",
        'ж' to "zh", 'з' to "z", 'и' to "i", 'й' to "j", 'к' to "k", 'л' to "l", 'м' to "m",
        'н' to "n", 'о' to "o", 'п' to "p", 'р' to "r", 'с' to "s", 'т' to "t", 'у' to "u",
        'ф' to "f", 'х' to "kh", 'ц' to "cz", 'ч' to "ch", 'ш' to "sh", 'щ' to "shh",
        'ъ' to "``", 'ы' to "y", 'ь' to "`", 'э' to "eh", 'ю' to "yu", 'я' to "ya"
    )

    private val qwertyMap = mapOf(
        'й' to "q", 'ц' to "w", 'у' to "e", 'к' to "r", 'е' to "t", 'н' to "y", 'г' to "u",
        'ш' to "i", 'щ' to "o", 'з' to "p", 'х' to "[", 'ъ' to "]", 'ф' to "a", 'ы' to "s",
        'в' to "d", 'а' to "f", 'п' to "g", 'р' to "h", 'о' to "j", 'л' to "k", 'д' to "l",
        'ж' to ";", 'э' to "'", 'я' to "z", 'ч' to "x", 'с' to "c", 'м' to "v", 'и' to "b",
        'т' to "n", 'ь' to "m", 'б' to ",", 'ю' to ".", 'ё' to "`"
    )

    private val leetMap = mapOf(
        'a' to listOf("@", "4"), 'b' to listOf("8", "6"), 'c' to listOf("(", "{"),
        'e' to listOf("3", "€"), 'g' to listOf("9", "&"), 'i' to listOf("1", "!"),
        'k' to listOf("<", "l"), 'l' to listOf("|", "1"), 'o' to listOf("0", "*"),
        's' to listOf("\$", "5"), 't' to listOf("7", "+"), 'z' to listOf("2", "%")
    )

    private fun applyGostTranslit(word: String): String {
        val sb = StringBuilder()
        for (ch in word.lowercase(Locale.ROOT)) {
            sb.append(gostTable[ch] ?: ch.toString())
        }
        return if (word.isNotEmpty() && word.all { it.isUpperCase() } && sb.isNotEmpty()) {
            sb.toString().uppercase(Locale.ROOT)
        } else if (word.isNotEmpty() && word[0].isUpperCase() && sb.isNotEmpty()) {
            sb.toString().substring(0, 1).uppercase(Locale.ROOT) + sb.toString().substring(1)
        } else {
            sb.toString()
        }
    }

    private fun applyQwertyShift(text: String): String {
        val sb = StringBuilder()
        for (ch in text.lowercase(Locale.ROOT)) {
            sb.append(qwertyMap[ch] ?: ch.toString())
        }
        return sb.toString()
    }
    fun generateMnemonicV2(config: MnemonicConfig): MnemonicGenerationSteps {
        if (config.phrase.isBlank()) return MnemonicGenerationSteps()

        val words = config.phrase.trim().split("\\s+".toRegex())
        val suffixBuilder = StringBuilder()
        
        if (config.includeAnchor && config.anchorWord.isNotBlank()) {
            suffixBuilder.append(config.anchorWord.trim())
        }
        if (config.includeService && config.serviceName.isNotBlank()) {
            val sName = config.serviceName.trim()
            val servicePart = if (sName.length >= 3) sName.substring(0, 3) else sName
            suffixBuilder.append(servicePart.uppercase(Locale.ROOT))
        }
        if (config.includeDate && config.dateContext.isNotBlank()) {
            suffixBuilder.append(config.dateContext.trim())
        }
        val generatedSuffix = suffixBuilder.toString()

        var baseProcessed = ""
        var postProcessed = ""

        when (config.method) {
            MnemonicMethod.AMPG_V2 -> {
                val transliteratedWords = words.map { applyGostTranslit(it) }
                baseProcessed = transliteratedWords.joinToString("")

                val truncatedWords = if (config.keepRoots) {
                    val budgetForPhrase = maxOf(1, config.targetLength - generatedSuffix.length)
                    allocateWordBudget(transliteratedWords, budgetForPhrase)
                } else {
                    transliteratedWords
                }
                val phase2Text = truncatedWords.joinToString("")

                postProcessed = if (config.mutateRepeats) {
                    mutateDuplicateCharacters(phase2Text)
                } else {
                    phase2Text
                }
            }

            MnemonicMethod.QWERTY_SHIFT -> {
                baseProcessed = applyQwertyShift(config.phrase.replace("\\s+".toRegex(), ""))
                postProcessed = baseProcessed
            }

            MnemonicMethod.ACRONYMS -> {
                val acronymChars = words.map { word ->
                    if (word.isNotEmpty()) word.substring(0, 1) else ""
                }.joinToString("")
                
                baseProcessed = applyGostTranslit(acronymChars)
                postProcessed = baseProcessed
            }
        }

        val combinedResult = postProcessed + generatedSuffix
        val finalPassword = if (combinedResult.length > config.targetLength) {
            combinedResult.substring(0, config.targetLength)
        } else {
            combinedResult
        }

        return MnemonicGenerationSteps(
            sourcePhrase = config.phrase,
            baseProcessed = baseProcessed,
            postProcessed = postProcessed,
            generatedSuffix = generatedSuffix,
            finalPassword = finalPassword
        )
    }

    private fun allocateWordBudget(words: List<String>, totalBudget: Int): List<String> {
        if (words.isEmpty()) return emptyList()
        val wordBudgets = IntArray(words.size) { 0 }
        var remainingBudget = totalBudget

        for (i in words.indices) {
            if (remainingBudget > 0) {
                wordBudgets[i] = 1
                remainingBudget--
            }
        }

        var budgetChanged: Boolean
        do {
            budgetChanged = false
            val uncompleted = words.indices.filter { wordBudgets[it] < words[it].length }
            if (uncompleted.isNotEmpty() && remainingBudget > 0) {
                val share = maxOf(1, remainingBudget / uncompleted.size)
                for (idx in uncompleted) {
                    if (remainingBudget <= 0) break
                    val currentWordLen = words[idx].length
                    val added = minOf(share, currentWordLen - wordBudgets[idx], remainingBudget)
                    wordBudgets[idx] += added
                    remainingBudget -= added
                    if (added > 0) budgetChanged = true
                }
            }
        } while (budgetChanged && remainingBudget > 0)

        return words.mapIndexed { index, word ->
            word.substring(0, minOf(word.length, wordBudgets[index]))
        }
    }

    private fun mutateDuplicateCharacters(text: String): String {
        val seen = mutableSetOf<Char>()
        val sb = StringBuilder()

        for (ch in text) {
            val lowerCh = ch.lowercaseChar()
            if (!seen.contains(lowerCh)) {
                seen.add(lowerCh)
                sb.append(ch)
            } else {
                val replacements = leetMap[lowerCh]
                var substituted = false
                if (replacements != null) {
                    for (rep in replacements) {
                        val repChar = rep.firstOrNull() ?: continue
                        if (!seen.contains(repChar)) {
                            seen.add(repChar)
                            sb.append(rep)
                            substituted = true
                            break
                        }
                    }
                }
                if (!substituted) {
                    sb.append(ch)
                }
            }
        }
        return sb.toString()
    }
}
