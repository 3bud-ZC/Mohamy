package com.example.ui.screens
import com.example.data.*
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.openCaseFile
import com.example.ui.theme.LegalGoldSecondary
import com.example.ui.theme.LegalNavyPrimary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: AppViewModel,
    files: List<CaseFile>,
    cases: List<LegalCase>,
    clients: List<Client>,
    sessions: List<CaseSession>,
    tasks: List<LegalTask>,
    templates: List<LegalTemplate>,
    generatedDocs: List<GeneratedDocument>
) {
    val context = LocalContext.current
    var queryTxt by remember { mutableStateOf("") }
    
    // Scopes: 0 = All files, 1 = Selected Case, 2 = Selected Client, 3 = One Specific File
    var selectedScope by remember { mutableStateOf(0) }
    
    // Binding parameters
    var boundCaseIndex by remember { mutableStateOf(0) }
    var caseDropdownExpanded by remember { mutableStateOf(false) }
    
    var boundClientIndex by remember { mutableStateOf(0) }
    var clientDropdownExpanded by remember { mutableStateOf(false) }
    
    var boundFileIndex by remember { mutableStateOf(0) }
    var fileDropdownExpanded by remember { mutableStateOf(false) }

    // Derive active bindings safely
    val activeCase = cases.getOrNull(boundCaseIndex)
    val activeClient = clients.getOrNull(boundClientIndex)
    
    // Files filtered by boundCaseId for "One Specific File" scope
    val caseSpecificFiles = remember(activeCase, files) {
        if (activeCase != null) files.filter { it.caseId == activeCase.id } else emptyList()
    }
    val activeFile = caseSpecificFiles.getOrNull(boundFileIndex)
    val normalizedQuery = remember(queryTxt) { viewModel.repository.normalizeArabic(queryTxt) }

    val matchedClients = remember(normalizedQuery, clients) {
        if (normalizedQuery.isEmpty()) {
            emptyList()
        } else {
            clients.filter { client ->
                val haystack = listOf(
                    client.name,
                    client.phone,
                    client.email,
                    client.nationalId,
                    client.address,
                    client.notes
                ).joinToString(" ")
                viewModel.repository.normalizeArabic(haystack).contains(normalizedQuery)
            }
        }
    }

    val matchedCases = remember(normalizedQuery, cases) {
        if (normalizedQuery.isEmpty()) {
            emptyList()
        } else {
            cases.filter { legalCase ->
                val haystack = listOf(
                    legalCase.title,
                    legalCase.caseNumber,
                    legalCase.caseYear,
                    legalCase.caseType,
                    legalCase.clientName,
                    legalCase.opponentName,
                    legalCase.courtName,
                    legalCase.courtCircle,
                    legalCase.summary,
                    legalCase.notes,
                    legalCase.status
                ).joinToString(" ")
                viewModel.repository.normalizeArabic(haystack).contains(normalizedQuery)
            }
        }
    }
    val matchedSessions = remember(normalizedQuery, sessions) {
        if (normalizedQuery.isEmpty()) {
            emptyList()
        } else {
            sessions.filter { session ->
                val haystack = listOf(
                    session.caseTitle,
                    session.title,
                    session.court,
                    session.courtCircle,
                    session.date,
                    session.time,
                    session.requirements,
                    session.result,
                    session.notes,
                    session.status
                ).joinToString(" ")
                viewModel.repository.normalizeArabic(haystack).contains(normalizedQuery)
            }
        }
    }
    val matchedTasks = remember(normalizedQuery, tasks) {
        if (normalizedQuery.isEmpty()) {
            emptyList()
        } else {
            tasks.filter { task ->
                val haystack = listOf(
                    task.title,
                    task.description,
                    task.dueDate,
                    task.priority,
                    task.status,
                    task.caseTitle ?: "",
                    task.clientName ?: ""
                ).joinToString(" ")
                viewModel.repository.normalizeArabic(haystack).contains(normalizedQuery)
            }
        }
    }
    val matchedTemplates = remember(normalizedQuery, templates) {
        if (normalizedQuery.isEmpty()) {
            emptyList()
        } else {
            templates.filter { template ->
                val haystack = listOf(
                    template.title,
                    template.category,
                    template.caseType,
                    template.description,
                    template.requiredFieldsJson,
                    template.templateBody
                ).joinToString(" ")
                viewModel.repository.normalizeArabic(haystack).contains(normalizedQuery)
            }
        }
    }
    val matchedGeneratedDocs = remember(normalizedQuery, generatedDocs) {
        if (normalizedQuery.isEmpty()) {
            emptyList()
        } else {
            generatedDocs.filter { doc ->
                val haystack = listOf(doc.documentTitle, doc.content, doc.filledFieldsJson).joinToString(" ")
                viewModel.repository.normalizeArabic(haystack).contains(normalizedQuery)
            }
        }
    }

    // Run local search filtering
    val matches = remember(normalizedQuery, selectedScope, activeCase, activeClient, activeFile, files) {
        files.filter { file ->
            // Filter by scope first
            val matchesScope = when (selectedScope) {
                0 -> true
                1 -> activeCase != null && file.caseId == activeCase.id
                2 -> activeClient != null && file.clientId == activeClient.id
                3 -> activeFile != null && file.id == activeFile.id
                else -> true
            }
            if (!matchesScope) return@filter false

            if (normalizedQuery.isEmpty()) {
                true // Display all for the selected scope if query is empty
            } else {
                // Check if match exists in normalizedSearchIndex or properties
                val indexMatches = file.normalizedSearchIndex.contains(normalizedQuery)
                val nameMatches = viewModel.repository.normalizeArabic(file.fileName).contains(normalizedQuery)
                val typeMatches = viewModel.repository.normalizeArabic(file.docType).contains(normalizedQuery)
                val textMatches = viewModel.repository.normalizeArabic(file.extractedText).contains(normalizedQuery)
                val caseMatches = viewModel.repository.normalizeArabic(file.caseTitle).contains(normalizedQuery)
                val clientMatches = viewModel.repository.normalizeArabic(file.clientName).contains(normalizedQuery)
                
                indexMatches || nameMatches || typeMatches || textMatches || caseMatches || clientMatches
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("الباحث الموحد وكشاف المستندات 🔍", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = LegalNavyPrimary)
        Spacer(modifier = Modifier.height(6.dp))
        Text("ابحث محلياً وبالمطابقة العربية المعقدة تحت أي ملف قضية، عريضة، أو مستند مؤرشف.", fontSize = 12.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))

        // Query input
        OutlinedTextField(
            value = queryTxt,
            onValueChange = { queryTxt = it },
            placeholder = { Text("اكتب مسمى المستند، اسم العميل، التصنيف، أو كلمات من قلب المستند...") },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = LegalGoldSecondary) },
            trailingIcon = {
                if (queryTxt.isNotEmpty()) {
                    IconButton(onClick = { queryTxt = "" }) {
                        Icon(Icons.Default.Close, null, tint = Color.Gray)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Scope chip selector title
        Text("نطاق ومجال البحث محلياً:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LegalNavyPrimary)
        Spacer(modifier = Modifier.height(8.dp))

        // Grid-like Row layout for Scope Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val scopesList = listOf("كل الملفات", "بالقضية", "بالعميل", "بملف معين")
            scopesList.forEachIndexed { idx, label ->
                val selected = selectedScope == idx
                Button(
                    onClick = { selectedScope = idx },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) LegalNavyPrimary else Color.LightGray.copy(alpha = 0.5f),
                        contentColor = if (selected) Color.White else Color.Black
                    ),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Context drop-downs forbound scope items
        when (selectedScope) {
            1 -> { // Search inside case
                if (cases.isEmpty()) {
                    Text("⚠️ لا توجد قضايا مضافة بالمكتب للتصفية عليها.", color = Color.Red, fontSize = 12.sp)
                } else {
                    ExposedDropdownMenuBox(
                        expanded = caseDropdownExpanded,
                        onExpandedChange = { caseDropdownExpanded = !caseDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = cases.getOrNull(boundCaseIndex)?.title ?: "اختر القضية للتصفية",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("اختر ملف القضية المستهدفة") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = caseDropdownExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = caseDropdownExpanded,
                            onDismissRequest = { caseDropdownExpanded = false }
                        ) {
                            cases.forEachIndexed { i, c ->
                                DropdownMenuItem(
                                    text = { Text(c.title) },
                                    onClick = {
                                        boundCaseIndex = i
                                        caseDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            2 -> { // Search inside client
                if (clients.isEmpty()) {
                    Text("⚠️ لا يوجد عملاء مسجلين للتصفية عليهم.", color = Color.Red, fontSize = 12.sp)
                } else {
                    ExposedDropdownMenuBox(
                        expanded = clientDropdownExpanded,
                        onExpandedChange = { clientDropdownExpanded = !clientDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = clients.getOrNull(boundClientIndex)?.name ?: "اختر العميل للتصفية",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("اختر اسم العميل المستهدف") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = clientDropdownExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = clientDropdownExpanded,
                            onDismissRequest = { clientDropdownExpanded = false }
                        ) {
                            clients.forEachIndexed { i, c ->
                                DropdownMenuItem(
                                    text = { Text(c.name) },
                                    onClick = {
                                        boundClientIndex = i
                                        clientDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            3 -> { // Search unique file in case
                if (cases.isEmpty()) {
                    Text("⚠️ لا توجد قضايا مضافة بالمكتب لاختيار ملفاتها.", color = Color.Red, fontSize = 12.sp)
                } else {
                    ExposedDropdownMenuBox(
                        expanded = caseDropdownExpanded,
                        onExpandedChange = { caseDropdownExpanded = !caseDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = cases.getOrNull(boundCaseIndex)?.title ?: "اختر القضية",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("1. حدد القضية المرتبطة") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = caseDropdownExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = caseDropdownExpanded,
                            onDismissRequest = { caseDropdownExpanded = false }
                        ) {
                            cases.forEachIndexed { i, c ->
                                DropdownMenuItem(
                                    text = { Text(c.title) },
                                    onClick = {
                                        boundCaseIndex = i
                                        boundFileIndex = 0
                                        caseDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    if (caseSpecificFiles.isEmpty()) {
                        Text("⚠️ لا توجد مسودة أو مستندات مرفوعة تحت ملف القضية المختار.", color = Color.Gray, fontSize = 12.sp)
                    } else {
                        ExposedDropdownMenuBox(
                            expanded = fileDropdownExpanded,
                            onExpandedChange = { fileDropdownExpanded = !fileDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = caseSpecificFiles.getOrNull(boundFileIndex)?.fileName ?: "اختر ملفاً معيناً",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("2. اختر المستند الفردي للبحث فيه") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fileDropdownExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = fileDropdownExpanded,
                                onDismissRequest = { fileDropdownExpanded = false }
                            ) {
                                caseSpecificFiles.forEachIndexed { i, f ->
                                    DropdownMenuItem(
                                        text = { Text(f.fileName) },
                                        onClick = {
                                            boundFileIndex = i
                                            fileDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        if (normalizedQuery.isNotEmpty()) {
            Text(
                text = "نتائج محلية: عملاء (${matchedClients.size}) | قضايا (${matchedCases.size}) | جلسات (${matchedSessions.size}) | مهام (${matchedTasks.size}) | قوالب (${matchedTemplates.size}) | مستندات مولدة (${matchedGeneratedDocs.size})",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = LegalNavyPrimary,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            if (matchedClients.isNotEmpty()) {
                Text("مطابقات العملاء", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = LegalNavyPrimary)
                matchedClients.forEach { client ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { viewModel.navigateTo(Screen.ClientDetails(client.id)) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        ListItem(
                            headlineContent = { Text(client.name, fontWeight = FontWeight.Bold) },
                            supportingContent = {
                                Text("هاتف: ${client.phone}${if (client.notes.isNotBlank()) " | ملاحظات متاحة" else ""}", fontSize = 12.sp)
                            },
                            trailingContent = { Icon(Icons.Default.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                    }
                }
            }

            if (matchedCases.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("مطابقات القضايا", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = LegalNavyPrimary)
                matchedCases.forEach { legalCase ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { viewModel.navigateTo(Screen.CaseDetails(legalCase.id)) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        ListItem(
                            headlineContent = { Text(legalCase.title, fontWeight = FontWeight.Bold) },
                            supportingContent = {
                                Text("رقم ${legalCase.caseNumber}/${legalCase.caseYear} | ${legalCase.caseType} | ${legalCase.clientName}", fontSize = 12.sp)
                            },
                            trailingContent = { Icon(Icons.Default.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                    }
                }
            }
            if (matchedSessions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("مطابقات الجلسات", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = LegalNavyPrimary)
                matchedSessions.forEach { session ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { viewModel.navigateTo(Screen.SessionAddEdit(sessionId = session.id)) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        ListItem(
                            headlineContent = { Text(session.title, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text("${session.caseTitle} | ${session.date} ${session.time}", fontSize = 12.sp) },
                            trailingContent = { Icon(Icons.Default.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                    }
                }
            }
            if (matchedTasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("مطابقات المهام", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = LegalNavyPrimary)
                matchedTasks.forEach { task ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { viewModel.navigateTo(Screen.TaskAddEdit(taskId = task.id)) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        ListItem(
                            headlineContent = { Text(task.title, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text("${task.caseTitle ?: "بدون قضية"} | استحقاق: ${task.dueDate}", fontSize = 12.sp) },
                            trailingContent = { Icon(Icons.Default.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                    }
                }
            }
            if (matchedTemplates.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("مطابقات القوالب", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = LegalNavyPrimary)
                matchedTemplates.forEach { template ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { viewModel.navigateTo(Screen.TemplateForm(template.id)) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        ListItem(
                            headlineContent = { Text(template.title, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text("${template.category} | ${template.caseType}", fontSize = 12.sp) },
                            trailingContent = { Icon(Icons.Default.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                    }
                }
            }
            if (matchedGeneratedDocs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("مطابقات المستندات المولدة", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = LegalNavyPrimary)
                matchedGeneratedDocs.forEach { doc ->
                    val linkedCase = cases.find { it.id == doc.caseId }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { viewModel.navigateTo(Screen.CaseDetails(doc.caseId)) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        ListItem(
                            headlineContent = { Text(doc.documentTitle, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text("قضية: ${linkedCase?.title ?: doc.caseId}", fontSize = 12.sp) },
                            trailingContent = { Icon(Icons.Default.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Section results count
        Text(
            text = "المطابقات المؤرشفة (${matches.size}):",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = LegalNavyPrimary,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        if (matches.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("لم تسفر التصفية والبحث عن مطابقات مطهرة للبحث الصوتي أو الدلالي حالياً.", color = Color.Gray, fontSize = 13.sp)
            }
        } else {
            // Display Results List directly inside the vertical Scroll Column using non-recycling loop for ease inside scroll state
            matches.forEach { file ->
                val snippet = remember(file.extractedText, queryTxt) {
                    if (file.extractedText.isNotEmpty()) {
                        getSnippetText(file.extractedText, queryTxt)
                    } else {
                        "مستند مؤرشف غير مدعوم للبحث في هذه النسخة - يمكنك كتابة نص يدوي في واجهة تعديل المستند لتفعيل الفهرسة."
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, LegalGoldSecondary.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FileCopy, null, tint = LegalNavyPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(file.fileName, fontWeight = FontWeight.Bold, color = LegalNavyPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(LegalGoldSecondary.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(file.docType, color = LegalNavyPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("ملف القضية: ${file.caseTitle}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
                        Text("العميل المرتبط: ${file.clientName}", fontSize = 12.sp, color = Color.Gray)
                        
                        // Snippet Card Preview
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("المطابقة من المتن:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = LegalGoldSecondary)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.LightGray.copy(alpha = 0.15f))
                                .padding(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        ) {
                            Text(snippet, fontSize = 12.sp, color = Color.Black, textAlign = TextAlign.Right)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { openCaseFile(context, file) },
                                colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("عرض وفتح الملف", fontSize = 11.sp)
                            }
                            Button(
                                onClick = {
                                    val boundCaseToSaveNote = cases.find { it.id == file.caseId }
                                    if (boundCaseToSaveNote != null) {
                                        val appendedNote = "\n\n[نتيجة بحث مؤرشف - ملف ${file.fileName} الأصلي (${file.docType})]:\n" +
                                            "تم توليد المذكرة من البحث في: \"$queryTxt\"\n" +
                                            "متن الجزء المطابق: $snippet"
                                        val uCase = boundCaseToSaveNote.copy(notes = boundCaseToSaveNote.notes + appendedNote)
                                        viewModel.saveCase(uCase) {
                                            Toast.makeText(context, "تم حفظ نتيجة المطابقة كمذكرة بملف القضية بنجاح!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LegalGoldSecondary),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Notes, null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("حفظ كمذكرة بالدعوى", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Snippet Utility function
fun getSnippetText(text: String, query: String): String {
    if (query.trim().isEmpty() || text.trim().isEmpty()) {
        return if (text.length > 80) text.take(80) + "..." else text
    }
    val normalizedText = text.lowercase(Locale.ROOT)
    val normalizedQuery = query.lowercase(Locale.ROOT)
    val idx = normalizedText.indexOf(normalizedQuery)
    if (idx == -1) {
        return if (text.length > 80) text.take(80) + "..." else text
    }
    val start = (idx - 30).coerceAtLeast(0)
    val end = (idx + normalizedQuery.length + 50).coerceAtMost(text.length)
    val prefix = if (start > 0) "..." else ""
    val suffix = if (end < text.length) "..." else ""
    return prefix + text.substring(start, end).trim().replace("\n", " ") + suffix
}
