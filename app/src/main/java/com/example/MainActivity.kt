package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppViewModel
import com.example.data.AppNotificationManager
import com.example.ui.MainLayout
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    AppNotificationManager.ensureChannels(this)
    enableEdgeToEdge()
    setContent {
      val appViewModel: AppViewModel = viewModel()
      MyApplicationTheme(darkTheme = appViewModel.isDarkThemeEnabled) {
        Surface(modifier = Modifier.fillMaxSize()) {
          MainLayout(viewModel = appViewModel)
        }
      }
    }
  }
}
