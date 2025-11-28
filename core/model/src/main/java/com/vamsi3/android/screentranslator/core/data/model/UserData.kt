package com.vamsi3.android.screentranslator.core.data.model

import kotlin.time.Duration

/**
 * Class summarizing user interest data
 */
data class UserData(
    val notificationShadeCollapseDelayDuration: Duration,
    val themeMode: ThemeMode,
    val translateApp: TranslateApp,
    val bubbleBackgroundColor: String = "#00000000",
    val bubbleBorderColor: String = "#4CAF50",
    val bubbleIconColor: String = "#FFFFFF",
    val bubbleSizeDp: Int = 56,
    val bubbleSnapToEdge: Boolean = true,
    val tileActionMode: TileActionMode = TileActionMode.SCREENSHOT,
    val translatorDismissAction: TranslatorDismissAction = TranslatorDismissAction.NOTHING,
)
