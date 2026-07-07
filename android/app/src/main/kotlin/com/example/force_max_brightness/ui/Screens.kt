package com.example.force_max_brightness.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.MutableState
import com.example.force_max_brightness.MainActivity
import com.example.force_max_brightness.OnePlusHBMController
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)

@Composable
fun BrightnessControlScreen(
    activity: MainActivity? = LocalContext.current as? MainActivity,
    permissionState: MutableState<Boolean>? = null
) {
    if (activity == null) return
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var hasPermission by remember { mutableStateOf(permissionState?.value ?: false) }
    var currentBrightness by remember { mutableStateOf(128) }
    var sliderValue by remember { mutableStateOf(128f) }
    var statusMessage by remember { mutableStateOf("Ready") }
    var brightnessMode by remember { mutableStateOf(0) }
    var windowBrightnessActive by remember { mutableStateOf(false) }
    var serviceRunning by remember { mutableStateOf(false) }
    var autoStartEnabled by remember { mutableStateOf(false) }
    var hbmController by remember { mutableStateOf<OnePlusHBMController?>(null) }
    var hbmIndicatorActive by remember { mutableStateOf(false) }
    var peakBrightnessNits by remember { mutableStateOf(1000) }

    LaunchedEffect(Unit) {
        hasPermission = activity.canWriteSettings()
        if (hasPermission) {
            val brightness = activity.getSystemBrightness()
            currentBrightness = brightness
            sliderValue = brightness.toFloat()
        }
        autoStartEnabled = activity.getAutoStart()
        
        // Initialize HBM controller
        val controller = OnePlusHBMController(context)
        hbmController = controller
        peakBrightnessNits = controller.peakBrightnessNits.value
        controller.startHDRMonitoring()
    }
    
    LaunchedEffect(permissionState?.value) {
        hasPermission = activity.canWriteSettings()
    }
    
    // Monitor HBM indicator
    LaunchedEffect(hbmController) {
        while (true) {
            hbmController?.updateHBMIndicator()
            hbmIndicatorActive = hbmController?.hbmIndicatorActive?.value ?: false
            kotlinx.coroutines.delay(1000) // Update every second
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Brightness Control",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = statusMessage.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            PermissionCard(
                hasPermission = hasPermission,
                onRequestPermission = {
                    val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                        data = Uri.parse("package:${activity.packageName}")
                    }
                    activity.permissionLauncher.launch(intent)
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

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

            Spacer(modifier = Modifier.height(20.dp))

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

            Spacer(modifier = Modifier.height(20.dp))

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

            Spacer(modifier = Modifier.height(20.dp))

            HBMIndicatorCard(
                hbmActive = hbmIndicatorActive,
                peakBrightnessNits = peakBrightnessNits
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun PermissionCard(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (hasPermission) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    color = if (hasPermission) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.error,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = if (hasPermission) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .padding(12.dp)
                            .size(32.dp)
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (hasPermission) "Permission Granted" else "Permission Required",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (hasPermission) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        if (hasPermission) "Ready to adjust brightness" else "Tap to grant WRITE_SETTINGS",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hasPermission) 
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) 
                        else 
                            MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                    )
                }
            }
            
            if (!hasPermission) {
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Grant Permission", style = MaterialTheme.typography.labelLarge)
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
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.BrightnessHigh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "System Brightness",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${(currentBrightness * 100 / 255)}%",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Slider(
                value = sliderValue,
                onValueChange = onSliderChange,
                onValueChangeFinished = { onSliderChangeEnd(sliderValue) },
                valueRange = 0f..255f,
                enabled = hasPermission,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = onMinClicked,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    enabled = hasPermission,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Min", style = MaterialTheme.typography.labelLarge)
                }
                FilledTonalButton(
                    onClick = onMidClicked,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    enabled = hasPermission,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Mid", style = MaterialTheme.typography.labelLarge)
                }
                Button(
                    onClick = onMaxClicked,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    enabled = hasPermission,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Max", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = brightnessMode == 0,
                    onClick = { onModeChanged(0) },
                    label = { Text("Manual") },
                    modifier = Modifier.weight(1f),
                    enabled = hasPermission,
                    leadingIcon = if (brightnessMode == 0) {
                        {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else null
                )
                FilterChip(
                    selected = brightnessMode == 1,
                    onClick = { onModeChanged(1) },
                    label = { Text("Auto") },
                    modifier = Modifier.weight(1f),
                    enabled = hasPermission,
                    leadingIcon = if (brightnessMode == 1) {
                        {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else null
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
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.LightMode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(28.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Window Brightness",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (windowBrightnessActive) "Active" else "Inactive",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (windowBrightnessActive) 
                            MaterialTheme.colorScheme.tertiary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onForceMax,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Text("Max", style = MaterialTheme.typography.labelLarge)
                }
                FilledTonalButton(
                    onClick = onMedium,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Mid", style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Reset", style = MaterialTheme.typography.labelLarge)
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
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(28.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Media Monitoring",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (serviceRunning) "Running" else "Stopped",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (serviceRunning) 
                            MaterialTheme.colorScheme.secondary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onStartClicked,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    enabled = !serviceRunning,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("Start", style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = onStopClicked,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    enabled = serviceRunning,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Stop", style = MaterialTheme.typography.labelLarge)
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
                    onCheckedChange = onAutoStartChanged,
                    modifier = Modifier.scale(scaleX = 0.9f, scaleY = 0.9f)
                )
            }
        }
    }
}

@Composable
fun HBMIndicatorCard(
    hbmActive: Boolean,
    peakBrightnessNits: Int
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (hbmActive) 
                MaterialTheme.colorScheme.tertiaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    color = if (hbmActive) 
                        MaterialTheme.colorScheme.tertiary 
                    else 
                        MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        if (hbmActive) Icons.Default.BrightnessHigh else Icons.Outlined.BrightnessHigh,
                        contentDescription = null,
                        tint = if (hbmActive)
                            Color.White
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "HBM Status (Non-Root Indicator)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (hbmActive) "HDR Detected" else "SDR Content",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (hbmActive)
                            MaterialTheme.colorScheme.onTertiaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Brightness4,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "Peak: $peakBrightnessNits nits",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "Non-root mode: Shows HDR detection only. Root access required to control HBM brightness.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
