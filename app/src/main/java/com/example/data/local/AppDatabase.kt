package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.DiscussionEntity
import com.example.data.model.ReportEntity
import com.example.data.model.ReportStatus
import com.example.data.model.Role
import com.example.data.model.STANDARD_ZONES
import com.example.data.model.UserEntity
import com.example.data.model.ZoneEntity
import com.example.data.model.ZoneProgressEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID

fun hashPassword(password: String): String {
  val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray(Charsets.UTF_8))
  return bytes.joinToString("") { "%02x".format(it) }
}

@Database(
  entities = [
    UserEntity::class,
    ZoneEntity::class,
    ReportEntity::class,
    DiscussionEntity::class,
    ZoneProgressEntity::class,
  ],
  version = 4,
  exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
  abstract fun userDao(): UserDao
  abstract fun zoneDao(): ZoneDao
  abstract fun reportDao(): ReportDao
  abstract fun discussionDao(): DiscussionDao
  abstract fun zoneProgressDao(): ZoneProgressDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "upenjanet_database",
        )
          .fallbackToDestructiveMigration()
          .addCallback(DatabaseCallback(scope))
          .build()
        INSTANCE = instance
        instance
      }
    }

    private class DatabaseCallback(
      private val scope: CoroutineScope,
    ) : RoomDatabase.Callback() {
      override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        INSTANCE?.let { database ->
          scope.launch(Dispatchers.IO) {
            populateInitialData(database)
          }
        }
      }
    }

    suspend fun populateInitialData(database: AppDatabase) {
      val userDao = database.userDao()
      val zoneDao = database.zoneDao()
      val reportDao = database.reportDao()
      val discussionDao = database.discussionDao()
      val zoneProgressDao = database.zoneProgressDao()

      // Seed Zones
      if (zoneDao.countZones() == 0) {
        val zones = STANDARD_ZONES.mapIndexed { index, name ->
          ZoneEntity(
            id = "ZONE_${index + 11}",
            zoneName = name,
            description = "Commercial sugarcane plantation sector ${name}",
          )
        }
        zoneDao.insertZones(zones)
      }

      // Seed Users: Admin RASHID (password 2002) + Headman accounts
      if (userDao.countUsers() == 0) {
        val adminUser = UserEntity(
          id = "USR_ADMIN_01",
          username = "RASHID",
          passwordHash = hashPassword("2002"),
          role = Role.ADMIN,
          zoneId = null,
        )
        userDao.insertUser(adminUser)

        // Seed demo headman accounts for field testing
        val headman11 = UserEntity(
          id = "USR_HEADMAN_11",
          username = "headman11",
          passwordHash = hashPassword("1111"),
          role = Role.HEADMAN,
          zoneId = "ZONE NO 11",
        )
        val headman12 = UserEntity(
          id = "USR_HEADMAN_12",
          username = "headman12",
          passwordHash = hashPassword("1212"),
          role = Role.HEADMAN,
          zoneId = "ZONE NO 12",
        )
        userDao.insertUser(headman11)
        userDao.insertUser(headman12)
      }

      val now = System.currentTimeMillis()

      // Seed realistic sample reports for all zones
      if (reportDao.countReports() == 0) {
        val sampleReports = listOf(
          ReportEntity(
            id = "UPJ-2026-0901",
            zone = "ZONE NO 11",
            dateOfOperation = "02/09/2026",
            operationName = "Stubble shaving",
            blockNumber = "B-02",
            contractorName = "Raphael",
            noOfLabourers = 20,
            areaCoveredHa = 45.0,
            balanceToBeDoneHa = 0.0,
            inputName = "Mechanical Disc Shaver",
            ratoonDate = "01/09/2026",
            remark = "Uniform ratoon cut executed cleanly across block B-02.",
            createdBy = "headman11",
            createdByRole = Role.HEADMAN,
            lastUpdatedBy = "headman11",
            status = ReportStatus.SYNCED,
            synced = true,
            createdAt = now - 3600000 * 240,
            updatedAt = now - 3600000 * 240,
          ),
          ReportEntity(
            id = "UPJ-2026-0902",
            zone = "ZONE NO 11",
            dateOfOperation = "05/09/2026",
            operationName = "Stubble shaving",
            blockNumber = "B-04",
            contractorName = "Raphael",
            noOfLabourers = 18,
            areaCoveredHa = 35.0,
            balanceToBeDoneHa = 5.0,
            inputName = "Mechanical Disc Shaver",
            ratoonDate = "03/09/2026",
            remark = "Second phase of stubble shaving completed. Total 80 Ac covered in week 1.",
            createdBy = "headman11",
            createdByRole = Role.HEADMAN,
            lastUpdatedBy = "headman11",
            status = ReportStatus.SYNCED,
            synced = true,
            createdAt = now - 3600000 * 180,
            updatedAt = now - 3600000 * 180,
          ),
          ReportEntity(
            id = "UPJ-2026-0903",
            zone = "ZONE NO 12",
            dateOfOperation = "03/09/2026",
            operationName = "Planting",
            blockNumber = "C-05",
            contractorName = "Hussein",
            noOfLabourers = 28,
            areaCoveredHa = 35.5,
            balanceToBeDoneHa = 10.0,
            inputName = "Seed Cane Variety R570",
            ratoonDate = "",
            remark = "Primary furrow planting completed with healthy seed setts.",
            createdBy = "RASHID",
            createdByRole = Role.ADMIN,
            lastUpdatedBy = "RASHID",
            status = ReportStatus.SUBMITTED,
            synced = true,
            createdAt = now - 3600000 * 210,
            updatedAt = now - 3600000 * 210,
          ),
          ReportEntity(
            id = "UPJ-2026-0904",
            zone = "ZONE NO 13",
            dateOfOperation = "04/09/2026",
            operationName = "Fertilizer Application",
            blockNumber = "D-08",
            contractorName = "Khamis Fumu",
            noOfLabourers = 22,
            areaCoveredHa = 28.0,
            balanceToBeDoneHa = 4.0,
            inputName = "Urea 46% N",
            ratoonDate = "28/08/2026",
            remark = "Basal top-dressing broadcast along ridges.",
            createdBy = "RASHID",
            createdByRole = Role.ADMIN,
            lastUpdatedBy = "RASHID",
            status = ReportStatus.SYNCED,
            synced = true,
            createdAt = now - 3600000 * 190,
            updatedAt = now - 3600000 * 190,
          ),
          ReportEntity(
            id = "UPJ-2026-0905",
            zone = "ZONE NO 14",
            dateOfOperation = "06/09/2026",
            operationName = "Weeding",
            blockNumber = "E-12",
            contractorName = "Jackson",
            noOfLabourers = 16,
            areaCoveredHa = 16.0,
            balanceToBeDoneHa = 2.0,
            inputName = "2,4-D Amine 720SL",
            ratoonDate = "30/08/2026",
            remark = "Manual spot-weeding along edge rows.",
            createdBy = "headman11",
            createdByRole = Role.HEADMAN,
            lastUpdatedBy = "headman11",
            status = ReportStatus.PENDING_SYNC,
            synced = false,
            createdAt = now - 3600000 * 150,
            updatedAt = now - 3600000 * 150,
          ),
          ReportEntity(
            id = "UPJ-2026-1101",
            zone = "ZONE NO 11",
            dateOfOperation = "11/09/2026",
            operationName = "Off-barring",
            blockNumber = "B-24",
            contractorName = "Raphael",
            noOfLabourers = 25,
            areaCoveredHa = 3.5,
            balanceToBeDoneHa = 1.5,
            inputName = "Filter Mud / Bagasse Compost",
            ratoonDate = "10/09/2026",
            remark = "Tractor cultivator operated smoothly along cane rows.",
            createdBy = "RASHID",
            createdByRole = Role.ADMIN,
            lastUpdatedBy = "RASHID",
            status = ReportStatus.SUBMITTED,
            synced = true,
            createdAt = now - 3600000 * 24,
            updatedAt = now - 3600000 * 24,
          ),
          ReportEntity(
            id = "UPJ-2026-1501",
            zone = "ZONE NO 15",
            dateOfOperation = "12/09/2026",
            operationName = "Sugarcane Harvesting",
            blockNumber = "F-22",
            contractorName = "Hussein",
            noOfLabourers = 45,
            areaCoveredHa = 6.4,
            balanceToBeDoneHa = 3.6,
            inputName = "N/A - Cane Harvesting",
            ratoonDate = "01/09/2026",
            remark = "High sucrose content reported. Cane loaded onto haulage trucks.",
            createdBy = "RASHID",
            createdByRole = Role.ADMIN,
            lastUpdatedBy = "RASHID",
            status = ReportStatus.SYNCED,
            synced = true,
            createdAt = now - 3600000 * 12,
            updatedAt = now - 3600000 * 12,
          ),
        )
        sampleReports.forEach { reportDao.insertOrUpdateReport(it) }
      }

      // Seed Division Discussions
      if (discussionDao.countDiscussions() == 0) {
        val sampleDiscussions = listOf(
          DiscussionEntity(
            id = "DISC_01",
            title = "Irrigation Canal Desilting Schedule",
            notes = "Main feeder canal to Zone 11 and 12 requires dredging before short rains. Excavator booked for Monday 15th.",
            division = "North Division",
            date = "12/09/2026",
            author = "RASHID",
            authorRole = Role.ADMIN,
            synced = true,
            createdAt = now - 3600000 * 18,
            updatedAt = now - 3600000 * 18,
          ),
          DiscussionEntity(
            id = "DISC_02",
            title = "Cane Haulage Fleet Dispatch Coordination",
            notes = "Haulage trailers 10T to be prioritized for Zone 15 block F-22 harvest to prevent cane staling at loading pads.",
            division = "Central Division",
            date = "11/09/2026",
            author = "RASHID",
            authorRole = Role.ADMIN,
            synced = true,
            createdAt = now - 3600000 * 36,
            updatedAt = now - 3600000 * 36,
          ),
          DiscussionEntity(
            id = "DISC_03",
            title = "Ratoon Stool Eradication & Gap Filling Strategy",
            notes = "Zone 13 ratoon crop shows 12% gap rate. Headman13 requested 2.5 tonnes of R570 seed cane for immediate gap filling.",
            division = "South Division",
            date = "10/09/2026",
            author = "headman11",
            authorRole = Role.HEADMAN,
            synced = true,
            createdAt = now - 3600000 * 50,
            updatedAt = now - 3600000 * 50,
          ),
        )
        discussionDao.insertAll(sampleDiscussions)
      }

      // Seed Zone Progress
      if (zoneProgressDao.countProgress() == 0) {
        val sampleProgress = listOf(
          ZoneProgressEntity(
            id = "PROG_11",
            zone = "ZONE NO 11",
            date = "12/09/2026",
            activity = "Planting & Basal Fertilizer",
            progressStatus = "On Track",
            percentage = 78,
            remarks = "Seed setts planted across 28 Acres out of 36 Acres target.",
            submittedBy = "headman11",
            synced = true,
            createdAt = now - 3600000 * 12,
            updatedAt = now - 3600000 * 12,
          ),
          ZoneProgressEntity(
            id = "PROG_12",
            zone = "ZONE NO 12",
            date = "11/09/2026",
            activity = "Off-barring & Cultivation",
            progressStatus = "In Progress",
            percentage = 62,
            remarks = "Tractor speed maintained at 4 km/h to prevent stool damage.",
            submittedBy = "headman12",
            synced = true,
            createdAt = now - 3600000 * 20,
            updatedAt = now - 3600000 * 20,
          ),
          ZoneProgressEntity(
            id = "PROG_13",
            zone = "ZONE NO 13",
            date = "11/09/2026",
            activity = "Chemical Weeding (Pre-emergence)",
            progressStatus = "Delayed",
            percentage = 45,
            remarks = "Delayed by windy afternoon; spraying halted for drift prevention.",
            submittedBy = "RASHID",
            synced = true,
            createdAt = now - 3600000 * 28,
            updatedAt = now - 3600000 * 28,
          ),
          ZoneProgressEntity(
            id = "PROG_15",
            zone = "ZONE NO 15",
            date = "10/09/2026",
            activity = "Sugarcane Harvesting",
            progressStatus = "Completed",
            percentage = 100,
            remarks = "Target block completed. Total 420 Metric Tonnes delivered to weighbridge.",
            submittedBy = "RASHID",
            synced = true,
            createdAt = now - 3600000 * 40,
            updatedAt = now - 3600000 * 40,
          ),
        )
        zoneProgressDao.insertAll(sampleProgress)
      }
    }
  }
}
