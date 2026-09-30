package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Role {
  ADMIN,
  HEADMAN,
}

enum class ReportStatus {
  DRAFT,
  SUBMITTED,
  PENDING_SYNC,
  SYNCED,
}

@Entity(tableName = "users")
data class UserEntity(
  @PrimaryKey val id: String,
  val username: String,
  @ColumnInfo(name = "password_hash") val passwordHash: String,
  val role: Role,
  @ColumnInfo(name = "zone_id") val zoneId: String? = null,
  @ColumnInfo(name = "phone_number") val phoneNumber: String = "",
  @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "zones")
data class ZoneEntity(
  @PrimaryKey val id: String,
  @ColumnInfo(name = "zone_name") val zoneName: String,
  val description: String = "",
)

@Entity(tableName = "reports")
data class ReportEntity(
  @PrimaryKey val id: String,
  @ColumnInfo(name = "zone_id") val zone: String,
  @ColumnInfo(name = "date_of_operation") val dateOfOperation: String,
  @ColumnInfo(name = "operation_name") val operationName: String,
  @ColumnInfo(name = "block_number") val blockNumber: String,
  @ColumnInfo(name = "contractor_name") val contractorName: String,
  @ColumnInfo(name = "number_of_labourers") val noOfLabourers: Int,
  @ColumnInfo(name = "area_covered") val areaCoveredHa: Double, // Represents Area Covered in Acres
  @ColumnInfo(name = "balance_to_be_done") val balanceToBeDoneHa: Double, // Represents Balance in Acres
  @ColumnInfo(name = "input_name") val inputName: String = "",
  @ColumnInfo(name = "ratoon_date") val ratoonDate: String = "",
  val remark: String = "",
  @ColumnInfo(name = "photo_uri") val photoUri: String? = null,
  @ColumnInfo(name = "created_by") val createdBy: String,
  @ColumnInfo(name = "created_by_role") val createdByRole: Role,
  @ColumnInfo(name = "headman_phone") val headmanPhone: String = "",
  @ColumnInfo(name = "admin_comment") val adminComment: String = "",
  @ColumnInfo(name = "admin_comment_author") val adminCommentAuthor: String = "",
  @ColumnInfo(name = "admin_comment_date") val adminCommentDate: String = "",
  @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false,
  @ColumnInfo(name = "last_updated_by") val lastUpdatedBy: String = createdBy,
  val status: ReportStatus,
  val synced: Boolean = false,
  @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
  @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "discussions")
data class DiscussionEntity(
  @PrimaryKey val id: String,
  val title: String,
  val notes: String,
  val division: String,
  val date: String,
  @ColumnInfo(name = "author_username") val author: String,
  @ColumnInfo(name = "author_role") val authorRole: Role,
  val synced: Boolean = false,
  @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
  @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
  @ColumnInfo(name = "last_updated_by") val lastUpdatedBy: String = author,
)

@Entity(tableName = "zone_progress")
data class ZoneProgressEntity(
  @PrimaryKey val id: String,
  val zone: String,
  val date: String,
  val activity: String,
  @ColumnInfo(name = "progress_status") val progressStatus: String, // "On Track", "Completed", "In Progress", "Delayed"
  @ColumnInfo(name = "percentage") val percentage: Int = 0,
  val remarks: String = "",
  @ColumnInfo(name = "submitted_by") val submittedBy: String,
  val synced: Boolean = false,
  @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
  @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
  @ColumnInfo(name = "last_updated_by") val lastUpdatedBy: String = submittedBy,
)

data class ReportLayoutConfig(
  val compactCards: Boolean = false,
  val showContractor: Boolean = true,
  val showInputName: Boolean = true,
  val showRatoonDate: Boolean = true,
  val showLabourers: Boolean = true,
  val showProgressBar: Boolean = true,
  val sortBy: String = "DATE_DESC", // DATE_DESC, DATE_ASC, ACRES_DESC, ZONE_ASC
)

val STANDARD_ZONES = listOf(
  "ZONE NO 11",
  "ZONE NO 12",
  "ZONE NO 13",
  "ZONE NO 14",
  "ZONE NO 15",
)

val COMMON_DIVISIONS = listOf(
  "North Division",
  "Central Division",
  "South Division",
  "East Division",
  "West Division",
  "Outgrower Sector",
)

val STANDARD_CONTRACTORS = listOf(
  "Raphael",
  "Hussein",
  "Khamis Fumu",
  "Jackson",
)

val COMMON_OPERATIONS = listOf(
  "Stubble shaving",
  "Planting",
  "Off-barring",
  "Ridging",
  "Weeding",
  "Gap Filling",
  "Fertilizer Application",
  "Herbicide Application",
  "Sugarcane Harvesting",
  "Inter-row Cultivation",
  "Ratoon Management",
  "Land Preparation",
  "Subsoiling / Ripping",
)

val COMMON_INPUTS = listOf(
  "Urea 46% N",
  "CAN (Calcium Ammonium Nitrate)",
  "NPK 20-10-10 Compound",
  "NPK 17-17-17 Compound",
  "Glyphosate 480SL",
  "2,4-D Amine 720SL",
  "Atrazine 500SC",
  "Diuron 80WP",
  "Seed Cane Variety R570",
  "Seed Cane Variety NCo376",
  "Filter Mud / Organic Compost",
  "Muriate of Potash (MOP)",
  "Broad-spectrum Pre-emergence",
)

data class OperationWeeklySummary(
  val operationName: String,
  val totalAreaAcres: Double,
  val reportCount: Int,
  val totalLabourers: Int,
  val contractors: List<String>,
  val zones: List<String>,
  val blocks: List<String>,
)
