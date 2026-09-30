package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.Role
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.DeleteConfirmationDialog
import com.example.ui.components.StatusBadge
import com.example.ui.theme.CaneBlueSecondary
import com.example.ui.theme.CaneGreenPrimary
import com.example.util.PdfExportHelper
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailScreen(
  viewModel: MainViewModel,
  modifier: Modifier = Modifier,
) {
  val report by viewModel.activeReport.collectAsStateWithLifecycle()
  val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
  var showDeleteDialog by remember { mutableStateOf(false) }

  if (report == null) {
    Box(
      modifier = modifier.fillMaxSize(),
      contentAlignment = Alignment.Center,
    ) {
      Text("No report selected.")
    }
    return
  }

  val active = report!!
  val isAdmin = currentUser?.role == Role.ADMIN
  val context = LocalContext.current
  val timeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text("UPENJAnet", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(active.zone, fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimary)
          }
        },
        navigationIcon = {
          IconButton(
            onClick = { viewModel.navigateTo(Screen.ReportsList) },
            modifier = Modifier.testTag("detail_back_button"),
          ) {
            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(
            onClick = {
              try {
                val file = PdfExportHelper.exportReportToPdf(context, active)
                PdfExportHelper.sharePdf(context, file, "Share ${active.id} PDF")
              } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
              }
            },
            modifier = Modifier.testTag("top_bar_export_pdf_button"),
          ) {
            Icon(
              imageVector = Icons.Default.PictureAsPdf,
              contentDescription = "Export PDF",
              tint = MaterialTheme.colorScheme.onPrimary,
            )
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
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(16.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Zone and Status Header Card
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
            Column {
              Text(
                text = active.zone,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
              )
              Text(
                text = "Report ID: ${active.id}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            StatusBadge(status = active.status)
          }
        }
      }

      // Operational Details Card
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
          Text(
            text = "Field Operation Details",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
          )

          DetailRow(
            icon = Icons.Default.CalendarToday,
            label = "Date of Operation",
            value = active.dateOfOperation,
          )

          DetailRow(
            icon = Icons.Default.Landscape,
            label = "Operation Name",
            value = active.operationName,
          )

          DetailRow(
            icon = Icons.Default.LocationOn,
            label = "Block Number",
            value = active.blockNumber,
          )

          DetailRow(
            icon = Icons.Default.Engineering,
            label = "Name of Contractor",
            value = active.contractorName,
          )

          DetailRow(
            icon = Icons.Default.Group,
            label = "No. of Labourers",
            value = active.noOfLabourers.toString(),
          )

          DetailRow(
            icon = Icons.Default.Info,
            label = "Area Covered by Operation",
            value = "${active.areaCoveredHa} Acres",
            highlight = true,
          )

          DetailRow(
            icon = Icons.Default.Info,
            label = "Balance to be Done",
            value = "${active.balanceToBeDoneHa} Acres",
          )

          if (active.inputName.isNotBlank()) {
            DetailRow(
              icon = Icons.Default.Info,
              label = "Input Name (Fertilizer / Chemical)",
              value = active.inputName,
              highlight = true,
            )
          }

          if (active.ratoonDate.isNotBlank()) {
            DetailRow(
              icon = Icons.Default.CalendarToday,
              label = "Ratoon Date",
              value = active.ratoonDate,
            )
          }

          if (active.remark.isNotBlank()) {
            DetailRow(
              icon = Icons.Default.Notes,
              label = "Remark",
              value = active.remark,
            )
          }
        }
      }

      // Photo Evidence Section
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.PhotoCamera,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Field Photo Evidence",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
              )
            }

            Surface(
              shape = RoundedCornerShape(6.dp),
              color = if (!active.photoUri.isNullOrBlank()) CaneGreenPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
            ) {
              Text(
                text = if (!active.photoUri.isNullOrBlank()) "Verified Attached" else "None Attached",
                color = if (!active.photoUri.isNullOrBlank()) CaneGreenPrimary else MaterialTheme.colorScheme.outline,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          if (!active.photoUri.isNullOrBlank()) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
              AsyncImage(
                model = active.photoUri,
                contentDescription = "Field photo evidence submitted for ${active.operationName}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Photo evidence submitted by ${active.createdBy} for ${active.zone} block ${active.blockNumber}.",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.outline,
            )
          } else {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = Icons.Default.Info,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.outline,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "No photo evidence was attached when this report was created.",
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }

      // Metadata Card
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = "System Tracking & Cloud Audit",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          DetailRow(
            icon = Icons.Default.Person,
            label = "Created By",
            value = "${active.createdBy} (${active.createdByRole.name})",
          )

          if (active.lastUpdatedBy.isNotBlank()) {
            DetailRow(
              icon = Icons.Default.Person,
              label = "Last Modified By",
              value = active.lastUpdatedBy,
            )
          }

          DetailRow(
            icon = Icons.Default.Schedule,
            label = "Date Created",
            value = timeFormat.format(Date(active.createdAt)),
          )

          DetailRow(
            icon = Icons.Default.Schedule,
            label = "Last Updated",
            value = timeFormat.format(Date(active.updatedAt)),
          )

          DetailRow(
            icon = Icons.Default.Info,
            label = "Cloud Sync Status",
            value = if (active.synced) "Synced with Shared Cloud Database" else "Saved locally on this device (Pending Cloud Upload)",
          )
        }
      }

      // Official PDF Export Card (Downloadable & Shareable)
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("detail_pdf_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = null,
                tint = CaneBlueSecondary,
                modifier = Modifier.size(24.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  text = "Official PDF Field Report",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = "All illustrations, photo evidence & operational metrics",
                  fontSize = 11.5.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }

          Text(
            text = "Export this complete field report to a formatted, printable PDF containing all metadata, contractor details, area metrics in Acres, and attached photo evidence. Download or share with supervisors directly.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Button(
              onClick = {
                try {
                  val file = PdfExportHelper.exportReportToPdf(context, active)
                  PdfExportHelper.sharePdf(context, file, "Share Field Report PDF (${active.id})")
                } catch (e: Exception) {
                  Toast.makeText(context, "Failed to generate PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                }
              },
              modifier = Modifier
                .weight(1f)
                .height(46.dp)
                .testTag("share_pdf_report_button"),
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = CaneBlueSecondary),
            ) {
              Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("SHARE PDF", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            OutlinedButton(
              onClick = {
                try {
                  val file = PdfExportHelper.exportReportToPdf(context, active)
                  PdfExportHelper.openPdf(context, file)
                  Toast.makeText(context, "PDF saved to downloads/cache: ${file.name}", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                  Toast.makeText(context, "Failed to open PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                }
              },
              modifier = Modifier
                .weight(1f)
                .height(46.dp)
                .testTag("view_save_pdf_button"),
              shape = RoundedCornerShape(10.dp),
            ) {
              Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("DOWNLOAD / VIEW", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      // Action Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        // Edit button - available to Admin and users
        Button(
          onClick = { viewModel.openEditReport(active) },
          modifier = Modifier
            .weight(1f)
            .height(50.dp)
            .testTag("edit_report_button"),
          shape = RoundedCornerShape(12.dp),
        ) {
          Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("EDIT REPORT", fontWeight = FontWeight.Bold)
        }

        // CRITICAL SECURITY RULE: DELETE BUTTON ONLY VISIBLE TO ADMIN
        if (isAdmin) {
          OutlinedButton(
            onClick = { showDeleteDialog = true },
            modifier = Modifier
              .weight(1f)
              .height(50.dp)
              .testTag("delete_report_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
              contentColor = MaterialTheme.colorScheme.error,
            ),
          ) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("DELETE", fontWeight = FontWeight.Bold)
          }
        }
      }

      // Security notice info
      if (!isAdmin) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "Record deletion is restricted to Administrators.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }

  // Admin Confirmation Dialog
  if (showDeleteDialog) {
    DeleteConfirmationDialog(
      onDismiss = { showDeleteDialog = false },
      onConfirm = {
        showDeleteDialog = false
        viewModel.deleteReport(active.id) { }
      },
    )
  }
}

@Composable
fun DetailRow(
  icon: ImageVector,
  label: String,
  value: String,
  highlight: Boolean = false,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.Top,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier
        .size(20.dp)
        .padding(top = 2.dp),
    )
    Spacer(modifier = Modifier.width(10.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = label,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = value,
        fontSize = 14.sp,
        fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium,
        color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}
