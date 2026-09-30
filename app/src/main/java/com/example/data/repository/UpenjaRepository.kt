package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.hashPassword
import com.example.data.model.*
import com.example.data.remote.CloudSyncService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class CloudSyncStatus {
  SYNCED,
  SYNCING,
  OFFLINE,
  PENDING_UPLOAD,
}

class UpenjaRepository(
  private val context: Context,
  private val database: AppDatabase,
  private val sessionManager: SessionManager,
  private val scope: CoroutineScope,
) {
  private val userDao = database.userDao()
  private val zoneDao = database.zoneDao()
  private val reportDao = database.reportDao()
  private val discussionDao = database.discussionDao()
  private val zoneProgressDao = database.zoneProgressDao()

  private val cloudSyncService = CloudSyncService(context)

  private val _currentUser = MutableStateFlow<UserEntity?>(null)
  val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

  val allReports: Flow<List<ReportEntity>> = reportDao.getAllReports()
  val allDiscussions: Flow<List<DiscussionEntity>> = discussionDao.getAllDiscussions()
  val allZoneProgress: Flow<List<ZoneProgressEntity>> = zoneProgressDao.getAllZoneProgress()
  val allZones: Flow<List<ZoneEntity>> = zoneDao.getAllZones()
  val allUsers: Flow<List<UserEntity>> = userDao.getAllUsers()

  private val _isSyncing = MutableStateFlow(false)
  val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

  private val _syncStatus = MutableStateFlow(CloudSyncStatus.SYNCED)
  val syncStatus: StateFlow<CloudSyncStatus> = _syncStatus.asStateFlow()

  private val _lastSyncMessage = MutableStateFlow<String?>("Cloud synchronization ready")
  val lastSyncMessage: StateFlow<String?> = _lastSyncMessage.asStateFlow()

  private val _lastSyncTimestamp = MutableStateFlow(System.currentTimeMillis())
  val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

  private val _pendingSyncCount = MutableStateFlow(0)
  val pendingSyncCount: StateFlow<Int> = _pendingSyncCount.asStateFlow()

  init {
    // Check initial seed, restore session, and start cloud sync
    scope.launch(Dispatchers.IO) {
      if (userDao.countUsers() == 0 || zoneDao.countZones() == 0) {
        AppDatabase.populateInitialData(database)
      }
      val saved = sessionManager.getSavedSession()
      if (saved != null) {
        _currentUser.value = saved
      }

      updatePendingCount()

      // Initial cloud sync on startup
      syncNow()

      // High-frequency periodic sync every 10 seconds to keep Headman & Admin in near-instant sync
      while (isActive) {
        delay(10_000)
        try {
          if (cloudSyncService.isNetworkAvailable()) {
            syncNow()
          } else {
            _syncStatus.value = CloudSyncStatus.OFFLINE
          }
        } catch (e: Exception) {
          Log.w("UpenjaRepo", "Periodic sync skipped: ${e.message}")
        }
      }
    }
  }

  suspend fun updatePendingCount() = withContext(Dispatchers.IO) {
    try {
      val count = reportDao.getPendingSyncCount()
      _pendingSyncCount.value = count
      if (count > 0 && _syncStatus.value != CloudSyncStatus.SYNCING) {
        _syncStatus.value = CloudSyncStatus.PENDING_UPLOAD
      }
    } catch (e: Exception) {
      Log.w("UpenjaRepo", "Could not check pending count: ${e.message}")
    }
  }

  suspend fun login(username: String, rawPassword: String): Result<UserEntity> = withContext(Dispatchers.IO) {
    val cleanUsername = username.trim()
    if (cleanUsername.isEmpty() || rawPassword.isEmpty()) {
      return@withContext Result.failure(IllegalArgumentException("Please enter both username and password."))
    }

    if (userDao.countUsers() == 0) {
      AppDatabase.populateInitialData(database)
    }

    var user = userDao.getUserByUsername(cleanUsername)
    if (user == null && cleanUsername.equals("RASHID", ignoreCase = true)) {
      AppDatabase.populateInitialData(database)
      user = userDao.getUserByUsername("RASHID")
    }

    if (user == null) {
      return@withContext Result.failure(IllegalArgumentException("User '$cleanUsername' not found."))
    }

    val inputHash = hashPassword(rawPassword)
    if (user.passwordHash != inputHash) {
      return@withContext Result.failure(IllegalArgumentException("Invalid password. Please check your credentials."))
    }

    sessionManager.saveUserSession(user)
    _currentUser.value = user

    // Trigger sync on login so user gets freshest shared data
    scope.launch(Dispatchers.IO) { syncNow() }

    Result.success(user)
  }

  fun logout() {
    sessionManager.clearSession()
    _currentUser.value = null
  }

  fun switchUser(user: UserEntity) {
    sessionManager.saveUserSession(user)
    _currentUser.value = user
    scope.launch(Dispatchers.IO) { syncNow() }
  }

  // --- CLOUD SYNCHRONIZATION ENGINE ---
  suspend fun syncNow(): Result<Int> = withContext(Dispatchers.IO) {
    if (_isSyncing.value) {
      return@withContext Result.success(0)
    }

    if (!cloudSyncService.isNetworkAvailable()) {
      _syncStatus.value = CloudSyncStatus.OFFLINE
      _lastSyncMessage.value = "Offline: No connection. Data stored safely in local database."
      updatePendingCount()
      return@withContext Result.failure(IllegalStateException("Device is offline. Reports saved locally."))
    }

    _isSyncing.value = true
    _syncStatus.value = CloudSyncStatus.SYNCING
    _lastSyncMessage.value = "Syncing with shared cloud database..."

    try {
      // 1. Fetch remote shared data
      val remoteResult = cloudSyncService.fetchCloudData()
      val remoteData = remoteResult.getOrNull()

      // 2. Read local data from Room
      val localReports = reportDao.getAllReportsList()
      val localDiscussions = discussionDao.getAllDiscussionsList()
      val localProgress = zoneProgressDao.getAllZoneProgressList()
      val localUsers = userDao.getAllUsersList()

      // Handle deleted reports from cloud tombstone
      val remoteDeletedIds = remoteData?.deletedReportIds.orEmpty().toSet()
      for (delId in remoteDeletedIds) {
        val rep = localReports.find { it.id == delId }
        if (rep != null) {
          reportDao.deleteReport(rep)
        }
      }

      // 3. Merge Reports (Conflict resolution by updatedAt, omitting deleted)
      val mergedReportsMap = mutableMapOf<String, ReportEntity>()
      localReports.filterNot { remoteDeletedIds.contains(it.id) || it.isDeleted }.forEach { mergedReportsMap[it.id] = it }
      remoteData?.reports?.filterNot { remoteDeletedIds.contains(it.id) || it.isDeleted }?.forEach { remoteRep ->
        val existing = mergedReportsMap[remoteRep.id]
        if (existing == null || remoteRep.updatedAt >= existing.updatedAt) {
          mergedReportsMap[remoteRep.id] = remoteRep.copy(synced = true, status = ReportStatus.SYNCED)
        }
      }
      val mergedReports = mergedReportsMap.values.toList()

      // 4. Merge Discussions
      val mergedDiscussionsMap = mutableMapOf<String, DiscussionEntity>()
      localDiscussions.forEach { mergedDiscussionsMap[it.id] = it }
      remoteData?.discussions?.forEach { remoteDisc ->
        val existing = mergedDiscussionsMap[remoteDisc.id]
        if (existing == null || remoteDisc.updatedAt >= existing.updatedAt) {
          mergedDiscussionsMap[remoteDisc.id] = remoteDisc.copy(synced = true)
        }
      }
      val mergedDiscussions = mergedDiscussionsMap.values.toList()

      // 5. Merge Zone Progress
      val mergedProgressMap = mutableMapOf<String, ZoneProgressEntity>()
      localProgress.forEach { mergedProgressMap[it.id] = it }
      remoteData?.zoneProgress?.forEach { remoteProg ->
        val existing = mergedProgressMap[remoteProg.id]
        if (existing == null || remoteProg.updatedAt >= existing.updatedAt) {
          mergedProgressMap[remoteProg.id] = remoteProg.copy(synced = true)
        }
      }
      val mergedProgress = mergedProgressMap.values.toList()

      // 6. Merge Users (including phone numbers)
      val mergedUsersMap = mutableMapOf<String, UserEntity>()
      localUsers.forEach { mergedUsersMap[it.id] = it }
      remoteData?.users?.forEach { remoteUser ->
        val existing = mergedUsersMap[remoteUser.id]
        if (existing == null) {
          mergedUsersMap[remoteUser.id] = remoteUser
        } else if (existing.phoneNumber.isBlank() && remoteUser.phoneNumber.isNotBlank()) {
          mergedUsersMap[remoteUser.id] = existing.copy(phoneNumber = remoteUser.phoneNumber)
        }
      }
      val mergedUsers = mergedUsersMap.values.toList()

      // 7. Upload merged payload back to cloud database
      val uploadResult = cloudSyncService.uploadCloudData(
        CloudSyncService.CloudDataPayload(
          reports = mergedReports,
          discussions = mergedDiscussions,
          zoneProgress = mergedProgress,
          users = mergedUsers,
          deletedReportIds = remoteDeletedIds.toList(),
          lastUpdated = System.currentTimeMillis(),
        )
      )

      if (uploadResult.isFailure) {
        throw uploadResult.exceptionOrNull() ?: Exception("Failed uploading merged state to cloud")
      }

      // 8. Persist merged records into Room local database
      reportDao.insertReports(mergedReports.map { it.copy(synced = true, status = if (it.status == ReportStatus.DRAFT) ReportStatus.DRAFT else ReportStatus.SYNCED) })
      discussionDao.insertAll(mergedDiscussions.map { it.copy(synced = true) })
      zoneProgressDao.insertAll(mergedProgress.map { it.copy(synced = true) })
      userDao.insertUsers(mergedUsers)

      val count = mergedReports.size
      _syncStatus.value = CloudSyncStatus.SYNCED
      _lastSyncMessage.value = "Synchronized: $count records active across all devices."
      _lastSyncTimestamp.value = System.currentTimeMillis()
      _pendingSyncCount.value = 0

      Result.success(count)
    } catch (e: Exception) {
      Log.e("UpenjaRepo", "Sync error: ${e.message}", e)
      _lastSyncMessage.value = "Sync notice: ${e.localizedMessage ?: "Network issue"}. Local data remains safe."
      updatePendingCount()
      Result.failure(e)
    } finally {
      _isSyncing.value = false
    }
  }

  // --- FARM REPORTS CRUD ---
  suspend fun saveReport(
    id: String?,
    zone: String,
    dateOfOperation: String,
    operationName: String,
    blockNumber: String,
    contractorName: String,
    noOfLabourers: Int,
    areaCoveredHa: Double,
    balanceToBeDoneHa: Double,
    inputName: String,
    ratoonDate: String,
    remark: String,
    photoUri: String? = null,
    isDraft: Boolean,
  ): Result<ReportEntity> = withContext(Dispatchers.IO) {
    val user = _currentUser.value
      ?: return@withContext Result.failure(IllegalStateException("No logged in user found."))

    if (!isDraft) {
      if (zone.isBlank()) return@withContext Result.failure(IllegalArgumentException("Please select a Zone."))
      if (operationName.isBlank()) return@withContext Result.failure(IllegalArgumentException("Operation Name is required."))
      if (blockNumber.isBlank()) return@withContext Result.failure(IllegalArgumentException("Block Number is required."))
      if (contractorName.isBlank()) return@withContext Result.failure(IllegalArgumentException("Contractor Name is required."))
      if (noOfLabourers < 0) return@withContext Result.failure(IllegalArgumentException("Number of labourers cannot be negative."))
      if (areaCoveredHa < 0.0) return@withContext Result.failure(IllegalArgumentException("Area covered cannot be negative."))
      if (balanceToBeDoneHa < 0.0) return@withContext Result.failure(IllegalArgumentException("Balance to be done cannot be negative."))
    }

    val now = System.currentTimeMillis()
    val reportId = if (!id.isNullOrBlank()) id else generateReportId(zone)
    val existing = id?.let { reportDao.getReportByIdOnce(it) }

    val status = if (isDraft) ReportStatus.DRAFT else ReportStatus.PENDING_SYNC

    val report = ReportEntity(
      id = reportId,
      zone = zone.ifBlank { "ZONE NO 11" },
      dateOfOperation = dateOfOperation.ifBlank {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
      },
      operationName = operationName.trim(),
      blockNumber = blockNumber.trim().uppercase(Locale.getDefault()),
      contractorName = contractorName.trim(),
      noOfLabourers = noOfLabourers,
      areaCoveredHa = areaCoveredHa,
      balanceToBeDoneHa = balanceToBeDoneHa,
      inputName = inputName.trim(),
      ratoonDate = ratoonDate.trim(),
      remark = remark.trim(),
      photoUri = photoUri ?: existing?.photoUri,
      createdBy = existing?.createdBy ?: user.username,
      createdByRole = existing?.createdByRole ?: user.role,
      headmanPhone = existing?.headmanPhone?.ifBlank { user.phoneNumber } ?: user.phoneNumber,
      adminComment = existing?.adminComment.orEmpty(),
      adminCommentAuthor = existing?.adminCommentAuthor.orEmpty(),
      adminCommentDate = existing?.adminCommentDate.orEmpty(),
      isDeleted = false,
      lastUpdatedBy = user.username,
      status = status,
      synced = false,
      createdAt = existing?.createdAt ?: now,
      updatedAt = now,
    )

    reportDao.insertOrUpdateReport(report)
    updatePendingCount()

    // Trigger immediate cloud synchronization so User B sees the change in real-time!
    scope.launch(Dispatchers.IO) { syncNow() }

    Result.success(report)
  }

  suspend fun deleteReport(actingUser: UserEntity, reportId: String): Result<Unit> = withContext(Dispatchers.IO) {
    if (actingUser.role != Role.ADMIN) {
      return@withContext Result.failure(
        SecurityException("Restricted: Only Supervisors and Administrators have permission to delete reports.")
      )
    }

    val report = reportDao.getReportByIdOnce(reportId)
      ?: return@withContext Result.failure(NoSuchElementException("Report not found."))

    reportDao.deleteReport(report)

    // Remove from cloud database and record tombstone so other devices delete it
    scope.launch(Dispatchers.IO) {
      try {
        val currentRemote = cloudSyncService.fetchCloudData().getOrNull()
        if (currentRemote != null) {
          val updatedReports = currentRemote.reports.filterNot { it.id == reportId }
          val updatedDeleted = (currentRemote.deletedReportIds + reportId).distinct()
          cloudSyncService.uploadCloudData(
            currentRemote.copy(
              reports = updatedReports,
              deletedReportIds = updatedDeleted,
              lastUpdated = System.currentTimeMillis(),
            )
          )
        }
      } catch (e: Exception) {
        Log.w("UpenjaRepo", "Failed to sync remote delete: ${e.message}")
      }
    }

    updatePendingCount()
    Result.success(Unit)
  }

  suspend fun addAdminComment(actingUser: UserEntity, reportId: String, comment: String): Result<ReportEntity> = withContext(Dispatchers.IO) {
    if (actingUser.role != Role.ADMIN) {
      return@withContext Result.failure(
        SecurityException("Restricted: Only Supervisors and Administrators can comment on reports.")
      )
    }

    val existing = reportDao.getReportByIdOnce(reportId)
      ?: return@withContext Result.failure(NoSuchElementException("Report not found."))

    val now = System.currentTimeMillis()
    val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(now))
    val updated = existing.copy(
      adminComment = comment.trim(),
      adminCommentAuthor = actingUser.username,
      adminCommentDate = dateStr,
      updatedAt = now,
      synced = false,
    )

    reportDao.insertOrUpdateReport(updated)
    updatePendingCount()
    scope.launch(Dispatchers.IO) { syncNow() }
    Result.success(updated)
  }

  // --- DIVISION DISCUSSIONS CRUD ---
  suspend fun saveDiscussion(
    id: String?,
    title: String,
    notes: String,
    division: String,
    date: String,
  ): Result<DiscussionEntity> = withContext(Dispatchers.IO) {
    val user = _currentUser.value
      ?: return@withContext Result.failure(IllegalStateException("No logged in user found."))

    if (title.isBlank()) return@withContext Result.failure(IllegalArgumentException("Discussion title is required."))
    if (notes.isBlank()) return@withContext Result.failure(IllegalArgumentException("Discussion notes cannot be empty."))

    val now = System.currentTimeMillis()
    val discId = if (!id.isNullOrBlank()) id else "DISC_${UUID.randomUUID().toString().take(8)}"
    val existing = id?.let { discussionDao.getDiscussionById(it) }

    val discussion = DiscussionEntity(
      id = discId,
      title = title.trim(),
      notes = notes.trim(),
      division = division.ifBlank { "North Division" },
      date = date.ifBlank { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) },
      author = existing?.author ?: user.username,
      authorRole = existing?.authorRole ?: user.role,
      lastUpdatedBy = user.username,
      synced = false,
      createdAt = existing?.createdAt ?: now,
      updatedAt = now,
    )

    discussionDao.insertOrUpdate(discussion)
    scope.launch(Dispatchers.IO) { syncNow() }

    Result.success(discussion)
  }

  suspend fun deleteDiscussion(actingUser: UserEntity, discussionId: String): Result<Unit> = withContext(Dispatchers.IO) {
    val discussion = discussionDao.getDiscussionById(discussionId)
      ?: return@withContext Result.failure(NoSuchElementException("Discussion not found."))

    if (actingUser.role != Role.ADMIN && discussion.author != actingUser.username) {
      return@withContext Result.failure(SecurityException("Only Administrators or the discussion author can delete this discussion."))
    }

    discussionDao.deleteById(discussionId)

    scope.launch(Dispatchers.IO) {
      try {
        val currentRemote = cloudSyncService.fetchCloudData().getOrNull()
        if (currentRemote != null) {
          val updatedList = currentRemote.discussions.filterNot { it.id == discussionId }
          cloudSyncService.uploadCloudData(currentRemote.copy(discussions = updatedList))
        }
      } catch (e: Exception) {
        Log.w("UpenjaRepo", "Failed sync remote delete discussion: ${e.message}")
      }
    }

    Result.success(Unit)
  }

  // --- ZONE PROGRESS CRUD ---
  suspend fun saveZoneProgress(
    id: String?,
    zone: String,
    date: String,
    activity: String,
    progressStatus: String,
    percentage: Int,
    remarks: String,
  ): Result<ZoneProgressEntity> = withContext(Dispatchers.IO) {
    val user = _currentUser.value
      ?: return@withContext Result.failure(IllegalStateException("No logged in user found."))

    if (activity.isBlank()) return@withContext Result.failure(IllegalArgumentException("Activity description is required."))

    val now = System.currentTimeMillis()
    val progId = if (!id.isNullOrBlank()) id else "PROG_${UUID.randomUUID().toString().take(8)}"
    val existing = id?.let { zoneProgressDao.getProgressById(it) }

    val progress = ZoneProgressEntity(
      id = progId,
      zone = zone.ifBlank { "ZONE NO 11" },
      date = date.ifBlank { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) },
      activity = activity.trim(),
      progressStatus = progressStatus.ifBlank { "In Progress" },
      percentage = percentage.coerceIn(0, 100),
      remarks = remarks.trim(),
      submittedBy = existing?.submittedBy ?: user.username,
      lastUpdatedBy = user.username,
      synced = false,
      createdAt = existing?.createdAt ?: now,
      updatedAt = now,
    )

    zoneProgressDao.insertOrUpdate(progress)
    scope.launch(Dispatchers.IO) { syncNow() }

    Result.success(progress)
  }

  suspend fun deleteZoneProgress(actingUser: UserEntity, progressId: String): Result<Unit> = withContext(Dispatchers.IO) {
    if (actingUser.role != Role.ADMIN) {
      return@withContext Result.failure(SecurityException("Only Administrators can delete Zone Progress records."))
    }
    zoneProgressDao.deleteById(progressId)

    scope.launch(Dispatchers.IO) {
      try {
        val currentRemote = cloudSyncService.fetchCloudData().getOrNull()
        if (currentRemote != null) {
          val updatedList = currentRemote.zoneProgress.filterNot { it.id == progressId }
          cloudSyncService.uploadCloudData(currentRemote.copy(zoneProgress = updatedList))
        }
      } catch (e: Exception) {
        Log.w("UpenjaRepo", "Failed sync remote delete progress: ${e.message}")
      }
    }

    Result.success(Unit)
  }

  // --- WEEKLY EQUIPMENT CRUD ---
  suspend fun saveWeeklyEquipment(
    id: String?,
    weekDate: String,
    zoneOrDivision: String,
    equipmentName: String,
    requiredQuantity: Int,
    availableQuantity: Int,
    remarks: String,
  ): Result<WeeklyEquipmentEntity> = withContext(Dispatchers.IO) {
    val user = _currentUser.value
      ?: return@withContext Result.failure(IllegalStateException("No logged in user found."))

    if (equipmentName.isBlank()) return@withContext Result.failure(IllegalArgumentException("Equipment Name is required."))
    if (requiredQuantity < 0 || availableQuantity < 0) return@withContext Result.failure(IllegalArgumentException("Quantities cannot be negative."))

    val now = System.currentTimeMillis()
    val eqId = if (!id.isNullOrBlank()) id else "EQ_${UUID.randomUUID().toString().take(8)}"
    val existing = id?.let { weeklyEquipmentDao.getEquipmentById(it) }

    val shortage = maxOf(0, requiredQuantity - availableQuantity)

    val equipment = WeeklyEquipmentEntity(
      id = eqId,
      weekDate = weekDate.ifBlank { "Week of ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}" },
      zoneOrDivision = zoneOrDivision.ifBlank { "ZONE NO 11" },
      equipmentName = equipmentName.trim(),
      requiredQuantity = requiredQuantity,
      availableQuantity = availableQuantity,
      shortageQuantity = shortage,
      remarks = remarks.trim(),
      submittedBy = existing?.submittedBy ?: user.username,
      lastUpdatedBy = user.username,
      synced = false,
      createdAt = existing?.createdAt ?: now,
      updatedAt = now,
    )

    weeklyEquipmentDao.insertOrUpdate(equipment)
    scope.launch(Dispatchers.IO) { syncNow() }

    Result.success(equipment)
  }

  suspend fun deleteWeeklyEquipment(actingUser: UserEntity, equipmentId: String): Result<Unit> = withContext(Dispatchers.IO) {
    if (actingUser.role != Role.ADMIN) {
      return@withContext Result.failure(SecurityException("Only Administrators can delete Equipment requirements."))
    }
    weeklyEquipmentDao.deleteById(equipmentId)

    scope.launch(Dispatchers.IO) {
      try {
        val currentRemote = cloudSyncService.fetchCloudData().getOrNull()
        if (currentRemote != null) {
          val updatedList = currentRemote.equipment.filterNot { it.id == equipmentId }
          cloudSyncService.uploadCloudData(currentRemote.copy(equipment = updatedList))
        }
      } catch (e: Exception) {
        Log.w("UpenjaRepo", "Failed sync remote delete equipment: ${e.message}")
      }
    }

    Result.success(Unit)
  }

  // --- USER MANAGEMENT ---
  suspend fun createHeadmanUser(
    actingUser: UserEntity,
    username: String,
    rawPassword: String,
    zone: String,
  ): Result<UserEntity> = withContext(Dispatchers.IO) {
    if (actingUser.role != Role.ADMIN) {
      return@withContext Result.failure(SecurityException("Only Administrators can create new user accounts."))
    }
    val cleanUsername = username.trim()
    if (cleanUsername.length < 3) {
      return@withContext Result.failure(IllegalArgumentException("Username must be at least 3 characters long."))
    }
    if (rawPassword.length < 4) {
      return@withContext Result.failure(IllegalArgumentException("Password must be at least 4 characters long."))
    }
    if (userDao.getUserByUsername(cleanUsername) != null) {
      return@withContext Result.failure(IllegalArgumentException("Username '$cleanUsername' already exists."))
    }

    val newUser = UserEntity(
      id = "USR_HEADMAN_${UUID.randomUUID().toString().take(8)}",
      username = cleanUsername,
      passwordHash = hashPassword(rawPassword),
      role = Role.HEADMAN,
      zoneId = zone,
    )
    userDao.insertUser(newUser)
    scope.launch(Dispatchers.IO) { syncNow() }
    Result.success(newUser)
  }

  fun exportReportsToCsv(reports: List<ReportEntity>): String {
    val sb = java.lang.StringBuilder()
    sb.append("Report ID,Zone,Date,Operation,Block,Contractor,Labourers,Area Covered (Acres),Balance (Acres),Input Name,Ratoon Date,Created By,Status,Last Updated\n")
    for (r in reports) {
      sb.append("\"${r.id}\",\"${r.zone}\",\"${r.dateOfOperation}\",\"${r.operationName}\",\"${r.blockNumber}\",\"${r.contractorName}\",${r.noOfLabourers},${r.areaCoveredHa},${r.balanceToBeDoneHa},\"${r.inputName}\",\"${r.ratoonDate}\",\"${r.createdBy}\",\"${r.status.name}\",\"${r.lastUpdatedBy}\"\n")
    }
    return sb.toString()
  }

  fun exportReportsToFormattedSummary(reports: List<ReportEntity>, zoneFilter: String?, dateFrom: String?, dateTo: String?): String {
    val totalArea = reports.sumOf { it.areaCoveredHa }
    val totalLabour = reports.sumOf { it.noOfLabourers }
    val zoneLabel = zoneFilter ?: "All Zones"
    val dateRange = if (!dateFrom.isNullOrBlank() && !dateTo.isNullOrBlank()) "$dateFrom to $dateTo" else "All recorded dates"

    return """
UPENJAnet Sugarcane Operations Summary
======================================
Zone Filter: $zoneLabel
Period: $dateRange
Total Reports: ${reports.size}
Total Area Covered: ${"%.2f".format(totalArea)} Acres
Total Labourers Engaged: $totalLabour

Recent Operations Breakdown:
${reports.take(10).joinToString("\n") { "- ${it.dateOfOperation} | ${it.zone} | ${it.blockNumber}: ${it.operationName} (${it.areaCoveredHa} Acres) [Input: ${it.inputName.ifBlank { "None" }}] - ${it.contractorName}" }}
    """.trimIndent()
  }

  fun parseDateSafely(dateStr: String): Date? {
    val formats = listOf("dd/MM/yyyy", "d/M/yyyy", "yyyy-MM-dd")
    for (fmt in formats) {
      try {
        val sdf = SimpleDateFormat(fmt, Locale.getDefault())
        sdf.isLenient = false
        val parsed = sdf.parse(dateStr.trim())
        if (parsed != null) return parsed
      } catch (_: Exception) {}
    }
    return null
  }

  fun getWeeklyOperationsSummary(
    startDateStr: String,
    endDateStr: String,
    zoneFilter: String? = null,
    allReports: List<ReportEntity>,
  ): List<OperationWeeklySummary> {
    val start = parseDateSafely(startDateStr)
    val end = parseDateSafely(endDateStr)

    val filtered = allReports.filter { report ->
      val inZone = zoneFilter.isNullOrBlank() || zoneFilter == "All Zones" || report.zone.equals(zoneFilter, ignoreCase = true)
      if (!inZone) return@filter false

      if (start != null && end != null) {
        val reportDate = parseDateSafely(report.dateOfOperation)
        if (reportDate != null) {
          val rTime = reportDate.time
          val sTime = start.time
          val eTime = end.time + 86399999L // Include the full end day
          rTime in sTime..eTime
        } else {
          true
        }
      } else {
        true
      }
    }

    val grouped = filtered.groupBy { it.operationName.trim() }

    return grouped.map { (opName, reports) ->
      val totalArea = reports.sumOf { it.areaCoveredHa }
      val totalLabour = reports.sumOf { it.noOfLabourers }
      val contractors = reports.map { it.contractorName.trim() }.filter { it.isNotBlank() }.distinct()
      val zones = reports.map { it.zone.trim() }.distinct()
      val blocks = reports.map { it.blockNumber.trim() }.filter { it.isNotBlank() }.distinct()

      OperationWeeklySummary(
        operationName = opName.ifBlank { "Unspecified Operation" },
        totalAreaAcres = Math.round(totalArea * 100.0) / 100.0,
        reportCount = reports.size,
        totalLabourers = totalLabour,
        contractors = contractors,
        zones = zones,
        blocks = blocks,
      )
    }.sortedByDescending { it.totalAreaAcres }
  }

  fun generateWeeklySummaryReportText(
    startDateStr: String,
    endDateStr: String,
    zoneFilter: String?,
    summaries: List<OperationWeeklySummary>,
  ): String {
    val totalAcres = summaries.sumOf { it.totalAreaAcres }
    val totalLabour = summaries.sumOf { it.totalLabourers }
    val zoneLabel = if (zoneFilter.isNullOrBlank() || zoneFilter == "All Zones") "All Plantation Zones" else zoneFilter

    val sb = StringBuilder()
    sb.append("UPENJAnet - WEEKLY OPERATIONS SUMMARY REPORT\n")
    sb.append("===========================================\n")
    sb.append("Period: From $startDateStr to $endDateStr\n")
    sb.append("Zone: $zoneLabel\n")
    sb.append("Generated: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}\n")
    sb.append("-------------------------------------------\n\n")

    if (summaries.isEmpty()) {
      sb.append("No farm operations recorded for the selected period.\n")
    } else {
      sb.append("Aggregated Operations Summary:\n\n")
      summaries.forEachIndexed { idx, op ->
        val contractorStr = if (op.contractors.isNotEmpty()) " | Contractor(s): ${op.contractors.joinToString(", ")}" else ""
        val blocksStr = if (op.blocks.isNotEmpty()) " | Blocks: ${op.blocks.joinToString(", ")}" else ""
        // Matches user's exact specification: "From 1/09/2026 to 7/09/2026, Area Covered by Stubble shaving is 80Ac"
        val formattedArea = if (op.totalAreaAcres % 1.0 == 0.0) "${op.totalAreaAcres.toInt()}Ac" else "${"%.1f".format(op.totalAreaAcres)}Ac"
        sb.append("${idx + 1}. From $startDateStr to $endDateStr, Area Covered by ${op.operationName} is $formattedArea\n")
        sb.append("   Records: ${op.reportCount}, Labourers Engaged: ${op.totalLabourers}$contractorStr$blocksStr\n\n")
      }
      sb.append("-------------------------------------------\n")
      val formattedGrandTotal = if (totalAcres % 1.0 == 0.0) "${totalAcres.toInt()}Ac" else "${"%.1f".format(totalAcres)}Ac"
      sb.append("Grand Total Area Covered: $formattedGrandTotal across ${summaries.size} operations\n")
      sb.append("Total Labourers Engaged: $totalLabour\n")
    }
    sb.append("===========================================\n")
    return sb.toString()
  }

  private fun generateReportId(zone: String): String {
    val zoneDigits = zone.filter { it.isDigit() }.ifBlank { "00" }
    val year = SimpleDateFormat("yy", Locale.getDefault()).format(Date())
    val randomSuffix = (1000..9999).random()
    return "UPJ-$year-$zoneDigits$randomSuffix"
  }
}
