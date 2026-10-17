package ru.man2ll.mnemokot.presentation.mnemonic

import com.mnemokot.core.crypto.domain.model.MnemonicConfig
import com.mnemokot.core.crypto.domain.model.MnemonicGenerationSteps

data class MnemonicGeneratorUiState(
    val config: MnemonicConfig = MnemonicConfig(),
    val steps: MnemonicGenerationSteps = MnemonicGenerationSteps(),
    val isHelpVisible: Boolean = false
)
