package com.vamsi3.android.screentranslator.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vamsi3.android.screentranslator.core.data.model.ThemeMode
import com.vamsi3.android.screentranslator.core.data.model.TileActionMode
import com.vamsi3.android.screentranslator.core.data.model.TranslateApp
import com.vamsi3.android.screentranslator.core.data.model.TranslatorDismissAction
import com.vamsi3.android.screentranslator.core.design.theme.ScreenTranslatorTheme
import com.vamsi3.android.screentranslator.core.design.util.ThemePreviews
import com.vamsi3.android.screentranslator.feature.settings.SettingsUiState.Success

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    isBubbleRunning: Boolean,
    onBubbleToggle: (Boolean) -> Unit,
    onBubbleRestart: () -> Unit,
    onRequestAddTile: () -> Unit,
) {
    val settingsUiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (settingsUiState is Success) {
        SettingsContent(
            settingsData = (settingsUiState as Success).settingsData,
            isBubbleRunning = isBubbleRunning,
            onBubbleToggle = onBubbleToggle,
            onBubbleRestart = onBubbleRestart,
            onChangeTranslateApp = viewModel::setTranslateApp,
            onChangeThemeMode = viewModel::setThemeMode,
            onChangeNotificationShadeCollapseDelayDuration = viewModel::setNotificationShadeCollapseDelayDuration,
            onChangeBubbleSizeDp = viewModel::setBubbleSizeDp,
            onChangeBubbleBackgroundColor = viewModel::setBubbleBackgroundColor,
            onChangeBubbleBorderColor = viewModel::setBubbleBorderColor,
            onChangeBubbleIconColor = viewModel::setBubbleIconColor,
            onChangeBubbleSnapToEdge = viewModel::setBubbleSnapToEdge,
            onChangeTileActionMode = viewModel::setTileActionMode,
            onChangeTranslatorDismissAction = viewModel::setTranslatorDismissAction,
            onRequestAddTile = onRequestAddTile,
        )
    }
}

