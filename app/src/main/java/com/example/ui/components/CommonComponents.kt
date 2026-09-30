package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ReportStatus
import com.example.ui.theme.StatusDraft
import com.example.ui.theme.StatusDraftBg
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusPendingBg
import com.example.ui.theme.StatusSubmitted
import com.example.ui.theme.StatusSubmittedBg
import com.example.ui.theme.StatusSynced
import com.example.ui.theme.StatusSyncedBg

@Composable
fun StatusBadge(status: ReportStatus, modifier: Modifier = Modifier) {
  val (bgColor, textColor, icon, label) = when (status) {
    ReportStatus.DRAFT -> Quad(StatusDraftBg, StatusDraft, Icons.Default.EditNote, "DRAFT")
    ReportStatus.SUBMITTED -> Quad(StatusSubmittedBg, StatusSubmitted, Icons.Default.CheckCircle, "SUBMITTED")
    ReportStatus.PENDING_SYNC -> Quad(StatusPendingBg, StatusPending, Icons.Default.CloudQueue, "PENDING SYNC")
    ReportStatus.SYNCED -> Quad(StatusSyncedBg, StatusSynced, Icons.Default.CloudDone, "SYNCED")
  }

  Row(
    modifier = modifier
      .clip(RoundedCornerShape(8.dp))
      .background(bgColor)
      .padding(horizontal = 8.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = textColor,
      modifier = Modifier.size(14.dp),
    )
    Spacer(modifier = Modifier.width(4.dp))
    Text(
      text = label,
      color = textColor,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
    )
  }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun DeleteConfirmationDialog(
  onDismiss: () -> Unit,
  onConfirm: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Box(
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.errorContainer),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Default.Warning,
          contentDescription = "Warning",
          tint = MaterialTheme.colorScheme.error,
        )
      }
    },
    title = {
      Text(
        text = "Delete Report",
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Text(
        text = "Are you sure you want to delete this report? This action cannot be undone.",
        fontSize = 14.sp,
      )
    },
    confirmButton = {
      TextButton(
        onClick = onConfirm,
        modifier = Modifier.testTag("confirm_delete_button"),
        colors = ButtonDefaults.textButtonColors(
          contentColor = MaterialTheme.colorScheme.error,
        ),
      ) {
        Text("DELETE", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      OutlinedButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("cancel_delete_button"),
      ) {
        Text("CANCEL")
      }
    },
  )
}

@Composable
fun UserProfileAvatar(
  avatarUri: String?,
  username: String?,
  modifier: Modifier = Modifier,
  size: androidx.compose.ui.unit.Dp = 48.dp,
  onClick: (() -> Unit)? = null,
  showEditBadge: Boolean = false,
) {
  Box(
    modifier = modifier
      .size(size)
      .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
    contentAlignment = Alignment.Center,
  ) {
    if (!avatarUri.isNullOrBlank()) {
      coil.compose.AsyncImage(
        model = avatarUri,
        contentDescription = "Profile picture of ${username ?: "user"}",
        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
        modifier = Modifier
          .size(size)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primaryContainer),
      )
    } else {
      androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.img_profile_avatar),
        contentDescription = "Profile picture of ${username ?: "user"}",
        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
        modifier = Modifier
          .size(size)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primaryContainer),
      )
    }

    if (showEditBadge) {
      Box(
        modifier = Modifier
          .size(size * 0.35f)
          .align(Alignment.BottomEnd)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primary)
          .padding(2.dp),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = androidx.compose.material.icons.Icons.Default.EditNote,
          contentDescription = "Change profile picture",
          tint = MaterialTheme.colorScheme.onPrimary,
          modifier = Modifier.size(size * 0.22f),
        )
      }
    }
  }
}

