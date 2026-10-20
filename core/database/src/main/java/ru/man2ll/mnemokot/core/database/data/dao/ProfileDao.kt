package ru.man2ll.mnemokot.core.database.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.man2ll.mnemokot.core.database.data.model.UserProfileEntity

@Dao
interface ProfileDao {
    @Query("SELECT * FROM user_profiles ORDER BY createdAt DESC")
    fun getAllProfilesFlow(): Flow<List<UserProfileEntity>>

    @Query("SELECT * FROM user_profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: Long): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProfile(profile: UserProfileEntity): Long

    @Update
    suspend fun updateProfile(profile: UserProfileEntity)

    @Delete
    suspend fun deleteProfile(profile: UserProfileEntity)

    @Query("UPDATE user_profiles SET failedAttempts = failedAttempts + 1 WHERE id = :profileId")
    suspend fun incrementFailedAttempts(profileId: Long)

    @Query("UPDATE user_profiles SET failedAttempts = 0 WHERE id = :profileId")
    suspend fun resetFailedAttempts(profileId: Long)
}
