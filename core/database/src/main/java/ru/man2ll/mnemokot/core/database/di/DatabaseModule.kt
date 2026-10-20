package ru.man2ll.mnemokot.core.database.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.man2ll.mnemokot.core.database.data.repository.MnemokotRepositoryImpl
import ru.man2ll.mnemokot.core.database.domain.repository.MnemokotRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseModule {

    @Binds
    @Singleton
    abstract fun bindMnemokotRepository(
        repositoryImpl: MnemokotRepositoryImpl
    ): MnemokotRepository
}
