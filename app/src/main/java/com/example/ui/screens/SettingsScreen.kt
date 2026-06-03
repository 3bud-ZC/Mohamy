package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.AppViewModel
import com.example.data.AppNotificationManager
import com.example.data.LicenseCache
import com.example.data.Screen
import com.example.ui.theme.LegalGoldLight
import com.example.ui.theme.LegalGoldSecondary
import com.example.ui.theme.LegalGrayLight
import com.example.ui.theme.LegalNavyPrimary

@Composable
fun SettingsScreen(
    viewModel: AppViewModel,
    license: LicenseCache?,
    onRequestNotificationPermission: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val statusLabel = license?.status ?: "غير نشط"
    val statusColor = if (statusLabel == "نشط") Color(0xFF2E7D32) else Color(0xFFB00020)
    val statusBackground = if (statusLabel == "نشط") Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
    val cloudConfigured = viewModel.hasConfiguredCloudAssistant

    var lawyerName by remember(license?.lawyerName) { mutableStateOf(license?.lawyerName ?: "") }
    var officeName by remember(license?.officeName) { mutableStateOf(license?.officeName ?: "") }
    var phone by remember(license?.phone) { mutableStateOf(license?.phone ?: "") }
    var barNumber by remember(license?.barNumber) { mutableStateOf(license?.barNumber ?: "") }
    var serverUrl by remember(viewModel.licenseServerUrlInput) { mutableStateOf(viewModel.licenseServerUrlInput) }
    var cloudAssistantEnabled by remember(viewModel.isCloudAssistantEnabled) { mutableStateOf(viewModel.isCloudAssistantEnabled) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFF4F7FC), Color(0xFFEFF3FA), Color.White)
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = LegalNavyPrimary)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(LegalGoldSecondary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AccountBalance, null, tint = LegalGoldLight, modifier = Modifier.size(30.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (lawyerName.isBlank()) "الأستاذ المحامي" else lawyerName,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 19.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (officeName.isBlank()) "مكتب المحاماة والخدمات القانونية" else officeName,
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(statusBackground)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(statusLabel, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(14.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Badge, null, tint = LegalGoldLight, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("المعرف الرقمي بالنقابة: ", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                    Text(if (barNumber.isBlank()) "غير معروف" else barNumber, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Phone, null, tint = LegalGoldLight, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("رقم هاتف الاتصال: ", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                    Text(if (phone.isBlank()) "لا يوجد" else phone, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        SettingsSectionCard(title = "بيانات المحامي والمكتب") {
            OutlinedTextField(value = lawyerName, onValueChange = { lawyerName = it }, label = { Text("اسم المحامي") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = officeName, onValueChange = { officeName = it }, label = { Text("اسم المكتب") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("هاتف المكتب") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = barNumber, onValueChange = { barNumber = it }, label = { Text("رقم النقابة") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = { viewModel.saveLawOfficeProfile(lawyerName, officeName, phone, barNumber) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("حفظ البيانات محلياً", fontWeight = FontWeight.Bold)
            }
        }

        SettingsSectionCard(title = "التفعيل المحلي والسيرفر") {
            androidx.compose.material3.Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, LegalNavyPrimary.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VerifiedUser, null, tint = LegalNavyPrimary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("حالة الترخيص الحالية", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.weight(1f))
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .background(statusBackground, shape = RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(statusLabel, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = LegalGrayLight)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VpnKey, null, tint = LegalNavyPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("رمز الحساب:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(license?.username ?: "غير متاح", fontSize = 13.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = { Text("رابط سيرفر التراخيص") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text("الرابط الافتراضي الموصى به: https://mohamy.abud.fun", fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { viewModel.saveLicenseServerUrl(serverUrl) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Settings, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("حفظ رابط السيرفر", fontWeight = FontWeight.Bold)
            }
        }

        SettingsSectionCard(title = "أدوات سريعة") {
            SettingsActionButton(title = "فتح المساعد الذكي", icon = Icons.Default.AutoAwesome, onClick = { viewModel.navigateTo(Screen.SmartAssistant) })
            Spacer(modifier = Modifier.height(8.dp))
            SettingsActionButton(title = "استيراد بيانات", icon = Icons.Default.FileUpload, onClick = { viewModel.navigateTo(Screen.ImportData) })
            Spacer(modifier = Modifier.height(8.dp))
            SettingsActionButton(
                title = "إدارة النسخ الاحتياطي",
                icon = Icons.Default.Sync,
                onClick = { viewModel.navigateTo(Screen.BackupRestore) },
                containerColor = LegalGoldSecondary,
                contentColor = LegalNavyPrimary
            )
        }

        SettingsSectionCard(title = "المظهر") {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("الوضع الليلي الاحترافي", fontWeight = FontWeight.Bold, color = LegalNavyPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (viewModel.isDarkThemeEnabled) {
                            "مفعل حالياً مع ألوان أكثر عمقاً وتبايناً."
                        } else {
                            "مغلق حالياً. يمكنك تفعيله للحصول على واجهة داكنة أوضح."
                        },
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                Switch(
                    checked = viewModel.isDarkThemeEnabled,
                    onCheckedChange = { viewModel.updateDarkThemeEnabled(it) }
                )
            }
        }

        if (cloudConfigured) {
        SettingsSectionCard(title = "المساعد الذكي") {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("التحسين السحابي الاختياري", fontWeight = FontWeight.Bold, color = LegalNavyPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            if (cloudAssistantEnabled) {
                                "مفعل حالياً. المساعد يبني الرد محلياً ثم قد يضيف تحسيناً سحابياً عند الحاجة."
                            } else {
                                "معطل حالياً. كل أوامر المساعد تعمل محلياً فقط داخل الجهاز."
                            },
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = cloudAssistantEnabled,
                        onCheckedChange = {
                            cloudAssistantEnabled = it
                            viewModel.updateCloudAssistantEnabled(it)
                        }
                    )
                }
            }
        }

        SettingsSectionCard(title = "تحديث التطبيق من GitHub") {
            val installedVersionLabel = "${BuildConfig.VERSION_NAME} · code ${BuildConfig.VERSION_CODE}"
            val remoteVersionLabel = when {
                viewModel.updateAvailable && !viewModel.updateRemoteVersionName.isNullOrBlank() ->
                    "${viewModel.updateRemoteVersionName} · code ${viewModel.updateRemoteVersionCode}"
                viewModel.updateAvailable ->
                    "code ${viewModel.updateRemoteVersionCode}"
                else -> "لا توجد نسخة جديدة"
            }
            val statusChipText = when {
                viewModel.updateDownloadInProgress -> "جارٍ التنزيل"
                viewModel.updateCheckInProgress -> "جارٍ الفحص"
                viewModel.updateAvailable -> "تحديث متاح"
                else -> "محدّث"
            }
            val statusChipColor = when {
                viewModel.updateDownloadInProgress -> Color(0xFFB26A00)
                viewModel.updateCheckInProgress -> Color(0xFF556B8E)
                viewModel.updateAvailable -> Color(0xFF1565C0)
                else -> Color(0xFF2E7D32)
            }
            val statusChipBackground = when {
                viewModel.updateDownloadInProgress -> Color(0xFFFFF5E6)
                viewModel.updateCheckInProgress -> Color(0xFFEAF0FA)
                viewModel.updateAvailable -> Color(0xFFEAF2FF)
                else -> Color(0xFFE8F5E9)
            }

            androidx.compose.material3.Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFF9FBFF),
                border = BorderStroke(1.dp, LegalNavyPrimary.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(LegalNavyPrimary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Sync, null, tint = LegalNavyPrimary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "التحديث المباشر من GitHub",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = LegalNavyPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "يفحص النسخة على الريبو ويعرض ملاحظات الإصدار قبل التنزيل.",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(statusChipBackground)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                statusChipText,
                                fontSize = 11.sp,
                                color = statusChipColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(color = LegalNavyPrimary.copy(alpha = 0.08f))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("النسخة المثبتة", fontSize = 11.sp, color = Color.Gray)
                            Text(installedVersionLabel, fontWeight = FontWeight.Bold, color = LegalNavyPrimary, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("النسخة المعروضة", fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                            Text(remoteVersionLabel, fontWeight = FontWeight.Bold, color = if (viewModel.updateAvailable) LegalNavyPrimary else Color.DarkGray, fontSize = 13.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                        }
                    }

                    Text(
                        "مصدر التحديث: ${BuildConfig.UPDATE_MANIFEST_URL}",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    viewModel.updateStatusMessage?.let {
                        androidx.compose.material3.Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.White,
                            border = BorderStroke(1.dp, LegalNavyPrimary.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = it,
                                modifier = Modifier.padding(12.dp),
                                fontSize = 12.sp,
                                color = Color.DarkGray,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    if (viewModel.updateAvailable && !viewModel.updateRemoteReleaseTitle.isNullOrBlank()) {
                        Text(
                            "عنوان الإصدار: ${viewModel.updateRemoteReleaseTitle}",
                            fontSize = 12.sp,
                            color = LegalNavyPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (viewModel.updateAvailable && viewModel.updateRemoteReleaseNotes.isNotBlank()) {
                        androidx.compose.material3.Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.White,
                            border = BorderStroke(1.dp, LegalNavyPrimary.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("ملاحظات الإصدار", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LegalNavyPrimary)
                                HorizontalDivider(color = LegalGrayLight)
                                Text(
                                    text = viewModel.updateRemoteReleaseNotes.trim(),
                                    fontSize = 11.sp,
                                    color = Color(0xFF4A5568),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    if (viewModel.updateDownloadInProgress) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            LinearProgressIndicator(
                                progress = { viewModel.updateDownloadProgress.coerceIn(0, 100) / 100f },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                "جارٍ تنزيل ملف التحديث: ${viewModel.updateDownloadProgress}%",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.checkForAppUpdate() },
                            enabled = !viewModel.updateCheckInProgress && !viewModel.updateDownloadInProgress,
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Sync, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (viewModel.updateCheckInProgress) "جارٍ الفحص" else "فحص")
                        }

                        if (viewModel.updateAvailable) {
                            Button(
                                onClick = { viewModel.downloadAndInstallAppUpdate() },
                                enabled = !viewModel.updateDownloadInProgress,
                                modifier = Modifier.weight(1.35f).height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LegalGoldSecondary,
                                    contentColor = LegalNavyPrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.FileUpload, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (viewModel.updateDownloadInProgress) "جارٍ التنزيل" else "تنزيل وتثبيت",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/3bud-ZC/Mohamy/releases"))) },
                                modifier = Modifier.weight(1.35f).height(48.dp)
                            ) {
                                Icon(Icons.Default.Language, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("إصدارات GitHub")
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (viewModel.downloadedUpdateFilePath != null) {
                            OutlinedButton(
                                onClick = { viewModel.installDownloadedUpdate() },
                                modifier = Modifier.weight(1f).height(46.dp)
                            ) {
                                Icon(Icons.Default.PowerSettingsNew, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("تثبيت الملف")
                            }
                        }
                        OutlinedButton(
                            onClick = { viewModel.clearUpdateState() },
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Text("مسح الحالة")
                        }
                    }
                }
            }
        }

        SettingsSectionCard(title = "إشعارات الهاتف") {
            val notificationsEnabled = AppNotificationManager.hasNotificationPermission(context)
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (notificationsEnabled) "الإشعارات مفعلة" else "الإشعارات غير مفعلة",
                        fontWeight = FontWeight.Bold,
                        color = LegalNavyPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "يستخدم التطبيق الإشعارات لتنبيهك عند وجود تحديث جديد، وحفظ مستند، وإنشاء نسخة احتياطية.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        lineHeight = 16.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                if (notificationsEnabled) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFFE8F5E9))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("مفعلة", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                } else {
                    Button(
                        onClick = { onRequestNotificationPermission?.invoke() },
                        enabled = onRequestNotificationPermission != null,
                        colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("تفعيل")
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = LegalGrayLight),
            border = BorderStroke(1.dp, LegalNavyPrimary.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("الخصوصية", fontWeight = FontWeight.Bold, color = LegalNavyPrimary)
                Text("• البيانات تبقى على الهاتف فقط.", fontSize = 12.sp)
                Text("• الاتصال الشبكي يستخدم فقط لتنشيط الترخيص والتحقق الدوري عند الحاجة.", fontSize = 12.sp)
                Text(
                    when {
                        cloudConfigured && cloudAssistantEnabled ->
                            "• عند استخدام أوامر المساعد قد يُرسل التطبيق ملخصاً نصياً مشتقاً من بيانات القضية إلى خدمة AI السحابية المهيأة."
                        cloudConfigured ->
                            "• التحسين السحابي للمساعد معطل حالياً، لذلك لا يتم إرسال ملخصات القضايا إلى أي خدمة AI."
                        else ->
                            "• لا توجد خدمة AI سحابية مهيأة داخل هذه النسخة."
                    },
                    fontSize = 12.sp
                )
                Text("• الملفات الأصلية وقاعدة البيانات المحلية لا يتم رفعها من التطبيق.", fontSize = 12.sp)
            }
        }

        SettingsSectionCard(title = "العلامة والحقوق") {
            Text("محامي فون - نظام تشغيل محلي لإدارة القضايا والموكلين والمستندات.", fontSize = 13.sp, color = Color.DarkGray)
            Spacer(modifier = Modifier.height(4.dp))
            Text("حقوق المهندس عبدالله ال علي", fontSize = 13.sp, color = LegalNavyPrimary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("الموقع الرسمي: abud.fun", fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://abud.fun"))) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Language, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("فتح موقع abud.fun")
            }
        }

        Button(
            onClick = { viewModel.logout() },
            modifier = Modifier.fillMaxWidth().height(55.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFF0F0))
        ) {
            Icon(Icons.Default.PowerSettingsNew, null, tint = Color.Red)
            Spacer(modifier = Modifier.width(8.dp))
            Text("تسجيل الخروج وحفظ مساحة الحساب", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Text(
            "حول التطبيق\nversionName=${BuildConfig.VERSION_NAME}\nbuildTime=${BuildConfig.BUILD_TIMESTAMP}",
            fontSize = 11.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun SettingsSectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, LegalNavyPrimary.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = title, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = LegalNavyPrimary, modifier = Modifier.padding(bottom = 6.dp))
            content()
        }
    }
}

@Composable
private fun SettingsActionButton(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    containerColor: Color = LegalNavyPrimary,
    contentColor: Color = Color.White
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor)
    ) {
        Icon(icon, null, tint = contentColor)
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, color = contentColor, fontWeight = FontWeight.Bold)
    }
}
