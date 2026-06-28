package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppViewModel
import com.example.data.CaseSession
import com.example.data.LegalCase
import com.example.data.Screen
import com.example.ui.components.MohamyBadgeTone
import com.example.ui.components.MohamyButton
import com.example.ui.components.MohamyButtonStyle
import com.example.ui.components.MohamyEmptyState
import com.example.ui.components.MohamySearchBar
import com.example.ui.components.MohamyStatusBadge
import com.example.ui.components.SessionCard
import com.example.ui.theme.LegalGoldLight
import com.example.ui.theme.LegalGoldSecondary
import com.example.ui.theme.LegalNavyPrimary
import com.example.ui.theme.MohamyDimens
import com.example.ui.theme.legalScreenBackground
import java.util.Calendar
import java.util.Locale

@Composable
fun SessionsListScreen(viewModel: AppViewModel, sessions: List<CaseSession>, cases: List<LegalCase>) {
    var searchTxt by remember { mutableStateOf("") }
    val normalizedSearch = remember(searchTxt) { viewModel.repository.normalizeArabic(searchTxt) }
    val filteredSessions = remember(sessions, normalizedSearch) {
        sessions.filter {
            val haystack = viewModel.repository.normalizeArabic(
                "${it.caseTitle} ${it.title} ${it.type} ${it.court} ${it.courtCircle} ${it.clientName} ${it.status} ${it.requirements} ${it.result}"
            )
            normalizedSearch.isBlank() || haystack.contains(normalizedSearch)
        }
    }
    val upcomingCount = remember(sessions) { sessions.count { it.status == "قادمة" } }
    val completedCount = remember(sessions) { sessions.count { it.status == "منتهية" } }
    val linkedCasesCount = remember(sessions, cases) { sessions.map { it.caseId }.distinct().size.coerceAtMost(cases.size) }
    val context = LocalContext.current

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.navigateTo(Screen.SessionAddEdit()) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة جلسة")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .legalScreenBackground()
                .padding(padding)
                .padding(horizontal = MohamyDimens.screenHorizontal, vertical = MohamyDimens.screenVertical),
            verticalArrangement = Arrangement.spacedBy(MohamyDimens.sectionGap)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(MohamyDimens.largeCardRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "سجل الجلسات",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                "واجهة متابعة المرافعات والمواعيد القادمة داخل المكتب المحلي.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        MohamyStatusBadge(
                            text = "${filteredSessions.size} نتيجة",
                            tone = MohamyBadgeTone.Gold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SessionStatCard("إجمالي الجلسات", sessions.size.toString(), Modifier.weight(1f))
                        SessionStatCard("الجلسات القادمة", upcomingCount.toString(), Modifier.weight(1f))
                        SessionStatCard("القضايا المرتبطة", linkedCasesCount.toString(), Modifier.weight(1f))
                    }
                }
            }

            MohamySearchBar(
                value = searchTxt,
                onValueChange = { searchTxt = it },
                placeholder = "ابحث بالدعوى أو المحكمة أو الموكل أو متطلبات الجلسة..."
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MohamyButton(
                    text = "تصدير تقرير الجلسات",
                    icon = Icons.Default.Description,
                    style = MohamyButtonStyle.Secondary,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val reportData = filteredSessions.map { session -> session to session.caseTitle }
                        val uri = com.example.util.PdfExporter.exportUpcomingSessions(context, reportData)
                        if (uri != null) {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "مشاركة تقرير الجلسات"))
                        } else {
                            android.widget.Toast.makeText(context, "فشل تصدير الـ PDF", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                MohamyStatusBadge(
                    text = "منتهية $completedCount",
                    tone = MohamyBadgeTone.Success
                )
            }

            if (filteredSessions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    MohamyEmptyState(
                        icon = Icons.Default.CalendarToday,
                        title = if (sessions.isEmpty()) "لا توجد جلسات محفوظة بعد" else "لا توجد نتائج مطابقة",
                        message =
                            if (sessions.isEmpty()) {
                                "ابدأ بجدولة أول جلسة وربطها بالقضية المناسبة داخل الأرشيف المحلي."
                            } else {
                                "جرّب تعديل كلمات البحث أو راجع حالة الجلسات الحالية."
                            },
                        actionText = if (sessions.isEmpty()) "إضافة جلسة" else null,
                        onActionClick =
                            if (sessions.isEmpty()) {
                                {
                                    if (cases.isEmpty()) viewModel.navigateTo(Screen.CaseAddEdit()) else viewModel.navigateTo(Screen.SessionAddEdit())
                                }
                            } else {
                                null
                            },
                        secondaryActionText = if (sessions.isEmpty() && !viewModel.hasDemoSeededForCurrentWorkspace) "عرض تجريبي" else null,
                        onSecondaryActionClick = if (sessions.isEmpty() && !viewModel.hasDemoSeededForCurrentWorkspace) {
                            { viewModel.seedDemoWorkspace() }
                        } else {
                            null
                        }
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredSessions, key = { it.id }) { session ->
                        SessionCard(
                            session = session,
                            onClick = { viewModel.navigateTo(Screen.SessionAddEdit(sessionId = session.id)) }
                        ) {
                            MohamyButton(
                                text = "تعديل",
                                icon = Icons.Default.Edit,
                                style = MohamyButtonStyle.Secondary,
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.navigateTo(Screen.SessionAddEdit(sessionId = session.id)) }
                            )
                            MohamyButton(
                                text = "حذف",
                                icon = Icons.Default.DeleteOutline,
                                style = MohamyButtonStyle.Ghost,
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.deleteSession(session) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionStatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionAddEditScreen(sessionId: Int?, presetCaseId: Int?, viewModel: AppViewModel, cases: List<LegalCase>) {
    val editing = sessionId?.let { id -> viewModel.allSessions.value.find { it.id == id } }

    var title by remember { mutableStateOf(editing?.title ?: "جلسة مرافعة") }
    var date by remember { mutableStateOf(editing?.date ?: "") }
    var time by remember { mutableStateOf(editing?.time ?: "10:00") }
    var requirements by remember { mutableStateOf(editing?.requirements ?: "") }
    var result by remember { mutableStateOf(editing?.result ?: "") }
    var status by remember { mutableStateOf(editing?.status ?: "قادمة") }

    var selectedCaseIndex by remember(editing?.id, cases) {
        mutableStateOf(
            cases.indexOfFirst { it.id == (editing?.caseId ?: presetCaseId) }
                .takeIf { it >= 0 }
                ?: 0
        )
    }
    var caseExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }
    val statusOptions = listOf("قادمة", "منتهية", "مؤجلة", "ملغاة")

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            date = String.format(Locale.ENGLISH, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val timePickerDialog = android.app.TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            time = String.format(Locale.ENGLISH, "%02d:%02d", hourOfDay, minute)
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        false
    )

    LaunchedEffect(presetCaseId, cases) {
        if (presetCaseId != null) {
            val idx = cases.indexOfFirst { it.id == presetCaseId }
            if (idx != -1) {
                selectedCaseIndex = idx
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .legalScreenBackground()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = LegalNavyPrimary)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(LegalGoldSecondary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (editing == null) Icons.Default.Add else Icons.Default.Edit,
                        contentDescription = null,
                        tint = LegalGoldLight,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = if (editing == null) "إضافة / جدولة جلسة محكمة" else "تعديل تفاصيل الجلسة",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "نظم مواعيد جلساتك وتفاصيلها بدقة",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        if (cases.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f)),
                colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.05f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "لا توجد قضايا نشطة مضافة بالمكتب حالياً لجدولة الجلسة عليها.",
                        color = Color.Red,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            ExposedDropdownMenuBox(
                expanded = caseExpanded,
                onExpandedChange = { caseExpanded = !caseExpanded }
            ) {
                OutlinedTextField(
                    value = cases.getOrNull(selectedCaseIndex)?.title ?: "اختر القضية",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("ارتباط بالقضية") },
                    leadingIcon = { Icon(Icons.Default.Folder, null, tint = LegalNavyPrimary) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = caseExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LegalGoldSecondary,
                        focusedLabelColor = LegalNavyPrimary
                    )
                )
                ExposedDropdownMenu(
                    expanded = caseExpanded,
                    onDismissRequest = { caseExpanded = false }
                ) {
                    cases.forEachIndexed { i, c ->
                        DropdownMenuItem(
                            text = { Text(c.title) },
                            onClick = {
                                selectedCaseIndex = i
                                caseExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("مسمى الجلسة / سبب الحضور") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = date,
                onValueChange = { },
                readOnly = true,
                label = { Text("تاريخ الجلسة") },
                modifier = Modifier.weight(1f).clickable { datePickerDialog.show() },
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = LegalNavyPrimary,
                    disabledBorderColor = LegalNavyPrimary.copy(alpha = 0.5f),
                    disabledLabelColor = LegalNavyPrimary
                )
            )
            OutlinedTextField(
                value = time,
                onValueChange = { },
                readOnly = true,
                label = { Text("وقت الجلسة") },
                modifier = Modifier.weight(1f).clickable { timePickerDialog.show() },
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = LegalNavyPrimary,
                    disabledBorderColor = LegalNavyPrimary.copy(alpha = 0.5f),
                    disabledLabelColor = LegalNavyPrimary
                )
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = requirements,
            onValueChange = { requirements = it },
            label = { Text("الأوراق المطلوبة / طلبات المحكمة بالجلسة") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = result,
            onValueChange = { result = it },
            label = { Text("القرار والنتيجة (في حال انتهت)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        ExposedDropdownMenuBox(
            expanded = statusExpanded,
            onExpandedChange = { statusExpanded = !statusExpanded }
        ) {
            OutlinedTextField(
                value = status,
                onValueChange = {},
                readOnly = true,
                label = { Text("حالة الجلسة") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = statusExpanded,
                onDismissRequest = { statusExpanded = false }
            ) {
                statusOptions.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item) },
                        onClick = {
                            status = item
                            statusExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val boundCase = cases.getOrNull(selectedCaseIndex)
                if (boundCase != null) {
                    val toSave = CaseSession(
                        id = editing?.id ?: 0,
                        caseId = boundCase.id,
                        caseTitle = boundCase.title,
                        clientId = boundCase.clientId,
                        clientName = boundCase.clientName,
                        title = title,
                        court = boundCase.courtName,
                        courtCircle = boundCase.courtCircle,
                        date = date,
                        time = time,
                        requirements = requirements,
                        result = result,
                        status = status
                    )
                    viewModel.saveSession(toSave) {
                        viewModel.navigateTo(Screen.Dashboard)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary),
            enabled = date.isNotEmpty() && cases.isNotEmpty()
        ) {
            Text("حفظ وتأكيد الجلسة بالتقويم", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
