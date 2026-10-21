package ru.man2ll.mnemokot.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.man2ll.mnemokot.core.database.security.PassphraseProvider
import ru.man2ll.mnemokot.security.AndroidPassphraseProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePassphraseProvider(
        @ApplicationContext context: Context
    ): PassphraseProvider {
        return AndroidPassphraseProvider(context)
    }
}
