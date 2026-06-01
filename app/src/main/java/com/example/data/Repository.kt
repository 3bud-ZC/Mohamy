package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import android.net.Uri
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class Repository(private val db: AppDatabase, private val context: Context) {

    val clientDao = db.clientDao()
    val caseDao = db.caseDao()
    val sessionDao = db.sessionDao()
    val taskDao = db.taskDao()
    val fileDao = db.fileDao()
    val templateDao = db.templateDao()
    val generatedDocDao = db.generatedDocDao()
    val licenseDao = db.licenseDao()
    private val prefs = context.getSharedPreferences("mohamy_phone_prefs", Context.MODE_PRIVATE)

    // --- Arabic Normalization ---
    fun normalizeArabic(text: String): String {
        var result = text.lowercase(Locale.ROOT)
        // Remove Tashkeel (diacritics)
        val diacritics = Regex("[\u064B-\u065F\u0670]")
        result = result.replace(diacritics, "")
        // Remove Tatweel (Kashida)
        result = result.replace(Regex("\u0640"), "")
        // Normalize Alef (آ أ إ -> ا)
        result = result.replace(Regex("[\u0622\u0623\u0625]"), "\u0627")
        // Normalize Yeh / Alef Maksura (ى -> ي)
        result = result.replace(Regex("\u0649"), "\u064A")
        // Normalize Te Marbuta (ة -> ه)
        result = result.replace(Regex("\u0629"), "\u0647")
        // Normalize Spaces
        result = result.replace(Regex("\\s+"), " ").trim()
        return result
    }

    // --- Search Engine ---
    fun searchAll(query: String, normalizedQuery: String): Flow<List<CaseFile>> {
        return fileDao.searchFilesText(normalizedQuery)
    }

    // --- Template Seeding ---
    suspend fun seedTemplatesIfEmpty() {
        val existingTemplates = templateDao.getAllTemplates().firstOrNull().orEmpty()
        if (existingTemplates.isNotEmpty()) {
            return
        }
        val defaultTemplates = listOf(
            LegalTemplate(
                id = 1,
                title = "إنذار بسداد أجرة",
                category = "إنذارات",
                caseType = "إيجارات",
                description = "إنذار قانوني بسداد الأجرة المتأخرة.",
                requiredFieldsJson = """["التاريخ","اسم_المؤجر","اسم_المستأجر","عنوان_العين","مبلغ_الأجرة","شهر_السداد","تاريخ_عقد_الإيجار","المحكمة"]""",
                templateBody = """إنه في يوم {{التاريخ}}
بناء على طلب السيد/ {{اسم_المؤجر}}، ننذر السيد/ {{اسم_المستأجر}} بسداد قيمة الأجرة عن شهر {{شهر_السداد}} الخاصة بالعين {{عنوان_العين}} والمقدرة بمبلغ {{مبلغ_الأجرة}}.
وحيث أن عقد الإيجار المؤرخ {{تاريخ_عقد_الإيجار}} ما زال نافذاً، لذا يلزم السداد فوراً وإلا اتخذت الإجراءات القضائية أمام محكمة {{المحكمة}}.
ولأجل العلم."""
            ),
            LegalTemplate(
                id = 2,
                title = "إنذار بإخلاء",
                category = "إنذارات",
                caseType = "إيجارات",
                description = "إنذار بإخلاء العين المؤجرة لانتهاء المدة أو الإخلال.",
                requiredFieldsJson = """["التاريخ","اسم_المؤجر","اسم_المستأجر","عنوان_العين","تاريخ_انتهاء_العقد","سبب_الإخلاء"]""",
                templateBody = """إنه في يوم {{التاريخ}}
يُنذر السيد/ {{اسم_المستأجر}} بإخلاء العين الكائنة في {{عنوان_العين}}، لانتهاء مدة العقد بتاريخ {{تاريخ_انتهاء_العقد}} أو بسبب {{سبب_الإخلاء}}.
في حال عدم الإخلاء سيتم اتخاذ الإجراءات القانونية اللازمة."""
            ),
            LegalTemplate(
                id = 3,
                title = "عقد إيجار",
                category = "عقود",
                caseType = "إيجارات",
                description = "صيغة عقد إيجار بين مؤجر ومستأجر.",
                requiredFieldsJson = """["التاريخ","اسم_المؤجر","رقم_بطاقة_المؤجر","اسم_المستأجر","رقم_بطاقة_المستأجر","عنوان_العين","مدة_الإيجار","تاريخ_البدء","القيمة_الإيجارية","مبلغ_التأمين"]""",
                templateBody = """عقد إيجار
في تاريخ {{التاريخ}} تم الاتفاق بين:
الطرف الأول (المؤجر): {{اسم_المؤجر}} - بطاقة {{رقم_بطاقة_المؤجر}}
الطرف الثاني (المستأجر): {{اسم_المستأجر}} - بطاقة {{رقم_بطاقة_المستأجر}}
العين المؤجرة: {{عنوان_العين}}، لمدة {{مدة_الإيجار}} تبدأ من {{تاريخ_البدء}} مقابل قيمة إيجارية {{القيمة_الإيجارية}} وتأمين {{مبلغ_التأمين}}."""
            ),
            LegalTemplate(
                id = 4,
                title = "عقد بيع ابتدائي",
                category = "عقود",
                caseType = "عقارات",
                description = "صيغة عقد بيع ابتدائي لعقار.",
                requiredFieldsJson = """["التاريخ","اسم_البائع","رقم_بطاقة_البائع","اسم_المشتري","رقم_بطاقة_المشتري","عنوان_العين","مساحة_العين","الثمن_الإجمالي","المقدم","المتبقي"]""",
                templateBody = """عقد بيع ابتدائي
في يوم {{التاريخ}} باع الطرف الأول {{اسم_البائع}} إلى الطرف الثاني {{اسم_المشتري}} العقار الكائن في {{عنوان_العين}} بمساحة {{مساحة_العين}}.
الثمن الإجمالي {{الثمن_الإجمالي}}، سدد منه {{المقدم}} وباقي قدره {{المتبقي}}."""
            ),
            LegalTemplate(
                id = 5,
                title = "مخالصة",
                category = "مخالصات",
                caseType = "عام",
                description = "مخالصة مالية وإبراء ذمة.",
                requiredFieldsJson = """["التاريخ","اسم_المقر","رقم_بطاقة_المقر","اسم_المستفيد","المبلغ","سبب_المخالصة"]""",
                templateBody = """إقرار مخالصة
أقر أنا {{اسم_المقر}} بطاقة رقم {{رقم_بطاقة_المقر}} أنني استلمت من {{اسم_المستفيد}} مبلغ {{المبلغ}} وذلك عن {{سبب_المخالصة}}.
وبذلك أبرئ ذمته نهائياً. تحريراً في {{التاريخ}}."""
            ),
            LegalTemplate(
                id = 6,
                title = "إقرار دين",
                category = "إقرارات",
                caseType = "عام",
                description = "إقرار مديونية وتعهد بالسداد.",
                requiredFieldsJson = """["التاريخ","اسم_المدين","رقم_بطاقة_المدين","اسم_الدائن","مبلغ_الدين","تاريخ_الاستحقاق"]""",
                templateBody = """إقرار دين
أقر أنا {{اسم_المدين}} بطاقة رقم {{رقم_بطاقة_المدين}} أنني مدين للسيد {{اسم_الدائن}} بمبلغ {{مبلغ_الدين}} وأتعهد بالسداد في {{تاريخ_الاستحقاق}}.
تحريراً في {{التاريخ}}."""
            ),
            LegalTemplate(
                id = 7,
                title = "طلب تأجيل",
                category = "طلبات",
                caseType = "عام",
                description = "طلب تأجيل جلسة.",
                requiredFieldsJson = """["التاريخ","المحكمة","الدائرة","رقم_الدعوى","سنة_الدعوى","اسم_المحامي","سبب_التأجيل"]""",
                templateBody = """السيد رئيس محكمة {{المحكمة}} الدائرة {{الدائرة}}
مقدمه: الأستاذ {{اسم_المحامي}}
في الدعوى رقم {{رقم_الدعوى}} لسنة {{سنة_الدعوى}}.
نلتمس التأجيل لجلسة قادمة بسبب: {{سبب_التأجيل}}.
وتفضلوا بقبول الاحترام. تحريراً في {{التاريخ}}."""
            ),
            LegalTemplate(
                id = 8,
                title = "مذكرة دفاع عامة",
                category = "مذكرات",
                caseType = "عام",
                description = "نموذج مذكرة دفاع عامة.",
                requiredFieldsJson = """["التاريخ","المحكمة","الدائرة","رقم_الدعوى","سنة_الدعوى","صفة_الموكل","اسم_الخصم","الوقائع","الدفوع","الطلبات"]""",
                templateBody = """مذكرة دفاع
بدفاع {{صفة_الموكل}} ضد {{اسم_الخصم}}
في الدعوى رقم {{رقم_الدعوى}} لسنة {{سنة_الدعوى}} أمام محكمة {{المحكمة}} دائرة {{الدائرة}}.
الوقائع: {{الوقائع}}
الدفوع: {{الدفوع}}
الطلبات: {{الطلبات}}
تحريراً في {{التاريخ}}."""
            ),
            LegalTemplate(
                id = 9,
                title = "شكوى عامة",
                category = "شكاوى",
                caseType = "عام",
                description = "نموذج شكوى عامة للجهة المختصة.",
                requiredFieldsJson = """["التاريخ","الجهة_المقدمة_إليها","اسم_مقدم_الشكوى","الصفة","موضوع_الشكوى","الوقائع","الطلبات"]""",
                templateBody = """شكوى عامة
مقدمة إلى: {{الجهة_المقدمة_إليها}}
مقدم الشكوى: {{اسم_مقدم_الشكوى}} ({{الصفة}})
الموضوع: {{موضوع_الشكوى}}
الوقائع: {{الوقائع}}
الطلبات: {{الطلبات}}
التاريخ: {{التاريخ}}."""
            ),
            LegalTemplate(
                id = 10,
                title = "محضر صلح",
                category = "محاضر",
                caseType = "عام",
                description = "نموذج محضر صلح بين طرفين.",
                requiredFieldsJson = """["التاريخ","الطرف_الأول","الطرف_الثاني","موضوع_النزاع","بنود_الصلح","مبلغ_إن_وجد"]""",
                templateBody = """محضر صلح
في يوم {{التاريخ}} تم الاتفاق بين {{الطرف_الأول}} و{{الطرف_الثاني}} بشأن {{موضوع_النزاع}}.
بنود الصلح: {{بنود_الصلح}}.
القيمة المتفق عليها إن وجدت: {{مبلغ_إن_وجد}}."""
            ),
            LegalTemplate(
                id = 11,
                title = "توكيل خاص",
                category = "توكيلات",
                caseType = "عام",
                description = "نموذج توكيل خاص بالإجراءات القضائية.",
                requiredFieldsJson = """["التاريخ","اسم_الموكل","رقم_بطاقة_الموكل","اسم_المحامي","موضوع_التوكيل","رقم_القضية_إن_وجد"]""",
                templateBody = """توكيل خاص
أنا {{اسم_الموكل}} بطاقة رقم {{رقم_بطاقة_الموكل}} أوكل الأستاذ {{اسم_المحامي}} في مباشرة إجراءات {{موضوع_التوكيل}}
وذلك بخصوص القضية رقم {{رقم_القضية_إن_وجد}}، مع حق الحضور والتوقيع واتخاذ ما يلزم قانوناً.
تحريراً في {{التاريخ}}."""
            )
        )
        templateDao.insertTemplates(defaultTemplates)
        Log.d("Repository", "Ensured core templates: ${defaultTemplates.size}")
    }

    // --- Private Files Storage helper ---
    fun getCaseFilesDirectory(caseId: Int): File {
        val baseDir = File(context.filesDir, "mohamy_phone/files/cases/$caseId")
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }
        return baseDir
    }

    suspend fun saveFileToPrivateStorage(caseId: Int, fileName: String, fileUri: Uri): File {
        return withContext(Dispatchers.IO) {
            val destinationDir = getCaseFilesDirectory(caseId)
            val destFile = File(destinationDir, fileName)
            context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
                FileOutputStream(destFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            destFile
        }
    }

    suspend fun extractAndCleanText(file: File, fileType: String): String {
        return withContext(Dispatchers.IO) {
            try {
                if (!file.exists()) return@withContext ""
                if (fileType.lowercase() == "txt" || file.extension.lowercase() == "txt") {
                    val bytes = FileInputStream(file).use { it.readBytes() }
                    return@withContext try {
                        String(bytes, Charsets.UTF_8)
                    } catch (_: Exception) {
                        String(bytes)
                    }
                }
                ""
            } catch (e: Exception) {
                Log.e("Repository", "Error extracting text", e)
                ""
            }
        }
    }

    suspend fun loadImportTable(uri: Uri): Result<ImportedTable> {
        return withContext(Dispatchers.IO) {
            DataImportParser.parse(context, uri)
        }
    }

    private fun addFileToZip(zip: ZipOutputStream, source: File, entryName: String) {
        if (!source.exists() || !source.isFile) return
        zip.putNextEntry(ZipEntry(entryName))
        FileInputStream(source).use { input ->
            input.copyTo(zip)
        }
        zip.closeEntry()
    }

    private fun addDirectoryToZip(zip: ZipOutputStream, sourceDir: File, baseEntry: String) {
        if (!sourceDir.exists()) return
        sourceDir.walkTopDown().forEach { file ->
            if (file.isDirectory) return@forEach
            val relative = file.relativeTo(sourceDir).invariantSeparatorsPath
            addFileToZip(zip, file, "$baseEntry/$relative")
        }
    }

    private fun copyDirectory(source: File, target: File) {
        if (!source.exists()) return
        source.walkTopDown().forEach { file ->
            val relative = file.relativeTo(source)
            val destination = File(target, relative.path)
            if (file.isDirectory) {
                destination.mkdirs()
            } else {
                destination.parentFile?.mkdirs()
                FileInputStream(file).use { input ->
                    FileOutputStream(destination).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }

    private fun deleteDirectoryContents(dir: File) {
        if (!dir.exists()) return
        dir.walkBottomUp().forEach {
            if (it != dir) it.delete()
        }
    }

    // --- Backup & Restore Logic ---
    suspend fun backupDatabaseFile(): File? {
        return withContext(Dispatchers.IO) {
            try {
                db.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL)")

                val dbFile = context.getDatabasePath("mohamy_phone_database")
                if (!dbFile.exists()) return@withContext null

                val walFile = File(dbFile.path + "-wal")
                val shmFile = File(dbFile.path + "-shm")
                val filesRoot = File(context.filesDir, "mohamy_phone/files")

                val dateString = SimpleDateFormat("yyyy_MM_dd_HHmmss", Locale.ENGLISH).format(Date())
                val backupFile = File(context.cacheDir, "mohamy_phone_backup_$dateString.mpb")

                ZipOutputStream(BufferedOutputStream(FileOutputStream(backupFile))).use { zip ->
                    addFileToZip(zip, dbFile, "database/mohamy_phone_database")
                    if (walFile.exists()) addFileToZip(zip, walFile, "database/mohamy_phone_database-wal")
                    if (shmFile.exists()) addFileToZip(zip, shmFile, "database/mohamy_phone_database-shm")
                    addDirectoryToZip(zip, filesRoot, "files")
                    zip.putNextEntry(ZipEntry("meta/info.txt"))
                    zip.write("format=mpb-zip-v2\nincludes=db+files\n".toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                }
                backupFile
            } catch (e: Exception) {
                Log.e("Repository", "Backup failed", e)
                null
            }
        }
    }

    suspend fun restoreDatabaseFile(backupUri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            val restoreTempDir = File(context.cacheDir, "restore_temp_${System.currentTimeMillis()}")
            val previousDbDir = File(context.cacheDir, "restore_prev_db_${System.currentTimeMillis()}")
            val previousFilesDir = File(context.cacheDir, "restore_prev_files_${System.currentTimeMillis()}")

            try {
                restoreTempDir.mkdirs()
                context.contentResolver.openInputStream(backupUri)?.use { rawInput ->
                    ZipInputStream(BufferedInputStream(rawInput)).use { zip ->
                        var entry = zip.nextEntry
                        while (entry != null) {
                            if (!entry.isDirectory) {
                                val outFile = File(restoreTempDir, entry.name)
                                outFile.parentFile?.mkdirs()
                                FileOutputStream(outFile).use { output -> zip.copyTo(output) }
                            }
                            zip.closeEntry()
                            entry = zip.nextEntry
                        }
                    }
                } ?: return@withContext false

                val extractedDb = File(restoreTempDir, "database/mohamy_phone_database")
                if (!extractedDb.exists()) return@withContext false

                val extractedWal = File(restoreTempDir, "database/mohamy_phone_database-wal")
                val extractedShm = File(restoreTempDir, "database/mohamy_phone_database-shm")
                val extractedFilesRoot = File(restoreTempDir, "files")

                val targetDb = context.getDatabasePath("mohamy_phone_database")
                val targetWal = File(targetDb.path + "-wal")
                val targetShm = File(targetDb.path + "-shm")
                val targetFilesRoot = File(context.filesDir, "mohamy_phone/files")

                db.close()

                if (targetDb.exists()) {
                    previousDbDir.mkdirs()
                    FileInputStream(targetDb).use { input ->
                        FileOutputStream(File(previousDbDir, targetDb.name)).use { output -> input.copyTo(output) }
                    }
                    if (targetWal.exists()) {
                        FileInputStream(targetWal).use { input ->
                            FileOutputStream(File(previousDbDir, targetWal.name)).use { output -> input.copyTo(output) }
                        }
                    }
                    if (targetShm.exists()) {
                        FileInputStream(targetShm).use { input ->
                            FileOutputStream(File(previousDbDir, targetShm.name)).use { output -> input.copyTo(output) }
                        }
                    }
                }

                if (targetFilesRoot.exists()) {
                    previousFilesDir.mkdirs()
                    copyDirectory(targetFilesRoot, previousFilesDir)
                }

                targetDb.parentFile?.mkdirs()
                FileInputStream(extractedDb).use { input ->
                    FileOutputStream(targetDb).use { output -> input.copyTo(output) }
                }
                if (targetWal.exists()) targetWal.delete()
                if (targetShm.exists()) targetShm.delete()
                if (extractedWal.exists()) {
                    FileInputStream(extractedWal).use { input ->
                        FileOutputStream(targetWal).use { output -> input.copyTo(output) }
                    }
                }
                if (extractedShm.exists()) {
                    FileInputStream(extractedShm).use { input ->
                        FileOutputStream(targetShm).use { output -> input.copyTo(output) }
                    }
                }

                deleteDirectoryContents(targetFilesRoot)
                if (extractedFilesRoot.exists()) {
                    targetFilesRoot.mkdirs()
                    copyDirectory(extractedFilesRoot, targetFilesRoot)
                }

                deleteDirectoryContents(restoreTempDir)
                restoreTempDir.delete()
                deleteDirectoryContents(previousDbDir)
                previousDbDir.delete()
                deleteDirectoryContents(previousFilesDir)
                previousFilesDir.delete()
                true
            } catch (e: Exception) {
                Log.e("Repository", "Restore failed", e)
                try {
                    val targetDb = context.getDatabasePath("mohamy_phone_database")
                    val targetWal = File(targetDb.path + "-wal")
                    val targetShm = File(targetDb.path + "-shm")
                    val targetFilesRoot = File(context.filesDir, "mohamy_phone/files")

                    val prevDb = File(previousDbDir, "mohamy_phone_database")
                    val prevWal = File(previousDbDir, "mohamy_phone_database-wal")
                    val prevShm = File(previousDbDir, "mohamy_phone_database-shm")

                    if (prevDb.exists()) {
                        FileInputStream(prevDb).use { input ->
                            FileOutputStream(targetDb).use { output -> input.copyTo(output) }
                        }
                    }
                    if (prevWal.exists()) {
                        FileInputStream(prevWal).use { input ->
                            FileOutputStream(targetWal).use { output -> input.copyTo(output) }
                        }
                    }
                    if (prevShm.exists()) {
                        FileInputStream(prevShm).use { input ->
                            FileOutputStream(targetShm).use { output -> input.copyTo(output) }
                        }
                    }
                    if (previousFilesDir.exists()) {
                        deleteDirectoryContents(targetFilesRoot)
                        targetFilesRoot.mkdirs()
                        copyDirectory(previousFilesDir, targetFilesRoot)
                    }
                } catch (_: Exception) {
                    // ignore rollback failures
                }
                false
            } finally {
                deleteDirectoryContents(restoreTempDir)
                restoreTempDir.delete()
                deleteDirectoryContents(previousDbDir)
                previousDbDir.delete()
                deleteDirectoryContents(previousFilesDir)
                previousFilesDir.delete()
            }
        }
    }

    // --- License System & Mock Backend Activation ---
    // The server must NOT store cases, files, clients. Only device authorization activation
    suspend fun checkLicenseStatus(): String {
        val currentLicense = licenseDao.getLicenseDirect() ?: return "غير نشط"
        val now = System.currentTimeMillis()
        if (currentLicense.expiryDate != 0L && now > currentLicense.expiryDate) {
            val updated = currentLicense.copy(status = "منتهي")
            licenseDao.insertLicense(updated)
            return "منتهي"
        }
        return currentLicense.status
    }

    private fun resolveLicenseServerBaseUrl(): String {
        return prefs.getString("license_server_url", null)
            ?.trim()
            ?.trimEnd('/')
            ?.takeIf { it.isNotBlank() }
            ?: BuildConfig.LICENSE_SERVER_URL.trim().trimEnd('/')
    }

    private fun currentDeviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown_device"
    }

    private fun hasInternetConnection(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun postJson(url: String, payload: JSONObject): Pair<Int, JSONObject?> {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 6000
            readTimeout = 10000
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
        }
        return try {
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload.toString())
            }
            val code = connection.responseCode
            val body = try {
                (if (code in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.readText()
            } catch (_: Exception) {
                null
            }
            code to body?.takeIf { it.isNotBlank() }?.let { JSONObject(it) }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseServerExpiryMillis(expiresAt: String?): Long {
        if (expiresAt.isNullOrBlank()) return System.currentTimeMillis() + (365L * 24L * 60L * 60L * 1000L)
        return try {
            java.time.Instant.parse(expiresAt).toEpochMilli()
        } catch (_: Exception) {
            System.currentTimeMillis() + (365L * 24L * 60L * 60L * 1000L)
        }
    }

    suspend fun activateLicense(username: String, activationCode: String): Result<LicenseCache> {
        return withContext(Dispatchers.IO) {
            try {
                if (username.isEmpty() || activationCode.isEmpty()) {
                    return@withContext Result.failure(Exception("يرجى إدخال اسم المستخدم وكلمة المرور."))
                }

                val baseUrl = resolveLicenseServerBaseUrl()
                val deviceId = currentDeviceId()
                val payload = JSONObject().apply {
                    put("username", username.trim())
                    put("password", activationCode)
                    put("device_id", deviceId)
                    put("device_name", Build.MODEL ?: "Android")
                    put("platform", "android")
                    put("app_version", BuildConfig.VERSION_NAME)
                }

                try {
                    val (statusCode, responseJson) = postJson("$baseUrl/api/license/activate", payload)
                    if (statusCode in 200..299 && responseJson != null) {
                        val now = System.currentTimeMillis()
                        val license = LicenseCache(
                            id = 1,
                            username = username.trim(),
                            activationCode = "server_auth",
                            activatedDeviceId = deviceId,
                            activationDate = now,
                            lastCheckDate = now,
                            expiryDate = parseServerExpiryMillis(responseJson.optString("expires_at", null)),
                            status = "نشط",
                            lawyerName = responseJson.optString("lawyer_name", "الأستاذ المحامي"),
                            officeName = responseJson.optString("office_name", "مكتب المحاماة والخدمات القانونية"),
                            phone = responseJson.optString("phone", ""),
                            barNumber = responseJson.optString("license_key", "")
                        )
                        licenseDao.insertLicense(license)
                        prefs.edit()
                            .putString("license_token", responseJson.optString("token", ""))
                            .putString("license_status", "نشط")
                            .putLong("license_last_check", now)
                            .apply()
                        seedTemplatesIfEmpty()
                        return@withContext Result.success(license)
                    }

                    val err = responseJson?.optString("error", "activation_failed") ?: "activation_failed"
                    return@withContext Result.failure(
                        Exception(
                            when (err) {
                                "blocked" -> "تم إيقاف هذا الحساب من قبل المسؤول."
                                "expired" -> "انتهت صلاحية الترخيص."
                                "device_bound" -> "الحساب مرتبط بجهاز آخر."
                                "inactive" -> "الحساب غير مفعل بعد."
                                else -> responseJson?.optString("message", "تعذر تفعيل الحساب.")
                                    ?: "تعذر تفعيل الحساب."
                            }
                        )
                    )
                } catch (networkError: Exception) {
                    // Development-safe fallback for local testing only in debug builds
                    if (BuildConfig.DEBUG && username.trim() == "test" && activationCode.trim() == "123456") {
                        val oneYearMillis = 365L * 24 * 60 * 60 * 1000
                        val license = LicenseCache(
                            id = 1,
                            username = "test",
                            activationCode = "123456",
                            activatedDeviceId = "DEV_MOCK_EMULATOR_001",
                            activationDate = System.currentTimeMillis(),
                            lastCheckDate = System.currentTimeMillis(),
                            expiryDate = System.currentTimeMillis() + oneYearMillis,
                            status = "نشط",
                            lawyerName = "الأستاذ المحامي (حساب تجريبي)",
                            officeName = "مكتب المحاماة والخدمات القانونية (نسخة تجريبية)",
                            phone = "01001234567",
                            barNumber = "أ/123456 (تجريبي)"
                        )
                        licenseDao.insertLicense(license)
                        seedTemplatesIfEmpty()
                        return@withContext Result.success(license)
                    }
                    return@withContext Result.failure(Exception("فشلت عملية التفعيل بسبب تعذر الوصول لسيرفر الترخيص."))
                }
            } catch (e: Exception) {
                Result.failure(Exception(e.message ?: "فشلت عملية التفعيل."))
            }
        }
    }

    suspend fun checkLicenseOnlineIfDue(): Result<String> {
        return withContext(Dispatchers.IO) {
            val local = licenseDao.getLicenseDirect() ?: return@withContext Result.success("no_license")
            if (local.status != "نشط") return@withContext Result.success(local.status)
            if (!hasInternetConnection()) return@withContext Result.success("offline_grace")

            val now = System.currentTimeMillis()
            val lastCheck = prefs.getLong("license_last_check", local.lastCheckDate)
            val every7Days = 7L * 24L * 60L * 60L * 1000L
            if (now - lastCheck < every7Days) return@withContext Result.success("not_due")

            val token = prefs.getString("license_token", "") ?: ""
            if (token.isBlank()) return@withContext Result.success("missing_token_grace")

            try {
                val baseUrl = resolveLicenseServerBaseUrl()
                val payload = JSONObject().apply {
                    put("token", token)
                    put("device_id", currentDeviceId())
                    put("app_version", BuildConfig.VERSION_NAME)
                }
                val (statusCode, responseJson) = postJson("$baseUrl/api/license/check", payload)
                if (statusCode in 200..299) {
                    licenseDao.insertLicense(local.copy(lastCheckDate = now, status = "نشط"))
                    prefs.edit()
                        .putLong("license_last_check", now)
                        .putString("license_status", "نشط")
                        .apply()
                    return@withContext Result.success("active")
                }

                val status = responseJson?.optString("status", "")
                if (status == "blocked") {
                    licenseDao.insertLicense(local.copy(status = "موقوف", lastCheckDate = now))
                    prefs.edit().putString("license_status", "موقوف").putLong("license_last_check", now).apply()
                    return@withContext Result.failure(Exception("تم إيقاف الحساب من الإدارة."))
                }
                if (status == "expired") {
                    licenseDao.insertLicense(local.copy(status = "منتهي", lastCheckDate = now))
                    prefs.edit().putString("license_status", "منتهي").putLong("license_last_check", now).apply()
                    return@withContext Result.failure(Exception("انتهت صلاحية الترخيص."))
                }
                Result.success("server_unavailable_grace")
            } catch (_: Exception) {
                Result.success("network_grace")
            }
        }
    }

    fun clearLicenseSessionCache() {
        prefs.edit()
            .remove("license_token")
            .remove("license_status")
            .remove("license_last_check")
            .apply()
    }
}
