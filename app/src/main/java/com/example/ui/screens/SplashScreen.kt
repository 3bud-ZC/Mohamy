package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MohamyBlack
import com.example.ui.theme.MohamyCharcoal
import com.example.ui.theme.MohamyGold
import com.example.ui.theme.MohamyWarmTextSoft

@Composable
fun SplashScreen() {
  Box(
    modifier =
      Modifier.fillMaxSize().background(
        Brush.verticalGradient(
          colors = listOf(MohamyBlack, MohamyCharcoal, MohamyBlack)
        )
      ),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Box(
        modifier = Modifier.size(96.dp).background(MohamyGold.copy(alpha = 0.14f), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.AccountBalance,
          contentDescription = "محامي فون",
          tint = MohamyGold,
          modifier = Modifier.size(42.dp)
        )
      }
      Spacer(modifier = Modifier.height(24.dp))
      Text(
        text = "محامي فون",
        style = MaterialTheme.typography.displayMedium,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.ExtraBold
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "منصة قانونية محلية آمنة لإدارة المكتب اليومي",
        style = MaterialTheme.typography.bodyMedium,
        color = MohamyWarmTextSoft,
        textAlign = TextAlign.Center
      )
      Spacer(modifier = Modifier.height(28.dp))
      CircularProgressIndicator(color = MohamyGold, strokeWidth = 3.dp)
    }
  }
}
