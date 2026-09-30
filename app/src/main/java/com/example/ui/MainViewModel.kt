package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.CloudSyncStatus
import com.example.data.repository.SessionManager
import com.example.data.repository.UpenjaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class Screen {
  object Login : Screen()
  object Dashboard : Screen()
  object ReportsList : Screen()
  object ReportDetail : Screen()
  object NewOrEditReport : Screen()
  object Discussions : Screen()
  object ZoneProgress : Screen()
  object WeeklyEquipment : Screen()
  object WeeklySummary : Screen()
  object Zones : Screen()
  object SyncAndExport : Screen()
  object Settings : Screen()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
  private val database = AppDatabase.getDatabase(application, viewModelScope)
  private val sessionManager = SessionManager(application)
  val repository = UpenjaRepository(application, database, sessionManager, viewModelScope)

  val currentUser: StateFlow<UserEntity?> = repository.currentUser

  val allReports: StateFlow<List<ReportEntity>> = repository.allReports
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allDiscussions: StateFlow<List<DiscussionEntity>> = repository.allDiscussions
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allZoneProgress: StateFlow<List<ZoneProgressEntity>> = repository.allZoneProgress
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allEquipment: StateFlow<List<WeeklyEquipmentEntity>> = repository.allEquipment
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allZones: StateFlow<List<ZoneEntity>> = repository.allZones
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allUsers: StateFlow<List<UserEntity>> = repository.allUsers
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val isSyncing: StateFlow<Boolean> = repository.isSyncing
  val syncStatus: StateFlow<CloudSyncStatus> = repository.syncStatus
  val lastSyncMessage: StateFlow<String?> = repository.lastSyncMessage
  val lastSyncTimestamp: StateFlow<Long> = repository.lastSyncTimestamp
  val pendingSyncCount: StateFlow<Int> = repository.pendingSyncCount

  private val _currentScreen = MutableStateFlow<Screen>(Screen.Login)
  val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

  // Selected report for Detail and Edit
  private val _activeReport = MutableStateFlow<ReportEntity?>(null)
  val activeReport: StateFlow<ReportEntity?> = _activeReport.asStateFlow()

  // Search and Filter states for Farm Records
  val searchQuery = MutableStateFlow("")
  val selectedZoneFilter = MutableStateFlow<String?>(null)
  val selectedStatusFilter = MutableStateFlow<ReportStatus?>(null)
  val selectedOperationFilter = MutableStateFlow<String?>(null)
  val selectedInputFilter = MutableStateFlow<String?>(null)
  val selectedUserFilter = MutableStateFlow<String?>(null)

  private val filterCriteria = combine(
    searchQuery,
    selectedZoneFilter,
    selectedStatusFilter,
    selectedOperationFilter,
  ) { query, zone, status, op ->
    Tuple4(query, zone, status, op)
  }.combine(selectedInputFilter) { tuple, input ->
    Tuple5(tuple.a, tuple.b, tuple.c, tuple.d, input)
  }.combine(selectedUserFilter) { tuple, user ->
    Tuple6(tuple.a, tuple.b, tuple.c, tuple.d, tuple.e, user)
  }

  // Filtered reports stream
  val filteredReports: StateFlow<List<ReportEntity>> = combine(allReports, filterCriteria) { reports, criteria ->
    val query = criteria.a
    val zone = criteria.b
    val status = criteria.c
    val op = criteria.d
    val inputFilt = criteria.e
    val userFilt = criteria.f

    reports.filter { r ->
      val matchesQuery = query.isBlank() ||
        r.blockNumber.contains(query, ignoreCase = true) ||
        r.contractorName.contains(query, ignoreCase = true) ||
        r.operationName.contains(query, ignoreCase = true) ||
        r.inputName.contains(query, ignoreCase = true) ||
        r.ratoonDate.contains(query, ignoreCase = true) ||
        r.dateOfOperation.contains(query, ignoreCase = true) ||
        r.createdBy.contains(query, ignoreCase = true) ||
        r.lastUpdatedBy.contains(query, ignoreCase = true) ||
        r.remark.contains(query, ignoreCase = true) ||
        r.id.contains(query, ignoreCase = true) ||
        r.zone.contains(query, ignoreCase = true)

      val matchesZone = zone.isNullOrBlank() || r.zone.equals(zone, ignoreCase = true)
      val matchesStatus = status == null || r.status == status
      val matchesOp = op.isNullOrBlank() || r.operationName.equals(op, ignoreCase = true)
      val matchesInput = inputFilt.isNullOrBlank() || r.inputName.contains(inputFilt, ignoreCase = true)
      val matchesUser = userFilt.isNullOrBlank() || r.createdBy.equals(userFilt, ignoreCase = true)

      matchesQuery && matchesZone && matchesStatus && matchesOp && matchesInput && matchesUser
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  fun clearAllFilters() {
    searchQuery.value = ""
    selectedZoneFilter.value = null
    selectedStatusFilter.value = null
    selectedOperationFilter.value = null
    selectedInputFilter.value = null
    selectedUserFilter.value = null
  }

  // Weekly Operations Summary State (Aggregates Area Covered per specific Operation)
  val weeklySummaryStartDate = MutableStateFlow("01/09/2026")
  val weeklySummaryEndDate = MutableStateFlow("07/09/2026")
  val weeklySummaryZoneFilter = MutableStateFlow<String?>("All Zones")

  val weeklyOperationsSummary: StateFlow<List<OperationWeeklySummary>> = combine(
    allReports,
    weeklySummaryStartDate,
    weeklySummaryEndDate,
    weeklySummaryZoneFilter,
  ) { reports, start, end, zone ->
    repository.getWeeklyOperationsSummary(start, end, zone, reports)
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  fun setWeeklySummaryDateRange(start: String, end: String) {
    weeklySummaryStartDate.value = start
    weeklySummaryEndDate.value = end
  }

  fun setWeeklySummaryZone(zone: String?) {
    weeklySummaryZoneFilter.value = zone
  }

  fun getWeeklySummaryShareText(): String {
    return repository.generateWeeklySummaryReportText(
      weeklySummaryStartDate.value,
      weeklySummaryEndDate.value,
      weeklySummaryZoneFilter.value,
      weeklyOperationsSummary.value,
    )
  }

  // UI Feedback / Alerts
  private val _toastMessage = MutableStateFlow<String?>(null)
  val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

  private val _userAvatarUri = MutableStateFlow<String?>(null)
  val userAvatarUri: StateFlow<String?> = _userAvatarUri.asStateFlow()

  init {
    viewModelScope.launch {
      currentUser.collect { user ->
        if (user != null) {
          _userAvatarUri.value = sessionManager.getUserAvatar(user.username)
          if (_currentScreen.value is Screen.Login) {
            _currentScreen.value = Screen.Dashboard
          }
        } else {
          _userAvatarUri.value = null
          _currentScreen.value = Screen.Login
        }
      }
    }
  }

  fun updateUserAvatar(uriString: String) {
    val username = currentUser.value?.username ?: return
    sessionManager.saveUserAvatar(username, uriString)
    _userAvatarUri.value = uriString
    showToast("Profile picture updated successfully")
  }

  fun navigateTo(screen: Screen) {
    _currentScreen.value = screen
  }

  fun openReportDetail(report: ReportEntity) {
    _activeReport.value = report
    _currentScreen.value = Screen.ReportDetail
  }

  fun openNewReport(prefilledZone: String? = null) {
    _activeReport.value = null
    prefilledZone?.let { selectedZoneFilter.value = it }
    _currentScreen.value = Screen.NewOrEditReport
  }

  fun openEditReport(report: ReportEntity) {
    _activeReport.value = report
    _currentScreen.value = Screen.NewOrEditReport
  }

  fun clearToast() {
    _toastMessage.value = null
  }

  fun showToast(msg: String) {
    _toastMessage.value = msg
  }

  fun login(username: String, password: String, onSuccess: () -> Unit = {}) {
    viewModelScope.launch {
      val result = repository.login(username, password)
      result.onSuccess {
        _toastMessage.value = "Welcome, ${it.username} (${it.role.name})"
        _currentScreen.value = Screen.Dashboard
        onSuccess()
      }.onFailure { err ->
        _toastMessage.value = err.message ?: "Login failed"
      }
    }
  }

  fun logout() {
    repository.logout()
    _activeReport.value = null
    _currentScreen.value = Screen.Login
    _toastMessage.value = "Logged out successfully"
  }

  fun switchUser(user: UserEntity) {
    repository.switchUser(user)
    _toastMessage.value = "Switched to ${user.username} (${user.role.name})"
    _currentScreen.value = Screen.Dashboard
  }

  // --- REPORT ACTIONS ---
  fun saveReport(
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
    onComplete: (Boolean) -> Unit,
  ) {
    viewModelScope.launch {
      val result = repository.saveReport(
        id = id,
        zone = zone,
        dateOfOperation = dateOfOperation,
        operationName = operationName,
        blockNumber = blockNumber,
        contractorName = contractorName,
        noOfLabourers = noOfLabourers,
        areaCoveredHa = areaCoveredHa,
        balanceToBeDoneHa = balanceToBeDoneHa,
        inputName = inputName,
        ratoonDate = ratoonDate,
        remark = remark,
        photoUri = photoUri,
        isDraft = isDraft,
      )
      result.onSuccess { report ->
        _activeReport.value = report
        if (isDraft) {
          _toastMessage.value = "Draft saved locally."
        } else {
          _toastMessage.value = "Report saved & synced to shared cloud database."
        }
        _currentScreen.value = Screen.ReportsList
        onComplete(true)
      }.onFailure { err ->
        _toastMessage.value = err.message ?: "Failed to save report"
        onComplete(false)
      }
    }
  }

  fun deleteReport(reportId: String, onComplete: (Boolean) -> Unit) {
    val user = currentUser.value
    if (user == null) {
      _toastMessage.value = "User not logged in."
      onComplete(false)
      return
    }

    if (user.role != Role.ADMIN) {
      _toastMessage.value = "Security notice: Only Administrators have permission to delete reports."
      onComplete(false)
      return
    }

    viewModelScope.launch {
      val result = repository.deleteReport(user, reportId)
      result.onSuccess {
        _toastMessage.value = "Report deleted successfully from local and cloud."
        _activeReport.value = null
        _currentScreen.value = Screen.ReportsList
        onComplete(true)
      }.onFailure { err ->
        _toastMessage.value = err.message ?: "Failed to delete report"
        onComplete(false)
      }
    }
  }

  // --- DISCUSSIONS ACTIONS ---
  fun saveDiscussion(
    id: String?,
    title: String,
    notes: String,
    division: String,
    date: String,
    onComplete: (Boolean) -> Unit,
  ) {
    viewModelScope.launch {
      val result = repository.saveDiscussion(id, title, notes, division, date)
      result.onSuccess {
        _toastMessage.value = "Discussion saved & shared to cloud."
        onComplete(true)
      }.onFailure { err ->
        _toastMessage.value = err.message ?: "Failed to save discussion"
        onComplete(false)
      }
    }
  }

  fun deleteDiscussion(discussionId: String, onComplete: (Boolean) -> Unit) {
    val user = currentUser.value ?: return
    viewModelScope.launch {
      val result = repository.deleteDiscussion(user, discussionId)
      result.onSuccess {
        _toastMessage.value = "Discussion deleted."
        onComplete(true)
      }.onFailure { err ->
        _toastMessage.value = err.message ?: "Failed to delete discussion"
        onComplete(false)
      }
    }
  }

  // --- ZONE PROGRESS ACTIONS ---
  fun saveZoneProgress(
    id: String?,
    zone: String,
    date: String,
    activity: String,
    progressStatus: String,
    percentage: Int,
    remarks: String,
    onComplete: (Boolean) -> Unit,
  ) {
    viewModelScope.launch {
      val result = repository.saveZoneProgress(id, zone, date, activity, progressStatus, percentage, remarks)
      result.onSuccess {
        _toastMessage.value = "Zone progress recorded & synced."
        onComplete(true)
      }.onFailure { err ->
        _toastMessage.value = err.message ?: "Failed to save progress"
        onComplete(false)
      }
    }
  }

  fun deleteZoneProgress(progressId: String, onComplete: (Boolean) -> Unit) {
    val user = currentUser.value ?: return
    if (user.role != Role.ADMIN) {
      _toastMessage.value = "Only Administrators can delete progress records."
      onComplete(false)
      return
    }
    viewModelScope.launch {
      val result = repository.deleteZoneProgress(user, progressId)
      result.onSuccess {
        _toastMessage.value = "Progress record deleted."
        onComplete(true)
      }.onFailure { err ->
        _toastMessage.value = err.message ?: "Failed to delete progress"
        onComplete(false)
      }
    }
  }

  // --- WEEKLY EQUIPMENT ACTIONS ---
  fun saveWeeklyEquipment(
    id: String?,
    weekDate: String,
    zoneOrDivision: String,
    equipmentName: String,
    requiredQuantity: Int,
    availableQuantity: Int,
    remarks: String,
    onComplete: (Boolean) -> Unit,
  ) {
    viewModelScope.launch {
      val result = repository.saveWeeklyEquipment(id, weekDate, zoneOrDivision, equipmentName, requiredQuantity, availableQuantity, remarks)
      result.onSuccess {
        _toastMessage.value = "Equipment requirement saved & synced."
        onComplete(true)
      }.onFailure { err ->
        _toastMessage.value = err.message ?: "Failed to save equipment requirement"
        onComplete(false)
      }
    }
  }

  fun deleteWeeklyEquipment(equipmentId: String, onComplete: (Boolean) -> Unit) {
    val user = currentUser.value ?: return
    if (user.role != Role.ADMIN) {
      _toastMessage.value = "Only Administrators can delete equipment requirements."
      onComplete(false)
      return
    }
    viewModelScope.launch {
      val result = repository.deleteWeeklyEquipment(user, equipmentId)
      result.onSuccess {
        _toastMessage.value = "Equipment requirement deleted."
        onComplete(true)
      }.onFailure { err ->
        _toastMessage.value = err.message ?: "Failed to delete equipment"
        onComplete(false)
      }
    }
  }

  fun syncNow() {
    viewModelScope.launch {
      repository.syncNow()
    }
  }

  fun createHeadmanUser(username: String, password: String, zone: String, onComplete: (Boolean) -> Unit) {
    val user = currentUser.value ?: return
    viewModelScope.launch {
      val result = repository.createHeadmanUser(user, username, password, zone)
      result.onSuccess {
        _toastMessage.value = "Headman user '${it.username}' created for ${it.zoneId}"
        onComplete(true)
      }.onFailure { err ->
        _toastMessage.value = err.message ?: "Failed to create user"
        onComplete(false)
      }
    }
  }

  fun shareExportCsv(context: Context, reports: List<ReportEntity>) {
    val csv = repository.exportReportsToCsv(reports)
    val sendIntent: Intent = Intent().apply {
      action = Intent.ACTION_SEND
      putExtra(Intent.EXTRA_TEXT, csv)
      putExtra(Intent.EXTRA_SUBJECT, "UPENJAnet Reports Export (${reports.size} records)")
      type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Export UPENJAnet CSV")
    shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(shareIntent)
  }

  fun shareExportSummary(context: Context, reports: List<ReportEntity>, zoneFilter: String?, dateFrom: String?, dateTo: String?) {
    val summary = repository.exportReportsToFormattedSummary(reports, zoneFilter, dateFrom, dateTo)
    val sendIntent: Intent = Intent().apply {
      action = Intent.ACTION_SEND
      putExtra(Intent.EXTRA_TEXT, summary)
      putExtra(Intent.EXTRA_SUBJECT, "UPENJAnet Sugarcane Operations Summary")
      type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Share Operations Summary")
    shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(shareIntent)
  }
}

private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
private data class Tuple5<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)
private data class Tuple6<A, B, C, D, E, F>(val a: A, val b: B, val c: C, val d: D, val e: E, val f: F)
