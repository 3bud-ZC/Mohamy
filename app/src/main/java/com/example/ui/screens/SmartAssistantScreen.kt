package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.example.ui.theme.legalScreenBackground

enum class AssistantRole { USER, ASSISTANT, SYSTEM }

data class AssistantChatMessage(
    val role: AssistantRole,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

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
    var assistantInput by remember { mutableStateOf("") }
    var assistantFileQuery by remember { mutableStateOf("") }
    var suggestedTemplates by remember { mutableStateOf<List<LegalTemplate>>(emptyList()) }
    val listState = rememberLazyListState()

    if (cases.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .legalScreenBackground()
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
            ) { Text("إضافة قضية") }
        }
        return
    }

    val selectedCase = cases.getOrNull(selectedCaseIndex) ?: cases.first()
    val selectedCaseFiles = files.filter { it.caseId == selectedCase.id }
    val matchedFile = selectedCaseFiles.find { file ->
        val query = assistantFileQuery.ifBlank { assistantInput }
        query.isNotBlank() && file.fileName.contains(query, ignoreCase = true)
    }

    val chatMessages = remember(selectedCase.id) {
        mutableStateListOf(
            AssistantChatMessage(
                role = AssistantRole.SYSTEM,
                text = "أهلاً. أنا المساعد المحلي لهذه القضية. اكتب سؤالك مباشرة أو استخدم الأوامر السريعة."
            ),
            AssistantChatMessage(
                role = AssistantRole.ASSISTANT,
                text = "جاهز للعمل على: ${selectedCase.title}. يمكنك طلب ملخص، تجهيز جلسة، مسودة مذكرة، بحث داخل الملفات، أو عرض الأتعاب."
            )
        )
    }

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.lastIndex)
        }
    }

    LaunchedEffect(selectedCase.id) {
        assistantInput = ""
        assistantFileQuery = ""
        suggestedTemplates = emptyList()
    }

    fun submitPrompt(prompt: String) {
        val text = prompt.trim()
        if (text.isBlank()) return
        chatMessages.add(AssistantChatMessage(AssistantRole.USER, text))
        viewModel.sendSmartAssistantChatMessage(selectedCase.id, text) { reply ->
            suggestedTemplates =
                if (text.contains("قالب") || text.contains("قوالب")) {
                    buildSuggestedTemplates(templates, selectedCase.caseType, viewModel)
                } else {
                    emptyList()
                }
            chatMessages.add(AssistantChatMessage(AssistantRole.ASSISTANT, reply))
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .background(LegalNavyPrimary.copy(alpha = 0.08f), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = LegalNavyPrimary, modifier = Modifier.size(30.dp))
                }
                Text("المساعد الذكي", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = LegalNavyPrimary)
                Text(
                    "شات بوت قانوني عملي مبني على بيانات القضية والملفات، مع ردود مباشرة أو أوامر سريعة بحسب ما تحتاجه.",
                    fontSize = 12.sp,
                    color = Color.DarkGray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    when {
                        viewModel.hasConfiguredCloudAssistant && viewModel.isCloudAssistantEnabled -> "محلي + تحسين سحابي اختياري"
                        viewModel.hasConfiguredCloudAssistant -> "محلي فقط"
                        else -> "محلي بالكامل"
                    },
                    fontSize = 11.sp,
                    color = LegalNavyPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = { },
                        enabled = false,
                        label = { Text("قوالب ${templates.size}", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    AssistChip(
                        onClick = { },
                        enabled = false,
                        label = { Text("ملفات ${selectedCaseFiles.size}", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, LegalGoldSecondary.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("اختيار القضية", fontWeight = FontWeight.Bold, color = LegalNavyPrimary, modifier = Modifier.weight(1f))
                    Text(
                        "${selectedCase.caseType} • ${viewModel.caseReadinessScore(selectedCase)}%",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    cases.forEachIndexed { idx, legalCase ->
                        AssistChip(
                            onClick = { selectedCaseIndex = idx },
                            label = { Text(legalCase.title, maxLines = 1) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (selectedCase.id == legalCase.id) LegalNavyPrimary else MaterialTheme.colorScheme.surface,
                                labelColor = if (selectedCase.id == legalCase.id) Color.White else LegalNavyPrimary
                            )
                        )
                    }
                }
                Text(
                    "النوع: ${selectedCase.caseType} | الجاهزية: ${viewModel.caseReadinessScore(selectedCase)}% - ${viewModel.caseReadinessLabel(selectedCase)}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        if (selectedCaseFiles.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = LegalGoldSecondary.copy(alpha = 0.10f)),
                border = BorderStroke(1.dp, LegalGoldSecondary.copy(alpha = 0.28f)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = LegalNavyPrimary)
                    Text(
                        "هذه القضية لا تحتوي على ملفات بعد. أضف مستندًا أو اختر قضية أخرى حتى تكون نتائج الملخص والبحث أدق.",
                        fontSize = 12.sp,
                        color = LegalNavyPrimary,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        val prompts = listOf(
            "ملخص القضية",
            "تجهيز الجلسة القادمة",
            "المستندات الناقصة",
            "مسودة مذكرة",
            "اعرض الأتعاب",
            "ابحث داخل الملفات"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            prompts.forEach { prompt ->
                AssistChip(
                    onClick = { submitPrompt(prompt) },
                    label = { Text(prompt, fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = LegalNavyPrimary
                    )
                )
            }
        }

        if (viewModel.isAssistantLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Card(
            modifier = Modifier.weight(1f, fill = true),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(chatMessages) { message ->
                    ChatBubble(message = message)
                }
            }
        }

        if (suggestedTemplates.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("قوالب مناسبة", fontWeight = FontWeight.Bold, color = LegalNavyPrimary)
                    suggestedTemplates.take(4).forEach { template ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.navigateTo(Screen.TemplateForm(template.id, selectedCase.id)) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, LegalNavyPrimary.copy(alpha = 0.08f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = assistantInput,
                    onValueChange = { assistantInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("اكتب أمراً مباشراً: لخص القضية، جهز الجلسة القادمة، أو ابحث داخل الملفات") },
                    minLines = 2,
                    maxLines = 4,
                    trailingIcon = {
                        IconButton(onClick = {
                            val text = assistantInput.trim()
                            if (text.isBlank()) return@IconButton
                            chatMessages.add(AssistantChatMessage(AssistantRole.USER, text))
                            assistantInput = ""
                            viewModel.sendSmartAssistantChatMessage(selectedCase.id, text) { reply ->
                                suggestedTemplates = buildSuggestedTemplates(templates, selectedCase.caseType, viewModel)
                                chatMessages.add(AssistantChatMessage(AssistantRole.ASSISTANT, reply))
                            }
                    }) {
                            Icon(Icons.Default.Send, contentDescription = null)
                        }
                    }
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val lastAssistant = chatMessages.lastOrNull { it.role == AssistantRole.ASSISTANT }
                            clipboard.setPrimaryClip(ClipData.newPlainText("نتيجة المساعد الذكي", lastAssistant?.text.orEmpty()))
                            Toast.makeText(context, "تم نسخ آخر رد.", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("نسخ آخر رد") }

                    Button(
                        onClick = {
                            val lastAssistant = chatMessages.lastOrNull { it.role == AssistantRole.ASSISTANT }
                            if (lastAssistant != null && lastAssistant.text.isNotBlank()) {
                                viewModel.saveAssistantResultAsCaseNote(selectedCase.id, lastAssistant.text)
                                Toast.makeText(context, "تم حفظ آخر رد داخل ملاحظات القضية.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "لا توجد نتيجة صالحة لحفظها بعد.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = LegalGoldSecondary)
                    ) { Text("حفظ كملاحظة", color = LegalNavyPrimary) }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            submitPrompt("ابحث داخل الملفات ${assistantFileQuery.ifBlank { assistantInput }}")
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("بحث داخل الملفات") }

                    OutlinedButton(
                        onClick = {
                            val matched = templates.filter { template ->
                                val rules = CaseRulesEngine.getRules(selectedCase.caseType, viewModel.repository::normalizeArabic)
                                rules.suggestedTemplates.any {
                                    viewModel.repository.normalizeArabic(template.title).contains(viewModel.repository.normalizeArabic(it))
                                } || viewModel.repository.normalizeArabic(template.caseType) == viewModel.repository.normalizeArabic(rules.key)
                            }.distinctBy { it.id }
                            suggestedTemplates = matched
                            submitPrompt("اعرض القوالب المناسبة")
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("القوالب المناسبة") }
                }
            }
        }

        if (matchedFile != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LegalGoldSecondary.copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ملف مطابق لمدخلاتك:", fontWeight = FontWeight.Bold, color = LegalNavyPrimary)
                    Text("${matchedFile.fileName} | ${matchedFile.docType}", fontSize = 12.sp)
                    Button(onClick = { openCaseFile(context, matchedFile) }, colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary)) {
                        Text("فتح الملف")
                    }
                }
            }
        }
    }
}

private fun buildSuggestedTemplates(
    templates: List<LegalTemplate>,
    caseType: String,
    viewModel: AppViewModel
): List<LegalTemplate> {
    val rules = CaseRulesEngine.getRules(caseType, viewModel.repository::normalizeArabic)
    return templates.filter { template ->
        rules.suggestedTemplates.any {
            viewModel.repository.normalizeArabic(template.title).contains(viewModel.repository.normalizeArabic(it))
        } || viewModel.repository.normalizeArabic(template.caseType) == viewModel.repository.normalizeArabic(rules.key)
    }.distinctBy { it.id }
}

@Composable
private fun ChatBubble(message: AssistantChatMessage) {
    val isUser = message.role == AssistantRole.USER
    val isSystem = message.role == AssistantRole.SYSTEM
    val bubbleColor = when {
        isSystem -> MaterialTheme.colorScheme.surfaceVariant
        isUser -> LegalNavyPrimary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
    val alignment = if (isUser) Arrangement.End else Arrangement.Start

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = alignment) {
        Card(
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            border = BorderStroke(1.dp, if (isUser) LegalNavyPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = when (message.role) {
                        AssistantRole.USER -> "أنت"
                        AssistantRole.ASSISTANT -> "المساعد"
                        AssistantRole.SYSTEM -> "نظام"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isUser) Color.White.copy(alpha = 0.85f) else LegalNavyPrimary
                )
                Text(
                    text = message.text,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = textColor
                )
            }
        }
    }
}

