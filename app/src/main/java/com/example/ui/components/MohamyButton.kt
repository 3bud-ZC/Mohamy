package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MohamyDimens
import com.example.ui.theme.MyApplicationTheme

enum class MohamyButtonStyle {
  Primary,
  Secondary,
  Ghost,
}

@Composable
fun MohamyButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  icon: ImageVector? = null,
  style: MohamyButtonStyle = MohamyButtonStyle.Primary,
) {
  val shape = RoundedCornerShape(MohamyDimens.buttonRadius)
  val rowContent: @Composable () -> Unit = {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      if (icon != null) {
        Icon(imageVector = icon, contentDescription = null)
      }
      Text(text = text, fontWeight = FontWeight.Bold)
    }
  }

  when (style) {
    MohamyButtonStyle.Primary -> {
      Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
        )
      ) { rowContent() }
    }

    MohamyButtonStyle.Secondary -> {
      Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
          contentColor = MaterialTheme.colorScheme.primary
        )
      ) { rowContent() }
    }

    MohamyButtonStyle.Ghost -> {
      OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        enabled = enabled,
        shape = shape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
      ) { rowContent() }
    }
  }
}

@Preview(showBackground = true, locale = "ar")
@Composable
private fun MohamyButtonPreview() {
  MyApplicationTheme(darkTheme = true) {
    MohamyButton(
      text = "تفعيل التطبيق",
      onClick = {},
      modifier = Modifier.padding(16.dp)
    )
  }
}
