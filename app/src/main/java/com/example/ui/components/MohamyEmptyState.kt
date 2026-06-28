package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MohamyGold
import com.example.ui.theme.MyApplicationTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gavel

@Composable
fun MohamyEmptyState(
  icon: ImageVector,
  title: String,
  message: String,
  actionText: String? = null,
  onActionClick: (() -> Unit)? = null,
  secondaryActionText: String? = null,
  onSecondaryActionClick: (() -> Unit)? = null,
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Box(
      modifier = Modifier.size(72.dp).background(MohamyGold.copy(alpha = 0.15f), CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MohamyGold,
        modifier = Modifier.size(34.dp)
      )
    }
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onSurface,
      fontWeight = FontWeight.Bold,
      textAlign = TextAlign.Center
    )
    Text(
      text = message,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center
    )
    if ((actionText != null && onActionClick != null) || (secondaryActionText != null && onSecondaryActionClick != null)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        if (actionText != null && onActionClick != null) {
          MohamyButton(
            text = actionText,
            onClick = onActionClick,
            modifier = Modifier.weight(1f)
          )
        }
        if (secondaryActionText != null && onSecondaryActionClick != null) {
          MohamyButton(
            text = secondaryActionText,
            onClick = onSecondaryActionClick,
            modifier = Modifier.weight(1f),
            style = MohamyButtonStyle.Ghost
          )
        }
      }
    }
  }
}

@Preview(showBackground = true, locale = "ar")
@Composable
private fun MohamyEmptyStatePreview() {
  MyApplicationTheme(darkTheme = true) {
    MohamyEmptyState(
      icon = Icons.Default.Gavel,
      title = "لا توجد قضايا بعد",
      message = "ابدأ بإضافة أول قضية داخل المكتب الرقمي.",
      actionText = "إضافة قضية",
      onActionClick = {}
    )
  }
}
