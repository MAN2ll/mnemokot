package ru.man2ll.mnemokot.core.database.data.dao

import androidx.room.*
import ru.man2ll.mnemokot.core.database.domain.model.MasterSettingsEntity

@Dao
interface MasterSettingsDao {
    @Query("SELECT * FROM master_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): MasterSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: MasterSettingsEntity)
}
