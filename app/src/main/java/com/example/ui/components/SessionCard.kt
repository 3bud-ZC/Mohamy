package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CaseSession
import com.example.ui.theme.MohamyGold
import com.example.ui.theme.MyApplicationTheme

private fun sessionBadgeTone(status: String): MohamyBadgeTone =
  when (status.trim()) {
    "منتهية" -> MohamyBadgeTone.Success
    "ملغاة", "غيابية", "مؤجلة" -> MohamyBadgeTone.Danger
    "قادمة" -> MohamyBadgeTone.Gold
    else -> MohamyBadgeTone.Neutral
  }

@Composable
private fun SessionMetaLine(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  text: String,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(16.dp)
    )
    Text(
      text = text,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@Composable
fun SessionCard(
  session: CaseSession,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
  footer: (@Composable RowScope.() -> Unit)? = null,
) {
  MohamyCard(modifier = modifier.clickable(onClick = onClick)) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
      ) {
        androidx.compose.foundation.layout.Box(
          modifier =
            Modifier
              .size(52.dp)
              .background(MohamyGold.copy(alpha = 0.12f), CircleShape)
              .border(1.dp, MohamyGold.copy(alpha = 0.36f), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Gavel,
            contentDescription = null,
            tint = MohamyGold,
            modifier = Modifier.size(24.dp)
          )
        }
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text(
            text = session.title.ifBlank { "جلسة بدون عنوان" },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = session.type.ifBlank { "جلسة قانونية" },
            color = MaterialTheme.colorScheme.primary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
          )
          Text(
            text = session.caseTitle.ifBlank { "قضية غير محددة" },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        MohamyStatusBadge(text = session.status.ifBlank { "غير محدد" }, tone = sessionBadgeTone(session.status))
      }

      if (session.clientName.isNotBlank()) {
        SessionMetaLine(icon = Icons.Default.Person, text = "الموكل: ${session.clientName}")
      }

      val courtLabel =
        listOf(session.court.ifBlank { "" }, session.courtCircle.ifBlank { "" })
          .filter { it.isNotBlank() }
          .joinToString(" - ")
      if (courtLabel.isNotBlank()) {
        SessionMetaLine(icon = Icons.Default.AccountBalance, text = courtLabel)
      }

      SessionMetaLine(
        icon = Icons.Default.CalendarToday,
        text =
          buildString {
            append(session.date.ifBlank { "بدون تاريخ" })
            if (session.time.isNotBlank()) {
              append(" - ")
              append(session.time)
            }
          }
      )

      if (session.requirements.isNotBlank()) {
        SessionMetaLine(
          icon = Icons.Default.Description,
          text = "المطلوب: ${session.requirements}",
        )
      } else if (session.result.isNotBlank()) {
        SessionMetaLine(
          icon = Icons.Default.Description,
          text = "النتيجة: ${session.result}",
        )
      }

      footer?.let {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
          content = it
        )
      }
    }
  }
}

@Preview(showBackground = true, locale = "ar")
@Composable
private fun SessionCardPreview() {
  MyApplicationTheme(darkTheme = true) {
    SessionCard(
      session =
        CaseSession(
          id = 1,
          caseId = 5,
          caseTitle = "دعوى صحة توقيع",
          clientId = 2,
          clientName = "سعيد مصطفى",
          title = "جلسة مرافعة",
          court = "محكمة جنوب الجيزة",
          courtCircle = "الدائرة الثالثة",
          date = "2026-07-05",
          time = "10:30",
          requirements = "أصل التوكيل والمذكرة الختامية",
          status = "قادمة"
        ),
      modifier = Modifier.padding(16.dp),
      onClick = {}
    )
  }
}
