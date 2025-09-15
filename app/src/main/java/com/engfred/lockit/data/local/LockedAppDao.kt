package com.engfred.lockit.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LockedAppDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(lockedApp: LockedAppEntity)

    @Delete
    suspend fun delete(lockedApp: LockedAppEntity)

    @Query("SELECT packageName FROM locked_apps")
    fun getAll(): Flow<List<String>>
}