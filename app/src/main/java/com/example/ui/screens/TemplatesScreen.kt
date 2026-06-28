package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppViewModel
import com.example.data.DocumentExportManager
import com.example.data.LegalCase
import com.example.data.LegalTemplate
import com.example.data.Screen
import com.example.ui.theme.LegalGoldSecondary
import com.example.ui.theme.LegalGrayLight
import com.example.ui.theme.LegalNavyPrimary
import com.example.ui.theme.legalScreenBackground
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun parseTemplateFields(raw: String): List<String> {
    return try {
        val arr = JSONArray(raw)
        buildList {
            for (i in 0 until arr.length()) add(arr.getString(i))
        }
    } catch (_: Exception) {
        listOf("التاريخ", "الاسم", "الطلبات")
    }
}

private fun professionalDocumentName(template: LegalTemplate, legalCase: LegalCase?): String {
    val casePart = legalCase?.caseNumber?.takeIf { it.isNotBlank() }?.let { "_$it" }.orEmpty()
    return "${template.title.replace(' ', '_')}$casePart"
}

@Composable
fun LegalTemplatesScreen(viewModel: AppViewModel, templates: List<LegalTemplate>) {
    var searchVar by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("الكل") }
    val normalizedSearch = remember(searchVar) { viewModel.repository.normalizeArabic(searchVar) }
    val categories = remember(templates) { listOf("الكل") + templates.map { it.category }.distinct() }
    val filtered = remember(templates, normalizedSearch, selectedCategory) {
        templates.filter {
            val text = viewModel.repository.normalizeArabic("${it.title} ${it.category} ${it.description} ${it.caseType}")
            val matchesSearch = normalizedSearch.isBlank() || text.contains(normalizedSearch)
            val matchesCategory = selectedCategory == "الكل" || it.category == selectedCategory
            matchesSearch && matchesCategory
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .legalScreenBackground()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, LegalNavyPrimary.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("القوالب والصياغات القانونية", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = LegalNavyPrimary)
                Text("القسم مخصص لتوليد مستندات بصياغة مصرية احترافية، ثم تعديلها يدويًا، ثم حفظها داخل التطبيق أو تصديرها.", fontSize = 13.sp, color = Color.Gray)
            }
        }

        OutlinedTextField(
            value = searchVar,
            onValueChange = { searchVar = it },
            placeholder = { Text("ابحث عن إنذار، مذكرة، عقد، طلب...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Search, null) }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
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

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا توجد قوالب مطابقة حالياً.", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered) { temp ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.navigateTo(Screen.TemplateForm(temp.id)) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        ListItem(
                            colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent),
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .background(LegalGrayLight, RoundedCornerShape(12.dp))
                                        .padding(10.dp)
                                ) {
                                    Icon(Icons.Default.Description, null, tint = LegalNavyPrimary)
                                }
                            },
                            headlineContent = { Text(temp.title, fontWeight = FontWeight.Bold, color = LegalNavyPrimary, fontSize = 16.sp) },
                            supportingContent = {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(temp.description, color = Color.Gray, fontSize = 13.sp)
                                    Text("نوع القالب: ${temp.category} | نوع القضية: ${temp.caseType}", color = Color.DarkGray, fontSize = 12.sp)
                                }
                            },
                            trailingContent = {
                                Box(
                                    modifier = Modifier
                                        .background(LegalGoldSecondary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("${parseTemplateFields(temp.requiredFieldsJson).size} حقول", color = LegalNavyPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateFormScreen(
    templateId: Int,
    presetCaseId: Int?,
    viewModel: AppViewModel,
    templates: List<LegalTemplate>,
    cases: List<LegalCase>
) {
    val context = LocalContext.current
    val template = templates.find { it.id == templateId }

    if (template == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("النموذج غير موجود.")
        }
        return
    }

    val fieldsList = remember(template) { parseTemplateFields(template.requiredFieldsJson) }
    val inputsMap = remember(template.id) { mutableStateMapOf<String, String>() }
    var selectedCaseId by remember(template.id, presetCaseId) { mutableStateOf(presetCaseId ?: cases.firstOrNull()?.id) }
    var caseExpanded by remember { mutableStateOf(false) }
    var generatedResultText by remember(template.id) { mutableStateOf("") }
    var generatedTitle by remember(template.id) { mutableStateOf(template.title) }
    var showTemplateBody by remember { mutableStateOf(false) }
    var lastExportedFile by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(template.id, selectedCaseId, cases) {
        val legalCase = cases.find { it.id == selectedCaseId }
        inputsMap.clear()
        fieldsList.forEach { inputsMap[it] = "" }
        inputsMap["التاريخ"] = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
        if (legalCase != null) {
            inputsMap["رقم_القضية"] = legalCase.caseNumber
            inputsMap["سنة_الدعوى"] = legalCase.caseYear
            inputsMap["المحكمة"] = legalCase.courtName
            inputsMap["الدائرة"] = legalCase.courtCircle
            inputsMap["اسم_الموكل"] = legalCase.clientName
            inputsMap["اسم_المدعي"] = legalCase.clientName
            inputsMap["اسم_المؤجر"] = legalCase.clientName
            inputsMap["اسم_البائع"] = legalCase.clientName
            inputsMap["اسم_المقر"] = legalCase.clientName
            inputsMap["اسم_الخصم"] = legalCase.opponentName
            inputsMap["اسم_المدعى_عليه"] = legalCase.opponentName
            inputsMap["اسم_المستأجر"] = legalCase.opponentName
            inputsMap["اسم_المشتري"] = legalCase.opponentName
            generatedTitle = professionalDocumentName(template, legalCase)
        } else {
            generatedTitle = template.title
        }
        generatedResultText = ""
        lastExportedFile = null
    }

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .legalScreenBackground()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, LegalNavyPrimary.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(template.title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = LegalNavyPrimary)
                    Text(template.description, fontSize = 12.sp, color = Color.Gray)
                    Text("الصياغة النهائية قابلة للتحرير قبل الحفظ أو التصدير.", fontSize = 12.sp, color = Color.DarkGray)
                }
            }

            if (cases.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = caseExpanded,
                    onExpandedChange = { caseExpanded = !caseExpanded }
                ) {
                    OutlinedTextField(
                        value = cases.find { it.id == selectedCaseId }?.title ?: "بدون ربط بقضية",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("القضية المرتبطة") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = caseExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = caseExpanded,
                        onDismissRequest = { caseExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("بدون ربط بقضية") },
                            onClick = {
                                selectedCaseId = null
                                caseExpanded = false
                            }
                        )
                        cases.forEach { legalCase ->
                            DropdownMenuItem(
                                text = { Text(legalCase.title) },
                                onClick = {
                                    selectedCaseId = legalCase.id
                                    caseExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, LegalNavyPrimary.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("نص القالب المرجعي", fontWeight = FontWeight.Bold, color = LegalNavyPrimary)
                        OutlinedButton(onClick = { showTemplateBody = !showTemplateBody }) {
                            Icon(Icons.Default.Visibility, null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (showTemplateBody) "إخفاء" else "إظهار")
                        }
                    }
                    if (showTemplateBody) {
                        Text(
                            template.templateBody,
                            fontSize = 12.sp,
                            color = Color.DarkGray,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFD), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        )
                    }
                }
            }

            Text("بيانات الصياغة", fontWeight = FontWeight.ExtraBold, color = LegalNavyPrimary)
            fieldsList.forEach { fieldKey ->
                OutlinedTextField(
                    value = inputsMap[fieldKey] ?: "",
                    onValueChange = { inputsMap[fieldKey] = it },
                    label = { Text(fieldKey.replace("_", " ")) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = {
                    var filledBody = template.templateBody
                    inputsMap.forEach { (k, v) ->
                        filledBody = filledBody.replace("{{$k}}", v.trim())
                    }
                    fieldsList.forEach { field ->
                        filledBody = filledBody.replace("{{${field.trim()}}}", inputsMap[field].orEmpty().trim())
                    }
                    generatedResultText = filledBody.trim()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary)
            ) {
                Icon(Icons.AutoMirrored.Filled.Article, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("توليد المسودة القانونية", fontWeight = FontWeight.Bold)
            }

            if (generatedResultText.isNotBlank()) {
                RenderGeneratedDocumentEditor(
                    context = context,
                    outputText = generatedResultText,
                    onOutputChanged = { generatedResultText = it },
                    generatedTitle = generatedTitle,
                    onTitleChanged = { generatedTitle = it },
                    templateId = templateId,
                    selectedCase = cases.find { it.id == selectedCaseId },
                    viewModel = viewModel,
                    inputsMap = inputsMap,
                    lastExportedFile = lastExportedFile,
                    onExported = { lastExportedFile = it }
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.RenderGeneratedDocumentEditor(
    context: Context,
    outputText: String,
    onOutputChanged: (String) -> Unit,
    generatedTitle: String,
    onTitleChanged: (String) -> Unit,
    templateId: Int,
    selectedCase: LegalCase?,
    viewModel: AppViewModel,
    inputsMap: Map<String, String>,
    lastExportedFile: File?,
    onExported: (File) -> Unit
) {
    Spacer(modifier = Modifier.height(8.dp))
    Text("المسودة النهائية القابلة للتحرير", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = LegalNavyPrimary)
    OutlinedTextField(
        value = generatedTitle,
        onValueChange = onTitleChanged,
        label = { Text("اسم المستند") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = outputText,
        onValueChange = onOutputChanged,
        label = { Text("نص المستند النهائي") },
        modifier = Modifier.fillMaxWidth().height(320.dp),
        textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Start, fontSize = 14.sp),
        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, null) }
    )
    Text(
        "يمكنك تعديل النص يدويًا بالكامل قبل الحفظ أو التصدير. هذه هي النسخة التي سيتم حفظها داخل التطبيق.",
        fontSize = 12.sp,
        color = Color.Gray
    )

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = {
                val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clip.setPrimaryClip(ClipData.newPlainText(generatedTitle, outputText))
                Toast.makeText(context, "تم نسخ المستند.", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = LegalGoldSecondary)
        ) {
            Icon(Icons.Default.ContentCopy, null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("نسخ")
        }
        Button(
            onClick = {
                if (selectedCase == null) {
                    Toast.makeText(context, "اختر قضية أولاً لحفظ المستند داخل التطبيق.", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                viewModel.saveGeneratedDocument(
                    caseId = selectedCase.id,
                    templateId = templateId,
                    title = generatedTitle.ifBlank { "مستند قانوني" },
                    filledFields = JSONObject(inputsMap).toString(),
                    docContent = outputText
                )
                Toast.makeText(context, "تم حفظ المستند داخل ملف القضية.", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary)
        ) {
            Icon(Icons.Default.Save, null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("حفظ داخل التطبيق")
        }
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ExportActionButton(
            label = "Word",
            icon = Icons.Default.Description,
            modifier = Modifier.weight(1f),
            onClick = {
                DocumentExportManager.exportToWord(context, generatedTitle, outputText)
                    .onSuccess {
                        onExported(it)
                        Toast.makeText(context, "تم حفظ ملف Word: ${it.name}", Toast.LENGTH_LONG).show()
                    }
                    .onFailure {
                        Toast.makeText(context, it.message ?: "فشل تصدير Word", Toast.LENGTH_SHORT).show()
                    }
            }
        )
        ExportActionButton(
            label = "PDF",
            icon = Icons.Default.PictureAsPdf,
            modifier = Modifier.weight(1f),
            onClick = {
                DocumentExportManager.exportToPdf(context, generatedTitle, outputText)
                    .onSuccess {
                        onExported(it)
                        Toast.makeText(context, "تم حفظ PDF: ${it.name}", Toast.LENGTH_LONG).show()
                    }
                    .onFailure {
                        Toast.makeText(context, it.message ?: "فشل تصدير PDF", Toast.LENGTH_SHORT).show()
                    }
            }
        )
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ExportActionButton(
            label = "HTML",
            icon = Icons.Default.FileOpen,
            modifier = Modifier.weight(1f),
            onClick = {
                DocumentExportManager.exportToHtml(context, generatedTitle, outputText)
                    .onSuccess {
                        onExported(it)
                        Toast.makeText(context, "تم حفظ HTML: ${it.name}", Toast.LENGTH_LONG).show()
                    }
                    .onFailure {
                        Toast.makeText(context, it.message ?: "فشل تصدير HTML", Toast.LENGTH_SHORT).show()
                    }
            }
        )
        ExportActionButton(
            label = "TXT",
            icon = Icons.AutoMirrored.Filled.TextSnippet,
            modifier = Modifier.weight(1f),
            onClick = {
                DocumentExportManager.exportToText(context, generatedTitle, outputText)
                    .onSuccess {
                        onExported(it)
                        Toast.makeText(context, "تم حفظ TXT: ${it.name}", Toast.LENGTH_LONG).show()
                    }
                    .onFailure {
                        Toast.makeText(context, it.message ?: "فشل تصدير TXT", Toast.LENGTH_SHORT).show()
                    }
            }
        )
    }

    if (lastExportedFile != null) {
        Text("آخر ملف محفوظ: ${lastExportedFile.name}", fontSize = 12.sp, color = Color.Gray)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "*/*"
                        val uri = DocumentExportManager.getShareUri(context, lastExportedFile)
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        putExtra(Intent.EXTRA_SUBJECT, generatedTitle)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "مشاركة المستند"))
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Share, null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("مشاركة")
            }
            OutlinedButton(
                onClick = {
                    val openIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(DocumentExportManager.getShareUri(context, lastExportedFile), "*/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(openIntent)
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Visibility, null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("عرض الملف")
            }
        }
    }
}

@Composable
private fun ExportActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary)
    ) {
        Icon(icon, null)
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, fontWeight = FontWeight.Bold)
    }
}
