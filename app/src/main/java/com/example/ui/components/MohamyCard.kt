package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MohamyDimens
import com.example.ui.theme.MyApplicationTheme

@Composable
fun MohamyCard(
  modifier: Modifier = Modifier,
  title: String? = null,
  subtitle: String? = null,
  contentPadding: PaddingValues = PaddingValues(20.dp),
  content: @Composable ColumnScope.() -> Unit,
) {
  val dark = isSystemInDarkTheme()
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(MohamyDimens.cardRadius),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (dark) 0.82f else 0.95f)),
    elevation = CardDefaults.cardElevation(defaultElevation = if (dark) 6.dp else 2.dp)
  ) {
    Column(modifier = Modifier.padding(contentPadding)) {
      if (title != null) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          fontWeight = FontWeight.ExtraBold
        )
      }
      if (subtitle != null) {
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 8.dp, bottom = 14.dp)
        )
      }
      content()
    }
  }
}

@Preview(showBackground = true, locale = "ar")
@Composable
private fun MohamyCardPreview() {
  MyApplicationTheme(darkTheme = true) {
    MohamyCard(title = "بطاقة محامي فون", subtitle = "واجهة احترافية لإدارة المكتب") {
      Text("محتوى تجريبي", color = MaterialTheme.colorScheme.onSurface)
    }
  }
}
