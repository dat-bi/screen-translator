package com.vamsi3.android.screentranslator.feature.translate

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings as AndroidSettings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.vamsi3.android.screentranslator.core.data.model.TranslatorDismissAction
import com.vamsi3.android.screentranslator.core.data.repository.UserDataRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import kotlin.math.abs
import com.vamsi3.android.screentranslator.core.resource.R as CoreR

@AndroidEntryPoint
class BubbleOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var layoutParams: WindowManager.LayoutParams
    private var bubbleView: View? = null
    private var initialX: Int = 0
    private var initialY: Int = 0
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f
    private var isDragging = false
    @Volatile
    private var shouldSnapToEdge = true

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Inject
    lateinit var userDataRepository: UserDataRepository

    companion object {
        private const val TAG = "BubbleOverlayService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "bubble_overlay_channel"
        const val ACTION_STOP_BUBBLE = "com.vamsi3.android.screentranslator.STOP_BUBBLE"

        // Translator app package names
        private val TRANSLATOR_PACKAGES = setOf(
            "com.google.android.apps.translate", // Google Translate
            "com.google.ar.lens", // Google Lens (standalone)
            "com.google.android.googlequicksearchbox", // Google app (includes Lens)
            "com.deepl.mobiletranslator", // DeepL
            "com.naver.labs.translator" // Papago
        )
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createBubbleView()
        observeSettings()
    }

    private fun observeSettings() {
        serviceScope.launch {
            userDataRepository.userData.collect { userData ->
                userData?.let {
                    shouldSnapToEdge = it.bubbleSnapToEdge
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Translator Bubble",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows the translation bubble overlay"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, BubbleOverlayService::class.java).apply {
            action = ACTION_STOP_BUBBLE
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Translator")
            .setContentText("Bubble is active. Tap to translate.")
            .setSmallIcon(CoreR.drawable.ic_translate_tile)
            .addAction(
                CoreR.drawable.ic_translate_tile,
                "Hide Bubble",
                stopPendingIntent
            )
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createBubbleView() {
        val layoutInflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        bubbleView = layoutInflater.inflate(R.layout.bubble_overlay, null)

        // Apply dynamic colors and size
        val userData = runBlocking { userDataRepository.userData.firstOrNull() }
        val bgColor = userData?.bubbleBackgroundColor ?: "#00000000"
        val borderColor = userData?.bubbleBorderColor ?: "#4CAF50"
        val iconColor = userData?.bubbleIconColor ?: "#FFFFFF"
        val sizeDp = userData?.bubbleSizeDp ?: 56
        shouldSnapToEdge = userData?.bubbleSnapToEdge ?: true

        val strokeWidthPx = (2 * resources.displayMetrics.density).toInt()
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor(bgColor))
            setStroke(strokeWidthPx, Color.parseColor(borderColor))
        }
        bubbleView?.background = drawable

        // Apply icon color tint
        val bubbleIcon = bubbleView?.findViewById<ImageView>(R.id.bubble_icon)
        bubbleIcon?.setColorFilter(Color.parseColor(iconColor))

        // Set size
        val sizePx = (sizeDp * resources.displayMetrics.density).toInt()

        layoutParams = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = clampX(userData?.bubblePositionX ?: 0, sizePx)
            y = clampY(userData?.bubblePositionY ?: 100, sizePx)
        }

        windowManager.addView(bubbleView, layoutParams)

        bubbleView?.setOnTouchListener(object : View.OnTouchListener {
            private var lastAction = 0
            private val clickThreshold = 200 // milliseconds
            private val longPressThreshold = 500 // milliseconds for long press
            private var pressStartTime = 0L
            private val moveThreshold = 10 // pixels

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        pressStartTime = System.currentTimeMillis()
                        isDragging = false

                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        lastAction = MotionEvent.ACTION_DOWN
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = event.rawX - initialTouchX
                        val deltaY = event.rawY - initialTouchY

                        // Check if moved beyond threshold
                        if (abs(deltaX) > moveThreshold || abs(deltaY) > moveThreshold) {
                            isDragging = true
                        }

                        if (isDragging) {
                            layoutParams.x = initialX + deltaX.toInt()
                            layoutParams.y = initialY + deltaY.toInt()
                            windowManager.updateViewLayout(bubbleView, layoutParams)
                        }
                        lastAction = MotionEvent.ACTION_MOVE
                        return true
                    }

                    MotionEvent.ACTION_UP -> {
                        val pressDuration = System.currentTimeMillis() - pressStartTime

                        if (isDragging) {
                            // Snap to edge after dragging (if enabled)
                            if (shouldSnapToEdge) {
                                snapToEdge()
                            } else {
                                persistBubblePosition()
                            }
                        } else {
                            if (pressDuration >= longPressThreshold) {
                                // Long press - open settings
                                onBubbleLongPressed()
                            } else if (pressDuration < clickThreshold) {
                                // Quick tap - trigger translation
                                onBubbleClicked()
                            }
                        }

                        lastAction = MotionEvent.ACTION_UP
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun onBubbleLongPressed() {
        Log.i(TAG, "Bubble long pressed - opening settings")

        // Open the main app activity (settings)
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        if (intent != null) {
            try {
                startActivity(intent)
                Log.i(TAG, "Settings activity launched successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch settings", e)
                showToast("Failed to open settings: ${e.message}")
            }
        } else {
            Log.e(TAG, "Failed to get launch intent")
            showToast("Failed to open settings")
        }

        // Provide visual feedback
        bubbleView?.animate()
            ?.scaleX(1.2f)
            ?.scaleY(1.2f)
            ?.setDuration(100)
            ?.withEndAction {
                bubbleView?.animate()
                    ?.scaleX(1f)
                    ?.scaleY(1f)
                    ?.setDuration(100)
                    ?.start()
            }
            ?.start()
    }

    private fun snapToEdge() {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels

        // Snap to nearest edge (left or right)
        val targetX = if (layoutParams.x + (bubbleView?.width ?: 0) / 2 < screenWidth / 2) {
            0 // Snap to left
        } else {
            screenWidth - (bubbleView?.width ?: 0) // Snap to right
        }

        // Animate to edge
        bubbleView?.animate()
            ?.translationX(0f)
            ?.setDuration(150)
            ?.withEndAction {
                layoutParams.x = targetX
                windowManager.updateViewLayout(bubbleView, layoutParams)
            }
            ?.start()

        // Update immediately for smoother feel
        layoutParams.x = targetX
        windowManager.updateViewLayout(bubbleView, layoutParams)
        persistBubblePosition()
    }

    private fun persistBubblePosition() {
        val bubbleSize = bubbleView?.width ?: layoutParams.width
        val safeX = clampX(layoutParams.x, bubbleSize)
        val safeY = clampY(layoutParams.y, bubbleSize)

        layoutParams.x = safeX
        layoutParams.y = safeY
        windowManager.updateViewLayout(bubbleView, layoutParams)

        serviceScope.launch {
            userDataRepository.setBubblePosition(safeX, safeY)
        }
    }

    private fun clampX(x: Int, bubbleSize: Int): Int {
        val maxX = (resources.displayMetrics.widthPixels - bubbleSize).coerceAtLeast(0)
        return x.coerceIn(0, maxX)
    }

    private fun clampY(y: Int, bubbleSize: Int): Int {
        val maxY = (resources.displayMetrics.heightPixels - bubbleSize).coerceAtLeast(0)
        return y.coerceIn(0, maxY)
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOpsManager = getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOpsManager.unsafeCheckOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOpsManager.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        }
        val hasPermission = mode == android.app.AppOpsManager.MODE_ALLOWED
        Log.d(TAG, "hasUsageStatsPermission: mode=$mode, hasPermission=$hasPermission")
        return hasPermission
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun getForegroundPackage(): String? {
        if (!hasUsageStatsPermission()) {
            Log.w(TAG, "Usage stats permission not granted in getForegroundPackage")
            return null
        }

        return try {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val endTime = System.currentTimeMillis()
            val startTime = endTime - 5000 // Last 5 seconds

            val usageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )

            Log.d(TAG, "usageStats count: ${usageStats?.size ?: 0}")

            if (usageStats.isNullOrEmpty()) {
                Log.w(TAG, "No usage stats available - list is empty")
                return null
            }

            // Log all recent apps for debugging
            val recentApps = usageStats
                .filter { it.lastTimeUsed > 0 }
                .sortedByDescending { it.lastTimeUsed }
                .take(5)

            Log.d(TAG, "Top 5 recent apps:")
            recentApps.forEachIndexed { index, stat ->
                Log.d(TAG, "  $index: ${stat.packageName} (lastUsed: ${stat.lastTimeUsed})")
            }

            // Find the most recently used app (excluding our own)
            val recentApp = usageStats
                .filter { it.lastTimeUsed > 0 && it.packageName != packageName }
                .maxByOrNull { it.lastTimeUsed }

            Log.d(TAG, "Selected foreground package: ${recentApp?.packageName}")
            recentApp?.packageName
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get foreground package", e)
            null
        }
    }

    private fun isTranslatorAppInForeground(): Boolean {
        val foregroundPackage = getForegroundPackage()
        val isTranslator = foregroundPackage != null && foregroundPackage in TRANSLATOR_PACKAGES
        Log.d(TAG, "isTranslatorAppInForeground: foregroundPackage=$foregroundPackage, isTranslator=$isTranslator")
        Log.d(TAG, "Known translator packages: $TRANSLATOR_PACKAGES")
        return isTranslator
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun performDismissAction(action: TranslatorDismissAction) {
        Log.i(TAG, "Performing dismiss action: $action")

        when (action) {
            TranslatorDismissAction.NOTHING -> {
                // Do nothing, will take screenshot below
            }
            TranslatorDismissAction.GO_BACK -> {
                // Send GO_BACK action to accessibility service
                val intent = Intent(this, ScreenTranslatorAccessibilityService::class.java)
                intent.action = "GO_BACK"
                try {
                    startService(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send GO_BACK action", e)
                }
                return
            }
            TranslatorDismissAction.GO_HOME -> {
                // Send GO_HOME action to accessibility service
                val intent = Intent(this, ScreenTranslatorAccessibilityService::class.java)
                intent.action = "GO_HOME"
                try {
                    startService(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send GO_HOME action", e)
                }
                return
            }
            TranslatorDismissAction.KILL_APP -> {
                // Go home first, then kill the translator apps
                val intent = Intent(this, ScreenTranslatorAccessibilityService::class.java)
                intent.action = "GO_HOME"
                try {
                    startService(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send GO_HOME action", e)
                }

                // Kill translator apps after a short delay
                Handler(Looper.getMainLooper()).postDelayed({
                    val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                    TRANSLATOR_PACKAGES.forEach { packageName ->
                        try {
                            activityManager.killBackgroundProcesses(packageName)
                            Log.i(TAG, "Killed background process: $packageName")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to kill $packageName", e)
                        }
                    }
                }, 300)
                return
            }
        }
    }

    private fun onBubbleClicked() {
        // Check if accessibility service is enabled
        if (!isScreenTranslatorAccessibilityServiceEnabled(application)) {
            showToast("Please enable Screen Translator in Accessibility Settings")
            // Open accessibility settings
            val intent = Intent(AndroidSettings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            return
        }

        // Check if a translator app is in the foreground
        val userData = runBlocking { userDataRepository.userData.firstOrNull() }
        val dismissAction = userData?.translatorDismissAction ?: TranslatorDismissAction.NOTHING

        Log.d(TAG, "=== BUBBLE CLICKED DEBUG ===")
        Log.d(TAG, "dismissAction setting: $dismissAction")
        Log.d(TAG, "hasUsageStatsPermission: ${hasUsageStatsPermission()}")

        if (dismissAction != TranslatorDismissAction.NOTHING) {
            // Check if we have permission to detect foreground app
            if (!hasUsageStatsPermission()) {
                Log.w(TAG, "Usage stats permission NOT granted - opening settings")
                showToast("Grant 'Usage Access' permission in Settings for this feature")
                // Open usage access settings
                val intent = Intent(AndroidSettings.ACTION_USAGE_ACCESS_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                return
            }

            val isTranslatorForeground = isTranslatorAppInForeground()
            Log.d(TAG, "isTranslatorForeground: $isTranslatorForeground")

            if (isTranslatorForeground) {
                Log.i(TAG, "Translator app is in foreground, performing dismiss action: $dismissAction")
                performDismissAction(dismissAction)

                // Provide visual feedback
                bubbleView?.animate()
                    ?.scaleX(0.8f)
                    ?.scaleY(0.8f)
                    ?.setDuration(100)
                    ?.withEndAction {
                        bubbleView?.animate()
                            ?.scaleX(1f)
                            ?.scaleY(1f)
                            ?.setDuration(100)
                            ?.start()
                    }
                    ?.start()
                return
            } else {
                Log.d(TAG, "Translator NOT in foreground, will take screenshot instead")
            }
        } else {
            Log.d(TAG, "dismissAction is NOTHING, skipping translator check")
        }

        Log.i(TAG, "Bubble clicked, taking screenshot")

        // Hide the bubble temporarily before taking screenshot
        bubbleView?.visibility = View.INVISIBLE

        // Small delay to ensure bubble is hidden, then take screenshot
        Handler(Looper.getMainLooper()).postDelayed({
            // Directly call the accessibility service via component name
            val intent = Intent(this, ScreenTranslatorAccessibilityService::class.java)
            intent.action = "TAKE_SCREENSHOT"
            try {
                startService(intent)
                Log.i(TAG, "Screenshot intent sent")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send intent", e)
                showToast("Failed to trigger translation: ${e.message}")
            }

            // Show bubble again after a delay
            Handler(Looper.getMainLooper()).postDelayed({
                bubbleView?.visibility = View.VISIBLE
            }, 2000)
        }, 100)

        // Optional: Provide visual feedback
        bubbleView?.animate()
            ?.scaleX(0.8f)
            ?.scaleY(0.8f)
            ?.setDuration(100)
            ?.withEndAction {
                bubbleView?.animate()
                    ?.scaleX(1f)
                    ?.scaleY(1f)
                    ?.setDuration(100)
                    ?.start()
            }
            ?.start()
    }

    private fun showToast(message: String) {
        Toast.makeText(application, message, Toast.LENGTH_LONG).show()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_BUBBLE) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        bubbleView?.let {
            windowManager.removeView(it)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
