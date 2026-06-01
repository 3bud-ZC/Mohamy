package com.example.ui.screens
import com.example.data.*
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
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

import com.example.ui.theme.LegalGoldSecondary
import com.example.ui.theme.LegalNavyPrimary
import com.example.ui.formatFileSize
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun BackupRestoreScreen(viewModel: AppViewModel, dbRestoreLauncher: androidx.activity.result.ActivityResultLauncher<String>) {
    val context = LocalContext.current
    var showRestoreWarning by remember { mutableStateOf(false) }
    val lastBackupText = viewModel.lastBackupAtMillis?.let {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH).format(it)
    } ?: "لا توجد نسخة احتياطية مسجلة بعد"
    val lastBackupSizeText = viewModel.lastBackupSizeBytes?.let { formatFileSize(it) } ?: "غير متاح"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.CloudSync, "نسخ احتياطي", tint = LegalNavyPrimary, modifier = Modifier.size(80.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("النسخة الاحتياطية واسترجاع البيانات 📂", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = LegalNavyPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "لأن بيانات قضائك وعملائك سرية تماماً ومخزنة محلياً بالكامل على هاتفك دون وساطة خادمة، فإن أخذ نسخ احتياطية بصفة دورية هي مسؤوليتك لمنع فقدان البيانات عند تبديل الهاتف أو ضياعه.",
            fontSize = 12.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text("نطاق النسخة الحالية: نسخة شاملة تشمل قاعدة البيانات + ملفات المرفقات المحلية داخل التخزين الخاص بالتطبيق.", fontSize = 12.sp, color = Color(0xFF2E7D32), textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Text("آخر نسخة: $lastBackupText", fontSize = 12.sp, color = LegalNavyPrimary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text("حجم آخر نسخة: $lastBackupSizeText", fontSize = 12.sp, color = LegalNavyPrimary, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                viewModel.executeDatabaseBackup { file ->
                    // Trigger native android sharesheet safely
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
                .height(55.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary),
            enabled = !viewModel.isBackupInProgress
        ) {
            if (viewModel.isBackupInProgress) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("جاري إنشاء النسخة...", fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.CloudDownload, "إنشاء وتصدير باقة البيانات")
                Spacer(modifier = Modifier.width(8.dp))
                Text("إنشاء تصدير نسخة احتياطية (.mpb)", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { showRestoreWarning = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = LegalGoldSecondary),
            enabled = !viewModel.isBackupInProgress
        ) {
            Icon(Icons.Default.CloudUpload, "استيراد ملف البيانات")
            Spacer(modifier = Modifier.width(8.dp))
            Text("استيراد واستعادة البيانات السابقة", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "تنبيه: سيؤدي استيراد كود ملف احتياطي سابق إلى مسح وتجاوز البيانات الحالية نهائياً.",
            color = Color.Red,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )

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
