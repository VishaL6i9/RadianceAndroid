package com.example.force_max_brightness.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.MutableState
import com.example.force_max_brightness.MainActivity
import kotlinx.coroutines.launch

@Composable
fun BrightnessControlScreen(
    activity: MainActivity? = LocalContext.current as? MainActivity,
    permissionState: MutableState<Boolean>? = null
) {
    if (activity == null) return
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasPermission by remember { mutableStateOf(permissionState?.value ?: false) }
    var currentBrightness by remember { mutableStateOf(128) }
    var sliderValue by remember { mutableStateOf(128f) }
    var statusMessage by remember { mutableStateOf("Initializing...") }
    var brightnessMode by remember { mutableStateOf(0) }
    var windowBrightnessActive by remember { mutableStateOf(false) }
    var serviceRunning by remember { mutableStateOf(false) }
    var autoStartEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        hasPermission = activity.canWriteSettings()
        if (hasPermission) {
            val brightness = activity.getSystemBrightness()
            currentBrightness = brightness
            sliderValue = brightness.toFloat()
        }
        autoStartEnabled = activity.getAutoStart()
    }
    
    // Recheck permission when screen comes into focus
    LaunchedEffect(permissionState?.value) {
        hasPermission = activity.canWriteSettings()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 16.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        // Permission Status Card
        PermissionCard(
            hasPermission = hasPermission,
            statusMessage = statusMessage,
            onRequestPermission = {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = Uri.parse("package:${activity.packageName}")
                }
                activity.permissionLauncher.launch(intent)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Brightness Control Card
        BrightnessControlCard(
            currentBrightness = currentBrightness,
            sliderValue = sliderValue,
            brightnessMode = brightnessMode,
            hasPermission = hasPermission,
            onSliderChange = { sliderValue = it },
            onSliderChangeEnd = {
                scope.launch {
                    activity.setSystemBrightness(it.toInt())
                    currentBrightness = it.toInt()
                    statusMessage = "Brightness: ${it.toInt()}/255"
                }
            },
            onMinClicked = {
                scope.launch {
                    activity.setSystemBrightness(0)
                    currentBrightness = 0
                    sliderValue = 0f
                    statusMessage = "Min brightness"
                }
            },
            onMidClicked = {
                scope.launch {
                    activity.setSystemBrightness(128)
                    currentBrightness = 128
                    sliderValue = 128f
                    statusMessage = "Mid brightness"
                }
            },
            onMaxClicked = {
                scope.launch {
                    activity.setSystemBrightness(255)
                    currentBrightness = 255
                    sliderValue = 255f
                    statusMessage = "Max brightness"
                }
            },
            onModeChanged = { newMode ->
                scope.launch {
                    activity.setBrightnessMode(newMode)
                    brightnessMode = newMode
                    statusMessage = if (newMode == 0) "Manual mode" else "Auto mode"
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Window Brightness Card
        WindowBrightnessCard(
            windowBrightnessActive = windowBrightnessActive,
            onForceMax = {
                scope.launch {
                    activity.setWindowBrightness(255)
                    windowBrightnessActive = true
                    statusMessage = "Window brightness: max"
                }
            },
            onMedium = {
                scope.launch {
                    activity.setWindowBrightness(128)
                    windowBrightnessActive = true
                    statusMessage = "Window brightness: medium"
                }
            },
            onReset = {
                scope.launch {
                    activity.setWindowBrightness(-1)
                    windowBrightnessActive = false
                    statusMessage = "Window brightness: reset"
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Media Monitor Card
        MediaMonitorCard(
            serviceRunning = serviceRunning,
            autoStartEnabled = autoStartEnabled,
            onStartClicked = {
                scope.launch {
                    activity.startMediaMonitor()
                    serviceRunning = true
                    statusMessage = "Monitor started"
                }
            },
            onStopClicked = {
                scope.launch {
                    activity.stopMediaMonitor()
                    serviceRunning = false
                    statusMessage = "Monitor stopped"
                }
            },
            onAutoStartChanged = { enabled ->
                scope.launch {
                    activity.setAutoStart(enabled)
                    autoStartEnabled = enabled
                    statusMessage = if (enabled) "Auto-start enabled" else "Auto-start disabled"
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Status Message
        Text(
            text = "Status: $statusMessage",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Composable
fun PermissionCard(
    hasPermission: Boolean,
    statusMessage: String,
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (hasPermission) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (hasPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Permission Status",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(statusMessage, style = MaterialTheme.typography.bodySmall)
            if (!hasPermission) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Grant Permission")
                }
            }
        }
    }
}

@Composable
fun BrightnessControlCard(
    currentBrightness: Int,
    sliderValue: Float,
    brightnessMode: Int,
    hasPermission: Boolean,
    onSliderChange: (Float) -> Unit,
    onSliderChangeEnd: (Float) -> Unit,
    onMinClicked: () -> Unit,
    onMidClicked: () -> Unit,
    onMaxClicked: () -> Unit,
    onModeChanged: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Brightness Control",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Current: $currentBrightness/255 (${(currentBrightness * 100 / 255)}%)",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "Mode: ${if (brightnessMode == 0) "Manual" else "Auto"}",
                style = MaterialTheme.typography.bodySmall,
                color = if (brightnessMode == 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = { onModeChanged(0) },
                    modifier = Modifier.weight(1f),
                    enabled = hasPermission
                ) {
                    Text("Manual", fontSize = MaterialTheme.typography.labelSmall.fontSize)
                }
                Button(
                    onClick = { onModeChanged(1) },
                    modifier = Modifier.weight(1f),
                    enabled = hasPermission
                ) {
                    Text("Auto", fontSize = MaterialTheme.typography.labelSmall.fontSize)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Slider(
                value = sliderValue,
                onValueChange = onSliderChange,
                onValueChangeFinished = { onSliderChangeEnd(sliderValue) },
                valueRange = 0f..255f,
                enabled = hasPermission,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = onMinClicked,
                    modifier = Modifier.weight(1f),
                    enabled = hasPermission
                ) {
                    Text("Min")
                }
                Button(
                    onClick = onMidClicked,
                    modifier = Modifier.weight(1f),
                    enabled = hasPermission
                ) {
                    Text("Mid")
                }
                Button(
                    onClick = onMaxClicked,
                    modifier = Modifier.weight(1f),
                    enabled = hasPermission
                ) {
                    Text("Max")
                }
            }
        }
    }
}

@Composable
fun WindowBrightnessCard(
    windowBrightnessActive: Boolean,
    onForceMax: () -> Unit,
    onMedium: () -> Unit,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Window Brightness",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                if (windowBrightnessActive) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "No permission needed",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = onForceMax,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Max")
                }
                Button(
                    onClick = onMedium,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Mid")
                }
                Button(
                    onClick = onReset,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reset")
                }
            }
        }
    }
}

@Composable
fun MediaMonitorCard(
    serviceRunning: Boolean,
    autoStartEnabled: Boolean,
    onStartClicked: () -> Unit,
    onStopClicked: () -> Unit,
    onAutoStartChanged: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Auto Monitor",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                if (serviceRunning) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Boost brightness during playback",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = onStartClicked,
                    modifier = Modifier.weight(1f),
                    enabled = !serviceRunning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Start")
                }
                Button(
                    onClick = onStopClicked,
                    modifier = Modifier.weight(1f),
                    enabled = serviceRunning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Stop")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Auto-start on boot",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = autoStartEnabled,
                    onCheckedChange = onAutoStartChanged
                )
            }
        }
    }
}
