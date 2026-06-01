package com.example.ui.screens
import com.example.data.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.example.ui.theme.LegalGoldSecondary
import com.example.ui.theme.LegalNavyPrimary

@Composable
fun ActivationScreen(viewModel: AppViewModel) {
    var passwordInput by remember { mutableStateOf("") }
    var agreementChecked by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Gavel,
            contentDescription = "شعار",
            tint = LegalNavyPrimary,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "تنشيط تطبيق محامي فون",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = LegalNavyPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "أدخل بيانات حسابك المسجل والمسلم من لوحة تحكم الإدارة لتنشيط رخصة التشغيل المحلية المشفرة لجهازك.",
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = viewModel.usernameInput,
            onValueChange = { viewModel.usernameInput = it },
            label = { Text("اسم المستخدم / البريد الإلكتروني") },
            leadingIcon = { Icon(Icons.Default.Person, null) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("username_input"),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = viewModel.LicenseCodeInput,
            onValueChange = { viewModel.LicenseCodeInput = it },
            label = { Text("كلمة المرور") },
            leadingIcon = { Icon(Icons.Default.VpnKey, null) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("activation_input"),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Checkbox(checked = agreementChecked, onCheckedChange = { agreementChecked = it })
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "أوافق على أن كافة ملفاتي وقضايا موكلي خصوصية 100% ولا يتم مشاركتها أو تسريبها لسحابة الإنترنت.",
                fontSize = 11.sp,
                color = Color.DarkGray
            )
        }

        Button(
            onClick = { viewModel.submitLicenseActivation() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("submit_button"),
            colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary),
            shape = RoundedCornerShape(12.dp),
            enabled = agreementChecked && viewModel.usernameInput.isNotEmpty() && viewModel.LicenseCodeInput.isNotEmpty()
        ) {
            Text("تفعيل الحساب وربطه بالجهاز", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "ملاحظة: الترخيص المحلي يعمل دون إنترنت بعد التنشيط الأول للتحكم والفلترة بخصوصيتك التامة.",
            fontSize = 11.sp,
            color = LegalGoldSecondary,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold
        )
    }
}