@Composable
fun SettingsContent(
    settingsData: SettingsData,
    isBubbleRunning: Boolean,
    onBubbleToggle: (Boolean) -> Unit,
    onBubbleRestart: () -> Unit,
    onChangeTranslateApp: (TranslateApp) -> Unit,
    onChangeThemeMode: (ThemeMode) -> Unit,
    onChangeNotificationShadeCollapseDelayDuration: (Long) -> Unit,
    onChangeBubbleSizeDp: (Int) -> Unit,
    onChangeBubbleBackgroundColor: (String) -> Unit,
    onChangeBubbleBorderColor: (String) -> Unit,
    onChangeBubbleIconColor: (String) -> Unit,
    onChangeBubbleSnapToEdge: (Boolean) -> Unit,
    onChangeTileActionMode: (TileActionMode) -> Unit,
    onChangeTranslatorDismissAction: (TranslatorDismissAction) -> Unit,
    onRequestAddTile: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
    ) {
        // 1. Translate App (top)
        TranslateAppSetting(
            translateApp = settingsData.translateApp,
            onChangeTranslateApp = onChangeTranslateApp
        )

        // 2. Floating Bubble (toggle + customization)
        FloatingBubbleSection(
            isBubbleRunning = isBubbleRunning,
            onBubbleToggle = onBubbleToggle,
            bubbleSizeDp = settingsData.bubbleSizeDp,
            backgroundColor = settingsData.bubbleBackgroundColor,
            borderColor = settingsData.bubbleBorderColor,
            iconColor = settingsData.bubbleIconColor,
            snapToEdge = settingsData.bubbleSnapToEdge,
            translatorDismissAction = settingsData.translatorDismissAction,
            onChangeBubbleSizeDp = onChangeBubbleSizeDp,
            onChangeBackgroundColor = onChangeBubbleBackgroundColor,
            onChangeBorderColor = onChangeBubbleBorderColor,
            onChangeIconColor = onChangeBubbleIconColor,
            onChangeSnapToEdge = onChangeBubbleSnapToEdge,
            onChangeTranslatorDismissAction = onChangeTranslatorDismissAction,
            onBubbleRestart = onBubbleRestart
        )

        // 4. Quick Settings Tile Action
        TileActionModeSetting(
            tileActionMode = settingsData.tileActionMode,
            onChangeTileActionMode = onChangeTileActionMode,
            onRequestAddTile = onRequestAddTile,
            screenshotDelay = settingsData.notificationShadeCollapseDelayDuration,
            onChangeScreenshotDelay = onChangeNotificationShadeCollapseDelayDuration
        )

        // 6. Theme (bottom)
        ThemeSetting(
            themeMode = settingsData.themeMode,
            onChangeThemeMode = onChangeThemeMode
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslateAppSetting(
    translateApp: TranslateApp,
    onChangeTranslateApp: (TranslateApp) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth(),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Translate App", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Select which translation app to use",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = translateApp.appName,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .width(160.dp),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    TranslateApp.values().forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.appName) },
                            onClick = {
                                onChangeTranslateApp(option)
                                expanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingBubbleSection(
    isBubbleRunning: Boolean,
    onBubbleToggle: (Boolean) -> Unit,
    bubbleSizeDp: Int,
    backgroundColor: String,
    borderColor: String,
    iconColor: String,
    snapToEdge: Boolean,
    translatorDismissAction: TranslatorDismissAction,
    onChangeBubbleSizeDp: (Int) -> Unit,
    onChangeBackgroundColor: (String) -> Unit,
    onChangeBorderColor: (String) -> Unit,
    onChangeIconColor: (String) -> Unit,
    onChangeSnapToEdge: (Boolean) -> Unit,
    onChangeTranslatorDismissAction: (TranslatorDismissAction) -> Unit,
    onBubbleRestart: () -> Unit,
) {
    var advancedExpanded by remember { mutableStateOf(true) }

    Surface(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth(),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(backgroundColor)))
                            .border(2.dp, Color(android.graphics.Color.parseColor(borderColor)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "A",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(android.graphics.Color.parseColor(iconColor))
                        )
                    }
                    Column {
                        Text("Floating Bubble", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (isBubbleRunning) "On screen now" else "Hidden",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = isBubbleRunning,
                    onCheckedChange = onBubbleToggle,
                )
            }

            HorizontalDivider()

            BubbleSizeControl(
                bubbleSizeDp = bubbleSizeDp,
                onChangeBubbleSizeDp = { size ->
                    onChangeBubbleSizeDp(size)
                    if (isBubbleRunning) onBubbleRestart()
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onChangeSnapToEdge(!snapToEdge) }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Snap to Edge", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (snapToEdge) "Snaps after dragging and saves that edge" else "Stays exactly where you drop it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = snapToEdge,
                    onCheckedChange = onChangeSnapToEdge,
                )
            }

            TranslatorDismissActionControl(
                translatorDismissAction = translatorDismissAction,
                onChangeTranslatorDismissAction = onChangeTranslatorDismissAction
            )

            TextButton(
                onClick = { advancedExpanded = !advancedExpanded },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(if (advancedExpanded) "Hide Button Style" else "Edit Button Style")
                Icon(
                    imageVector = if (advancedExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                )
            }

            AnimatedVisibility(
                visible = advancedExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HorizontalDivider()
                    BubbleColorControl(
                        label = "Background",
                        description = "Bubble fill colour",
                        color = backgroundColor,
                        onColorChange = { color ->
                            onChangeBackgroundColor(color)
                            if (isBubbleRunning) onBubbleRestart()
                        }
                    )

                    BubbleColorControl(
                        label = "Border",
                        description = "Outer ring colour",
                        color = borderColor,
                        onColorChange = { color ->
                            onChangeBorderColor(color)
                            if (isBubbleRunning) onBubbleRestart()
                        }
                    )

                    BubbleColorControl(
                        label = "Icon",
                        description = "Translate icon colour",
                        color = iconColor,
                        onColorChange = { color ->
                            onChangeIconColor(color)
                            if (isBubbleRunning) onBubbleRestart()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BubbleSizeControl(
    bubbleSizeDp: Int,
    onChangeBubbleSizeDp: (Int) -> Unit,
) {
    var bubbleSize by remember(bubbleSizeDp) { mutableStateOf(bubbleSizeDp.toFloat()) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Size", style = MaterialTheme.typography.bodyLarge)
            Text(
                "${bubbleSize.toInt()}dp",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = bubbleSize,
            onValueChange = { bubbleSize = it },
            onValueChangeFinished = { onChangeBubbleSizeDp(bubbleSize.toInt()) },
            valueRange = 32f..128f,
            steps = 23,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun BubbleColorControl(
    label: String,
    description: String,
    color: String,
    onColorChange: (String) -> Unit,
) {
    var showColorPicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showColorPicker = !showColorPicker },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(color)))
                        .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
                Icon(
                    imageVector = if (showColorPicker) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        AnimatedVisibility(
            visible = showColorPicker,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            ColorPickerContent(
                color = color,
                onColorChange = onColorChange
            )
        }
    }
}

@Composable
fun ColorPickerContent(
    color: String,
    onColorChange: (String) -> Unit,
) {
    var alpha by remember { mutableStateOf(android.graphics.Color.alpha(android.graphics.Color.parseColor(color)) / 255f) }
    var red by remember { mutableStateOf(android.graphics.Color.red(android.graphics.Color.parseColor(color)) / 255f) }
    var green by remember { mutableStateOf(android.graphics.Color.green(android.graphics.Color.parseColor(color)) / 255f) }
    var blue by remember { mutableStateOf(android.graphics.Color.blue(android.graphics.Color.parseColor(color)) / 255f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(red, green, blue, alpha))
                .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
        )

        ColorSlider("Alpha", alpha) {
            alpha = it
            val newColor = Color(red, green, blue, alpha).toArgb()
            onColorChange(String.format("#%08X", newColor))
        }
        ColorSlider("Red", red) {
            red = it
            val newColor = Color(red, green, blue, alpha).toArgb()
            onColorChange(String.format("#%08X", newColor))
        }
        ColorSlider("Green", green) {
            green = it
            val newColor = Color(red, green, blue, alpha).toArgb()
            onColorChange(String.format("#%08X", newColor))
        }
        ColorSlider("Blue", blue) {
            blue = it
            val newColor = Color(red, green, blue, alpha).toArgb()
            onColorChange(String.format("#%08X", newColor))
        }
    }
}

@Composable
private fun ColorSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
) {
    // Track local slider value for visual feedback during drag
    var sliderValue by remember(value) { mutableStateOf(value) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(40.dp)
        )
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },  // Update local state during drag
            onValueChangeFinished = { onValueChange(sliderValue) },  // Commit on release
            modifier = Modifier.weight(1f)
        )
        Text(
            "${(sliderValue * 255).toInt()}",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(30.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorDismissActionControl(
    translatorDismissAction: TranslatorDismissAction,
    onChangeTranslatorDismissAction: (TranslatorDismissAction) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    val actionLabel = when (translatorDismissAction) {
        TranslatorDismissAction.NOTHING -> "Nothing (translate again)"
        TranslatorDismissAction.GO_BACK -> "Go Back"
        TranslatorDismissAction.GO_HOME -> "Go Home"
        TranslatorDismissAction.KILL_APP -> "Close Translator"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("If Translator is Open", style = MaterialTheme.typography.bodyLarge)
        Text(
            "Action when bubble is tapped while translator app is in foreground",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = actionLabel,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = {
                        Column {
                            Text("Nothing (translate again)")
                            Text(
                                "Take another screenshot",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onChangeTranslatorDismissAction(TranslatorDismissAction.NOTHING)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
                DropdownMenuItem(
                    text = {
                        Column {
                            Text("Go Back")
                            Text(
                                "Press back button in translator",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onChangeTranslatorDismissAction(TranslatorDismissAction.GO_BACK)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
                DropdownMenuItem(
                    text = {
                        Column {
                            Text("Go Home")
                            Text(
                                "Return to home screen",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onChangeTranslatorDismissAction(TranslatorDismissAction.GO_HOME)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
                DropdownMenuItem(
                    text = {
                        Column {
                            Text("Close Translator")
                            Text(
                                "Kill translator app and return to previous app",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onChangeTranslatorDismissAction(TranslatorDismissAction.KILL_APP)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@Composable
fun TileActionModeSetting(
    tileActionMode: TileActionMode,
    onChangeTileActionMode: (TileActionMode) -> Unit,
    onRequestAddTile: () -> Unit,
    screenshotDelay: Long,
    onChangeScreenshotDelay: (Long) -> Unit,
) {
    var delayDuration by remember { mutableStateOf(screenshotDelay) }

    Surface(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth(),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Quick Settings Tile", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Choose what the Quick Settings tile does",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onRequestAddTile) {
                    Text("Add Tile")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Screenshot option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onChangeTileActionMode(TileActionMode.SCREENSHOT) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = tileActionMode == TileActionMode.SCREENSHOT,
                    onClick = { onChangeTileActionMode(TileActionMode.SCREENSHOT) }
                )
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text("Take Screenshot", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Capture screen and translate text",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Toggle Bubble option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onChangeTileActionMode(TileActionMode.TOGGLE_BUBBLE) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = tileActionMode == TileActionMode.TOGGLE_BUBBLE,
                    onClick = { onChangeTileActionMode(TileActionMode.TOGGLE_BUBBLE) }
                )
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text("Toggle Bubble", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Show or hide the floating bubble",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Screenshot Delay (only relevant for Screenshot mode)
            if (tileActionMode == TileActionMode.SCREENSHOT) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    "Screenshot Delay",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Wait for notification shade to collapse",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Slider(
                        value = delayDuration.toFloat(),
                        onValueChange = { delayDuration = it.toLong() },
                        onValueChangeFinished = {
                            onChangeScreenshotDelay(delayDuration)
                        },
                        valueRange = 0f..1000f,
                        steps = 9,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (delayDuration == 0L) "Off" else "${delayDuration}ms",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (delayDuration == 0L)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(60.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSetting(
    themeMode: ThemeMode,
    onChangeThemeMode: (ThemeMode) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth(),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Theme", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Choose light, dark, or system default",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = themeMode.modeName,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .width(130.dp),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    ThemeMode.values().forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.modeName) },
                            onClick = {
                                onChangeThemeMode(option)
                                expanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationShadeCollapseSetting(
    notificationShadeCollapseDelayDuration: Long,
    onChangeNotificationShadeCollapseDelayDuration: (Long) -> Unit,
) {
    Surface(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth(),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(8.dp),
    ) {
        var delayDuration by remember { mutableStateOf(notificationShadeCollapseDelayDuration) }

        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text("Tile Screenshot Delay", style = MaterialTheme.typography.titleMedium)
            Text(
                "Wait for notification shade to collapse before screenshot",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Slider(
                    value = delayDuration.toFloat(),
                    onValueChange = { delayDuration = it.toLong() },
                    onValueChangeFinished = {
                        onChangeNotificationShadeCollapseDelayDuration(delayDuration)
                    },
                    valueRange = 0f..1000f,
                    steps = 9,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (delayDuration == 0L) "Off" else "${delayDuration}ms",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (delayDuration == 0L)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(60.dp)
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun SettingsPreview() {
    ScreenTranslatorTheme {
        Surface {
            SettingsContent(
                settingsData = SettingsData(
                    themeMode = ThemeMode.SYSTEM,
                    translateApp = TranslateApp.GOOGLE,
                    notificationShadeCollapseDelayDuration = 300L,
                    bubbleBackgroundColor = "#00000000",
                    bubbleBorderColor = "#4CAF50",
                    bubbleIconColor = "#FFFFFF",
                    bubbleSizeDp = 56,
                    bubbleSnapToEdge = true,
                    tileActionMode = TileActionMode.SCREENSHOT,
                    translatorDismissAction = TranslatorDismissAction.NOTHING,
                ),
                isBubbleRunning = false,
                onBubbleToggle = {},
                onBubbleRestart = {},
                onChangeTranslateApp = {},
                onChangeThemeMode = {},
                onChangeNotificationShadeCollapseDelayDuration = {},
                onChangeBubbleSizeDp = {},
                onChangeBubbleBackgroundColor = {},
                onChangeBubbleBorderColor = {},
                onChangeBubbleIconColor = {},
                onChangeBubbleSnapToEdge = {},
                onChangeTileActionMode = {},
                onChangeTranslatorDismissAction = {},
                onRequestAddTile = {},
            )
        }
    }
}
