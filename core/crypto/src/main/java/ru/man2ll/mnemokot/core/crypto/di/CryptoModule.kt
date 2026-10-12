package ru.man2ll.mnemokot.core.crypto.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.man2ll.mnemokot.core.crypto.data.PasswordGeneratorImpl
import ru.man2ll.mnemokot.core.crypto.domain.PasswordGenerator
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CryptoModule {

    @Binds
    @Singleton
    abstract fun bindPasswordGenerator(
        passwordGeneratorImpl: PasswordGeneratorImpl
    ): PasswordGenerator
}
