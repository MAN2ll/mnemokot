package ru.man2ll.mnemokot.core.database.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportFactory
import ru.man2ll.mnemokot.core.database.data.AppDatabase
import ru.man2ll.mnemokot.core.database.data.dao.AccountDao
import ru.man2ll.mnemokot.core.database.data.dao.ProfileDao
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
        private const val DATABASE_NAME = "mnemokot_secure.db"

        @Provides
        @Singleton
        fun provideAppDatabase(
            @ApplicationContext context: Context
        ): AppDatabase {
            val passphrase = "MnemoKot_Temporary_Secure_Salt_Key_Change_Me".toCharArray()
            val factory = SupportFactory(SupportFactory.toCharArray(passphrase))
            passphrase.fill(' ')

            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                DATABASE_NAME
            )
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()
            .build()
        }

        @Provides
        @Singleton
        fun provideProfileDao(database: AppDatabase): ProfileDao {
            return database.profileDao()
        }

        @Provides
        @Singleton
        fun provideAccountDao(database: AppDatabase): AccountDao {
            return database.accountDao()
        }
    }
}
