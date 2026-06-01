package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppViewModel
import com.example.data.DuplicateStrategy
import com.example.data.ImportPreview
import com.example.data.ImportTarget
import com.example.data.ImportedTable
import com.example.ui.theme.LegalNavyPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportDataScreen(viewModel: AppViewModel) {
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
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("استيراد بيانات", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = LegalNavyPrimary)
        Text("يدعم CSV و XLSX (أول Sheet).", fontSize = 12.sp, color = Color.Gray)

        Card(colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color.LightGray)) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("نوع الاستيراد", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { selectedTarget = ImportTarget.CLIENTS; table = null; preview = null }) { Text("العملاء") }
                    OutlinedButton(onClick = { selectedTarget = ImportTarget.CASES; table = null; preview = null }) { Text("القضايا") }
                    OutlinedButton(onClick = { selectedTarget = ImportTarget.SESSIONS; table = null; preview = null }) { Text("الجلسات") }
                }
                Button(
                    onClick = { picker.launch("*/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary)
                ) {
                    Text("اختيار ملف Excel/CSV")
                }
                if (selectedFileUri != null) {
                    Text("تم اختيار الملف: $selectedFileUri", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }

        if (table != null) {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color.LightGray)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Mapping الأعمدة", fontWeight = FontWeight.Bold)
                    Text("يمكن تغيير التعيين أو تجاهل أي عمود.", fontSize = 11.sp, color = Color.Gray)

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
                                modifier = Modifier.fillMaxWidth().menuAnchor()
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
                    }

                    if (selectedTarget == ImportTarget.CLIENTS) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { duplicateStrategy = DuplicateStrategy.SKIP }) { Text("تخطي المكرر") }
                            OutlinedButton(onClick = { duplicateStrategy = DuplicateStrategy.UPDATE }) { Text("تحديث الموجود") }
                            OutlinedButton(onClick = { duplicateStrategy = DuplicateStrategy.CREATE_NEW }) { Text("إنشاء جديد") }
                        }
                    }

                    if (selectedTarget == ImportTarget.CASES) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary)
                    ) {
                        Text("Preview")
                    }
                }
            }
        }

        if (preview != null) {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color.LightGray)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("نتيجة المعاينة", fontWeight = FontWeight.Bold)
                    Text("Total rows: ${preview!!.totalRows}")
                    Text("Valid rows: ${preview!!.validRows}")
                    Text("Invalid rows: ${preview!!.invalidRows}")
                    Text("Duplicates: ${preview!!.duplicates}")
                    Text("Warnings: ${preview!!.warnings}")

                    HorizontalDivider()

                    preview!!.rows.take(50).forEach { row ->
                        Text("صف ${row.rowNumber} | ${row.status} | ${row.reason}", fontSize = 12.sp)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                viewModel.importFromPreview(
                                    preview = preview!!,
                                    duplicateStrategy = duplicateStrategy,
                                    autoCreateClientForCases = autoCreateClientForCases
                                ) {
                                    // no-op
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary)
                        ) {
                            Text("Import valid rows")
                        }
                        OutlinedButton(onClick = { preview = null }) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
