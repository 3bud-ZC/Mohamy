package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.data.AppViewModel
import com.example.data.CaseFile
import com.example.data.CaseSession
import com.example.data.Client
import com.example.data.LegalCase
import com.example.data.LegalTask
import com.example.data.Screen
import com.example.ui.components.MohamyBadgeTone
import com.example.ui.components.MohamyButton
import com.example.ui.components.MohamyButtonStyle
import com.example.ui.components.MohamyCard
import com.example.ui.components.MohamyEmptyState
import com.example.ui.components.MohamyStatusBadge
import com.example.ui.components.QuickActionTile
import com.example.ui.theme.MohamyDanger
import com.example.ui.theme.MohamyDimens
import com.example.ui.theme.MohamyGold
import com.example.ui.theme.MohamyGoldBright
import com.example.ui.theme.MohamyGoldStrong
import com.example.ui.theme.MohamyInfo
import com.example.ui.theme.MohamyLightHero
import com.example.ui.theme.MohamyLightHeroEnd
import com.example.ui.theme.MohamySuccess
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.legalScreenBackground
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class DashboardStat(
  val title: String,
  val value: String,
  val icon: ImageVector,
  val accent: Color,
)

private data class QuickAction(
  val title: String,
  val subtitle: String,
  val icon: ImageVector,
  val accent: Color,
  val onClick: () -> Unit,
)

@Composable
fun DashboardScreen(
  viewModel: AppViewModel,
  clients: List<Client>,
  cases: List<LegalCase>,
  sessions: List<CaseSession>,
  tasks: List<LegalTask>,
  files: List<CaseFile>,
) {
  val license by viewModel.licenseState.collectAsState(initial = null)
  var showDemoSeedDialog by remember { mutableStateOf(false) }
  val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
  val lawyerName = license?.lawyerName?.takeIf { it.isNotBlank() } ?: "الأستاذ المحامي"
  val todaySessions = sessions.filter { it.date.trim() == todayDateStr && it.status != "ملغاة" }
  val isWorkspaceEmpty =
    clients.isEmpty() && cases.isEmpty() && sessions.isEmpty() && tasks.isEmpty() && files.isEmpty()
  val urgentTasks =
    tasks.filter { task ->
      task.status != "منتهية" &&
        task.status != "مكتملة" &&
        task.dueDate.isNotBlank() &&
        task.dueDate <= todayDateStr
    }.take(4)

  val stats =
    listOf(
      DashboardStat("قضايا نشطة", cases.size.toString(), Icons.Default.WorkOutline, MohamyGold),
      DashboardStat("جلسات اليوم", todaySessions.size.toString(), Icons.Default.Event, MohamyInfo),
      DashboardStat("مهام عاجلة", urgentTasks.size.toString(), Icons.Default.TaskAlt, MohamyDanger),
      DashboardStat("ملفات محلية", files.size.toString(), Icons.Default.FolderCopy, MohamySuccess),
    )

  val quickActions =
    listOf(
      QuickAction("القضايا", "عرض الملفات النشطة والأرشيف", Icons.Default.WorkOutline, MohamyGold) {
        viewModel.navigateTo(Screen.CasesList)
      },
      QuickAction("العملاء", "إدارة الموكلين وسجل التواصل", Icons.Default.Groups, MohamyInfo) {
        viewModel.navigateTo(Screen.ClientsList)
      },
      QuickAction("الجلسات", "تنظيم المرافعات والمواعيد", Icons.Default.CalendarMonth, MohamySuccess) {
        viewModel.navigateTo(Screen.SessionsList)
      },
      QuickAction("المهام", "متابعة الأعمال والتنبيهات", Icons.Default.TaskAlt, MohamyDanger) {
        viewModel.navigateTo(Screen.TasksList)
      },
      QuickAction("المستندات", "فتح الأرشيف القانوني المحلي", Icons.AutoMirrored.Filled.InsertDriveFile, MohamyGoldBright) {
        viewModel.navigateTo(Screen.FilesLibrary)
      },
      QuickAction("الخصوصية المحلية", "مراجعة الأمان والإعدادات", Icons.Default.PrivacyTip, MohamyInfo) {
        viewModel.navigateTo(Screen.Settings)
      },
    )

  val demoAction: () -> Unit = {
    when {
      viewModel.hasDemoSeededForCurrentWorkspace -> viewModel.navigateTo(Screen.CasesList)
      isWorkspaceEmpty -> viewModel.seedDemoWorkspace()
      else -> showDemoSeedDialog = true
    }
  }

  if (showDemoSeedDialog) {
    AlertDialog(
      onDismissRequest = { showDemoSeedDialog = false },
      title = { Text("إضافة بيانات تجريبية") },
      text = {
        Text(
          "سيتم إدراج بيانات عرض محلية ووهمية داخل هذه المساحة دون حذف بياناتك الحالية. الهدف هو تقديم واجهة التطبيق للمحامين فقط.",
          textAlign = TextAlign.Start
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showDemoSeedDialog = false
            viewModel.seedDemoWorkspace(forceAppend = true)
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
          Text("إضافة البيانات")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDemoSeedDialog = false }) {
          Text("إلغاء")
        }
      }
    )
  }

  DashboardShell(
    lawyerName = lawyerName,
    stats = stats,
    todaySessions = todaySessions.take(4),
    urgentTasks = urgentTasks,
    recentFiles = files.takeLast(3).reversed(),
    quickActions = quickActions,
    showOnboardingCtas = isWorkspaceEmpty || !viewModel.isOnboardingCompleted,
    isWorkspaceEmpty = isWorkspaceEmpty,
    hasDemoSeeded = viewModel.hasDemoSeededForCurrentWorkspace,
    onStartUsing = {
      viewModel.completeOnboarding()
      if (isWorkspaceEmpty) {
        viewModel.navigateTo(Screen.ClientAddEdit())
      } else {
        viewModel.navigateTo(Screen.CasesList)
      }
    },
    onDemoAction = demoAction,
    onSettingsAction = { viewModel.navigateTo(Screen.Settings) },
    onCasesAction = { viewModel.navigateTo(Screen.CasesList) },
    onTasksAction = { viewModel.navigateTo(Screen.TasksList) },
    onFilesAction = { viewModel.navigateTo(Screen.FilesLibrary) },
    onSessionClick = { session -> viewModel.navigateTo(Screen.SessionAddEdit(sessionId = session.id)) },
    onTaskClick = { task ->
      viewModel.navigateTo(Screen.TaskAddEdit(taskId = task.id, presetCaseId = task.caseId))
    },
    onFileClick = { file -> viewModel.navigateTo(Screen.CaseDetails(file.caseId)) }
  )
}

