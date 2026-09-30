package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.OperationWeeklySummary
import com.example.data.model.STANDARD_ZONES
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.theme.CaneBlueSecondary
import com.example.ui.theme.CaneGreenPrimary
import com.example.util.PdfExportHelper
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklySummaryScreen(
  viewModel: MainViewModel,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val startDate by viewModel.weeklySummaryStartDate.collectAsStateWithLifecycle()
  val endDate by viewModel.weeklySummaryEndDate.collectAsStateWithLifecycle()
  val selectedZone by viewModel.weeklySummaryZoneFilter.collectAsStateWithLifecycle()
  val summaries by viewModel.weeklyOperationsSummary.collectAsStateWithLifecycle()

  var zoneMenuExpanded by remember { mutableStateOf(false) }

  val calendar = Calendar.getInstance()
  val startDatePicker = remember {
    DatePickerDialog(
      context,
      { _, y, m, d ->
        val cal = Calendar.getInstance().apply { set(y, m, d) }
        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time)
        viewModel.weeklySummaryStartDate.value = dateStr
      },
      calendar.get(Calendar.YEAR),
      calendar.get(Calendar.MONTH),
      calendar.get(Calendar.DAY_OF_MONTH),
    )
  }

  val endDatePicker = remember {
    DatePickerDialog(
      context,
      { _, y, m, d ->
        val cal = Calendar.getInstance().apply { set(y, m, d) }
        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time)
        viewModel.weeklySummaryEndDate.value = dateStr
      },
      calendar.get(Calendar.YEAR),
      calendar.get(Calendar.MONTH),
      calendar.get(Calendar.DAY_OF_MONTH),
    )
  }

  val totalAreaCovered = summaries.sumOf { it.totalAreaAcres }
  val totalLabourers = summaries.sumOf { it.totalLabourers }
  val totalReportsCount = summaries.sumOf { it.reportCount }

  fun copyToClipboard(text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Weekly Operations Summary", text)
    clipboard.setPrimaryClip(clip)
    viewModel.showToast("Weekly summary copied to clipboard.")
  }

  fun shareSummaryText(text: String) {
    val sendIntent = Intent().apply {
      action = Intent.ACTION_SEND
      putExtra(Intent.EXTRA_TEXT, text)
      type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share Weekly Operations Summary"))
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Weekly Operations Summary",
              fontWeight = FontWeight.Bold,
              fontSize = 17.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "Area Covered Aggregation (Acres)",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
              maxLines = 1,
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = { viewModel.navigateTo(Screen.Dashboard) },
            modifier = Modifier.testTag("weekly_summary_back_button"),
          ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(
            onClick = {
              try {
                val pdfFile = PdfExportHelper.exportWeeklySummaryToPdf(
                  context,
                  startDate,
                  endDate,
                  selectedZone,
                  summaries,
                )
                PdfExportHelper.sharePdf(context, pdfFile, "Share Weekly Operations Summary PDF")
              } catch (e: Exception) {
                viewModel.showToast("Failed to generate PDF: ${e.message}")
              }
            },
            modifier = Modifier.testTag("pdf_weekly_summary_button"),
          ) {
            Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = MaterialTheme.colorScheme.onPrimary)
          }
          IconButton(
            onClick = {
              val text = viewModel.getWeeklySummaryShareText()
              copyToClipboard(text)
            },
            modifier = Modifier.testTag("copy_weekly_summary_button"),
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Summary", tint = MaterialTheme.colorScheme.onPrimary)
          }
          IconButton(
            onClick = {
              val text = viewModel.getWeeklySummaryShareText()
              shareSummaryText(text)
            },
            modifier = Modifier.testTag("share_weekly_summary_button"),
          ) {
            Icon(Icons.Default.Share, contentDescription = "Share Summary", tint = MaterialTheme.colorScheme.onPrimary)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.primary,
          titleContentColor = MaterialTheme.colorScheme.onPrimary,
          navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
          actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
      )
    },
    containerColor = Color.Transparent,
    modifier = modifier.fillMaxSize(),
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp),
      contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // 1. DATE RANGE & FILTER CONTROLS
      item {
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
          Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Text(
              text = "Select Weekly Period & Filters",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Date Selection Row
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              OutlinedTextField(
                value = startDate,
                onValueChange = {},
                readOnly = true,
                label = { Text("From Date") },
                trailingIcon = {
                  IconButton(onClick = { startDatePicker.show() }) {
                    Icon(Icons.Default.CalendarToday, contentDescription = "Select Start Date", modifier = Modifier.size(18.dp))
                  }
                },
                modifier = Modifier
                  .weight(1f)
                  .clickable { startDatePicker.show() }
                  .testTag("weekly_start_date_picker"),
                singleLine = true,
              )

              OutlinedTextField(
                value = endDate,
                onValueChange = {},
                readOnly = true,
                label = { Text("To Date") },
                trailingIcon = {
                  IconButton(onClick = { endDatePicker.show() }) {
                    Icon(Icons.Default.CalendarToday, contentDescription = "Select End Date", modifier = Modifier.size(18.dp))
                  }
                },
                modifier = Modifier
                  .weight(1f)
                  .clickable { endDatePicker.show() }
                  .testTag("weekly_end_date_picker"),
                singleLine = true,
              )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Preset Chips
            Text(
              text = "Quick Week Presets:",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.outline,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              // Exact user requested week
              FilterChip(
                selected = startDate == "01/09/2026" && endDate == "07/09/2026",
                onClick = { viewModel.setWeeklySummaryDateRange("01/09/2026", "07/09/2026") },
                label = { Text("01/09/2026 - 07/09/2026", fontSize = 12.sp) },
                leadingIcon = if (startDate == "01/09/2026" && endDate == "07/09/2026") {
                  { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                } else null,
              )

              FilterChip(
                selected = startDate == "08/09/2026" && endDate == "14/09/2026",
                onClick = { viewModel.setWeeklySummaryDateRange("08/09/2026", "14/09/2026") },
                label = { Text("08/09/2026 - 14/09/2026", fontSize = 12.sp) },
                leadingIcon = if (startDate == "08/09/2026" && endDate == "14/09/2026") {
                  { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                } else null,
              )

              FilterChip(
                selected = startDate == "01/09/2026" && endDate == "30/09/2026",
                onClick = { viewModel.setWeeklySummaryDateRange("01/09/2026", "30/09/2026") },
                label = { Text("September 2026 (Full Month)", fontSize = 12.sp) },
                leadingIcon = if (startDate == "01/09/2026" && endDate == "30/09/2026") {
                  { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                } else null,
              )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Zone Filter
            ExposedDropdownMenuBox(
              expanded = zoneMenuExpanded,
              onExpandedChange = { zoneMenuExpanded = !zoneMenuExpanded },
            ) {
              OutlinedTextField(
                value = selectedZone ?: "All Zones",
                onValueChange = {},
                readOnly = true,
                label = { Text("Zone Filter") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = zoneMenuExpanded) },
                modifier = Modifier
                  .fillMaxWidth()
                  .menuAnchor()
                  .testTag("weekly_zone_filter_dropdown"),
              )

              ExposedDropdownMenu(
                expanded = zoneMenuExpanded,
                onDismissRequest = { zoneMenuExpanded = false },
              ) {
                DropdownMenuItem(
                  text = { Text("All Zones") },
                  onClick = {
                    viewModel.setWeeklySummaryZone("All Zones")
                    zoneMenuExpanded = false
                  },
                )
                STANDARD_ZONES.forEach { zone ->
                  DropdownMenuItem(
                    text = { Text(zone) },
                    onClick = {
                      viewModel.setWeeklySummaryZone(zone)
                      zoneMenuExpanded = false
                    },
                  )
                }
              }
            }
          }
        }
      }

      // 2. GRAND TOTAL SUMMARY BANNER
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                  text = "Period: From $startDate to $endDate",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.ExtraBold,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = if (selectedZone.isNullOrBlank() || selectedZone == "All Zones") "All Plantation Zones" else selectedZone!!,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
              }

              Surface(
                shape = RoundedCornerShape(8.dp),
                color = CaneGreenPrimary,
              ) {
                Text(
                  text = "${summaries.size} Operations",
                  color = Color.White,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Metrics row
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text("Total Area Covered", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                Text(
                  text = "${"%.1f".format(totalAreaCovered)} Ac",
                  fontSize = 24.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
              }

              Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Total Labourers", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                Text(
                  text = "$totalLabourers",
                  fontSize = 22.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
              }

              Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text("Total Records", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                Text(
                  text = "$totalReportsCount",
                  fontSize = 22.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Share Button Row
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Button(
                onClick = {
                  val text = viewModel.getWeeklySummaryShareText()
                  copyToClipboard(text)
                },
                modifier = Modifier.weight(1f).height(42.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
              ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copy Report", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }

              OutlinedButton(
                onClick = {
                  val text = viewModel.getWeeklySummaryShareText()
                  shareSummaryText(text)
                },
                modifier = Modifier.weight(1f).height(42.dp),
                shape = RoundedCornerShape(10.dp),
              ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Share Report", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Dedicated PDF Export Action Button
            Button(
              onClick = {
                try {
                  val pdfFile = PdfExportHelper.exportWeeklySummaryToPdf(
                    context,
                    startDate,
                    endDate,
                    selectedZone,
                    summaries,
                  )
                  PdfExportHelper.sharePdf(context, pdfFile, "Share Weekly Operations Summary PDF")
                } catch (e: Exception) {
                  viewModel.showToast("Failed to generate PDF: ${e.message}")
                }
              },
              modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("export_weekly_summary_pdf_button"),
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = CaneBlueSecondary),
            ) {
              Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("EXPORT SUMMARY TO PDF (DOWNLOAD & SHARE)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      // 3. OPERATIONS BREAKDOWN HEADER
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Area Covered per Operation",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Text(
            text = "Aggregated Total",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
          )
        }
      }

      // 4. EMPTY OR LIST OF AGGREGATED OPERATIONS
      if (summaries.isEmpty()) {
        item {
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.outline)
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "No operations found for this period ($startDate to $endDate).",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = "Try adjusting the date range or zone filter above.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline,
              )
            }
          }
        }
      } else {
        items(summaries, key = { it.operationName }) { summary ->
          OperationSummaryCard(
            summary = summary,
            startDate = startDate,
            endDate = endDate,
            grandTotalArea = totalAreaCovered,
          )
        }
      }
    }
  }
}

