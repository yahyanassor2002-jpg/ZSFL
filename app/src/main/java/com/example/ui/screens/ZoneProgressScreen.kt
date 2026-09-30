package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Role
import com.example.data.model.STANDARD_ZONES
import com.example.data.model.ZoneProgressEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.CaneGreenPrimary
import java.text.SimpleDateFormat
import java.util.*

private val PROGRESS_STATUSES = listOf("All", "On Track", "In Progress", "Delayed", "Completed")

private val SAMPLE_ACTIVITIES = listOf(
  "Land Preparation (Ploughing & Harrowing)",
  "Furrowing & Ridge Formation",
  "Seed Cane Planting (R570 / N14)",
  "Pre-Emergence Herbicide Spraying",
  "Basal Fertilizer Application",
  "First Ratoon Inter-row Weeding",
  "Top Dressing with Urea",
  "Cane Harvesting & Infield Loading",
  "Drainage Clearance & Desilting",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZoneProgressScreen(
  viewModel: MainViewModel,
  modifier: Modifier = Modifier,
) {
  val progressList by viewModel.allZoneProgress.collectAsStateWithLifecycle()
  val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
  val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()

  var selectedZone by remember { mutableStateOf<String?>(null) }
  var selectedStatus by remember { mutableStateOf<String>("All") }
  var searchQuery by remember { mutableStateOf("") }
  var showDialog by remember { mutableStateOf(false) }
  var editingProgress by remember { mutableStateOf<ZoneProgressEntity?>(null) }
  var progressToDelete by remember { mutableStateOf<ZoneProgressEntity?>(null) }

  val filteredList = remember(progressList, selectedZone, selectedStatus, searchQuery) {
    progressList.filter { p ->
      val matchesZone = selectedZone == null || p.zone.equals(selectedZone, ignoreCase = true)
      val matchesStatus = selectedStatus == "All" || p.progressStatus.equals(selectedStatus, ignoreCase = true)
      val matchesQuery = searchQuery.isBlank() ||
        p.activity.contains(searchQuery, ignoreCase = true) ||
        p.remarks.contains(searchQuery, ignoreCase = true) ||
        p.zone.contains(searchQuery, ignoreCase = true) ||
        p.submittedBy.contains(searchQuery, ignoreCase = true)
      matchesZone && matchesStatus && matchesQuery
    }
  }

  // Calculate Metrics
  val avgPercentage = if (progressList.isNotEmpty()) progressList.map { it.percentage }.average().toInt() else 0
  val onTrackCount = progressList.count { it.progressStatus.equals("On Track", ignoreCase = true) }
  val delayedCount = progressList.count { it.progressStatus.equals("Delayed", ignoreCase = true) }
  val completedCount = progressList.count { it.progressStatus.equals("Completed", ignoreCase = true) }

  Scaffold(
    containerColor = Color.Transparent,
    modifier = modifier.fillMaxSize(),
    floatingActionButton = {
      FloatingActionButton(
        onClick = {
          editingProgress = null
          showDialog = true
        },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.testTag("add_progress_fab"),
      ) {
        Icon(imageVector = Icons.Default.Addchart, contentDescription = "Record Progress")
      }
    },
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp),
    ) {
      Spacer(modifier = Modifier.height(12.dp))

      // Header Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = "Zone Progress Monitoring",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = "Field operations completion & pace tracking",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        IconButton(
          onClick = { viewModel.syncNow() },
          enabled = !isSyncing,
          modifier = Modifier.testTag("sync_progress_button"),
        ) {
          Icon(
            imageVector = Icons.Default.Sync,
            contentDescription = "Sync Progress",
            tint = MaterialTheme.colorScheme.primary,
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Overview KPI Card
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Overall Plantation Pace",
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = "$avgPercentage% Complete",
              fontWeight = FontWeight.ExtraBold,
              fontSize = 16.sp,
              color = MaterialTheme.colorScheme.primary,
            )
          }

          Spacer(modifier = Modifier.height(8.dp))
          LinearProgressIndicator(
            progress = { avgPercentage / 100f },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
          )

          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
          ) {
            MetricPill("On Track", onTrackCount, CaneGreenPrimary)
            MetricPill("Delayed", delayedCount, Color(0xFFB71C1C))
            MetricPill("Completed", completedCount, CaneGreenPrimary)
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Search Bar
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search by activity, remarks, zone...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { searchQuery = "" }) {
              Icon(Icons.Default.Clear, contentDescription = "Clear")
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().testTag("progress_search_input"),
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Zone Filters
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        FilterChip(
          selected = selectedZone == null,
          onClick = { selectedZone = null },
          label = { Text("All Zones") },
        )
        STANDARD_ZONES.forEach { z ->
          FilterChip(
            selected = selectedZone == z,
            onClick = { selectedZone = if (selectedZone == z) null else z },
            label = { Text(z) },
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      // Status Filters
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        PROGRESS_STATUSES.forEach { st ->
          FilterChip(
            selected = selectedStatus == st,
            onClick = { selectedStatus = st },
            label = { Text(st) },
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "${filteredList.size} progress milestones recorded",
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.outline,
      )

      Spacer(modifier = Modifier.height(6.dp))

      if (filteredList.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          contentAlignment = Alignment.Center,
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = Icons.Default.TrendingUp,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.outline,
              modifier = Modifier.size(52.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "No progress records found for this filter.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.outline,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
              onClick = {
                editingProgress = null
                showDialog = true
              },
            ) {
              Icon(Icons.Default.Add, contentDescription = null)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Record Zone Progress")
            }
          }
        }
      } else {
        LazyColumn(
          contentPadding = PaddingValues(bottom = 88.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxSize(),
        ) {
          items(filteredList, key = { it.id }) { prog ->
            ProgressCard(
              progress = prog,
              currentUser = currentUser,
              onEdit = {
                editingProgress = prog
                showDialog = true
              },
              onDelete = {
                progressToDelete = prog
              },
            )
          }
        }
      }
    }
  }

  // Create / Edit Dialog
  if (showDialog) {
    ProgressFormDialog(
      existing = editingProgress,
      onDismiss = { showDialog = false },
      onSave = { zone, date, activity, status, percent, remarks ->
        viewModel.saveZoneProgress(
          id = editingProgress?.id,
          zone = zone,
          date = date,
          activity = activity,
          progressStatus = status,
          percentage = percent,
          remarks = remarks,
        ) { success ->
          if (success) showDialog = false
        }
      },
    )
  }

  // Delete Dialog (Admin Only)
  if (progressToDelete != null) {
    val prog = progressToDelete!!
    AlertDialog(
      onDismissRequest = { progressToDelete = null },
      title = { Text("Delete Progress Record?", fontWeight = FontWeight.Bold) },
      text = { Text("Are you sure you want to delete the progress milestone for ${prog.zone} (${prog.activity})? Only Administrators can perform this action.") },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteZoneProgress(prog.id) {
              progressToDelete = null
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) {
          Text("DELETE")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { progressToDelete = null }) {
          Text("CANCEL")
        }
      },
    )
  }
}

