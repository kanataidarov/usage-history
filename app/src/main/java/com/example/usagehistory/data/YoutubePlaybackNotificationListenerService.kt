package com.example.usagehistory.data

import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.usagehistory.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class YoutubePlaybackNotificationListenerService : NotificationListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val mediaSessionManager by lazy {
        getSystemService(MediaSessionManager::class.java)
    }

    private val tracker by lazy {
        YoutubeSessionTracker(
            usageSessionDao = AppDatabase.getInstance(applicationContext).usageSessionDao(),
        )
    }

    private val whatsappMessageTracker by lazy {
        WhatsappMessageTracker(
            usageSessionDao = AppDatabase.getInstance(applicationContext).usageSessionDao(),
        )
    }

    private var currentController: MediaController? = null

    private val activeSessionsChangedListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            handleActiveControllers(controllers.orEmpty())
        }

    private val controllerCallback =
        object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) {
                serviceScope.launch {
                    tracker.onMetadataChanged(metadata)
                }
            }

            override fun onPlaybackStateChanged(state: PlaybackState?) {
                serviceScope.launch {
                    tracker.onPlaybackStateChanged(state)
                }
            }

            override fun onSessionDestroyed() {
                handleActiveControllers(emptyList())
            }
        }

    override fun onListenerConnected() {
        super.onListenerConnected()
        registerActiveSessionListener()
        handleActiveControllers(getActiveControllers())
    }

    override fun onListenerDisconnected() {
        unregisterCurrentController()
        serviceScope.launch {
            tracker.onSessionEnded()
        }
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        serviceScope.launch {
            when (sbn.packageName) {
                YoutubeSessionTracker.YOUTUBE_PACKAGE -> tracker.onNotificationChanged(sbn.notification)
                WhatsappMessageTracker.WHATSAPP_PACKAGE -> whatsappMessageTracker.onNotificationPosted(sbn)
                else -> return@launch
            }
        }
        if (sbn.packageName == YoutubeSessionTracker.YOUTUBE_PACKAGE) {
            handleActiveControllers(getActiveControllers())
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.packageName != YoutubeSessionTracker.YOUTUBE_PACKAGE) return
        handleActiveControllers(getActiveControllers())
    }

    override fun onDestroy() {
        runCatching {
            mediaSessionManager.removeOnActiveSessionsChangedListener(activeSessionsChangedListener)
        }
        unregisterCurrentController()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun registerActiveSessionListener() {
        runCatching {
            mediaSessionManager.addOnActiveSessionsChangedListener(
                activeSessionsChangedListener,
                listenerComponentName(),
            )
        }
    }

    private fun getActiveControllers(): List<MediaController> {
        return runCatching {
            mediaSessionManager.getActiveSessions(listenerComponentName())
        }.getOrDefault(emptyList())
    }

    private fun handleActiveControllers(controllers: List<MediaController>) {
        val nextController = controllers.firstOrNull { it.packageName == YoutubeSessionTracker.YOUTUBE_PACKAGE }
        val controllerChanged = currentController?.sessionToken != nextController?.sessionToken

        if (controllerChanged) {
            unregisterCurrentController()
            currentController = nextController
            nextController?.registerCallback(controllerCallback)
        }

        serviceScope.launch {
            tracker.onControllerChanged(nextController)
        }
    }

    private fun unregisterCurrentController() {
        currentController?.unregisterCallback(controllerCallback)
        currentController = null
    }

    private fun listenerComponentName(): ComponentName {
        return ComponentName(this, YoutubePlaybackNotificationListenerService::class.java)
    }
}
