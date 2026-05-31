package com.vamsi3.android.screentranslator.core.datastore

import android.util.Log
import androidx.datastore.core.DataStore
import com.vamsi3.android.screentranslator.core.data.model.ThemeMode
import com.vamsi3.android.screentranslator.core.data.model.TranslateApp
import com.vamsi3.android.screentranslator.core.data.model.TranslatorDismissAction
import com.vamsi3.android.screentranslator.core.data.model.UserData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class UserPreferencesDataSource @Inject constructor(
    private val userPreferencesProto: DataStore<UserPreferencesProto>,
) {
    val userData = userPreferencesProto.data
        .map {
            UserData(
                notificationShadeCollapseDelayDuration = it.notificationShadeCollapseDelayMilliseconds.milliseconds,
                themeMode = when(it.userThemePreference) {
                    null,
                    UserThemePreferenceProto.UNRECOGNIZED,
                    UserThemePreferenceProto.THEME_UNSPECIFIED,
                    UserThemePreferenceProto.THEME_SYSTEM -> ThemeMode.SYSTEM
                    UserThemePreferenceProto.THEME_DARK -> ThemeMode.DARK
                    UserThemePreferenceProto.THEME_LIGHT -> ThemeMode.LIGHT
                },
                translateApp = when(it.userTranslateAppPreference) {
                    null,
                    UserTranslateAppPreferenceProto.UNRECOGNIZED,
                    UserTranslateAppPreferenceProto.TRANSLATE_APP_UNSPECIFIED,
                    UserTranslateAppPreferenceProto.TRANSLATE_APP_GOOGLE -> TranslateApp.GOOGLE

                    UserTranslateAppPreferenceProto.TRANSLATE_APP_DEEPL_TRANSLATE -> TranslateApp.DEEPL_TRANSLATE
                    UserTranslateAppPreferenceProto.TRANSLATE_APP_NAVER_PAPAGO -> TranslateApp.NAVER_PAPAGO
                },
                bubbleBackgroundColor = it.bubbleBackgroundColor.ifEmpty { "#00000000" },
                bubbleBorderColor = it.bubbleBorderColor.ifEmpty { "#4CAF50" },
                bubbleIconColor = it.bubbleIconColor.ifEmpty { "#FFFFFF" },
                bubbleSizeDp = if (it.bubbleSizeDp > 0) it.bubbleSizeDp else 56,
                bubbleSnapToEdge = it.bubbleSnapToEdge,
                bubblePositionX = it.bubblePositionX,
                bubblePositionY = if (it.bubblePositionY > 0) it.bubblePositionY else 100,
                tileActionMode = when(it.tileActionMode) {
                    null,
                    com.vamsi3.android.screentranslator.core.datastore.TileActionMode.UNRECOGNIZED,
                    com.vamsi3.android.screentranslator.core.datastore.TileActionMode.TILE_ACTION_UNSPECIFIED,
                    com.vamsi3.android.screentranslator.core.datastore.TileActionMode.TILE_ACTION_SCREENSHOT ->
                        com.vamsi3.android.screentranslator.core.data.model.TileActionMode.SCREENSHOT
                    com.vamsi3.android.screentranslator.core.datastore.TileActionMode.TILE_ACTION_TOGGLE_BUBBLE ->
                        com.vamsi3.android.screentranslator.core.data.model.TileActionMode.TOGGLE_BUBBLE
                },
                translatorDismissAction = when(it.translatorDismissAction) {
                    com.vamsi3.android.screentranslator.core.datastore.TranslatorDismissAction.TRANSLATOR_DISMISS_GO_BACK ->
                        TranslatorDismissAction.GO_BACK
                    com.vamsi3.android.screentranslator.core.datastore.TranslatorDismissAction.TRANSLATOR_DISMISS_GO_HOME ->
                        TranslatorDismissAction.GO_HOME
                    com.vamsi3.android.screentranslator.core.datastore.TranslatorDismissAction.TRANSLATOR_DISMISS_KILL_APP ->
                        TranslatorDismissAction.KILL_APP
                    else -> TranslatorDismissAction.NOTHING
                },
            )
        }
        .stateIn(
            scope = CoroutineScope(Dispatchers.IO),
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    suspend fun setNotificationShadeCollapseDelayDuration(notificationShadeCollapseDelayDuration: Duration) {
        userPreferencesProto.updateData {
            it.copy {
                Log.i("ScreenTranslator", "updating to $notificationShadeCollapseDelayDuration")
                this.notificationShadeCollapseDelayMilliseconds = notificationShadeCollapseDelayDuration.inWholeMilliseconds.toInt()
            }
        }
    }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        userPreferencesProto.updateData {
            it.copy {
                this.userThemePreference = when (themeMode) {
                    ThemeMode.SYSTEM -> UserThemePreferenceProto.THEME_SYSTEM
                    ThemeMode.LIGHT -> UserThemePreferenceProto.THEME_LIGHT
                    ThemeMode.DARK -> UserThemePreferenceProto.THEME_DARK
                }
            }
        }
    }

    suspend fun setTranslateApp(translateApp: TranslateApp) {
        userPreferencesProto.updateData {
            it.copy {
                this.userTranslateAppPreference = when (translateApp) {
                    TranslateApp.DEEPL_TRANSLATE -> UserTranslateAppPreferenceProto.TRANSLATE_APP_DEEPL_TRANSLATE
                    TranslateApp.GOOGLE -> UserTranslateAppPreferenceProto.TRANSLATE_APP_GOOGLE
                    TranslateApp.NAVER_PAPAGO -> UserTranslateAppPreferenceProto.TRANSLATE_APP_NAVER_PAPAGO
                }
            }
        }
    }

    suspend fun setBubbleBackgroundColor(color: String) {
        userPreferencesProto.updateData {
            it.copy {
                this.bubbleBackgroundColor = color
            }
        }
    }

    suspend fun setBubbleBorderColor(color: String) {
        userPreferencesProto.updateData {
            it.copy {
                this.bubbleBorderColor = color
            }
        }
    }

    suspend fun setBubbleIconColor(color: String) {
        userPreferencesProto.updateData {
            it.copy {
                this.bubbleIconColor = color
            }
        }
    }

    suspend fun setBubbleSizeDp(sizeDp: Int) {
        userPreferencesProto.updateData {
            it.copy {
                this.bubbleSizeDp = sizeDp
            }
        }
    }

    suspend fun setBubbleSnapToEdge(snapToEdge: Boolean) {
        userPreferencesProto.updateData {
            it.copy {
                this.bubbleSnapToEdge = snapToEdge
            }
        }
    }

    suspend fun setBubblePosition(x: Int, y: Int) {
        userPreferencesProto.updateData {
            it.copy {
                this.bubblePositionX = x
                this.bubblePositionY = y
            }
        }
    }

    suspend fun setTileActionMode(mode: com.vamsi3.android.screentranslator.core.data.model.TileActionMode) {
        userPreferencesProto.updateData {
            it.copy {
                this.tileActionMode = when (mode) {
                    com.vamsi3.android.screentranslator.core.data.model.TileActionMode.SCREENSHOT ->
                        com.vamsi3.android.screentranslator.core.datastore.TileActionMode.TILE_ACTION_SCREENSHOT
                    com.vamsi3.android.screentranslator.core.data.model.TileActionMode.TOGGLE_BUBBLE ->
                        com.vamsi3.android.screentranslator.core.datastore.TileActionMode.TILE_ACTION_TOGGLE_BUBBLE
                }
            }
        }
    }

    suspend fun setTranslatorDismissAction(action: TranslatorDismissAction) {
        userPreferencesProto.updateData {
            it.copy {
                this.translatorDismissAction = when (action) {
                    TranslatorDismissAction.NOTHING ->
                        com.vamsi3.android.screentranslator.core.datastore.TranslatorDismissAction.TRANSLATOR_DISMISS_NOTHING
                    TranslatorDismissAction.GO_BACK ->
                        com.vamsi3.android.screentranslator.core.datastore.TranslatorDismissAction.TRANSLATOR_DISMISS_GO_BACK
                    TranslatorDismissAction.GO_HOME ->
                        com.vamsi3.android.screentranslator.core.datastore.TranslatorDismissAction.TRANSLATOR_DISMISS_GO_HOME
                    TranslatorDismissAction.KILL_APP ->
                        com.vamsi3.android.screentranslator.core.datastore.TranslatorDismissAction.TRANSLATOR_DISMISS_KILL_APP
                }
            }
        }
    }
}
