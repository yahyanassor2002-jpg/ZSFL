package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Role
import com.example.data.repository.CloudSyncStatus
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.UserProfileAvatar
import com.example.ui.screens.*
import com.example.ui.theme.AppDarkBackgroundGradient
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  private val viewModel: MainViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        UpenjaApp(viewModel = viewModel)
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpenjaApp(viewModel: MainViewModel) {
  val context = LocalContext.current
  val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
  val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
  val userAvatarUri by viewModel.userAvatarUri.collectAsStateWithLifecycle()
  val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
  val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
  val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(toastMessage) {
    toastMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.clearToast()
    }
  }

  if (currentUser == null || currentScreen is Screen.Login) {
    Scaffold(
      modifier = Modifier
        .fillMaxSize()
        .background(AppDarkBackgroundGradient),
      containerColor = Color.Transparent,
      snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(AppDarkBackgroundGradient)
          .padding(innerPadding),
      ) {
        LoginScreen(viewModel = viewModel)
      }
    }
    return
  }

  val isDetailOrEdit = currentScreen is Screen.ReportDetail || currentScreen is Screen.NewOrEditReport || currentScreen is Screen.WeeklySummary

  Scaffold(
    modifier = Modifier
      .fillMaxSize()
      .background(AppDarkBackgroundGradient),
    containerColor = Color.Transparent,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      if (!isDetailOrEdit) {
        TopAppBar(
          title = {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth().padding(end = 4.dp),
            ) {
              Image(
                painter = painterResource(id = R.drawable.img_app_logo),
                contentDescription = "UPENJAnet Logo",
                modifier = Modifier
                  .size(28.dp)
                  .clip(CircleShape),
                contentScale = ContentScale.Crop,
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "UPENJAnet",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp,
                letterSpacing = 0.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Spacer(modifier = Modifier.width(8.dp))
              // User Role pill
              val isAdmin = currentUser?.role == Role.ADMIN
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .background(if (isAdmin) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer)
                  .padding(horizontal = 6.dp, vertical = 2.dp),
              ) {
                Text(
                  text = currentUser?.username ?: "",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (isAdmin) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          },
          actions = {
            // Real-time Cloud Sync Trigger Pill
            IconButton(
              onClick = { viewModel.syncNow() },
              enabled = !isSyncing,
              modifier = Modifier.testTag("top_app_bar_sync_button"),
            ) {
              if (isSyncing) {
                CircularProgressIndicator(
                  modifier = Modifier.size(20.dp),
                  color = MaterialTheme.colorScheme.onPrimary,
                  strokeWidth = 2.dp,
                )
              } else {
                Icon(
                  imageVector = when (syncStatus) {
                    CloudSyncStatus.SYNCED -> Icons.Default.CloudDone
                    CloudSyncStatus.SYNCING -> Icons.Default.Sync
                    CloudSyncStatus.PENDING_UPLOAD -> Icons.Default.CloudUpload
                    CloudSyncStatus.OFFLINE -> Icons.Default.CloudOff
                  },
                  contentDescription = "Sync Cloud",
                  tint = MaterialTheme.colorScheme.onPrimary,
                )
              }
            }
            UserProfileAvatar(
              avatarUri = userAvatarUri,
              username = currentUser?.username,
              size = 32.dp,
              onClick = { viewModel.navigateTo(Screen.Settings) },
              modifier = Modifier
                .padding(end = 8.dp)
                .testTag("top_bar_profile_avatar"),
            )
          },
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
          ),
        )
      }
    },
    bottomBar = {
      if (!isDetailOrEdit) {
        NavigationBar(
          containerColor = MaterialTheme.colorScheme.surface,
          tonalElevation = 6.dp,
        ) {
          NavigationBarItem(
            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
            label = { Text("Dashboard", fontSize = 10.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
            selected = currentScreen is Screen.Dashboard,
            onClick = { viewModel.navigateTo(Screen.Dashboard) },
            modifier = Modifier.testTag("nav_dashboard"),
          )
          NavigationBarItem(
            icon = { Icon(Icons.Default.Assessment, contentDescription = "Records") },
            label = { Text("Records", fontSize = 10.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
            selected = currentScreen is Screen.ReportsList,
            onClick = { viewModel.navigateTo(Screen.ReportsList) },
            modifier = Modifier.testTag("nav_records"),
          )
          NavigationBarItem(
            icon = { Icon(Icons.Default.Analytics, contentDescription = "Summary") },
            label = { Text("Summary", fontSize = 10.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
            selected = currentScreen is Screen.WeeklySummary,
            onClick = { viewModel.navigateTo(Screen.WeeklySummary) },
            modifier = Modifier.testTag("nav_weekly_summary"),
          )
          NavigationBarItem(
            icon = { Icon(Icons.Default.TrendingUp, contentDescription = "Progress") },
            label = { Text("Progress", fontSize = 10.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
            selected = currentScreen is Screen.ZoneProgress,
            onClick = { viewModel.navigateTo(Screen.ZoneProgress) },
            modifier = Modifier.testTag("nav_progress"),
          )
          NavigationBarItem(
            icon = { Icon(Icons.Default.PrecisionManufacturing, contentDescription = "Equipment") },
            label = { Text("Equipment", fontSize = 10.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
            selected = currentScreen is Screen.WeeklyEquipment,
            onClick = { viewModel.navigateTo(Screen.WeeklyEquipment) },
            modifier = Modifier.testTag("nav_equipment"),
          )
        }
      }
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(AppDarkBackgroundGradient)
        .padding(innerPadding),
    ) {
      when (currentScreen) {
        Screen.Login -> LoginScreen(viewModel = viewModel)
        Screen.Dashboard -> DashboardScreen(viewModel = viewModel)
        Screen.ReportsList -> ReportsListScreen(viewModel = viewModel)
        Screen.ReportDetail -> ReportDetailScreen(viewModel = viewModel)
        Screen.NewOrEditReport -> NewOrEditReportScreen(viewModel = viewModel)
        Screen.WeeklySummary -> WeeklySummaryScreen(viewModel = viewModel)
        Screen.Discussions -> DiscussionsScreen(viewModel = viewModel)
        Screen.ZoneProgress -> ZoneProgressScreen(viewModel = viewModel)
        Screen.WeeklyEquipment -> WeeklyEquipmentScreen(viewModel = viewModel)
        Screen.Zones -> ZonesScreen(viewModel = viewModel)
        Screen.SyncAndExport -> SyncAndExportScreen(viewModel = viewModel)
        Screen.Settings -> SettingsScreen(viewModel = viewModel)
      }
    }
  }
}

// Retain Greeting for screenshot & robolectric baseline tests
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  MyApplicationTheme { Greeting("UPENJAnet") }
}
