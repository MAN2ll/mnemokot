package ru.man2ll.mnemokot.ui.vault

import ru.man2ll.mnemokot.core.crypto.domain.model.MnemonicMode

/**
 * Контракт данных для создания новой карточки пароля.
 * Все секретные данные передаются как CharArray для крипто-гигиены.
 */
data class CreateAccountData(
    val serviceName: String,
    val login: String,
    val password: CharArray, // ← Секрет, должен быть затёрт после использования
    val isMnemonic: Boolean,
    val isStoredInVault: Boolean, // ← Гибридный режим: сохранить и фразу, и пароль
    val mnemonicPhrase: CharArray?, // ← Для гибрида, тоже секрет
    val mode: MnemonicMode,
    val passwordLength: Int,
    val phraseHint: String? // ← Не секретная подсказка
) {
    /**
     *  принудительное затирание всех секретных массивов
     */
    fun clearSensitiveData() {
        password.fill('\u0000')
        mnemonicPhrase?.fill('\u0000')
    }
}
