package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MohamyCharcoal
import com.example.ui.theme.MohamyDimens
import com.example.ui.theme.MohamyGold
import com.example.ui.theme.MohamyGoldBright

data class MohamyTopBarAction(
  val icon: ImageVector,
  val contentDescription: String,
  val onClick: () -> Unit,
)

@Composable
fun MohamyTopBar(
  title: String,
  subtitle: String? = null,
  modifier: Modifier = Modifier,
  showBackButton: Boolean = false,
  onBackClick: (() -> Unit)? = null,
  actions: List<MohamyTopBarAction> = emptyList(),
) {
  val dark = isSystemInDarkTheme()
  val backgroundBrush =
    if (dark) {
      Brush.verticalGradient(colors = listOf(MohamyCharcoal, MaterialTheme.colorScheme.surface))
    } else {
      Brush.verticalGradient(
        colors = listOf(
          MaterialTheme.colorScheme.surface,
          MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
      )
    }
  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .padding(horizontal = MohamyDimens.screenHorizontal, vertical = 10.dp)
        .clip(RoundedCornerShape(28.dp))
        .background(backgroundBrush)
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (dark) 0.72f else 0.9f), RoundedCornerShape(28.dp))
        .padding(horizontal = 16.dp, vertical = 14.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      if (showBackButton && onBackClick != null) {
        IconButton(
          onClick = onBackClick,
          modifier = Modifier.background(MohamyGold.copy(alpha = 0.14f), CircleShape)
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "رجوع",
            tint = MohamyGold
          )
        }
      }

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onSurface,
          fontWeight = FontWeight.ExtraBold
        )
        if (subtitle != null) {
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      actions.forEach { action ->
        IconButton(
          onClick = action.onClick,
          modifier =
            Modifier
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.76f), CircleShape)
              .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f), CircleShape)
        ) {
          Icon(
            imageVector = action.icon,
            contentDescription = action.contentDescription,
            tint = if (action == actions.firstOrNull()) MohamyGoldBright else MaterialTheme.colorScheme.onSurface
          )
        }
      }
    }
  }
}
