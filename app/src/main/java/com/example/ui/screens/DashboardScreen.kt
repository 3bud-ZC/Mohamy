package com.example.ui.screens
import com.example.data.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

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
    files: List<CaseFile>
) {
    val openCasesCount = cases.size
    val pendingTasksCount = tasks.filter { it.status == "مفتوحة" }.size
    val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
    val todaySessionsCount = sessions.count { it.date.trim() == todayDateStr && it.status != "ملغاة" }
    val todayDisplayLabel = SimpleDateFormat("dd MMMM yyyy", Locale.forLanguageTag("ar")).format(Date())
    val latestSessions = sessions
        .sortedByDescending { parseDashboardSessionMillis(it) ?: Long.MIN_VALUE }
        .take(3)
    val latestFiles = files.sortedByDescending { it.uploadDate }.take(3)
    val upcomingSessions = sessions
        .mapNotNull { session -> parseDashboardSessionMillis(session)?.let { session to it } }
        .filter { (session, time) -> time >= System.currentTimeMillis() && session.status != "ملغاة" }
        .sortedBy { it.second }
        .take(3)
    val alerts = remember(sessions, tasks) { viewModel.localAlertsSummary() }
    val readinessCases = remember(cases, sessions, tasks, files) {
        cases.sortedByDescending { viewModel.caseReadinessScore(it) }.take(3)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFF4F7FC), Color(0xFFEFF3FA), Color.White)
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(LegalNavyPrimary, LegalSlateDark)
                    )
                )
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalance,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.08f),
                modifier = Modifier
                    .size(150.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 24.dp, y = 24.dp)
            )

            Column(modifier = Modifier.padding(22.dp)) {
                Text(
                    text = "مرحباً بسيادة المستشار ⚖️",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "مكتبك الرقمي محمي بالكامل محلياً على هاتفك الذكي. ينظم شؤونك وقضاياك بخصوصية وسرية تامة.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LegalGoldLight.copy(alpha = 0.9f),
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DashboardPill(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Event,
                        text = "اليوم: $todayDisplayLabel"
                    )
                    DashboardPill(
                        modifier = Modifier.weight(1f),
                        icon = Icons.AutoMirrored.Filled.Assignment,
                        text = "مهام مفتوحة: $pendingTasksCount"
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, LegalNavyPrimary.copy(alpha = 0.08f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "إحصائيات المكتب",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = LegalNavyPrimary,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(
                        title = "قضايا مفتوحة",
                        count = openCasesCount.toString(),
                        icon = Icons.Default.Work,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "موكلون نشطون",
                        count = clients.size.toString(),
                        icon = Icons.Default.Person,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(
                        title = "جلسات اليوم",
                        count = todaySessionsCount.toString(),
                        icon = Icons.Default.Event,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "مهام معلقة",
                        count = pendingTasksCount.toString(),
                        icon = Icons.AutoMirrored.Filled.Assignment,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, LegalNavyPrimary.copy(alpha = 0.08f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("تقويم المكتب اليومي", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = LegalNavyPrimary)
                if (alerts.isEmpty()) {
                    Text("لا توجد تنبيهات حالياً.", color = Color.Gray, fontSize = 12.sp)
                } else {
                    alerts.forEach { alert ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = LegalGoldSecondary, modifier = Modifier.size(16.dp))
                            Text(alert, fontSize = 12.sp, color = Color.DarkGray)
                        }
                    }
                }
            }
        }

        val nextPrioritySession = upcomingSessions.firstOrNull()
        val urgentTasks = tasks.filter { task ->
            val due = try {
                SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(task.dueDate)?.time
            } catch (_: Exception) {
                null
            }
            due != null && due < System.currentTimeMillis() && task.status != "منتهية"
        }.take(3)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, LegalNavyPrimary.copy(alpha = 0.08f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("أولوية اليوم", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = LegalNavyPrimary)
                if (nextPrioritySession == null && urgentTasks.isEmpty()) {
                    Text("لا توجد عناصر عاجلة حالياً. المكتب في وضع هادئ.", color = Color.Gray, fontSize = 12.sp)
                } else {
                    nextPrioritySession?.let { (session, _) ->
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(LegalGoldSecondary.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Event, contentDescription = null, tint = LegalNavyPrimary, modifier = Modifier.size(18.dp))
                                }
                            },
                            headlineContent = { Text("أقرب جلسة: ${session.title}", fontWeight = FontWeight.Bold, color = LegalNavyPrimary) },
                            supportingContent = { Text("${session.caseTitle} | ${session.date} ${session.time}", fontSize = 12.sp, color = Color.Gray) }
                        )
                    }
                    if (urgentTasks.isNotEmpty()) {
                        urgentTasks.forEach { task ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, tint = LegalGoldSecondary, modifier = Modifier.size(16.dp))
                                Text("مهمة متأخرة: ${task.title}", fontSize = 12.sp, color = Color.DarkGray)
                            }
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, LegalNavyPrimary.copy(alpha = 0.08f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "إجراءات سريعة",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = LegalNavyPrimary,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionBtn(
                        title = "موكل جديد",
                        icon = Icons.Default.PersonAdd,
                        onClick = { viewModel.navigateTo(Screen.ClientAddEdit()) },
                        modifier = Modifier.weight(1f),
                        accentColor = LegalNavyPrimary
                    )
                    QuickActionBtn(
                        title = "قضية جديدة",
                        icon = Icons.Default.AddBusiness,
                        onClick = { viewModel.navigateTo(Screen.CaseAddEdit()) },
                        modifier = Modifier.weight(1f),
                        accentColor = LegalNavyPrimary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionBtn(
                        title = "جلسة جديدة",
                        icon = Icons.Default.AddAlert,
                        onClick = { viewModel.navigateTo(Screen.SessionAddEdit()) },
                        modifier = Modifier.weight(1f),
                        accentColor = LegalNavyPrimary
                    )
                    QuickActionBtn(
                        title = "مكتبة الملفات",
                        icon = Icons.Default.FolderCopy,
                        onClick = { viewModel.navigateTo(Screen.FilesLibrary) },
                        modifier = Modifier.weight(1f),
                        accentColor = LegalGoldSecondary
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionBtn(
                        title = "المساعد الذكي",
                        icon = Icons.Default.AutoAwesome,
                        onClick = { viewModel.navigateTo(Screen.SmartAssistant) },
                        modifier = Modifier.weight(1f),
                        accentColor = LegalGoldSecondary
                    )
                    QuickActionBtn(
                        title = "القوالب القانونية",
                        icon = Icons.AutoMirrored.Filled.InsertDriveFile,
                        onClick = { viewModel.navigateTo(Screen.LegalTemplatesList) },
                        modifier = Modifier.weight(1f),
                        accentColor = LegalGoldSecondary
                    )
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.navigateTo(Screen.SmartAssistant) },
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = LegalNavyPrimary),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(LegalGoldSecondary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = LegalGoldLight)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("المساعد الذكي", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    Text(
                        "لخص القضايا، ابحث داخل الملفات، واعرض المستندات الناقصة",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                }
                OutlinedButton(
                    onClick = { viewModel.navigateTo(Screen.SmartAssistant) },
                    border = BorderStroke(1.dp, LegalGoldLight.copy(alpha = 0.35f))
                ) {
                    Text("فتح", color = LegalGoldLight)
                }
            }
        }

        DashboardSectionTitle("أحدث القضايا النشطة")
        if (cases.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "لا توجد قضايا نشطة مسجلة حالياً.",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        } else {
            cases.take(4).forEach { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { viewModel.navigateTo(Screen.CaseDetails(item.id)) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(item.title, fontWeight = FontWeight.Bold, color = LegalNavyPrimary) },
                        supportingContent = { Text("رقم ${item.caseNumber} لسنة ${item.caseYear} | محكمة ${item.courtName}", color = Color.Gray, fontSize = 12.sp) },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(LegalGrayLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Gavel, null, tint = LegalNavyPrimary)
                            }
                        },
                        trailingContent = {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(LegalGoldSecondary.copy(alpha = 0.15f))
                                    .border(1.dp, LegalGoldSecondary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(item.status, color = LegalNavyPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }
            }
        }

        if (readinessCases.isNotEmpty()) {
            DashboardSectionTitle("أعلى القضايا جاهزية")
            readinessCases.forEach { item ->
                val readiness = viewModel.caseReadinessScore(item)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { viewModel.navigateTo(Screen.CaseDetails(item.id)) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(item.title, fontWeight = FontWeight.Bold, color = LegalNavyPrimary, modifier = Modifier.weight(1f))
                            Text("$readiness%", color = LegalNavyPrimary, fontWeight = FontWeight.ExtraBold)
                        }
                        LinearProgressIndicator(
                            progress = readiness / 100f,
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(99.dp)),
                            color = LegalGoldSecondary,
                            trackColor = LegalGrayLight
                        )
                        Text("${viewModel.caseReadinessLabel(item)} | ${item.caseType} | ${item.clientName}", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }

        DashboardSectionTitle("أقرب الجلسات")
        if (upcomingSessions.isEmpty()) {
            Text("لا توجد جلسات قادمة مجدولة حالياً.", color = Color.Gray, fontSize = 13.sp)
        } else {
            upcomingSessions.forEach { (session, _) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { viewModel.navigateTo(Screen.SessionAddEdit(sessionId = session.id)) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(session.title, fontWeight = FontWeight.Bold, color = LegalNavyPrimary) },
                        supportingContent = { Text("${session.caseTitle} | ${session.date} ${session.time} | ${session.status}", fontSize = 12.sp, color = Color.Gray) },
                        trailingContent = { Icon(Icons.Default.Event, contentDescription = null, tint = LegalGoldSecondary) }
                    )
                }
            }
        }

        if (latestSessions.isNotEmpty()) {
            DashboardSectionTitle("آخر تحديثات الجلسات")
            latestSessions.forEach { session ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { viewModel.navigateTo(Screen.SessionAddEdit(sessionId = session.id)) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(session.title, fontWeight = FontWeight.Bold, color = LegalNavyPrimary) },
                        supportingContent = { Text("${session.caseTitle} | ${session.date} ${session.time} | ${session.status}", fontSize = 12.sp, color = Color.Gray) },
                        trailingContent = { Icon(Icons.Default.History, contentDescription = null, tint = LegalGoldSecondary) }
                    )
                }
            }
        }

        DashboardSectionTitle("آخر الملفات المضافة")
        if (latestFiles.isEmpty()) {
            Text("لا توجد ملفات مرفوعة بعد.", color = Color.Gray, fontSize = 13.sp)
        } else {
            latestFiles.forEach { file ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { viewModel.navigateTo(Screen.CaseDetails(file.caseId)) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(file.fileName, fontWeight = FontWeight.Bold, color = LegalNavyPrimary) },
                        supportingContent = { Text("${file.docType} | ${file.caseTitle}", fontSize = 12.sp, color = Color.Gray) },
                        trailingContent = { Icon(Icons.Default.FileCopy, contentDescription = null, tint = LegalGoldSecondary) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardPill(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    text: String
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LegalGoldLight,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                color = Color.White.copy(alpha = 0.92f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DashboardSectionTitle(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 16.sp,
        color = LegalNavyPrimary,
        modifier = Modifier.padding(top = 12.dp)
    )
}

@Composable
fun StatCard(title: String, count: String, icon: ImageVector, modifier: Modifier) {
    Card(
        modifier = modifier.height(112.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, LegalNavyPrimary.copy(alpha = 0.08f)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LegalGoldSecondary.copy(alpha = 0.05f),
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 20.dp, y = 20.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(LegalNavyPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = title, tint = LegalNavyPrimary, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        text = count,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = LegalNavyPrimary
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )
            }
        }
    }
}

@Composable
fun QuickActionBtn(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = LegalNavyPrimary
) {
    Card(
        modifier = modifier
            .height(92.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.16f)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(7.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = LegalNavyPrimary,
                textAlign = TextAlign.Center,
                lineHeight = 15.sp
            )
        }
    }
}
