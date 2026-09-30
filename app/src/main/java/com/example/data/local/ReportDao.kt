package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {
  @Query("SELECT * FROM reports ORDER BY date_of_operation DESC, created_at DESC")
  fun getAllReports(): Flow<List<ReportEntity>>

  @Query("SELECT * FROM reports WHERE zone_id = :zone ORDER BY date_of_operation DESC, created_at DESC")
  fun getReportsByZone(zone: String): Flow<List<ReportEntity>>

  @Query("SELECT * FROM reports WHERE id = :id LIMIT 1")
  fun getReportById(id: String): Flow<ReportEntity?>

  @Query("SELECT * FROM reports WHERE id = :id LIMIT 1")
  suspend fun getReportByIdOnce(id: String): ReportEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdateReport(report: ReportEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertReports(reports: List<ReportEntity>)

  @Delete
  suspend fun deleteReport(report: ReportEntity)

  @Query("DELETE FROM reports WHERE id = :id")
  suspend fun deleteReportById(id: String)

  @Query("SELECT * FROM reports")
  suspend fun getAllReportsList(): List<ReportEntity>

  @Query("SELECT COUNT(*) FROM reports WHERE status = 'PENDING_SYNC' OR synced = 0")
  suspend fun getPendingSyncCount(): Int

  @Query("UPDATE reports SET status = 'SYNCED', synced = 1, updated_at = :timestamp WHERE status = 'PENDING_SYNC' OR (status = 'SUBMITTED' AND synced = 0)")
  suspend fun markAllPendingAsSynced(timestamp: Long = System.currentTimeMillis())

  @Query("SELECT COUNT(*) FROM reports")
  suspend fun countReports(): Int
}
