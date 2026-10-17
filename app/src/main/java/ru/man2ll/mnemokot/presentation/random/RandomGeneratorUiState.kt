package ru.man2ll.mnemokot.presentation.random

import ru.man2ll.core.crypto.domain.model.RandomGeneratorConfig
import ru.man2ll.core.crypto.domain.model.RandomGeneratorResult

data class RandomGeneratorUiState(
    val config: RandomGeneratorConfig = RandomGeneratorConfig(),
    val result: RandomGeneratorResult = RandomGeneratorResult(),
    val isHelpVisible: Boolean = false
)