@Composable
fun OperationSummaryCard(
  summary: OperationWeeklySummary,
  startDate: String,
  endDate: String,
  grandTotalArea: Double,
) {
  val percentOfTotal = if (grandTotalArea > 0.0) ((summary.totalAreaAcres / grandTotalArea) * 100).toInt() else 0
  val formattedArea = if (summary.totalAreaAcres % 1.0 == 0.0) "${summary.totalAreaAcres.toInt()}Ac" else "${"%.1f".format(summary.totalAreaAcres)}Ac"

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
      // Header: Operation Name & Total Area Covered Pill
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          modifier = Modifier.weight(1f).padding(end = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = when {
                summary.operationName.contains("shaving", ignoreCase = true) -> Icons.Default.ContentCut
                summary.operationName.contains("Planting", ignoreCase = true) -> Icons.Default.Eco
                summary.operationName.contains("Fertilizer", ignoreCase = true) -> Icons.Default.Science
                summary.operationName.contains("Harvesting", ignoreCase = true) -> Icons.Default.Agriculture
                summary.operationName.contains("Weeding", ignoreCase = true) -> Icons.Default.Grass
                else -> Icons.Default.WorkOutline
              },
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp),
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = summary.operationName,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 15.sp,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = "${summary.reportCount} record(s) filed",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.outline,
            )
          }
        }

        // Highlight Area Covered Pill
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = CaneGreenPrimary.copy(alpha = 0.12f),
        ) {
          Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          ) {
            Text(
              text = formattedArea,
              fontSize = 18.sp,
              fontWeight = FontWeight.ExtraBold,
              color = CaneGreenPrimary,
            )
            Text(
              text = "$percentOfTotal% of period",
              fontSize = 10.sp,
              fontWeight = FontWeight.SemiBold,
              color = CaneGreenPrimary,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Direct User Example Box:
      // "From 1/09/2026 to 7/09/2026, Area Covered by Stubble shaving is 80Ac"
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = CaneGreenPrimary,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "From $startDate to $endDate, Area Covered by ${summary.operationName} is $formattedArea",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Progress bar representing share of total area
      LinearProgressIndicator(
        progress = { (percentOfTotal / 100f).coerceIn(0f, 1f) },
        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
        color = CaneGreenPrimary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Operational Breakdown Row (Contractors, Labourers, Blocks, Zones)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f).padding(end = 4.dp)) {
          if (summary.contractors.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Engineering, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Contractor: ${summary.contractors.joinToString(", ")}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
          if (summary.blocks.isNotEmpty()) {
            Text(
              text = "Blocks: ${summary.blocks.joinToString(", ")}",
              fontSize = 10.sp,
              color = MaterialTheme.colorScheme.outline,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "${summary.totalLabourers} Labourers",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
          )
        }
      }
    }
  }
}
