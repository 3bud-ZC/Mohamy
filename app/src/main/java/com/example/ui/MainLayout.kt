package com.example.ui

import android.os.Build
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
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
import com.example.MainActivity
import com.example.data.*
import com.example.ui.theme.LegalGoldSecondary
import com.example.ui.theme.LegalNavyPrimary
import com.example.ui.theme.LegalGoldLight
import com.example.ui.theme.legalScreenBackground
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import org.json.JSONObject
import org.json.JSONArray
import com.example.ui.screens.*
import kotlinx.coroutines.flow.flowOf

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

// Removed BottomDockItem to use standard NavigationBar

tailrec fun Context.findHostActivity(): androidx.activity.ComponentActivity? {
    return when (this) {
        is androidx.activity.ComponentActivity -> this
        is ContextWrapper -> baseContext.findHostActivity()
        else -> null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLayout(viewModel: AppViewModel) {
    // Force RTL direction for Arabic Layout Harmony
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val context = LocalContext.current
        val hostActivity = remember(context) { context.findHostActivity() }
        val activeScreen = viewModel.currentScreen
        val shouldCollectWorkspaceData = activeScreen !is Screen.Splash && activeScreen !is Screen.Activation
        val notificationPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (!granted) {
                viewModel.globalInfoMsg = "يمكنك تفعيل إشعارات الهاتف لاحقاً من إعدادات الجهاز أو داخل التطبيق."
            }
        }
        var notificationPermissionRequested by remember { mutableStateOf(false) }

        BackHandler(enabled = activeScreen != Screen.Splash && activeScreen != Screen.Activation) {
            if (!viewModel.goBack()) {
                if (activeScreen != Screen.Dashboard) {
                    viewModel.navigateTo(Screen.Dashboard, addToBackStack = false)
                } else {
                    hostActivity?.finish()
                }
            }
        }

        // Observe Room states
        val clientsFlow = remember(activeScreen, shouldCollectWorkspaceData) {
            if (shouldCollectWorkspaceData) viewModel.allClients else flowOf(emptyList<Client>())
        }
        val casesFlow = remember(activeScreen, shouldCollectWorkspaceData) {
            if (shouldCollectWorkspaceData) viewModel.allCases else flowOf(emptyList<LegalCase>())
        }
        val archivedCasesFlow = remember(activeScreen, shouldCollectWorkspaceData) {
            if (shouldCollectWorkspaceData) viewModel.archivedCases else flowOf(emptyList<LegalCase>())
        }
        val sessionsFlow = remember(activeScreen, shouldCollectWorkspaceData) {
            if (shouldCollectWorkspaceData) viewModel.allSessions else flowOf(emptyList<CaseSession>())
        }
        val tasksFlow = remember(activeScreen, shouldCollectWorkspaceData) {
            if (shouldCollectWorkspaceData) viewModel.allTasks else flowOf(emptyList<LegalTask>())
        }
        val filesFlow = remember(activeScreen, shouldCollectWorkspaceData) {
            if (shouldCollectWorkspaceData) viewModel.allFiles else flowOf(emptyList<CaseFile>())
        }
        val clientInteractionsFlow = remember(activeScreen, shouldCollectWorkspaceData) {
            if (shouldCollectWorkspaceData) viewModel.allClientInteractions else flowOf(emptyList<ClientInteraction>())
        }
        val templatesFlow = remember(activeScreen, shouldCollectWorkspaceData) {
            if (shouldCollectWorkspaceData) viewModel.allTemplates else flowOf(emptyList<LegalTemplate>())
        }
        val generatedDocsFlow = remember(activeScreen, shouldCollectWorkspaceData) {
            if (shouldCollectWorkspaceData) viewModel.allGeneratedDocuments else flowOf(emptyList<GeneratedDocument>())
        }
        val customCategoriesFlow = remember(activeScreen, shouldCollectWorkspaceData) {
            if (shouldCollectWorkspaceData) viewModel.allCustomCaseCategories else flowOf(emptyList<CustomCaseCategory>())
        }
        val licenseFlow = remember(activeScreen, shouldCollectWorkspaceData) {
            if (shouldCollectWorkspaceData) viewModel.licenseState else flowOf<LicenseCache?>(null)
        }

        val clients by clientsFlow.collectAsState(initial = emptyList())
        val cases by casesFlow.collectAsState(initial = emptyList())
        val archivedList by archivedCasesFlow.collectAsState(initial = emptyList())
        val sessions by sessionsFlow.collectAsState(initial = emptyList())
        val tasks by tasksFlow.collectAsState(initial = emptyList())
        val files by filesFlow.collectAsState(initial = emptyList())
        val clientInteractions by clientInteractionsFlow.collectAsState(initial = emptyList())
        val templates by templatesFlow.collectAsState(initial = emptyList())
        val generatedDocs by generatedDocsFlow.collectAsState(initial = emptyList())
        val customCategories by customCategoriesFlow.collectAsState(initial = emptyList())
        val licenseObj by licenseFlow.collectAsState(initial = null)

        LaunchedEffect(viewModel.appReloadNonce) {
            if (viewModel.appReloadNonce > 0) {
                viewModel.consumeAppReload()
                hostActivity?.let { activity ->
                    val restartIntent = Intent(activity, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                    activity.startActivity(restartIntent)
                    activity.finish()
                }
            }
        }

        LaunchedEffect(activeScreen) {
            if (!notificationPermissionRequested && activeScreen !is Screen.Splash && activeScreen !is Screen.Activation) {
                notificationPermissionRequested = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !com.example.data.AppNotificationManager.hasNotificationPermission(context)
                ) {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
        var pendingImportName by remember { mutableStateOf("") }
        var pendingImportSize by remember { mutableStateOf(0L) }
        var pendingImportType by remember { mutableStateOf("عقد") }
        var pendingImportCustomType by remember { mutableStateOf("") }
        var pendingImportLinkedSessionId by remember { mutableStateOf<Int?>(null) }
        var showImportDialog by remember { mutableStateOf(false) }
        var typeDropdownExpanded by remember { mutableStateOf(false) }
        var sessionDropdownExpanded by remember { mutableStateOf(false) }

        // Handle File Pickers
        val fileImportLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                val resolved = getUriMetadata(context, it)
                pendingImportUri = it
                pendingImportName = resolved.first
                pendingImportSize = resolved.second
                pendingImportType = viewModel.suggestDocumentType(resolved.first)
                pendingImportCustomType = ""
                pendingImportLinkedSessionId = null
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
            containerColor = Color.Transparent,
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
                                IconButton(onClick = {
                                    if (!viewModel.goBack()) {
                                        viewModel.navigateTo(Screen.Dashboard, addToBackStack = false)
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "رجوع",
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
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        tonalElevation = 8.dp,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        val items = listOf(
                            Triple("المكتبة", Icons.Default.FolderCopy, activeScreen is Screen.FilesLibrary || activeScreen is Screen.Search),
                            Triple("القضايا", Icons.Default.Gavel, activeScreen is Screen.CasesList || activeScreen is Screen.CaseDetails || activeScreen is Screen.CaseAddEdit),
                            Triple("المساعد", Icons.Default.AutoAwesome, activeScreen is Screen.SmartAssistant),
                            Triple("الموكلون", Icons.Default.People, activeScreen is Screen.ClientsList || activeScreen is Screen.ClientDetails || activeScreen is Screen.ClientAddEdit),
                            Triple("الجلسات", Icons.Default.CalendarToday, activeScreen is Screen.SessionsList || activeScreen is Screen.SessionAddEdit)
                        )
                        
                        items.forEach { (label, icon, selected) ->
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    when (label) {
                                        "المكتبة" -> viewModel.navigateTo(Screen.FilesLibrary)
                                        "القضايا" -> viewModel.navigateTo(Screen.CasesList)
                                        "المساعد" -> viewModel.navigateTo(Screen.SmartAssistant)
                                        "الموكلون" -> viewModel.navigateTo(Screen.ClientsList)
                                        "الجلسات" -> viewModel.navigateTo(Screen.SessionsList)
                                    }
                                },
                                icon = { Icon(imageVector = icon, contentDescription = label) },
                                label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = LegalNavyPrimary,
                                    selectedTextColor = LegalNavyPrimary,
                                    indicatorColor = LegalGoldSecondary.copy(alpha = 0.2f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .legalScreenBackground()
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
                        is Screen.ClientDetails -> ClientDetailsScreen(screen.clientId, viewModel, clients, cases, files, sessions, clientInteractions)
                        is Screen.ClientAddEdit -> ClientAddEditScreen(screen.clientId, viewModel, clients)
                        is Screen.CasesList -> CasesListScreen(viewModel, cases, archivedList, customCategories)
                        is Screen.CaseDetails -> CaseDetailsScreen(screen.caseId, viewModel, cases, sessions, tasks, files, generatedDocs, fileImportLauncher)
                        is Screen.CaseAddEdit -> CaseAddEditScreen(screen.caseId, viewModel, clients, customCategories)
                        is Screen.SessionsList -> SessionsListScreen(viewModel, sessions, cases)
                        is Screen.SessionAddEdit -> SessionAddEditScreen(screen.sessionId, screen.presetCaseId, viewModel, cases)
                        is Screen.TasksList -> TasksListScreen(viewModel, tasks, cases)
                        is Screen.TaskAddEdit -> TaskAddEditScreen(screen.taskId, screen.presetCaseId, viewModel, cases)
                        is Screen.LegalTemplatesList -> LegalTemplatesScreen(viewModel, templates)
                        is Screen.TemplateForm -> TemplateFormScreen(screen.templateId, screen.presetCaseId, viewModel, templates, cases)
                        is Screen.BackupRestore -> BackupRestoreScreen(viewModel, dbRestoreLauncher)
                        is Screen.Settings -> SettingsScreen(
                            viewModel,
                            licenseObj,
                            onRequestNotificationPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        )
                        is Screen.Search -> SearchScreen(viewModel, files, cases, clients, sessions, tasks, templates, generatedDocs)
                        is Screen.FilesLibrary -> FilesLibraryScreen(viewModel, files, cases, clients)
                        is Screen.SmartAssistant -> SmartAssistantScreen(viewModel, cases, files, templates)
                        is Screen.ImportData -> ImportDataScreen(viewModel)
                    }
                }

                if (showImportDialog && pendingImportUri != null) {
                    val caseObj = viewModel.activeCase
                    if (caseObj != null) {
                        val caseSessions = sessions.filter { it.caseId == caseObj.id }
                        AlertDialog(
                            onDismissRequest = {
                                pendingImportLinkedSessionId = null
                                sessionDropdownExpanded = false
                                showImportDialog = false
                            },
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
                                    
                                    val docTypesList = listOf("مستند", "عقد", "توكيل", "محضر", "حكم", "مذكرة", "عريضة دعوى", "هوية", "إيصال", "صورة مستند", "أخرى")
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
                                                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
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

                                    if (caseSessions.isNotEmpty()) {
                                        Text("ربط المستند بجلسة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        ExposedDropdownMenuBox(
                                            expanded = sessionDropdownExpanded,
                                            onExpandedChange = { sessionDropdownExpanded = !sessionDropdownExpanded }
                                        ) {
                                            OutlinedTextField(
                                                value = pendingImportLinkedSessionId
                                                    ?.let { selectedId -> caseSessions.find { it.id == selectedId } }
                                                    ?.let { "${it.title} - ${it.date}" }
                                                    ?: "بدون ربط بجلسة",
                                                onValueChange = {},
                                                readOnly = true,
                                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sessionDropdownExpanded) },
                                                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                            )
                                            ExposedDropdownMenu(
                                                expanded = sessionDropdownExpanded,
                                                onDismissRequest = { sessionDropdownExpanded = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("بدون ربط بجلسة") },
                                                    onClick = {
                                                        pendingImportLinkedSessionId = null
                                                        sessionDropdownExpanded = false
                                                    }
                                                )
                                                caseSessions.forEach { session ->
                                                    DropdownMenuItem(
                                                        text = { Text("${session.title} - ${session.date}") },
                                                        onClick = {
                                                            pendingImportLinkedSessionId = session.id
                                                            sessionDropdownExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
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
                                            docLength = pendingImportSize,
                                            linkedSessionId = pendingImportLinkedSessionId
                                        )
                                        pendingImportLinkedSessionId = null
                                        sessionDropdownExpanded = false
                                        showImportDialog = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary),
                                    enabled = pendingImportType != "أخرى" || pendingImportCustomType.isNotBlank()
                                ) {
                                    Text("أرشفة وتأكيد")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = {
                                    pendingImportLinkedSessionId = null
                                    sessionDropdownExpanded = false
                                    showImportDialog = false
                                }) {
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

                viewModel.globalInfoMsg?.let { msg ->
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        action = {
                            TextButton(onClick = { viewModel.globalInfoMsg = null }) {
                                Text("حسناً", color = MaterialTheme.colorScheme.onPrimaryContainer)
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

