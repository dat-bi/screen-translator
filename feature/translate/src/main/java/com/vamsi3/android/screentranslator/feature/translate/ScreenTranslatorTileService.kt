package com.vamsi3.android.screentranslator.feature.translate

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.vamsi3.android.screentranslator.core.data.model.TileActionMode
import com.vamsi3.android.screentranslator.core.data.repository.UserDataRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ScreenTranslatorTileService : TileService() {

    @Inject
    lateinit var userDataRepository: UserDataRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onStartListening() {
        super.onStartListening()
        // Update tile state when it becomes visible
        serviceScope.launch {
            val userData = userDataRepository.userData.first()
            val tileActionMode = userData?.tileActionMode ?: TileActionMode.SCREENSHOT

            when (tileActionMode) {
                TileActionMode.SCREENSHOT -> {
                    val accessibilityEnabled = isScreenTranslatorAccessibilityServiceEnabled(application)
                    qsTile.state = if (accessibilityEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                    updateTileSubtitle(null) // No subtitle for screenshot mode
                }
                TileActionMode.TOGGLE_BUBBLE -> {
                    val isBubbleRunning = BubbleOverlayManager.isBubbleServiceRunning(application)
                    qsTile.state = if (isBubbleRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                    updateTileSubtitle(if (isBubbleRunning) "On" else "Off")
                }
            }
            qsTile.updateTile()
        }
    }

    override fun onClick() {
        super.onClick()

        serviceScope.launch {
            val userData = userDataRepository.userData.first()
            val tileActionMode = userData?.tileActionMode ?: TileActionMode.SCREENSHOT

            when (tileActionMode) {
                TileActionMode.SCREENSHOT -> handleScreenshotAction()
                TileActionMode.TOGGLE_BUBBLE -> handleToggleBubbleAction()
            }
        }
    }

    private fun handleScreenshotAction() {
        val accessibilityPermissionGranted = isScreenTranslatorAccessibilityServiceEnabled(application)
        val previousTileState = qsTile.state
        qsTile.state = if (accessibilityPermissionGranted) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        if (qsTile.state != previousTileState) {
            qsTile.updateTile()
        }

        val intent = Intent(application, ScreenTranslatorAccessibilityService::class.java)
        startService(intent)
    }

    private fun handleToggleBubbleAction() {
        val isBubbleRunning = BubbleOverlayManager.isBubbleServiceRunning(application)

        if (isBubbleRunning) {
            // Stop the bubble service
            BubbleOverlayManager.stopBubbleService(application)
            qsTile.state = Tile.STATE_INACTIVE
            updateTileSubtitle("Off")
        } else {
            // Start the bubble service if we have permission
            if (BubbleOverlayManager.hasOverlayPermission(application)) {
                BubbleOverlayManager.startBubbleService(application)
                qsTile.state = Tile.STATE_ACTIVE
                updateTileSubtitle("On")
            } else {
                // Request permission
                BubbleOverlayManager.requestOverlayPermission(application)
                qsTile.state = Tile.STATE_INACTIVE
                updateTileSubtitle("Off")
            }
        }
        qsTile.updateTile()
    }

    private fun updateTileSubtitle(subtitle: String?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            qsTile.subtitle = subtitle
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
