package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
  @Query("SELECT * FROM users WHERE LOWER(username) = LOWER(:username) LIMIT 1")
  suspend fun getUserByUsername(username: String): UserEntity?

  @Query("SELECT * FROM users ORDER BY username ASC")
  fun getAllUsers(): Flow<List<UserEntity>>

  @Query("SELECT * FROM users ORDER BY username ASC")
  suspend fun getAllUsersList(): List<UserEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertUser(user: UserEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertUsers(users: List<UserEntity>)

  @Query("SELECT COUNT(*) FROM users")
  suspend fun countUsers(): Int
}
