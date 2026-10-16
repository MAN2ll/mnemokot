package ru.man2ll.mnemokot.core.database.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.man2ll.mnemokot.core.database.data.dao.AccountDao
import ru.man2ll.mnemokot.core.database.data.dao.ProfileDao
import ru.man2ll.mnemokot.core.database.data.dao.MasterSettingsDao
import ru.man2ll.mnemokot.core.database.data.MnemokotDatabase
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

    companion object {
        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): MnemokotDatabase {
            return Room.databaseBuilder(
                context,
                MnemokotDatabase::class.java,
                "mnemokot.db"
            )
            .fallbackToDestructiveMigration()
            .build()
        }

        @Provides
        fun provideProfileDao(db: MnemokotDatabase): ProfileDao = db.profileDao()

        @Provides
        fun provideAccountDao(db: MnemokotDatabase): AccountDao = db.accountDao()

        @Provides
        fun provideMasterSettingsDao(db: MnemokotDatabase): MasterSettingsDao = db.masterSettingsDao()
    }
}
