package ru.man2ll.mnemokot.core.database.di

import android.content.Context
import androidx.room.Room
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.man2ll.mnemokot.core.database.data.dao.AccountDao
import ru.man2ll.mnemokot.core.database.data.dao.ProfileDao
import ru.man2ll.mnemokot.core.database.data.dao.MasterSettingsDao
import ru.man2ll.mnemokot.core.database.data.MnemokotDatabase
import ru.man2ll.mnemokot.core.database.security.PassphraseProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocalDataModule {

    @Provides
    @Singleton
    fun provideSupportOpenHelperFactory(passphraseProvider: PassphraseProvider): SupportOpenHelperFactory {
        val passphrase = passphraseProvider.getPassphrase()
        val factory = SupportOpenHelperFactory(passphrase)
        passphrase.fill(0)
        return factory
    }

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        supportFactory: SupportOpenHelperFactory
    ): MnemokotDatabase {
        return Room.databaseBuilder(
            context,
            MnemokotDatabase::class.java,
            "mnemokot_secure.db"
        )
        .openHelperFactory(supportFactory)
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideProfileDao(db: MnemokotDatabase): ProfileDao = db.profileDao()

    @Provides
    @Singleton
    fun provideAccountDao(db: MnemokotDatabase): AccountDao = db.accountDao()

    @Provides
    @Singleton
    fun provideMasterSettingsDao(db: MnemokotDatabase): MasterSettingsDao = db.masterSettingsDao()
}