@Composable
private fun DashboardShell(
  lawyerName: String,
  stats: List<DashboardStat>,
  todaySessions: List<CaseSession>,
  urgentTasks: List<LegalTask>,
  recentFiles: List<CaseFile>,
  quickActions: List<QuickAction>,
  showOnboardingCtas: Boolean,
  isWorkspaceEmpty: Boolean,
  hasDemoSeeded: Boolean,
  onStartUsing: () -> Unit,
  onDemoAction: () -> Unit,
  onSettingsAction: () -> Unit,
  onCasesAction: () -> Unit,
  onTasksAction: () -> Unit,
  onFilesAction: () -> Unit,
  onSessionClick: (CaseSession) -> Unit,
  onTaskClick: (LegalTask) -> Unit,
  onFileClick: (CaseFile) -> Unit,
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize().legalScreenBackground(),
    contentPadding =
      androidx.compose.foundation.layout.PaddingValues(
        horizontal = MohamyDimens.screenHorizontal,
        vertical = MohamyDimens.screenVertical
      ),
    verticalArrangement = Arrangement.spacedBy(MohamyDimens.sectionGap)
  ) {
    item {
      DashboardHeroCard(
        lawyerName = lawyerName,
        showOnboardingCtas = showOnboardingCtas,
        hasDemoSeeded = hasDemoSeeded,
        onPrimaryClick = onStartUsing,
        onSecondaryClick = if (showOnboardingCtas) onDemoAction else onSettingsAction
      )
    }

    item {
      DashboardSectionHeader(
        title = "نظرة سريعة",
        subtitle = "ملخص اليوم داخل مكتبك القانوني المحلي"
      )
    }

    item {
      Column(verticalArrangement = Arrangement.spacedBy(MohamyDimens.itemGap)) {
        stats.chunked(2).forEach { rowItems ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MohamyDimens.itemGap)
          ) {
            rowItems.forEach { stat ->
              DashboardStatCard(stat = stat, modifier = Modifier.weight(1f))
            }
            if (rowItems.size == 1) {
              Spacer(modifier = Modifier.weight(1f))
            }
          }
        }
      }
    }

    item {
      DashboardSectionHeader(
        title = "إجراءات سريعة",
        subtitle = "وصول مباشر إلى أهم مهام العمل اليومية"
      )
    }

    item {
      Column(verticalArrangement = Arrangement.spacedBy(MohamyDimens.itemGap)) {
        quickActions.chunked(2).forEach { rowItems ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MohamyDimens.itemGap)
          ) {
            rowItems.forEach { action ->
              QuickActionTile(
                title = action.title,
                subtitle = action.subtitle,
                icon = action.icon,
                accent = action.accent,
                modifier = Modifier.weight(1f),
                onClick = action.onClick
              )
            }
          }
        }
      }
    }

    item {
      DashboardSectionHeader(
        title = "جلسات اليوم",
        subtitle = "ما يحتاج انتباهك داخل التقويم القانوني"
      )
    }

    item {
      if (todaySessions.isEmpty()) {
        MohamyCard {
          MohamyEmptyState(
            icon = Icons.Default.CalendarMonth,
            title = "لا توجد جلسات اليوم",
            message = "اليوم هادئ داخل المكتب. يمكنك مراجعة القضايا القادمة أو جدولة جلسة جديدة متى احتجت.",
            actionText = "فتح القضايا",
            onActionClick = onCasesAction,
            secondaryActionText = if (isWorkspaceEmpty && !hasDemoSeeded) "إنشاء مساحة تجريبية" else null,
            onSecondaryActionClick = if (isWorkspaceEmpty && !hasDemoSeeded) onDemoAction else null
          )
        }
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          todaySessions.forEach { session ->
            DashboardInfoCard(
              icon = Icons.Default.Event,
              accent = MohamyInfo,
              title = session.title,
              subtitle = "${session.caseTitle} • ${session.court.ifBlank { "المحكمة غير محددة" }}",
              meta = "${session.date} ${session.time.ifBlank { "" }}",
              badgeText = session.status,
              badgeTone = MohamyBadgeTone.Gold,
              onClick = { onSessionClick(session) }
            )
          }
        }
      }
    }

    item {
      DashboardSectionHeader(
        title = "مهام عاجلة",
        subtitle = "المهام التي تحتاج قراراً أو متابعة قريبة"
      )
    }

    item {
      if (urgentTasks.isEmpty()) {
        MohamyCard {
          MohamyEmptyState(
            icon = Icons.Default.TaskAlt,
            title = "لا توجد مهام عاجلة",
            message = "كل المهام الحالية تحت السيطرة. راجع القائمة الكاملة إذا أردت التخطيط لخطوات اليوم التالية.",
            actionText = "فتح المهام",
            onActionClick = onTasksAction,
            secondaryActionText = if (isWorkspaceEmpty && !hasDemoSeeded) "عرض تجريبي" else null,
            onSecondaryActionClick = if (isWorkspaceEmpty && !hasDemoSeeded) onDemoAction else null
          )
        }
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          urgentTasks.forEach { task ->
            val overdue = task.dueDate < SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
            DashboardInfoCard(
              icon = Icons.Default.TaskAlt,
              accent = if (overdue) MohamyDanger else MohamyGold,
              title = task.title,
              subtitle = task.caseTitle ?: "مهمة عامة داخل المكتب",
              meta = "الاستحقاق: ${task.dueDate.ifBlank { "غير محدد" }}",
              badgeText = if (overdue) "متأخرة" else task.status,
              badgeTone = if (overdue) MohamyBadgeTone.Danger else MohamyBadgeTone.Gold,
              onClick = { onTaskClick(task) }
            )
          }
        }
      }
    }

    item {
      DashboardSectionHeader(
        title = "ملفات حديثة",
        subtitle = "آخر ما تمت أرشفته داخل المساحة المحلية"
      )
    }

    item {
      if (recentFiles.isEmpty()) {
        MohamyCard {
          MohamyEmptyState(
            icon = Icons.Default.Description,
            title = "لا توجد مستندات بعد",
            message = "أضف أول ملف أو مذكرة لتبدأ أرشفة المستندات داخل التطبيق بمظهر قانوني منظم.",
            actionText = "فتح الملفات",
            onActionClick = onFilesAction,
            secondaryActionText = if (isWorkspaceEmpty && !hasDemoSeeded) "بيانات تجريبية" else null,
            onSecondaryActionClick = if (isWorkspaceEmpty && !hasDemoSeeded) onDemoAction else null
          )
        }
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          recentFiles.forEach { file ->
            DashboardInfoCard(
              icon = Icons.Default.Description,
              accent = MohamySuccess,
              title = file.fileName,
              subtitle = "${file.docType} • ${file.caseTitle}",
              meta = if (file.clientName.isBlank()) "محفوظ محلياً داخل الجهاز" else "الموكل: ${file.clientName}",
              badgeText = "فتح",
              badgeTone = MohamyBadgeTone.Success,
              onClick = { onFileClick(file) }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun DashboardHeroCard(
  lawyerName: String,
  showOnboardingCtas: Boolean,
  hasDemoSeeded: Boolean,
  onPrimaryClick: () -> Unit,
  onSecondaryClick: () -> Unit,
) {
  val dark = isSystemInDarkTheme()
  val heroGradient =
    if (dark) {
      Brush.linearGradient(
        colors = listOf(
          MaterialTheme.colorScheme.surfaceVariant,
          MaterialTheme.colorScheme.surface,
          MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
        )
      )
    } else {
      Brush.linearGradient(
        colors = listOf(MohamyLightHero, MohamyLightHeroEnd, MohamyLightHeroEnd.copy(alpha = 0.7f))
      )
    }
  val watermarkAlpha = if (dark) 0.13f else 0.22f
  MohamyCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(22.dp)) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .background(
            heroGradient,
            androidx.compose.foundation.shape.RoundedCornerShape(MohamyDimens.largeCardRadius)
          )
          .padding(22.dp)
    ) {
      Icon(
        imageVector = Icons.Default.AccountBalance,
        contentDescription = null,
        tint = MohamyGoldStrong.copy(alpha = watermarkAlpha),
        modifier = Modifier.size(116.dp).align(Alignment.BottomEnd)
      )
      Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
        MohamyStatusBadge(text = "محلي وآمن", tone = MohamyBadgeTone.Success)
        Text(
          text = "مرحباً، $lawyerName",
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onSurface,
          fontWeight = FontWeight.ExtraBold
        )
        Text(
          text = "منصة عربية احترافية لإدارة القضايا والموكلين والجلسات والمستندات مع خصوصية كاملة داخل جهازك.",
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          DashboardPill("إدارة القضايا", Icons.Default.WorkOutline, Modifier.weight(1f))
          DashboardPill("متابعة الجلسات", Icons.Default.CalendarMonth, Modifier.weight(1f))
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          DashboardPill("المستندات المحلية", Icons.Default.Lock, Modifier.weight(1f))
          DashboardPill("تنظيم العملاء", Icons.Default.Groups, Modifier.weight(1f))
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          MohamyButton(
            text = if (showOnboardingCtas) "ابدأ استخدام التطبيق" else "متابعة العمل",
            icon = Icons.Default.PersonAddAlt1,
            modifier = Modifier.weight(1f),
            onClick = onPrimaryClick
          )
          MohamyButton(
            text =
              if (showOnboardingCtas) {
                if (hasDemoSeeded) "فتح العرض التجريبي" else "إنشاء مساحة تجريبية"
              } else {
                "الإعدادات"
              },
            icon = if (showOnboardingCtas) Icons.Default.AutoAwesome else Icons.Default.Settings,
            modifier = Modifier.weight(1f),
            style = MohamyButtonStyle.Secondary,
            onClick = onSecondaryClick
          )
        }
      }
    }
  }
}

