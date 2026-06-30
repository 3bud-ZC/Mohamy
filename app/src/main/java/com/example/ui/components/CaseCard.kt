package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LegalCase
import com.example.ui.theme.MohamyDimens
import com.example.ui.theme.MohamyGold
import com.example.ui.theme.MyApplicationTheme

private fun casePriorityTone(priority: String): MohamyBadgeTone =
  when (priority.trim()) {
    "عالية" -> MohamyBadgeTone.Danger
    "متوسطة" -> MohamyBadgeTone.Gold
    else -> MohamyBadgeTone.Neutral
  }

private fun caseStatusTone(status: String): MohamyBadgeTone =
  when (status.trim()) {
    "منتهية", "مغلقة", "محفوظة" -> MohamyBadgeTone.Neutral
    "مؤجلة", "متأخرة" -> MohamyBadgeTone.Danger
    "نشطة", "متداولة", "جديدة" -> MohamyBadgeTone.Success
    else -> MohamyBadgeTone.Gold
  }

@Composable
private fun CaseMetaLine(
  icon: @Composable () -> Unit,
  text: String,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    icon()
    Text(
      text = text,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      style = MaterialTheme.typography.bodySmall
    )
  }
}

@Composable
fun CaseCard(
  legalCase: LegalCase,
  readinessScore: Int,
  readinessLabel: String,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  Card(
    modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
    shape = RoundedCornerShape(MohamyDimens.cardRadius),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border = androidx.compose.foundation.BorderStroke(
      1.dp,
      MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    )
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
          modifier =
            Modifier
              .size(48.dp)
              .background(MohamyGold.copy(alpha = 0.12f), CircleShape)
              .border(1.dp, MohamyGold.copy(alpha = 0.28f), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Gavel,
            contentDescription = null,
            tint = MohamyGold,
            modifier = Modifier.size(22.dp)
          )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = legalCase.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "رقم ${legalCase.caseNumber} لسنة ${legalCase.caseYear}",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp
          )
        }
        Column(
          horizontalAlignment = Alignment.End,
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          MohamyStatusBadge(text = legalCase.status, tone = caseStatusTone(legalCase.status))
          MohamyStatusBadge(text = legalCase.priority, tone = casePriorityTone(legalCase.priority))
        }
      }

      CaseMetaLine(
        icon = {
          Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(15.dp)
          )
        },
        text = "الموكل: ${legalCase.clientName.ifBlank { "غير محدد" }}"
      )

      if (legalCase.courtName.isNotBlank()) {
        CaseMetaLine(
          icon = {
            Icon(
              imageVector = Icons.Default.AccountBalance,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(15.dp)
            )
          },
          text = "المحكمة: ${legalCase.courtName}"
        )
      }

      if (legalCase.nextSessionDate.isNotBlank()) {
        CaseMetaLine(
          icon = {
            Icon(
              imageVector = Icons.Default.CalendarToday,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(15.dp)
            )
          },
          text = "الجلسة القادمة: ${legalCase.nextSessionDate}"
        )
      }

      Text(
        text = "جاهزية الملف: $readinessScore% - $readinessLabel",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
        lineHeight = 18.sp
      )
    }
  }
}

@Preview(showBackground = true, locale = "ar")
@Composable
private fun CaseCardPreview() {
  MyApplicationTheme(darkTheme = true) {
    CaseCard(
      legalCase =
        LegalCase(
          id = 1,
          title = "دعوى تعويض مدني",
          caseNumber = "125",
          caseYear = "2025",
          caseType = "مدني",
          clientId = 1,
          clientName = "أحمد سالم",
          courtName = "محكمة شمال القاهرة",
          nextSessionDate = "2026-07-10",
          status = "نشطة",
          priority = "عالية"
        ),
      readinessScore = 78,
      readinessLabel = "جاهزة للمرافعة",
      modifier = Modifier.padding(16.dp),
      onClick = {}
    )
  }
}
