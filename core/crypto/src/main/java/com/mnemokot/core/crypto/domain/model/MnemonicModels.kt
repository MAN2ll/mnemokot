package com.mnemokot.core.crypto.domain.model

/**
 * Поддерживаемые методы мнемонической генерации в комбайне.
 */
enum class MnemonicMethod {
    AMPG_V2,       // Мнемонический генератор с умным усечением корней
    QWERTY_SHIFT,  // Сдвиг букв по клавиатурной сетке (имитация ввода в англ. раскладке)
    ACRONYMS       // Сборка пароля по первым буквам слов фразы
}

/**
 * Конфигурация параметров генерации, настраиваемая пользователем в UI.
 */
data class MnemonicConfig(
    val method: MnemonicMethod = MnemonicMethod.AMPG_V2,
    val phrase: String = "",
    val anchorWord: String = "",      // Якорное слово (дополнительная соль)
    val serviceName: String = "",     // Имя сервиса (например, "github")
    val dateContext: String = "",     // Дата генерации в формате ММГГ (например, "1026")
    val targetLength: Int = 16,       // Желаемая длина (12, 16, 20)
    val keepRoots: Boolean = true,    // Опция для AMPG: умное усечение бюджетов слов
    val mutateRepeats: Boolean = true,// Опция для AMPG: Leet-мутация вместо удаления повторов
    val includeAnchor: Boolean = false, // Включить якорное слово в финальный суффикс
    val includeService: Boolean = true, // Включить имя сервиса в финальный суффикс
    val includeDate: Boolean = true     // Включить дату в финальный суффикс
)

/**
 * Модель для real-time отображения шагов трансформации текста в UI.
 */
data class MnemonicGenerationSteps(
    val sourcePhrase: String = "",
    val baseProcessed: String = "",   // Результат работы основного метода (Транслит / Сдвиг / Акроним)
    val postProcessed: String = "",   // Результат после усечения / мутации (если применимо)
    val generatedSuffix: String = "", // Сформированный хвост безопасности из метаданных
    val finalPassword: String = ""    // Итоговый утвержденный результат
)
