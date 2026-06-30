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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
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
import com.example.data.CaseFile
import com.example.ui.formatFileSize
import com.example.ui.theme.MohamyGold
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun fileExtractionTone(status: String): MohamyBadgeTone {
  val normalized = status.trim()
  return when {
    normalized.contains("OCR") || normalized.contains("جاهز") || normalized.contains("نجاح") -> MohamyBadgeTone.Success
    normalized.contains("غير مكتمل") || normalized.contains("فشل") -> MohamyBadgeTone.Danger
    normalized.isBlank() -> MohamyBadgeTone.Neutral
    else -> MohamyBadgeTone.Gold
  }
}

private fun extractionLabel(status: String): String =
  when {
    status.trim().contains("نجاح") -> "مفهرس"
    status.trim().contains("جاهز") -> "مفهرس"
    status.trim().contains("OCR") -> "OCR"
    status.trim().contains("فشل") -> "فشل الفهرسة"
    status.isBlank() -> "محفوظ"
    else -> status
  }

private fun fileIcon(fileName: String): androidx.compose.ui.graphics.vector.ImageVector {
  val lower = fileName.lowercase()
  return when {
    lower.endsWith(".pdf") -> Icons.Default.PictureAsPdf
    lower.endsWith(".doc") || lower.endsWith(".docx") -> Icons.Default.Description
    lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp") -> Icons.Default.Image
    else -> Icons.Default.FolderCopy
  }
}

@Composable
private fun FileMetaLine(
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
fun FileDocumentCard(
  file: CaseFile,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
  footer: (@Composable RowScope.() -> Unit)? = null,
) {
  val uploadLabel =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH).format(Date(file.uploadDate))

  MohamyCard(modifier = modifier.clickable(onClick = onClick)) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
      ) {
        androidx.compose.foundation.layout.Box(
          modifier =
            Modifier
              .size(48.dp)
              .background(MohamyGold.copy(alpha = 0.12f), CircleShape)
              .border(1.dp, MohamyGold.copy(alpha = 0.28f), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = fileIcon(file.fileName),
            contentDescription = null,
            tint = MohamyGold,
            modifier = Modifier.size(22.dp)
          )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
          Text(
            text = file.fileName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = file.caseTitle.ifBlank { "قضية غير محددة" },
            color = MaterialTheme.colorScheme.primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        Column(
          horizontalAlignment = Alignment.End,
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          MohamyStatusBadge(text = file.docType.ifBlank { "مستند" }, tone = MohamyBadgeTone.Gold)
          MohamyStatusBadge(
            text = extractionLabel(file.extractionStatus),
            tone = fileExtractionTone(file.extractionStatus)
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (file.clientName.isNotBlank()) {
          FileMetaLine(icon = Icons.Default.Person, text = file.clientName, modifier = Modifier.weight(1f))
        }
        FileMetaLine(icon = Icons.Default.Storage, text = formatFileSize(file.fileLength))
      }

      FileMetaLine(icon = Icons.Default.CalendarToday, text = "تاريخ الرفع: $uploadLabel")

      if (file.extractedText.isNotBlank()) {
        Text(
          text = file.extractedText,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          lineHeight = 18.sp
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
private fun FileDocumentCardPreview() {
  MyApplicationTheme(darkTheme = true) {
    FileDocumentCard(
      file =
        CaseFile(
          id = 2,
          caseId = 9,
          caseTitle = "دعوى إيجار",
          clientId = 3,
          clientName = "محمد رجب",
          fileName = "hukm-final.pdf",
          filePath = "C:/docs/hukm-final.pdf",
          docType = "حكم",
          fileLength = 245760,
          extractionStatus = "جاهز للبحث النصي",
          extractedText = "حكمت المحكمة بإلزام المدعى عليه بالمصروفات...",
        ),
      modifier = Modifier.padding(16.dp),
      onClick = {}
    )
  }
}
