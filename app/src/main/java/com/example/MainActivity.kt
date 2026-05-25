package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.ui.main.MainScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Safely verify and manually initialize FirebaseApp if it didn't auto-initialize
    try {
      if (com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
        val options = com.google.firebase.FirebaseOptions.Builder()
            .setApplicationId("1:1234567890:android:1234567890abcdef")
            .setApiKey("mock-api-key-to-prevent-startup-crash")
            .setProjectId("medremind-rxqpzw")
            .build()
        com.google.firebase.FirebaseApp.initializeApp(this, options)
        android.util.Log.d("MainActivity", "FirebaseApp initialized manually in onCreate")
      }
    } catch (e: Exception) {
      android.util.Log.e("MainActivity", "Error verifying/initializing FirebaseApp: ${e.message}")
    }

    // Request notification permissions for Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
      }
    }

    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          MainScreen()
        }
      }
    }
  }
}
