package com.vamsi3.android.screentranslator

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.vamsi3.android.screentranslator.core.ui.HomeScreen
import com.vamsi3.android.screentranslator.feature.settings.SettingsUiState
import com.vamsi3.android.screentranslator.feature.settings.SettingsUiState.Loading
import com.vamsi3.android.screentranslator.feature.settings.SettingsViewModel
import com.vamsi3.android.screentranslator.feature.translate.ACTION_REQUEST_MEDIA_PROJECTION_SCREENSHOT
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: SettingsViewModel by viewModels()

    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK || result.data == null) {
            Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_LONG).show()
            return@registerForActivityResult
        }

        lifecycleScope.launch {
            moveTaskToBack(true)
            delay(600)
            startMediaProjectionCaptureService(result.resultCode, result.data!!)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val splashScreen = installSplashScreen()

        var uiState: SettingsUiState by mutableStateOf(Loading)

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState
                    .collectLatest {
                        uiState = it
                    }
            }
        }

        splashScreen.setKeepOnScreenCondition { uiState == Loading }

        setContent {
            HomeScreen()
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == ACTION_REQUEST_MEDIA_PROJECTION_SCREENSHOT) {
            requestMediaProjectionScreenshot()
        }
    }

    private fun requestMediaProjectionScreenshot() {
        val mediaProjectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjectionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
    }

    private fun startMediaProjectionCaptureService(resultCode: Int, data: Intent) {
        val intent = Intent(this, MediaProjectionCaptureService::class.java)
            .putExtra(MediaProjectionCaptureService.EXTRA_RESULT_CODE, resultCode)
            .putExtra(MediaProjectionCaptureService.EXTRA_DATA, data)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, intent)
        } else {
            startService(intent)
        }
    }
}
