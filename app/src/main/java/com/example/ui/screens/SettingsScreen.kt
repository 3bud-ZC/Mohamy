package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.AppNotificationManager
import com.example.data.AppViewModel
import com.example.data.LicenseCache
import com.example.data.Screen
import com.example.ui.components.MohamyBadgeTone
import com.example.ui.components.MohamyButton
import com.example.ui.components.MohamyButtonStyle
import com.example.ui.components.MohamyStatusBadge
import com.example.ui.components.SettingsActionDivider
import com.example.ui.components.SettingsActionRow
import com.example.ui.components.SettingsSection
import com.example.ui.theme.MohamyBlackSoft
import com.example.ui.theme.MohamyCharcoalSoft
import com.example.ui.theme.MohamyDimens
import com.example.ui.theme.MohamyGold
import com.example.ui.theme.MohamyGoldStrong
import com.example.ui.theme.MohamyLightHero
import com.example.ui.theme.MohamyLightHeroEnd
import com.example.ui.theme.legalScreenBackground

@Composable
fun SettingsScreen(
    viewModel: AppViewModel,
    license: LicenseCache?,
    onRequestNotificationPermission: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val clients by viewModel.allClients.collectAsState(initial = emptyList())
    val activeCases by viewModel.allCases.collectAsState(initial = emptyList())
    val archivedCases by viewModel.archivedCases.collectAsState(initial = emptyList())
    val sessions by viewModel.allSessions.collectAsState(initial = emptyList())
    val tasks by viewModel.allTasks.collectAsState(initial = emptyList())
    val files by viewModel.allFiles.collectAsState(initial = emptyList())
    val statusLabel = license?.status ?: "غير نشط"
    val cloudConfigured = viewModel.hasConfiguredCloudAssistant
    val notificationsEnabled = AppNotificationManager.hasNotificationPermission(context)
    val hasWorkspaceData =
        clients.isNotEmpty() ||
            activeCases.isNotEmpty() ||
            archivedCases.isNotEmpty() ||
            sessions.isNotEmpty() ||
            tasks.isNotEmpty() ||
            files.isNotEmpty()

    var lawyerName by remember(license?.lawyerName) { mutableStateOf(license?.lawyerName ?: "") }
    var officeName by remember(license?.officeName) { mutableStateOf(license?.officeName ?: "") }
    var phone by remember(license?.phone) { mutableStateOf(license?.phone ?: "") }
    var barNumber by remember(license?.barNumber) { mutableStateOf(license?.barNumber ?: "") }
    var serverUrl by remember(viewModel.licenseServerUrlInput) { mutableStateOf(viewModel.licenseServerUrlInput) }
    var cloudAssistantEnabled by remember(viewModel.isCloudAssistantEnabled) { mutableStateOf(viewModel.isCloudAssistantEnabled) }
    var showDemoSeedDialog by remember { mutableStateOf(false) }

    if (showDemoSeedDialog) {
        AlertDialog(
            onDismissRequest = { showDemoSeedDialog = false },
            title = { Text("إضافة بيانات تجريبية") },
            text = {
                Text("سيتم إدراج بيانات عرض وهمية داخل هذه المساحة المحلية دون حذف أو استبدال بياناتك الحالية.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDemoSeedDialog = false
                        viewModel.seedDemoWorkspace(forceAppend = true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("إضافة البيانات")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDemoSeedDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .legalScreenBackground()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MohamyDimens.screenHorizontal, vertical = MohamyDimens.screenVertical),
        verticalArrangement = Arrangement.spacedBy(MohamyDimens.sectionGap)
    ) {
        SettingsHeroCard(
            lawyerName = lawyerName,
            officeName = officeName,
            barNumber = barNumber,
            phone = phone,
            statusLabel = statusLabel
        )

        SettingsSection(
            title = "الحساب والترخيص",
            subtitle = "بيانات المكتب المحلي ورابط التفعيل وحالة الحساب الحالية.",
            icon = Icons.Default.VerifiedUser
        ) {
            SettingsActionRow(
                title = "حالة الترخيص",
                subtitle = "التفعيل المحلي الحالي لهذه النسخة",
                icon = Icons.Default.Shield,
                badgeText = statusLabel,
                badgeTone = licenseStatusTone(statusLabel)
            )
            Spacer(modifier = Modifier.height(10.dp))
            SettingsActionRow(
                title = "رمز الحساب",
                subtitle = license?.username ?: "غير متاح حالياً",
                icon = Icons.Default.VpnKey
            )
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = lawyerName,
                onValueChange = { lawyerName = it },
                label = { Text("اسم المحامي") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = mohamyOutlinedTextColors()
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = officeName,
                onValueChange = { officeName = it },
                label = { Text("اسم المكتب") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = mohamyOutlinedTextColors()
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("هاتف المكتب") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = mohamyOutlinedTextColors()
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = barNumber,
                onValueChange = { barNumber = it },
                label = { Text("رقم النقابة") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = mohamyOutlinedTextColors()
            )
            Spacer(modifier = Modifier.height(12.dp))
            MohamyButton(
                text = "حفظ بيانات المكتب محلياً",
                icon = Icons.Default.Save,
                modifier = Modifier.fillMaxWidth(),
                onClick = { viewModel.saveLawOfficeProfile(lawyerName, officeName, phone, barNumber) }
            )

            Spacer(modifier = Modifier.height(16.dp))
            SettingsActionDivider()
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = { Text("رابط سيرفر التراخيص") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = mohamyOutlinedTextColors()
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "الرابط الافتراضي الموصى به: https://mohamy.abud.fun",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            MohamyButton(
                text = "حفظ رابط السيرفر",
                icon = Icons.Default.Settings,
                modifier = Modifier.fillMaxWidth(),
                style = MohamyButtonStyle.Secondary,
                onClick = { viewModel.saveLicenseServerUrl(serverUrl) }
            )
        }

        SettingsSection(
            title = "التحديثات",
            subtitle = "فحص النسخة المنشورة من GitHub دون المساس بإعدادات التوقيع أو ملفات النشر.",
            icon = Icons.Default.Sync
        ) {
            SettingsActionRow(
                title = "النسخة المثبتة",
                subtitle = "${BuildConfig.VERSION_NAME} · code ${BuildConfig.VERSION_CODE}",
                icon = Icons.Default.Info,
                badgeText = updateBadgeText(viewModel),
                badgeTone = updateBadgeTone(viewModel)
            )
            Spacer(modifier = Modifier.height(10.dp))
            SettingsActionRow(
                title = "النسخة المعروضة",
                subtitle = remoteVersionLabel(viewModel),
                icon = Icons.Default.CloudUpload
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "مصدر التحديث: ${BuildConfig.UPDATE_MANIFEST_URL}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            viewModel.updateStatusMessage?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                    )
                ) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(14.dp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }

            if (viewModel.updateAvailable && !viewModel.updateRemoteReleaseTitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "عنوان الإصدار: ${viewModel.updateRemoteReleaseTitle}",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            if (viewModel.updateAvailable && viewModel.updateRemoteReleaseNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("ملاحظات الإصدار", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Text(
                            viewModel.updateRemoteReleaseNotes.trim(),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            if (viewModel.updateDownloadInProgress) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { viewModel.updateDownloadProgress.coerceIn(0, 100) / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "جارٍ تنزيل ملف التحديث: ${viewModel.updateDownloadProgress}%",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MohamyButton(
                    text = if (viewModel.updateCheckInProgress) "جارٍ الفحص" else "فحص التحديث",
                    icon = Icons.Default.Sync,
                    modifier = Modifier.weight(1f),
                    enabled = !viewModel.updateCheckInProgress && !viewModel.updateDownloadInProgress,
                    onClick = { viewModel.checkForAppUpdate() }
                )
                if (viewModel.updateAvailable) {
                    MohamyButton(
                        text = if (viewModel.updateDownloadInProgress) "جارٍ التنزيل" else "تنزيل وتثبيت",
                        icon = Icons.Default.FileUpload,
                        modifier = Modifier.weight(1f),
                        enabled = !viewModel.updateDownloadInProgress,
                        style = MohamyButtonStyle.Secondary,
                        onClick = { viewModel.downloadAndInstallAppUpdate() }
                    )
                } else {
                    MohamyButton(
                        text = "إصدارات GitHub",
                        icon = Icons.Default.Language,
                        modifier = Modifier.weight(1f),
                        style = MohamyButtonStyle.Ghost,
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/3bud-ZC/Mohamy/releases")))
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (viewModel.downloadedUpdateFilePath != null) {
                    MohamyButton(
                        text = "تثبيت الملف",
                        icon = Icons.Default.PowerSettingsNew,
                        modifier = Modifier.weight(1f),
                        style = MohamyButtonStyle.Ghost,
                        onClick = { viewModel.installDownloadedUpdate() }
                    )
                }
                MohamyButton(
                    text = "مسح الحالة",
                    modifier = Modifier.weight(1f),
                    style = MohamyButtonStyle.Ghost,
                    onClick = { viewModel.clearUpdateState() }
                )
            }
        }

        SettingsSection(
            title = "النسخ والبيانات",
            subtitle = "النسخ الاحتياطي والاستيراد والإشعارات اليومية الخاصة بالجلسات والمهام.",
            icon = Icons.Default.Backup
        ) {
            SettingsActionRow(
                title = "إدارة النسخ الاحتياطي",
                subtitle = "إنشاء نسخة محلية أو استعادة قاعدة البيانات من شاشة مخصصة",
                icon = Icons.Default.Backup,
                onClick = { viewModel.navigateTo(Screen.BackupRestore) }
            )
            Spacer(modifier = Modifier.height(10.dp))
            SettingsActionRow(
                title = "استيراد بيانات",
                subtitle = "نقل بيانات قانونية أو أرشيف قديم إلى التطبيق المحلي",
                icon = Icons.Default.DataObject,
                onClick = { viewModel.navigateTo(Screen.ImportData) }
            )
            Spacer(modifier = Modifier.height(10.dp))
            SettingsActionRow(
                title = if (viewModel.hasDemoSeededForCurrentWorkspace) "فتح العرض التجريبي" else "إضافة بيانات تجريبية",
                subtitle =
                    if (viewModel.hasDemoSeededForCurrentWorkspace) {
                        "تم إنشاء العرض التجريبي مسبقاً داخل هذه المساحة"
                    } else if (hasWorkspaceData) {
                        "لن يتم حذف بياناتك الحالية"
                    } else {
                        "ينشئ عرضاً محلياً آمناً ببيانات وهمية واضحة"
                    },
                icon = Icons.Default.AutoAwesome,
                badgeText = "محلي فقط",
                badgeTone = MohamyBadgeTone.Gold,
                onClick = {
                    when {
                        viewModel.hasDemoSeededForCurrentWorkspace -> viewModel.navigateTo(Screen.CasesList)
                        hasWorkspaceData -> showDemoSeedDialog = true
                        else -> viewModel.seedDemoWorkspace()
                    }
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
            SettingsActionRow(
                title = "إعادة عرض بطاقة الترحيب",
                subtitle = "مفيد لتقديم التطبيق للمحامين أو مراجعة خطوات البداية",
                icon = Icons.AutoMirrored.Filled.Redo,
                onClick = { viewModel.reopenOnboarding() }
            )
            Spacer(modifier = Modifier.height(10.dp))
            SettingsActionRow(
                title = if (notificationsEnabled) "إشعارات الهاتف مفعلة" else "إشعارات الهاتف غير مفعلة",
                subtitle = "تنبيهات للتحديثات والمستندات والنسخ الاحتياطي",
                icon = Icons.Default.Notifications,
                badgeText = if (notificationsEnabled) "مفعلة" else "غير مفعلة",
                badgeTone = if (notificationsEnabled) MohamyBadgeTone.Success else MohamyBadgeTone.Danger,
                trailing =
                    if (notificationsEnabled) {
                        null
                    } else {
                        {
                            Button(
                                onClick = { onRequestNotificationPermission?.invoke() },
                                enabled = onRequestNotificationPermission != null,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("تفعيل")
                            }
                        }
                    }
            )
            Spacer(modifier = Modifier.height(10.dp))
            SettingsActionRow(
                title = "تذكير بالجلسات والمهام",
                subtitle = "فحص يومي للجلسات القريبة والمهام المتأخرة والتنبيهات المهمة",
                icon = Icons.Default.Notifications,
                trailing = {
                    Switch(
                        checked = viewModel.isRemindersEnabled,
                        onCheckedChange = { viewModel.updateRemindersEnabled(it) }
                    )
                }
            )
        }

        SettingsSection(
            title = "المظهر والمساعد",
            subtitle = "الواجهة الداكنة والمساعد المحلي والتحسين السحابي الاختياري إن كان مهيأً.",
            icon = Icons.Default.Palette
        ) {
            SettingsActionRow(
                title = "الوضع الليلي الاحترافي",
                subtitle =
                    if (viewModel.isDarkThemeEnabled) {
                        "مفعل حالياً مع ألوان أكثر عمقاً وتبايناً."
                    } else {
                        "يمكن تفعيله لإظهار الواجهة الداكنة المعتمدة في إعادة البناء."
                    },
                icon = Icons.Default.Palette,
                trailing = {
                    Switch(
                        checked = viewModel.isDarkThemeEnabled,
                        onCheckedChange = { viewModel.updateDarkThemeEnabled(it) }
                    )
                }
            )
            if (cloudConfigured) {
                Spacer(modifier = Modifier.height(10.dp))
                SettingsActionRow(
                    title = "التحسين السحابي الاختياري",
                    subtitle =
                        if (cloudAssistantEnabled) {
                            "المساعد يبني الرد محلياً ثم قد يضيف تحسيناً سحابياً عند الحاجة."
                        } else {
                            "كل أوامر المساعد تعمل محلياً فقط داخل الجهاز."
                        },
                    icon = Icons.Default.AutoAwesome,
                    trailing = {
                        Switch(
                            checked = cloudAssistantEnabled,
                            onCheckedChange = {
                                cloudAssistantEnabled = it
                                viewModel.updateCloudAssistantEnabled(it)
                            }
                        )
                    }
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            SettingsActionRow(
                title = "فتح المساعد الذكي",
                subtitle = "الوصول السريع إلى الأدوات المحلية المساعدة للقضايا والمستندات",
                icon = Icons.Default.AutoAwesome,
                onClick = { viewModel.navigateTo(Screen.SmartAssistant) }
            )
        }

        SettingsSection(
            title = "البيانات والأمان",
            subtitle = "ملخص الخصوصية والاستخدام الشبكي الحالي لهذه النسخة المحلية.",
            icon = Icons.Default.Security
        ) {
            PrivacyBullet("البيانات تبقى على الهاتف فقط.")
            PrivacyBullet("الاتصال الشبكي يستخدم فقط لتنشيط الترخيص والتحقق من التحديثات عند الحاجة.")
            PrivacyBullet(
                when {
                    cloudConfigured && cloudAssistantEnabled ->
                        "عند استخدام أوامر المساعد قد يُرسل التطبيق ملخصاً نصياً مشتقاً من بيانات القضية إلى الخدمة السحابية المهيأة."
                    cloudConfigured ->
                        "التحسين السحابي للمساعد معطل حالياً، لذلك لا يتم إرسال ملخصات القضايا إلى أي خدمة AI."
                    else ->
                        "لا توجد خدمة AI سحابية مهيأة داخل هذه النسخة."
                }
            )
            PrivacyBullet("الملفات الأصلية وقاعدة البيانات المحلية لا يتم رفعها من التطبيق.")
        }

        SettingsSection(
            title = "معلومات التطبيق",
            subtitle = "الهوية التجارية وبيانات النسخة الحالية داخل الجهاز.",
            icon = Icons.Default.AccountBalance
        ) {
            SettingsActionRow(
                title = "محامي فون",
                subtitle = "نظام تشغيل محلي لإدارة القضايا والموكلين والمستندات.",
                icon = Icons.Default.Badge
            )
            Spacer(modifier = Modifier.height(10.dp))
            SettingsActionRow(
                title = "المطور",
                subtitle = "حقوق المهندس عبدالله ال علي",
                icon = Icons.Default.VerifiedUser
            )
            Spacer(modifier = Modifier.height(10.dp))
            SettingsActionRow(
                title = "الموقع الرسمي",
                subtitle = "abud.fun",
                icon = Icons.Default.Language,
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://abud.fun"))) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            ) {
                Text(
                    text = "versionName=${BuildConfig.VERSION_NAME}\nversionCode=${BuildConfig.VERSION_CODE}\nbuildTime=${BuildConfig.BUILD_TIMESTAMP}",
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }

        SettingsSection(
            title = "الصيانة والخروج",
            subtitle = "إجراءات واضحة للمستخدم دون تغيير أي منطق إصدار أو نشر.",
            icon = Icons.Default.PowerSettingsNew
        ) {
            SettingsActionRow(
                title = "تسجيل الخروج",
                subtitle = "يحفظ مساحة الحساب الحالية محلياً ثم يعيدك إلى شاشة التفعيل",
                icon = Icons.Default.PowerSettingsNew,
                badgeText = "تنبيه",
                badgeTone = MohamyBadgeTone.Danger,
                onClick = { viewModel.logout() }
            )
        }
    }
}

@Composable
private fun SettingsHeroCard(
    lawyerName: String,
    officeName: String,
    barNumber: String,
    phone: String,
    statusLabel: String
) {
    val dark = isSystemInDarkTheme()
    val heroGradient =
        if (dark) {
            Brush.verticalGradient(
                listOf(MohamyBlackSoft, MohamyCharcoalSoft, MaterialTheme.colorScheme.surface)
            )
        } else {
            Brush.verticalGradient(
                listOf(MohamyLightHero, MohamyLightHeroEnd, MaterialTheme.colorScheme.surface)
            )
        }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MohamyDimens.largeCardRadius),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = if (dark) 12.dp else 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(heroGradient)
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MohamyGold.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = MohamyGold,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (lawyerName.isBlank()) "الأستاذ المحامي" else lawyerName,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp
                            )
                            Text(
                                text = if (officeName.isBlank()) "مكتب المحاماة والخدمات القانونية" else officeName,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }
                    MohamyStatusBadge(text = statusLabel, tone = licenseStatusTone(statusLabel))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HeroMiniInfo("رقم النقابة", barNumber.ifBlank { "غير معروف" }, Modifier.weight(1f))
                    HeroMiniInfo("هاتف المكتب", phone.ifBlank { "لا يوجد" }, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HeroMiniInfo(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            Text(value, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun PrivacyBullet(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text("•", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun mohamyOutlinedTextColors() =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        cursorColor = MaterialTheme.colorScheme.primary
    )

private fun licenseStatusTone(statusLabel: String): MohamyBadgeTone =
    when (statusLabel.trim()) {
        "نشط" -> MohamyBadgeTone.Success
        "غير نشط", "موقوف", "منتهي" -> MohamyBadgeTone.Danger
        else -> MohamyBadgeTone.Gold
    }

private fun updateBadgeText(viewModel: AppViewModel): String =
    when {
        viewModel.updateDownloadInProgress -> "جارٍ التنزيل"
        viewModel.updateCheckInProgress -> "جارٍ الفحص"
        viewModel.updateAvailable -> "تحديث متاح"
        else -> "محدّث"
    }

private fun updateBadgeTone(viewModel: AppViewModel): MohamyBadgeTone =
    when {
        viewModel.updateDownloadInProgress -> MohamyBadgeTone.Gold
        viewModel.updateCheckInProgress -> MohamyBadgeTone.Neutral
        viewModel.updateAvailable -> MohamyBadgeTone.Gold
        else -> MohamyBadgeTone.Success
    }

private fun remoteVersionLabel(viewModel: AppViewModel): String =
    when {
        viewModel.updateAvailable && !viewModel.updateRemoteVersionName.isNullOrBlank() ->
            "${viewModel.updateRemoteVersionName} · code ${viewModel.updateRemoteVersionCode}"
        viewModel.updateAvailable ->
            "code ${viewModel.updateRemoteVersionCode}"
        else -> "لا توجد نسخة جديدة"
    }
