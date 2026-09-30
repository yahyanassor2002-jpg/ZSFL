package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ReportStatus
import com.example.data.model.STANDARD_ZONES
import com.example.ui.MainViewModel
import com.example.ui.theme.CaneBlueSecondary
import com.example.util.PdfExportHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncAndExportScreen(
  viewModel: MainViewModel,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val reports by viewModel.allReports.collectAsStateWithLifecycle()
  val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
  val lastSyncMsg by viewModel.lastSyncMessage.collectAsStateWithLifecycle()

  val pendingCount = reports.count { it.status == ReportStatus.PENDING_SYNC || !it.synced }
  val syncedCount = reports.count { it.status == ReportStatus.SYNCED || it.synced }

  // Export filters
  var exportZone by remember { mutableStateOf<String?>("All Zones") }
  var exportDateFrom by remember { mutableStateOf("") }
  var exportDateTo by remember { mutableStateOf("") }
  var zoneMenuExpanded by remember { mutableStateOf(false) }

  // Date picker helper
  val calendar = Calendar.getInstance()
  val fromDatePicker = remember {
    DatePickerDialog(
      context,
      { _, y, m, d ->
        val cal = Calendar.getInstance().apply { set(y, m, d) }
        exportDateFrom = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time)
      },
      calendar.get(Calendar.YEAR),
      calendar.get(Calendar.MONTH),
      calendar.get(Calendar.DAY_OF_MONTH),
    )
  }
  val toDatePicker = remember {
    DatePickerDialog(
      context,
      { _, y, m, d ->
        val cal = Calendar.getInstance().apply { set(y, m, d) }
        exportDateTo = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time)
      },
      calendar.get(Calendar.YEAR),
      calendar.get(Calendar.MONTH),
      calendar.get(Calendar.DAY_OF_MONTH),
    )
  }

  // Filtered reports for export
  val exportReports = reports.filter { r ->
    val matchZone = exportZone == "All Zones" || r.zone == exportZone
    // Filter date if set
    val matchDateFrom = exportDateFrom.isBlank() || r.dateOfOperation >= exportDateFrom
    val matchDateTo = exportDateTo.isBlank() || r.dateOfOperation <= exportDateTo
    matchZone && matchDateFrom && matchDateTo
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item {
      Text(
        text = "Cloud Sync & Data Export",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
      )
      Text(
        text = "Manage offline queues and export field records for management reports",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    // SYNCHRONIZATION STATUS CARD
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(42.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Default.CloudSync,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(24.dp),
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "Cloud Synchronization",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = "Offline-First Local Storage Active",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Card(
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(10.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(imageVector = Icons.Default.CloudQueue, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Pending Sync", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "$pendingCount reports",
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }
            }

            Card(
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(10.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(imageVector = Icons.Default.CloudDone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Cloud Synced", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "$syncedCount reports",
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }
            }
          }

          if (lastSyncMsg != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = lastSyncMsg ?: "",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Medium,
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          Button(
            onClick = { viewModel.syncNow() },
            enabled = !isSyncing,
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("sync_now_screen_button"),
            shape = RoundedCornerShape(10.dp),
          ) {
            if (isSyncing) {
              CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text("Synchronizing Pending Records...")
            } else {
              Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("SYNC NOW", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // EXPORT SECTION
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Default.FileDownload,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Export Field Reports",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "Generate CSV and formatted summary reports",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          // Zone Filter Dropdown
          ExposedDropdownMenuBox(
            expanded = zoneMenuExpanded,
            onExpandedChange = { zoneMenuExpanded = !zoneMenuExpanded },
          ) {
            OutlinedTextField(
              value = exportZone ?: "All Zones",
              onValueChange = {},
              readOnly = true,
              label = { Text("Filter Zone") },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = zoneMenuExpanded) },
              modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
                .testTag("export_zone_filter_dropdown"),
            )

            ExposedDropdownMenu(
              expanded = zoneMenuExpanded,
              onDismissRequest = { zoneMenuExpanded = false },
            ) {
              DropdownMenuItem(
                text = { Text("All Zones (Consolidated)") },
                onClick = {
                  exportZone = "All Zones"
                  zoneMenuExpanded = false
                },
              )
              STANDARD_ZONES.forEach { z ->
                DropdownMenuItem(
                  text = { Text(z) },
                  onClick = {
                    exportZone = z
                    zoneMenuExpanded = false
                  },
                )
              }
            }
          }

          // Date Range Inputs
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            OutlinedTextField(
              value = exportDateFrom,
              onValueChange = { exportDateFrom = it },
              label = { Text("From Date") },
              placeholder = { Text("DD/MM/YYYY") },
              trailingIcon = {
                IconButton(onClick = { fromDatePicker.show() }) {
                  Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "Pick date")
                }
              },
              modifier = Modifier.weight(1f),
            )

            OutlinedTextField(
              value = exportDateTo,
              onValueChange = { exportDateTo = it },
              label = { Text("To Date") },
              placeholder = { Text("DD/MM/YYYY") },
              trailingIcon = {
                IconButton(onClick = { toDatePicker.show() }) {
                  Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "Pick date")
                }
              },
              modifier = Modifier.weight(1f),
            )
          }

          Text(
            text = "${exportReports.size} record(s) ready for export",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
          )

          // Export Buttons
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Button(
              onClick = {
                viewModel.shareExportCsv(context, exportReports)
              },
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("export_csv_button"),
              shape = RoundedCornerShape(10.dp),
            ) {
              Icon(imageVector = Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Export CSV", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            OutlinedButton(
              onClick = {
                val zf = if (exportZone == "All Zones") null else exportZone
                viewModel.shareExportSummary(context, exportReports, zf, exportDateFrom.ifBlank { null }, exportDateTo.ifBlank { null })
              },
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("export_summary_button"),
              shape = RoundedCornerShape(10.dp),
            ) {
              Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Share Sheet", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Dedicated PDF Export Action
          Button(
            onClick = {
              if (exportReports.isNotEmpty()) {
                try {
                  val reportToExport = exportReports.first()
                  val pdfFile = PdfExportHelper.exportReportToPdf(context, reportToExport)
                  PdfExportHelper.sharePdf(context, pdfFile, "Share Field Report PDF (${reportToExport.id})")
                } catch (e: Exception) {
                  viewModel.showToast("Failed to generate PDF: ${e.message}")
                }
              } else {
                viewModel.showToast("No records match the current export filters.")
              }
            },
            modifier = Modifier
              .fillMaxWidth()
              .height(46.dp)
              .testTag("export_pdf_button"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CaneBlueSecondary),
          ) {
            Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("EXPORT TO ILLUSTRATED PDF", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
          }
        }
      }
    }
  }
}
