package com.example.ui.screens
import com.example.data.*
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LegalGoldSecondary
import com.example.ui.theme.LegalGrayLight
import com.example.ui.theme.LegalNavyPrimary
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LegalTemplatesScreen(viewModel: AppViewModel, templates: List<LegalTemplate>) {
    var searchVar by remember { mutableStateOf("") }
    val filtered = templates.filter {
        it.title.contains(searchVar) || it.category.contains(searchVar) || it.description.contains(searchVar)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("نماذج وصياغة الأوراق القانونية 📄", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = LegalNavyPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text("اختر نموذجاً لصياغته تلقائياً، واستبدال الأسماء والتواريخ لنسخها وحفظها محلياً بملف القضية.", fontSize = 13.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchVar,
            onValueChange = { searchVar = it },
            placeholder = { Text("ابحث عن النماذج (عقود، إنذارات، مذكرات دفاع)...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Search, null) }
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا توجد نماذج مبرمة مطابقة حالياً.", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filtered) { temp ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { viewModel.navigateTo(Screen.TemplateForm(temp.id)) },
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
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(LegalGrayLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = LegalNavyPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            },
                            headlineContent = { Text(temp.title, fontWeight = FontWeight.Bold, color = LegalNavyPrimary, fontSize = 16.sp) },
                            supportingContent = { Text(temp.description, color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp)) },
                            trailingContent = {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(LegalGoldSecondary.copy(alpha = 0.15f))
                                        .border(1.dp, LegalGoldSecondary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(temp.category, color = LegalNavyPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
    val template = templates.find { it.id == templateId }

    if (template == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("النموذج غير موجود.")
        }
        return
    }

    // Parse needed fields
    val fieldsList = remember(template) {
        try {
            val arr = JSONArray(template.requiredFieldsJson)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
            list
        } catch (e: Exception) {
            listOf("التاريخ", "الاسم", "المبلغ")
        }
    }

    // Input fields mapping states
    val inputsMap = remember { mutableStateMapOf<String, String>() }

    // Init defaults for presets if matches
    LaunchedEffect(template, presetCaseId, cases) {
        inputsMap.clear()
        fieldsList.forEach { inputsMap[it] = "" }
        inputsMap["التاريخ"] = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
        
        // If case is preset, bind known traits
        if (presetCaseId != null) {
            val c = cases.find { it.id == presetCaseId }
            if (c != null) {
                inputsMap["رقم_القضية"] = c.caseNumber
                inputsMap["سنة_الدعوى"] = c.caseYear
                inputsMap["المحكمة"] = c.courtName
                inputsMap["الدائرة"] = c.courtCircle
                inputsMap["اسم_المؤجر"] = c.clientName
                inputsMap["اسم_البائع"] = c.clientName
                inputsMap["اسم_المقر"] = c.clientName
                inputsMap["اسم_المستأجر"] = c.opponentName
                inputsMap["اسم_المشتري"] = c.opponentName
                inputsMap["اسم_الخصم"] = c.opponentName
            }
        }
    }

    var generatedResultText by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("صياغة مستند: ${template.title}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = LegalNavyPrimary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(template.description, fontSize = 12.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(20.dp))

        // Document Fields input cells
        fieldsList.forEach { fieldKey ->
            OutlinedTextField(
                value = inputsMap[fieldKey] ?: "",
                onValueChange = { inputsMap[fieldKey] = it },
                label = { Text(fieldKey.replace("_", " ")) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                // Dynamically replace variables in template body
                var filledBody = template.templateBody
                inputsMap.forEach { (k, v) ->
                    filledBody = filledBody.replace("{{$k}}", v)
                }
                // Fallbacks generic tags
                filledBody = filledBody.replace("{{التاريخ}}", inputsMap["التاريخ"] ?: "")
                generatedResultText = filledBody
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary)
        ) {
            Text("صياغة وتوليد المستند القانوني النهائي", fontWeight = FontWeight.Bold)
        }

        renderedResultField(generatedResultText, templateId, viewModel, cases, inputsMap)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColumnScope.renderedResultField(
    outputText: String?,
    templateId: Int,
    viewModel: AppViewModel,
    cases: List<LegalCase>,
    inputsMap: Map<String, String>
) {
    val context = LocalContext.current
    if (outputText != null) {
        var selectedCaseIndex by remember { mutableStateOf(0) }
        var caseExpanded by remember { mutableStateOf(false) }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("النص النهائي المولّد:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = LegalNavyPrimary)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, LegalGoldSecondary)
        ) {
            Text(
                text = outputText,
                modifier = Modifier.padding(12.dp),
                fontSize = 12.sp,
                textAlign = TextAlign.Right
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clip.setPrimaryClip(ClipData.newPlainText("محامي فون مستند مبرم", outputText))
                    Toast.makeText(context, "تم نسخ النص إلى الحافظة بنجاح!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = LegalGoldSecondary)
            ) {
                Text("نسخ مستند")
            }
        }

        // Save inside local case generated files
        if (cases.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("حفظ المستند محلياً تحت ملف قضية:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                ExposedDropdownMenuBox(
                    expanded = caseExpanded,
                    onExpandedChange = { caseExpanded = !caseExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = cases.getOrNull(selectedCaseIndex)?.title ?: "اختر القضية للحفظ",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = caseExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = caseExpanded,
                        onDismissRequest = { caseExpanded = false }
                    ) {
                        cases.forEachIndexed { idx, c ->
                            DropdownMenuItem(
                                text = { Text(c.title) },
                                onClick = {
                                    selectedCaseIndex = idx
                                    caseExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val bound = cases.getOrNull(selectedCaseIndex)
                        if (bound != null) {
                            val inputsJson = JSONObject(inputsMap).toString()
                            viewModel.saveGeneratedDocument(
                                caseId = bound.id,
                                templateId = templateId,
                                title = "مستند_مبرم_${bound.caseNumber}",
                                filledFields = inputsJson,
                                docContent = outputText
                            )
                            Toast.makeText(context, "تم إرسال وحفظ المستند لملف القضية بنجاح بنجاح!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary)
                ) {
                    Icon(Icons.Default.Save, "حفظ بالدعوى")
                }
            }
        }
    }
}

