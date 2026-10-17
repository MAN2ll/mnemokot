package ru.man2ll.mnemokot.presentation.random

import ru.mnemokot.core.crypto.domain.model.RandomGeneratorConfig
import ru.mnemokot.core.crypto.domain.model.RandomGeneratorResult

data class RandomGeneratorUiState(
    val config: RandomGeneratorConfig = RandomGeneratorConfig(),
    val result: RandomGeneratorResult = RandomGeneratorResult(),
    val isHelpVisible: Boolean = false
)
