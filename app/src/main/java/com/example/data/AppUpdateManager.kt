package com.example.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val releaseNotes: String = "",
    val releaseTitle: String = "",
    val sourceUrl: String = BuildConfig.UPDATE_MANIFEST_URL
) {
    val displayVersionLabel: String
        get() = versionName.ifBlank { "v$versionCode" }
}

object AppUpdateParser {
    fun parse(rawJson: String, sourceUrl: String = BuildConfig.UPDATE_MANIFEST_URL): AppUpdateInfo {
        val json = rawJson.trim()
        val versionCode = readVersionCode(json)
            ?: throw IllegalArgumentException("تعذر قراءة رقم النسخة من ملف التحديث.")
        val versionName = readVersionName(json, versionCode)
        val apkUrl = readDownloadUrl(json)
            ?: throw IllegalArgumentException("تعذر العثور على رابط ملف APK داخل ملف التحديث.")
        return AppUpdateInfo(
            versionCode = versionCode,
            versionName = versionName,
            apkUrl = apkUrl,
            releaseNotes = readReleaseNotes(json),
            releaseTitle = extractStringField(json, "title")
                ?: extractStringField(json, "name").orEmpty(),
            sourceUrl = sourceUrl
        )
    }

    private fun readVersionCode(rawJson: String): Int? {
        extractNumberField(rawJson, "versionCode")?.let { return it }
        extractNumberField(rawJson, "version_code")?.let { return it }
        extractStringField(rawJson, "versionCode")?.toIntOrNull()?.let { return it }
        extractStringField(rawJson, "version_code")?.toIntOrNull()?.let { return it }

        val body = extractStringField(rawJson, "body").orEmpty()
        Regex("""versionCode\s*[:=]\s*(\d+)""", RegexOption.IGNORE_CASE)
            .find(body)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
            ?.let { return it }

        extractStringField(rawJson, "tag_name")?.trim()?.takeIf { it.isNotBlank() }?.let { tag ->
            parseVersionCodeFromTag(tag)?.let { return it }
        }

        return null
    }

    private fun parseVersionCodeFromTag(tag: String): Int? {
        val cleaned = tag.trim().removePrefix("v").removePrefix("V")
        cleaned.toIntOrNull()?.let { return it }
        val exactNumber = Regex("""^\d+$""").find(cleaned)?.value?.toIntOrNull()
        if (exactNumber != null) return exactNumber

        val semanticParts = cleaned.split('.')
        if (semanticParts.size >= 2 && semanticParts.all { it.all(Char::isDigit) }) {
            val major = semanticParts.getOrNull(0)?.toIntOrNull() ?: return null
            val minor = semanticParts.getOrNull(1)?.toIntOrNull() ?: 0
            val patch = semanticParts.getOrNull(2)?.toIntOrNull() ?: 0
            return major * 10000 + minor * 100 + patch
        }

        return Regex("""\d+""")
            .find(cleaned)
            ?.value
            ?.toIntOrNull()
    }

    private fun readVersionName(rawJson: String, versionCode: Int): String {
        val direct = extractStringField(rawJson, "versionName").orEmpty().trim()
        if (direct.isNotBlank()) return direct
        val alt = extractStringField(rawJson, "version_name").orEmpty().trim()
        if (alt.isNotBlank()) return alt
        val name = extractStringField(rawJson, "name").orEmpty().trim()
        if (name.isNotBlank()) return name
        val tag = extractStringField(rawJson, "tag_name").orEmpty().trim()
        return if (tag.isNotBlank()) tag else "v$versionCode"
    }

    private fun readReleaseNotes(rawJson: String): String {
        val notes = extractStringField(rawJson, "notes").orEmpty().trim()
        if (notes.isNotBlank()) return notes
        val changelog = extractStringField(rawJson, "changelog").orEmpty().trim()
        if (changelog.isNotBlank()) return changelog
        return extractStringField(rawJson, "body").orEmpty().trim()
    }

    private fun readDownloadUrl(rawJson: String): String? {
        val directKeys = listOf("apkUrl", "downloadUrl", "download_url", "assetUrl", "asset_url")
        for (key in directKeys) {
            val value = extractStringField(rawJson, key).orEmpty().trim()
            if (value.isNotBlank()) return value
        }

        val preferred = findPreferredAsset(rawJson)
        if (preferred.isNotBlank()) return preferred

        return null
    }

