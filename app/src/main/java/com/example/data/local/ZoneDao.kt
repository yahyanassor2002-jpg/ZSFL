package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ZoneEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ZoneDao {
  @Query("SELECT * FROM zones ORDER BY zone_name ASC")
  fun getAllZones(): Flow<List<ZoneEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertZones(zones: List<ZoneEntity>)

  @Query("SELECT COUNT(*) FROM zones")
  suspend fun countZones(): Int
}
