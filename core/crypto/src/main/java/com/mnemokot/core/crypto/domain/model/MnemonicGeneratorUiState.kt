package com.mnemokot.core.crypto.domain.model

data class MnemonicGeneratorUiState(
    val config: MnemonicConfig = MnemonicConfig(),
    val steps: MnemonicGenerationSteps = MnemonicGenerationSteps(),
    val isHelpVisible: Boolean = false
)
