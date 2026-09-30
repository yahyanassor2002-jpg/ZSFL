package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ReportEntity
import com.example.data.model.ReportStatus
import com.example.data.model.Role
import com.example.data.model.STANDARD_ZONES
import com.example.data.repository.CloudSyncStatus
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.StatusBadge
import com.example.ui.components.UserProfileAvatar
import com.example.ui.theme.CaneGreenPrimary
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
  viewModel: MainViewModel,
  modifier: Modifier = Modifier,
) {
  val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
  val userAvatarUri by viewModel.userAvatarUri.collectAsStateWithLifecycle()
  val reports by viewModel.allReports.collectAsStateWithLifecycle()
  val discussions by viewModel.allDiscussions.collectAsStateWithLifecycle()
  val zoneProgress by viewModel.allZoneProgress.collectAsStateWithLifecycle()
  val equipment by viewModel.allEquipment.collectAsStateWithLifecycle()
  val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
  val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
  val lastSyncMsg by viewModel.lastSyncMessage.collectAsStateWithLifecycle()

  val todayFormatted = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

  // Metrics calculation
  val totalReports = reports.size
  val todayReports = reports.count { it.dateOfOperation == todayFormatted }
  val totalZones = STANDARD_ZONES.size
  val pendingReports = reports.count { it.status == ReportStatus.PENDING_SYNC || !it.synced || it.status == ReportStatus.DRAFT }
  val totalEquipmentUnits = equipment.sumOf { it.requiredQuantity }
  val totalShortages = equipment.sumOf { it.shortageQuantity }
  val avgProgress = if (zoneProgress.isNotEmpty()) zoneProgress.map { it.percentage }.average().toInt() else 0

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // 1. Welcome & Cloud Sync Banner
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
        ),
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "UPENJAnet Sugarcane Operations",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
              Text(
                text = "Real-Time Cloud Synchronized Plantation Management",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
              )
            }

            // Role badge
            val isAdmin = currentUser?.role == Role.ADMIN
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isAdmin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
              Text(
                text = currentUser?.role?.name ?: "USER",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // User info row with avatar
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.clickable { viewModel.navigateTo(Screen.Settings) },
            ) {
              UserProfileAvatar(
                username = currentUser?.username ?: "User",
                avatarUri = userAvatarUri,
                size = 46.dp,
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "Officer: ${currentUser?.username ?: "Logged In"}",
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 15.sp,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                  text = currentUser?.zoneId?.let { "Assigned to $it" } ?: "All Plantation Zones",
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
              }
            }

            IconButton(
              onClick = { viewModel.syncNow() },
              enabled = !isSyncing,
              modifier = Modifier.testTag("dashboard_sync_button"),
            ) {
              if (isSyncing) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
              } else {
                Icon(
                  imageVector = Icons.Default.CloudSync,
                  contentDescription = "Sync Cloud Database",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(28.dp),
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Sync status message
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = when (syncStatus) {
                  CloudSyncStatus.SYNCED -> Icons.Default.CloudDone
                  CloudSyncStatus.SYNCING -> Icons.Default.Sync
                  CloudSyncStatus.PENDING_UPLOAD -> Icons.Default.CloudUpload
                  CloudSyncStatus.OFFLINE -> Icons.Default.CloudOff
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = lastSyncMsg ?: "Cloud database synchronized across all phones.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
          }
        }
      }
    }

    // 2. High-Level Summary Metrics Grid
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Row 1: Total Farm Records & Total Zones
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          MetricCard(
            title = "Total Farm Records",
            value = totalReports.toString(),
            subtitle = "$todayReports added today",
            icon = Icons.Default.Agriculture,
            iconColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f).clickable { viewModel.navigateTo(Screen.ReportsList) },
          )

          MetricCard(
            title = "Total Zones",
            value = "$totalZones Sectors",
            subtitle = "Active plantation zones",
            icon = Icons.Default.Landscape,
            iconColor = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f).clickable { viewModel.navigateTo(Screen.ReportsList) },
          )
        }

        // Row 2: Zone Progress & Pending Reports
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          MetricCard(
            title = "Zone Progress",
            value = "$avgProgress%",
            subtitle = "${zoneProgress.size} active milestones",
            icon = Icons.Default.TrendingUp,
            iconColor = CaneGreenPrimary,
            modifier = Modifier.weight(1f).clickable { viewModel.navigateTo(Screen.ZoneProgress) },
          )

          MetricCard(
            title = "Pending Reports",
            value = "$pendingReports",
            subtitle = if (pendingReports > 0) "Needs sync / review" else "All reports synced",
            icon = Icons.Default.PendingActions,
            iconColor = if (pendingReports > 0) Color(0xFFE65100) else CaneGreenPrimary,
            modifier = Modifier.weight(1f).clickable { viewModel.navigateTo(Screen.ReportsList) },
          )
        }

        // Row 3: Weekly Equipment & Sync Status
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          MetricCard(
            title = "Weekly Equipment",
            value = "$totalEquipmentUnits Units",
            subtitle = if (totalShortages > 0) "$totalShortages units shortage" else "Requirements covered",
            icon = Icons.Default.PrecisionManufacturing,
            iconColor = if (totalShortages > 0) Color(0xFFB71C1C) else CaneGreenPrimary,
            modifier = Modifier.weight(1f).clickable { viewModel.navigateTo(Screen.WeeklyEquipment) },
          )

          MetricCard(
            title = "Sync Status",
            value = when (syncStatus) {
              CloudSyncStatus.SYNCED -> "Cloud Synced"
              CloudSyncStatus.SYNCING -> "Syncing..."
              CloudSyncStatus.PENDING_UPLOAD -> "Pending Upload"
              CloudSyncStatus.OFFLINE -> "Offline Mode"
            },
            subtitle = "Tap to sync database",
            icon = when (syncStatus) {
              CloudSyncStatus.SYNCED -> Icons.Default.CloudDone
              CloudSyncStatus.SYNCING -> Icons.Default.Sync
              CloudSyncStatus.PENDING_UPLOAD -> Icons.Default.CloudUpload
              CloudSyncStatus.OFFLINE -> Icons.Default.CloudOff
            },
            iconColor = when (syncStatus) {
              CloudSyncStatus.SYNCED -> CaneGreenPrimary
              CloudSyncStatus.SYNCING -> MaterialTheme.colorScheme.primary
              CloudSyncStatus.PENDING_UPLOAD -> Color(0xFFE65100)
              CloudSyncStatus.OFFLINE -> Color.Gray
            },
            modifier = Modifier.weight(1f).clickable { viewModel.syncNow() },
          )
        }
      }
    }

    // 3. Quick Action Hub
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text(
            text = "Operational Action Hub",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Button(
              onClick = { viewModel.openNewReport() },
              modifier = Modifier.weight(1f).testTag("quick_new_report_button"),
              shape = RoundedCornerShape(10.dp),
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("New Record", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
              onClick = { viewModel.navigateTo(Screen.WeeklySummary) },
              modifier = Modifier.weight(1f).testTag("dashboard_weekly_summary_button"),
              shape = RoundedCornerShape(10.dp),
            ) {
              Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Weekly Summary", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
              onClick = { viewModel.navigateTo(Screen.ZoneProgress) },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(10.dp),
            ) {
              Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Progress", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // 3b. Weekly Operations Summary Highlight Card
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { viewModel.navigateTo(Screen.WeeklySummary) }
          .testTag("dashboard_weekly_summary_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f).padding(end = 8.dp),
          ) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Default.Assessment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Weekly Operations Summary",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
              )
              Text(
                text = "Aggregate total Area Covered per operation (e.g. Stubble shaving 80Ac)",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                maxLines = 2,
              )
            }
          }

          Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = "View Weekly Report",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
          )
        }
      }
    }

    // 4. Equipment Shortages Alert (if any)
    if (totalShortages > 0) {
      item {
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth().clickable { viewModel.navigateTo(Screen.WeeklyEquipment) },
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "Urgent: $totalShortages Equipment Units Short",
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                  color = MaterialTheme.colorScheme.error,
                )
                Text(
                  text = "Tap to review weekly equipment allocations",
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.error)
          }
        }
      }
    }

    // 5. Recent Farm Operations
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Recent Farm Records",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground,
        )
        TextButton(onClick = { viewModel.navigateTo(Screen.ReportsList) }) {
          Text("View All (${reports.size})")
        }
      }
    }

    if (reports.isEmpty()) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
          Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("No reports filed yet. Click '+ New Record' to submit one.", color = MaterialTheme.colorScheme.outline)
          }
        }
      }
    } else {
      items(reports.take(4), key = { it.id }) { report ->
        RecentReportMiniCard(
          report = report,
          onClick = { viewModel.openReportDetail(report) },
        )
      }
    }

    // 6. Latest Division Discussions
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Division Discussions",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground,
        )
        TextButton(onClick = { viewModel.navigateTo(Screen.Discussions) }) {
          Text("View All (${discussions.size})")
        }
      }
    }

    items(discussions.take(2), key = { it.id }) { disc ->
      Card(
        modifier = Modifier.fillMaxWidth().clickable { viewModel.navigateTo(Screen.Discussions) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(disc.division, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(disc.date, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(disc.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
          Spacer(modifier = Modifier.height(2.dp))
          Text(disc.notes, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        }
      }
    }
  }
}

@Composable
fun MetricCard(
  title: String,
  value: String,
  subtitle: String,
  icon: ImageVector,
  iconColor: Color,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
      }
      Spacer(modifier = Modifier.height(8.dp))
      Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
      Spacer(modifier = Modifier.height(4.dp))
      Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline, maxLines = 1)
    }
  }
}

@Composable
fun RecentReportMiniCard(
  report: ReportEntity,
  onClick: () -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth().clickable { onClick() },
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(report.zone, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.width(6.dp))
          Text("• ${report.dateOfOperation}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
          if (!report.photoUri.isNullOrBlank()) {
            Spacer(modifier = Modifier.width(6.dp))
            Icon(Icons.Default.PhotoCamera, contentDescription = "Photo attached", tint = CaneGreenPrimary, modifier = Modifier.size(14.dp))
          }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(report.operationName, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
        if (report.inputName.isNotBlank()) {
          Text("Input: ${report.inputName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Medium)
        }
      }

      Column(horizontalAlignment = Alignment.End) {
        StatusBadge(status = report.status)
        Spacer(modifier = Modifier.height(4.dp))
        Text("${report.areaCoveredHa} Acres", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
      }
    }
  }
}
