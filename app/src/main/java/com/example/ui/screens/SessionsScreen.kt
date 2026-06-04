package com.example.ui.screens
import com.example.data.*
import java.util.Calendar
import java.util.Locale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LegalGoldLight
import com.example.ui.theme.LegalGoldSecondary
import com.example.ui.theme.LegalGrayLight
import com.example.ui.theme.LegalNavyPrimary
import com.example.ui.theme.legalScreenBackground

@Composable
fun SessionsListScreen(viewModel: AppViewModel, sessions: List<CaseSession>, cases: List<LegalCase>) {
    var searchTxt by remember { mutableStateOf("") }
    val normalizedSearch = remember(searchTxt) { viewModel.repository.normalizeArabic(searchTxt) }
    val filtered = sessions.filter {
        val haystack = viewModel.repository.normalizeArabic("${it.caseTitle} ${it.title} ${it.court} ${it.courtCircle} ${it.clientName} ${it.status}")
        normalizedSearch.isBlank() || haystack.contains(normalizedSearch)
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.navigateTo(Screen.SessionAddEdit()) },
                containerColor = LegalNavyPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.AddAlert, "إضافة جلسة")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .legalScreenBackground()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, LegalNavyPrimary.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("سجل الجلسات", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = LegalNavyPrimary)
                    Text("إجمالي الجلسات: ${sessions.size}", fontSize = 12.sp, color = Color.Gray)
                }
            }

            OutlinedTextField(
                value = searchTxt,
                onValueChange = { searchTxt = it },
                placeholder = { Text("البحث في الجلسات حسب الدعوى أو المحكمة...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Search, null) }
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("لا توجد أي جلسات مسجلة حالياً.", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filtered) { ses ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable { viewModel.navigateTo(Screen.SessionAddEdit(sessionId = ses.id)) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                leadingContent = {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(LegalGrayLight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Event,
                                            contentDescription = null,
                                            tint = LegalNavyPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                },
                                headlineContent = { Text(ses.caseTitle, fontWeight = FontWeight.Bold, color = LegalNavyPrimary, fontSize = 16.sp) },
                                supportingContent = { 
                                    Column(modifier = Modifier.padding(top = 4.dp)) {
                                        Text("موضوع: ${ses.title}", color = Color.DarkGray, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("${ses.date} في ${ses.time}", color = Color.Gray, fontSize = 12.sp) 
                                        }
                                    }
                                },
                                trailingContent = {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(LegalGoldSecondary.copy(alpha = 0.15f))
                                                .border(1.dp, LegalGoldSecondary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(ses.status, color = LegalNavyPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        TextButton(
                                            onClick = { viewModel.navigateTo(Screen.SessionAddEdit(sessionId = ses.id)) },
                                            contentPadding = PaddingValues(0.dp),
                                            modifier = Modifier.height(20.dp)
                                        ) {
                                            Text("تعديل", color = LegalNavyPrimary, fontSize = 11.sp)
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        TextButton(
                                            onClick = { viewModel.deleteSession(ses) },
                                            contentPadding = PaddingValues(0.dp),
                                            modifier = Modifier.height(20.dp)
                                        ) {
                                            Text("حذف الجلسة", color = Color.Red, fontSize = 11.sp)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
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
                        imageVector = if (editing == null) Icons.Default.AddAlert else Icons.Default.Edit,
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
                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f)),
                colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.05f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = Color.Red)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("لا توجد قضايا نشطة مضافة بالمكتب حالياً لجدولة الجلسة عليها.", color = Color.Red, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LegalGoldSecondary, focusedLabelColor = LegalNavyPrimary)
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
            label = { Text("القرار والنتيجة (في حل انتهت)") },
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
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary),
            enabled = date.isNotEmpty() && cases.isNotEmpty()
        ) {
            Text("حفظ وتأكيد الجلسة بالتقويم", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
