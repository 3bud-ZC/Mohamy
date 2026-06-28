package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Groups
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.data.AppViewModel
import com.example.data.CaseFile
import com.example.data.CaseSession
import com.example.data.Client
import com.example.data.LegalCase
import com.example.data.LegalTask
import com.example.data.Screen
import com.example.ui.components.MohamyButton
import com.example.ui.components.MohamyButtonStyle
import com.example.ui.components.MohamyCard
import com.example.ui.components.MohamyEmptyState
import com.example.ui.components.MohamyStatusBadge
import com.example.ui.components.MohamyBadgeTone
import com.example.ui.theme.MohamyBlack
import com.example.ui.theme.MohamyCharcoal
import com.example.ui.theme.MohamyDanger
import com.example.ui.theme.MohamyDimens
import com.example.ui.theme.MohamyGold
import com.example.ui.theme.MohamySuccess
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class DashboardStat(
  val title: String,
  val value: String,
  val icon: ImageVector,
)

private data class QuickAction(
  val title: String,
  val icon: ImageVector,
  val onClick: () -> Unit,
)

private fun parseDashboardSessionMillis(session: CaseSession): Long? {
  val full = if (session.time.isBlank()) "${session.date} 23:59" else "${session.date} ${session.time}"
  val formats = listOf("yyyy-MM-dd HH:mm", "yyyy-MM-dd H:mm")
  formats.forEach { pattern ->
    try {
      return SimpleDateFormat(pattern, Locale.ENGLISH).parse(full)?.time
    } catch (_: Exception) {
    }
  }
  return null
}

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
  val lawyerName = license?.lawyerName?.takeIf { it.isNotBlank() } ?: "أستاذ / أستاذة"
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
      DashboardStat("قضايا نشطة", cases.size.toString(), Icons.Default.WorkOutline),
      DashboardStat("جلسات اليوم", todaySessions.size.toString(), Icons.Default.Event),
      DashboardStat(
        "مهام متأخرة",
        urgentTasks.count { it.dueDate < todayDateStr }.toString(),
        Icons.Default.TaskAlt
      ),
      DashboardStat("عملاء", clients.size.toString(), Icons.Default.Groups)
    )

  val quickActions =
    listOf(
      QuickAction("إضافة قضية", Icons.Default.WorkOutline) { viewModel.navigateTo(Screen.CaseAddEdit()) },
      QuickAction("إضافة عميل", Icons.Default.PersonAddAlt1) { viewModel.navigateTo(Screen.ClientAddEdit()) },
      QuickAction("إضافة جلسة", Icons.Default.CalendarMonth) { viewModel.navigateTo(Screen.SessionAddEdit()) },
      QuickAction("إضافة مستند", Icons.AutoMirrored.Filled.InsertDriveFile) { viewModel.navigateTo(Screen.FilesLibrary) }
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
    showWelcomeCard = isWorkspaceEmpty || !viewModel.isOnboardingCompleted,
    isWorkspaceEmpty = isWorkspaceEmpty,
    hasDemoSeeded = viewModel.hasDemoSeededForCurrentWorkspace,
    onStartUsing = {
      viewModel.completeOnboarding()
      if (isWorkspaceEmpty) {
        viewModel.navigateTo(Screen.ClientAddEdit())
      }
    },
    onDemoAction = demoAction,
    onSettingsAction = { viewModel.navigateTo(Screen.Settings) },
    onCasesAction = { viewModel.navigateTo(Screen.CasesList) },
    onTasksAction = { viewModel.navigateTo(Screen.TasksList) },
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
  showWelcomeCard: Boolean,
  isWorkspaceEmpty: Boolean,
  hasDemoSeeded: Boolean,
  onStartUsing: () -> Unit,
  onDemoAction: () -> Unit,
  onSettingsAction: () -> Unit,
  onCasesAction: () -> Unit,
  onTasksAction: () -> Unit,
  onSessionClick: (CaseSession) -> Unit,
  onTaskClick: (LegalTask) -> Unit,
  onFileClick: (CaseFile) -> Unit,
) {
  LazyColumn(
    modifier =
      Modifier.fillMaxSize().background(
        Brush.verticalGradient(colors = listOf(MohamyBlack, MohamyCharcoal, MohamyBlack))
      ),
    contentPadding = androidx.compose.foundation.layout.PaddingValues(
      horizontal = MohamyDimens.screenHorizontal,
      vertical = MohamyDimens.screenVertical
    ),
    verticalArrangement = Arrangement.spacedBy(MohamyDimens.sectionGap)
  ) {
    if (showWelcomeCard) {
      item {
        WorkspaceWelcomeCard(
          isWorkspaceEmpty = isWorkspaceEmpty,
          hasDemoSeeded = hasDemoSeeded,
          onStartUsing = onStartUsing,
          onDemoAction = onDemoAction,
          onSettingsAction = onSettingsAction
        )
      }
    }

    item {
      MohamyCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
          Box(
            modifier =
              Modifier.fillMaxWidth().background(
                Brush.linearGradient(
                  colors = listOf(MohamyCharcoal, Color(0xFF292317))
                ),
                RoundedCornerShape(26.dp)
              ).padding(18.dp)
          ) {
            Icon(
              imageVector = Icons.Default.AccountBalance,
              contentDescription = null,
              tint = MohamyGold.copy(alpha = 0.12f),
              modifier = Modifier.size(108.dp).align(Alignment.BottomEnd)
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                text =
                  if (lawyerName.isBlank() || lawyerName.any { it.isDigit() }) {
                    "مرحبًا بك"
                  } else {
                    "مرحبًا، $lawyerName"
                  },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold
              )
              Text(
                text = "لوحة قيادة قانونية مصممة لمتابعة القضايا والجلسات والمهام اليومية بواجهة عربية احترافية.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }

    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("ملخص اليوم")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
          stats.chunked(2).first().forEach { stat ->
            DashboardStatCard(stat = stat, modifier = Modifier.weight(1f))
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
          stats.chunked(2).getOrNull(1)?.forEach { stat ->
            DashboardStatCard(stat = stat, modifier = Modifier.weight(1f))
          }
        }
      }
    }

    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("إجراءات سريعة")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
          quickActions.chunked(2).first().forEach { action ->
            QuickActionCard(action = action, modifier = Modifier.weight(1f))
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
          quickActions.chunked(2).getOrNull(1)?.forEach { action ->
            QuickActionCard(action = action, modifier = Modifier.weight(1f))
          }
        }
      }
    }

    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("جلسات اليوم")
        if (todaySessions.isEmpty()) {
          MohamyCard {
            MohamyEmptyState(
              icon = Icons.Default.CalendarMonth,
              title = "لا توجد جلسات اليوم",
              message = "يوم هادئ داخل المكتب. يمكنك إضافة جلسة جديدة أو مراجعة القضايا القادمة.",
              actionText = "فتح القضايا",
              onActionClick = onCasesAction,
              secondaryActionText = if (isWorkspaceEmpty && !hasDemoSeeded) "مساحة تجريبية" else null,
              onSecondaryActionClick = if (isWorkspaceEmpty && !hasDemoSeeded) onDemoAction else null
            )
          }
        } else {
          todaySessions.forEach { session ->
            MohamyCard(
              modifier = Modifier.fillMaxWidth().clickable { onSessionClick(session) }
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier.size(46.dp).background(MohamyGold.copy(alpha = 0.12f), CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(Icons.Default.Event, contentDescription = null, tint = MohamyGold)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text(session.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                  Text(
                    "${session.caseTitle} • ${session.court.ifBlank { "المحكمة غير محددة" }}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                  )
                  Text(
                    "${session.date} ${session.time.ifBlank { "" }}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                  )
                }
                MohamyStatusBadge(text = session.status, tone = MohamyBadgeTone.Gold)
              }
            }
          }
        }
      }
    }

    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("مهام عاجلة")
        if (urgentTasks.isEmpty()) {
          MohamyCard {
            MohamyEmptyState(
              icon = Icons.Default.TaskAlt,
              title = "لا توجد مهام عاجلة",
              message = "كل المهام الحالية تحت السيطرة. يمكنك متابعة قائمة المهام الكاملة لاحقًا.",
              actionText = "فتح المهام",
              onActionClick = onTasksAction,
              secondaryActionText = if (isWorkspaceEmpty && !hasDemoSeeded) "عرض تجريبي" else null,
              onSecondaryActionClick = if (isWorkspaceEmpty && !hasDemoSeeded) onDemoAction else null
            )
          }
        } else {
          urgentTasks.forEach { task ->
            val tone = if (task.dueDate < SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())) {
              MohamyBadgeTone.Danger
            } else {
              MohamyBadgeTone.Gold
            }
            MohamyCard(modifier = Modifier.fillMaxWidth().clickable { onTaskClick(task) }) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier.size(42.dp).background(MohamyDanger.copy(alpha = 0.16f), CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(Icons.Default.TaskAlt, contentDescription = null, tint = MohamyDanger)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text(task.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                  Text(
                    "الاستحقاق: ${task.dueDate.ifBlank { "غير محدد" }}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                  )
                }
                MohamyStatusBadge(text = task.status, tone = tone)
              }
            }
          }
        }
      }
    }

    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("ملفات حديثة")
        if (recentFiles.isEmpty()) {
          MohamyCard {
            MohamyEmptyState(
              icon = Icons.Default.Description,
              title = "لا توجد مستندات بعد",
              message = "أضف أول ملف أو مذكرة لتبدأ أرشفة المستندات داخل التطبيق.",
              actionText = "فتح القضايا",
              onActionClick = onCasesAction,
              secondaryActionText = if (isWorkspaceEmpty && !hasDemoSeeded) "بيانات تجريبية" else null,
              onSecondaryActionClick = if (isWorkspaceEmpty && !hasDemoSeeded) onDemoAction else null
            )
          }
        } else {
          recentFiles.forEach { file ->
            MohamyCard(modifier = Modifier.fillMaxWidth().clickable { onFileClick(file) }) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier.size(42.dp).background(MohamySuccess.copy(alpha = 0.16f), CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(Icons.Default.Description, contentDescription = null, tint = MohamySuccess)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text(file.fileName, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                  Text(
                    "${file.docType} • ${file.caseTitle}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                  )
                }
                MohamyButton(
                  text = "فتح",
                  onClick = { onFileClick(file) },
                  style = MohamyButtonStyle.Secondary
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DashboardStatCard(stat: DashboardStat, modifier: Modifier = Modifier) {
  MohamyCard(modifier = modifier.aspectRatio(1.1f)) {
    Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
      Box(
        modifier = Modifier.size(42.dp).background(MohamyGold.copy(alpha = 0.12f), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(stat.icon, contentDescription = null, tint = MohamyGold)
      }
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = stat.value,
        style = MaterialTheme.typography.displayMedium,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.ExtraBold
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = stat.title,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
private fun QuickActionCard(action: QuickAction, modifier: Modifier = Modifier) {
  MohamyCard(modifier = modifier.clickable { action.onClick() }) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Box(
        modifier = Modifier.size(46.dp).background(MohamyGold.copy(alpha = 0.14f), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(action.icon, contentDescription = null, tint = MohamyGold)
      }
      Text(
        text = action.title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold
      )
    }
  }
}

@Composable
private fun WorkspaceWelcomeCard(
  isWorkspaceEmpty: Boolean,
  hasDemoSeeded: Boolean,
  onStartUsing: () -> Unit,
  onDemoAction: () -> Unit,
  onSettingsAction: () -> Unit,
) {
  MohamyCard {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
      Box(
        modifier =
          Modifier.fillMaxWidth().background(
            Brush.linearGradient(colors = listOf(MohamyCharcoal, Color(0xFF2B2417))),
            RoundedCornerShape(26.dp)
          ).padding(18.dp)
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = if (isWorkspaceEmpty) "ابدأ مكتبك الرقمي أو جهّز عرضاً تجريبياً" else "بطاقة ترحيب سريعة للمساحة الحالية",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold
          )
          Text(
            text = "محامي فون يدير القضايا والجلسات والعملاء والمستندات والمهام داخل مساحة محلية خاصة بالمكتب.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        WelcomePill("إدارة القضايا", Icons.Default.WorkOutline, Modifier.weight(1f))
        WelcomePill("متابعة الجلسات", Icons.Default.CalendarMonth, Modifier.weight(1f))
      }
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        WelcomePill("تنظيم العملاء", Icons.Default.Groups, Modifier.weight(1f))
        WelcomePill("أرشفة المستندات", Icons.Default.Description, Modifier.weight(1f))
      }
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        WelcomePill("المهام والتنبيهات", Icons.Default.TaskAlt, Modifier.weight(1f))
        WelcomePill("الخصوصية المحلية", Icons.Default.PrivacyTip, Modifier.weight(1f))
      }

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MohamyButton(
          text = "ابدأ استخدام التطبيق",
          icon = Icons.Default.PersonAddAlt1,
          modifier = Modifier.weight(1f),
          onClick = onStartUsing
        )
        MohamyButton(
          text = if (hasDemoSeeded) "فتح العرض التجريبي" else "إنشاء مساحة تجريبية",
          icon = Icons.Default.AutoAwesome,
          modifier = Modifier.weight(1f),
          style = MohamyButtonStyle.Secondary,
          onClick = onDemoAction
        )
      }

      MohamyButton(
        text = "الإعدادات والملف المهني",
        icon = Icons.Default.Settings,
        modifier = Modifier.fillMaxWidth(),
        style = MohamyButtonStyle.Ghost,
        onClick = onSettingsAction
      )
    }
  }
}

@Composable
private fun WelcomePill(text: String, icon: ImageVector, modifier: Modifier = Modifier) {
  MohamyCard(modifier = modifier) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier.size(36.dp).background(MohamyGold.copy(alpha = 0.14f), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(icon, contentDescription = null, tint = MohamyGold, modifier = Modifier.size(18.dp))
      }
      Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold
      )
    }
  }
}

