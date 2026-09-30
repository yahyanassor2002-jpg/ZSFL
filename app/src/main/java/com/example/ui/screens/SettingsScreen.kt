package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Role
import com.example.data.model.STANDARD_ZONES
import com.example.data.model.UserEntity
import com.example.ui.MainViewModel
import com.example.ui.components.UserProfileAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  viewModel: MainViewModel,
  modifier: Modifier = Modifier,
) {
  val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
  val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
  val userAvatarUri by viewModel.userAvatarUri.collectAsStateWithLifecycle()
  val isAdmin = currentUser?.role == Role.ADMIN

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    uri?.let {
      viewModel.updateUserAvatar(it.toString())
    }
  }

  var newUsername by remember { mutableStateOf("") }
  var newPassword by remember { mutableStateOf("") }
  var newZone by remember { mutableStateOf(STANDARD_ZONES.first()) }
  var zoneMenuExpanded by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item {
      Text(
        text = "System Settings & Roles",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
      )
      Text(
        text = "Manage user credentials, security roles, and application session",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    // CURRENT PROFILE CARD
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
              UserProfileAvatar(
                avatarUri = userAvatarUri,
                username = currentUser?.username,
                size = 54.dp,
                showEditBadge = true,
                onClick = {
                  photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                  )
                },
                modifier = Modifier.testTag("profile_avatar_settings"),
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = currentUser?.username ?: "Not Logged In",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = if (isAdmin) "Full System Administrator" else "Headman (${currentUser?.zoneId ?: "General"})",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }

            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isAdmin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
              Text(
                text = currentUser?.role?.name ?: "",
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          OutlinedButton(
            onClick = {
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            modifier = Modifier
              .fillMaxWidth()
              .height(38.dp)
              .testTag("change_profile_photo_button"),
          ) {
            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Change Profile Picture (Photo / Gallery)", fontSize = 12.sp)
          }

          Spacer(modifier = Modifier.height(14.dp))
          Divider()
          Spacer(modifier = Modifier.height(14.dp))

          // Security Rules Summary
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Active Permissions", fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = if (isAdmin) {
              "• Full control across all 5 zones\n• Report creation, editing, and filtering\n• ONLY ADMIN: Permanently delete reports\n• Manage Headman accounts and system export"
            } else {
              "• Record and edit field reports\n• Offline local saving & sync\n• CANNOT DELETE REPORTS (Enforced by role security)"
            },
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          Spacer(modifier = Modifier.height(16.dp))

          OutlinedButton(
            onClick = { viewModel.logout() },
            modifier = Modifier
              .fillMaxWidth()
              .height(44.dp)
              .testTag("logout_button"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(
              contentColor = MaterialTheme.colorScheme.error,
            ),
          ) {
            Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("LOGOUT OF UPENJAnet", fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    // SHARED CLOUD DATABASE & SYNC CARD
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
              Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Shared Cloud Backend", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
            ) {
              Text(
                text = "RESTful API v1",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Every farm report, discussion note, zone progress milestone, and weekly equipment requirement is synchronized across all plantation phones in real time.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          Spacer(modifier = Modifier.height(10.dp))
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text("Shared Object Cloud Endpoint ID:", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
              Text("ff808181a09d98f701a0b605409f38e9", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Button(
              onClick = { viewModel.syncNow() },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(10.dp),
            ) {
              Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Sync Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
              onClick = { viewModel.navigateTo(com.example.ui.Screen.SyncAndExport) },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(10.dp),
            ) {
              Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Export Hub", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // FAST ACCOUNT SWITCHER (FOR TESTING ROLE PERMISSIONS)
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Quick Switch User (Test Role Permissions)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Switch between Admin and Headman to test the delete button restriction.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            allUsers.forEach { user ->
              val isCurrent = user.username.equals(currentUser?.username, ignoreCase = true)
              OutlinedButton(
                onClick = { viewModel.switchUser(user) },
                enabled = !isCurrent,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
              ) {
                Text(
                  text = "${user.username}\n(${user.role.name})",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  maxLines = 2,
                )
              }
            }
          }
        }
      }
    }

    // USER MANAGEMENT: CREATE HEADMAN ACCOUNT (ADMIN ONLY)
    if (isAdmin) {
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
              Icon(imageVector = Icons.Default.ManageAccounts, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Add New Headman Account", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }

            OutlinedTextField(
              value = newUsername,
              onValueChange = { newUsername = it },
              label = { Text("Headman Username (e.g. headman13)") },
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("new_user_username_input"),
            )

            OutlinedTextField(
              value = newPassword,
              onValueChange = { newPassword = it },
              label = { Text("Password") },
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("new_user_password_input"),
            )

            ExposedDropdownMenuBox(
              expanded = zoneMenuExpanded,
              onExpandedChange = { zoneMenuExpanded = !zoneMenuExpanded },
            ) {
              OutlinedTextField(
                value = newZone,
                onValueChange = {},
                readOnly = true,
                label = { Text("Assigned Zone") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = zoneMenuExpanded) },
                modifier = Modifier
                  .fillMaxWidth()
                  .menuAnchor(),
              )

              ExposedDropdownMenu(
                expanded = zoneMenuExpanded,
                onDismissRequest = { zoneMenuExpanded = false },
              ) {
                STANDARD_ZONES.forEach { z ->
                  DropdownMenuItem(
                    text = { Text(z) },
                    onClick = {
                      newZone = z
                      zoneMenuExpanded = false
                    },
                  )
                }
              }
            }

            Button(
              onClick = {
                viewModel.createHeadmanUser(newUsername, newPassword, newZone) { success ->
                  if (success) {
                    newUsername = ""
                    newPassword = ""
                  }
                }
              },
              modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("create_headman_button"),
              shape = RoundedCornerShape(10.dp),
            ) {
              Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("CREATE HEADMAN USER", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}
