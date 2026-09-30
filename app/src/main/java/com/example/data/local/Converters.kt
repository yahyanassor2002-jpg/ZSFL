package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.ReportStatus
import com.example.data.model.Role

class Converters {
  @TypeConverter
  fun fromRole(role: Role): String = role.name

  @TypeConverter
  fun toRole(value: String): Role = runCatching { Role.valueOf(value) }.getOrDefault(Role.HEADMAN)

  @TypeConverter
  fun fromReportStatus(status: ReportStatus): String = status.name

  @TypeConverter
  fun toReportStatus(value: String): ReportStatus =
    runCatching { ReportStatus.valueOf(value) }.getOrDefault(ReportStatus.DRAFT)
}
