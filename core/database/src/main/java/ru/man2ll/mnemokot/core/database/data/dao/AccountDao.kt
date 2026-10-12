package ru.man2ll.mnemokot.core.database.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.man2ll.mnemokot.core.database.domain.model.AccountEntity

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE profileId = :profileId ORDER BY serviceName ASC")
    fun getAccountsByProfileFlow(profileId: Long): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: Long): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    @Query("SELECT COUNT(*) FROM accounts WHERE profileId = :profileId")
    suspend fun getAccountCountForProfile(profileId: Long): Int
}
