package com.example.data

import android.app.Application
import android.net.Uri
import com.example.BuildConfig
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.runtime.snapshotFlow
import java.text.SimpleDateFormat
import java.util.Locale

sealed class Screen {
    object Splash : Screen()
    object Activation : Screen()
    object Dashboard : Screen()
    object ClientsList : Screen()
    data class ClientDetails(val clientId: Int) : Screen()
    data class ClientAddEdit(val clientId: Int? = null) : Screen()
    object CasesList : Screen()
    data class CaseDetails(val caseId: Int) : Screen()
    data class CaseAddEdit(val caseId: Int? = null) : Screen()
    object SessionsList : Screen()
    data class SessionAddEdit(val sessionId: Int? = null, val presetCaseId: Int? = null) : Screen()
    object TasksList : Screen()
    data class TaskAddEdit(val taskId: Int? = null, val presetCaseId: Int? = null) : Screen()
    object LegalTemplatesList : Screen()
    data class TemplateForm(val templateId: Int, val presetCaseId: Int? = null) : Screen()
    object BackupRestore : Screen()
    object Settings : Screen()
    object Search : Screen()
    object SmartAssistant : Screen()
    object ImportData : Screen()
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("mohamy_phone_prefs", Application.MODE_PRIVATE)
    private val db = AppDatabase.getDatabase(application)
    val repository = Repository(db, application)

    // --- State Routing ---
    var currentScreen by mutableStateOf<Screen>(Screen.Splash)
        private set

    fun navigateTo(screen: Screen) {
        currentScreen = screen
    }

    // --- Active Selection State ID Storage ---
    var activeClient: Client? by mutableStateOf(null)
    var activeCase: LegalCase? by mutableStateOf(null)

    // --- State Flows from Room ---
    val allClients = repository.clientDao.getAllClients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCases = repository.caseDao.getAllActiveCases()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedCases = repository.caseDao.getAllArchivedCases()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSessions = repository.sessionDao.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasks = repository.taskDao.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFiles = repository.fileDao.getAllFiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTemplates = repository.templateDao.getAllTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allGeneratedDocuments = repository.generatedDocDao.getAllGeneratedDocuments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val licenseState = repository.licenseDao.getLicenseFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- Search Engine Flow & Normalization ---
    var searchEngineQuery by mutableStateOf("")
    val searchEngineResult = snapshotFlow { searchEngineQuery }
        .debounce(400)
        .flatMapLatest { query ->
            if (query.trim().isEmpty()) {
                flowOf(emptyList())
            } else {
                val normalized = repository.normalizeArabic(query)
                repository.fileDao.searchFilesText(normalized)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Temporary Screens Message Banner States ---
    var globalErrorMsg by mutableStateOf<String?>(null)
    var globalSuccessMsg by mutableStateOf<String?>(null)
    var isBackupInProgress by mutableStateOf(false)
    var isAssistantLoading by mutableStateOf(false)
    var lastBackupAtMillis by mutableStateOf<Long?>(null)
    var lastBackupSizeBytes by mutableStateOf<Long?>(null)
    var licenseServerUrlInput by mutableStateOf(BuildConfig.LICENSE_SERVER_URL)

    // --- License Form Inputs ---
    var usernameInput by mutableStateOf("")
    var LicenseCodeInput by mutableStateOf("")

    // Initialize checking login cache status
    init {
        viewModelScope.launch {
            lastBackupAtMillis = prefs.getLong("last_backup_at", 0L).takeIf { it > 0L }
            lastBackupSizeBytes = prefs.getLong("last_backup_size", 0L).takeIf { it > 0L }
            licenseServerUrlInput = prefs.getString("license_server_url", BuildConfig.LICENSE_SERVER_URL) ?: BuildConfig.LICENSE_SERVER_URL
            // Check first-run templates seeding
            repository.seedTemplatesIfEmpty()
            
            // Wait for Splash reveal briefly
            kotlinx.coroutines.delay(1800)
            
            val status = repository.checkLicenseStatus()
            if (status == "نشط") {
                navigateTo(Screen.Dashboard)
                repository.checkLicenseOnlineIfDue().onFailure {
                    globalErrorMsg = it.message
                    navigateTo(Screen.Activation)
                }
            } else {
                navigateTo(Screen.Activation)
            }
        }
    }

    // --- Operations on License / Registration ---
    fun submitLicenseActivation() {
        viewModelScope.launch {
            globalErrorMsg = null
            globalSuccessMsg = null
            repository.activateLicense(usernameInput, LicenseCodeInput)
                .onSuccess {
                    globalSuccessMsg = "تم تفعيل الترخيص بنجاح وارتباطه بجهازك!"
                    navigateTo(Screen.Dashboard)
                }
                .onFailure {
                    globalErrorMsg = it.message ?: "خطأ تفعيل ترخيص التطبيق غامض."
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.licenseDao.deleteLicense()
            repository.clearLicenseSessionCache()
            navigateTo(Screen.Activation)
        }
    }

    fun saveLicenseServerUrl(url: String) {
        val normalized = url.trim().trimEnd('/')
        if (normalized.isBlank()) {
            globalErrorMsg = "يرجى إدخال رابط سيرفر صحيح."
            return
        }
        prefs.edit().putString("license_server_url", normalized).apply()
        licenseServerUrlInput = normalized
        globalSuccessMsg = "تم حفظ رابط سيرفر التراخيص."
    }

    fun saveLawOfficeProfile(lawyerName: String, officeName: String, phone: String, barNumber: String) {
        viewModelScope.launch {
            val current = repository.licenseDao.getLicenseDirect()
            if (current == null) {
                globalErrorMsg = "لا يمكن تحديث الملف المهني قبل وجود ترخيص محلي مفعل."
                return@launch
            }
            repository.licenseDao.insertLicense(
                current.copy(
                    lawyerName = lawyerName.trim().ifEmpty { current.lawyerName },
                    officeName = officeName.trim().ifEmpty { current.officeName },
                    phone = phone.trim(),
                    barNumber = barNumber.trim()
                )
            )
            globalSuccessMsg = "تم حفظ بيانات المحامي والمكتب محلياً بنجاح."
        }
    }

    // --- Clients Operations ---
    fun saveClient(client: Client, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.clientDao.insertClient(client)
            onDone()
        }
    }

    fun deleteClient(client: Client, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.clientDao.deleteClient(client)
            onDone()
        }
    }

    // --- Cases Operations ---
    fun saveCase(legalCase: LegalCase, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.caseDao.insertCase(legalCase)
            onDone()
        }
    }

