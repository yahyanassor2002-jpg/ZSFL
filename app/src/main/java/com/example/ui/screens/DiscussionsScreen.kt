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
import com.example.data.model.DiscussionEntity
import com.example.data.model.Role
import com.example.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

private val DIVISIONS = listOf(
  "North Division",
  "South Division",
  "Central Division",
  "East Division",
  "West Division",
  "Factory Liaison",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscussionsScreen(
  viewModel: MainViewModel,
  modifier: Modifier = Modifier,
) {
  val discussions by viewModel.allDiscussions.collectAsStateWithLifecycle()
  val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
  val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()

  var selectedDivision by remember { mutableStateOf<String?>(null) }
  var searchQuery by remember { mutableStateOf("") }
  var showDialog by remember { mutableStateOf(false) }
  var editingDiscussion by remember { mutableStateOf<DiscussionEntity?>(null) }
  var discussionToDelete by remember { mutableStateOf<DiscussionEntity?>(null) }

  val filteredList = remember(discussions, selectedDivision, searchQuery) {
    discussions.filter { d ->
      val matchesDivision = selectedDivision == null || d.division.equals(selectedDivision, ignoreCase = true)
      val matchesQuery = searchQuery.isBlank() ||
        d.title.contains(searchQuery, ignoreCase = true) ||
        d.notes.contains(searchQuery, ignoreCase = true) ||
        d.author.contains(searchQuery, ignoreCase = true) ||
        d.division.contains(searchQuery, ignoreCase = true)
      matchesDivision && matchesQuery
    }
  }

  Scaffold(
    containerColor = Color.Transparent,
    modifier = modifier.fillMaxSize(),
    floatingActionButton = {
      FloatingActionButton(
        onClick = {
          editingDiscussion = null
          showDialog = true
        },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.testTag("add_discussion_fab"),
      ) {
        Icon(imageVector = Icons.Default.AddComment, contentDescription = "New Discussion")
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

      // Top Header with Title and Sync Action
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = "Division Discussions",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = "Field operations coordination & notes",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        IconButton(
          onClick = { viewModel.syncNow() },
          enabled = !isSyncing,
          modifier = Modifier.testTag("sync_discussions_button"),
        ) {
          Icon(
            imageVector = Icons.Default.Sync,
            contentDescription = "Sync Discussions",
            tint = MaterialTheme.colorScheme.primary,
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Search Bar
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search discussions, topics, notes...") },
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
        modifier = Modifier.fillMaxWidth().testTag("discussion_search_input"),
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Division Filter Chips
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        FilterChip(
          selected = selectedDivision == null,
          onClick = { selectedDivision = null },
          label = { Text("All Divisions") },
        )

        DIVISIONS.forEach { div ->
          FilterChip(
            selected = selectedDivision == div,
            onClick = { selectedDivision = if (selectedDivision == div) null else div },
            label = { Text(div) },
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = "${filteredList.size} discussions recorded",
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
              imageVector = Icons.Default.Forum,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.outline,
              modifier = Modifier.size(54.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "No discussions found for this criteria.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.outline,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
              onClick = {
                editingDiscussion = null
                showDialog = true
              },
            ) {
              Icon(Icons.Default.Add, contentDescription = null)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Start New Discussion")
            }
          }
        }
      } else {
        LazyColumn(
          contentPadding = PaddingValues(bottom = 88.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxSize(),
        ) {
          items(filteredList, key = { it.id }) { disc ->
            DiscussionCard(
              discussion = disc,
              currentUser = currentUser,
              onEdit = {
                editingDiscussion = disc
                showDialog = true
              },
              onDelete = {
                discussionToDelete = disc
              },
            )
          }
        }
      }
    }
  }

  // Create or Edit Dialog
  if (showDialog) {
    DiscussionFormDialog(
      existing = editingDiscussion,
      onDismiss = { showDialog = false },
      onSave = { title, notes, div, date ->
        viewModel.saveDiscussion(
          id = editingDiscussion?.id,
          title = title,
          notes = notes,
          division = div,
          date = date,
        ) { success ->
          if (success) showDialog = false
        }
      },
    )
  }

  // Delete Confirmation Dialog
  if (discussionToDelete != null) {
    val disc = discussionToDelete!!
    AlertDialog(
      onDismissRequest = { discussionToDelete = null },
      title = { Text("Delete Discussion?", fontWeight = FontWeight.Bold) },
      text = { Text("Are you sure you want to delete '${disc.title}'? This will sync deletion across all devices.") },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteDiscussion(disc.id) {
              discussionToDelete = null
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) {
          Text("DELETE")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { discussionToDelete = null }) {
          Text("CANCEL")
        }
      },
    )
  }
}

@Composable
fun DiscussionCard(
  discussion: DiscussionEntity,
  currentUser: com.example.data.model.UserEntity?,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
) {
  val canModify = currentUser?.role == Role.ADMIN || currentUser?.username.equals(discussion.author, ignoreCase = true)

  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = Modifier.fillMaxWidth().testTag("discussion_card_${discussion.id}"),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
      // Top header: Division badge & Date
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
        ) {
          Text(
            text = discussion.division,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.CalendarToday,
            contentDescription = null,
            modifier = Modifier.size(13.dp),
            tint = MaterialTheme.colorScheme.outline,
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = discussion.date,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.outline,
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = discussion.title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = discussion.notes,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Spacer(modifier = Modifier.height(10.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
      Spacer(modifier = Modifier.height(8.dp))

      // Footer: Author & actions
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(15.dp),
            tint = MaterialTheme.colorScheme.primary,
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "${discussion.author} (${discussion.authorRole.name})",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }

        if (canModify) {
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
              Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit Discussion",
                modifier = Modifier.size(17.dp),
                tint = MaterialTheme.colorScheme.primary,
              )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Discussion",
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
fun DiscussionFormDialog(
  existing: DiscussionEntity?,
  onDismiss: () -> Unit,
  onSave: (title: String, notes: String, division: String, date: String) -> Unit,
) {
  val context = LocalContext.current
  val todayFormatted = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

  var title by remember { mutableStateOf(existing?.title ?: "") }
  var notes by remember { mutableStateOf(existing?.notes ?: "") }
  var division by remember { mutableStateOf(existing?.division ?: DIVISIONS.first()) }
  var date by remember { mutableStateOf(existing?.date ?: todayFormatted) }
  var divisionExpanded by remember { mutableStateOf(false) }

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
        text = if (existing != null) "Edit Discussion" else "New Division Discussion",
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
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Topic / Title") },
          placeholder = { Text("e.g. Cane Transport Bottleneck") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("discussion_title_input"),
        )

        // Division Selector
        ExposedDropdownMenuBox(
          expanded = divisionExpanded,
          onExpandedChange = { divisionExpanded = !divisionExpanded },
        ) {
          OutlinedTextField(
            value = division,
            onValueChange = {},
            readOnly = true,
            label = { Text("Division") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = divisionExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
          )
          ExposedDropdownMenu(
            expanded = divisionExpanded,
            onDismissRequest = { divisionExpanded = false },
          ) {
            DIVISIONS.forEach { div ->
              DropdownMenuItem(
                text = { Text(div) },
                onClick = {
                  division = div
                  divisionExpanded = false
                },
              )
            }
          }
        }

        // Date Picker Field
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

        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Discussion Notes / Action Items") },
          placeholder = { Text("Record points discussed, decisions reached, responsible parties...") },
          minLines = 4,
          maxLines = 6,
          modifier = Modifier.fillMaxWidth().testTag("discussion_notes_input"),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (title.isNotBlank() && notes.isNotBlank()) {
            onSave(title, notes, division, date)
          }
        },
        enabled = title.isNotBlank() && notes.isNotBlank(),
        modifier = Modifier.testTag("save_discussion_button"),
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
