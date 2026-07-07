package com.example.force_max_brightness

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val CHANNEL = "com.example.force_max_brightness/brightness"
    
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "canWriteSettings" -> {
                    result.success(Settings.System.canWrite(context))
                }
                "requestWriteSettingsPermission" -> {
                    val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                    result.success(null)
                }
                "getSystemBrightness" -> {
                    try {
                        val brightness = Settings.System.getInt(
                            contentResolver,
                            Settings.System.SCREEN_BRIGHTNESS
                        )
                        result.success(brightness)
                    } catch (e: Settings.SettingNotFoundException) {
                        result.error("ERROR", "Could not get brightness", e.message)
                    }
                }
                "setSystemBrightness" -> {
                    val brightness = call.argument<Int>("brightness")
                    if (brightness == null) {
                        result.error("INVALID_ARGUMENT", "Brightness value is required", null)
                        return@setMethodCallHandler
                    }
                    if (!Settings.System.canWrite(context)) {
                        result.error("PERMISSION_DENIED", "WRITE_SETTINGS permission not granted", null)
                        return@setMethodCallHandler
                    }
                    try {
                        Settings.System.putInt(
                            contentResolver,
                            Settings.System.SCREEN_BRIGHTNESS,
                            brightness.coerceIn(0, 255)
                        )
                        result.success(null)
                    } catch (e: Exception) {
                        result.error("ERROR", "Could not set brightness", e.message)
                    }
                }
                "getSystemSetting" -> {
                    val key = call.argument<String>("key")
                    val isGlobal = call.argument<Boolean>("isGlobal") ?: false
                    if (key == null) {
                        result.error("INVALID_ARGUMENT", "Key is required", null)
                        return@setMethodCallHandler
                    }
                    try {
                        val value = if (isGlobal) {
                            Settings.Global.getString(contentResolver, key)
                        } else {
                            Settings.System.getString(contentResolver, key)
                        }
                        result.success(value)
                    } catch (e: Exception) {
                        result.error("ERROR", "Could not read setting", e.message)
                    }
                }
                "setSystemSetting" -> {
                    val key = call.argument<String>("key")
                    val value = call.argument<String>("value")
                    val isGlobal = call.argument<Boolean>("isGlobal") ?: false
                    if (key == null || value == null) {
                        result.error("INVALID_ARGUMENT", "Key and value are required", null)
                        return@setMethodCallHandler
                    }
                    if (!Settings.System.canWrite(context)) {
                        result.error("PERMISSION_DENIED", "WRITE_SETTINGS permission not granted", null)
                        return@setMethodCallHandler
                    }
                    try {
                        val success = if (isGlobal) {
                            Settings.Global.putString(contentResolver, key, value)
                        } else {
                            Settings.System.putString(contentResolver, key, value)
                        }
                        result.success(success)
                    } catch (e: Exception) {
                        result.error("ERROR", "Could not write setting: ${e.message}", null)
                    }
                }
                else -> {
                    result.notImplemented()
                }
            }
        }
    }
}