    private fun findPreferredAsset(rawJson: String): String {
        var fallback = ""
        val assetRegex = Regex(
            """"name"\s*:\s*"((?:\\.|[^"])*)".*?"browser_download_url"\s*:\s*"((?:\\.|[^"])*)"""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        for (match in assetRegex.findAll(rawJson)) {
            val name = unescapeJsonString(match.groupValues.getOrNull(1).orEmpty()).trim()
            val url = unescapeJsonString(match.groupValues.getOrNull(2).orEmpty()).trim()
            if (url.isBlank()) continue
            if (name.equals("app-release.apk", ignoreCase = true) || name.endsWith(".apk", ignoreCase = true)) {
                return url
            }
            if (fallback.isBlank()) fallback = url
        }
        return fallback
    }

    private fun extractNumberField(rawJson: String, key: String): Int? {
        val match = Regex(
            """"${Regex.escape(key)}"\s*:\s*(\d+)""",
            RegexOption.IGNORE_CASE
        ).find(rawJson)
        return match?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    private fun extractStringField(rawJson: String, key: String): String? {
        val match = Regex(
            """"${Regex.escape(key)}"\s*:\s*"((?:\\.|[^"])*)"""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        ).find(rawJson) ?: return null
        return unescapeJsonString(match.groupValues.getOrNull(1).orEmpty())
    }

    private fun unescapeJsonString(value: String): String {
        return buildString(value.length) {
            var index = 0
            while (index < value.length) {
                val ch = value[index]
                if (ch == '\\' && index + 1 < value.length) {
                    when (val next = value[index + 1]) {
                        '"' -> append('"')
                        '\\' -> append('\\')
                        '/' -> append('/')
                        'b' -> append('\b')
                        'f' -> append('\u000C')
                        'n' -> append('\n')
                        'r' -> append('\r')
                        't' -> append('\t')
                        'u' -> {
                            val hex = value.substring(index + 2, minOf(index + 6, value.length))
                            if (hex.length == 4) {
                                hex.toIntOrNull(16)?.let { append(it.toChar()) } ?: append("\\u$hex")
                                index += 4
                            } else {
                                append("\\u")
                            }
                        }
                        else -> append(next)
                    }
                    index += 2
                } else {
                    append(ch)
                    index++
                }
            }
        }
    }
}

class AppUpdateManager(private val context: Context) {
    suspend fun checkForUpdate(manifestUrl: String = BuildConfig.UPDATE_MANIFEST_URL): Result<AppUpdateInfo?> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val rawJson = fetchText(manifestUrl)
                val updateInfo = AppUpdateParser.parse(rawJson, manifestUrl)
                updateInfo.takeIf { it.versionCode > BuildConfig.VERSION_CODE }
            }
        }
    }

    suspend fun downloadUpdate(
        updateInfo: AppUpdateInfo,
        onProgress: (Int) -> Unit = {}
    ): Result<File> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val outputDir = File(context.cacheDir, "app-updates").apply { mkdirs() }
                val outputFile = File(outputDir, "mohamy-update-${updateInfo.versionCode}.apk")
                if (outputFile.exists()) {
                    outputFile.delete()
                }

                val connection = openConnection(updateInfo.apkUrl)
                val totalBytes = connection.contentLengthLong.takeIf { it > 0L }

                connection.inputStream.use { input ->
                    FileOutputStream(outputFile).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var bytesCopied = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            bytesCopied += read
                            totalBytes?.let { total ->
                                val progress = ((bytesCopied * 100L) / total).toInt().coerceIn(0, 100)
                                onProgress(progress)
                            }
                        }
                        output.flush()
                    }
                }
                connection.disconnect()

                onProgress(100)
                if (outputFile.length() <= 0L) {
                    throw IOException("تم تنزيل ملف فارغ.")
                }
                outputFile
            }
        }
    }

    fun createInstallIntent(apkFile: File): Intent {
        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun launchInstall(apkFile: File) {
        context.startActivity(createInstallIntent(apkFile))
    }

    private fun openConnection(urlString: String): HttpURLConnection {
        val connection = (URL(urlString).openConnection() as HttpURLConnection)
        connection.instanceFollowRedirects = true
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.setRequestProperty("User-Agent", "MohamyPhone/${BuildConfig.VERSION_NAME}")
        connection.setRequestProperty("Accept", "application/json, application/vnd.github+json, */*")
        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            val errorMessage = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            connection.disconnect()
            throw IOException(
                "تعذر الوصول إلى ملف التحديث. HTTP $responseCode${
                    if (errorMessage.isNotBlank()) " - $errorMessage" else ""
                }"
            )
        }
        return connection
    }

    private fun fetchText(urlString: String): String {
        val connection = openConnection(urlString)
        val text = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        connection.disconnect()
        return text
    }
}
