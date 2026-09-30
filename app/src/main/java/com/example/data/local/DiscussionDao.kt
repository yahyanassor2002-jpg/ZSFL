package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.DiscussionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DiscussionDao {
  @Query("SELECT * FROM discussions ORDER BY date DESC, created_at DESC")
  fun getAllDiscussions(): Flow<List<DiscussionEntity>>

  @Query("SELECT * FROM discussions WHERE division = :division ORDER BY date DESC, created_at DESC")
  fun getDiscussionsByDivision(division: String): Flow<List<DiscussionEntity>>

  @Query("SELECT * FROM discussions WHERE id = :id LIMIT 1")
  suspend fun getDiscussionById(id: String): DiscussionEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdate(discussion: DiscussionEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(discussions: List<DiscussionEntity>)

  @Delete
  suspend fun delete(discussion: DiscussionEntity)

  @Query("DELETE FROM discussions WHERE id = :id")
  suspend fun deleteById(id: String)

  @Query("SELECT * FROM discussions")
  suspend fun getAllDiscussionsList(): List<DiscussionEntity>

  @Query("SELECT COUNT(*) FROM discussions")
  suspend fun countDiscussions(): Int
}
