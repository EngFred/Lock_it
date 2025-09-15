package com.engfred.lockit.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [LockedAppEntity::class, AuthCredentialEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun lockedAppDao(): LockedAppDao
    abstract fun authDao(): AuthDao
}