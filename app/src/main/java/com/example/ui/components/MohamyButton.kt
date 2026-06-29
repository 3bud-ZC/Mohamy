package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
      }
      Text(text = text, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelLarge)
    }
  }

  when (style) {
    MohamyButtonStyle.Primary -> {
      Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 1.dp)
      ) { rowContent() }
    }

    MohamyButtonStyle.Secondary -> {
      Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
      ) { rowContent() }
    }

    MohamyButtonStyle.Ghost -> {
      OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        enabled = enabled,
        shape = shape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        colors = ButtonDefaults.outlinedButtonColors(
          containerColor = MaterialTheme.colorScheme.surface,
          contentColor = MaterialTheme.colorScheme.onSurface
        )
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
