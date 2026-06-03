package com.example.ui.screens
import com.example.data.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.saveable.rememberSaveable

import com.example.ui.theme.LegalGoldSecondary
import com.example.ui.theme.LegalGoldLight
import com.example.ui.theme.LegalNavyPrimary

@Composable
fun ActivationScreen(viewModel: AppViewModel) {
    var agreementChecked by rememberSaveable { mutableStateOf(true) }
    val canSubmit = agreementChecked &&
        viewModel.usernameInput.isNotBlank() &&
        viewModel.LicenseCodeInput.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFF4F7FC), Color.White)
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = LegalNavyPrimary)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Default.Gavel,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier
                        .size(140.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 18.dp, y = (-20).dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(LegalGoldSecondary.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = null,
                            tint = LegalGoldLight,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تنشيط محامي فون",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 21.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "ترخيص محلي مشفر للحساب على هذا الجهاز فقط.",
                            color = Color.White.copy(alpha = 0.86f),
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, LegalNavyPrimary.copy(alpha = 0.08f)),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "بيانات تسجيل الدخول",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = LegalNavyPrimary
                )
                Text(
                    text = "استخدم بيانات الحساب المسجلة من لوحة إدارة النظام لبدء مساحة العمل الخاصة بك.",
                    fontSize = 13.sp,
                    color = Color(0xFF4B5563),
                    lineHeight = 20.sp
                )

                ActivationHintRow(
                    icon = Icons.Default.Shield,
                    text = "كل القضايا والملفات تبقى داخل الهاتف ولا تنتقل للسحابة."
                )
                ActivationHintRow(
                    icon = Icons.Default.Lock,
                    text = "كل حساب يفتح مساحة بيانات مستقلة عن أي حساب آخر."
                )

                Spacer(modifier = Modifier.height(2.dp))
                OutlinedTextField(
                    value = viewModel.usernameInput,
                    onValueChange = { viewModel.usernameInput = it },
                    label = { Text("اسم المستخدم / البريد الإلكتروني") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("username_input"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = viewModel.LicenseCodeInput,
                    onValueChange = { viewModel.LicenseCodeInput = it },
                    label = { Text("كلمة المرور") },
                    leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("activation_input"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Checkbox(checked = agreementChecked, onCheckedChange = { agreementChecked = it })
                    Text(
                        text = "أوافق أن بيانات مكتبي وموكلي تظل محلية وخاصة 100% على هذا الجهاز.",
                        fontSize = 12.sp,
                        color = Color(0xFF374151),
                        lineHeight = 18.sp,
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .weight(1f)
                    )
                }

                Button(
                    onClick = { viewModel.submitLicenseActivation() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary),
                    shape = RoundedCornerShape(14.dp),
                    enabled = canSubmit
                ) {
                    Text(
                        text = "تفعيل الحساب وربطه بالجهاز",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = LegalGoldSecondary.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, LegalGoldSecondary.copy(alpha = 0.25f))
        ) {
            Text(
                text = "بعد التفعيل الأول، يمكن تشغيل التطبيق بدون إنترنت لمعظم المهام اليومية.",
                fontSize = 12.sp,
                color = LegalNavyPrimary,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun ActivationHintRow(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(LegalNavyPrimary.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LegalNavyPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = text,
            fontSize = 12.sp,
            color = Color(0xFF4B5563),
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
