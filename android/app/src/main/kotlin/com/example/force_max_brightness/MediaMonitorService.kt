package com.example.force_max_brightness

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat

class MediaMonitorService : Service() {
    private val TAG = "MediaMonitorService"
    private val CHANNEL_ID = "MediaMonitorChannel"
    private val NOTIFICATION_ID = 1
    
    private var mediaSessionManager: MediaSessionManager? = null
    private val activeControllers = mutableListOf<MediaController>()
    private var originalBrightness: Int = -1
    private var isMonitoring = false
    
    private val sessionCallback = object : MediaSessionManager.OnActiveSessionsChangedListener {
        override fun onActiveSessionsChanged(controllers: MutableList<MediaController>?) {
            Log.d(TAG, "Active sessions changed: ${controllers?.size ?: 0} controllers")
            activeControllers.clear()
            controllers?.forEach { controller ->
                activeControllers.add(controller)
                controller.registerCallback(playbackCallback)
                checkPlaybackState(controller)
            }
        }
    }
    
    private val playbackCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            super.onPlaybackStateChanged(state)
            state?.let {
                Log.d(TAG, "Playback state: ${getStateName(it.state)}")
                handlePlaybackStateChange(it)
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        createNotificationChannel()
        mediaSessionManager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        
        when (intent?.action) {
            "START_MONITORING" -> startMonitoring()
            "STOP_MONITORING" -> stopMonitoring()
        }
        
        val notification = createNotification("Monitoring media playback...")
        startForeground(NOTIFICATION_ID, notification)
        
        return START_STICKY
    }
    
    private fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true
        
        try {
            // Note: Requires BIND_NOTIFICATION_LISTENER_SERVICE permission
            // User must enable in Settings > Apps > Special Access > Notification Access
            val controllers = mediaSessionManager?.getActiveSessions(null)
            Log.d(TAG, "Found ${controllers?.size ?: 0} active media sessions")
            
            controllers?.forEach { controller ->
                activeControllers.add(controller)
                controller.registerCallback(playbackCallback)
                checkPlaybackState(controller)
            }
            
            mediaSessionManager?.addOnActiveSessionsChangedListener(sessionCallback, null)
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied: ${e.message}")
            updateNotification("Permission required - enable Notification Access")
        }
    }
    
    private fun stopMonitoring() {
        isMonitoring = false
        activeControllers.forEach { it.unregisterCallback(playbackCallback) }
        activeControllers.clear()
        mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionCallback)
        restoreBrightness()
    }
    
    private fun checkPlaybackState(controller: MediaController) {
        val state = controller.playbackState
        val metadata = controller.metadata
        
        Log.d(TAG, "Package: ${controller.packageName}")
        Log.d(TAG, "State: ${state?.state}")
        Log.d(TAG, "Title: ${metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE)}")
        
        state?.let { handlePlaybackStateChange(it) }
    }
    
    private fun handlePlaybackStateChange(state: PlaybackState) {
        when (state.state) {
            PlaybackState.STATE_PLAYING -> {
                Log.d(TAG, "Media is playing - checking for HDR")
                // For now, force max brightness on any video playback
                // TODO: Add HDR detection logic
                forceMaxBrightness()
                updateNotification("Media playing - Brightness forced to max")
            }
            PlaybackState.STATE_PAUSED,
            PlaybackState.STATE_STOPPED -> {
                Log.d(TAG, "Media paused/stopped - restoring brightness")
                restoreBrightness()
                updateNotification("Monitoring media playback...")
            }
        }
    }
    
    private fun forceMaxBrightness() {
        if (originalBrightness == -1) {
            try {
                originalBrightness = Settings.System.getInt(
                    contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS
                )
                Log.d(TAG, "Saved original brightness: $originalBrightness")
            } catch (e: Exception) {
                Log.e(TAG, "Error reading brightness: ${e.message}")
                originalBrightness = 128
            }
        }
        
        if (Settings.System.canWrite(this)) {
            try {
                Settings.System.putInt(
                    contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    255
                )
                Log.d(TAG, "Brightness set to max")
            } catch (e: Exception) {
                Log.e(TAG, "Error setting brightness: ${e.message}")
            }
        } else {
            Log.w(TAG, "No WRITE_SETTINGS permission")
        }
    }
    
    private fun restoreBrightness() {
        if (originalBrightness != -1 && Settings.System.canWrite(this)) {
            try {
                Settings.System.putInt(
                    contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    originalBrightness
                )
                Log.d(TAG, "Brightness restored to $originalBrightness")
                originalBrightness = -1
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring brightness: ${e.message}")
            }
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Media Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors media playback for HDR content"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("HDR Brightness Monitor")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun updateNotification(text: String) {
        val notification = createNotification(text)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun getStateName(state: Int): String {
        return when (state) {
            PlaybackState.STATE_PLAYING -> "PLAYING"
            PlaybackState.STATE_PAUSED -> "PAUSED"
            PlaybackState.STATE_STOPPED -> "STOPPED"
            PlaybackState.STATE_BUFFERING -> "BUFFERING"
            else -> "UNKNOWN ($state)"
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        stopMonitoring()
    }
}
