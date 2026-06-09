package com.example.ui.screens
import com.example.data.*
import android.graphics.BitmapFactory
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.formatFileSize
import com.example.ui.openCaseFile
import kotlinx.coroutines.delay

private fun casePriorityRank(priority: String): Int = when (priority) {
    "عالية" -> 0
    "متوسطة" -> 1
    else -> 2
}

@Composable
private fun FeeSummaryCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, LegalNavyPrimary.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = LegalNavyPrimary)
            Text(title, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@Composable
fun CasesListScreen(
    viewModel: AppViewModel,
    activeCases: List<LegalCase>,
    archivedCases: List<LegalCase>,
    customCategories: List<CustomCaseCategory>
) {
    var searchTxt by remember { mutableStateOf("") }
    var showingArchived by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("الكل") }
    val normalizedSearch = remember(searchTxt) { viewModel.repository.normalizeArabic(searchTxt) }

    val allCategories = remember(customCategories) {
        (listOf("الكل") + viewModel.repository.fixedCaseCategories() + customCategories.map { it.name.trim() })
            .filter { it.isNotBlank() }
            .distinctBy { viewModel.repository.normalizeArabic(it) }
    }

    val listToFilter = if (showingArchived) archivedCases else activeCases
    val filtered = remember(listToFilter, normalizedSearch, selectedCategory) {
        listToFilter
            .filter {
                val searchableText = viewModel.repository.normalizeArabic(
                    listOf(
                        it.title,
                        it.caseNumber,
                        it.caseYear,
                        it.opponentName,
                        it.clientName,
                        it.courtName,
                        it.courtCircle,
                        it.summary,
                        it.status,
                        it.caseType
                    ).joinToString(" ")
                )
                val matchesSearch = normalizedSearch.isBlank() || searchableText.contains(normalizedSearch)
                val matchesCategory = selectedCategory == "الكل" ||
                    viewModel.repository.normalizeArabic(it.caseType) == viewModel.repository.normalizeArabic(selectedCategory)
                matchesSearch && matchesCategory
            }
            .sortedWith(
                compareBy<LegalCase> { casePriorityRank(it.priority) }
                    .thenBy { if (showingArchived) "9999-12-31" else it.nextSessionDate.ifBlank { "9999-12-31" } }
                    .thenByDescending { it.createdDate }
            )
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            if (!showingArchived) {
                FloatingActionButton(
                    onClick = { viewModel.navigateTo(Screen.CaseAddEdit()) },
                    containerColor = LegalNavyPrimary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, "إضافة قضية")
                }
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
                    Text("ملفات القضايا", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = LegalNavyPrimary)
                    Text(
                        "نشطة: ${activeCases.size} | مؤرشفة: ${archivedCases.size}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            // Tab Header select active vs archived
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showingArchived = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!showingArchived) LegalNavyPrimary else Color.LightGray,
                        contentColor = if (!showingArchived) Color.White else Color.Black
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("القضايا النشطة")
                }
                Button(
                    onClick = { showingArchived = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showingArchived) LegalNavyPrimary else Color.LightGray,
                        contentColor = if (showingArchived) Color.White else Color.Black
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("مكتبة الأرشيف")
                }
            }

            OutlinedTextField(
                value = searchTxt,
                onValueChange = { searchTxt = it },
                placeholder = { Text("ابحث برقم القضية أو المحكمة أو الخصم...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Search, null) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allCategories.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LegalNavyPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "النتائج الحالية: ${filtered.size}",
                fontSize = 12.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("لم يتم العثور على أي قضايا مسجلة.", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filtered) { item ->
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
                                leadingContent = {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(LegalGrayLight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Gavel,
                                            contentDescription = null,
                                            tint = LegalNavyPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                },
                                headlineContent = { Text(item.title, fontWeight = FontWeight.Bold, color = LegalNavyPrimary, fontSize = 16.sp) },
                                supportingContent = { 
                                    val readiness = viewModel.caseReadinessScore(item)
                                    Column(modifier = Modifier.padding(top = 4.dp)) {
                                        Text("دعوى ${item.caseType} | رقم ${item.caseNumber} لسنة ${item.caseYear}", color = Color.Gray, fontSize = 13.sp) 
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("الخصم: ${item.opponentName}", color = Color.DarkGray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("جاهزية الملف: $readiness% - ${viewModel.caseReadinessLabel(item)}", color = Color.DarkGray, fontSize = 12.sp)
                                    }
                                },
                                trailingContent = {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                when (item.priority) {
                                                    "عالية" -> Color.Red.copy(alpha = 0.1f)
                                                    else -> LegalGoldSecondary.copy(alpha = 0.15f)
                                                }
                                            )
                                            .border(
                                                1.dp,
                                                when (item.priority) {
                                                    "عالية" -> Color.Red.copy(alpha = 0.3f)
                                                    else -> LegalGoldSecondary.copy(alpha = 0.3f)
                                                },
                                                RoundedCornerShape(12.dp)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(item.status, color = LegalNavyPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

@Composable
fun CaseDetailsScreen(
    caseId: Int,
    viewModel: AppViewModel,
    cases: List<LegalCase>,
    sessions: List<CaseSession>,
    tasks: List<LegalTask>,
    files: List<CaseFile>,
    generatedDocs: List<GeneratedDocument>,
    fileImportLauncher: androidx.activity.result.ActivityResultLauncher<String>
) {
    val context = LocalContext.current
    val legalCase = cases.find { it.id == caseId } ?: viewModel.archivedCases.value.find { it.id == caseId }
    val caseSessions = sessions.filter { it.caseId == caseId }
    val caseTasks = tasks.filter { it.caseId == caseId }
    val caseFiles = files.filter { it.caseId == caseId }
    val caseDocs = generatedDocs.filter { it.caseId == caseId }
    val feeRecords by viewModel.allFeeRecords.collectAsState(initial = emptyList())
    val caseFees = feeRecords.filter { it.caseId == caseId }

    if (legalCase == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("الملف غير عثور عليه")
        }
        return
    }

    // Capture active case state in View Model for imports/actions
    DisposableEffect(caseId) {
        viewModel.activeCase = legalCase
        onDispose { viewModel.activeCase = null }
    }

    var activeTab by remember { mutableStateOf(0) }
    var smartAssistantResult by remember { mutableStateOf<String?>(null) }
    var smartAssistantShowing by remember { mutableStateOf(false) }
    var assistantFileQuery by remember { mutableStateOf("") }
    var assistantMatchedFileId by remember { mutableStateOf<Int?>(null) }

    var activeEditingFile by remember { mutableStateOf<CaseFile?>(null) }
    var fileManualTextInput by remember { mutableStateOf("") }
    var fileDetailDialogShowing by remember { mutableStateOf(false) }
    var feeTitle by remember(caseId) { mutableStateOf("") }
    var feeTotalText by remember(caseId) { mutableStateOf("") }
    var feePaidText by remember(caseId) { mutableStateOf("") }
    var feeDueDate by remember(caseId) { mutableStateOf("") }
    var feeStatus by remember(caseId) { mutableStateOf("مستحقة") }
    var feeCurrency by remember(caseId) { mutableStateOf("ج.م") }
    var feeNotes by remember(caseId) { mutableStateOf("") }
    val readinessScore = viewModel.caseReadinessScore(legalCase)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .legalScreenBackground()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Case Header panel bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = LegalNavyPrimary),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(LegalGoldSecondary.copy(alpha = 0.2f))
                            .border(2.dp, LegalGoldSecondary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Gavel, null, tint = LegalGoldLight, modifier = Modifier.size(26.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(legalCase.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("موكل القضية: ${legalCase.clientName}", fontSize = 14.sp, color = LegalGoldLight)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(LegalGoldSecondary)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(legalCase.status, color = LegalNavyPrimary, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ConfirmationNumber, null, tint = LegalGoldSecondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("رقم القضية: ${legalCase.caseNumber} لسنة ${legalCase.caseYear}", fontSize = 13.sp, color = Color.White.copy(alpha = 0.9f))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalance, null, tint = LegalGoldSecondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("المحكمة: ${legalCase.courtName}", fontSize = 13.sp, color = Color.White.copy(alpha = 0.9f))
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Category, null, tint = LegalGoldSecondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("النوع: ${legalCase.caseType}", fontSize = 13.sp, color = Color.White.copy(alpha = 0.9f))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PriorityHigh, null, tint = LegalGoldSecondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("الأهمية: ${legalCase.priority}", fontSize = 13.sp, color = Color.White.copy(alpha = 0.9f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Groups, null, tint = LegalGoldSecondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("الدائرة: ${legalCase.courtCircle}", fontSize = 13.sp, color = Color.White.copy(alpha = 0.9f))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("جاهزية القضية: $readinessScore% - ${viewModel.caseReadinessLabel(legalCase)}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = readinessScore / 100f,
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(99.dp)),
                    color = LegalGoldSecondary,
                    trackColor = Color.White.copy(alpha = 0.18f)
                )
            }
        }

        // Action Toolbar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.navigateTo(Screen.CaseAddEdit(legalCase.id)) },
                    colors = ButtonDefaults.buttonColors(containerColor = LegalGoldSecondary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Icon(Icons.Default.Edit, "تعديل", tint = LegalNavyPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تعديل", color = LegalNavyPrimary, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        val archiveState = !legalCase.isArchived
                        viewModel.saveCase(legalCase.copy(isArchived = archiveState)) {
                            viewModel.navigateTo(Screen.CasesList)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (legalCase.isArchived) LegalNavyPrimary else LegalSlateDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Icon(if (legalCase.isArchived) Icons.Default.Unarchive else Icons.Default.Archive, "أرشفة", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (legalCase.isArchived) "تنشيط" else "أرشفة", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        viewModel.exportCaseBundle(legalCase) { bundle ->
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                bundle
                            )
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/zip"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "تصدير ملف القضية الكامل"))
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.FolderZip, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تصدير ZIP", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val uri = com.example.util.PdfExporter.exportCaseSummary(
                            context, legalCase, legalCase.clientName, caseSessions, caseTasks
                        )
                        if (uri != null) {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "مشاركة ملخص القضية"))
                        } else {
                            Toast.makeText(context, "فشل تصدير الـ PDF", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LegalGoldSecondary, contentColor = LegalNavyPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تصدير PDF", fontWeight = FontWeight.Bold)
                }
            }
        }

        // المساعد الذكي Section Trigger
        Text("مساعد القضية", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(vertical = 8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LegalGoldSecondary.copy(alpha = 0.08f)),
            border = BorderStroke(1.dp, LegalGoldSecondary.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "مساعد محلي فوري يستقصي مستنداتك ويصوغ متطلباتك دون اتصال ودون تكلفة مالية.\n⚠️ تنبيه: المساعد الذكي يساعد في تنظيم البيانات والبحث داخل الملفات وتجهيز القوالب، ولا يعتبر رأيًا قانونيًا نهائيًا.",
                    fontSize = 12.sp,
                    color = LegalNavyPrimary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (viewModel.isAssistantLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = LegalNavyPrimary, trackColor = LegalGoldSecondary.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = {
                            viewModel.getSmartAssistantSummary(legalCase.id) {
                                assistantMatchedFileId = null
                                smartAssistantResult = it
                                smartAssistantShowing = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("ملخص القضية", fontSize = 10.sp)
                    }
                    Button(
                        onClick = {
                            viewModel.getMissingDocumentsSuggestion(legalCase.id) {
                                assistantMatchedFileId = null
                                smartAssistantResult = it
                                smartAssistantShowing = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("مستندات ناقصة", fontSize = 10.sp)
                    }
                    Button(
                        onClick = {
                            viewModel.getNextSessionAssistant(legalCase.id) {
                                assistantMatchedFileId = null
                                smartAssistantResult = it
                                smartAssistantShowing = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("الجلسة القادمة", fontSize = 10.sp)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = {
                            viewModel.getOpenTasksAssistant(legalCase.id) {
                                assistantMatchedFileId = null
                                activeTab = 1
                                smartAssistantResult = it
                                smartAssistantShowing = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("المهام المفتوحة", fontSize = 10.sp)
                    }
                    Button(
                        onClick = {
                            viewModel.getSuggestedTemplatesForCase(legalCase.caseType) {
                                assistantMatchedFileId = null
                                smartAssistantResult = it
                                smartAssistantShowing = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("قوالب مناسبة", fontSize = 10.sp)
                    }
                    Button(
                        onClick = {
                            viewModel.triggerSmartChecklist(legalCase.id, legalCase.caseType) {
                                assistantMatchedFileId = null
                                smartAssistantResult = it
                                smartAssistantShowing = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("قائمة التحقق", fontSize = 10.sp)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = {
                            viewModel.getSessionPrepAssistant(legalCase.id) {
                                assistantMatchedFileId = null
                                smartAssistantResult = it
                                smartAssistantShowing = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("تجهيز الجلسة", fontSize = 10.sp)
                    }
                    Button(
                        onClick = {
                            viewModel.draftCaseMemo(legalCase.id) {
                                assistantMatchedFileId = null
                                smartAssistantResult = it
                                smartAssistantShowing = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("مسودة مذكرة", fontSize = 10.sp)
                    }
                    Button(
                        onClick = {
                            viewModel.answerCaseQuestion(legalCase.id, "readiness") {
                                assistantMatchedFileId = null
                                smartAssistantResult = it
                                smartAssistantShowing = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("سؤال: الجاهزية", fontSize = 10.sp)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = {
                            viewModel.answerCaseQuestion(legalCase.id, "last_session") {
                                assistantMatchedFileId = null
                                smartAssistantResult = it
                                smartAssistantShowing = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("آخر جلسة", fontSize = 10.sp)
                    }
                    Button(
                        onClick = {
                            viewModel.answerCaseQuestion(legalCase.id, "opponent") {
                                assistantMatchedFileId = null
                                smartAssistantResult = it
                                smartAssistantShowing = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("سؤال: الخصم", fontSize = 10.sp)
                    }
                    Button(
                        onClick = {
                            viewModel.answerCaseQuestion(legalCase.id, "missing_docs") {
                                assistantMatchedFileId = null
                                smartAssistantResult = it
                                smartAssistantShowing = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("سؤال: النواقص", fontSize = 10.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = assistantFileQuery,
                    onValueChange = { assistantFileQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("ابحث داخل ملفات القضية...") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = {
                            viewModel.searchInsideCaseFiles(legalCase.id, assistantFileQuery) { result, fileId ->
                                assistantMatchedFileId = fileId
                                smartAssistantResult = result
                                smartAssistantShowing = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("بحث في الملفات", fontSize = 10.sp)
                    }
                    Button(
                        onClick = {
                            viewModel.navigateTo(Screen.LegalTemplatesList)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("فتح القوالب", fontSize = 10.sp)
                    }
                }
            }
        }

        // Sub tabs of Case Sessions, Tasks, Files, generated templates, custom rich notes
        Spacer(modifier = Modifier.height(16.dp))
        ScrollableTabRow(
            selectedTabIndex = activeTab,
            containerColor = Color.Transparent,
            contentColor = LegalNavyPrimary,
            edgePadding = 0.dp
        ) {
            Tab(selected = activeTab == 0, onClick = { activeTab = 0 }) { Text("الجلسات (${caseSessions.size})", modifier = Modifier.padding(12.dp), fontSize = 13.sp) }
            Tab(selected = activeTab == 1, onClick = { activeTab = 1 }) { Text("المهام (${caseTasks.size})", modifier = Modifier.padding(12.dp), fontSize = 13.sp) }
            Tab(selected = activeTab == 2, onClick = { activeTab = 2 }) { Text("المستندات (${caseFiles.size})", modifier = Modifier.padding(12.dp), fontSize = 13.sp) }
            Tab(selected = activeTab == 3, onClick = { activeTab = 3 }) { Text("المستندات المولدة (${caseDocs.size})", modifier = Modifier.padding(12.dp), fontSize = 13.sp) }
            Tab(selected = activeTab == 4, onClick = { activeTab = 4 }) { Text("مذكرة القضية", modifier = Modifier.padding(12.dp), fontSize = 13.sp) }
            Tab(selected = activeTab == 5, onClick = { activeTab = 5 }) { Text("الأتعاب (${caseFees.size})", modifier = Modifier.padding(12.dp), fontSize = 13.sp) }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (activeTab) {
            0 -> { // SESSIONS TAB
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("الجلسات المجدولة", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { viewModel.navigateTo(Screen.SessionAddEdit(presetCaseId = legalCase.id)) }) {
                        Icon(Icons.Default.AddAlarm, null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة جلسة")
                    }
                }
                if (caseSessions.isEmpty()) {
                    Text("لا توجد جلسات مجدولة مسبقاً لهذه القضية.", color = Color.Gray, fontSize = 13.sp)
                } else {
                    caseSessions.forEach { ses ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(0.5.dp, Color.LightGray)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row {
                                    Text(ses.title, fontWeight = FontWeight.Bold, color = LegalNavyPrimary)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(ses.date + " " + ses.time, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                                if (ses.requirements.isNotEmpty()) {
                                    Text("المطالب: " + ses.requirements, fontSize = 12.sp, color = Color.DarkGray)
                                }
                                if (ses.result.isNotEmpty()) {
                                    Text("النتيجة والقرار: " + ses.result, fontSize = 12.sp, color = LegalGoldSecondary, fontWeight = FontWeight.Bold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    TextButton(onClick = { viewModel.deleteSession(ses) }) {
                                        Text("حذف", color = Color.Red)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            1 -> { // TASKS TAB
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("المهام المطلوبة من المكتب", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { viewModel.navigateTo(Screen.TaskAddEdit(presetCaseId = legalCase.id)) }) {
                        Icon(Icons.Default.Add, null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة مهمة")
                    }
                }
                if (caseTasks.isEmpty()) {
                    Text("لا يوجد مهام خاصة بملف القضية.", color = Color.Gray, fontSize = 13.sp)
                } else {
                    caseTasks.forEach { tsk ->
                        val isDone = tsk.status == "منتهية"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDone) Color.LightGray.copy(alpha = 0.3f) else Color.Transparent)
                                .padding(8.dp)
                        ) {
                            Checkbox(checked = isDone, onCheckedChange = { viewModel.toggleTaskCompleted(tsk) })
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tsk.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isDone) Color.Gray else Color.Black
                                )
                                Text("تاريخ السداد: ${tsk.dueDate} | الأهمية: ${tsk.priority}", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
            2 -> { // DOCUMENTS IMPORT/FILES TAB
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("الأوراق والمستندات المشفرة", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = { fileImportLauncher.launch("*/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary)
                    ) {
                        Icon(Icons.Default.CloudUpload, null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("استيراد مستند")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("سيتم حفظ ونسخ الأوراق داخل مساحة التطبيق المشفرة والخاصة بملفاتك. انقر على أي ملف لعرض خيارات الفتح الأوتوماتيكي وتعديل فهرس وملاحظات البحث يدوياً.", fontSize = 11.sp, color = Color.Gray)

                if (caseFiles.isEmpty()) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("لم يتم أرشفة أو إرفاق مستندات بعد في هذا الملف.", color = Color.Gray, fontSize = 13.sp)
                    }
                } else {
                    caseFiles.forEach { f ->
                        val linkedSession = caseSessions.find { it.id == f.linkedSessionId }
                        val fileAccent = parseHexColorOrDefault(f.accentColorHex, LegalNavyPrimary)
                        val fileShape = caseFileShape(f.cardStyle)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    activeEditingFile = f
                                    fileManualTextInput = f.extractedText
                                    fileDetailDialogShowing = true
                                },
                            shape = fileShape,
                            colors = CardDefaults.cardColors(containerColor = fileAccent.copy(alpha = 0.08f)),
                            border = BorderStroke(1.dp, fileAccent.copy(alpha = 0.22f))
                        ) {
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                leadingContent = {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(fileAccent.copy(alpha = 0.14f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(imageVector = Icons.Default.FileCopy, contentDescription = "ملف", tint = fileAccent)
                                    }
                                },
                                headlineContent = { Text(f.fileName, fontWeight = FontWeight.Bold) },
                                supportingContent = {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("التصنيف: ${f.docType} | الحجم: ${formatFileSize(f.fileLength)}")
                                        if (linkedSession != null) {
                                            Text("مرتبط بجلسة: ${linkedSession.title} - ${linkedSession.date}", fontSize = 11.sp, color = Color.Gray)
                                        }
                                        Text("الهوية المرئية: ${f.cardStyle} | ${f.accentColorHex}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                },
                                trailingContent = {
                                    IconButton(onClick = { viewModel.deleteFile(f) }) {
                                        Icon(Icons.Default.Delete, "حذف", tint = Color.Red)
                                    }
                                }
                            )
                        }
                    }
                }

                if (fileDetailDialogShowing && activeEditingFile != null) {
                    val editingF = activeEditingFile!!
                    val linkedSession = caseSessions.find { it.id == editingF.linkedSessionId }
                    AlertDialog(
                        onDismissRequest = { fileDetailDialogShowing = false },
                        title = {
                            Text(
                                "بيانات وأرشفة المستند 📄",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = LegalNavyPrimary
                            )
                        },
                        text = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                var editingAccent by remember(editingF.id) { mutableStateOf(editingF.accentColorHex) }
                                var editingShape by remember(editingF.id) { mutableStateOf(editingF.cardStyle) }
                                ListItem(
                                    headlineContent = { Text("اسم المستند الأصلي", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray) },
                                    supportingContent = { Text(editingF.fileName, fontWeight = FontWeight.SemiBold) }
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("نوع المستند", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
                                        Text(editingF.docType, fontWeight = FontWeight.SemiBold)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("حجم الملف", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
                                        Text(formatFileSize(editingF.fileLength), fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                if (linkedSession != null) {
                                    Text("الجلسة المرتبطة", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
                                    Text("${linkedSession.title} - ${linkedSession.date}", fontWeight = FontWeight.SemiBold)
                                }
                                Text("حالة استخراج وفهرسة النص تلقائياً:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)
                                if (editingF.extractionStatus.contains("جاهز") || editingF.extractedText.isNotBlank()) {
                                    Text(editingF.extractionStatus, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                } else {
                                    Text("${editingF.extractionStatus} - يمكنك كتابة نص يدوي للفهرسة بالأسفل.", color = Color(0xFFD84315), fontSize = 12.sp)
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text("الفهرس النصي ومفتاح البحث وملاحظاتك:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = LegalNavyPrimary)
                                OutlinedTextField(
                                    value = fileManualTextInput,
                                    onValueChange = { fileManualTextInput = it },
                                    modifier = Modifier.fillMaxWidth().height(120.dp),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                    placeholder = { Text("اكتب محتوى المستند أو كلمات وملاحظات هامة للبحث عنها لاحقاً...") }
                                )

                                Text("الهوية المرئية للمستند:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = LegalNavyPrimary)
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CaseFileStylePalette.colors.forEach { colorHex ->
                                        val chipColor = parseHexColorOrDefault(colorHex)
                                        FilterChip(
                                            selected = editingAccent == colorHex,
                                            onClick = { editingAccent = colorHex },
                                            label = {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(14.dp)
                                                            .clip(CircleShape)
                                                            .background(chipColor)
                                                    )
                                                    Text(colorHex, fontSize = 11.sp)
                                                }
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = chipColor.copy(alpha = 0.12f),
                                                selectedLabelColor = chipColor
                                            )
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CaseFileStylePalette.styles.forEach { style ->
                                        FilterChip(
                                            selected = editingShape == style,
                                            onClick = { editingShape = style },
                                            label = {
                                                Text(
                                                    when (style) {
                                                        "paper" -> "ورقي"
                                                        "sharp" -> "حاد"
                                                        else -> "مقوس"
                                                    },
                                                    fontSize = 11.sp
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = LegalNavyPrimary,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        viewModel.updateFileManualText(
                                            editingF,
                                            fileManualTextInput,
                                            editingAccent,
                                            editingShape
                                        ) {
                                            fileDetailDialogShowing = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = LegalGoldSecondary),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("حفظ وتحديث كشاف البحث اليدوي")
                                }
                                OutlinedButton(
                                    onClick = {
                                        val appearance = defaultCaseFileStyle(editingF.docType, editingF.fileName)
                                        editingAccent = appearance.accentColorHex
                                        editingShape = appearance.cardStyle
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("استعادة الهوية الافتراضية")
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    openCaseFile(context, editingF)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("عرض وفتح الملف")
                            }
                        },
                        dismissButton = {
                            OutlinedButton(
                                onClick = { fileDetailDialogShowing = false },
                                border = BorderStroke(1.dp, Color.LightGray)
                            ) {
                                Text("إغلاق", color = Color.DarkGray)
                            }
                        }
                    )
                }
            }
            3 -> { // GENERATED DOCS/CONTRACTS TAB
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("المستندات المولدة من القوالب", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { viewModel.navigateTo(Screen.LegalTemplatesList) }) {
                        Icon(imageVector = Icons.Default.Create, contentDescription = "صياغة")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("صياغة نموذج")
                    }
                }
                if (caseDocs.isEmpty()) {
                    Text("لا يوجد مستندات مولدة لهذه القضية.", color = Color.Gray, fontSize = 13.sp)
                } else {
                    caseDocs.forEach { doc ->
                        var isExpanded by remember { mutableStateOf(false) }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Description, contentDescription = "مستند", tint = LegalNavyPrimary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(doc.documentTitle, fontWeight = FontWeight.Bold, color = LegalNavyPrimary)
                                    Spacer(modifier = Modifier.weight(1f))
                                    IconButton(onClick = { isExpanded = !isExpanded }) {
                                        Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, "توسعة")
                                    }
                                }
                                if (isExpanded) {
                                    Text(
                                        text = doc.content,
                                        fontSize = 12.sp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.White)
                                            .padding(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                val cManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                cManager.setPrimaryClip(ClipData.newPlainText("محامي فون مستند", doc.content))
                                                Toast.makeText(context, "تم نسخ النص بنجاح!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary)
                                        ) {
                                            Text("نسخ النص")
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                DocumentExportManager.exportToWord(context, doc.documentTitle, doc.content)
                                                    .onSuccess {
                                                        Toast.makeText(context, "تم حفظ نسخة Word: ${it.name}", Toast.LENGTH_LONG).show()
                                                    }
                                                    .onFailure {
                                                        Toast.makeText(context, it.message ?: "فشل تصدير Word", Toast.LENGTH_SHORT).show()
                                                    }
                                            }
                                        ) {
                                            Text("Word")
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                DocumentExportManager.exportToPdf(context, doc.documentTitle, doc.content)
                                                    .onSuccess {
                                                        Toast.makeText(context, "تم حفظ نسخة PDF: ${it.name}", Toast.LENGTH_LONG).show()
                                                    }
                                                    .onFailure {
                                                        Toast.makeText(context, it.message ?: "فشل تصدير PDF", Toast.LENGTH_SHORT).show()
                                                    }
                                            }
                                        ) {
                                            Text("PDF")
                                        }
                                        OutlinedButton(
                                            onClick = { viewModel.deleteGeneratedDocument(doc) }
                                        ) {
                                            Text("حذف", color = Color.Red)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            4 -> { // RICH MANUAL NOTES FOR THE LAWYER
                var notesText by remember(legalCase.id) { mutableStateOf(legalCase.notes) }
                var lastSavedNotes by remember(legalCase.id) { mutableStateOf(legalCase.notes) }

                LaunchedEffect(notesText, legalCase.id) {
                    if (notesText == lastSavedNotes) return@LaunchedEffect
                    delay(600)
                    if (notesText != lastSavedNotes) {
                        lastSavedNotes = notesText
                        viewModel.saveCase(legalCase.copy(notes = notesText))
                    }
                }

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("أكتب ملاحظاتك، مرافعاتك وأقوالك بالدعوى وسيقوم التطبيق بحفظها تلقائياً بالملف هنا...") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "يتم حفظ الملاحظات محلياً بعد توقف الكتابة للحظات لتقليل الضغط على قاعدة البيانات.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            5 -> { // FEES TRACKING
                val totalAgreed = caseFees.sumOf { it.totalAmount }
                val totalPaid = caseFees.sumOf { it.paidAmount }
                val totalOutstanding = (totalAgreed - totalPaid).coerceAtLeast(0.0)
                val overdueCount = caseFees.count { fee ->
                    val due = try {
                        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ENGLISH).parse(fee.dueDate)?.time
                    } catch (_: Exception) {
                        null
                    }
                    due != null && due < System.currentTimeMillis() && fee.paidAmount < fee.totalAmount && fee.status != "مدفوعة"
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FeeSummaryCard(title = "إجمالي الأتعاب", value = String.format(java.util.Locale.ENGLISH, "%.2f", totalAgreed), modifier = Modifier.weight(1f))
                    FeeSummaryCard(title = "المدفوع", value = String.format(java.util.Locale.ENGLISH, "%.2f", totalPaid), modifier = Modifier.weight(1f))
                    FeeSummaryCard(title = "المتبقي", value = String.format(java.util.Locale.ENGLISH, "%.2f", totalOutstanding), modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "السجلات المتأخرة: $overdueCount | عدد السجلات: ${caseFees.size}",
                    fontWeight = FontWeight.Bold,
                    color = LegalNavyPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, LegalNavyPrimary.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("إضافة سجل أتعاب", fontWeight = FontWeight.Bold, color = LegalNavyPrimary)
                        OutlinedTextField(
                            value = feeTitle,
                            onValueChange = { feeTitle = it },
                            label = { Text("عنوان السجل") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = feeTotalText,
                                onValueChange = { feeTotalText = it },
                                label = { Text("الإجمالي") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = feePaidText,
                                onValueChange = { feePaidText = it },
                                label = { Text("المدفوع") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = feeDueDate,
                                onValueChange = { feeDueDate = it },
                                label = { Text("تاريخ الاستحقاق YYYY-MM-DD") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = feeCurrency,
                                onValueChange = { feeCurrency = it },
                                label = { Text("العملة") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedTextField(
                            value = feeNotes,
                            onValueChange = { feeNotes = it },
                            label = { Text("ملاحظات") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                        Button(
                            onClick = {
                                val total = feeTotalText.toDoubleOrNull() ?: 0.0
                                val paid = feePaidText.toDoubleOrNull() ?: 0.0
                                val status = when {
                                    paid <= 0.0 -> feeStatus
                                    paid >= total && total > 0.0 -> "مدفوعة"
                                    paid > 0.0 -> "جزئية"
                                    else -> feeStatus
                                }
                                viewModel.saveFeeRecord(
                                    FeeRecord(
                                        clientId = legalCase.clientId,
                                        clientName = legalCase.clientName,
                                        caseId = legalCase.id,
                                        caseTitle = legalCase.title,
                                        title = feeTitle.ifBlank { "أتعاب القضية" },
                                        totalAmount = total,
                                        paidAmount = paid,
                                        currency = feeCurrency.ifBlank { "ج.م" },
                                        dueDate = feeDueDate,
                                        status = status,
                                        notes = feeNotes
                                    )
                                ) {
                                    feeTitle = ""
                                    feeTotalText = ""
                                    feePaidText = ""
                                    feeDueDate = ""
                                    feeCurrency = "ج.م"
                                    feeNotes = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("حفظ سجل الأتعاب")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                if (caseFees.isEmpty()) {
                    Text("لا توجد سجلات أتعاب لهذه القضية حالياً.", color = Color.Gray)
                } else {
                    caseFees.forEach { fee ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color.LightGray)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(fee.title, fontWeight = FontWeight.Bold, color = LegalNavyPrimary)
                                    Text(fee.status, color = LegalGoldSecondary, fontWeight = FontWeight.Bold)
                                }
                                Text("الإجمالي: ${fee.totalAmount} ${fee.currency} | المدفوع: ${fee.paidAmount} ${fee.currency}", fontSize = 12.sp)
                                if (fee.dueDate.isNotBlank()) {
                                    Text("الاستحقاق: ${fee.dueDate}", fontSize = 12.sp, color = Color.Gray)
                                }
                                if (fee.notes.isNotBlank()) {
                                    Text(fee.notes, fontSize = 12.sp, color = Color.DarkGray)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { viewModel.deleteFeeRecord(fee) }) {
                                        Text("حذف", color = Color.Red)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dialog for Smart Assistant Outlets results offline
        if (smartAssistantShowing && smartAssistantResult != null) {
            AlertDialog(
                onDismissRequest = { smartAssistantShowing = false },
                title = { Text("المساعد القضائي الذكي 🤖", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = LegalNavyPrimary) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(smartAssistantResult!!, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "⚠️ تنبيه: المساعد الذكي يساعد في تنظيم البيانات والبحث داخل الملفات وتجهيز القوالب، ولا يعتبر رأيًا قانونيًا نهائيًا.",
                            color = Color.Red,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val matchedFile = caseFiles.find { it.id == assistantMatchedFileId }
                        if (matchedFile != null) {
                            OutlinedButton(onClick = { openCaseFile(context, matchedFile) }) {
                                Text("فتح الملف")
                            }
                        }
                        Button(
                            onClick = {
                                viewModel.saveAssistantResultAsCaseNote(legalCase.id, smartAssistantResult!!)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LegalGoldSecondary)
                        ) {
                            Text("حفظ كملاحظة", color = LegalNavyPrimary)
                        }
                        Button(
                            onClick = { smartAssistantShowing = false },
                            colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary)
                        ) {
                            Text("تمت المراجعة")
                        }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseAddEditScreen(
    caseId: Int?,
    viewModel: AppViewModel,
    clients: List<Client>,
    customCategories: List<CustomCaseCategory>
) {
    val editing = caseId?.let { id -> viewModel.allCases.value.find { it.id == id } ?: viewModel.archivedCases.value.find { it.id == id } }

    var title by remember { mutableStateOf(editing?.title ?: "") }
    var number by remember { mutableStateOf(editing?.caseNumber ?: "") }
    var year by remember { mutableStateOf(editing?.caseYear ?: "") }
    var opponent by remember { mutableStateOf(editing?.opponentName ?: "") }
    var court by remember { mutableStateOf(editing?.courtName ?: "") }
    var circle by remember { mutableStateOf(editing?.courtCircle ?: "") }
    var summary by remember { mutableStateOf(editing?.summary ?: "") }

    var caseType by remember { mutableStateOf(editing?.caseType ?: "مدني") }
    var priority by remember { mutableStateOf(editing?.priority ?: "متوسطة") }
    var newCategoryName by remember { mutableStateOf("") }
    var selectedClientIndex by remember(editing?.id, clients) {
        mutableStateOf(
            clients.indexOfFirst { it.id == editing?.clientId }
                .takeIf { it >= 0 }
                ?: 0
        )
    }

    var clientExpanded by remember { mutableStateOf(false) }
    var caseTypeExpanded by remember { mutableStateOf(false) }
    var priorityExpanded by remember { mutableStateOf(false) }

    val caseTypesList = remember(customCategories) {
        (viewModel.repository.fixedCaseCategories() + customCategories.map { it.name.trim() })
            .filter { it.isNotBlank() }
            .distinctBy { viewModel.repository.normalizeArabic(it) }
    }
    val prioritiesList = listOf("عالية", "متوسطة", "منخفضة")

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
                        imageVector = if (editing == null) Icons.Default.AddBusiness else Icons.Default.Edit,
                        contentDescription = null,
                        tint = LegalGoldLight,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = if (editing == null) "فتح ملف قضية جديدة" else "تعديل ملف القضية",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "أدخل بيانات الدعوى بدقة لتنظيمها",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Dropdown Client selector: requires active clients is present
        if (clients.isEmpty()) {
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
                    Text(
                        text = "تنبيه: يجب إضافة موكل أولاً لربط القضية به قبل الحفظ والمتابعة.",
                        color = Color.Red,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            ExposedDropdownMenuBox(
                expanded = clientExpanded,
                onExpandedChange = { clientExpanded = !clientExpanded }
            ) {
                OutlinedTextField(
                    value = clients.getOrNull(selectedClientIndex)?.name ?: "اختر الموكل",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("الموكل المرتبط بالقضية") },
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = LegalNavyPrimary) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = clientExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LegalGoldSecondary,
                        focusedLabelColor = LegalNavyPrimary,
                        cursorColor = LegalNavyPrimary
                    )
                )
                ExposedDropdownMenu(
                    expanded = clientExpanded,
                    onDismissRequest = { clientExpanded = false }
                ) {
                    clients.forEachIndexed { idx, c ->
                        DropdownMenuItem(
                            text = { Text(c.name) },
                            onClick = {
                                selectedClientIndex = idx
                                clientExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("موضوع الدعوى / عنوان القضية (مطلوب)") },
            leadingIcon = { Icon(Icons.Default.Title, null, tint = LegalNavyPrimary) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LegalGoldSecondary, focusedLabelColor = LegalNavyPrimary)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = number,
                onValueChange = { number = it },
                label = { Text("رقم القضية") },
                leadingIcon = { Icon(Icons.Default.Numbers, null, tint = LegalNavyPrimary) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LegalGoldSecondary, focusedLabelColor = LegalNavyPrimary)
            )
            OutlinedTextField(
                value = year,
                onValueChange = { year = it },
                label = { Text("سنة الاستئناف") },
                leadingIcon = { Icon(Icons.Default.DateRange, null, tint = LegalNavyPrimary) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LegalGoldSecondary, focusedLabelColor = LegalNavyPrimary)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = opponent,
            onValueChange = { opponent = it },
            label = { Text("اسم الخصم بالدفاع") },
            leadingIcon = { Icon(Icons.Default.PersonOutline, null, tint = LegalNavyPrimary) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LegalGoldSecondary, focusedLabelColor = LegalNavyPrimary)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = court,
                onValueChange = { court = it },
                label = { Text("المحكمة المختصة") },
                leadingIcon = { Icon(Icons.Default.AccountBalance, null, tint = LegalNavyPrimary) },
                modifier = Modifier.weight(1.3f),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LegalGoldSecondary, focusedLabelColor = LegalNavyPrimary)
            )
            OutlinedTextField(
                value = circle,
                onValueChange = { circle = it },
                label = { Text("الدائرة") },
                leadingIcon = { Icon(Icons.Default.DonutLarge, null, tint = LegalNavyPrimary) },
                modifier = Modifier.weight(0.7f),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LegalGoldSecondary, focusedLabelColor = LegalNavyPrimary)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Document Categories / Case Types Dropdown
        ExposedDropdownMenuBox(
            expanded = caseTypeExpanded,
            onExpandedChange = { caseTypeExpanded = !caseTypeExpanded }
        ) {
            OutlinedTextField(
                value = caseType,
                onValueChange = {},
                readOnly = true,
                label = { Text("نوع القضية") },
                leadingIcon = { Icon(Icons.Default.Category, null, tint = LegalNavyPrimary) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = caseTypeExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LegalGoldSecondary, focusedLabelColor = LegalNavyPrimary)
            )
            ExposedDropdownMenu(
                expanded = caseTypeExpanded,
                onDismissRequest = { caseTypeExpanded = false }
            ) {
                caseTypesList.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type) },
                        onClick = {
                            caseType = type
                            caseTypeExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newCategoryName,
                onValueChange = { newCategoryName = it },
                label = { Text("إضافة تصنيف مخصص") },
                placeholder = { Text("مثال: أحوال شخصية دولي") },
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    val raw = newCategoryName.trim()
                    if (raw.isNotBlank()) {
                        viewModel.addCustomCaseCategory(raw)
                        caseType = raw
                        newCategoryName = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary)
            ) {
                Text("إضافة")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Priority Dropdown selector and tags
        ExposedDropdownMenuBox(
            expanded = priorityExpanded,
            onExpandedChange = { priorityExpanded = !priorityExpanded }
        ) {
            OutlinedTextField(
                value = priority,
                onValueChange = {},
                readOnly = true,
                label = { Text("درجة الأهمية / الأولوية") },
                leadingIcon = { Icon(Icons.Default.PriorityHigh, null, tint = LegalNavyPrimary) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = priorityExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LegalGoldSecondary, focusedLabelColor = LegalNavyPrimary)
            )
            ExposedDropdownMenu(
                expanded = priorityExpanded,
                onDismissRequest = { priorityExpanded = false }
            ) {
                prioritiesList.forEach { p ->
                    DropdownMenuItem(
                        text = { Text(p) },
                        onClick = {
                            priority = p
                            priorityExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = summary,
            onValueChange = { summary = it },
            label = { Text("ملخص وموجز وقائع الدعوى") },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, null, tint = LegalNavyPrimary) },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LegalGoldSecondary, focusedLabelColor = LegalNavyPrimary)
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val boundClient = clients.getOrNull(selectedClientIndex)
                if (boundClient != null) {
                    val toSave = LegalCase(
                        id = editing?.id ?: 0,
                        title = title,
                        caseNumber = number,
                        caseYear = year,
                        caseType = caseType,
                        clientId = boundClient.id,
                        clientName = boundClient.name,
                        opponentName = opponent,
                        courtName = court,
                        courtCircle = circle,
                        summary = summary,
                        priority = priority,
                        startDate = editing?.startDate.orEmpty(),
                        lastSessionDate = editing?.lastSessionDate.orEmpty(),
                        nextSessionDate = editing?.nextSessionDate.orEmpty(),
                        status = editing?.status ?: "جديدة",
                        notes = editing?.notes.orEmpty(),
                        isArchived = editing?.isArchived ?: false,
                        createdDate = editing?.createdDate ?: System.currentTimeMillis()
                    )
                    viewModel.saveCase(toSave) {
                        viewModel.navigateTo(Screen.CasesList)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = LegalNavyPrimary,
                disabledContainerColor = LegalNavyPrimary.copy(alpha = 0.5f)
            ),
            enabled = title.isNotEmpty() && number.isNotEmpty() && clients.isNotEmpty()
        ) {
            Icon(Icons.Default.Save, contentDescription = null, tint = LegalGoldLight)
            Spacer(modifier = Modifier.width(8.dp))
            Text("حفظ ملف القضية بالمكتب", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}



