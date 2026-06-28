package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.data.CaseSession
import com.example.ui.theme.MohamyGold
import com.example.ui.theme.MyApplicationTheme

@Composable
fun CaseTimelineItem(
  session: CaseSession,
  modifier: Modifier = Modifier,
  trailing: (@Composable () -> Unit)? = null,
) {
  androidx.compose.material3.Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(22.dp),
    colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f))
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.Top
    ) {
      Box(
        modifier =
          Modifier
            .size(42.dp)
            .background(MohamyGold.copy(alpha = 0.12f), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Gavel,
          contentDescription = null,
          tint = MohamyGold,
          modifier = Modifier.size(20.dp)
        )
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          text = session.title,
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = buildString {
            append(session.date)
            if (session.time.isNotBlank()) {
              append(" - ")
              append(session.time)
            }
          },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.SemiBold
        )
        if (session.requirements.isNotBlank()) {
          Text(
            text = "المطلوب: ${session.requirements}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        if (session.result.isNotBlank()) {
          Text(
            text = "النتيجة: ${session.result}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }
      if (trailing != null) {
        trailing()
      } else {
        Spacer(modifier = Modifier)
      }
    }
  }
}

@Preview(showBackground = true, locale = "ar")
@Composable
private fun CaseTimelineItemPreview() {
  MyApplicationTheme(darkTheme = true) {
    CaseTimelineItem(
      session =
        CaseSession(
          id = 1,
          caseId = 1,
          caseTitle = "دعوى مدنية",
          clientId = 1,
          clientName = "أحمد",
          title = "جلسة مرافعة أولى",
          date = "2026-07-10",
          time = "10:30",
          requirements = "مذكرة دفاع ومستندات أصلية",
          result = "تأجيل للاطلاع"
        ),
      modifier = Modifier.padding(16.dp)
    )
  }
}
