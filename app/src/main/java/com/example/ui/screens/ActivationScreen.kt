package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.AppViewModel
import com.example.ui.components.MohamyButton
import com.example.ui.components.MohamyButtonStyle
import com.example.ui.components.MohamyCard
import com.example.ui.theme.MohamyBlack
import com.example.ui.theme.MohamyCharcoal
import com.example.ui.theme.MohamyDimens
import com.example.ui.theme.MohamyGold
import com.example.ui.theme.MohamyWarmTextSoft

@Composable
fun ActivationScreen(viewModel: AppViewModel) {
  var agreementChecked by rememberSaveable { mutableStateOf(true) }
  val canSubmit =
    agreementChecked && viewModel.usernameInput.isNotBlank() && viewModel.LicenseCodeInput.isNotBlank()

  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(Brush.verticalGradient(colors = listOf(MohamyBlack, MohamyCharcoal, MohamyBlack)))
        .verticalScroll(rememberScrollState())
        .padding(horizontal = MohamyDimens.screenHorizontal, vertical = MohamyDimens.screenVertical),
    verticalArrangement = Arrangement.spacedBy(MohamyDimens.sectionGap)
  ) {
    MohamyCard {
      Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
          modifier = Modifier.size(74.dp).background(MohamyGold.copy(alpha = 0.12f), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.AccountBalance,
            contentDescription = null,
            tint = MohamyGold,
            modifier = Modifier.size(34.dp)
          )
        }
        Text(
          text = "تفعيل حساب المحامي",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onSurface,
          fontWeight = FontWeight.ExtraBold
        )
        Text(
          text = "واجهة دخول قانونية خاصة بمكتبك. البيانات القانونية تبقى محلية على الهاتف، والاتصال بالشبكة يستخدم فقط للتفعيل والتحقق عند الحاجة.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
          LegalActivationHint(icon = Icons.Default.VerifiedUser, text = "تفعيل آمن ومربوط بالحساب")
          LegalActivationHint(icon = Icons.Default.PrivacyTip, text = "خصوصية محلية")
        }
      }
    }

    MohamyCard(title = "الدخول إلى مساحة العمل", subtitle = "استخدم بيانات التفعيل المسجلة من لوحة الإدارة") {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
          value = viewModel.LicenseCodeInput,
          onValueChange = { viewModel.LicenseCodeInput = it },
          modifier = Modifier.fillMaxWidth().testTag("activation_input"),
          shape = RoundedCornerShape(MohamyDimens.buttonRadius),
          label = { Text("كود التفعيل") },
          leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
          singleLine = true,
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

        OutlinedTextField(
          value = viewModel.usernameInput,
          onValueChange = { viewModel.usernameInput = it },
          modifier = Modifier.fillMaxWidth().testTag("username_input"),
          shape = RoundedCornerShape(MohamyDimens.buttonRadius),
          label = { Text("رقم الهاتف أو البريد") },
          leadingIcon = { Icon(Icons.Default.PhoneIphone, contentDescription = null) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
          singleLine = true,
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.Top
        ) {
          Checkbox(checked = agreementChecked, onCheckedChange = { agreementChecked = it })
          Text(
            text = "أوافق على إبقاء بيانات القضايا والموكلين والمستندات داخل التخزين المحلي للتطبيق على هذا الجهاز.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp)
          )
        }

        MohamyButton(
          text = "تفعيل التطبيق",
          onClick = { viewModel.submitLicenseActivation() },
          modifier = Modifier.fillMaxWidth().testTag("submit_button"),
          enabled = canSubmit
        )
      }
    }

    MohamyCard {
      Text(
        text = "ملاحظة: بعد التفعيل الأول، يمكن تشغيل التطبيق بدون إنترنت لمعظم أعمال المكتب اليومية مع الحفاظ على الطابع المحلي للبيانات.",
        style = MaterialTheme.typography.bodyMedium,
        color = MohamyWarmTextSoft,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
      )
    }
  }
}

@Composable
private fun LegalActivationHint(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
  Row(
    modifier =
      Modifier.background(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
        RoundedCornerShape(16.dp)
      ).padding(horizontal = 12.dp, vertical = 10.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(imageVector = icon, contentDescription = null, tint = MohamyGold, modifier = Modifier.size(18.dp))
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = Color.White)
  }
}
