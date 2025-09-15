package com.engfred.lockit.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AuthDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(credential: AuthCredentialEntity)

    @Query("SELECT * FROM auth_credentials LIMIT 1")
    suspend fun get(): AuthCredentialEntity?

    @Query("DELETE FROM auth_credentials")
    suspend fun clear()
}