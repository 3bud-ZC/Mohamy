package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Subject
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppViewModel
import com.example.data.LegalCase
import com.example.data.LegalTask
import com.example.data.Screen
import com.example.ui.components.MohamyBadgeTone
import com.example.ui.components.MohamyEmptyState
import com.example.ui.components.MohamySearchBar
import com.example.ui.components.MohamyStatusBadge
import com.example.ui.components.TaskCard
import com.example.ui.theme.LegalGoldLight
import com.example.ui.theme.LegalGoldSecondary
import com.example.ui.theme.LegalNavyPrimary
import com.example.ui.theme.MohamyDimens
import com.example.ui.theme.legalScreenBackground
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun TasksListScreen(viewModel: AppViewModel, tasks: List<LegalTask>, cases: List<LegalCase>) {
    var searchTxt by remember { mutableStateOf("") }
    var showingCompleted by remember { mutableStateOf(false) }
    val normalizedSearch = remember(searchTxt) { viewModel.repository.normalizeArabic(searchTxt) }
    val filteredTasks = remember(tasks, normalizedSearch, showingCompleted) {
        tasks.filter { task ->
            val haystack = viewModel.repository.normalizeArabic(
                "${task.title} ${task.description} ${task.caseTitle.orEmpty()} ${task.clientName.orEmpty()} ${task.priority} ${task.status}"
            )
            val matchesSearch = normalizedSearch.isBlank() || haystack.contains(normalizedSearch)
            val matchesCompleted = if (showingCompleted) task.status == "منتهية" else task.status != "منتهية"
            matchesSearch && matchesCompleted
        }
    }
    val openCount = remember(tasks) { tasks.count { it.status != "منتهية" } }
    val completedCount = remember(tasks) { tasks.count { it.status == "منتهية" } }
    val caseLinkedCount = remember(tasks, cases) { tasks.mapNotNull { it.caseId }.distinct().size.coerceAtMost(cases.size) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.navigateTo(Screen.TaskAddEdit()) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "مهمة جديدة")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .legalScreenBackground()
                .padding(padding)
                .padding(horizontal = MohamyDimens.screenHorizontal, vertical = MohamyDimens.screenVertical),
            verticalArrangement = Arrangement.spacedBy(MohamyDimens.sectionGap)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(MohamyDimens.largeCardRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "إدارة المهام",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                "تنظيم المتابعات القانونية والإدارية مع إبراز المهام المتأخرة بوضوح.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        MohamyStatusBadge(
                            text = "${filteredTasks.size} نتيجة",
                            tone = MohamyBadgeTone.Gold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TaskStatCard("مفتوحة", openCount.toString(), Modifier.weight(1f))
                        TaskStatCard("منتهية", completedCount.toString(), Modifier.weight(1f))
                        TaskStatCard("مرتبطة بقضايا", caseLinkedCount.toString(), Modifier.weight(1f))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = !showingCompleted,
                    onClick = { showingCompleted = false },
                    label = { Text("المهام المفتوحة") },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                FilterChip(
                    selected = showingCompleted,
                    onClick = { showingCompleted = true },
                    label = { Text("المهام المنتهية") },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }

            MohamySearchBar(
                value = searchTxt,
                onValueChange = { searchTxt = it },
                placeholder = "ابحث بعنوان المهمة أو القضية أو الموكل أو الوصف..."
            )

            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    MohamyEmptyState(
                        icon = Icons.Default.TaskAlt,
                        title = if (tasks.isEmpty()) "لا توجد مهام محفوظة بعد" else "لا توجد مهام مطابقة",
                        message =
                            if (tasks.isEmpty()) {
                                "أضف أول مهمة قانونية أو إدارية لمتابعة سير العمل اليومي."
                            } else {
                                "جرّب تغيير البحث أو بدّل بين المهام المفتوحة والمنتهية."
                            },
                        actionText = if (tasks.isEmpty()) "إضافة مهمة" else null,
                        onActionClick =
                            if (tasks.isEmpty()) {
                                { viewModel.navigateTo(Screen.TaskAddEdit()) }
                            } else {
                                null
                            },
                        secondaryActionText = if (tasks.isEmpty() && !viewModel.hasDemoSeededForCurrentWorkspace) "مساحة تجريبية" else null,
                        onSecondaryActionClick = if (tasks.isEmpty() && !viewModel.hasDemoSeededForCurrentWorkspace) {
                            { viewModel.seedDemoWorkspace() }
                        } else {
                            null
                        }
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
                        val overdue = task.isOverdueToday()
                        TaskCard(
                            task = task,
                            isOverdue = overdue,
                            onClick = { viewModel.navigateTo(Screen.TaskAddEdit(taskId = task.id)) }
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Checkbox(
                                    checked = task.status == "منتهية",
                                    onCheckedChange = { viewModel.toggleTaskCompleted(task) }
                                )
                                Text(
                                    text = if (task.status == "منتهية") "إعادة فتح" else "تمت",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            IconButton(
                                onClick = { viewModel.navigateTo(Screen.TaskAddEdit(taskId = task.id)) },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(
                                onClick = { viewModel.deleteTask(task) },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f))
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskStatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun LegalTask.isOverdueToday(now: Long = System.currentTimeMillis()): Boolean {
    if (status == "منتهية" || dueDate.isBlank()) return false
    return runCatching {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        format.isLenient = false
        val due = format.parse(dueDate)?.time ?: return false
        due < now
    }.getOrDefault(false)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskAddEditScreen(taskId: Int?, presetCaseId: Int?, viewModel: AppViewModel, cases: List<LegalCase>) {
    val editing = taskId?.let { id -> viewModel.allTasks.value.find { it.id == id } }

    var title by remember { mutableStateOf(editing?.title ?: "") }
    var description by remember { mutableStateOf(editing?.description ?: "") }
    var dueDate by remember { mutableStateOf(editing?.dueDate ?: "") }
    var priority by remember { mutableStateOf(editing?.priority ?: "متوسطة") }

    var selectedCaseIndex by remember(editing?.caseId, cases) {
        mutableStateOf(
            cases.indexOfFirst { it.id == (editing?.caseId ?: presetCaseId) }
                .takeIf { it >= 0 }
                ?: 0
        )
    }
    var caseExpanded by remember { mutableStateOf(false) }
    var priorityExpanded by remember { mutableStateOf(false) }
    val priorityOptions = listOf("عاجل", "متوسطة", "عادية")

    LaunchedEffect(presetCaseId, cases) {
        if (presetCaseId != null) {
            val i = cases.indexOfFirst { it.id == presetCaseId }
            if (i != -1) selectedCaseIndex = i
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
                        imageVector = if (editing == null) Icons.Default.AddTask else Icons.Default.Edit,
                        contentDescription = null,
                        tint = LegalGoldLight,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = if (editing == null) "إضافة مهمة جديدة لموظفي أو مكتب المحاماة" else "تعديل تفاصيل المهمة",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "نظم مهامك ومسؤولياتك بفاعلية",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("عنوان المهمة / المتطلب المكتبي (مطلوب)") },
            leadingIcon = { Icon(Icons.Default.TaskAlt, null, tint = LegalNavyPrimary) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LegalGoldSecondary, focusedLabelColor = LegalNavyPrimary)
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = dueDate,
            onValueChange = { dueDate = it },
            label = { Text("تاريخ الاستحقاق (YYYY-MM-DD)") },
            leadingIcon = { Icon(Icons.Default.DateRange, null, tint = LegalNavyPrimary) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("2026-06-15") },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LegalGoldSecondary, focusedLabelColor = LegalNavyPrimary)
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (cases.isNotEmpty()) {
            ExposedDropdownMenuBox(
                expanded = caseExpanded,
                onExpandedChange = { caseExpanded = !caseExpanded }
            ) {
                OutlinedTextField(
                    value = cases.getOrNull(selectedCaseIndex)?.title ?: "اختر القضية الملحقة بها",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("ارتباط بملف دعوى محددة (اختياري)") },
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
            Spacer(modifier = Modifier.height(16.dp))
        }

        ExposedDropdownMenuBox(
            expanded = priorityExpanded,
            onExpandedChange = { priorityExpanded = !priorityExpanded }
        ) {
            OutlinedTextField(
                value = priority,
                onValueChange = {},
                readOnly = true,
                label = { Text("أولوية المهمة") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = priorityExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LegalGoldSecondary,
                    focusedLabelColor = LegalNavyPrimary
                )
            )
            ExposedDropdownMenu(
                expanded = priorityExpanded,
                onDismissRequest = { priorityExpanded = false }
            ) {
                priorityOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            priority = option
                            priorityExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("شرح وتوصيف المهمة القانونية بالتفصيل") },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Subject, null, tint = LegalNavyPrimary) },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LegalGoldSecondary, focusedLabelColor = LegalNavyPrimary)
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val boundCase = cases.getOrNull(selectedCaseIndex)
                val toSave = LegalTask(
                    id = editing?.id ?: 0,
                    title = title,
                    description = description,
                    dueDate = dueDate,
                    priority = priority,
                    status = editing?.status ?: "مفتوحة",
                    caseId = boundCase?.id,
                    caseTitle = boundCase?.title,
                    clientId = boundCase?.clientId,
                    clientName = boundCase?.clientName
                )
                viewModel.saveTask(toSave) {
                    viewModel.navigateTo(Screen.TasksList)
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
            enabled = title.isNotEmpty()
        ) {
            Icon(Icons.Default.Save, contentDescription = null, tint = LegalGoldLight)
            Spacer(modifier = Modifier.width(8.dp))
            Text("حفظ وإدراج كطلب بالمكتب", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}
