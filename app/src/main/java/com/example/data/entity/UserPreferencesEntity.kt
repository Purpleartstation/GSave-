package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_preferences")
data class UserPreferencesEntity(
    @PrimaryKey val id: Long = 1L,
    val pinCode: String = "1234",
    val isPinEnabled: Boolean = true,
    val googleCalendarSyncEnabled: Boolean = true,
    val darkThemeMode: Boolean = false,
    val isRegistered: Boolean = false,
    val userName: String = "Juan Dela Cruz"
)
