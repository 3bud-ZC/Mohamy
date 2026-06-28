package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MohamyDimens
import com.example.ui.theme.MohamyDanger
import com.example.ui.theme.MohamyGold
import com.example.ui.theme.MohamySuccess

enum class MohamyBadgeTone {
  Gold,
  Success,
  Danger,
  Neutral,
}

@Composable
fun MohamyStatusBadge(
  text: String,
  modifier: Modifier = Modifier,
  tone: MohamyBadgeTone = MohamyBadgeTone.Gold,
) {
  val background = when (tone) {
    MohamyBadgeTone.Gold -> MohamyGold.copy(alpha = 0.16f)
    MohamyBadgeTone.Success -> MohamySuccess.copy(alpha = 0.18f)
    MohamyBadgeTone.Danger -> MohamyDanger.copy(alpha = 0.18f)
    MohamyBadgeTone.Neutral -> MaterialTheme.colorScheme.surfaceVariant
  }
  val border = when (tone) {
    MohamyBadgeTone.Gold -> MohamyGold.copy(alpha = 0.4f)
    MohamyBadgeTone.Success -> MohamySuccess.copy(alpha = 0.45f)
    MohamyBadgeTone.Danger -> MohamyDanger.copy(alpha = 0.45f)
    MohamyBadgeTone.Neutral -> MaterialTheme.colorScheme.outlineVariant
  }
  val textColor = when (tone) {
    MohamyBadgeTone.Gold -> MohamyGold
    MohamyBadgeTone.Success -> MohamySuccess
    MohamyBadgeTone.Danger -> MohamyDanger
    MohamyBadgeTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
  }

  Box(
    modifier =
      modifier
        .background(background, RoundedCornerShape(MohamyDimens.badgeRadius))
        .border(1.dp, border, RoundedCornerShape(MohamyDimens.badgeRadius))
        .padding(horizontal = 10.dp, vertical = 5.dp)
  ) {
    Text(text = text, color = textColor, fontWeight = FontWeight.Bold)
  }
}
