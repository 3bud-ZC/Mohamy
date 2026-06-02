package com.example.ui.screens

import com.example.BuildConfig
import com.example.data.AppViewModel
import com.example.data.LicenseCache
import com.example.data.Screen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LegalGoldLight
import com.example.ui.theme.LegalGoldSecondary
import com.example.ui.theme.LegalGrayLight
import com.example.ui.theme.LegalNavyPrimary

@Composable
fun SettingsScreen(viewModel: AppViewModel, license: LicenseCache?) {
    val statusLabel = license?.status ?: "غير نشط"
    val statusColor = if (statusLabel == "نشط") Color(0xFF2E7D32) else Color(0xFFB00020)
    val statusBackground = if (statusLabel == "نشط") Color(0xFFE8F5E9) else Color(0xFFFFEBEE)

    var lawyerName by remember(license?.lawyerName) { mutableStateOf(license?.lawyerName ?: "") }
    var officeName by remember(license?.officeName) { mutableStateOf(license?.officeName ?: "") }
    var phone by remember(license?.phone) { mutableStateOf(license?.phone ?: "") }
    var barNumber by remember(license?.barNumber) { mutableStateOf(license?.barNumber ?: "") }
    var serverUrl by remember(viewModel.licenseServerUrlInput) { mutableStateOf(viewModel.licenseServerUrlInput) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "الملف الشخصي وإعدادات التطبيق",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = LegalNavyPrimary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = LegalNavyPrimary)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(LegalGoldSecondary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AccountBalance, null, tint = LegalGoldLight, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = if (lawyerName.isBlank()) "الأستاذ المحامي" else lawyerName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (officeName.isBlank()) "مكتب المحاماة والخدمات القانونية" else officeName,
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(16.dp))

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

        Spacer(modifier = Modifier.height(16.dp))
        Text("بيانات المحامي والمكتب", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = LegalNavyPrimary)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(value = lawyerName, onValueChange = { lawyerName = it }, label = { Text("اسم المحامي") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = officeName, onValueChange = { officeName = it }, label = { Text("اسم المكتب") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("هاتف المكتب") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = barNumber, onValueChange = { barNumber = it }, label = { Text("رقم النقابة") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { viewModel.saveLawOfficeProfile(lawyerName, officeName, phone, barNumber) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary)
        ) {
            Icon(Icons.Default.Save, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("حفظ البيانات محلياً", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("تراخيص وتفعيل النظام المحلية", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = LegalNavyPrimary)
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VerifiedUser, null, tint = LegalNavyPrimary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("حالة الترخيص الحالية:", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .background(statusBackground, shape = RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(statusLabel, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = LegalGrayLight)
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VpnKey, null, tint = LegalNavyPrimary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("رمز الحساب:", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(license?.username ?: "غير متاح", fontSize = 13.sp, color = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = serverUrl,
            onValueChange = { serverUrl = it },
            label = { Text("رابط سيرفر التراخيص") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "الرابط الافتراضي الموصى به: https://mohamy.abud.fun",
            fontSize = 12.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { viewModel.saveLicenseServerUrl(serverUrl) },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary)
        ) {
            Text("حفظ رابط السيرفر")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { viewModel.navigateTo(Screen.SmartAssistant) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary)
        ) {
            Icon(Icons.Default.AutoAwesome, null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("فتح المساعد الذكي", color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = { viewModel.navigateTo(Screen.ImportData) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary)
        ) {
            Icon(Icons.Default.FileUpload, null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("استيراد بيانات", color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = { viewModel.navigateTo(Screen.BackupRestore) },
            modifier = Modifier.fillMaxWidth().height(55.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LegalGoldSecondary)
        ) {
            Icon(Icons.Default.Sync, null, tint = LegalNavyPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("إدارة النسخ الاحتياطي", color = LegalNavyPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LegalGrayLight)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("الخصوصية", fontWeight = FontWeight.Bold, color = LegalNavyPrimary)
                Text("• البيانات تبقى على الهاتف فقط.", fontSize = 12.sp)
                Text("• لا يتم إرسال القضايا أو الملفات أو المستندات لأي خادم.", fontSize = 12.sp)
                Text("• الاتصال الشبكي يستخدم فقط لتنشيط الترخيص والتحقق الدوري عند الحاجة.", fontSize = 12.sp)
                Text("• لا توجد مزودات AI سحابية داخل التطبيق.", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = { viewModel.logout() },
            modifier = Modifier.fillMaxWidth().height(55.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFF0F0))
        ) {
            Icon(Icons.Default.PowerSettingsNew, null, tint = Color.Red)
            Spacer(modifier = Modifier.width(8.dp))
            Text("تسجيل الخروج", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
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
