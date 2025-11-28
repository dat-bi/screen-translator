package com.vamsi3.android.screentranslator.core.ui

import android.app.StatusBarManager
import android.content.ComponentName
import android.graphics.drawable.Icon
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vamsi3.android.screentranslator.core.data.model.ThemeMode
import com.vamsi3.android.screentranslator.core.design.theme.ScreenTranslatorTheme
import com.vamsi3.android.screentranslator.core.design.util.ThemePreviews
import com.vamsi3.android.screentranslator.core.resource.R
import com.vamsi3.android.screentranslator.feature.settings.SettingsScreen
import com.vamsi3.android.screentranslator.feature.settings.SettingsUiState.Loading
import com.vamsi3.android.screentranslator.feature.settings.SettingsUiState.Success
import com.vamsi3.android.screentranslator.feature.settings.SettingsViewModel
import java.util.concurrent.Executor

@Composable
fun HomeScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settingsUiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (settingsUiState) {
        Loading -> {
            Text(
                text = "Loading...",
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
        is Success -> {
            val darkTheme = when ((settingsUiState as Success).settingsData.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            val context = LocalContext.current
            var isBubbleEnabled by remember {
                mutableStateOf(BubbleOverlayManager.isBubbleServiceRunning(context))
            }

            ScreenTranslatorTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        val uriHandler = LocalUriHandler.current
                        val gitHubRepoUrl = stringResource(R.string.top_bar_favorite_url)

                        ScreenTranslatorTopAppBar(
                            titleRes = R.string.app_name,
                            actionIcon = Icons.Filled.Favorite,
                            actionIconContentDescription = stringResource(R.string.top_bar_favorite_description),
                            onActionClick = { uriHandler.openUri(gitHubRepoUrl) }
                        )

                        // Usage Guide (expandable)
                        var howToUseExpanded by remember { mutableStateOf(true) }
                        Surface(
                            modifier = Modifier
                                .padding(8.dp)
                                .fillMaxWidth(),
                            tonalElevation = 2.dp,
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { howToUseExpanded = !howToUseExpanded }
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Text(
                                        "How to use",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Icon(
                                        imageVector = if (howToUseExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (howToUseExpanded) "Collapse" else "Expand",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                AnimatedVisibility(
                                    visible = howToUseExpanded,
                                    enter = expandVertically(),
                                    exit = shrinkVertically()
                                ) {
                                    Text(
                                        buildAnnotatedString {
                                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                                append("Option 1:")
                                            }
                                            append(" Floating Bubble — Enable below, tap to translate.\n")
                                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                                append("Option 2:")
                                            }
                                            append(" Quick Settings Tile — Press \"Add Tile\", tap to translate.")
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                                    )
                                }
                            }
                        }

                        // All Settings
                        SettingsScreen(
                            viewModel = viewModel,
                            isBubbleRunning = isBubbleEnabled,
                            onBubbleToggle = { enabled ->
                                if (enabled) {
                                    if (BubbleOverlayManager.hasOverlayPermission(context)) {
                                        try {
                                            val serviceClass = Class.forName(
                                                "com.vamsi3.android.screentranslator.feature.translate.BubbleOverlayService"
                                            )
                                            BubbleOverlayManager.startBubbleService(context, serviceClass)
                                            isBubbleEnabled = true
                                        } catch (e: Exception) {
                                            // Service not found
                                        }
                                    } else {
                                        BubbleOverlayManager.requestOverlayPermission(context)
                                    }
                                } else {
                                    try {
                                        val serviceClass = Class.forName(
                                            "com.vamsi3.android.screentranslator.feature.translate.BubbleOverlayService"
                                        )
                                        BubbleOverlayManager.stopBubbleService(context, serviceClass)
                                        isBubbleEnabled = false
                                    } catch (e: Exception) {
                                        // Service not found
                                    }
                                }
                            },
                            onBubbleRestart = {
                                if (BubbleOverlayManager.isBubbleServiceRunning(context)) {
                                    try {
                                        val serviceClass = Class.forName(
                                            "com.vamsi3.android.screentranslator.feature.translate.BubbleOverlayService"
                                        )
                                        BubbleOverlayManager.stopBubbleService(context, serviceClass)
                                        BubbleOverlayManager.startBubbleService(context, serviceClass)
                                    } catch (e: Exception) {
                                        // Service not found
                                    }
                                }
                            },
                            onRequestAddTile = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    // Android 13+ - use system API to request adding tile
                                    try {
                                        val statusBarManager = context.getSystemService(StatusBarManager::class.java)
                                        statusBarManager.requestAddTileService(
                                            ComponentName(
                                                context.packageName,
                                                "com.vamsi3.android.screentranslator.feature.translate.ScreenTranslatorTileService"
                                            ),
                                            context.getString(R.string.app_name),
                                            Icon.createWithResource(context, R.drawable.ic_translate_tile),
                                            Executor { it.run() }
                                        ) { result ->
                                            // Result callback - tile was added or user declined
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            context,
                                            "Failed to request tile: ${e.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } else {
                                    // Older Android - show instructions
                                    Toast.makeText(
                                        context,
                                        "Pull down Quick Settings, tap edit (✎), and drag Screen Translator tile to add it",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun AppPreview() {
    HomeScreen()
}
