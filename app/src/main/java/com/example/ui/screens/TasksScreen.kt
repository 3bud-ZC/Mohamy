package com.example.ui.screens
import com.example.data.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LegalGoldLight
import com.example.ui.theme.LegalGoldSecondary
import com.example.ui.theme.LegalGrayLight
import com.example.ui.theme.LegalNavyPrimary

@Composable
fun TasksListScreen(viewModel: AppViewModel, tasks: List<LegalTask>, cases: List<LegalCase>) {
    var searchTxt by remember { mutableStateOf("") }
    var showingCompleted by remember { mutableStateOf(false) }

    val filtered = tasks.filter {
        val matchesSearch = it.title.contains(searchTxt) || (it.caseTitle ?: "").contains(searchTxt)
        val matchesCompleted = if (showingCompleted) it.status == "منتهية" else it.status == "مفتوحة"
        matchesSearch && matchesCompleted
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.navigateTo(Screen.TaskAddEdit()) },
                containerColor = LegalNavyPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.AddTask, "مهمة جديدة")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showingCompleted = false },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = if (!showingCompleted) LegalNavyPrimary else Color.LightGray)
                ) {
                    Text("مهام معلقة")
                }
                Button(
                    onClick = { showingCompleted = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = if (showingCompleted) LegalNavyPrimary else Color.LightGray)
                ) {
                    Text("مهام منتهية")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchTxt,
                onValueChange = { searchTxt = it },
                placeholder = { Text("ابحث في المهام والمجهودات القانونية...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Search, null) }
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("لا توجد مهام مطابقة للمواصفات حالياً.", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filtered) { tsk ->
                        val completed = tsk.status == "منتهية"
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable { viewModel.navigateTo(Screen.TaskAddEdit(taskId = tsk.id)) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = if (completed) LegalGrayLight else Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (completed) 0.dp else 2.dp)
                        ) {
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                leadingContent = {
                                    Checkbox(
                                        checked = completed,
                                        onCheckedChange = { viewModel.toggleTaskCompleted(tsk) },
                                        colors = CheckboxDefaults.colors(checkedColor = LegalNavyPrimary)
                                    )
                                },
                                headlineContent = { 
                                    Text(
                                        text = tsk.title, 
                                        fontWeight = FontWeight.Bold, 
                                        color = if (completed) Color.Gray else LegalNavyPrimary,
                                        style = if (completed) androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough) else androidx.compose.ui.text.TextStyle.Default,
                                        fontSize = 16.sp
                                    ) 
                                },
                                supportingContent = { 
                                    Column(modifier = Modifier.padding(top = 4.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("الاستحقاق: ${tsk.dueDate}", color = Color.Gray, fontSize = 12.sp) 
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("ارتباط: ${tsk.caseTitle ?: "مهمة إدارية عامة"}", color = Color.DarkGray, fontSize = 12.sp)
                                    }
                                },
                                trailingContent = {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        IconButton(
                                            onClick = { viewModel.navigateTo(Screen.TaskAddEdit(taskId = tsk.id)) },
                                            modifier = Modifier.background(LegalNavyPrimary.copy(alpha = 0.12f), CircleShape)
                                        ) {
                                            Icon(Icons.Default.Edit, "تعديل", tint = LegalNavyPrimary)
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteTask(tsk) },
                                            modifier = Modifier.background(Color.Red.copy(alpha = 0.1f), CircleShape)
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, "حذف", tint = Color.Red)
                                        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskAddEditScreen(taskId: Int?, presetCaseId: Int?, viewModel: AppViewModel, cases: List<LegalCase>) {
    val editing = taskId?.let { id -> viewModel.allTasks.value.find { it.id == id } }

    var title by remember { mutableStateOf(editing?.title ?: "") }
    var description by remember { mutableStateOf(editing?.description ?: "") }
    var dueDate by remember { mutableStateOf(editing?.dueDate ?: "") }
    var priority by remember { mutableStateOf(editing?.priority ?: "متوسطة") }

    var selectedCaseIndex by remember { mutableStateOf(0) }
    var caseExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(presetCaseId, cases) {
        if (presetCaseId != null) {
            val i = cases.indexOfFirst { it.id == presetCaseId }
            if (i != -1) selectedCaseIndex = i
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
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
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
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

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("شرح وتوصيف المهمة القانونية بالتفصيل") },
            leadingIcon = { Icon(Icons.Default.Subject, null, tint = LegalNavyPrimary) },
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
                    status = "مفتوحة",
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