@Composable
private fun MetricPill(label: String, count: Int, color: Color) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Surface(
      shape = RoundedCornerShape(4.dp),
      color = color.copy(alpha = 0.15f),
    ) {
      Text(
        text = "$count",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
      )
    }
    Spacer(modifier = Modifier.width(4.dp))
    Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@Composable
fun ProgressCard(
  progress: ZoneProgressEntity,
  currentUser: com.example.data.model.UserEntity?,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
) {
  val isAdmin = currentUser?.role == Role.ADMIN

  val statusBgColor = when (progress.progressStatus) {
    "Completed" -> Color(0xFFE2EFE5)
    "On Track" -> Color(0xFFE8F0E9)
    "Delayed" -> Color(0xFFFFEBEE)
    else -> Color(0xFFFFF3E0)
  }

  val statusTextColor = when (progress.progressStatus) {
    "Completed" -> CaneGreenPrimary
    "On Track" -> CaneGreenPrimary
    "Delayed" -> Color(0xFFC62828)
    else -> Color(0xFFE65100)
  }

  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = Modifier.fillMaxWidth().testTag("progress_card_${progress.id}"),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = progress.zone,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = progress.date,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.outline,
          )
        }

        Surface(
          shape = RoundedCornerShape(6.dp),
          color = statusBgColor,
        ) {
          Text(
            text = progress.progressStatus,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = statusTextColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = progress.activity,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Progress Bar & Percentage
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        LinearProgressIndicator(
          progress = { progress.percentage / 100f },
          modifier = Modifier.weight(1f).height(10.dp),
          color = when {
            progress.percentage >= 100 -> CaneGreenPrimary
            progress.progressStatus == "Delayed" -> Color(0xFFC62828)
            else -> MaterialTheme.colorScheme.primary
          },
          trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "${progress.percentage}%",
          fontSize = 13.sp,
          fontWeight = FontWeight.ExtraBold,
          color = MaterialTheme.colorScheme.primary,
        )
      }

      if (progress.remarks.isNotBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "Note: ${progress.remarks}",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Spacer(modifier = Modifier.height(10.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
      Spacer(modifier = Modifier.height(6.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Updated by: ${progress.submittedBy}",
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.outline,
        )

        Row {
          IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
            Icon(
              imageVector = Icons.Default.Edit,
              contentDescription = "Edit Progress",
              modifier = Modifier.size(17.dp),
              tint = MaterialTheme.colorScheme.primary,
            )
          }
          if (isAdmin) {
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Progress",
                modifier = Modifier.size(17.dp),
                tint = MaterialTheme.colorScheme.error,
              )
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressFormDialog(
  existing: ZoneProgressEntity?,
  onDismiss: () -> Unit,
  onSave: (zone: String, date: String, activity: String, status: String, percent: Int, remarks: String) -> Unit,
) {
  val context = LocalContext.current
  val todayFormatted = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

  var zone by remember { mutableStateOf(existing?.zone ?: STANDARD_ZONES.first()) }
  var date by remember { mutableStateOf(existing?.date ?: todayFormatted) }
  var activity by remember { mutableStateOf(existing?.activity ?: "") }
  var status by remember { mutableStateOf(existing?.progressStatus ?: "In Progress") }
  var percentage by remember { mutableStateOf(existing?.percentage?.toFloat() ?: 25f) }
  var remarks by remember { mutableStateOf(existing?.remarks ?: "") }

  var zoneExpanded by remember { mutableStateOf(false) }
  var statusExpanded by remember { mutableStateOf(false) }

  val calendar = Calendar.getInstance()
  val datePicker = remember {
    DatePickerDialog(
      context,
      { _, year, month, dayOfMonth ->
        val cal = Calendar.getInstance().apply {
          set(Calendar.YEAR, year)
          set(Calendar.MONTH, month)
          set(Calendar.DAY_OF_MONTH, dayOfMonth)
        }
        date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time)
      },
      calendar.get(Calendar.YEAR),
      calendar.get(Calendar.MONTH),
      calendar.get(Calendar.DAY_OF_MONTH),
    )
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = if (existing != null) "Update Zone Progress" else "Record Zone Progress",
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        // Zone Selector
        ExposedDropdownMenuBox(
          expanded = zoneExpanded,
          onExpandedChange = { zoneExpanded = !zoneExpanded },
        ) {
          OutlinedTextField(
            value = zone,
            onValueChange = {},
            readOnly = true,
            label = { Text("Zone") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = zoneExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
          )
          ExposedDropdownMenu(
            expanded = zoneExpanded,
            onDismissRequest = { zoneExpanded = false },
          ) {
            STANDARD_ZONES.forEach { z ->
              DropdownMenuItem(
                text = { Text(z) },
                onClick = {
                  zone = z
                  zoneExpanded = false
                },
              )
            }
          }
        }

        // Date Picker
        OutlinedTextField(
          value = date,
          onValueChange = { date = it },
          readOnly = true,
          label = { Text("Date") },
          trailingIcon = {
            IconButton(onClick = { datePicker.show() }) {
              Icon(Icons.Default.CalendarToday, contentDescription = "Select Date")
            }
          },
          modifier = Modifier.fillMaxWidth(),
        )

        // Activity Input
        OutlinedTextField(
          value = activity,
          onValueChange = { activity = it },
          label = { Text("Activity / Operation Name") },
          placeholder = { Text("e.g. Basal Fertilizer Application") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("progress_activity_input"),
        )

        // Activity suggestions
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          SAMPLE_ACTIVITIES.take(4).forEach { act ->
            SuggestionChip(
              onClick = { activity = act },
              label = { Text(act.take(20) + "...", fontSize = 11.sp) },
            )
          }
        }

        // Progress Status Selector
        ExposedDropdownMenuBox(
          expanded = statusExpanded,
          onExpandedChange = { statusExpanded = !statusExpanded },
        ) {
          OutlinedTextField(
            value = status,
            onValueChange = {},
            readOnly = true,
            label = { Text("Pace Status") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
          )
          ExposedDropdownMenu(
            expanded = statusExpanded,
            onDismissRequest = { statusExpanded = false },
          ) {
            listOf("On Track", "In Progress", "Delayed", "Completed").forEach { st ->
              DropdownMenuItem(
                text = { Text(st) },
                onClick = {
                  status = st
                  statusExpanded = false
                  if (st == "Completed") percentage = 100f
                },
              )
            }
          }
        }

        // Percentage Slider
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text("Completion: ${percentage.toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
          Slider(
            value = percentage,
            onValueChange = { percentage = it },
            valueRange = 0f..100f,
            steps = 19,
            modifier = Modifier.fillMaxWidth().testTag("progress_slider"),
          )
        }

        OutlinedTextField(
          value = remarks,
          onValueChange = { remarks = it },
          label = { Text("Remarks / Bottlenecks") },
          placeholder = { Text("e.g. Rain slowed down tractors, 20 Acres remaining") },
          minLines = 2,
          maxLines = 4,
          modifier = Modifier.fillMaxWidth().testTag("progress_remarks_input"),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (activity.isNotBlank()) {
            onSave(zone, date, activity, status, percentage.toInt(), remarks)
          }
        },
        enabled = activity.isNotBlank(),
        modifier = Modifier.testTag("save_progress_button"),
      ) {
        Text("SAVE & SYNC")
      }
    },
    dismissButton = {
      OutlinedButton(onClick = onDismiss) {
        Text("CANCEL")
      }
    },
  )
}
