package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.AppViewModel
import com.example.ui.components.MohamyButton
import com.example.ui.components.MohamyCard
import com.example.ui.theme.MohamyBlack
import com.example.ui.theme.MohamyCharcoal
import com.example.ui.theme.MohamyDimens
import com.example.ui.theme.MohamyGold
import com.example.ui.theme.MohamyGoldBright
import com.example.ui.theme.legalScreenBackground

private val ActivationCardBorder = Color(0xFF74603A)
private val ActivationFieldBorder = Color(0xFF8D7743)
private val ActivationFieldFocus = Color(0xFFF0CF68)
private val ActivationHintBackground = Color(0xFF20180D)
private val ActivationHintText = Color(0xFFF3E5B7)
private val ActivationNoteBackground = Color(0xFF261D10)
private val ActivationNoteText = Color(0xFFF5E8C3)
private val ActivationBodyText = Color(0xFF655541)
private val ActivationLabelText = Color(0xFF8A7347)

@Composable
fun ActivationScreen(viewModel: AppViewModel) {
  var agreementChecked by rememberSaveable { mutableStateOf(true) }
  val canSubmit =
    agreementChecked && viewModel.usernameInput.isNotBlank() && viewModel.LicenseCodeInput.isNotBlank()

  Column(
    modifier =
      Modifier.fillMaxSize()
        .legalScreenBackground()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = MohamyDimens.screenHorizontal, vertical = MohamyDimens.screenVertical),
    verticalArrangement = Arrangement.spacedBy(MohamyDimens.sectionGap)
  ) {
    MohamyCard(modifier = Modifier.border(1.dp, ActivationCardBorder, RoundedCornerShape(MohamyDimens.cardRadius))) {
      Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
          modifier = Modifier.size(74.dp).background(MohamyGold.copy(alpha = 0.16f), CircleShape),
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
          text = "واجهة موثوقة لدخول مكتبك القانوني. بيانات القضايا والمستندات تبقى محلية على الهاتف، ويُستخدم الاتصال فقط للتفعيل والتحقق عند الحاجة.",
          style = MaterialTheme.typography.bodyMedium,
          color = ActivationBodyText,
          lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
          LegalActivationHint(icon = Icons.Default.VerifiedUser, text = "ربط مرخّص بالحساب")
          LegalActivationHint(icon = Icons.Default.PrivacyTip, text = "خصوصية محلية")
        }
      }
    }

    MohamyCard(
      modifier = Modifier.border(1.dp, ActivationCardBorder, RoundedCornerShape(MohamyDimens.cardRadius)),
      title = "الدخول إلى مساحة العمل",
      subtitle = "استخدم اسم المستخدم أو رقم الهاتف مع كلمة المرور الصادرة من لوحة الإدارة"
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .background(ActivationNoteBackground, RoundedCornerShape(18.dp))
              .border(1.dp, ActivationFieldBorder, RoundedCornerShape(18.dp))
              .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
          Text(
            text = "مهم: هذا الحقل يطلب كلمة المرور الخاصة بالحساب، وليس license key.",
            style = MaterialTheme.typography.bodySmall,
            color = ActivationNoteText,
            lineHeight = MaterialTheme.typography.bodySmall.lineHeight
          )
        }

        OutlinedTextField(
          value = viewModel.usernameInput,
          onValueChange = { viewModel.usernameInput = it },
          modifier = Modifier.fillMaxWidth().testTag("username_input"),
          shape = RoundedCornerShape(MohamyDimens.buttonRadius),
          label = { Text("اسم المستخدم أو رقم الهاتف") },
          leadingIcon = { Icon(Icons.Default.PhoneIphone, contentDescription = null) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
          singleLine = true,
          colors = activationFieldColors()
        )

        OutlinedTextField(
          value = viewModel.LicenseCodeInput,
          onValueChange = { viewModel.LicenseCodeInput = it },
          modifier = Modifier.fillMaxWidth().testTag("activation_input"),
          shape = RoundedCornerShape(MohamyDimens.buttonRadius),
          label = { Text("كلمة المرور") },
          leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
          visualTransformation = PasswordVisualTransformation(),
          singleLine = true,
          colors = activationFieldColors()
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.Top
        ) {
          Checkbox(
            checked = agreementChecked,
            onCheckedChange = { agreementChecked = it },
            colors =
              CheckboxDefaults.colors(
                checkedColor = ActivationFieldFocus,
                uncheckedColor = ActivationFieldBorder,
                checkmarkColor = MohamyBlack
              )
          )
          Text(
            text = "أوافق على إبقاء بيانات القضايا والموكلين والمستندات داخل التخزين المحلي للتطبيق على هذا الجهاز.",
            style = MaterialTheme.typography.bodySmall,
            color = ActivationBodyText,
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

    MohamyCard(modifier = Modifier.border(1.dp, ActivationCardBorder, RoundedCornerShape(MohamyDimens.cardRadius))) {
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .background(ActivationNoteBackground, RoundedCornerShape(20.dp))
            .border(1.dp, ActivationFieldBorder, RoundedCornerShape(20.dp))
            .padding(16.dp)
      ) {
        Text(
          text = "بعد التفعيل الأول، يمكن تشغيل التطبيق بدون إنترنت لمعظم أعمال المكتب اليومية مع الحفاظ على الطابع المحلي الكامل للبيانات.",
          style = MaterialTheme.typography.bodyMedium,
          color = ActivationNoteText,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth()
        )
      }
    }
  }
}

@Composable
private fun LegalActivationHint(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
  Row(
    modifier =
      Modifier.background(ActivationHintBackground, RoundedCornerShape(16.dp))
        .border(1.dp, ActivationFieldBorder, RoundedCornerShape(16.dp))
        .padding(horizontal = 12.dp, vertical = 10.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(imageVector = icon, contentDescription = null, tint = MohamyGoldBright, modifier = Modifier.size(18.dp))
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = ActivationHintText, fontWeight = FontWeight.SemiBold)
  }
}

@Composable
private fun activationFieldColors() =
  OutlinedTextFieldDefaults.colors(
    focusedBorderColor = ActivationFieldFocus,
    unfocusedBorderColor = ActivationFieldBorder,
    disabledBorderColor = ActivationFieldBorder.copy(alpha = 0.6f),
    focusedLabelColor = ActivationFieldFocus,
    unfocusedLabelColor = ActivationLabelText,
    focusedLeadingIconColor = ActivationFieldFocus,
    unfocusedLeadingIconColor = MohamyGoldBright,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    cursorColor = ActivationFieldFocus,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    focusedPlaceholderColor = ActivationLabelText,
    unfocusedPlaceholderColor = ActivationLabelText
  )
