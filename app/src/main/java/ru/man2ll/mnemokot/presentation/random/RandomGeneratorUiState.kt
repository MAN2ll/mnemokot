package com.mnemokot.app.presentation.random

import com.mnemokot.core.crypto.domain.model.RandomGeneratorConfig
import com.mnemokot.core.crypto.domain.model.RandomGeneratorResult

data class RandomGeneratorUiState(
    val config: RandomGeneratorConfig = RandomGeneratorConfig(),
    val result: RandomGeneratorResult = RandomGeneratorResult(),
    val isHelpVisible: Boolean = false
)
