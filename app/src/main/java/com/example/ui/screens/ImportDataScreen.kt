package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Publish
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
import com.example.data.DuplicateStrategy
import com.example.data.ImportPreview
import com.example.data.ImportTarget
import com.example.data.ImportedTable
import com.example.ui.theme.LegalGoldSecondary
import com.example.ui.theme.LegalNavyPrimary
import com.example.ui.theme.ScreenSectionCard
import com.example.ui.theme.legalScreenBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportDataScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    var selectedTarget by remember { mutableStateOf(ImportTarget.CLIENTS) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var table by remember { mutableStateOf<ImportedTable?>(null) }
    var mapping by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var preview by remember { mutableStateOf<ImportPreview?>(null) }
    var duplicateStrategy by remember { mutableStateOf(DuplicateStrategy.SKIP) }
    var autoCreateClientForCases by remember { mutableStateOf(false) }
    var fieldsExpanded by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedFileUri = uri
            preview = null
            viewModel.parseImportFile(uri, selectedTarget) { parsed, autoMap ->
                table = parsed
                mapping = autoMap
            }
        }
    }

    val fieldsByTarget = remember(selectedTarget) {
        when (selectedTarget) {
            ImportTarget.CLIENTS -> listOf("name", "phone", "email", "national_id", "address", "notes", "status")
            ImportTarget.CASES -> listOf("title", "case_number", "case_year", "client_name", "opponent_name", "court_name", "court_circle", "case_type", "status", "priority", "start_date", "next_session_date", "notes")
            ImportTarget.SESSIONS -> listOf("case_number", "case_title", "client_name", "session_date", "session_time", "court_name", "court_circle", "requirements", "result", "notes")
            ImportTarget.TASKS -> listOf("title", "description", "due_date", "priority", "status", "case_number", "case_title", "client_name")
            ImportTarget.FILES_METADATA -> listOf("file_name", "doc_type", "case_number", "case_title", "client_name", "file_path", "file_length", "extracted_text", "notes")
            ImportTarget.GENERATED_DOCS -> listOf("document_title", "content", "case_number", "case_title", "template_id", "filled_fields_json")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .legalScreenBackground()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = LegalNavyPrimary)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(LegalGoldSecondary.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Icon(Icons.Default.Publish, contentDescription = null, tint = LegalGoldSecondary)
                    }
                    Column {
                        Text("استيراد بيانات من ملف", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text("CSV و XLSX (الورقة الأولى)", fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f))
                    }
                }
                Text(
                    "راجع المعاينة قبل التنفيذ النهائي لتجنب إدخال مكرر أو تعيين أعمدة خاطئ.",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    lineHeight = 19.sp
                )
            }
        }

        ScreenSectionCard(title = "إعداد ملف الاستيراد") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTarget == ImportTarget.CLIENTS,
                    onClick = {
                        selectedTarget = ImportTarget.CLIENTS
                        table = null
                        preview = null
                    },
                    label = { Text("العملاء") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LegalNavyPrimary,
                        selectedLabelColor = Color.White
                    )
                )
                FilterChip(
                    selected = selectedTarget == ImportTarget.CASES,
                    onClick = {
                        selectedTarget = ImportTarget.CASES
                        table = null
                        preview = null
                    },
                    label = { Text("القضايا") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LegalNavyPrimary,
                        selectedLabelColor = Color.White
                    )
                )
                FilterChip(
                    selected = selectedTarget == ImportTarget.SESSIONS,
                    onClick = {
                        selectedTarget = ImportTarget.SESSIONS
                        table = null
                        preview = null
                    },
                    label = { Text("الجلسات") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LegalNavyPrimary,
                        selectedLabelColor = Color.White
                    )
                )
                FilterChip(
                    selected = selectedTarget == ImportTarget.TASKS,
                    onClick = {
                        selectedTarget = ImportTarget.TASKS
                        table = null
                        preview = null
                    },
                    label = { Text("المهام") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LegalNavyPrimary,
                        selectedLabelColor = Color.White
                    )
                )
                FilterChip(
                    selected = selectedTarget == ImportTarget.FILES_METADATA,
                    onClick = {
                        selectedTarget = ImportTarget.FILES_METADATA
                        table = null
                        preview = null
                    },
                    label = { Text("مستندات metadata") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LegalNavyPrimary,
                        selectedLabelColor = Color.White
                    )
                )
                FilterChip(
                    selected = selectedTarget == ImportTarget.GENERATED_DOCS,
                    onClick = {
                        selectedTarget = ImportTarget.GENERATED_DOCS
                        table = null
                        preview = null
                    },
                    label = { Text("مستندات مولدة") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LegalNavyPrimary,
                        selectedLabelColor = Color.White
                    )
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { picker.launch("*/*") },
                colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text("اختيار ملف Excel / CSV", fontWeight = FontWeight.Bold)
            }

            if (selectedFileUri != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF3F5FA)
                ) {
                    Text(
                        "تم اختيار الملف: $selectedFileUri",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }
            }
        }

        if (table != null) {
            ScreenSectionCard(title = "مطابقة الأعمدة") {
                Text("يمكن تغيير التعيين أو تجاهل أي عمود.", fontSize = 11.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(6.dp))

                table!!.headers.forEach { header ->
                    val current = mapping[header] ?: "__ignore__"
                    ExposedDropdownMenuBox(
                        expanded = fieldsExpanded == header,
                        onExpandedChange = { fieldsExpanded = if (fieldsExpanded == header) null else header }
                    ) {
                        OutlinedTextField(
                            value = "$header → $current",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fieldsExpanded == header) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = fieldsExpanded == header,
                            onDismissRequest = { fieldsExpanded = null }
                        ) {
                            DropdownMenuItem(text = { Text("__ignore__") }, onClick = {
                                mapping = mapping.toMutableMap().also { it[header] = "__ignore__" }
                                fieldsExpanded = null
                            })
                            fieldsByTarget.forEach { field ->
                                DropdownMenuItem(text = { Text(field) }, onClick = {
                                    mapping = mapping.toMutableMap().also { it[header] = field }
                                    fieldsExpanded = null
                                })
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                if (selectedTarget == ImportTarget.CLIENTS) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = { duplicateStrategy = DuplicateStrategy.SKIP }) { Text("تخطي المكرر") }
                        OutlinedButton(onClick = { duplicateStrategy = DuplicateStrategy.UPDATE }) { Text("تحديث الموجود") }
                        OutlinedButton(onClick = { duplicateStrategy = DuplicateStrategy.CREATE_NEW }) { Text("إنشاء جديد") }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                if (selectedTarget == ImportTarget.CASES) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("إنشاء العميل تلقائيًا عند عدم وجوده", fontSize = 12.sp)
                        TextButton(onClick = { autoCreateClientForCases = !autoCreateClientForCases }) {
                            Text(if (autoCreateClientForCases) "مفعل" else "غير مفعل")
                        }
                    }
                }

                Button(
                    onClick = {
                        viewModel.buildImportPreview(
                            target = selectedTarget,
                            headers = table!!.headers,
                            rows = table!!.rows,
                            mapping = mapping,
                            duplicateStrategy = duplicateStrategy,
                            autoCreateClientForCases = autoCreateClientForCases
                        ) {
                            preview = it
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إظهار المعاينة", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (preview != null) {
            ScreenSectionCard(title = "نتيجة المعاينة") {
                Text("إجمالي الصفوف: ${preview!!.totalRows}", color = LegalNavyPrimary, fontWeight = FontWeight.SemiBold)
                Text("الصفوف الصالحة: ${preview!!.validRows}")
                Text("الصفوف غير الصالحة: ${preview!!.invalidRows}")
                Text("المدخلات المكررة: ${preview!!.duplicates}")
                Text("التحذيرات: ${preview!!.warnings}")

                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                preview!!.rows.take(50).forEach { row ->
                    Text("صف ${row.rowNumber} | ${row.status} | ${row.reason}", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        val report = viewModel.generateImportReportText(preview!!)
                        val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clip.setPrimaryClip(ClipData.newPlainText("Import Report", report))
                    }) {
                        Text("نسخ التقرير")
                    }
                    OutlinedButton(onClick = {
                        val report = viewModel.generateImportReportText(preview!!)
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Import Report")
                            putExtra(Intent.EXTRA_TEXT, report)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "مشاركة تقرير الاستيراد"))
                    }) {
                        Text("مشاركة التقرير")
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            viewModel.importFromPreview(
                                preview = preview!!,
                                duplicateStrategy = duplicateStrategy,
                                autoCreateClientForCases = autoCreateClientForCases
                            ) {
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary)
                    ) {
                        Text("استيراد الصفوف الصالحة")
                    }
                    OutlinedButton(onClick = { preview = null }) {
                        Text("إلغاء المعاينة")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

