package ru.man2ll.mnemokot.core.security.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.man2ll.mnemokot.core.database.domain.repository.MnemokotRepository
import ru.man2ll.mnemokot.core.security.audit.PasswordAuditor
import ru.man2ll.mnemokot.core.security.backup.BackupManager
import ru.man2ll.mnemokot.core.security.crypto.SecurityCryptoEngine
import ru.man2ll.mnemokot.core.crypto.domain.hasher.PinHasher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {

    @Binds
    @Singleton
    abstract fun bindPinHasher(impl: SecurityCryptoEngine): PinHasher

    companion object {
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
}
