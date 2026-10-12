package ru.man2ll.mnemokot.core.security.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.man2ll.mnemokot.core.database.domain.repository.MnemokotRepository
import ru.man2ll.mnemokot.core.security.audit.PasswordAuditor
import ru.man2ll.mnemokot.core.security.backup.BackupManager
import ru.man2ll.mnemokot.core.security.crypto.SecurityCryptoEngine
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideSecurityCryptoEngine(): SecurityCryptoEngine {
        return SecurityCryptoEngine()
    }

    @Provides
    @Singleton
    fun provideBackupManager(
        repository: MnemokotRepository,
        cryptoEngine: SecurityCryptoEngine
    ): BackupManager {
        return BackupManager(repository, cryptoEngine)
    }

    @Provides
    @Singleton
    fun providePasswordAuditor(
        repository: MnemokotRepository
    ): PasswordAuditor {
        return PasswordAuditor(repository)
    }
}
