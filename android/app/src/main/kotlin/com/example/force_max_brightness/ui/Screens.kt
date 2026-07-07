package com.example.force_max_brightness.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        HeaderSection()
        Spacer(modifier = Modifier.height(24.dp))

        PermissionCard(
            hasPermission = hasPermission,
            onRequestPermission = {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = Uri.parse("package:${activity.packageName}")
                }
                activity.permissionLauncher.launch(intent)
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

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

        Spacer(modifier = Modifier.height(24.dp))

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

        Spacer(modifier = Modifier.height(24.dp))

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

        Spacer(modifier = Modifier.height(24.dp))
        StatusBar(statusMessage)
    }
}

@Composable
fun HeaderSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(
            "Brightness Control",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            ),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Manage system & window brightness",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PermissionCard(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    val backgroundColor = animateColorAsState(
        targetValue = if (hasPermission) 
            MaterialTheme.colorScheme.primaryContainer 
        else 
            MaterialTheme.colorScheme.errorContainer,
        animationSpec = tween(500)
    )
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .shadow(4.dp, shape = RoundedCornerShape(16.dp)),
        color = backgroundColor.value
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    color = if (hasPermission) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.error
                ) {
                    Icon(
                        imageVector = if (hasPermission) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(28.dp)
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Permissions",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (hasPermission) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        if (hasPermission) "Granted" else "Required",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (hasPermission) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            if (!hasPermission) {
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .shadow(4.dp, shape = RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.BrightnessHigh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "System Brightness",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "$currentBrightness/255 (${(currentBrightness * 100 / 255)}%)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Slider(
                value = sliderValue,
                onValueChange = onSliderChange,
                onValueChangeFinished = { onSliderChangeEnd(sliderValue) },
                valueRange = 0f..255f,
                enabled = hasPermission,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onMinClicked,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    enabled = hasPermission
                ) {
                    Text("Min", style = MaterialTheme.typography.labelMedium)
                }
                FilledTonalButton(
                    onClick = onMidClicked,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    enabled = hasPermission
                ) {
                    Text("Mid", style = MaterialTheme.typography.labelMedium)
                }
                FilledTonalButton(
                    onClick = onMaxClicked,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    enabled = hasPermission,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text("Max", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = brightnessMode == 0,
                    onClick = { onModeChanged(0) },
                    label = { Text("Manual") },
                    modifier = Modifier.weight(1f),
                    enabled = hasPermission
                )
                FilterChip(
                    selected = brightnessMode == 1,
                    onClick = { onModeChanged(1) },
                    label = { Text("Auto") },
                    modifier = Modifier.weight(1f),
                    enabled = hasPermission
                )
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .shadow(4.dp, shape = RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.LightMode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Window Brightness Override",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        if (windowBrightnessActive) "Active" else "Inactive",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (windowBrightnessActive) 
                            MaterialTheme.colorScheme.tertiary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onForceMax,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Text("Max", style = MaterialTheme.typography.labelMedium)
                }
                FilledTonalButton(
                    onClick = onMedium,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                ) {
                    Text("Mid", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                ) {
                    Text("Reset", style = MaterialTheme.typography.labelMedium)
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .shadow(4.dp, shape = RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Media Playback Monitor",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        if (serviceRunning) "Running" else "Stopped",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (serviceRunning) 
                            MaterialTheme.colorScheme.secondary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onStartClicked,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    enabled = !serviceRunning,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text("Start", style = MaterialTheme.typography.labelMedium)
                }
                FilledTonalButton(
                    onClick = onStopClicked,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    enabled = serviceRunning
                ) {
                    Text("Stop", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Auto-start on boot",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Switch(
                    checked = autoStartEnabled,
                    onCheckedChange = onAutoStartChanged
                )
            }
        }
    }
}

@Composable
fun StatusBar(statusMessage: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .shadow(2.dp, shape = RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                statusMessage,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
