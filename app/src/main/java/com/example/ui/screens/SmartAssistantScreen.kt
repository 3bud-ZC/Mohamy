package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppViewModel
import com.example.data.CaseFile
import com.example.data.CaseRulesEngine
import com.example.data.LegalCase
import com.example.data.LegalTemplate
import com.example.data.Screen
import com.example.ui.openCaseFile
import com.example.ui.theme.legalScreenBackground
import com.example.ui.theme.MohamyGold
import com.example.ui.theme.MohamyGoldBright
import com.example.ui.theme.MohamySurfaceRaised

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
    var suggestedTemplates by remember { mutableStateOf<List<LegalTemplate>>(emptyList()) }
    var isCaseDropdownExpanded by remember { mutableStateOf(false) }
    
    val listState = rememberLazyListState()

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                assistantInput = matches[0]
            }
        }
    }

    if (cases.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .legalScreenBackground()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MohamySurfaceRaised, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("المساعد الذكي غير متاح", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MohamySurfaceRaised)
            Spacer(modifier = Modifier.height(8.dp))
            Text("لا توجد قضايا مسجلة. أضف قضية أولاً للبدء.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { viewModel.navigateTo(Screen.CaseAddEdit()) },
                colors = ButtonDefaults.buttonColors(containerColor = MohamySurfaceRaised),
                shape = RoundedCornerShape(12.dp)
            ) { Text("إضافة قضية جديدة", modifier = Modifier.padding(8.dp)) }
        }
        return
    }

    val selectedCase = cases.getOrNull(selectedCaseIndex) ?: cases.first()

    val chatMessages = remember(selectedCase.id) {
        mutableStateListOf(
            AssistantChatMessage(
                role = AssistantRole.SYSTEM,
                text = "مرحباً! أنا المساعد الذكي لقضية \"${selectedCase.title}\".\n• أعمل محلياً على الجهاز دون إرسال الملفات أو البيانات الأصلية.\n• يمكنني تلخيص القضية، كتابة المسودات، البحث في المستندات، وتجهيز الجلسة."
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
        suggestedTemplates = emptyList()
    }

    fun submitPrompt(prompt: String) {
        val text = prompt.trim()
        if (text.isBlank()) return
        chatMessages.add(AssistantChatMessage(AssistantRole.USER, text))
        assistantInput = ""
        viewModel.sendSmartAssistantChatMessage(selectedCase.id, text) { reply ->
            suggestedTemplates =
                if (text.contains("قالب") || text.contains("قوالب") || text.contains("نموذج")) {
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
    ) {
        // --- 1. Top App Bar & Case Selection ---
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MohamySurfaceRaised.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MohamySurfaceRaised)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("المساعد الذكي", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MohamySurfaceRaised)
                        Text(
                            if (viewModel.hasConfiguredCloudAssistant) "متصل بالسحابة (اختياري)" else "محلي بالكامل (آمن وموثوق)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (viewModel.hasConfiguredCloudAssistant) {
                            MohamyGold.copy(alpha = 0.18f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }
                    ) {
                        Text(
                            text = if (viewModel.hasConfiguredCloudAssistant) "محلي + سحابي" else "محلي فقط",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (viewModel.hasConfiguredCloudAssistant) MohamyGold else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Case Selector Dropdown
                ExposedDropdownMenuBox(
                    expanded = isCaseDropdownExpanded,
                    onExpandedChange = { isCaseDropdownExpanded = it }
                ) {
                    @Suppress("DEPRECATION")
                    OutlinedTextField(
                        value = selectedCase.title,
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, tint = MohamySurfaceRaised) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCaseDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MohamySurfaceRaised,
                            unfocusedBorderColor = MohamySurfaceRaised.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = isCaseDropdownExpanded,
                        onDismissRequest = { isCaseDropdownExpanded = false }
                    ) {
                        cases.forEachIndexed { index, legalCase ->
                            DropdownMenuItem(
                                text = { 
                                    Column {
                                        Text(legalCase.title, fontWeight = FontWeight.Bold)
                                        Text("الجاهزية: ${viewModel.caseReadinessScore(legalCase)}%", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                onClick = {
                                    selectedCaseIndex = index
                                    isCaseDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // --- 2. Chat Area ---
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(chatMessages) { message ->
                    ChatBubble(message = message, viewModel = viewModel, selectedCase = selectedCase, context = context)
                }
                if (viewModel.isAssistantLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MohamySurfaceRaised, strokeWidth = 2.dp)
                                    Text("جاري التفكير...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Suggested Templates Display (If any)
        AnimatedVisibility(
            visible = suggestedTemplates.isNotEmpty(),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MohamyGold.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("قوالب مقترحة", fontWeight = FontWeight.Bold, color = MohamySurfaceRaised, fontSize = 12.sp)
                        IconButton(onClick = { suggestedTemplates = emptyList() }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    suggestedTemplates.take(3).forEach { template ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.navigateTo(Screen.TemplateForm(template.id, selectedCase.id)) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = MohamySurfaceRaised, modifier = Modifier.size(18.dp))
                            Text(template.title, fontSize = 13.sp, color = MohamySurfaceRaised, modifier = Modifier.weight(1f))
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, tint = MohamyGold, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // --- 3. Quick Suggestions Row ---
        val promptGroups = listOf(
            "وضع القضية" to Icons.Default.QueryStats,
            "لخص القضية" to Icons.Default.Summarize,
            "النواقص" to Icons.Default.RuleFolder,
            "تجهيز الجلسة" to Icons.Default.Event,
            "مسودة مذكرة" to Icons.Default.EditNote,
            "تحديث للعميل" to Icons.Default.Forum,
            "كم باقي أتعاب" to Icons.Default.AttachMoney,
            "ابحث داخل الملفات" to Icons.Default.Search
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            promptGroups.forEach { (label, icon) ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.clickable { submitPrompt(label) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, contentDescription = null, tint = MohamySurfaceRaised, modifier = Modifier.size(16.dp))
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            color = MohamySurfaceRaised
                        )
                    }
                }
            }
        }

        // --- 4. Bottom Input Bar ---
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-EG")
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "تحدث الآن...")
                        }
                        try {
                            speechRecognizerLauncher.launch(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "التعرف على الصوت غير مدعوم.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "تحدث", tint = MohamySurfaceRaised)
                }

                OutlinedTextField(
                    value = assistantInput,
                    onValueChange = { assistantInput = it },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp, max = 120.dp),
                    placeholder = { Text("اسألني عن القضية...", fontSize = 14.sp) },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MohamySurfaceRaised,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    ),
                    maxLines = 4
                )

                IconButton(
                    onClick = { submitPrompt(assistantInput) },
                    modifier = Modifier
                        .size(40.dp)
                        .background(MohamySurfaceRaised, CircleShape),
                    enabled = assistantInput.isNotBlank() && !viewModel.isAssistantLoading
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "إرسال", tint = Color.White, modifier = Modifier.padding(start = 4.dp))
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
private fun FormattedAssistantText(text: String, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        text.lines().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isBlank()) return@forEach
            when {
                line.startsWith("#") -> {
                    val clean = line.trimStart('#').trim()
                    Text(
                        text = clean,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = onSurface
                    )
                }
                line.startsWith("---") -> {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
                line.startsWith("•") || line.startsWith("-") || line.startsWith("*") -> {
                    val clean = line.trimStart('•', '-', '*').trim()
                    val bulletText = buildAnnotatedString {
                        withStyle(SpanStyle(color = primary, fontWeight = FontWeight.Bold)) {
                            append("•  ")
                        }
                        appendStyledBullet(clean, primary, onSurface)
                    }
                    Text(
                        text = bulletText,
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp,
                        color = onSurface
                    )
                }
                line.matches(Regex("^\\d+[.\\)]\\s+.*")) -> {
                    val bulletText = buildAnnotatedString {
                        withStyle(SpanStyle(color = primary, fontWeight = FontWeight.Bold)) {
                            append(line.substringBefore(" "))
                            append("  ")
                        }
                        appendStyledBullet(line.substringAfter(" ").trim(), primary, onSurface)
                    }
                    Text(
                        text = bulletText,
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp,
                        color = onSurface
                    )
                }
                line.startsWith("💡") || line.startsWith("⚠️") || line.startsWith("📊") || line.startsWith("📋") || line.startsWith("📝") -> {
                    Text(
                        text = line,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = onSurface,
                        lineHeight = 20.sp
                    )
                }
                else -> {
                    val normalText = buildAnnotatedString {
                        appendStyledBullet(line, primary, onSurface)
                    }
                    Text(
                        text = normalText,
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp,
                        color = onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun androidx.compose.ui.text.AnnotatedString.Builder.appendStyledBullet(
    line: String,
    accent: Color,
    default: Color
) {
    val regex = Regex("\\*\\*(.+?)\\*\\*")
    var cursor = 0
    regex.findAll(line).forEach { match ->
        if (match.range.first > cursor) {
            withStyle(SpanStyle(color = default)) {
                append(line.substring(cursor, match.range.first))
            }
        }
        withStyle(SpanStyle(color = accent, fontWeight = FontWeight.Bold)) {
            append(match.groupValues[1])
        }
        cursor = match.range.last + 1
    }
    if (cursor < line.length) {
        withStyle(SpanStyle(color = default)) {
            append(line.substring(cursor))
        }
    }
}

@Composable
private fun ChatBubble(message: AssistantChatMessage, viewModel: AppViewModel, selectedCase: LegalCase, context: Context) {
    val isUser = message.role == AssistantRole.USER
    val isSystem = message.role == AssistantRole.SYSTEM
    
    val bubbleColor = when {
        isUser -> MohamySurfaceRaised
        isSystem -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f)
        else -> MaterialTheme.colorScheme.surface
    }
    val textColor = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val shape = if (isUser) {
        RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Card(
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            shape = shape,
            elevation = CardDefaults.cardElevation(if (isUser || isSystem) 0.dp else 2.dp),
            modifier = Modifier.widthIn(min = 80.dp, max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (!isUser) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
                        Icon(
                            if (isSystem) Icons.Default.Info else Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (isSystem) MaterialTheme.colorScheme.onSurfaceVariant else MohamyGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSystem) "توجيه" else "المساعد الذكي",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSystem) MaterialTheme.colorScheme.onSurfaceVariant else MohamyGold
                        )
                    }
                }
                
                if (isUser) {
                    Text(
                        text = message.text,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = textColor
                    )
                } else {
                    FormattedAssistantText(text = message.text)
                }
                
                if (message.role == AssistantRole.ASSISTANT) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("نسخ", message.text))
                                Toast.makeText(context, "تم النسخ", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = {
                                viewModel.saveAssistantResultAsCaseNote(selectedCase.id, message.text)
                                Toast.makeText(context, "تم الحفظ كملاحظة", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.BookmarkBorder, contentDescription = "حفظ", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
