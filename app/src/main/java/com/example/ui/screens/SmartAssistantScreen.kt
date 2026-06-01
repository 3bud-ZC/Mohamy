package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppViewModel
import com.example.data.CaseFile
import com.example.data.CaseRulesEngine
import com.example.data.LegalCase
import com.example.data.LegalTemplate
import com.example.data.Screen
import com.example.ui.openCaseFile
import com.example.ui.theme.LegalGoldSecondary
import com.example.ui.theme.LegalNavyPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartAssistantScreen(
    viewModel: AppViewModel,
    cases: List<LegalCase>,
    files: List<CaseFile>,
    templates: List<LegalTemplate>
) {
    val context = LocalContext.current
    var selectedCaseIndex by remember { mutableStateOf(0) }
    var caseExpanded by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf("اختر قضية ثم شغّل أحد أوامر المساعد.") }
    var matchedFileId by remember { mutableStateOf<Int?>(null) }
    var searchInput by remember { mutableStateOf("") }
    var suggestedTemplates by remember { mutableStateOf<List<LegalTemplate>>(emptyList()) }

    if (cases.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = LegalNavyPrimary, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text("لا توجد قضايا بعد. أضف قضية أولًا لاستخدام المساعد الذكي.", color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.navigateTo(Screen.CaseAddEdit()) },
                colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary)
            ) {
                Text("إضافة قضية")
            }
        }
        return
    }

    val selectedCase = cases.getOrNull(selectedCaseIndex) ?: cases.first()
    val selectedCaseFiles = files.filter { it.caseId == selectedCase.id }
    val matchedFile = selectedCaseFiles.find { it.id == matchedFileId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("المساعد الذكي", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = LegalNavyPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "المساعد الذكي يساعد في تنظيم البيانات والبحث داخل الملفات وتجهيز القوالب، ولا يعتبر رأيًا قانونيًا نهائيًا.",
            fontSize = 12.sp,
            color = Color.DarkGray
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text("يعمل محليًا على بياناتك دون إرسال ملفاتك لأي خادم.", fontSize = 12.sp, color = LegalNavyPrimary)
        Spacer(modifier = Modifier.height(14.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, LegalGoldSecondary.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("اختار قضية", fontWeight = FontWeight.Bold, color = LegalNavyPrimary)
                ExposedDropdownMenuBox(
                    expanded = caseExpanded,
                    onExpandedChange = { caseExpanded = !caseExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCase.title,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = caseExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = caseExpanded,
                        onDismissRequest = { caseExpanded = false }
                    ) {
                        cases.forEachIndexed { idx, legalCase ->
                            DropdownMenuItem(
                                text = { Text(legalCase.title) },
                                onClick = {
                                    selectedCaseIndex = idx
                                    matchedFileId = null
                                    suggestedTemplates = emptyList()
                                    resultText = "تم اختيار القضية: ${legalCase.title}"
                                    caseExpanded = false
                                }
                            )
                        }
                    }
                }
                Text("النوع: ${selectedCase.caseType} | الملفات: ${selectedCaseFiles.size}", fontSize = 12.sp, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(
                onClick = {
                    suggestedTemplates = emptyList()
                    viewModel.getSmartAssistantSummary(selectedCase.id) {
                        matchedFileId = null
                        resultText = it
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary)
            ) { Text("لخّص القضية", fontSize = 11.sp) }
            Button(
                onClick = {
                    suggestedTemplates = emptyList()
                    viewModel.getMissingDocumentsSuggestion(selectedCase.id) {
                        matchedFileId = null
                        resultText = it
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary)
            ) { Text("المستندات الناقصة", fontSize = 11.sp) }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(
                onClick = {
                    suggestedTemplates = emptyList()
                    viewModel.getNextSessionAssistant(selectedCase.id) {
                        matchedFileId = null
                        resultText = it
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary)
            ) { Text("الجلسة القادمة", fontSize = 11.sp) }
            Button(
                onClick = {
                    suggestedTemplates = emptyList()
                    viewModel.getOpenTasksAssistant(selectedCase.id) {
                        matchedFileId = null
                        resultText = it
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary)
            ) { Text("المهام المفتوحة", fontSize = 11.sp) }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(
                onClick = {
                    val rules = CaseRulesEngine.getRules(selectedCase.caseType, viewModel.repository::normalizeArabic)
                    val matched = templates.filter { template ->
                        rules.suggestedTemplates.any {
                            viewModel.repository.normalizeArabic(template.title).contains(viewModel.repository.normalizeArabic(it))
                        } || viewModel.repository.normalizeArabic(template.caseType) == viewModel.repository.normalizeArabic(rules.key)
                    }.distinctBy { it.id }
                    suggestedTemplates = matched
                    viewModel.getSuggestedTemplatesForCase(selectedCase.caseType) {
                        matchedFileId = null
                        resultText = it
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary)
            ) { Text("القوالب المناسبة", fontSize = 11.sp) }
            Button(
                onClick = {
                    suggestedTemplates = emptyList()
                    viewModel.triggerSmartChecklist(selectedCase.id, selectedCase.caseType) {
                        matchedFileId = null
                        resultText = it
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary)
            ) { Text("Checklist", fontSize = 11.sp) }
        }

        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = searchInput,
            onValueChange = { searchInput = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("بحث داخل ملفات القضية المختارة") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
        )
        Spacer(modifier = Modifier.height(6.dp))
        Button(
            onClick = {
                suggestedTemplates = emptyList()
                viewModel.searchInsideCaseFiles(selectedCase.id, searchInput) { text, fileId ->
                    matchedFileId = fileId
                    resultText = text
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary)
        ) {
            Text("بحث داخل ملفات القضية")
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("النتيجة", fontWeight = FontWeight.Bold, color = LegalNavyPrimary)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color.LightGray)
        ) {
            Text(resultText, modifier = Modifier.padding(12.dp), fontSize = 13.sp)
        }

        if (matchedFile != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LegalGoldSecondary.copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("نتيجة ملف:", fontWeight = FontWeight.Bold, color = LegalNavyPrimary)
                    Text("${matchedFile.fileName} | ${matchedFile.docType}", fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { openCaseFile(context, matchedFile) }, colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary)) {
                            Text("فتح الملف")
                        }
                        OutlinedButton(onClick = {
                            viewModel.saveAssistantResultAsCaseNote(selectedCase.id, resultText)
                            Toast.makeText(context, "تم حفظ النتيجة كملاحظة.", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.NoteAdd, contentDescription = null)
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("حفظ كملاحظة")
                        }
                    }
                }
            }
        }

        if (suggestedTemplates.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text("القوالب المناسبة", fontWeight = FontWeight.Bold, color = LegalNavyPrimary)
            suggestedTemplates.forEach { template ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .clickable { viewModel.navigateTo(Screen.TemplateForm(template.id, selectedCase.id)) },
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = LegalNavyPrimary)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(template.title, fontWeight = FontWeight.Bold, color = LegalNavyPrimary)
                            Text("${template.category} | ${template.caseType}", fontSize = 11.sp, color = Color.Gray)
                        }
                        Icon(Icons.Default.Gavel, contentDescription = null, tint = LegalGoldSecondary)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
