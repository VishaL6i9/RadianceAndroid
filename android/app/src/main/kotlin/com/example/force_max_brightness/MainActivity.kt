package com.example.force_max_brightness

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import com.example.force_max_brightness.ui.BrightnessControlScreen
import com.example.force_max_brightness.ui.theme.ForceMaxBrightnessTheme

class MainActivity : ComponentActivity() {
    val permissionState = mutableStateOf(false)
    
    val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Recheck permission after user returns from settings
        permissionState.value = Settings.System.canWrite(this@MainActivity)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ForceMaxBrightnessTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    BrightnessControlScreen(
                        activity = this@MainActivity,
                        permissionState = permissionState
                    )
                }
            }
        }
    }

    // Brightness Control Methods
    fun canWriteSettings(): Boolean {
        return Settings.System.canWrite(this)
    }

    fun requestWriteSettingsPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }

    fun getSystemBrightness(): Int {
        return try {
            Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (e: Settings.SettingNotFoundException) {
            128
        }
    }

    fun setSystemBrightness(brightness: Int) {
        if (!Settings.System.canWrite(this)) return
        try {
            Settings.System.putInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                brightness.coerceIn(0, 255)
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getBrightnessMode(): Int {
        return try {
            Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE)
        } catch (e: Exception) {
            0
        }
    }

    fun setBrightnessMode(mode: Int) {
        if (!Settings.System.canWrite(this)) return
        try {
            Settings.System.putInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                mode.coerceIn(0, 1)
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setWindowBrightness(brightness: Int) {
        try {
            val layoutParams = window?.attributes
            layoutParams?.screenBrightness = if (brightness == -1) {
                -1.0f
            } else {
                brightness.coerceIn(0, 255) / 255.0f
            }
            window?.attributes = layoutParams
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Media Monitor
    fun startMediaMonitor() {
        val intent = Intent(this, MediaMonitorService::class.java).apply {
            action = "START_MONITORING"
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    fun stopMediaMonitor() {
        val intent = Intent(this, MediaMonitorService::class.java).apply {
            action = "STOP_MONITORING"
        }
        stopService(intent)
    }

    // Preferences
    fun setAutoStart(enabled: Boolean) {
        // Use blocking call in a separate thread or just write to preferences
        val prefs = getSharedPreferences("force_brightness_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("auto_start_service", enabled).apply()
    }

    fun getAutoStart(): Boolean {
        val prefs = getSharedPreferences("force_brightness_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("auto_start_service", false)
    }
}

