package ru.man2ll.mnemokot.core.database.data

import androidx.room.Database
import androidx.room.RoomDatabase
import ru.man2ll.mnemokot.core.database.data.dao.AccountDao
import ru.man2ll.mnemokot.core.database.data.dao.ProfileDao
import ru.man2ll.mnemokot.core.database.data.dao.MasterSettingsDao
import ru.man2ll.mnemokot.core.database.domain.model.AccountEntity
import ru.man2ll.mnemokot.core.database.domain.model.UserProfileEntity
import ru.man2ll.mnemokot.core.database.domain.model.MasterSettingsEntity

@Database(
    entities = [
        UserProfileEntity::class, 
        AccountEntity::class,
        MasterSettingsEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class MnemokotDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun accountDao(): AccountDao
    abstract fun masterSettingsDao(): MasterSettingsDao
}
