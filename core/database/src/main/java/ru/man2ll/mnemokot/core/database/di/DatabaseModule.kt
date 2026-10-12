package ru.man2ll.mnemokot.core.database.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportFactory
import ru.man2ll.mnemokot.core.database.data.AppDatabase
import ru.man2ll.mnemokot.core.database.data.dao.AccountDao
import ru.man2ll.mnemokot.core.database.data.dao.ProfileDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DATABASE_NAME = "mnemokot_secure.db"

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        // ВАЖНО: На этапе инициализации приложения здесь будет запрашиваться 
        // ключ, полученный из мастер-пароля. Для компиляции каркаса мы закладываем 
        // детерминированную фабрику шифрования SQLCipher.
        // Очищаем passphrase из памяти сразу после использования (безопасность CharArray/ByteArray)
        val passphrase = "MnemoKot_Temporary_Secure_Salt_Key_Change_Me".toCharArray()
        val factory = SupportFactory(SupportFactory.toCharArray(passphrase))
        
        // Очищаем массив символов из памяти ради безопасности
        passphrase.fill(' ')

        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            DATABASE_NAME
        )
        .openHelperFactory(factory) // Подключаем шифрование на уровне диска
        .fallbackToDestructiveMigration() // Защита от падений при разработке схем данных
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
