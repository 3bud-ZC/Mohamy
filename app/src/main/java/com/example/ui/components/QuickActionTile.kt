package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MohamyGold
import com.example.ui.theme.MyApplicationTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add

@Composable
fun QuickActionTile(
  title: String,
  icon: ImageVector,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  onClick: () -> Unit,
) {
  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .height(108.dp)
        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(22.dp))
        .border(
          1.dp,
          MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.9f),
          RoundedCornerShape(22.dp)
        )
        .clickable(onClick = onClick)
        .padding(16.dp)
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Box(
        modifier =
          Modifier
            .size(38.dp)
            .background(MohamyGold.copy(alpha = 0.14f), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
      ) {
        Icon(imageVector = icon, contentDescription = null, tint = MohamyGold)
      }
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold
      )
      if (subtitle != null) {
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@Preview(showBackground = true, locale = "ar")
@Composable
private fun QuickActionTilePreview() {
  MyApplicationTheme(darkTheme = true) {
    QuickActionTile(
      title = "إضافة جلسة",
      subtitle = "إلحاق جلسة جديدة بملف القضية",
      icon = Icons.Default.Add,
      modifier = Modifier.padding(16.dp),
      onClick = {}
    )
  }
}
