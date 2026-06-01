package com.example.ui.screens
import com.example.data.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

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
    val todaySessionsCount = sessions.filter { it.date == todayDateStr }.size
    val latestSessions = sessions.sortedByDescending { "${it.date} ${it.time}" }.take(3)
    val latestFiles = files.sortedByDescending { it.uploadDate }.take(3)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Welcome Header Setup
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(LegalNavyPrimary, LegalSlateDark)
                    )
                )
        ) {
            // Decorative background element
            Icon(
                imageVector = Icons.Default.AccountBalance,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.05f),
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 20.dp, y = 20.dp)
            )

            Column(modifier = Modifier.padding(24.dp)) {
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
            }
        }

        // Stats Row Widget Layout grid-style
        Text(
            text = "إحصائيات المكتب",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(
                title = "قضايا مفتوحة",
                count = openCasesCount.toString(),
                icon = Icons.Default.Work,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "عملاء نشطون",
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
                icon = Icons.Default.Assignment,
                modifier = Modifier.weight(1f)
            )
        }

        // Quick Actions panel
        Text(
            text = "إجراءات سريعة",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickActionBtn(
                title = "عميل جديد",
                icon = Icons.Default.PersonAdd,
                onClick = { viewModel.navigateTo(Screen.ClientAddEdit()) },
                modifier = Modifier.weight(1.0f)
            )
            QuickActionBtn(
                title = "قضية جديدة",
                icon = Icons.Default.AddBusiness,
                onClick = { viewModel.navigateTo(Screen.CaseAddEdit()) },
                modifier = Modifier.weight(1.0f)
            )
            QuickActionBtn(
                title = "جلسة جديدة",
                icon = Icons.Default.AddAlert,
                onClick = { viewModel.navigateTo(Screen.SessionAddEdit()) },
                modifier = Modifier.weight(1.0f)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
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
                Icon(Icons.Default.Gavel, contentDescription = null, tint = LegalGoldLight)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickActionBtn(
                title = "القوالب القانونية",
                icon = Icons.Default.InsertDriveFile,
                onClick = { viewModel.navigateTo(Screen.LegalTemplatesList) },
                modifier = Modifier.weight(1.0f)
            )
            QuickActionBtn(
                title = "نسخة احتياطية",
                icon = Icons.Default.Backup,
                onClick = { viewModel.navigateTo(Screen.BackupRestore) },
                modifier = Modifier.weight(1.0f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        QuickActionBtn(
            title = "استيراد بيانات",
            icon = Icons.Default.FileUpload,
            onClick = { viewModel.navigateTo(Screen.ImportData) },
            modifier = Modifier.fillMaxWidth()
        )

        // Recent cases list
        Text(
            text = "أحدث القضايا النشطة",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
        )
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

        Text(
            text = "أحدث الجلسات",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
        )
        if (latestSessions.isEmpty()) {
            Text("لا توجد جلسات مضافة بعد.", color = Color.Gray, fontSize = 13.sp)
        } else {
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
                        supportingContent = { Text("${session.caseTitle} | ${session.date} ${session.time}", fontSize = 12.sp, color = Color.Gray) },
                        trailingContent = { Icon(Icons.Default.Event, contentDescription = null, tint = LegalGoldSecondary) }
                    )
                }
            }
        }

        Text(
            text = "آخر الملفات المضافة",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
        )
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
fun StatCard(title: String, count: String, icon: ImageVector, modifier: Modifier) {
    Card(
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Decorative background icon
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
                    .padding(16.dp),
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
                            .background(LegalNavyPrimary.copy(alpha = 0.1f)),
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
fun QuickActionBtn(title: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(85.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = LegalGrayLight)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = LegalNavyPrimary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = LegalNavyPrimary,
                textAlign = TextAlign.Center
            )
        }
    }
}