@Composable
private fun SectionLabel(text: String) {
  Text(
    text = text,
    style = MaterialTheme.typography.titleMedium,
    color = MaterialTheme.colorScheme.onBackground,
    fontWeight = FontWeight.ExtraBold
  )
}

@Preview(showBackground = true, locale = "ar", backgroundColor = 0xFF121212)
@Composable
private fun DashboardShellPreview() {
  MyApplicationTheme(darkTheme = true) {
    DashboardShell(
      lawyerName = "أستاذ / أستاذة",
      stats =
        listOf(
          DashboardStat("قضايا نشطة", "12", Icons.Default.WorkOutline),
          DashboardStat("جلسات اليوم", "3", Icons.Default.Event),
          DashboardStat("مهام متأخرة", "2", Icons.Default.TaskAlt),
          DashboardStat("عملاء", "26", Icons.Default.Groups)
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
          QuickAction("إضافة قضية", Icons.Default.WorkOutline, {}),
          QuickAction("إضافة عميل", Icons.Default.PersonAddAlt1, {}),
          QuickAction("إضافة جلسة", Icons.Default.CalendarMonth, {}),
          QuickAction("إضافة مستند", Icons.AutoMirrored.Filled.InsertDriveFile, {})
        ),
      showWelcomeCard = true,
      isWorkspaceEmpty = true,
      hasDemoSeeded = false,
      onStartUsing = {},
      onDemoAction = {},
      onSettingsAction = {},
      onCasesAction = {},
      onTasksAction = {},
      onSessionClick = {},
      onTaskClick = {},
      onFileClick = {}
    )
  }
}
