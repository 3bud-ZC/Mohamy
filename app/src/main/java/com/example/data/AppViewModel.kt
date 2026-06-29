package com.example.data

import android.app.Application
import android.net.Uri
import android.content.Intent
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
    object FilesLibrary : Screen()
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
    private val navigationBackStack = mutableListOf<Screen>()

    fun navigateTo(screen: Screen, addToBackStack: Boolean = true) {
        if (currentScreen == screen) return
        if (addToBackStack) {
            navigationBackStack.add(currentScreen)
        } else {
            navigationBackStack.clear()
        }
        currentScreen = screen
    }

    fun canGoBack(): Boolean = navigationBackStack.isNotEmpty()

    fun goBack(): Boolean {
        if (navigationBackStack.isEmpty()) return false
        currentScreen = navigationBackStack.removeAt(navigationBackStack.lastIndex)
        return true
    }

    // --- Active Selection State ID Storage ---
    var activeClient: Client? by mutableStateOf(null)
    var activeCase: LegalCase? by mutableStateOf(null)

    // --- State Flows from Room ---
    val allClients = repository.clientDao.getAllClients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), emptyList())

    val allCases = repository.caseDao.getAllActiveCases()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), emptyList())

    val archivedCases = repository.caseDao.getAllArchivedCases()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), emptyList())

    val allSessions = repository.sessionDao.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), emptyList())

    val allTasks = repository.taskDao.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), emptyList())

    val allFiles = repository.fileDao.getAllFiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), emptyList())

    val allClientInteractions = repository.clientInteractionDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), emptyList())

    val allTemplates = repository.templateDao.getAllTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), emptyList())

    val allGeneratedDocuments = repository.generatedDocDao.getAllGeneratedDocuments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), emptyList())

    val allFeeRecords = repository.feeDao.getAllFeeRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), emptyList())

    val allCustomCaseCategories = repository.customCaseCategoryDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), emptyList())

    val licenseState = repository.licenseDao.getLicenseFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), null)

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
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), emptyList())

    // --- Temporary Screens Message Banner States ---
    var globalErrorMsg by mutableStateOf<String?>(null)
    var globalSuccessMsg by mutableStateOf<String?>(null)
    var globalInfoMsg by mutableStateOf<String?>(null)
    var isBackupInProgress by mutableStateOf(false)
    var isAssistantLoading by mutableStateOf(false)
    var isCloudAssistantEnabled by mutableStateOf(repository.isCloudAssistantEnabled())
    var isDarkThemeEnabled by mutableStateOf(false)
    var lastBackupAtMillis by mutableStateOf<Long?>(null)
    var lastBackupSizeBytes by mutableStateOf<Long?>(null)
    var licenseServerUrlInput by mutableStateOf(BuildConfig.LICENSE_SERVER_URL)
    var appReloadNonce by mutableStateOf(0)
        private set
    var updateCheckInProgress by mutableStateOf(false)
    var updateDownloadInProgress by mutableStateOf(false)
    var updateAvailable by mutableStateOf(false)
    var updateRemoteVersionCode by mutableStateOf<Int?>(null)
    var updateRemoteVersionName by mutableStateOf<String?>(null)
    var updateRemoteReleaseTitle by mutableStateOf<String?>(null)
    var updateRemoteReleaseNotes by mutableStateOf("")
    var updateRemoteDownloadUrl by mutableStateOf("")
    var updateRemoteReleasePageUrl by mutableStateOf("")
    var updateDownloadProgress by mutableStateOf(0)
    var downloadedUpdateFilePath by mutableStateOf<String?>(null)
    var updateStatusMessage by mutableStateOf<String?>(null)
    var isOnboardingCompleted by mutableStateOf(false)
        private set
    var hasDemoSeededForCurrentWorkspace by mutableStateOf(false)
        private set
    val hasConfiguredCloudAssistant: Boolean
        get() = repository.hasConfiguredCloudAssistant()

    // --- License Form Inputs ---
    var usernameInput by mutableStateOf("")
    var LicenseCodeInput by mutableStateOf("")

    // Initialize checking login cache status
    init {
        viewModelScope.launch {
            lastBackupAtMillis = prefs.getLong("last_backup_at", 0L).takeIf { it > 0L }
            lastBackupSizeBytes = prefs.getLong("last_backup_size", 0L).takeIf { it > 0L }
            licenseServerUrlInput = repository.migrateLicenseServerUrlIfNeeded()
            isCloudAssistantEnabled = repository.isCloudAssistantEnabled()
            isDarkThemeEnabled = prefs.getBoolean("dark_mode_enabled", false)
            repository.ensureActiveWorkspaceMarker()
            refreshWorkspacePresentationState()
            repository.consumePendingActivation()?.let {
                repository.persistActivatedLicense(it)
                refreshWorkspacePresentationState()
                globalSuccessMsg = "تم فتح مساحة الحساب المحلية بنجاح."
            }
            // Check first-run templates seeding
            repository.seedTemplatesIfEmpty()
            
            // Wait for Splash reveal briefly
            kotlinx.coroutines.delay(1800)
            
            val status = repository.checkLicenseStatus()
            if (status == "نشط") {
                navigateTo(Screen.Dashboard, addToBackStack = false)
                repository.checkLicenseOnlineIfDue().onFailure {
                    globalErrorMsg = it.message
                    navigateTo(Screen.Activation, addToBackStack = false)
                }
            } else {
                navigateTo(Screen.Activation, addToBackStack = false)
            }

            checkForAppUpdate(showUserFeedback = false)
        }
    }

    // --- Operations on License / Registration ---
    fun submitLicenseActivation() {
        viewModelScope.launch {
            globalErrorMsg = null
            globalSuccessMsg = null
            globalInfoMsg = null
            val targetUsername = usernameInput.trim()
            repository.activateLicense(usernameInput, LicenseCodeInput)
                .onSuccess { activation ->
                    val currentWorkspaceUsername = repository.currentWorkspaceUsername()
                    if (currentWorkspaceUsername.equals(targetUsername, ignoreCase = true)) {
                        repository.persistActivatedLicense(activation)
                        refreshWorkspacePresentationState()
                        globalSuccessMsg = "تم تفعيل الترخيص بنجاح وارتباطه بجهازك!"
                        navigateTo(Screen.Dashboard, addToBackStack = false)
                    } else {
                        repository.switchToAccountWorkspace(targetUsername)
                        repository.queuePendingActivation(activation)
                        activeClient = null
                        activeCase = null
                        searchEngineQuery = ""
                        usernameInput = ""
                        LicenseCodeInput = ""
                        requestAppReload()
                    }
                }
                .onFailure {
                    globalErrorMsg = it.message ?: "خطأ تفعيل ترخيص التطبيق غامض."
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.archiveActiveWorkspaceAndLogout()
            activeClient = null
            activeCase = null
            searchEngineQuery = ""
            usernameInput = ""
            LicenseCodeInput = ""
            isOnboardingCompleted = false
            hasDemoSeededForCurrentWorkspace = false
            globalSuccessMsg = "تم تسجيل الخروج مع حفظ مساحة هذا الحساب محلياً على الجهاز."
            globalInfoMsg = null
            navigateTo(Screen.Activation, addToBackStack = false)
        }
    }

    private fun refreshWorkspacePresentationState() {
        isOnboardingCompleted = repository.isCurrentWorkspaceOnboardingCompleted()
        hasDemoSeededForCurrentWorkspace = repository.hasSeededDemoWorkspace()
    }

    private fun requestAppReload() {
        appReloadNonce += 1
    }

    fun consumeAppReload() {
        appReloadNonce = 0
    }

    fun saveLicenseServerUrl(url: String) {
        val normalized = repository.normalizeLicenseServerUrl(url)
        if (normalized.isBlank()) {
            globalErrorMsg = "يرجى إدخال رابط سيرفر صحيح."
            return
        }
        repository.saveLicenseServerUrl(normalized)
        licenseServerUrlInput = normalized
        globalSuccessMsg = "تم حفظ رابط السيرفر. قم بالتفعيل للتحقق من الاتصال."
    }

    var isRemindersEnabled by mutableStateOf(prefs.getBoolean("reminders_enabled", true))
        private set

    fun updateRemindersEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("reminders_enabled", enabled).apply()
        isRemindersEnabled = enabled
    }


    fun updateDarkThemeEnabled(enabled: Boolean) {
        isDarkThemeEnabled = enabled
        prefs.edit().putBoolean("dark_mode_enabled", enabled).apply()
        globalInfoMsg = if (enabled) "تم تفعيل الوضع الليلي الاحترافي." else "تم تفعيل الوضع الفاتح."
    }

    fun completeOnboarding() {
        repository.setCurrentWorkspaceOnboardingCompleted(true)
        refreshWorkspacePresentationState()
        globalInfoMsg = "تم إخفاء بطاقة الترحيب لهذا الحساب المحلي."
    }

    fun reopenOnboarding() {
        repository.setCurrentWorkspaceOnboardingCompleted(false)
        refreshWorkspacePresentationState()
        navigateTo(Screen.Dashboard)
        globalInfoMsg = "تمت إعادة تفعيل بطاقة الترحيب لعرض خطوات البداية."
    }

    fun seedDemoWorkspace(forceAppend: Boolean = false) {
        viewModelScope.launch {
            repository.seedDemoWorkspaceData(forceAppend)
                .onSuccess { result ->
                    refreshWorkspacePresentationState()
                    globalSuccessMsg =
                        "تم إنشاء مساحة عرض محلية: ${result.clients} موكلين، ${result.cases} قضايا، ${result.sessions} جلسات، ${result.tasks} مهام، ${result.files} مستندات، ${result.fees} سجلات أتعاب."
                    navigateTo(Screen.Dashboard)
                }
                .onFailure {
                    globalErrorMsg = it.message ?: "تعذر إنشاء بيانات العرض التجريبية."
                }
        }
    }

    fun checkForAppUpdate(showUserFeedback: Boolean = true) {
        viewModelScope.launch {
            updateCheckInProgress = true
            updateStatusMessage = null
            val manager = AppUpdateManager(getApplication())
            manager.checkForUpdate().onSuccess { updateInfo ->
                updateCheckInProgress = false
                downloadedUpdateFilePath = null
                updateDownloadProgress = 0
                if (updateInfo == null) {
                    updateAvailable = false
                    updateRemoteVersionCode = null
                    updateRemoteVersionName = null
                    updateRemoteReleaseTitle = null
                    updateRemoteReleaseNotes = ""
                    updateRemoteDownloadUrl = ""
                    updateRemoteReleasePageUrl = ""
                    updateStatusMessage = "التطبيق محدث بالفعل."
                    if (showUserFeedback) {
                        globalInfoMsg = "التطبيق محدث بالفعل."
                    }
                    return@onSuccess
                }

                updateAvailable = true
                updateRemoteVersionCode = updateInfo.versionCode
                updateRemoteVersionName = updateInfo.versionName
                updateRemoteReleaseTitle = updateInfo.releaseTitle
                updateRemoteReleaseNotes = updateInfo.releaseNotes
                updateRemoteDownloadUrl = updateInfo.apkUrl
                updateRemoteReleasePageUrl = updateInfo.releasePageUrl
                updateStatusMessage = "توجد نسخة أحدث جاهزة للتنزيل."
                globalInfoMsg = "يوجد تحديث جديد للتطبيق."
                AppNotificationManager.notifyUpdateAvailable(
                    getApplication(),
                    updateInfo.displayVersionLabel,
                    updateInfo.releaseNotes,
                    updateInfo.releaseTitle
                )
            }.onFailure { error ->
                updateCheckInProgress = false
                updateAvailable = false
                updateRemoteVersionCode = null
                updateRemoteVersionName = null
                updateRemoteReleaseTitle = null
                updateRemoteReleaseNotes = ""
                updateRemoteDownloadUrl = ""
                updateRemoteReleasePageUrl = ""
                downloadedUpdateFilePath = null
                updateStatusMessage = null
                if (showUserFeedback) {
                    globalErrorMsg = error.message ?: "تعذر فحص التحديثات حالياً."
                }
            }
        }
    }

    fun downloadAndInstallAppUpdate() {
        viewModelScope.launch {
            val remoteVersionCode = updateRemoteVersionCode
            val remoteDownloadUrl = updateRemoteDownloadUrl
            if (!updateAvailable || remoteVersionCode == null || remoteDownloadUrl.isBlank()) {
                globalErrorMsg = "لا توجد نسخة جديدة جاهزة للتنزيل."
                return@launch
            }

            // Prefer opening the published APK URL (or release page) externally to avoid blocking UI.
            val ctx = getApplication<Application>()
            val primaryUri = runCatching { Uri.parse(remoteDownloadUrl) }.getOrNull()
            val fallbackUri = runCatching {
                Uri.parse(updateRemoteReleasePageUrl.ifBlank { BuildConfig.UPDATE_MANIFEST_URL })
            }.getOrNull()

            fun tryOpen(uri: Uri?): Boolean {
                if (uri == null) return false
                return runCatching {
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    ctx.startActivity(intent)
                    true
                }.getOrDefault(false)
            }

            val opened = tryOpen(primaryUri) || tryOpen(fallbackUri)
            if (opened) {
                updateStatusMessage = "جارٍ فتح رابط التحديث في المتصفح..."
                globalInfoMsg = "تم فتح رابط التحديث في المتصفح. إذا لم يبدأ التنزيل، استخدم صفحة الإصدار."
                return@launch
            }

            // Fallback to internal download if no external handler is available.
            updateDownloadInProgress = true
            updateDownloadProgress = 0
            updateStatusMessage = "جارٍ تنزيل ملف التحديث..."
            val manager = AppUpdateManager(getApplication())
            val updateInfo = AppUpdateInfo(
                versionCode = remoteVersionCode,
                versionName = updateRemoteVersionName.orEmpty(),
                apkUrl = remoteDownloadUrl,
                releaseNotes = updateRemoteReleaseNotes,
                releaseTitle = updateRemoteReleaseTitle.orEmpty(),
                sourceUrl = BuildConfig.UPDATE_MANIFEST_URL,
                releasePageUrl = updateRemoteReleasePageUrl
            )
            manager.downloadUpdate(updateInfo) { progress ->
                updateDownloadProgress = progress
            }.onSuccess { file ->
                updateDownloadInProgress = false
                downloadedUpdateFilePath = file.absolutePath
                updateStatusMessage = "تم تنزيل التحديث. ستظهر شاشة التثبيت الآن."
                globalInfoMsg = "تم تنزيل التحديث بنجاح."
                try {
                    manager.launchInstall(file)
                } catch (e: Exception) {
                    globalErrorMsg = e.message ?: "تم تنزيل التحديث، لكن تعذر فتح شاشة التثبيت."
                }
            }.onFailure { error ->
                updateDownloadInProgress = false
                updateStatusMessage = null
                globalErrorMsg = error.message ?: "فشل تنزيل التحديث."
            }
        }
    }

    fun installDownloadedUpdate() {
        val path = downloadedUpdateFilePath ?: run {
            globalErrorMsg = "لا يوجد ملف تحديث محمّل محلياً."
            return
        }
        val file = File(path)
        if (!file.exists()) {
            globalErrorMsg = "ملف التحديث غير موجود على الجهاز."
            downloadedUpdateFilePath = null
            return
        }
        try {
            AppUpdateManager(getApplication()).launchInstall(file)
            updateStatusMessage = "تم فتح شاشة التثبيت."
        } catch (e: Exception) {
            globalErrorMsg = e.message ?: "تعذر فتح ملف التحديث."
        }
    }

    fun clearUpdateState() {
        updateCheckInProgress = false
        updateDownloadInProgress = false
        updateAvailable = false
        updateRemoteVersionCode = null
        updateRemoteVersionName = null
        updateRemoteReleaseTitle = null
        updateRemoteReleaseNotes = ""
        updateRemoteDownloadUrl = ""
        updateRemoteReleasePageUrl = ""
        updateDownloadProgress = 0
        downloadedUpdateFilePath = null
        updateStatusMessage = null
    }

    fun updateCloudAssistantEnabled(enabled: Boolean) {
        repository.setCloudAssistantEnabled(enabled)
        isCloudAssistantEnabled = repository.isCloudAssistantEnabled()
        globalSuccessMsg = if (isCloudAssistantEnabled) {
            "تم تفعيل التحسين السحابي للمساعد الذكي."
        } else {
            "تم ضبط المساعد الذكي على الوضع المحلي فقط."
        }
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

    fun availableCaseCategories(): List<String> {
        val fixed = repository.fixedCaseCategories()
        val custom = allCustomCaseCategories.value.map { it.name.trim() }.filter { it.isNotBlank() }
        return (fixed + custom).distinctBy { repository.normalizeArabic(it) }
    }

    fun addCustomCaseCategory(name: String) {
        viewModelScope.launch {
            repository.addCustomCaseCategory(name)
                .onSuccess {
                    globalSuccessMsg = "تم إضافة تصنيف مخصص جديد بنجاح."
                }
                .onFailure {
                    globalErrorMsg = it.message ?: "تعذر إضافة التصنيف المخصص."
                }
        }
    }

    fun removeCustomCaseCategory(categoryId: Int) {
        viewModelScope.launch {
            repository.customCaseCategoryDao.deleteById(categoryId)
            globalSuccessMsg = "تم حذف التصنيف المخصص."
        }
    }

    // --- Clients Operations ---
    fun saveClient(client: Client, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val normalizedName = client.name.trim()
            val normalizedPhone = client.phone.trim()
            if (normalizedName.isBlank() || normalizedPhone.isBlank()) {
                globalErrorMsg = "اسم الموكل ورقم الهاتف مطلوبان."
                return@launch
            }
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
            if (legalCase.title.trim().isBlank()) {
                globalErrorMsg = "عنوان القضية مطلوب."
                return@launch
            }
            if (legalCase.caseNumber.trim().isBlank()) {
                globalErrorMsg = "رقم القضية مطلوب."
                return@launch
            }
            if (legalCase.clientId <= 0) {
                globalErrorMsg = "يجب ربط القضية بموكل صالح."
                return@launch
            }
            val isNewCase = legalCase.id == 0
            val insertedId = repository.caseDao.insertCase(legalCase).toInt()
            
            // Generate automated tasks for new cases based on rules engine
            if (isNewCase) {
                val rules = CaseRulesEngine.getRules(legalCase.caseType, repository::normalizeArabic)
                rules.automatedTasks.forEach { (taskTitle, daysOffset) ->
                    val dueDateMs = System.currentTimeMillis() + daysOffset * 24L * 60 * 60 * 1000
                    val dueDateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ENGLISH).format(java.util.Date(dueDateMs))
                    repository.taskDao.insertTask(
                        LegalTask(
                            caseId = insertedId,
                            title = taskTitle,
                            description = "مهمة تلقائية مولدة بناءً على نوع القضية",
                            status = "مفتوحة",
                            dueDate = dueDateStr
                        )
                    )
                }
            }
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
            if (session.caseId <= 0) {
                globalErrorMsg = "يجب ربط الجلسة بقضية."
                return@launch
            }
            if (ensureDateFormat(session.date).isBlank()) {
                globalErrorMsg = "تاريخ الجلسة يجب أن يكون بصيغة YYYY-MM-DD."
                return@launch
            }
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
            val now = System.currentTimeMillis()
            val orderedSessions = sessionsFlow
                .mapNotNull { session -> parseSessionDateTime(session)?.let { timestamp -> session to timestamp } }
                .sortedBy { it.second }
            val incoming = orderedSessions
                .filter { (session, timestamp) -> timestamp >= now && session.status != "ملغاة" }
                .map { it.first }
            val past = orderedSessions
                .filter { (_, timestamp) -> timestamp < now }
                .map { it.first }
            
            val targetCase = repository.caseDao.getCaseById(caseId)
            if (targetCase != null) {
                val nextDate = incoming.firstOrNull()?.date.orEmpty()
                val lastDate = past.lastOrNull()?.date.orEmpty()
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

    // --- Fee Tracking ---
    fun saveFeeRecord(feeRecord: FeeRecord, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.feeDao.insert(feeRecord)
            onDone()
            globalSuccessMsg = "تم حفظ سجل الأتعاب بنجاح."
        }
    }

    fun deleteFeeRecord(feeRecord: FeeRecord, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.feeDao.delete(feeRecord)
            onDone()
            globalSuccessMsg = "تم حذف سجل الأتعاب."
        }
    }

    // --- Case Files Import ---
    fun importCaseFile(
        caseId: Int,
        clientId: Int,
        fileName: String,
        fileUri: Uri,
        docType: String,
        docLength: Long,
        linkedSessionId: Int? = null
    ) {
        viewModelScope.launch {
            try {
                val caseTitle = repository.caseDao.getCaseById(caseId)?.title ?: "قضية غير معروفة"
                val clientName = repository.clientDao.getClientById(clientId)?.name ?: "عميل غير معروف"
                
                // 1. Copy to private app storage
                val copiedFile = repository.saveFileToPrivateStorage(caseId, fileName, fileUri)
                val extension = if (fileName.contains('.')) fileName.substringAfterLast('.').lowercase() else "txt"
                
                // 2. Check and perform text reading / index
                val supportsSearchIndex = LocalBusinessRules.isSearchableTextExtension(extension)
                val supportsOcr = LocalBusinessRules.isImageExtension(extension)
                val extractedText = when {
                    supportsSearchIndex -> repository.extractAndCleanText(copiedFile, extension)
                    supportsOcr -> repository.extractTextFromImage(copiedFile)
                    else -> ""
                }
                val extractionStatus = when {
                    supportsSearchIndex && extractedText.isNotBlank() -> "جاهز للبحث النصي"
                    supportsOcr && extractedText.isNotBlank() -> "تم OCR بنجاح"
                    supportsOcr -> "صورة محفوظة - OCR غير مكتمل"
                    else -> "مرفوع ومحفوظ محلياً"
                }
                
                // 2b. If the OCR did not return enough data, keep file searchable by metadata
                val finalExtractedText = if (extractedText.isNotBlank()) extractedText else ""
                
                val finalDocType = docType.ifBlank { repository.suggestDocumentType(fileName) }
                val statusText = extractionStatus
                val appearance = defaultCaseFileStyle(finalDocType, fileName)
                
                // Construct normalized search index matching all search criteria:
                // file name, doc type, custom manually entered/extracted text, case title, client name
                val searchIndexContent = "${copiedFile.name} $finalDocType $caseTitle $clientName $finalExtractedText"
                val normalizedIndex = repository.normalizeArabic(searchIndexContent)

                // 3. Save inside Database
                val caseFile = CaseFile(
                    caseId = caseId,
                    caseTitle = caseTitle,
                    clientId = clientId,
                    clientName = clientName,
                    linkedSessionId = linkedSessionId,
                    fileName = copiedFile.name,
                    filePath = copiedFile.absolutePath,
                    docType = finalDocType,
                    fileLength = docLength,
                    extractedText = finalExtractedText,
                    extractionStatus = statusText,
                    normalizedSearchIndex = normalizedIndex,
                    accentColorHex = appearance.accentColorHex,
                    cardStyle = appearance.cardStyle
                )
                repository.fileDao.insertFile(caseFile)
                globalSuccessMsg = "تم رفع المستند وربطه بالقضية وفهرسته محلياً بنجاح."
                AppNotificationManager.notifyDocumentStored(getApplication(), copiedFile.name, caseTitle)
            } catch (e: Exception) {
                globalErrorMsg = "فشل في حفظ المستند: ${e.message}"
            }
        }
    }

    fun suggestDocumentType(fileName: String): String = repository.suggestDocumentType(fileName)

    fun addClientInteraction(
        client: Client,
        interactionType: String,
        title: String,
        details: String,
        relatedCase: LegalCase? = null
    ) {
        viewModelScope.launch {
            if (title.trim().isBlank()) {
                globalErrorMsg = "عنوان سجل التواصل مطلوب."
                return@launch
            }
            repository.clientInteractionDao.insert(
                ClientInteraction(
                    clientId = client.id,
                    clientName = client.name,
                    interactionType = interactionType,
                    title = title.trim(),
                    details = details.trim(),
                    relatedCaseId = relatedCase?.id,
                    relatedCaseTitle = relatedCase?.title.orEmpty()
                )
            )
            globalSuccessMsg = "تم حفظ سجل التواصل للموكل."
        }
    }

    fun deleteClientInteraction(interaction: ClientInteraction) {
        viewModelScope.launch {
            repository.clientInteractionDao.delete(interaction)
            globalSuccessMsg = "تم حذف سجل التواصل."
        }
    }

    fun updateFileManualText(
        fileObj: CaseFile,
        newText: String,
        accentColorHex: String = fileObj.accentColorHex,
        cardStyle: String = fileObj.cardStyle,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val searchIndexContent = "${fileObj.fileName} ${fileObj.docType} ${fileObj.caseTitle} ${fileObj.clientName} $newText"
                val normalizedIndex = repository.normalizeArabic(searchIndexContent)
                val updatedFile = fileObj.copy(
                    extractedText = newText,
                    normalizedSearchIndex = normalizedIndex,
                    accentColorHex = accentColorHex,
                    cardStyle = cardStyle
                )
                repository.fileDao.insertFile(updatedFile)
                globalSuccessMsg = "تم حفظ النص وتحديث الفهرسة بالملف بنجاح."
                onDone()
            } catch (e: Exception) {
                globalErrorMsg = "فشل في تحديث النص: ${e.message}"
            }
        }
    }

    fun updateFileAppearance(
        fileObj: CaseFile,
        accentColorHex: String,
        cardStyle: String,
        onDone: () -> Unit = {}
    ) {
        updateFileManualText(fileObj, fileObj.extractedText, accentColorHex, cardStyle, onDone)
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

    fun exportCaseBundle(legalCase: LegalCase, onShareFile: (File) -> Unit) {
        viewModelScope.launch {
            val sessions = repository.sessionDao.getSessionsForCase(legalCase.id).firstOrNull().orEmpty()
            val tasks = repository.taskDao.getTasksForCase(legalCase.id).firstOrNull().orEmpty()
            val files = repository.fileDao.getFilesForCase(legalCase.id).firstOrNull().orEmpty()
            val bundle = repository.exportCaseBundle(legalCase, sessions, tasks, files)
            if (bundle != null) {
                globalSuccessMsg = "تم تجهيز حزمة كاملة لملف القضية والمرفقات."
                onShareFile(bundle)
            } else {
                globalErrorMsg = "تعذر تجهيز حزمة القضية."
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
            AppNotificationManager.notifyDocumentStored(
                getApplication(),
                generated.documentTitle,
                repository.caseDao.getCaseById(caseId)?.title.orEmpty()
            )
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
                AppNotificationManager.notifyBackupCreated(getApplication(), backup.name)
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
                AppNotificationManager.notifyInfo(
                    getApplication(),
                    "تمت الاستعادة",
                    "تم استرداد النسخة الاحتياطية بنجاح."
                )
                onRestart()
            } else {
                globalErrorMsg = "فشل استيراد النسخة الاحتياطية. تأكد من سلامة الملف وصيغة .mpb"
            }
            isBackupInProgress = false
        }
    }

    fun cleanupDuplicateCaseFiles() {
        viewModelScope.launch {
            val removed = repository.cleanupDuplicateCaseFiles()
            globalSuccessMsg = if (removed > 0) {
                "تم حذف $removed ملف/مرجع مكرر من أرشيف القضايا."
            } else {
                "لم يتم العثور على ملفات مكررة داخل أرشيف القضايا."
            }
        }
    }

    fun cleanupMissingFileReferences() {
        viewModelScope.launch {
            val removed = repository.cleanupMissingFileReferences()
            globalSuccessMsg = if (removed > 0) {
                "تم حذف $removed مرجع ملف مفقود من الفهرس."
            } else {
                "لا توجد مراجع ملفات مفقودة حالياً."
            }
        }
    }

    fun cleanupDuplicateGeneratedDocuments() {
        viewModelScope.launch {
            val removed = repository.cleanupDuplicateGeneratedDocuments()
            globalSuccessMsg = if (removed > 0) {
                "تم حذف $removed مستند مولد مكرر."
            } else {
                "لا توجد مستندات مولدة مكررة."
            }
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
            ImportTarget.TASKS -> mapOf(
                "title" to listOf("عنوان المهمة", "المهمة", "task", "title"),
                "description" to listOf("الوصف", "description"),
                "due_date" to listOf("تاريخ الاستحقاق", "due date", "due"),
                "priority" to listOf("الأولوية", "priority"),
                "status" to listOf("الحالة", "status"),
                "case_number" to listOf("رقم القضية", "case number"),
                "case_title" to listOf("عنوان القضية", "case title"),
                "client_name" to listOf("اسم العميل", "client")
            )
            ImportTarget.FILES_METADATA -> mapOf(
                "file_name" to listOf("اسم الملف", "file", "file name"),
                "doc_type" to listOf("نوع المستند", "doc type", "document type"),
                "case_number" to listOf("رقم القضية", "case number"),
                "case_title" to listOf("عنوان القضية", "case title"),
                "client_name" to listOf("اسم العميل", "client"),
                "file_path" to listOf("مسار الملف", "path", "file path"),
                "file_length" to listOf("حجم الملف", "size", "length"),
                "extracted_text" to listOf("النص المستخرج", "extracted", "text"),
                "notes" to listOf("ملاحظات", "notes")
            )
            ImportTarget.GENERATED_DOCS -> mapOf(
                "document_title" to listOf("عنوان المستند", "document title", "title"),
                "content" to listOf("المحتوى", "content", "text"),
                "case_number" to listOf("رقم القضية", "case number"),
                "case_title" to listOf("عنوان القضية", "case title"),
                "template_id" to listOf("template id", "رقم القالب"),
                "filled_fields_json" to listOf("الحقول", "fields", "json")
            )
        }
    }

    fun generateImportReportText(preview: ImportPreview): String {
        return buildString {
            appendLine("تقرير معاينة الاستيراد")
            appendLine("-----------------------")
            appendLine("الهدف: ${preview.target}")
            appendLine("إجمالي الصفوف: ${preview.totalRows}")
            appendLine("الصالحة: ${preview.validRows}")
            appendLine("غير الصالحة: ${preview.invalidRows}")
            appendLine("المكررات: ${preview.duplicates}")
            appendLine("التحذيرات: ${preview.warnings}")
            appendLine()
            preview.rows.take(200).forEach { row ->
                appendLine("صف ${row.rowNumber} | ${row.status} | ${row.reason}")
            }
            if (preview.rows.size > 200) {
                appendLine()
                appendLine("... تم اختصار التقرير إلى أول 200 صف")
            }
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
        return LocalBusinessRules.normalizeDateInput(dateText)
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
                    ImportTarget.TASKS -> {
                        val title = fieldValues["title"].orEmpty().trim()
                        if (title.isBlank()) {
                            previewRows += ImportRowPreview(rowNo, "INVALID", "عنوان المهمة مطلوب", fieldValues)
                            return@forEachIndexed
                        }
                        val caseNumber = fieldValues["case_number"].orEmpty().trim()
                        val caseTitle = fieldValues["case_title"].orEmpty().trim()
                        val linkedCase = findCaseByNumberOrTitle(existingCases, caseNumber, caseTitle)
                        if (caseNumber.isNotBlank() || caseTitle.isNotBlank()) {
                            if (linkedCase == null) {
                                warnings++
                                previewRows += ImportRowPreview(rowNo, "INVALID", "تعذر ربط المهمة بالقضية المحددة", fieldValues)
                                return@forEachIndexed
                            }
                        }
                        previewRows += ImportRowPreview(rowNo, "VALID", "جاهز للاستيراد", fieldValues)
                    }
                    ImportTarget.FILES_METADATA -> {
                        val fileName = fieldValues["file_name"].orEmpty().trim()
                        if (fileName.isBlank()) {
                            previewRows += ImportRowPreview(rowNo, "INVALID", "اسم الملف مطلوب", fieldValues)
                            return@forEachIndexed
                        }
                        val caseNumber = fieldValues["case_number"].orEmpty().trim()
                        val caseTitle = fieldValues["case_title"].orEmpty().trim()
                        val linkedCase = findCaseByNumberOrTitle(existingCases, caseNumber, caseTitle)
                        if (linkedCase == null) {
                            warnings++
                            previewRows += ImportRowPreview(rowNo, "INVALID", "تعذر ربط الملف بأي قضية", fieldValues)
                            return@forEachIndexed
                        }
                        previewRows += ImportRowPreview(rowNo, "VALID", "جاهز للاستيراد", fieldValues)
                    }
                    ImportTarget.GENERATED_DOCS -> {
                        val title = fieldValues["document_title"].orEmpty().trim()
                        val content = fieldValues["content"].orEmpty().trim()
                        if (title.isBlank() || content.isBlank()) {
                            previewRows += ImportRowPreview(rowNo, "INVALID", "عنوان المستند والمحتوى مطلوبان", fieldValues)
                            return@forEachIndexed
                        }
                        val caseNumber = fieldValues["case_number"].orEmpty().trim()
                        val caseTitle = fieldValues["case_title"].orEmpty().trim()
                        val linkedCase = findCaseByNumberOrTitle(existingCases, caseNumber, caseTitle)
                        if (linkedCase == null) {
                            warnings++
                            previewRows += ImportRowPreview(rowNo, "INVALID", "تعذر ربط المستند المولد بالقضية", fieldValues)
                            return@forEachIndexed
                        }
                        previewRows += ImportRowPreview(rowNo, "VALID", "جاهز للاستيراد", fieldValues)
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
                    ImportTarget.TASKS -> {
                        val title = row.values["title"].orEmpty().trim()
                        if (title.isBlank()) continue
                        val caseNumber = row.values["case_number"].orEmpty().trim()
                        val caseTitle = row.values["case_title"].orEmpty().trim()
                        val linkedCase = findCaseByNumberOrTitle(existingCases, caseNumber, caseTitle)

                        val clientFromCase = linkedCase?.let { c ->
                            existingClients.firstOrNull { it.id == c.clientId }
                        }

                        val task = LegalTask(
                            title = title,
                            description = row.values["description"].orEmpty(),
                            dueDate = ensureDateFormat(row.values["due_date"].orEmpty()),
                            priority = row.values["priority"].orEmpty().ifBlank { "متوسطة" },
                            status = row.values["status"].orEmpty().ifBlank { "مفتوحة" },
                            caseId = linkedCase?.id,
                            caseTitle = linkedCase?.title,
                            clientId = clientFromCase?.id,
                            clientName = clientFromCase?.name
                        )
                        repository.taskDao.insertTask(task)
                        importedCount++
                    }
                    ImportTarget.FILES_METADATA -> {
                        val fileName = row.values["file_name"].orEmpty().trim()
                        if (fileName.isBlank()) continue

                        val caseNumber = row.values["case_number"].orEmpty().trim()
                        val caseTitle = row.values["case_title"].orEmpty().trim()
                        val linkedCase = findCaseByNumberOrTitle(existingCases, caseNumber, caseTitle) ?: continue

                        val combinedText = buildString {
                            append(row.values["extracted_text"].orEmpty())
                            val notes = row.values["notes"].orEmpty()
                            if (notes.isNotBlank()) {
                                append("\n")
                                append(notes)
                            }
                        }

                        val file = CaseFile(
                            caseId = linkedCase.id,
                            caseTitle = linkedCase.title,
                            clientId = linkedCase.clientId,
                            clientName = linkedCase.clientName,
                            fileName = fileName,
                            filePath = row.values["file_path"].orEmpty().ifBlank { "metadata_only" },
                            docType = row.values["doc_type"].orEmpty().ifBlank { "مستند" },
                            fileLength = row.values["file_length"].orEmpty().toLongOrNull() ?: 0L,
                            extractedText = combinedText,
                            extractionStatus = "مستورد metadata",
                            normalizedSearchIndex = repository.normalizeArabic("${linkedCase.title} ${linkedCase.clientName} $fileName ${row.values["doc_type"].orEmpty()} $combinedText")
                        )
                        repository.fileDao.insertFile(file)
                        importedCount++
                    }
                    ImportTarget.GENERATED_DOCS -> {
                        val title = row.values["document_title"].orEmpty().trim()
                        val content = row.values["content"].orEmpty().trim()
                        if (title.isBlank() || content.isBlank()) continue

                        val caseNumber = row.values["case_number"].orEmpty().trim()
                        val caseTitle = row.values["case_title"].orEmpty().trim()
                        val linkedCase = findCaseByNumberOrTitle(existingCases, caseNumber, caseTitle) ?: continue

                        val generated = GeneratedDocument(
                            caseId = linkedCase.id,
                            templateId = row.values["template_id"].orEmpty().toIntOrNull() ?: 0,
                            documentTitle = title,
                            filledFieldsJson = row.values["filled_fields_json"].orEmpty().ifBlank { "{}" },
                            content = content
                        )
                        repository.generatedDocDao.insertGeneratedDocument(generated)
                        importedCount++
                    }
                }
            }
            globalSuccessMsg = "تم استيراد $importedCount صف بنجاح."
            onDone(importedCount)
        }
    }

    // --- Offline Smart Legal Assistant Calculations ---
    private fun String.toEnglishDigits(): String {
        var res = this
        val arabicDigits = arrayOf("٠","١","٢","٣","٤","٥","٦","٧","٨","٩")
        for (i in 0..9) { res = res.replace(arabicDigits[i], i.toString()) }
        return res
    }

    private fun parseDate(dateText: String): Long? {
        return try {
            val d = dateText.toEnglishDigits().trim()
            if (d.isBlank()) null else SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(d)?.time
        } catch (_: Exception) {
            null
        }
    }

    private fun parseSessionDateTime(session: CaseSession): Long? {
        val dateEng = session.date.toEnglishDigits().trim()
        val timeEng = session.time.toEnglishDigits().trim()
        val full = if (timeEng.isBlank()) "$dateEng 23:59" else "$dateEng $timeEng"
        val formats = listOf(
            "yyyy-MM-dd HH:mm", "yyyy-MM-dd H:mm",
            "dd-MM-yyyy HH:mm", "dd-MM-yyyy H:mm",
            "dd/MM/yyyy HH:mm", "dd/MM/yyyy H:mm",
            "yyyy/MM/dd HH:mm", "yyyy/MM/dd H:mm"
        )
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

    private fun isFeeOverdue(fee: FeeRecord, now: Long): Boolean {
        val outstanding = (fee.totalAmount - fee.paidAmount).coerceAtLeast(0.0)
        if (outstanding <= 0.0) return false
        val due = parseDate(fee.dueDate) ?: return false
        return due < now && fee.status != "مدفوعة"
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

    private fun findCaseByNumberOrTitle(
        existingCases: List<LegalCase>,
        caseNumber: String,
        caseTitle: String
    ): LegalCase? {
        return existingCases.firstOrNull {
            (caseNumber.isNotBlank() && repository.normalizeArabic(it.caseNumber) == repository.normalizeArabic(caseNumber)) ||
                (caseTitle.isNotBlank() && repository.normalizeArabic(it.title) == repository.normalizeArabic(caseTitle))
        }
    }

    fun caseReadinessScore(legalCase: LegalCase): Int {
        val sessions = allSessions.value.filter { it.caseId == legalCase.id }
        val files = allFiles.value.filter { it.caseId == legalCase.id }
        val tasks = allTasks.value.filter { it.caseId == legalCase.id }
        val rules = CaseRulesEngine.getRules(legalCase.caseType, repository::normalizeArabic)

        var score = 0
        if (legalCase.summary.isNotBlank()) score += 15
        if (legalCase.opponentName.isNotBlank()) score += 10
        if (legalCase.courtName.isNotBlank()) score += 10
        if (sessions.isNotEmpty()) score += 20
        if (tasks.any { it.status == "منتهية" }) score += 10
        if (files.isNotEmpty()) score += 10

        val matchedDocs = rules.requiredDocuments.count { required ->
            files.any { file -> requiredDocMatched(required, file) }
        }
        if (rules.requiredDocuments.isNotEmpty()) {
            score += ((matchedDocs.toDouble() / rules.requiredDocuments.size.toDouble()) * 25.0).toInt()
        }
        return score.coerceIn(0, 100)
    }

    fun caseReadinessLabel(legalCase: LegalCase): String {
        return LocalBusinessRules.readinessLabel(caseReadinessScore(legalCase))
    }

    private fun hasMeaningfulCaseData(
        legalCase: LegalCase,
        sessions: List<CaseSession>,
        tasks: List<LegalTask>,
        files: List<CaseFile>
    ): Boolean {
        return legalCase.summary.isNotBlank() ||
            legalCase.opponentName.isNotBlank() ||
            legalCase.courtName.isNotBlank() ||
            legalCase.courtCircle.isNotBlank() ||
            legalCase.notes.isNotBlank() ||
            sessions.isNotEmpty() ||
            tasks.isNotEmpty() ||
            files.isNotEmpty()
    }

    fun localAlertsSummary(): List<String> {
        val now = System.currentTimeMillis()
        val upcomingSessions = allSessions.value
            .mapNotNull { session -> parseSessionDateTime(session)?.let { session to it } }
            .filter { (session, time) -> time >= now && session.status != "ملغاة" && session.status != "منتهية" }
            .sortedBy { it.second }
            .take(3)
            .map { (session, _) -> "جلسة قريبة: ${session.caseTitle} - ${session.date} ${session.time}" }

        val overdueTasks = allTasks.value
            .filter { isTaskOverdue(it, now) }
            .take(3)
            .map { task -> "مهمة متأخرة: ${task.title}${task.caseTitle?.let { " | $it" } ?: ""}" }

        val overdueFees = allFeeRecords.value
            .filter { isFeeOverdue(it, now) }
            .take(3)
            .map { fee -> "أتعاب متأخرة: ${fee.title} | ${fee.clientName}${fee.caseTitle?.let { " | $it" } ?: ""}" }

        return (upcomingSessions + overdueTasks + overdueFees).ifEmpty { listOf("لا توجد تنبيهات محلية عاجلة حالياً.") }
    }

    private suspend fun composeHybridAssistantOutput(prompt: String, offlineText: String): String {
        val onlineResult = repository.requestOnlineAssistantAnswer(prompt = prompt, offlineSummary = offlineText)
        val cloudText = onlineResult?.getOrNull()?.trim().orEmpty()
        if (cloudText.isBlank()) return offlineText
        return buildString {
            appendLine(offlineText)
            appendLine()
            appendLine("[تحسين سحابي اختياري]")
            appendLine(cloudText)
        }
    }

    fun sendSmartAssistantChatMessage(caseId: Int, userMessage: String, onResult: (String) -> Unit) {
        val normalizedMessage = repository.normalizeArabic(userMessage)
        
        fun containsAny(vararg words: String): Boolean {
            return words.any { normalizedMessage.contains(repository.normalizeArabic(it)) }
        }
        
        // --- 1. Intent Scoring System ---
        val intents = mutableMapOf<String, Int>()
        
        // Summary Intent
        if (containsAny("ملخص", "لخص", "خلاصه")) intents["summary"] = 10
        if (containsAny("موجز", "نبذة", "اختصار")) intents["summary"] = (intents["summary"] ?: 0) + 5
        
        // Plan/Strategy Intent
        if (containsAny("خطة", "استراتيجية", "نعمل ايه", "نعمل إيه")) intents["plan"] = 10
        
        // Missing Docs Intent
        if (containsAny("مستند", "ورق", "ملف") && containsAny("ناقص", "نواقص", "مطلوب")) intents["missing_docs"] = 10
        if (containsAny("النواقص", "الناقصة")) intents["missing_docs"] = (intents["missing_docs"] ?: 0) + 8
        
        // Next Session Intent
        if (containsAny("جلسة") && containsAny("قادمة", "القادمة", "الجاية", "اللي جايه")) intents["next_session"] = 10
        if (containsAny("متى الجلسة", "امتي الجلسه", "تاريخ الجلسة", "الجلسه الجايه امتي")) intents["next_session"] = 10
        
        // Open Tasks Intent
        if (containsAny("مهام", "مهمة", "مطلوب مننا", "ورايا ايه")) intents["tasks"] = 10
        
        // Add Session Intent
        if (containsAny("اضف جلسة", "اضافة جلسة", "جلسة جديدة", "سجل جلسة")) intents["add_session"] = 15
        
        // Add Task Intent
        if (containsAny("اضف مهمة", "مهمة جديدة", "اضافة مهمة", "سجل مهمة")) intents["add_task"] = 15
        
        // Upload Doc Intent
        if (containsAny("رفع مستند", "اضافة مستند", "ارفع ملف", "اضف ملف")) intents["upload_doc"] = 15
        
        // Client Update Intent
        if (containsAny("تحديث للعميل", "رسالة للعميل", "اخبر العميل", "لخص للعميل")) intents["client_update"] = 15
        
        // Case Status Intent
        if (containsAny("وضع القضية", "حالة القضية", "ايه الوضع", "لخص الوضع")) intents["case_status"] = 15
        
        // Templates Intent
        if (containsAny("قالب", "قوالب", "نموذج", "صيغة")) intents["templates"] = 10
        
        // Prep Intent
        if (containsAny("تجهيز", "استعداد", "احضر ايه", "استعد للجلسه")) intents["prep"] = 10
        
        // Draft Intent
        if (containsAny("مذكرة", "صياغة", "مسودة", "اكتب", "مرافعة")) intents["draft"] = 10
        
        // Opponent Intent
        if (containsAny("خصم", "ضد من", "مين الخصم", "المنازع")) intents["opponent"] = 10
        
        // Fees Intent
        if (containsAny("اتعاب", "الأتعاب", "فلوس", "مستحقات", "دفع")) intents["fees"] = 10

        // Sessions Week Intent
        if (containsAny("جلسات الاسبوع", "هذا الاسبوع", "جلسات اسبوع")) intents["sessions_week"] = 12
        
        // Overdue Fees Intent
        if (containsAny("اتعاب متأخرة", "فلوس متاخره", "ديون", "مديونية")) intents["overdue_fees"] = 12
        
        // WhatsApp Update Intent
        if (containsAny("واتساب", "ابعت للعميل", "رسالة للعميل", "تحديث للعميل")) intents["whatsapp_update"] = 12

        // Document Q&A Intent (Smart Extract)
        if (containsAny("مستند", "ملف", "الورق") && containsAny("ماذا", "هل", "كم", "سؤال", "ايه المكتوب")) {
             intents["doc_qa"] = 15 
        }

        // Search Intent
        if (containsAny("بحث", "ابحث", "دورلي", "دور علي")) intents["search"] = 10

        val topIntent = intents.maxByOrNull { it.value }?.key

        // --- 2. Execute Top Intent ---
        when (topIntent) {
            "summary" -> getSmartAssistantSummary(caseId, onResult)
            "plan" -> getCaseActionPlan(caseId, onResult)
            "missing_docs" -> answerCaseQuestion(caseId, "missing_docs", onResult)
            "next_session" -> getNextSessionAssistant(caseId, onResult)
            "sessions_week" -> onResult("ميزة 'جلسات الأسبوع' ستتوفر قريباً لعرض كافة الجلسات المجدولة خلال الـ 7 أيام القادمة.")
            "overdue_fees" -> onResult("ميزة 'الأتعاب المتأخرة' قيد التطوير وستعرض إجمالي المستحقات المطلوبة من الموكل.")
            "add_session" -> {
                onResult("حاضر، جاري فتح شاشة إضافة جلسة جديدة لهذه القضية...")
                navigateTo(Screen.SessionAddEdit(presetCaseId = caseId))
            }
            "add_task" -> {
                onResult("حاضر، جاري فتح شاشة إضافة مهمة جديدة لهذه القضية...")
                navigateTo(Screen.TaskAddEdit(presetCaseId = caseId))
            }
            "upload_doc" -> {
                onResult("حاضر، سأقوم بفتح تفاصيل القضية حيث يمكنك الضغط على 'إرفاق مستند' لرفع الملفات بسهولة.")
                navigateTo(Screen.CaseDetails(caseId))
            }
            "client_update", "whatsapp_update" -> getClientUpdateAssistant(caseId, userMessage, onResult)
            "case_status" -> getCaseStatusAssistant(caseId, userMessage, onResult)
            "tasks" -> getOpenTasksAssistant(caseId, onResult)
            "templates" -> {
                val caseType = (allCases.value + archivedCases.value).firstOrNull { it.id == caseId }?.caseType.orEmpty()
                getSuggestedTemplatesForCase(caseType, onResult)
            }
            "prep" -> getSessionPrepAssistant(caseId, onResult)
            "draft" -> draftCaseMemo(caseId, onResult)
            "opponent" -> answerCaseQuestion(caseId, "opponent", onResult)
            "fees" -> getFeesAssistant(caseId, userMessage, onResult)
            "doc_qa" -> {
                // For document Q&A, fallback to cloud if enabled, otherwise do local search
                viewModelScope.launch {
                    val caseTitle = (allCases.value + archivedCases.value).firstOrNull { it.id == caseId }?.title ?: "القضية"
                    val files = repository.fileDao.getFilesForCase(caseId).firstOrNull().orEmpty()
                    val filesText = files.joinToString("\n\n") { "مستند ${it.fileName}:\n${it.extractedText.take(1500)}" }
                    val contextText = "تفاصيل القضية: $caseTitle\nالملفات:\n$filesText"
                    
                    val onlineReply = repository.requestOnlineAssistantAnswer(userMessage, contextText)
                    if (onlineReply != null && onlineReply.isSuccess) {
                        onResult("استناداً إلى مستندات القضية:\n${onlineReply.getOrNull()}")
                    } else {
                        // Fallback to local search logic
                        val query = userMessage.replace(Regex("(ماذا|هل|كم|عن|في|مستند|ملف|سؤال)"), "").trim()
                        searchInsideCaseFiles(caseId, query.ifBlank { userMessage }) { text, _ -> onResult(text) }
                    }
                }
            }
            "search" -> {
                val query = userMessage
                    .replace("ابحث داخل الملفات", "", ignoreCase = true)
                    .replace("بحث داخل الملفات", "", ignoreCase = true)
                    .replace("ابحث", "", ignoreCase = true)
                    .replace("بحث", "", ignoreCase = true)
                    .trim()
                searchInsideCaseFiles(caseId, query.ifBlank { userMessage }) { text, _ -> onResult(text) }
            }
            else -> {
                getDefaultAssistantResponse(caseId, userMessage, onResult)
            }
        }
    }

    private fun getFeesAssistant(caseId: Int, userMessage: String, onResult: (String) -> Unit) {
                viewModelScope.launch {
                    isAssistantLoading = true
                    val legalCase = repository.caseDao.getCaseById(caseId)
                    if (legalCase == null) {
                        isAssistantLoading = false
                        onResult("تعذر تحميل القضية.")
                        return@launch
                    }
                    val fees = repository.feeDao.getForCase(caseId).firstOrNull().orEmpty()
                    val totalOutstanding = fees.sumOf { (it.totalAmount - it.paidAmount).coerceAtLeast(0.0) }
                    val overdue = fees.count { isFeeOverdue(it, System.currentTimeMillis()) }
                    val text = buildString {
                        appendLine("ملخص الأتعاب للقضية: ${legalCase.title}")
                        appendLine("إجمالي السجلات: ${fees.size}")
                        appendLine("الإجمالي المستحق: ${String.format(Locale.ENGLISH, "%.2f", totalOutstanding)} ${fees.firstOrNull()?.currency ?: "ج.م"}")
                        appendLine("السجلات المتأخرة: $overdue")
                        if (fees.isEmpty()) {
                            appendLine("لا توجد سجلات أتعاب مسجلة لهذه القضية حالياً.")
                        } else {
                            appendLine("تفاصيل مختصرة:")
                            fees.take(6).forEach { fee ->
                                appendLine("- ${fee.title} | مستحق: ${fee.totalAmount} | مدفوع: ${fee.paidAmount} | الحالة: ${fee.status}")
                            }
                        }
                    }
                    val finalText = composeHybridAssistantOutput(
                        prompt = userMessage,
                        offlineText = text
                    )
                    isAssistantLoading = false
                    onResult(finalText)
                }
                return
            }

    private fun getClientUpdateAssistant(caseId: Int, userMessage: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            isAssistantLoading = true
            val legalCase = repository.caseDao.getCaseById(caseId)
            val client = legalCase?.clientId?.let { repository.clientDao.getClientById(it) }
            val nextSession = repository.sessionDao.getSessionsForCase(caseId).firstOrNull()?.filter {
                (parseSessionDateTime(it) ?: Long.MIN_VALUE) >= System.currentTimeMillis()
            }?.minByOrNull { parseSessionDateTime(it) ?: Long.MAX_VALUE }
            
            val offlineText = buildString {
                appendLine("رسالة مقترحة للموكل (${client?.name ?: "العميل"}):")
                appendLine("---")
                appendLine("السيد/ة المحترم/ة ${client?.name ?: ""}،")
                appendLine("نحيطكم علماً بأن متابعة قضيتكم (${legalCase?.title ?: ""}) مستمرة بشكل ممتاز.")
                if (nextSession != null) {
                    appendLine("ونود إعلامكم بأن الجلسة القادمة محددة بتاريخ ${nextSession.date} الساعة ${nextSession.time}.")
                    appendLine("لأي استفسارات، نحن في خدمتكم.")
                } else {
                    appendLine("نحن نعمل على إنهاء كافة الإجراءات وسنوافيكم بأي تحديثات قريباً.")
                }
                appendLine("مع تحيات مكتب المحاماة.")
                appendLine("---")
                appendLine("💡 (يمكنك نسخ هذه الرسالة وإرسالها للعميل عبر الواتساب)")
            }
            val finalText = composeHybridAssistantOutput(prompt = userMessage, offlineText = offlineText)
            isAssistantLoading = false
            onResult(finalText)
        }
    }

    private fun getCaseStatusAssistant(caseId: Int, userMessage: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            isAssistantLoading = true
            val legalCase = repository.caseDao.getCaseById(caseId)
            if (legalCase == null) {
                isAssistantLoading = false
                onResult("تعذر تحميل القضية.")
                return@launch
            }
            val nextSession = repository.sessionDao.getSessionsForCase(caseId).firstOrNull()?.filter {
                (parseSessionDateTime(it) ?: Long.MIN_VALUE) >= System.currentTimeMillis()
            }?.minByOrNull { parseSessionDateTime(it) ?: Long.MAX_VALUE }
            
            val tasks = repository.taskDao.getTasksForCase(caseId).firstOrNull() ?: emptyList()
            val pendingTasks = tasks.count { isTaskOpen(it) }
            
            val fees = repository.feeDao.getForCase(caseId).firstOrNull() ?: emptyList()
            val totalOutstanding = fees.sumOf { (it.totalAmount - it.paidAmount).coerceAtLeast(0.0) }
            
            val offlineText = buildString {
                appendLine("📊 **موجز حالة القضية: ${legalCase.title}**")
                appendLine("- **الحالة:** ${legalCase.status}")
                appendLine("- **تاريخ الجلسة القادمة:** ${nextSession?.date ?: "غير محدد"}")
                appendLine("- **المهام المتبقية:** $pendingTasks مهمة")
                appendLine("- **إجمالي المستحقات المتبقية:** ${String.format(Locale.ENGLISH, "%.2f", totalOutstanding)} ج.م")
                appendLine("")
                appendLine("هذا الموجز يعطيك لمحة سريعة وشاملة لإدارة القضية.")
            }
            val finalText = composeHybridAssistantOutput(prompt = userMessage, offlineText = offlineText)
            isAssistantLoading = false
            onResult(finalText)
        }
    }

    private fun getDefaultAssistantResponse(caseId: Int, userMessage: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            isAssistantLoading = true
            val legalCase = repository.caseDao.getCaseById(caseId)
            if (legalCase == null) {
                isAssistantLoading = false
                onResult("تعذر تحميل القضية.")
                return@launch
            }
            
            val text = buildString {
                appendLine("عذراً، لم أفهم طلبك بدقة 🧐.")
                appendLine("يرجى استخدام عبارات مباشرة أو الضغط على أحد الأزرار السريعة بالأعلى.")
                appendLine("أمثلة لما يمكنني فهمه:")
                appendLine("• لخص القضية")
                appendLine("• ايه الوضع")
                appendLine("• متى الجلسة القادمة")
                appendLine("• النواقص")
                appendLine("• اضف جلسة / اضافة مهمة")
            }
            
            val onlineResult = repository.requestOnlineAssistantAnswer(prompt = userMessage, offlineSummary = "المستخدم سأل: $userMessage\nهذه القضية: ${legalCase.title}")
            val cloudText = onlineResult?.getOrNull()?.trim().orEmpty()
            
            val finalText = if (cloudText.isNotBlank()) cloudText else text
            
            isAssistantLoading = false
            onResult(finalText)
        }
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
            if (!hasMeaningfulCaseData(caseObj, sessionsList, tasksList, filesList)) {
                isAssistantLoading = false
                onResult("لا توجد معلومات كافية داخل هذه القضية بعد لتوليد ملخص. أضف جلسة أو مستندًا أو ملخصًا أولًا.")
                return@launch
            }

            val sortedByDate = sessionsList.sortedBy { parseSessionDateTime(it) ?: Long.MAX_VALUE }
            val upcoming = sortedByDate.firstOrNull {
                (parseSessionDateTime(it) ?: Long.MAX_VALUE) >= System.currentTimeMillis() &&
                    it.status != "منتهية" &&
                    it.status != "ملغاة"
            }
            val lastSession = sessionsList
                .filter {
                    (parseSessionDateTime(it) ?: Long.MIN_VALUE) < System.currentTimeMillis() &&
                        it.status != "ملغاة"
                }
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
            val finalText = composeHybridAssistantOutput(
                prompt = "لخص القضية وقدم خطوات عملية مختصرة.",
                offlineText = text
            )
            isAssistantLoading = false
            onResult(finalText)
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
            if (files.isEmpty()) {
                isAssistantLoading = false
                onResult("لا توجد ملفات مرفوعة لهذه القضية بعد، لذلك لا يمكن تحديد النواقص بدقة.")
                return@launch
            }
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
            val finalText = composeHybridAssistantOutput(
                prompt = "حلل نواقص المستندات المطلوبة للقضية واقترح أولويات الاستكمال.",
                offlineText = text
            )
            isAssistantLoading = false
            onResult(finalText)
        }
    }

    fun getNextSessionAssistant(caseId: Int, onResult: (String) -> Unit) {
        viewModelScope.launch {
            isAssistantLoading = true
            val sessions = repository.sessionDao.getSessionsForCase(caseId).firstOrNull().orEmpty()
            val now = System.currentTimeMillis()
            val startOfDay = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
            val next = sessions
                .filter {
                    (parseSessionDateTime(it) ?: Long.MAX_VALUE) >= startOfDay &&
                        it.status != "منتهية" &&
                        it.status != "ملغاة"
                }
                .minByOrNull { parseSessionDateTime(it) ?: Long.MAX_VALUE }

            val text = if (next == null) {
                if (sessions.isEmpty()) {
                    "لا توجد جلسات مسجلة لهذه القضية بعد."
                } else {
                    "لا توجد جلسات قادمة حالياً لهذه القضية. راجع تبويب الجلسات لتحديد موعد جديد."
                }
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
            val finalText = composeHybridAssistantOutput(
                prompt = "اقترح تجهيزات سريعة للجلسة القادمة وفق البيانات الحالية.",
                offlineText = text
            )
            isAssistantLoading = false
            onResult(finalText)
        }
    }

    fun getOpenTasksAssistant(caseId: Int, onResult: (String) -> Unit) {
        viewModelScope.launch {
            isAssistantLoading = true
            val now = System.currentTimeMillis()
            val tasks = repository.taskDao.getTasksForCase(caseId).firstOrNull().orEmpty().filter(::isTaskOpen)
            val text = if (tasks.isEmpty()) {
                "لا توجد مهام مفتوحة مسجلة لهذه القضية حالياً."
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
            val finalText = composeHybridAssistantOutput(
                prompt = "رتب المهام المفتوحة حسب الأولوية والتنفيذ العملي.",
                offlineText = text
            )
            isAssistantLoading = false
            onResult(finalText)
        }
    }

    fun getCaseActionPlan(caseId: Int, onResult: (String) -> Unit) {
        viewModelScope.launch {
            isAssistantLoading = true
            val now = System.currentTimeMillis()
            val caseObj = repository.caseDao.getCaseById(caseId)
            if (caseObj == null) {
                isAssistantLoading = false
                onResult("تعذر تحميل القضية.")
                return@launch
            }

            val sessions = repository.sessionDao.getSessionsForCase(caseId).firstOrNull().orEmpty()
            val tasks = repository.taskDao.getTasksForCase(caseId).firstOrNull().orEmpty()
            val files = repository.fileDao.getFilesForCase(caseId).firstOrNull().orEmpty()
            val rules = CaseRulesEngine.getRules(caseObj.caseType, repository::normalizeArabic)
            if (!hasMeaningfulCaseData(caseObj, sessions, tasks, files)) {
                isAssistantLoading = false
                onResult("لا توجد بيانات كافية حالياً لبناء خطة عمل لهذه القضية. أضف جلسة أو ملفًا أو مهامًا أولاً.")
                return@launch
            }

            val nextSession = sessions
                .filter {
                    (parseSessionDateTime(it) ?: Long.MIN_VALUE) >= now &&
                        it.status != "منتهية" &&
                        it.status != "ملغاة"
                }
                .minByOrNull { parseSessionDateTime(it) ?: Long.MAX_VALUE }
            val overdueTasks = tasks.filter { isTaskOverdue(it, now) }
            val openTasks = tasks.filter(::isTaskOpen)
            val missingDocuments = rules.requiredDocuments.filterNot { required ->
                files.any { file -> requiredDocMatched(required, file) }
            }

            val text = buildString {
                appendLine("خطة عمل تنفيذية للقضية: ${caseObj.title}")
                appendLine("1. راجع بيانات الملف الأساسية وتأكد من اكتمال رقم القضية والمحكمة والدائرة.")
                if (nextSession != null) {
                    appendLine("2. جهّز للجلسة القادمة بتاريخ ${nextSession.date}${nextSession.time.takeIf { it.isNotBlank() }?.let { " الساعة $it" } ?: ""}.")
                    appendLine("   المطلوب حالياً: ${nextSession.requirements.ifBlank { "مراجعة المستندات والمذكرة قبل الجلسة." }}")
                } else {
                    appendLine("2. لا توجد جلسة قادمة مسجلة؛ أضف موعد الجلسة التالية أو حدّث موقف الدعوى.")
                }
                if (missingDocuments.isNotEmpty()) {
                    appendLine("3. استكمل المستندات الناقصة بالأولوية:")
                    missingDocuments.take(4).forEach { appendLine("   • $it") }
                    if (missingDocuments.size > 4) appendLine("   • وهناك ${missingDocuments.size - 4} مستندات أخرى تحتاج مراجعة.")
                } else {
                    appendLine("3. ملف المستندات الأساسي مكتمل وفق نوع القضية الحالي.")
                }
                if (openTasks.isNotEmpty()) {
                    appendLine("4. نفّذ المهام المفتوحة التالية:")
                    openTasks.take(4).forEach { task ->
                        val overdue = if (isTaskOverdue(task, now)) " [متأخرة]" else ""
                        appendLine("   • ${task.title}$overdue")
                    }
                } else {
                    appendLine("4. لا توجد مهام مفتوحة؛ أنشئ مهام متابعة عملية حتى لا يظل الملف بدون Tracking.")
                }
                appendLine("5. حدّث ملخص القضية وملاحظات الدفاع بعد كل خطوة تنفيذية.")
                if (overdueTasks.isNotEmpty()) {
                    appendLine("تنبيه عاجل: يوجد ${overdueTasks.size} مهام متأخرة تحتاج إغلاقاً أو إعادة جدولة.")
                }
            }

            val finalText = composeHybridAssistantOutput(
                prompt = "حوّل بيانات القضية الحالية إلى خطة عمل تنفيذية قصيرة وواضحة للمحامي.",
                offlineText = text
            )
            isAssistantLoading = false
            onResult(finalText)
        }
    }

    fun getSessionPrepAssistant(caseId: Int, onResult: (String) -> Unit) {
        viewModelScope.launch {
            isAssistantLoading = true
            val caseObj = repository.caseDao.getCaseById(caseId)
            if (caseObj == null) {
                isAssistantLoading = false
                onResult("تعذر تحميل القضية.")
                return@launch
            }
            val sessions = repository.sessionDao.getSessionsForCase(caseId).firstOrNull().orEmpty()
            val files = repository.fileDao.getFilesForCase(caseId).firstOrNull().orEmpty()
            val next = sessions
                .filter {
                    (parseSessionDateTime(it) ?: Long.MIN_VALUE) >= System.currentTimeMillis() &&
                        it.status != "منتهية" &&
                        it.status != "ملغاة"
                }
                .minByOrNull { parseSessionDateTime(it) ?: Long.MAX_VALUE }
            val rules = CaseRulesEngine.getRules(caseObj.caseType, repository::normalizeArabic)
            val missing = rules.requiredDocuments.filterNot { required -> files.any { requiredDocMatched(required, it) } }
            if (next == null && sessions.isEmpty()) {
                isAssistantLoading = false
                onResult("لا توجد جلسة مسجلة بعد لهذه القضية، لذلك لا يمكن تجهيز جلسة قادمة.")
                return@launch
            }
            val text = buildString {
                if (next == null) {
                    appendLine("لا توجد جلسة قادمة مسجلة لهذه القضية.")
                } else {
                    appendLine("تجهيز الجلسة القادمة")
                    appendLine("• القضية: ${caseObj.title}")
                    appendLine("• الموعد: ${next.date} ${next.time.ifBlank { "" }}")
                    appendLine("• المحكمة: ${next.court.ifBlank { caseObj.courtName }}")
                    appendLine("• المطلوب بالجلسة: ${next.requirements.ifBlank { "مراجعة الملف والمذكرة قبل الجلسة." }}")
                    appendLine("• المستندات الناقصة الأهم:")
                    if (missing.isEmpty()) appendLine("  - لا توجد نواقص أساسية وفق نوع القضية")
                    missing.take(5).forEach { appendLine("  - $it") }
                }
            }
            val finalText = composeHybridAssistantOutput(
                prompt = "جهز المحامي للجلسة القادمة بخطوات مختصرة وتنبيهات عملية.",
                offlineText = text
            )
            isAssistantLoading = false
            onResult(finalText)
        }
    }

    fun draftCaseMemo(caseId: Int, onResult: (String) -> Unit) {
        viewModelScope.launch {
            isAssistantLoading = true
            val caseObj = repository.caseDao.getCaseById(caseId)
            if (caseObj == null) {
                isAssistantLoading = false
                onResult("تعذر تحميل القضية.")
                return@launch
            }
            val text = buildString {
                appendLine("مسودة أولية لمذكرة/طلب في القضية")
                appendLine("عنوان القضية: ${caseObj.title}")
                appendLine("رقمها: ${caseObj.caseNumber}/${caseObj.caseYear}")
                appendLine("المحكمة: ${caseObj.courtName} - ${caseObj.courtCircle}")
                appendLine("الموكل: ${caseObj.clientName}")
                appendLine("الخصم: ${caseObj.opponentName.ifBlank { "................" }}")
                appendLine()
                appendLine("أولاً: الوقائع")
                appendLine(caseObj.summary.ifBlank { "يُستكمل عرض الوقائع بحسب مستندات الملف." })
                appendLine()
                appendLine("ثانياً: الطلبات")
                appendLine("- يلتمس الطالب/المدعى اتخاذ ما يلزم قانوناً.")
                appendLine("- تُستكمل الطلبات التفصيلية وفق نوع الدعوى والمستندات.")
                appendLine()
                appendLine("ثالثاً: المستندات المؤيدة")
                appendLine("- تُدرج حافظة المستندات وفق المرفوع داخل ملف القضية.")
            }
            val finalText = composeHybridAssistantOutput(
                prompt = "أنشئ مسودة قانونية أولية واضحة ومهنية اعتماداً على بيانات القضية.",
                offlineText = text
            )
            isAssistantLoading = false
            onResult(finalText)
        }
    }

    fun answerCaseQuestion(caseId: Int, questionKey: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val caseObj = repository.caseDao.getCaseById(caseId)
            if (caseObj == null) {
                onResult("تعذر تحميل القضية.")
                return@launch
            }
            when (questionKey) {
                "last_session" -> {
                    val sessions = repository.sessionDao.getSessionsForCase(caseId).firstOrNull().orEmpty()
                    val last = sessions
                        .filter { (parseSessionDateTime(it) ?: Long.MIN_VALUE) < System.currentTimeMillis() }
                        .maxByOrNull { parseSessionDateTime(it) ?: Long.MIN_VALUE }
                    onResult(last?.let { "آخر جلسة كانت بتاريخ ${it.date} ${it.time} بعنوان ${it.title}." } ?: "لا توجد جلسات سابقة مسجلة لهذه القضية.")
                }
                "missing_docs" -> getMissingDocumentsSuggestion(caseId, onResult)
                "opponent" -> onResult("الخصم المسجل في هذه القضية: ${caseObj.opponentName.ifBlank { "غير محدد" }}")
                "readiness" -> onResult("جاهزية القضية الحالية: ${caseReadinessScore(caseObj)}% - ${caseReadinessLabel(caseObj)}")
                else -> onResult("السؤال غير مدعوم حالياً.")
            }
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
            val finalText = composeHybridAssistantOutput(
                prompt = "رشح أفضل القوالب المتاحة لنوع القضية مع استخدام عملي.",
                offlineText = text
            )
            isAssistantLoading = false
            onResult(finalText)
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
            val finalText = composeHybridAssistantOutput(
                prompt = "حوّل القائمة إلى خطوات تنفيذ يومية قصيرة ومنظمة.",
                offlineText = checklistText
            )
            isAssistantLoading = false
            onResult(finalText)
        }
    }
}
