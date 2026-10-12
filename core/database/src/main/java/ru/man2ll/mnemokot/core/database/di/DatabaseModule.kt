package ru.man2ll.mnemokot.core.database.di

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteOpenHelper
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
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
            // Переводим временный пароль в ByteArray, как того требует SQLCipher 4.5.4
            val passphraseBytes = "MnemoKot_Temporary_Secure_Salt_Key_Change_Me".toByteArray(Charsets.UTF_8)
            
            // Инициализируем фабрику через массив байт
            val factory: SupportSQLiteOpenHelper.Factory = SupportOpenHelperFactory(passphraseBytes)
            
            // Незамедлительно затираем байты в оперативной памяти ради безопасности
            passphraseBytes.fill(0)

            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                DATABASE_NAME
            )
            .openHelperFactory(factory) // Включаем полнодисковое шифрование AES-256
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
