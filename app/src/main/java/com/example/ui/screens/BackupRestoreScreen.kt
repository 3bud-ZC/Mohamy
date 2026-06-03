package com.example.ui.screens
import com.example.data.*
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.example.ui.theme.LegalGoldSecondary
import com.example.ui.theme.LegalGrayLight
import com.example.ui.theme.LegalNavyPrimary
import com.example.ui.theme.ScreenSectionCard
import com.example.ui.formatFileSize
import com.example.ui.theme.legalScreenBackground
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun BackupRestoreScreen(viewModel: AppViewModel, dbRestoreLauncher: androidx.activity.result.ActivityResultLauncher<String>) {
    val context = LocalContext.current
    var showRestoreWarning by remember { mutableStateOf(false) }
    val files by viewModel.allFiles.collectAsStateWithLifecycle()
    val generatedDocs by viewModel.allGeneratedDocuments.collectAsStateWithLifecycle()
    val clients by viewModel.allClients.collectAsStateWithLifecycle()
    val cases by viewModel.allCases.collectAsStateWithLifecycle()
    val lastBackupText = viewModel.lastBackupAtMillis?.let {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH).format(it)
    } ?: "لا توجد نسخة احتياطية مسجلة بعد"
    val lastBackupSizeText = viewModel.lastBackupSizeBytes?.let { formatFileSize(it) } ?: "غير متاح"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .legalScreenBackground()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = LegalNavyPrimary)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(LegalGoldSecondary.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CloudSync, "نسخ احتياطي", tint = LegalGoldSecondary, modifier = Modifier.size(26.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("النسخة الاحتياطية والاستعادة", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text("حماية بيانات المكتب محلياً", fontSize = 13.sp, color = Color.White.copy(alpha = 0.82f))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "النسخة تشمل قاعدة البيانات + المرفقات داخل تخزين التطبيق الخاص، وتُصدّر كملف واحد بصيغة .mpb.",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    lineHeight = 19.sp
                )
            }
        }

        ScreenSectionCard(title = "حالة النسخ") {
            Text("آخر نسخة: $lastBackupText", fontSize = 12.sp, color = LegalNavyPrimary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("حجم آخر نسخة: $lastBackupSizeText", fontSize = 12.sp, color = LegalNavyPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Text("الموكلون: ${clients.size} | القضايا: ${cases.size}", fontSize = 12.sp, color = LegalNavyPrimary)
            Text("ملفات القضايا: ${files.size} | المستندات المولدة: ${generatedDocs.size}", fontSize = 12.sp, color = LegalNavyPrimary)
        }

        ScreenSectionCard(title = "إجراءات النسخ") {
            Button(
                onClick = {
                    viewModel.executeDatabaseBackup { file ->
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/octet-stream"
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "تصدير وحفظ ملف الاحتياط (.mpb)"))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary),
                enabled = !viewModel.isBackupInProgress,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (viewModel.isBackupInProgress) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("جاري إنشاء النسخة...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.CloudDownload, "إنشاء وتصدير باقة البيانات")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إنشاء وتصدير النسخة الاحتياطية", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showRestoreWarning = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = LegalGoldSecondary),
                border = BorderStroke(1.dp, LegalGoldSecondary.copy(alpha = 0.45f)),
                enabled = !viewModel.isBackupInProgress,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.CloudUpload, "استيراد ملف البيانات")
                Spacer(modifier = Modifier.width(8.dp))
                Text("استيراد واستعادة البيانات", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = LegalGrayLight,
                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.18f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    "تنبيه: استيراد نسخة قديمة سيستبدل البيانات الحالية بالكامل.",
                    color = Color.Red,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
                )
            }
        }

        ScreenSectionCard(title = "صيانة وتنظيف") {
            Text(
                "استخدم هذه الأدوات قبل الاستعادة أو بعد الاستيراد لتصفية البيانات المكررة وتنظيف المراجع المكسورة.",
                fontSize = 12.sp,
                color = Color.Gray,
                lineHeight = 19.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = { viewModel.cleanupDuplicateCaseFiles() },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.ContentCopy, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("حذف الملفات المكررة من الأرشيف", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { viewModel.cleanupMissingFileReferences() },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                border = BorderStroke(1.dp, LegalNavyPrimary.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.BrokenImage, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("تنظيف مراجع الملفات المفقودة", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { viewModel.cleanupDuplicateGeneratedDocuments() },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                border = BorderStroke(1.dp, LegalGoldSecondary.copy(alpha = 0.45f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = LegalGoldSecondary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.AutoDelete, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("حذف المستندات المولدة المكررة", fontWeight = FontWeight.Bold)
            }
        }

        if (showRestoreWarning) {
            AlertDialog(
                onDismissRequest = { showRestoreWarning = false },
                title = { Text("تحذير قبل الاستعادة") },
                text = { Text("سيتم استبدال قاعدة البيانات الحالية. هل تريد المتابعة لاختيار ملف النسخة؟") },
                confirmButton = {
                    Button(onClick = {
                        showRestoreWarning = false
                        dbRestoreLauncher.launch("*/*")
                    }) { Text("متابعة") }
                },
                dismissButton = {
                    TextButton(onClick = { showRestoreWarning = false }) { Text("إلغاء") }
                }
            )
        }
    }
}
