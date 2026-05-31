package com.vamsi3.android.screentranslator

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.vamsi3.android.screentranslator.core.data.model.TranslateApp
import com.vamsi3.android.screentranslator.core.data.repository.UserDataRepository
import com.vamsi3.android.screentranslator.feature.translate.MIME_TYPE_JPEG
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import com.vamsi3.android.screentranslator.core.resource.R as CoreR

@AndroidEntryPoint
class MediaProjectionCaptureService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Inject
    lateinit var userDataRepository: UserDataRepository

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(EXTRA_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_DATA)
        }

        if (resultCode == 0 || data == null) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        serviceScope.launch {
            captureAndTranslate(resultCode, data)
            stopSelf(startId)
        }

        return START_NOT_STICKY
    }

    private fun startAsForeground() {
        createNotificationChannel()
        val notification = createNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Translator Capture",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Captures the screen for translation"
                setShowBadge(false)
            }

            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Translator")
            .setContentText("Capturing screen for translation")
            .setSmallIcon(CoreR.drawable.ic_translate_tile)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private suspend fun captureAndTranslate(resultCode: Int, data: Intent) {
        val mediaProjectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)

        val bitmap = try {
            withTimeoutOrNull(3_000) {
                captureScreen(mediaProjection)
            }
        } catch (e: Exception) {
            Log.e(TAG, "MediaProjection capture failed", e)
            null
        } finally {
            mediaProjection.stop()
        }

        if (bitmap == null) {
            Toast.makeText(this, "Translation failed", Toast.LENGTH_LONG).show()
            return
        }

        forwardScreenshotToTranslateApp(writeBitmapToFile(bitmap))
    }

    private suspend fun captureScreen(mediaProjection: MediaProjection): Bitmap? {
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val densityDpi = metrics.densityDpi
        val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        val handler = Handler(Looper.getMainLooper())

        return suspendCancellableCoroutine { continuation ->
            var resumed = false
            var virtualDisplay: android.hardware.display.VirtualDisplay? = null

            mediaProjection.registerCallback(
                object : MediaProjection.Callback() {
                    override fun onStop() {
                        imageReader.close()
                        virtualDisplay?.release()
                    }
                },
                handler
            )

            virtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenTranslatorCapture",
                width,
                height,
                densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.surface,
                null,
                handler
            )

            imageReader.setOnImageAvailableListener({ reader ->
                if (resumed) return@setOnImageAvailableListener

                val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                val bitmap = image.use {
                    val plane = it.planes.first()
                    val buffer = plane.buffer
                    val pixelStride = plane.pixelStride
                    val rowStride = plane.rowStride
                    val rowPadding = rowStride - pixelStride * width
                    val paddedWidth = width + rowPadding / pixelStride
                    val paddedBitmap = Bitmap.createBitmap(
                        paddedWidth,
                        height,
                        Bitmap.Config.ARGB_8888
                    )
                    paddedBitmap.copyPixelsFromBuffer(buffer)
                    Bitmap.createBitmap(paddedBitmap, 0, 0, width, height)
                }

                resumed = true
                reader.setOnImageAvailableListener(null, null)
                virtualDisplay?.release()
                reader.close()
                continuation.resume(bitmap) {}
            }, handler)

            continuation.invokeOnCancellation {
                imageReader.setOnImageAvailableListener(null, null)
                virtualDisplay?.release()
                imageReader.close()
            }
        }
    }

    private fun writeBitmapToFile(bitmap: Bitmap): File {
        cacheDir
            .listFiles { _, filename -> filename.endsWith(".jpg") }
            ?.forEach { file -> file.delete() }

        val file = File(cacheDir, UUID.randomUUID().toString() + ".jpg")
            .apply { if (!exists()) createNewFile() }

        FileOutputStream(file).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        }

        return file
    }

    private suspend fun forwardScreenshotToTranslateApp(file: File) {
        val translateApp = userDataRepository.userData.firstOrNull()?.translateApp
            ?: TranslateApp.default

        val uri = FileProvider.getUriForFile(
            this,
            "$packageName.fileProvider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND)
            .setType(MIME_TYPE_JPEG)
            .setPackage(translateApp.packageName)
            .setFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
                    or Intent.FLAG_ACTIVITY_NEW_DOCUMENT
                    or Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP
                    or Intent.FLAG_ACTIVITY_FORWARD_RESULT
                    or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                    or Intent.FLAG_ACTIVITY_NEW_TASK
            )
            .putExtra(Intent.EXTRA_STREAM, uri)

        if (translateApp.imageTranslateActivity.isNotBlank()) {
            intent.setClassName(translateApp.packageName, translateApp.imageTranslateActivity)
        }

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Please install ${translateApp.appName}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_DATA = "extra_data"
        private const val TAG = "MediaProjectionCapture"
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "media_projection_capture_channel"
    }
}