@Composable
private fun DashboardPill(text: String, icon: ImageVector, modifier: Modifier = Modifier) {
  MohamyCard(
    modifier = modifier,
    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
      Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold
      )
    }
  }
}

@Composable
private fun DashboardStatCard(stat: DashboardStat, modifier: Modifier = Modifier) {
  val dark = isSystemInDarkTheme()
  MohamyCard(modifier = modifier) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Box(
        modifier =
          Modifier.size(40.dp)
            .background(stat.accent.copy(alpha = if (dark) 0.14f else 0.18f), androidx.compose.foundation.shape.CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(stat.icon, contentDescription = null, tint = stat.accent, modifier = Modifier.size(22.dp))
      }
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = stat.value,
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onSurface,
          fontWeight = FontWeight.ExtraBold
        )
        Text(
          text = stat.title,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@Composable
private fun DashboardInfoCard(
  icon: ImageVector,
  accent: Color,
  title: String,
  subtitle: String,
  meta: String,
  badgeText: String,
  badgeTone: MohamyBadgeTone,
  onClick: () -> Unit,
) {
  val dark = isSystemInDarkTheme()
  MohamyCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier =
          Modifier.size(40.dp)
            .background(accent.copy(alpha = if (dark) 0.14f else 0.18f), androidx.compose.foundation.shape.CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface,
          fontWeight = FontWeight.ExtraBold
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        Text(
          text = meta,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      MohamyStatusBadge(text = badgeText, tone = badgeTone)
    }
  }
}

@Composable
private fun DashboardSectionHeader(title: String, subtitle: String) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleLarge,
      color = MaterialTheme.colorScheme.onBackground,
      fontWeight = FontWeight.ExtraBold
    )
    Text(
      text = subtitle,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@Preview(showBackground = true, locale = "ar", backgroundColor = 0xFF11110F)
@Composable
private fun DashboardShellPreview() {
  MyApplicationTheme(darkTheme = true) {
    DashboardShell(
      lawyerName = "الأستاذ المحامي",
      stats =
        listOf(
          DashboardStat("قضايا نشطة", "12", Icons.Default.WorkOutline, MohamyGold),
          DashboardStat("جلسات اليوم", "3", Icons.Default.Event, MohamyInfo),
          DashboardStat("مهام عاجلة", "2", Icons.Default.TaskAlt, MohamyDanger),
          DashboardStat("ملفات محلية", "8", Icons.Default.FolderCopy, MohamySuccess)
        ),
      todaySessions =
        listOf(
          CaseSession(
            id = 1,
            caseId = 1,
            caseTitle = "نزاع إيجار تجاري",
            clientId = 1,
            clientName = "شركة النور",
            title = "جلسة إثبات حالة",
            court = "محكمة شمال القاهرة",
            date = "2026-06-28",
            time = "10:30",
            status = "اليوم",
          )
        ),
      urgentTasks =
        listOf(
          LegalTask(
            id = 1,
            caseId = 1,
            title = "مراجعة مذكرة الدفاع",
            dueDate = "2026-06-28",
            priority = "عالي",
            status = "متأخرة"
          )
        ),
      recentFiles =
        listOf(
          CaseFile(
            id = 1,
            caseId = 1,
            caseTitle = "نزاع إيجار تجاري",
            clientId = 1,
            clientName = "شركة النور",
            fileName = "مذكرة رد",
            filePath = "",
            docType = "مذكرة",
            fileLength = 0L,
            uploadDate = 0L,
            extractedText = "",
            normalizedSearchIndex = "",
          )
        ),
      quickActions =
        listOf(
          QuickAction("القضايا", "عرض الملفات النشطة والأرشيف", Icons.Default.WorkOutline, MohamyGold, {}),
          QuickAction("العملاء", "إدارة الموكلين", Icons.Default.Groups, MohamyInfo, {}),
          QuickAction("الجلسات", "تنظيم المواعيد", Icons.Default.CalendarMonth, MohamySuccess, {}),
          QuickAction("المهام", "متابعة الأعمال", Icons.Default.TaskAlt, MohamyDanger, {}),
          QuickAction("المستندات", "الأرشيف المحلي", Icons.AutoMirrored.Filled.InsertDriveFile, MohamyGoldBright, {}),
          QuickAction("الخصوصية المحلية", "الأمان والإعدادات", Icons.Default.PrivacyTip, MohamyInfo, {})
        ),
      showOnboardingCtas = true,
      isWorkspaceEmpty = true,
      hasDemoSeeded = false,
      onStartUsing = {},
      onDemoAction = {},
      onSettingsAction = {},
      onCasesAction = {},
      onTasksAction = {},
      onFilesAction = {},
      onSessionClick = {},
      onTaskClick = {},
      onFileClick = {}
    )
  }
}