    fun deleteCase(legalCase: LegalCase, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.caseDao.deleteCase(legalCase)
            onDone()
        }
    }

    // --- Sessions Operations ---
    fun saveSession(session: CaseSession, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.sessionDao.insertSession(session)
            // Update last or next session dates on the case if required
            updateCaseDatesFromSessions(session.caseId)
            onDone()
        }
    }

    fun deleteSession(session: CaseSession, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.sessionDao.deleteSession(session)
            updateCaseDatesFromSessions(session.caseId)
            onDone()
        }
    }

    private fun updateCaseDatesFromSessions(caseId: Int) {
        viewModelScope.launch {
            val sessionsFlow = repository.sessionDao.getSessionsForCase(caseId).firstOrNull() ?: emptyList()
            val incoming = sessionsFlow.filter { it.status == "قادمة" }.sortedBy { it.date }
            val past = sessionsFlow.filter { it.status == "منتهية" }.sortedByDescending { it.date }
            
            val targetCase = repository.caseDao.getCaseById(caseId)
            if (targetCase != null) {
                var nextDate = targetCase.nextSessionDate
                var lastDate = targetCase.lastSessionDate
                if (incoming.isNotEmpty()) {
                    nextDate = incoming.first().date
                }
                if (past.isNotEmpty()) {
                    lastDate = past.first().date
                }
                repository.caseDao.updateCase(targetCase.copy(nextSessionDate = nextDate, lastSessionDate = lastDate))
            }
        }
    }

    // --- Tasks Operations ---
    fun saveTask(task: LegalTask, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.taskDao.insertTask(task)
            onDone()
        }
    }

    fun toggleTaskCompleted(task: LegalTask) {
        viewModelScope.launch {
            val newStatus = if (task.status == "منتهية") "مفتوحة" else "منتهية"
            repository.taskDao.updateTask(task.copy(status = newStatus))
        }
    }

    fun deleteTask(task: LegalTask, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.taskDao.deleteTask(task)
            onDone()
        }
    }

    // --- Case Files Import ---
    fun importCaseFile(caseId: Int, clientId: Int, fileName: String, fileUri: Uri, docType: String, docLength: Long) {
        viewModelScope.launch {
            try {
                val caseTitle = repository.caseDao.getCaseById(caseId)?.title ?: "قضية غير معروفة"
                val clientName = repository.clientDao.getClientById(clientId)?.name ?: "عميل غير معروف"
                
                // 1. Copy to private app storage
                val copiedFile = repository.saveFileToPrivateStorage(caseId, fileName, fileUri)
                val extension = if (fileName.contains('.')) fileName.substringAfterLast('.').lowercase() else "txt"
                
                // 2. Check and perform text reading / index
                val isTxt = extension == "txt"
                val extractedText = if (isTxt) {
                    repository.extractAndCleanText(copiedFile, extension)
                } else {
                    "" // Save the file but do not simulate text extraction for xlsx/pdf/jpg/png
                }
                
                val statusText = if (isTxt) "ناجحة" else "غير مدعوم للبحث في هذه النسخة"
                
                // Construct normalized search index matching all search criteria:
                // file name, doc type, custom manually entered/extracted text, case title, client name
                val searchIndexContent = "$fileName $docType $caseTitle $clientName $extractedText"
                val normalizedIndex = repository.normalizeArabic(searchIndexContent)
                
                // 3. Save inside Database
                val caseFile = CaseFile(
                    caseId = caseId,
                    caseTitle = caseTitle,
                    clientId = clientId,
                    clientName = clientName,
                    fileName = fileName,
                    filePath = copiedFile.absolutePath,
                    docType = docType,
                    fileLength = docLength,
                    extractedText = extractedText,
                    extractionStatus = statusText,
                    normalizedSearchIndex = normalizedIndex
                )
                repository.fileDao.insertFile(caseFile)
                globalSuccessMsg = "تم حفظ المستند محلياً وربطه بالقضية بنجاح."
            } catch (e: Exception) {
                globalErrorMsg = "فشل في حفظ المستند: ${e.message}"
            }
        }
    }

    fun updateFileManualText(fileObj: CaseFile, newText: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val searchIndexContent = "${fileObj.fileName} ${fileObj.docType} ${fileObj.caseTitle} ${fileObj.clientName} $newText"
                val normalizedIndex = repository.normalizeArabic(searchIndexContent)
                val updatedFile = fileObj.copy(
                    extractedText = newText,
                    normalizedSearchIndex = normalizedIndex
                )
                repository.fileDao.insertFile(updatedFile)
                globalSuccessMsg = "تم حفظ النص وتحديث الفهرسة بالملف بنجاح."
                onDone()
            } catch (e: Exception) {
                globalErrorMsg = "فشل في تحديث النص: ${e.message}"
            }
        }
    }

    fun deleteFile(fileObj: CaseFile) {
        viewModelScope.launch {
            try {
                val file = File(fileObj.filePath)
                if (file.exists()) {
                    file.delete()
                }
                repository.fileDao.deleteFile(fileObj)
                globalSuccessMsg = "تم حذف المستند المحفوظ بنجاح."
            } catch (e: Exception) {
                globalErrorMsg = "خطأ في حذف المستند: ${e.message}"
            }
        }
    }

    // --- Generated Documents ---
    fun saveGeneratedDocument(caseId: Int, templateId: Int, title: String, filledFields: String, docContent: String) {
        viewModelScope.launch {
            val generated = GeneratedDocument(
                caseId = caseId,
                templateId = templateId,
                documentTitle = title,
                filledFieldsJson = filledFields,
                content = docContent
            )
            repository.generatedDocDao.insertGeneratedDocument(generated)
            globalSuccessMsg = "تم حفظ نموذج المستند ضمن القضايا الملحقة لملف القضية بنجاح!"
        }
    }

    fun deleteGeneratedDocument(doc: GeneratedDocument) {
        viewModelScope.launch {
            repository.generatedDocDao.deleteGeneratedDocument(doc)
            globalSuccessMsg = "تم حذف المستند المولد بنجاح."
        }
    }

    // --- Backup & Restore Actions ---
    fun executeDatabaseBackup(onShareFile: (File) -> Unit) {
        viewModelScope.launch {
            isBackupInProgress = true
            val backup = repository.backupDatabaseFile()
            if (backup != null) {
                val now = System.currentTimeMillis()
                prefs.edit().putLong("last_backup_at", now).apply()
                prefs.edit().putLong("last_backup_size", backup.length()).apply()
                lastBackupAtMillis = now
                lastBackupSizeBytes = backup.length()
                onShareFile(backup)
                globalSuccessMsg = "تم إنشاء نسخة احتياطية شاملة (قاعدة البيانات + المرفقات) باسم: ${backup.name}."
            } else {
                globalErrorMsg = "فشل إنشاء نسخة الاحتياطية. الرجاء المحاولة مجدداً."
            }
            isBackupInProgress = false
        }
    }

    fun executeDatabaseRestore(backupUri: Uri, onRestart: () -> Unit) {
        viewModelScope.launch {
            isBackupInProgress = true
            val result = repository.restoreDatabaseFile(backupUri)
            if (result) {
                globalSuccessMsg = "تم استعادة كافة البيانات والملفات السابقة بنجاح! يتم الآن إعادة تحميل التطبيق."
                onRestart()
            } else {
                globalErrorMsg = "فشل استيراد النسخة الاحتياطية. تأكد من سلامة الملف وصيغة .mpb"
            }
            isBackupInProgress = false
        }
    }

    private fun normalizedHeader(raw: String): String {
        return repository.normalizeArabic(raw).replace("_", " ").trim()
    }

    private fun targetFieldAliases(target: ImportTarget): Map<String, List<String>> {
        return when (target) {
            ImportTarget.CLIENTS -> mapOf(
                "name" to listOf("الاسم", "اسم العميل", "العميل", "client", "client name", "name"),
                "phone" to listOf("الهاتف", "الموبايل", "رقم الهاتف", "تليفون", "phone", "mobile"),
                "email" to listOf("email", "البريد", "البريد الالكتروني"),
                "national_id" to listOf("الرقم القومي", "بطاقة", "national id"),
                "address" to listOf("العنوان", "address"),
                "notes" to listOf("ملاحظات", "notes"),
                "status" to listOf("status", "الحالة")
            )
            ImportTarget.CASES -> mapOf(
                "title" to listOf("القضية", "عنوان القضية", "موضوع القضية", "title"),
                "case_number" to listOf("رقم القضية", "رقم الدعوى", "case number"),
                "case_year" to listOf("سنة القضية", "case year", "السنة"),
                "client_name" to listOf("العميل", "اسم العميل", "الموكل", "client"),
                "opponent_name" to listOf("الخصم", "opponent"),
                "court_name" to listOf("المحكمة", "court"),
                "court_circle" to listOf("الدائرة", "circle"),
                "case_type" to listOf("نوع القضية", "النوع", "type"),
                "status" to listOf("الحالة", "status"),
                "priority" to listOf("الأولوية", "priority"),
                "start_date" to listOf("تاريخ البدء", "start date"),
                "next_session_date" to listOf("الجلسة القادمة", "next session"),
                "notes" to listOf("ملاحظات", "notes")
            )
            ImportTarget.SESSIONS -> mapOf(
                "case_number" to listOf("رقم القضية", "case number"),
                "case_title" to listOf("عنوان القضية", "القضية", "case title"),
                "client_name" to listOf("اسم العميل", "العميل", "client"),
                "session_date" to listOf("تاريخ الجلسة", "date", "session date"),
                "session_time" to listOf("وقت الجلسة", "time", "session time"),
                "court_name" to listOf("المحكمة", "court"),
                "court_circle" to listOf("الدائرة", "circle"),
                "requirements" to listOf("المطلوب", "requirements"),
                "result" to listOf("النتيجة", "result"),
                "notes" to listOf("ملاحظات", "notes")
            )
        }
    }

    fun parseImportFile(
        uri: Uri,
        target: ImportTarget,
        onLoaded: (ImportedTable, Map<String, String>) -> Unit
    ) {
        viewModelScope.launch {
            repository.loadImportTable(uri)
                .onSuccess { table ->
                    val aliases = targetFieldAliases(target)
                    val autoMapping = table.headers.associateWith { header ->
                        val normalized = normalizedHeader(header)
                        aliases.entries.firstOrNull { (_, names) ->
                            names.any { repository.normalizeArabic(it) == normalized }
                        }?.key ?: "__ignore__"
                    }
                    onLoaded(table, autoMapping)
                }
                .onFailure {
                    globalErrorMsg = it.message ?: "تعذر قراءة ملف الاستيراد."
                }
        }
    }

    private fun rowAsFieldMap(headers: List<String>, row: List<String>, mapping: Map<String, String>): Map<String, String> {
        val values = mutableMapOf<String, String>()
        headers.forEachIndexed { idx, header ->
            val field = mapping[header] ?: "__ignore__"
            if (field != "__ignore__") {
                values[field] = row.getOrElse(idx) { "" }.trim()
            }
        }
        return values
    }

    private fun ensureDateFormat(dateText: String): String {
        val raw = dateText.trim()
        if (raw.isBlank()) return ""
        val slash = raw.split("/")
        if (slash.size == 3) {
            val day = slash[0].padStart(2, '0')
            val month = slash[1].padStart(2, '0')
            val year = slash[2]
            return "$year-$month-$day"
        }
        val dash = raw.split("-")
        if (dash.size == 3 && dash[0].length == 4) return raw
        return raw
    }

    fun buildImportPreview(
        target: ImportTarget,
        headers: List<String>,
        rows: List<List<String>>,
        mapping: Map<String, String>,
        duplicateStrategy: DuplicateStrategy,
        autoCreateClientForCases: Boolean,
        onResult: (ImportPreview) -> Unit
    ) {
        viewModelScope.launch {
            val existingClients = repository.clientDao.getAllClients().firstOrNull().orEmpty()
            val existingCases = repository.caseDao.getAllActiveCases().firstOrNull().orEmpty()
            val existingClientsByPhone = existingClients
                .filter { it.phone.isNotBlank() }
                .associateBy { repository.normalizeArabic(it.phone) }

            val previewRows = mutableListOf<ImportRowPreview>()
            var duplicates = 0
            var warnings = 0

            rows.forEachIndexed { index, row ->
                val fieldValues = rowAsFieldMap(headers, row, mapping)
                val rowNo = index + 2
                when (target) {
                    ImportTarget.CLIENTS -> {
                        val name = fieldValues["name"].orEmpty().trim()
                        if (name.isBlank()) {
                            previewRows += ImportRowPreview(rowNo, "INVALID", "الاسم مطلوب", fieldValues)
                            return@forEachIndexed
                        }
                        val phone = fieldValues["phone"].orEmpty().trim()
                        val duplicateClient = if (phone.isBlank()) null else existingClientsByPhone[repository.normalizeArabic(phone)]
                        if (duplicateClient != null) {
                            duplicates++
                            when (duplicateStrategy) {
                                DuplicateStrategy.SKIP -> previewRows += ImportRowPreview(rowNo, "DUPLICATE", "رقم الهاتف مكرر - سيتم تخطيه", fieldValues)
                                DuplicateStrategy.UPDATE -> previewRows += ImportRowPreview(rowNo, "VALID", "رقم مكرر - سيتم تحديث العميل الحالي", fieldValues)
                                DuplicateStrategy.CREATE_NEW -> previewRows += ImportRowPreview(rowNo, "VALID", "رقم مكرر - سيتم إنشاء عميل جديد", fieldValues)
                            }
                        } else {
                            previewRows += ImportRowPreview(rowNo, "VALID", "جاهز للاستيراد", fieldValues)
                        }
                    }
                    ImportTarget.CASES -> {
                        val title = fieldValues["title"].orEmpty().trim()
                        if (title.isBlank()) {
                            previewRows += ImportRowPreview(rowNo, "INVALID", "عنوان القضية مطلوب", fieldValues)
                            return@forEachIndexed
                        }
                        val clientName = fieldValues["client_name"].orEmpty().trim()
                        val clientFound = existingClients.firstOrNull {
                            repository.normalizeArabic(it.name) == repository.normalizeArabic(clientName)
                        }
                        if (clientName.isBlank()) {
                            previewRows += ImportRowPreview(rowNo, "INVALID", "اسم العميل مطلوب لربط القضية", fieldValues)
                            return@forEachIndexed
                        }
                        if (clientFound == null && !autoCreateClientForCases) {
                            warnings++
                            previewRows += ImportRowPreview(rowNo, "INVALID", "العميل غير موجود (فعّل خيار الإنشاء التلقائي)", fieldValues)
                            return@forEachIndexed
                        }
                        previewRows += ImportRowPreview(rowNo, "VALID", "جاهز للاستيراد", fieldValues)
                    }
                    ImportTarget.SESSIONS -> {
                        val sessionDate = ensureDateFormat(fieldValues["session_date"].orEmpty())
                        if (sessionDate.isBlank()) {
                            previewRows += ImportRowPreview(rowNo, "INVALID", "تاريخ الجلسة مطلوب", fieldValues)
                            return@forEachIndexed
                        }
                        val byCaseNumber = fieldValues["case_number"].orEmpty().trim()
                        val byCaseTitle = fieldValues["case_title"].orEmpty().trim()
                        val targetCase = existingCases.firstOrNull {
                            (byCaseNumber.isNotBlank() && repository.normalizeArabic(it.caseNumber) == repository.normalizeArabic(byCaseNumber)) ||
                                (byCaseTitle.isNotBlank() && repository.normalizeArabic(it.title) == repository.normalizeArabic(byCaseTitle))
                        }
                        if (targetCase == null) {
                            warnings++
                            previewRows += ImportRowPreview(rowNo, "INVALID", "لم يتم العثور على القضية المرتبطة", fieldValues + ("session_date" to sessionDate))
                            return@forEachIndexed
                        }
                        previewRows += ImportRowPreview(rowNo, "VALID", "جاهز للاستيراد", fieldValues + ("session_date" to sessionDate))
                    }
                }
            }

            val valid = previewRows.count { it.status == "VALID" }
            val invalid = previewRows.count { it.status != "VALID" }
            onResult(
                ImportPreview(
                    target = target,
                    totalRows = rows.size,
                    validRows = valid,
                    invalidRows = invalid,
                    duplicates = duplicates,
                    warnings = warnings,
                    rows = previewRows
                )
            )
        }
    }

    fun importFromPreview(
        preview: ImportPreview,
        duplicateStrategy: DuplicateStrategy,
        autoCreateClientForCases: Boolean,
        onDone: (Int) -> Unit
    ) {
        viewModelScope.launch {
            var importedCount = 0
            val existingClients = repository.clientDao.getAllClients().firstOrNull().orEmpty().toMutableList()
            val existingCases = repository.caseDao.getAllActiveCases().firstOrNull().orEmpty().toMutableList()

            for (row in preview.rows) {
                if (row.status != "VALID") continue
                when (preview.target) {
                    ImportTarget.CLIENTS -> {
                        val name = row.values["name"].orEmpty().trim()
                        val phone = row.values["phone"].orEmpty().trim()
                        if (name.isBlank()) continue
                        val existing = if (phone.isBlank()) null else existingClients.firstOrNull {
                            repository.normalizeArabic(it.phone) == repository.normalizeArabic(phone)
                        }
                        if (existing != null && duplicateStrategy == DuplicateStrategy.SKIP) {
                            continue
                        }
                        if (existing != null && duplicateStrategy == DuplicateStrategy.UPDATE) {
                            val updated = existing.copy(
                                name = name,
                                phone = phone,
                                email = row.values["email"].orEmpty(),
                                nationalId = row.values["national_id"].orEmpty(),
                                address = row.values["address"].orEmpty(),
                                notes = row.values["notes"].orEmpty(),
                                status = row.values["status"].orEmpty().ifBlank { existing.status }
                            )
                            repository.clientDao.updateClient(updated)
                            importedCount++
                        } else {
                            val newClient = Client(
                                name = name,
                                phone = phone,
                                email = row.values["email"].orEmpty(),
                                nationalId = row.values["national_id"].orEmpty(),
                                address = row.values["address"].orEmpty(),
                                notes = row.values["notes"].orEmpty(),
                                status = row.values["status"].orEmpty().ifBlank { "نشط" }
                            )
                            val id = repository.clientDao.insertClient(newClient).toInt()
                            existingClients += newClient.copy(id = id)
                            importedCount++
                        }
                    }
                    ImportTarget.CASES -> {
                        val title = row.values["title"].orEmpty().trim()
                        val clientName = row.values["client_name"].orEmpty().trim()
                        if (title.isBlank() || clientName.isBlank()) continue

                        var client = existingClients.firstOrNull {
                            repository.normalizeArabic(it.name) == repository.normalizeArabic(clientName)
                        }
                        if (client == null && autoCreateClientForCases) {
                            val insertedId = repository.clientDao.insertClient(
                                Client(name = clientName, phone = "", notes = "تم إنشاؤه تلقائياً عبر الاستيراد")
                            ).toInt()
                            client = Client(id = insertedId, name = clientName, phone = "")
                            existingClients += client
                        }
                        if (client == null) continue

                        val legalCase = LegalCase(
                            title = title,
                            caseNumber = row.values["case_number"].orEmpty(),
                            caseYear = row.values["case_year"].orEmpty(),
                            caseType = row.values["case_type"].orEmpty().ifBlank { "عام" },
                            clientId = client.id,
                            clientName = client.name,
                            opponentName = row.values["opponent_name"].orEmpty(),
                            courtName = row.values["court_name"].orEmpty(),
                            courtCircle = row.values["court_circle"].orEmpty(),
                            startDate = ensureDateFormat(row.values["start_date"].orEmpty()),
                            nextSessionDate = ensureDateFormat(row.values["next_session_date"].orEmpty()),
                            status = row.values["status"].orEmpty().ifBlank { "جديدة" },
                            priority = row.values["priority"].orEmpty().ifBlank { "متوسطة" },
                            notes = row.values["notes"].orEmpty()
                        )
                        val insertedId = repository.caseDao.insertCase(legalCase).toInt()
                        existingCases += legalCase.copy(id = insertedId)
                        importedCount++
                    }
                    ImportTarget.SESSIONS -> {
                        val sessionDate = ensureDateFormat(row.values["session_date"].orEmpty())
                        if (sessionDate.isBlank()) continue
                        val caseNumber = row.values["case_number"].orEmpty()
                        val caseTitle = row.values["case_title"].orEmpty()
                        val linkedCase = existingCases.firstOrNull {
                            (caseNumber.isNotBlank() && repository.normalizeArabic(it.caseNumber) == repository.normalizeArabic(caseNumber)) ||
                                (caseTitle.isNotBlank() && repository.normalizeArabic(it.title) == repository.normalizeArabic(caseTitle))
                        } ?: continue

                        val session = CaseSession(
                            caseId = linkedCase.id,
                            caseTitle = linkedCase.title,
                            clientId = linkedCase.clientId,
                            clientName = linkedCase.clientName,
                            title = "جلسة ${linkedCase.title}",
                            court = row.values["court_name"].orEmpty(),
                            courtCircle = row.values["court_circle"].orEmpty(),
                            date = sessionDate,
                            time = row.values["session_time"].orEmpty(),
                            requirements = row.values["requirements"].orEmpty(),
                            result = row.values["result"].orEmpty(),
                            notes = row.values["notes"].orEmpty(),
                            status = "قادمة"
                        )
                        repository.sessionDao.insertSession(session)
                        importedCount++
                    }
                }
            }
            globalSuccessMsg = "تم استيراد $importedCount صف بنجاح."
            onDone(importedCount)
        }
    }

    // --- Offline Smart Legal Assistant Calculations ---
    private fun parseDate(dateText: String): Long? {
        return try {
            if (dateText.isBlank()) null else SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(dateText)?.time
        } catch (_: Exception) {
            null
        }
    }

    private fun parseSessionDateTime(session: CaseSession): Long? {
        val full = if (session.time.isBlank()) "${session.date} 23:59" else "${session.date} ${session.time}"
        val formats = listOf("yyyy-MM-dd HH:mm", "yyyy-MM-dd H:mm")
        formats.forEach { pattern ->
            try {
                return SimpleDateFormat(pattern, Locale.ENGLISH).parse(full)?.time
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun isTaskOpen(task: LegalTask): Boolean = task.status != "منتهية"

    private fun isTaskOverdue(task: LegalTask, now: Long): Boolean {
        if (!isTaskOpen(task)) return false
        val due = parseDate(task.dueDate) ?: return false
        return due < now
    }

    private fun extractLastCaseNote(notes: String): String {
        if (notes.isBlank()) return "لا توجد"
        return notes.split("\n\n").last().trim().takeIf { it.isNotBlank() } ?: "لا توجد"
    }

    private fun requiredDocMatched(requiredItem: String, file: CaseFile): Boolean {
        val need = repository.normalizeArabic(requiredItem)
        val fileIndex = repository.normalizeArabic("${file.fileName} ${file.docType} ${file.extractedText} ${file.normalizedSearchIndex}")
        if (fileIndex.contains(need)) return true
        val parts = need.split(" ").filter { it.length >= 3 }
        return parts.isNotEmpty() && parts.all { fileIndex.contains(it) }
    }

    private fun extractSnippet(rawText: String, normalizedQuery: String): String {
        if (rawText.isBlank()) return ""
        val tokens = rawText.split(Regex("\\s+"))
        val hit = tokens.indexOfFirst { repository.normalizeArabic(it).contains(normalizedQuery) }
        if (hit == -1) return rawText.take(80)
        val start = (hit - 6).coerceAtLeast(0)
        val end = (hit + 8).coerceAtMost(tokens.size)
        return tokens.subList(start, end).joinToString(" ")
    }

    fun getSmartAssistantSummary(caseId: Int, onResult: (String) -> Unit) {
        viewModelScope.launch {
            isAssistantLoading = true
            val caseObj = repository.caseDao.getCaseById(caseId)
            if (caseObj == null) {
                isAssistantLoading = false
                onResult("تعذر تحميل القضية.")
                return@launch
            }
            val clientObj = repository.clientDao.getClientById(caseObj.clientId)
            val sessionsList = repository.sessionDao.getSessionsForCase(caseId).firstOrNull().orEmpty()
            val tasksList = repository.taskDao.getTasksForCase(caseId).firstOrNull().orEmpty()
            val filesList = repository.fileDao.getFilesForCase(caseId).firstOrNull().orEmpty()
            val openTasks = tasksList.filter(::isTaskOpen)

            val sortedByDate = sessionsList.sortedBy { parseSessionDateTime(it) ?: Long.MAX_VALUE }
            val upcoming = sortedByDate.firstOrNull { (parseSessionDateTime(it) ?: Long.MAX_VALUE) >= System.currentTimeMillis() }
            val lastSession = sessionsList
                .filter { (parseSessionDateTime(it) ?: Long.MIN_VALUE) < System.currentTimeMillis() }
                .maxByOrNull { parseSessionDateTime(it) ?: Long.MIN_VALUE }

            val text = buildString {
                appendLine("ملخص القضية من البيانات المحلية:")
                appendLine("• عنوان القضية: ${caseObj.title}")
                appendLine("• رقم القضية: ${caseObj.caseNumber} / ${caseObj.caseYear}")
                appendLine("• العميل: ${clientObj?.name ?: caseObj.clientName}")
                appendLine("• الخصم: ${caseObj.opponentName.ifBlank { "غير محدد" }}")
                appendLine("• المحكمة: ${caseObj.courtName.ifBlank { "غير محددة" }}")
                appendLine("• الدائرة: ${caseObj.courtCircle.ifBlank { "غير محددة" }}")
                appendLine("• نوع القضية: ${caseObj.caseType}")
                appendLine("• الحالة: ${caseObj.status}")
                appendLine("• الأولوية: ${caseObj.priority}")
                appendLine("• آخر جلسة: ${lastSession?.date ?: caseObj.lastSessionDate.ifBlank { "لا توجد" }}")
                appendLine("• الجلسة القادمة: ${upcoming?.date ?: caseObj.nextSessionDate.ifBlank { "لا توجد" }}")
                appendLine("• عدد الملفات: ${filesList.size}")
                appendLine("• عدد المهام المفتوحة: ${openTasks.size}")
                appendLine("• آخر ملاحظة: ${extractLastCaseNote(caseObj.notes)}")
            }
            isAssistantLoading = false
            onResult(text)
        }
    }

    fun getMissingDocumentsSuggestion(caseId: Int, onResult: (String) -> Unit) {
        viewModelScope.launch {
            isAssistantLoading = true
            val caseObj = repository.caseDao.getCaseById(caseId)
            if (caseObj == null) {
                isAssistantLoading = false
                onResult("تعذر تحميل القضية.")
                return@launch
            }
            val files = repository.fileDao.getFilesForCase(caseId).firstOrNull().orEmpty()
            val rules = CaseRulesEngine.getRules(caseObj.caseType, repository::normalizeArabic)
            val existing = mutableListOf<String>()
            val missing = mutableListOf<String>()
            rules.requiredDocuments.forEach { required ->
                val found = files.any { requiredDocMatched(required, it) }
                if (found) existing.add(required) else missing.add(required)
            }

            val text = buildString {
                appendLine("مطابقة المستندات المطلوبة لنوع قضية: ${rules.key}")
                appendLine("الموجودة (${existing.size}):")
                if (existing.isEmpty()) appendLine("• لا يوجد")
                existing.forEach { appendLine("• $it") }
                appendLine("الناقصة (${missing.size}):")
                if (missing.isEmpty()) appendLine("• لا يوجد")
                missing.forEach { appendLine("• $it") }
                appendLine("الملفات الحالية:")
                if (files.isEmpty()) appendLine("• لا توجد ملفات مرفوعة")
                files.forEach { appendLine("• ${it.fileName} (${it.docType})") }
            }
            isAssistantLoading = false
            onResult(text)
        }
    }

    fun getNextSessionAssistant(caseId: Int, onResult: (String) -> Unit) {
        viewModelScope.launch {
            isAssistantLoading = true
            val sessions = repository.sessionDao.getSessionsForCase(caseId).firstOrNull().orEmpty()
            val now = System.currentTimeMillis()
            val next = sessions
                .filter { (parseSessionDateTime(it) ?: Long.MIN_VALUE) >= now }
                .minByOrNull { parseSessionDateTime(it) ?: Long.MAX_VALUE }

            val text = if (next == null) {
                "لا توجد جلسات قادمة. استخدم زر \"إضافة جلسة\" في تبويب الجلسات."
            } else {
                buildString {
                    appendLine("أقرب جلسة قادمة:")
                    appendLine("• التاريخ: ${next.date}")
                    appendLine("• الساعة: ${next.time.ifBlank { "غير محددة" }}")
                    appendLine("• المحكمة: ${next.court.ifBlank { "غير محددة" }}")
                    appendLine("• الدائرة: ${next.courtCircle.ifBlank { "غير محددة" }}")
                    appendLine("• المطلوب: ${next.requirements.ifBlank { "لا يوجد" }}")
                }
            }
            isAssistantLoading = false
            onResult(text)
        }
    }

    fun getOpenTasksAssistant(caseId: Int, onResult: (String) -> Unit) {
        viewModelScope.launch {
            isAssistantLoading = true
            val now = System.currentTimeMillis()
            val tasks = repository.taskDao.getTasksForCase(caseId).firstOrNull().orEmpty().filter(::isTaskOpen)
            val text = if (tasks.isEmpty()) {
                "لا توجد مهام مفتوحة لهذه القضية."
            } else {
                buildString {
                    appendLine("المهام المفتوحة (${tasks.size}):")
                    tasks.forEachIndexed { index, task ->
                        val overdue = if (isTaskOverdue(task, now)) " [متأخرة]" else ""
                        appendLine("${index + 1}. ${task.title}$overdue")
                        if (task.dueDate.isNotBlank()) appendLine("   استحقاق: ${task.dueDate}")
                    }
                    appendLine("يمكنك فتح تبويب المهام وتعليم أي مهمة كمكتملة مباشرة.")
                }
            }
            isAssistantLoading = false
            onResult(text)
        }
    }

    fun getSuggestedTemplatesForCase(caseType: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            isAssistantLoading = true
            val templates = repository.templateDao.getAllTemplates().firstOrNull().orEmpty()
            val rules = CaseRulesEngine.getRules(caseType, repository::normalizeArabic)
            val suggested = templates.filter { template ->
                rules.suggestedTemplates.any {
                    repository.normalizeArabic(template.title).contains(repository.normalizeArabic(it))
                } || repository.normalizeArabic(template.caseType) == repository.normalizeArabic(rules.key)
            }
            val text = if (suggested.isEmpty()) {
                "لا توجد قوالب مطابقة حالياً."
            } else {
                buildString {
                    appendLine("القوالب المناسبة لنوع ${rules.key}:")
                    suggested.distinctBy { it.id }.forEachIndexed { index, template ->
                        appendLine("${index + 1}. ${template.title}")
                    }
                    appendLine("يمكنك فتح أي قالب وتوليد مستند ثم حفظه داخل القضية.")
                }
            }
            isAssistantLoading = false
            onResult(text)
        }
    }

    fun searchInsideCaseFiles(caseId: Int, query: String, onResult: (String, Int?) -> Unit) {
        viewModelScope.launch {
            isAssistantLoading = true
            val normalizedQuery = repository.normalizeArabic(query)
            if (normalizedQuery.isBlank()) {
                isAssistantLoading = false
                onResult("يرجى كتابة عبارة بحث داخل ملفات القضية.", null)
                return@launch
            }
            val files = repository.fileDao.getFilesForCase(caseId).firstOrNull().orEmpty()
            val matches = files.mapNotNull { file ->
                val combined = "${file.fileName} ${file.docType} ${file.extractedText} ${file.normalizedSearchIndex}"
                val normalized = repository.normalizeArabic(combined)
                if (!normalized.contains(normalizedQuery)) return@mapNotNull null
                val snippet = extractSnippet(file.extractedText, normalizedQuery)
                Pair(file, snippet)
            }
            val text = if (matches.isEmpty()) {
                "لا توجد نتائج مطابقة داخل ملفات القضية."
            } else {
                buildString {
                    appendLine("نتائج البحث داخل ملفات القضية (${matches.size}):")
                    matches.forEach { (file, snippet) ->
                        appendLine("• ${file.fileName} | ${file.docType}")
                        if (snippet.isNotBlank()) appendLine("  مطابق: $snippet")
                    }
                    appendLine("يمكنك فتح الملف من نتيجة البحث أو حفظ الناتج كملاحظة.")
                }
            }
            isAssistantLoading = false
            onResult(text, matches.firstOrNull()?.first?.id)
        }
    }

    fun saveAssistantResultAsCaseNote(caseId: Int, assistantOutput: String) {
        viewModelScope.launch {
            val legalCase = repository.caseDao.getCaseById(caseId) ?: return@launch
            val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH).format(System.currentTimeMillis())
            val merged = buildString {
                append(legalCase.notes)
                if (legalCase.notes.isNotBlank()) append("\n\n")
                append("[مخرجات المساعد المحلي - $stamp]\n")
                append(assistantOutput)
            }
            repository.caseDao.updateCase(legalCase.copy(notes = merged))
            globalSuccessMsg = "تم حفظ نتيجة المساعد داخل ملاحظات القضية."
        }
    }

    fun triggerSmartChecklist(caseId: Int, caseType: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            isAssistantLoading = true
            val caseObj = repository.caseDao.getCaseById(caseId)
            if (caseObj == null) {
                isAssistantLoading = false
                onResult("تعذر تحميل القضية.")
                return@launch
            }
            val rules = CaseRulesEngine.getRules(caseType, repository::normalizeArabic)
            val checklistText = buildString {
                appendLine("Checklist عملي - ${rules.key}")
                rules.checklist.forEach { appendLine("• $it") }
                appendLine("أسئلة مهمة:")
                rules.importantQuestions.forEach { appendLine("• $it") }
            }
            isAssistantLoading = false
            onResult(checklistText)
        }
    }
}
