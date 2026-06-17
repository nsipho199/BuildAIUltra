package com.buildai.ultra.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BuildHistoryDao {
    @Query("SELECT * FROM build_history ORDER BY createdAt DESC")
    fun getAllBuilds(): Flow<List<BuildHistoryEntity>>

    @Query("SELECT * FROM build_history WHERE id = :id")
    suspend fun getBuild(id: String): BuildHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBuild(build: BuildHistoryEntity)

    @Query("DELETE FROM build_history WHERE id = :id")
    suspend fun deleteBuild(id: String)

    @Query("DELETE FROM build_history")
    suspend fun clearAll()
}
