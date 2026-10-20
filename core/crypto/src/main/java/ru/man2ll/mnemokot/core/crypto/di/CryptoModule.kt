package ru.man2ll.mnemokot.core.crypto.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.man2ll.mnemokot.core.crypto.data.MnemonicPasswordGeneratorV2Impl
import ru.man2ll.mnemokot.core.crypto.domain.generator.MnemonicPasswordGeneratorV2
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CryptoModule {

    @Binds
    @Singleton
    abstract fun bindMnemonicPasswordGeneratorV2(
        impl: MnemonicPasswordGeneratorV2Impl
    ): MnemonicPasswordGeneratorV2
}
