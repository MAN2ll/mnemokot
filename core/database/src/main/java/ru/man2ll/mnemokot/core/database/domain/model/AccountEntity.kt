package ru.man2ll.mnemokot.core.database.domain.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "accounts",
    foreignKeys = [
        ForeignKey(
            entity = UserProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE // Если профиль удален, удаляются все его пароли
        )
    ],
    indices = [Index(value = ["profileId"])]
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long,          // Привязка к конкретному профилю
    val serviceName: String,       // Например, "Gmail"
    val login: String,             // Логин пользователя
    
    // Параметры генератора паролей AMPG v2
    val isMnemonic: Boolean,       // Создан по фразе или вручную/случайно
    val passwordLength: Int = 16,
    val salt: String = "",
    val rotationDate: String = "", // Зафиксированная дата ротации (например, "1026")
    
    // Хранение явного пароля (если введен вручную или сгенерирован случайно)
    // Будет зашифрован внутри БД через SQLCipher, но для надежности оставим поле nullable
    val encryptedPassword: String? = null,
    
    val updatedAt: Long = System.currentTimeMillis()
)
