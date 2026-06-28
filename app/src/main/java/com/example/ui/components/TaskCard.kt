package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LegalTask
import com.example.ui.theme.MohamyGold
import com.example.ui.theme.MyApplicationTheme

private fun taskPriorityTone(priority: String): MohamyBadgeTone =
  when (priority.trim()) {
    "عاجل", "عالية" -> MohamyBadgeTone.Danger
    "متوسطة", "متوسط" -> MohamyBadgeTone.Gold
    else -> MohamyBadgeTone.Neutral
  }

private fun taskStatusTone(status: String, isOverdue: Boolean): MohamyBadgeTone =
  when {
    isOverdue -> MohamyBadgeTone.Danger
    status.trim() == "منتهية" -> MohamyBadgeTone.Success
    status.trim() == "متأخرة" -> MohamyBadgeTone.Danger
    else -> MohamyBadgeTone.Gold
  }

@Composable
private fun TaskMetaLine(
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
fun TaskCard(
  task: LegalTask,
  isOverdue: Boolean,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
  footer: (@Composable RowScope.() -> Unit)? = null,
) {
  val completed = task.status == "منتهية"

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
            imageVector = Icons.Default.TaskAlt,
            contentDescription = null,
            tint = MohamyGold,
            modifier = Modifier.size(24.dp)
          )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = task.title.ifBlank { "مهمة بدون عنوان" },
            style = MaterialTheme.typography.titleMedium,
            color = if (completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            textDecoration = if (completed) TextDecoration.LineThrough else null
          )
          if (task.description.isNotBlank()) {
            Text(
              text = task.description,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
        Column(
          horizontalAlignment = Alignment.End,
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          MohamyStatusBadge(
            text = if (isOverdue && !completed) "متأخرة" else task.status.ifBlank { "غير محدد" },
            tone = taskStatusTone(task.status, isOverdue && !completed)
          )
          MohamyStatusBadge(text = task.priority.ifBlank { "بدون أولوية" }, tone = taskPriorityTone(task.priority))
        }
      }

      if (task.dueDate.isNotBlank()) {
        TaskMetaLine(
          icon = Icons.Default.CalendarToday,
          text =
            if (isOverdue && !completed) {
              "الاستحقاق: ${task.dueDate} - متأخرة"
            } else {
              "الاستحقاق: ${task.dueDate}"
            }
        )
      }

      if (!task.caseTitle.isNullOrBlank()) {
        TaskMetaLine(icon = Icons.Default.WorkspacePremium, text = "القضية: ${task.caseTitle}")
      }

      if (!task.clientName.isNullOrBlank()) {
        TaskMetaLine(icon = Icons.Default.Person, text = "الموكل: ${task.clientName}")
      }

      if (!completed && isOverdue) {
        TaskMetaLine(icon = Icons.Default.Flag, text = "تحتاج متابعة عاجلة أو إعادة جدولة")
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
private fun TaskCardPreview() {
  MyApplicationTheme(darkTheme = true) {
    TaskCard(
      task =
        LegalTask(
          id = 4,
          title = "مراجعة المذكرة النهائية",
          description = "استكمال المستندات وإرسال النسخة الأخيرة قبل الجلسة.",
          dueDate = "2026-07-01",
          priority = "عاجل",
          status = "مفتوحة",
          caseTitle = "دعوى نفقة",
          clientName = "هدى السيد"
        ),
      isOverdue = true,
      modifier = Modifier.padding(16.dp),
      onClick = {}
    )
  }
}
