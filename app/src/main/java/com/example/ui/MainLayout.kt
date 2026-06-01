package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.*
import com.example.ui.theme.LegalGoldSecondary
import com.example.ui.theme.LegalNavyPrimary
import com.example.ui.theme.LegalGoldLight
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import org.json.JSONObject
import org.json.JSONArray
import com.example.ui.screens.*

fun getUriMetadata(context: android.content.Context, uri: Uri): Pair<String, Long> {
    var rawName = "doc_" + System.currentTimeMillis() + ".txt"
    var rawSize = 0L
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) {
                    val resolvedName = cursor.getString(nameIndex)
                    if (!resolvedName.isNullOrEmpty()) {
                        rawName = resolvedName
                    }
                }
                if (sizeIndex != -1) {
                    rawSize = cursor.getLong(sizeIndex)
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return Pair(rawName, rawSize)
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 بايت"
    if (bytes < 1024) return "$bytes بايت"
    if (bytes < 1024 * 1024) return String.format(Locale.ENGLISH, "%.1f كيلوبايت", bytes / 1024.0)
    return String.format(Locale.ENGLISH, "%.1f ميجابايت", bytes / (1024.0 * 1024.0))
}

fun openCaseFile(context: Context, f: CaseFile) {
    try {
        val file = File(f.filePath)
        if (!file.exists()) {
            Toast.makeText(context, "العدول عن الفتح: الملف المحفوظ غير متواجد على تخزين الهاتف المحلي.", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val mime = context.contentResolver.getType(uri) ?: when (file.extension.lowercase()) {
            "pdf" -> "application/pdf"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            else -> "text/plain"
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "لا يوجد تطبيق مناسب على هاتفك لعرض هذا الملف: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLayout(viewModel: AppViewModel) {
    // Force RTL direction for Arabic Layout Harmony
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val context = LocalContext.current
        val activeScreen = viewModel.currentScreen

        // Observe Room states
        val clients by viewModel.allClients.collectAsState()
        val cases by viewModel.allCases.collectAsState()
        val archivedList by viewModel.archivedCases.collectAsState()
        val sessions by viewModel.allSessions.collectAsState()
        val tasks by viewModel.allTasks.collectAsState()
        val files by viewModel.allFiles.collectAsState()
        val templates by viewModel.allTemplates.collectAsState()
        val generatedDocs by viewModel.allGeneratedDocuments.collectAsState()
        val licenseObj by viewModel.licenseState.collectAsState()

        var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
        var pendingImportName by remember { mutableStateOf("") }
        var pendingImportSize by remember { mutableStateOf(0L) }
        var pendingImportType by remember { mutableStateOf("عقد") }
        var pendingImportCustomType by remember { mutableStateOf("") }
        var showImportDialog by remember { mutableStateOf(false) }
        var typeDropdownExpanded by remember { mutableStateOf(false) }

        // Handle File Pickers
        val fileImportLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                val resolved = getUriMetadata(context, it)
                pendingImportUri = it
                pendingImportName = resolved.first
                pendingImportSize = resolved.second
                pendingImportType = "عقد"
                pendingImportCustomType = ""
                showImportDialog = true
            }
        }

        val dbRestoreLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                viewModel.executeDatabaseRestore(it) {
                    // Refresh database or let system trigger
                    Toast.makeText(context, "تم استرداد البيانات بنجاح!", Toast.LENGTH_LONG).show()
                }
            }
        }

        Scaffold(
            topBar = {
                if (activeScreen != Screen.Splash && activeScreen != Screen.Activation) {
                    TopAppBar(
                        title = {
                            Text(
                                text = "محامي فون",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            titleContentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        actions = {
                            IconButton(onClick = { viewModel.navigateTo(Screen.SmartAssistant) }) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "المساعد الذكي",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            IconButton(onClick = { viewModel.navigateTo(Screen.Search) }) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "بحث المستندات والأدلة",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            IconButton(onClick = { viewModel.navigateTo(Screen.Settings) }) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "الإعدادات",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        },
                        navigationIcon = {
                            if (activeScreen != Screen.Dashboard) {
                                IconButton(onClick = { viewModel.navigateTo(Screen.Dashboard) }) {
                                    Icon(
                                        imageVector = Icons.Default.Home,
                                        contentDescription = "الرئيسية",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (activeScreen != Screen.Splash && activeScreen != Screen.Activation) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        val navColors = NavigationBarItemDefaults.colors(
                            selectedIconColor = LegalNavyPrimary,
                            selectedTextColor = LegalNavyPrimary,
                            indicatorColor = LegalGoldSecondary.copy(alpha = 0.25f),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                        NavigationBarItem(
                            selected = activeScreen is Screen.Dashboard,
                            onClick = { viewModel.navigateTo(Screen.Dashboard) },
                            icon = { Icon(Icons.Default.Dashboard, "لوحة التحكم") },
                            label = { Text("المكتب", fontSize = 11.sp) },
                            colors = navColors
                        )
                        NavigationBarItem(
                            selected = activeScreen is Screen.CasesList || activeScreen is Screen.CaseDetails,
                            onClick = { viewModel.navigateTo(Screen.CasesList) },
                            icon = { Icon(Icons.Default.Gavel, "القضايا") },
                            label = { Text("القضايا", fontSize = 11.sp) },
                            colors = navColors
                        )
                        NavigationBarItem(
                            selected = activeScreen is Screen.ClientsList || activeScreen is Screen.ClientDetails,
                            onClick = { viewModel.navigateTo(Screen.ClientsList) },
                            icon = { Icon(Icons.Default.People, "العملاء") },
                            label = { Text("العملاء", fontSize = 11.sp) },
                            colors = navColors
                        )
                        NavigationBarItem(
                            selected = activeScreen is Screen.SessionsList,
                            onClick = { viewModel.navigateTo(Screen.SessionsList) },
                            icon = { Icon(Icons.Default.CalendarToday, "الجلسات") },
                            label = { Text("الجلسات", fontSize = 11.sp) },
                            colors = navColors
                        )
                        NavigationBarItem(
                            selected = activeScreen is Screen.TasksList,
                            onClick = { viewModel.navigateTo(Screen.TasksList) },
                            icon = { Icon(Icons.Default.Task, "المهام") },
                            label = { Text("المهام", fontSize = 11.sp) },
                            colors = navColors
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Main Switch Routing UI Screen Rendering
                AnimatedContent(
                    targetState = activeScreen,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "ScreenTransition"
                ) { screen ->
                    when (screen) {
                        is Screen.Splash -> SplashScreen()
                        is Screen.Activation -> ActivationScreen(viewModel)
                        is Screen.Dashboard -> DashboardScreen(
                            viewModel,
                            clients,
                            cases,
                            sessions,
                            tasks,
                            files
                        )
                        is Screen.ClientsList -> ClientsListScreen(viewModel, clients)
                        is Screen.ClientDetails -> ClientDetailsScreen(screen.clientId, viewModel, clients, cases)
                        is Screen.ClientAddEdit -> ClientAddEditScreen(screen.clientId, viewModel, clients)
                        is Screen.CasesList -> CasesListScreen(viewModel, cases, archivedList)
                        is Screen.CaseDetails -> CaseDetailsScreen(screen.caseId, viewModel, cases, sessions, tasks, files, generatedDocs, fileImportLauncher)
                        is Screen.CaseAddEdit -> CaseAddEditScreen(screen.caseId, viewModel, clients)
                        is Screen.SessionsList -> SessionsListScreen(viewModel, sessions, cases)
                        is Screen.SessionAddEdit -> SessionAddEditScreen(screen.sessionId, screen.presetCaseId, viewModel, cases)
                        is Screen.TasksList -> TasksListScreen(viewModel, tasks, cases)
                        is Screen.TaskAddEdit -> TaskAddEditScreen(screen.taskId, screen.presetCaseId, viewModel, cases)
                        is Screen.LegalTemplatesList -> LegalTemplatesScreen(viewModel, templates)
                        is Screen.TemplateForm -> TemplateFormScreen(screen.templateId, screen.presetCaseId, viewModel, templates, cases)
                        is Screen.BackupRestore -> BackupRestoreScreen(viewModel, dbRestoreLauncher)
                        is Screen.Settings -> SettingsScreen(viewModel, licenseObj)
                        is Screen.Search -> SearchScreen(viewModel, files, cases, clients, sessions, tasks, templates, generatedDocs)
                        is Screen.SmartAssistant -> SmartAssistantScreen(viewModel, cases, files, templates)
                        is Screen.ImportData -> ImportDataScreen(viewModel)
                    }
                }

                if (showImportDialog && pendingImportUri != null) {
                    val caseObj = viewModel.activeCase
                    if (caseObj != null) {
                        AlertDialog(
                            onDismissRequest = { showImportDialog = false },
                            title = {
                                Text(
                                    "تأكيد استيراد وأرشفة مستند 📄",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = LegalNavyPrimary
                                )
                            },
                            text = {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "سيتم نسخ هذا الملف بخصوصية تامة داخل المساحة الآمنة للتطبيق على هاتفك وربطه بملف القضية والعميل.",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                    
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text("اسم الملف الأصلي:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = LegalNavyPrimary)
                                            Text(pendingImportName, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("الحجم على القرص:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = LegalNavyPrimary)
                                            Text(formatFileSize(pendingImportSize), fontSize = 12.sp)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("ارتباط بالقضية:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = LegalNavyPrimary)
                                            Text(caseObj.title, fontSize = 12.sp)
                                        }
                                    }

                                    Text("تحديد تصنيف المستند القانوني:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    
                                    val docTypesList = listOf("عقد", "توكيل", "محضر", "حكم", "عريضة دعوى", "أخرى")
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        ExposedDropdownMenuBox(
                                            expanded = typeDropdownExpanded,
                                            onExpandedChange = { typeDropdownExpanded = !typeDropdownExpanded }
                                        ) {
                                            OutlinedTextField(
                                                value = pendingImportType,
                                                onValueChange = {},
                                                readOnly = true,
                                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                                                modifier = Modifier.fillMaxWidth().menuAnchor()
                                            )
                                            ExposedDropdownMenu(
                                                expanded = typeDropdownExpanded,
                                                onDismissRequest = { typeDropdownExpanded = false }
                                            ) {
                                                docTypesList.forEach { type ->
                                                    DropdownMenuItem(
                                                        text = { Text(type) },
                                                        onClick = {
                                                            pendingImportType = type
                                                            typeDropdownExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (pendingImportType == "أخرى") {
                                        OutlinedTextField(
                                            value = pendingImportCustomType,
                                            onValueChange = { pendingImportCustomType = it },
                                            label = { Text("أدخل نوع المستند يدوياً") },
                                            placeholder = { Text("مثال: شكوى، مراسلة، إقرار") },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        val finalDocType = if (pendingImportType == "أخرى") {
                                            pendingImportCustomType.trim().ifEmpty { "أخرى" }
                                        } else {
                                            pendingImportType
                                        }
                                        viewModel.importCaseFile(
                                            caseId = caseObj.id,
                                            clientId = caseObj.clientId,
                                            fileName = pendingImportName,
                                            fileUri = pendingImportUri!!,
                                            docType = finalDocType,
                                            docLength = pendingImportSize
                                        )
                                        showImportDialog = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary),
                                    enabled = pendingImportType != "أخرى" || pendingImportCustomType.isNotBlank()
                                ) {
                                    Text("أرشفة وتأكيد")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showImportDialog = false }) {
                                    Text("إلغاء", color = Color.Gray)
                                }
                            }
                        )
                    } else {
                        showImportDialog = false
                    }
                }

                // Show global error/success snackbars
                viewModel.globalSuccessMsg?.let { msg ->
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        action = {
                            TextButton(onClick = { viewModel.globalSuccessMsg = null }) {
                                Text("حسناً", color = MaterialTheme.colorScheme.primaryContainer)
                            }
                        }
                    ) {
                        Text(msg)
                    }
                }

                viewModel.globalErrorMsg?.let { msg ->
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        action = {
                            TextButton(onClick = { viewModel.globalErrorMsg = null }) {
                                Text("إغلاق", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    ) {
                        Text(msg)
                    }
                }
            }
        }
    }
}

