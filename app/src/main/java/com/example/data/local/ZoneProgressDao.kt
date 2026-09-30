package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ZoneProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ZoneProgressDao {
  @Query("SELECT * FROM zone_progress ORDER BY date DESC, created_at DESC")
  fun getAllZoneProgress(): Flow<List<ZoneProgressEntity>>

  @Query("SELECT * FROM zone_progress WHERE zone = :zone ORDER BY date DESC, created_at DESC")
  fun getProgressByZone(zone: String): Flow<List<ZoneProgressEntity>>

  @Query("SELECT * FROM zone_progress WHERE id = :id LIMIT 1")
  suspend fun getProgressById(id: String): ZoneProgressEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdate(progress: ZoneProgressEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(progressList: List<ZoneProgressEntity>)

  @Delete
  suspend fun delete(progress: ZoneProgressEntity)

  @Query("DELETE FROM zone_progress WHERE id = :id")
  suspend fun deleteById(id: String)

  @Query("SELECT * FROM zone_progress")
  suspend fun getAllZoneProgressList(): List<ZoneProgressEntity>

  @Query("SELECT COUNT(*) FROM zone_progress")
  suspend fun countProgress(): Int
}
