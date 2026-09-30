package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.COMMON_OPERATIONS
import com.example.data.model.ReportEntity
import com.example.data.model.ReportStatus
import com.example.data.model.STANDARD_ZONES
import com.example.data.repository.CloudSyncStatus
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.StatusBadge
import com.example.ui.theme.CaneBlueSecondary
import com.example.ui.theme.CaneGreenPrimary
import com.example.util.PdfExportHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsListScreen(
  viewModel: MainViewModel,
  modifier: Modifier = Modifier,
) {
  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
  val selectedZone by viewModel.selectedZoneFilter.collectAsStateWithLifecycle()
  val selectedStatus by viewModel.selectedStatusFilter.collectAsStateWithLifecycle()
  val selectedOp by viewModel.selectedOperationFilter.collectAsStateWithLifecycle()
  val selectedInput by viewModel.selectedInputFilter.collectAsStateWithLifecycle()
  val selectedUser by viewModel.selectedUserFilter.collectAsStateWithLifecycle()
  val reports by viewModel.filteredReports.collectAsStateWithLifecycle()
  val allReportsList by viewModel.allReports.collectAsStateWithLifecycle()
  val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
  val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()

  var showMoreFilters by remember { mutableStateOf(false) }

  val hasActiveFilters = searchQuery.isNotEmpty() ||
    selectedZone != null ||
    selectedStatus != null ||
    selectedOp != null ||
    selectedInput != null ||
    selectedUser != null

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = Color.Transparent,
    floatingActionButton = {
      FloatingActionButton(
        onClick = { viewModel.openNewReport() },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.testTag("new_report_fab"),
      ) {
        Icon(imageVector = Icons.Default.Add, contentDescription = "Create Report")
      }
    },
  ) { innerPadding ->
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
    ) {
      val isWideScreen = maxWidth > 650.dp

      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 14.dp),
      ) {
        Spacer(modifier = Modifier.height(2.dp))

        // 1. ULTRA-COMPACT CLOUD SYNC STATUS BAR (Red area #1)
        Surface(
          shape = RoundedCornerShape(4.dp),
          color = when (syncStatus) {
            CloudSyncStatus.SYNCED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            CloudSyncStatus.SYNCING -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
            CloudSyncStatus.PENDING_UPLOAD -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
            CloudSyncStatus.OFFLINE -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(20.dp),
        ) {
          Row(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp),
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
                modifier = Modifier.size(11.dp),
              )
              Spacer(modifier = Modifier.width(5.dp))
              Text(
                text = when (syncStatus) {
                  CloudSyncStatus.SYNCED -> "Cloud Synced (Shared across phones)"
                  CloudSyncStatus.SYNCING -> "Syncing database..."
                  CloudSyncStatus.PENDING_UPLOAD -> "Pending upload (Saved locally)"
                  CloudSyncStatus.OFFLINE -> "Offline: Saved in Room"
                },
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }

            Text(
              text = if (isSyncing) "Syncing..." else "Sync Now",
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier
                .clickable(enabled = !isSyncing) { viewModel.syncNow() }
                .padding(horizontal = 2.dp),
            )
          }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // 2. ULTRA-COMPACT WEEKLY OPERATIONS SUMMARY CALLOUT BAR (Red area #2)
        Surface(
          shape = RoundedCornerShape(4.dp),
          color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
          modifier = Modifier
            .fillMaxWidth()
            .height(21.dp)
            .clickable { viewModel.navigateTo(Screen.WeeklySummary) }
            .testTag("weekly_summary_card_trigger"),
        ) {
          Row(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp),
            ) {
              Icon(
                imageVector = Icons.Default.Assessment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(11.dp),
              )
              Spacer(modifier = Modifier.width(5.dp))
              Text(
                text = "Weekly Operations Summary: Area Covered per Operation",
                fontWeight = FontWeight.Bold,
                fontSize = 9.5.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }

            Text(
              text = "View >",
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // 3. ULTRA-COMPACT SEARCH BOX (Red area #3)
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(horizontal = 8.dp),
          contentAlignment = Alignment.CenterStart,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = "Search",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(13.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(modifier = Modifier.weight(1f)) {
              if (searchQuery.isEmpty()) {
                Text(
                  text = "Search block, contractor, input, operation, ID...",
                  fontSize = 10.5.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              BasicTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                  color = MaterialTheme.colorScheme.onSurface,
                  fontSize = 11.sp,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("reports_search_input"),
              )
            }
            if (searchQuery.isNotEmpty()) {
              IconButton(
                onClick = { viewModel.searchQuery.value = "" },
                modifier = Modifier.size(18.dp),
              ) {
                Icon(
                  imageVector = Icons.Default.Clear,
                  contentDescription = "Clear search",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(11.dp),
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // 4. ULTRA-COMPACT SINGLE-ROW FILTER STRIP
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Zone:",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.outline,
          )

          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (selectedZone == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier
              .height(20.dp)
              .clickable { viewModel.selectedZoneFilter.value = null },
          ) {
            Text(
              text = "All",
              fontSize = 9.5.sp,
              fontWeight = FontWeight.SemiBold,
              color = if (selectedZone == null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
          }

          STANDARD_ZONES.forEach { zone ->
            val isSel = selectedZone == zone
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
              modifier = Modifier
                .height(20.dp)
                .clickable { viewModel.selectedZoneFilter.value = if (isSel) null else zone },
            ) {
              Text(
                text = zone.replace("ZONE NO ", "Z"),
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }

          Spacer(modifier = Modifier.width(3.dp))
          Text(
            text = "Status:",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.outline,
          )

          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (selectedStatus == null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier
              .height(20.dp)
              .clickable { viewModel.selectedStatusFilter.value = null },
          ) {
            Text(
              text = "All",
              fontSize = 9.5.sp,
              fontWeight = FontWeight.SemiBold,
              color = if (selectedStatus == null) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
          }

          ReportStatus.values().forEach { status ->
            val isSel = selectedStatus == status
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (isSel) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
              modifier = Modifier
                .height(20.dp)
                .clickable { viewModel.selectedStatusFilter.value = if (isSel) null else status },
            ) {
              Text(
                text = status.name,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSel) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }

          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (showMoreFilters) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier
              .height(20.dp)
              .clickable { showMoreFilters = !showMoreFilters },
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
              Icon(
                imageVector = if (showMoreFilters) Icons.Default.ExpandLess else Icons.Default.Tune,
                contentDescription = null,
                tint = if (showMoreFilters) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(11.dp),
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = if (showMoreFilters) "Less" else "More",
                fontSize = 9.5.sp,
                color = if (showMoreFilters) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        // Expandable More Filters (Input Name)
        if (showMoreFilters) {
          Spacer(modifier = Modifier.height(2.dp))
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 3.dp),
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text("Input:", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
              listOf("All", "Urea", "CAN", "Glyphosate", "R570", "NPK", "Trash Blanketing").forEach { inp ->
                val isSel = (inp == "All" && selectedInput == null) || selectedInput == inp
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                  modifier = Modifier
                    .height(18.dp)
                    .clickable { viewModel.selectedInputFilter.value = if (inp == "All" || isSel) null else inp },
                ) {
                  Text(
                    text = inp,
                    fontSize = 9.sp,
                    color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // 5. ACTIVE COUNT & CLEAR (Immediate visibility of records)
        Row(
          modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Showing ${reports.size} of ${allReportsList.size} records",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.outline,
          )

          if (hasActiveFilters) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.clickable { viewModel.clearAllFilters() },
            ) {
              Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(11.dp))
              Spacer(modifier = Modifier.width(2.dp))
              Text("Clear Filters", fontSize = 10.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
          }
        }

        Spacer(modifier = Modifier.height(2.dp))

        if (reports.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f),
            contentAlignment = Alignment.Center,
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(48.dp),
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "No farm reports match your search or filter.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
              )
              Spacer(modifier = Modifier.height(8.dp))
              OutlinedButton(
                onClick = { viewModel.clearAllFilters() },
              ) {
                Text("Clear All Filters")
              }
            }
          }
        } else if (isWideScreen) {
          // Tablet / Wide Screen Grid
          LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(bottom = 80.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
          ) {
            items(reports, key = { it.id }) { report ->
              ReportCardItem(report = report, onClick = { viewModel.openReportDetail(report) })
            }
          }
        } else {
          // Mobile Card List
          LazyColumn(
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
          ) {
            items(reports, key = { it.id }) { report ->
              ReportCardItem(report = report, onClick = { viewModel.openReportDetail(report) })
            }
          }
        }
      }
    }
  }
}

@Composable
fun ReportCardItem(
  report: ReportEntity,
  onClick: () -> Unit,
) {
  val context = LocalContext.current

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag("report_card_${report.id}"),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
    ) {
      // Top row: Zone, Date, Status & PDF Export
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = report.zone,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = "Date: ${report.dateOfOperation}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          // Quick PDF Export Pill
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = CaneBlueSecondary.copy(alpha = 0.15f),
            modifier = Modifier
              .padding(end = 6.dp)
              .clickable {
                try {
                  val pdfFile = PdfExportHelper.exportReportToPdf(context, report)
                  PdfExportHelper.sharePdf(context, pdfFile, "Share ${report.id} PDF")
                } catch (e: Exception) {
                  // Fallback
                }
              }
              .testTag("quick_pdf_${report.id}"),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
              Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = "Export PDF",
                tint = CaneBlueSecondary,
                modifier = Modifier.size(12.dp),
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = "PDF",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = CaneBlueSecondary,
              )
            }
          }

          if (!report.photoUri.isNullOrBlank()) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = CaneGreenPrimary.copy(alpha = 0.15f),
              modifier = Modifier.padding(end = 6.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = CaneGreenPrimary, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text("Photo", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CaneGreenPrimary)
              }
            }
          }
          StatusBadge(status = report.status)
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Operation Name & Block
      Text(
        text = report.operationName,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )

      // Input Name & Ratoon Date badges
      if (report.inputName.isNotBlank() || report.ratoonDate.isNotBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          if (report.inputName.isNotBlank()) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            ) {
              Text(
                text = "Input: ${report.inputName}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }
          if (report.ratoonDate.isNotBlank()) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
            ) {
              Text(
                text = "Ratoon: ${report.ratoonDate}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Field Metrics Grid / Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Block: ${report.blockNumber}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
          )
          Text(
            text = "Contractor: ${report.contractorName}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
          Text(
            text = "Covered: ${report.areaCoveredHa} Acres",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "Balance: ${report.balanceToBeDoneHa} Acres",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Labourers: ${report.noOfLabourers} • By: ${report.createdBy}",
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = "ID: ${report.id}",
          fontSize = 10.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
      }
    }
  }
}
