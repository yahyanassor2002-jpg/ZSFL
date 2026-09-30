package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.COMMON_OPERATIONS
import com.example.data.model.STANDARD_CONTRACTORS
import com.example.data.model.STANDARD_ZONES
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.theme.CaneGreenPrimary
import java.text.SimpleDateFormat
import java.util.*

private val COMMON_INPUTS = listOf(
  "Urea 46% N",
  "CAN (Calcium Ammonium Nitrate)",
  "NPK 20-10-10",
  "Glyphosate 480 SL",
  "2,4-D Amine Salt",
  "R570 Seed Cane",
  "N14 Seed Cane",
  "Filter Mud / Bagasse Compost",
  "Pre-emergence Herbicide",
  "Chlorpyrifos Termiticide",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewOrEditReportScreen(
  viewModel: MainViewModel,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val activeReport by viewModel.activeReport.collectAsStateWithLifecycle()
  val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

  val isEditing = activeReport != null
  val defaultZone = currentUser?.zoneId?.takeIf { it.isNotBlank() } ?: STANDARD_ZONES.first()
  val todayFormatted = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

  // Form Fields State
  var selectedZone by remember(activeReport) {
    mutableStateOf(activeReport?.zone ?: defaultZone)
  }
  var dateOfOperation by remember(activeReport) {
    mutableStateOf(activeReport?.dateOfOperation ?: todayFormatted)
  }
  var operationName by remember(activeReport) {
    mutableStateOf(activeReport?.operationName ?: "")
  }
  var blockNumber by remember(activeReport) {
    mutableStateOf(activeReport?.blockNumber ?: "")
  }
  var contractorName by remember(activeReport) {
    mutableStateOf(activeReport?.contractorName ?: "")
  }
  var noOfLabourersText by remember(activeReport) {
    mutableStateOf(activeReport?.noOfLabourers?.toString() ?: "")
  }
  var areaCoveredText by remember(activeReport) {
    mutableStateOf(activeReport?.areaCoveredHa?.toString() ?: "")
  }
  var balanceToBeDoneText by remember(activeReport) {
    mutableStateOf(activeReport?.balanceToBeDoneHa?.toString() ?: "")
  }
  var inputName by remember(activeReport) {
    mutableStateOf(activeReport?.inputName ?: "")
  }
  var ratoonDate by remember(activeReport) {
    mutableStateOf(activeReport?.ratoonDate ?: "")
  }
  var remark by remember(activeReport) {
    mutableStateOf(activeReport?.remark ?: "")
  }
  var photoUri by remember(activeReport) {
    mutableStateOf(activeReport?.photoUri)
  }

  // Photo Picker Launcher
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    if (uri != null) {
      photoUri = uri.toString()
    }
  }

  // Validation & Menu States
  var hasAttemptedSubmit by remember { mutableStateOf(false) }
  var zoneMenuExpanded by remember { mutableStateOf(false) }
  var contractorMenuExpanded by remember { mutableStateOf(false) }
  var inputMenuExpanded by remember { mutableStateOf(false) }

  // Validation calculations
  val isZoneError = hasAttemptedSubmit && selectedZone.isBlank()
  val isDateError = hasAttemptedSubmit && dateOfOperation.isBlank()
  val isOperationError = hasAttemptedSubmit && operationName.isBlank()
  val isBlockError = hasAttemptedSubmit && blockNumber.isBlank()
  val isContractorError = hasAttemptedSubmit && contractorName.isBlank()
  val areaValue = areaCoveredText.toDoubleOrNull()
  val isAreaError = hasAttemptedSubmit && (areaValue == null || areaValue <= 0.0)

  // DatePicker Setup for Date of Operation
  val calOp = Calendar.getInstance()
  val opDatePickerDialog = remember {
    DatePickerDialog(
      context,
      { _, year, month, dayOfMonth ->
        val cal = Calendar.getInstance().apply {
          set(Calendar.YEAR, year)
          set(Calendar.MONTH, month)
          set(Calendar.DAY_OF_MONTH, dayOfMonth)
        }
        dateOfOperation = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time)
      },
      calOp.get(Calendar.YEAR),
      calOp.get(Calendar.MONTH),
      calOp.get(Calendar.DAY_OF_MONTH),
    )
  }

  // DatePicker Setup for Ratoon Date
  val calRatoon = Calendar.getInstance()
  val ratoonDatePickerDialog = remember {
    DatePickerDialog(
      context,
      { _, year, month, dayOfMonth ->
        val cal = Calendar.getInstance().apply {
          set(Calendar.YEAR, year)
          set(Calendar.MONTH, month)
          set(Calendar.DAY_OF_MONTH, dayOfMonth)
        }
        ratoonDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time)
      },
      calRatoon.get(Calendar.YEAR),
      calRatoon.get(Calendar.MONTH),
      calRatoon.get(Calendar.DAY_OF_MONTH),
    )
  }

  Scaffold(
    containerColor = Color.Transparent,
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = if (isEditing) "Edit Sugarcane Report" else "New Farm Record",
            fontWeight = FontWeight.Bold,
          )
        },
        navigationIcon = {
          IconButton(
            onClick = {
              if (isEditing && activeReport != null) {
                viewModel.navigateTo(Screen.ReportDetail)
              } else {
                viewModel.navigateTo(Screen.ReportsList)
              }
            },
            modifier = Modifier.testTag("report_form_back_button"),
          ) {
            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.primary,
          titleContentColor = MaterialTheme.colorScheme.onPrimary,
          navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
      )
    },
    modifier = modifier.fillMaxSize(),
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp)
        .verticalScroll(rememberScrollState()),
    ) {
      Spacer(modifier = Modifier.height(14.dp))

      // VALIDATION ALERT BANNER
      if (hasAttemptedSubmit && (isZoneError || isDateError || isOperationError || isBlockError || isContractorError || isAreaError)) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.errorContainer,
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Please complete all required fields highlighted in red below.",
              color = MaterialTheme.colorScheme.onErrorContainer,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }

      // ZONE SELECTOR
      Text(
        text = "Plantation Zone *",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = if (isZoneError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
      )

      Spacer(modifier = Modifier.height(6.dp))

      ExposedDropdownMenuBox(
        expanded = zoneMenuExpanded,
        onExpandedChange = { zoneMenuExpanded = !zoneMenuExpanded },
      ) {
        OutlinedTextField(
          value = selectedZone,
          onValueChange = {},
          readOnly = true,
          label = { Text("Zone") },
          isError = isZoneError,
          supportingText = if (isZoneError) { { Text("Zone selection is required", color = MaterialTheme.colorScheme.error) } } else null,
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = zoneMenuExpanded) },
          modifier = Modifier
            .fillMaxWidth()
            .menuAnchor()
            .testTag("zone_selector_dropdown"),
        )

        ExposedDropdownMenu(
          expanded = zoneMenuExpanded,
          onDismissRequest = { zoneMenuExpanded = false },
        ) {
          STANDARD_ZONES.forEach { zoneOption ->
            DropdownMenuItem(
              text = { Text(zoneOption) },
              onClick = {
                selectedZone = zoneOption
                zoneMenuExpanded = false
              },
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // DATE OF OPERATION (WITH MATERIAL DATE PICKER)
      OutlinedTextField(
        value = dateOfOperation,
        onValueChange = { dateOfOperation = it },
        label = { Text("Date of Operation *") },
        placeholder = { Text("dd/MM/yyyy") },
        readOnly = true,
        isError = isDateError,
        supportingText = if (isDateError) { { Text("Operation date is required", color = MaterialTheme.colorScheme.error) } } else null,
        trailingIcon = {
          IconButton(
            onClick = { opDatePickerDialog.show() },
            modifier = Modifier.testTag("date_picker_trigger"),
          ) {
            Icon(
              imageVector = Icons.Default.CalendarMonth,
              contentDescription = "Select Date",
              tint = MaterialTheme.colorScheme.primary,
            )
          }
        },
        modifier = Modifier
          .fillMaxWidth()
          .clickable { opDatePickerDialog.show() }
          .testTag("date_input"),
      )

      Spacer(modifier = Modifier.height(14.dp))

      // RATOON DATE (NEW FIELD WITH DATE PICKER)
      Card(
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        shape = RoundedCornerShape(12.dp),
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Text(
            text = "Ratoon Information",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
          )
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = ratoonDate,
            onValueChange = { ratoonDate = it },
            label = { Text("Ratoon Date") },
            placeholder = { Text("Select ratoon or cane cutting date (dd/MM/yyyy)") },
            readOnly = true,
            trailingIcon = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                if (ratoonDate.isNotBlank()) {
                  TextButton(onClick = { ratoonDate = "" }) {
                    Text("Clear", fontSize = 12.sp)
                  }
                }
                IconButton(
                  onClick = { ratoonDatePickerDialog.show() },
                  modifier = Modifier.testTag("ratoon_date_picker_trigger"),
                ) {
                  Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Select Ratoon Date",
                    tint = MaterialTheme.colorScheme.primary,
                  )
                }
              }
            },
            modifier = Modifier
              .fillMaxWidth()
              .clickable { ratoonDatePickerDialog.show() }
              .testTag("ratoon_date_input"),
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // OPERATION NAME
      Text(
        text = "Operation Name *",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = if (isOperationError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
      )
      Spacer(modifier = Modifier.height(4.dp))

      OutlinedTextField(
        value = operationName,
        onValueChange = { operationName = it },
        label = { Text("Operation / Activity") },
        placeholder = { Text("e.g. CANE HARVESTING, FERTILIZER APPLICATION...") },
        singleLine = true,
        isError = isOperationError,
        supportingText = if (isOperationError) { { Text("Operation name is required", color = MaterialTheme.colorScheme.error) } } else null,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("operation_name_input"),
      )

      Spacer(modifier = Modifier.height(6.dp))

      // SUGGESTION CHIPS FOR COMMON OPERATIONS
      Text(
        text = "Quick Select Standard Operations:",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        COMMON_OPERATIONS.take(6).forEach { op ->
          val isSelected = operationName.equals(op, ignoreCase = true)
          FilterChip(
            selected = isSelected,
            onClick = { operationName = op },
            label = { Text(op, fontSize = 12.sp) },
            leadingIcon = if (isSelected) {
              { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
            } else null,
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // INPUT NAME (NEW FIELD - FERTILIZER, HERBICIDE, SEED CANE)
      Card(
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
        ),
        shape = RoundedCornerShape(12.dp),
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Eco,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Agricultural Input Details",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
          }
          Spacer(modifier = Modifier.height(6.dp))

          ExposedDropdownMenuBox(
            expanded = inputMenuExpanded,
            onExpandedChange = { inputMenuExpanded = !inputMenuExpanded },
          ) {
            OutlinedTextField(
              value = inputName,
              onValueChange = { inputName = it },
              label = { Text("Input Name (Fertilizer, Herbicide, Seed Cane)") },
              placeholder = { Text("e.g. Urea 46%, CAN, Glyphosate 480SL, R570 Seed Cane") },
              singleLine = true,
              trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = inputMenuExpanded)
              },
              modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
                .testTag("input_name_field"),
            )

            ExposedDropdownMenu(
              expanded = inputMenuExpanded,
              onDismissRequest = { inputMenuExpanded = false },
            ) {
              COMMON_INPUTS.forEach { inputOption ->
                DropdownMenuItem(
                  text = { Text(inputOption) },
                  onClick = {
                    inputName = inputOption
                    inputMenuExpanded = false
                  },
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Quick Select Plantation Inputs:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            COMMON_INPUTS.forEach { inputChip ->
              val isSelected = inputName.equals(inputChip, ignoreCase = true)
              FilterChip(
                selected = isSelected,
                onClick = { inputName = inputChip },
                label = { Text(inputChip, fontSize = 11.sp) },
                leadingIcon = if (isSelected) {
                  { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                } else null,
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // BLOCK NUMBER
      OutlinedTextField(
        value = blockNumber,
        onValueChange = { blockNumber = it.uppercase(Locale.getDefault()) },
        label = { Text("Block Number *") },
        placeholder = { Text("e.g. B-04, BLK-12A") },
        singleLine = true,
        isError = isBlockError,
        supportingText = if (isBlockError) { { Text("Block number is required", color = MaterialTheme.colorScheme.error) } } else null,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("block_number_input"),
      )

      Spacer(modifier = Modifier.height(12.dp))

      // CONTRACTOR NAME (DROPDOWN & QUICK SELECT: Raphael, Hussein, Khamis Fumu, Jackson)
      Text(
        text = "Name of Contractor / Supervisor *",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = if (isContractorError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
      )
      Spacer(modifier = Modifier.height(4.dp))

      ExposedDropdownMenuBox(
        expanded = contractorMenuExpanded,
        onExpandedChange = { contractorMenuExpanded = !contractorMenuExpanded },
      ) {
        OutlinedTextField(
          value = contractorName,
          onValueChange = { contractorName = it },
          label = { Text("Select or Enter Contractor Name *") },
          placeholder = { Text("e.g. Raphael, Hussein, Khamis Fumu, Jackson") },
          singleLine = true,
          isError = isContractorError,
          supportingText = if (isContractorError) { { Text("Contractor / supervisor name is required", color = MaterialTheme.colorScheme.error) } } else null,
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = contractorMenuExpanded) },
          modifier = Modifier
            .fillMaxWidth()
            .menuAnchor()
            .testTag("contractor_name_input"),
        )

        ExposedDropdownMenu(
          expanded = contractorMenuExpanded,
          onDismissRequest = { contractorMenuExpanded = false },
        ) {
          STANDARD_CONTRACTORS.forEach { contractorOption ->
            DropdownMenuItem(
              text = { Text(contractorOption, fontWeight = FontWeight.Medium) },
              onClick = {
                contractorName = contractorOption
                contractorMenuExpanded = false
              },
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "Quick Select Contractors:",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        STANDARD_CONTRACTORS.forEach { cName ->
          val isSelected = contractorName.equals(cName, ignoreCase = true)
          FilterChip(
            selected = isSelected,
            onClick = { contractorName = cName },
            label = { Text(cName, fontSize = 12.sp) },
            leadingIcon = if (isSelected) {
              { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
            } else null,
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // NO OF LABOURERS
      OutlinedTextField(
        value = noOfLabourersText,
        onValueChange = { text ->
          if (text.isEmpty() || text.all { it.isDigit() }) {
            noOfLabourersText = text
          }
        },
        label = { Text("No. of Labourers Engaged") },
        placeholder = { Text("e.g. 25") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("labourers_input"),
      )

      Spacer(modifier = Modifier.height(12.dp))

      // AREA COVERED & BALANCE TO BE DONE
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = areaCoveredText,
          onValueChange = { text ->
            if (text.isEmpty() || text.matches(Regex("^\\d*\\.?\\d*$"))) {
              areaCoveredText = text
            }
          },
          label = { Text("Area Covered (Acres) *") },
          placeholder = { Text("e.g. 3.5") },
          isError = isAreaError,
          supportingText = if (isAreaError) { { Text("Valid area (> 0) required", color = MaterialTheme.colorScheme.error) } } else null,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier
            .weight(1f)
            .testTag("area_covered_input"),
        )

        OutlinedTextField(
          value = balanceToBeDoneText,
          onValueChange = { text ->
            if (text.isEmpty() || text.matches(Regex("^\\d*\\.?\\d*$"))) {
              balanceToBeDoneText = text
            }
          },
          label = { Text("Balance to Do (Acres)") },
          placeholder = { Text("e.g. 1.5") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier
            .weight(1f)
            .testTag("balance_to_be_done_input"),
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // PHOTO EVIDENCE ATTACHMENT CARD (FOR HEADMAN & OFFICERS)
      Card(
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
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

            if (!photoUri.isNullOrBlank()) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = CaneGreenPrimary.copy(alpha = 0.15f),
              ) {
                Text(
                  text = "Photo Attached",
                  color = CaneGreenPrimary,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Attach photo evidence of field work (cane growth, stubble shaving, weed clearance, fertilizer application).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          Spacer(modifier = Modifier.height(12.dp))

          if (!photoUri.isNullOrBlank()) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface),
            ) {
              AsyncImage(
                model = photoUri,
                contentDescription = "Attached photo evidence",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
              )

              Row(
                modifier = Modifier
                  .align(Alignment.TopEnd)
                  .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                IconButton(
                  onClick = {
                    photoPickerLauncher.launch(
                      PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                  },
                  modifier = Modifier
                    .size(36.dp)
                    .background(Color.Black.copy(alpha = 0.65f), CircleShape),
                ) {
                  Icon(Icons.Default.Edit, contentDescription = "Change photo", tint = Color.White, modifier = Modifier.size(18.dp))
                }
                IconButton(
                  onClick = { photoUri = null },
                  modifier = Modifier
                    .size(36.dp)
                    .background(Color.Black.copy(alpha = 0.65f), CircleShape),
                ) {
                  Icon(Icons.Default.Delete, contentDescription = "Remove photo", tint = Color.White, modifier = Modifier.size(18.dp))
                }
              }
            }
          } else {
            OutlinedButton(
              onClick = {
                photoPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("attach_photo_button"),
              shape = RoundedCornerShape(10.dp),
            ) {
              Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Upload Photo Evidence", fontWeight = FontWeight.SemiBold)
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // REMARKS
      OutlinedTextField(
        value = remark,
        onValueChange = { remark = it },
        label = { Text("Remark / Field Comments") },
        placeholder = { Text("Additional notes, soil condition, weather, cane quality...") },
        minLines = 3,
        maxLines = 5,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("remark_input"),
      )

      Spacer(modifier = Modifier.height(24.dp))

      // ACTION BUTTONS: SAVE DRAFT & SUBMIT REPORT
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        // SAVE DRAFT
        OutlinedButton(
          onClick = {
            val labourers = noOfLabourersText.toIntOrNull() ?: 0
            val area = areaCoveredText.toDoubleOrNull() ?: 0.0
            val balance = balanceToBeDoneText.toDoubleOrNull() ?: 0.0

            viewModel.saveReport(
              id = activeReport?.id,
              zone = selectedZone,
              dateOfOperation = dateOfOperation,
              operationName = operationName.ifBlank { "Untitled Operation" },
              blockNumber = blockNumber,
              contractorName = contractorName,
              noOfLabourers = labourers,
              areaCoveredHa = area,
              balanceToBeDoneHa = balance,
              inputName = inputName,
              ratoonDate = ratoonDate,
              remark = remark,
              photoUri = photoUri,
              isDraft = true,
            ) { }
          },
          modifier = Modifier
            .weight(1f)
            .height(52.dp)
            .testTag("save_draft_button"),
          shape = RoundedCornerShape(12.dp),
        ) {
          Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("SAVE DRAFT", fontWeight = FontWeight.Bold)
        }

        // SUBMIT & SYNC REPORT
        Button(
          onClick = {
            hasAttemptedSubmit = true
            if (isZoneError || isDateError || isOperationError || isBlockError || isContractorError || isAreaError) {
              viewModel.showToast("Please fill in all required fields marked in red.")
              return@Button
            }

            val labourers = noOfLabourersText.toIntOrNull() ?: 0
            val area = areaCoveredText.toDoubleOrNull() ?: 0.0
            val balance = balanceToBeDoneText.toDoubleOrNull() ?: 0.0

            viewModel.saveReport(
              id = activeReport?.id,
              zone = selectedZone,
              dateOfOperation = dateOfOperation,
              operationName = operationName,
              blockNumber = blockNumber,
              contractorName = contractorName,
              noOfLabourers = labourers,
              areaCoveredHa = area,
              balanceToBeDoneHa = balance,
              inputName = inputName,
              ratoonDate = ratoonDate,
              remark = remark,
              photoUri = photoUri,
              isDraft = false,
            ) { }
          },
          modifier = Modifier
            .weight(1f)
            .height(52.dp)
            .testTag("submit_report_button"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
          Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(if (isEditing) "UPDATE & SYNC" else "SUBMIT & SYNC", fontWeight = FontWeight.Bold)
        }
      }

      Spacer(modifier = Modifier.height(32.dp))
    }
  }
}
