package com.vamsi3.android.screentranslator.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vamsi3.android.screentranslator.core.data.model.ThemeMode
import com.vamsi3.android.screentranslator.core.data.model.TileActionMode
import com.vamsi3.android.screentranslator.core.data.model.TranslateApp
import com.vamsi3.android.screentranslator.core.data.model.TranslatorDismissAction
import com.vamsi3.android.screentranslator.core.data.repository.UserDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userDataRepository: UserDataRepository,
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> =
        userDataRepository.userData
            .filter { it != null }
            .map { userData ->
                SettingsUiState.Success(
                    settingsData = SettingsData(
                        userData!!.themeMode,
                        userData.translateApp,
                        userData.notificationShadeCollapseDelayDuration.inWholeMilliseconds,
                        userData.bubbleBackgroundColor,
                        userData.bubbleBorderColor,
                        userData.bubbleIconColor,
                        userData.bubbleSizeDp,
                        userData.bubbleSnapToEdge,
                        userData.tileActionMode,
                        userData.translatorDismissAction,
                    )
                )
            }
            .onEmpty {
                SettingsUiState.Success(
                    SettingsData(
                        ThemeMode.default,
                        TranslateApp.default,
                        0L,
                        "#00000000",
                        "#4CAF50",
                        "#FFFFFF",
                        56,
                        true,
                        TileActionMode.default,
                        TranslatorDismissAction.default,
                    )
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = SettingsUiState.Loading
            )

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            userDataRepository.setThemeMode(themeMode)
        }
    }

    fun setTranslateApp(translateApp: TranslateApp) {
        viewModelScope.launch {
            userDataRepository.setTranslateApp(translateApp)
        }
    }

    fun setNotificationShadeCollapseDelayDuration(notificationShadeCollapseDelayDuration: Long) {
        viewModelScope.launch {
            userDataRepository.setNotificationShadeCollapseDelayDuration(notificationShadeCollapseDelayDuration.milliseconds)
        }
    }

    fun setBubbleBackgroundColor(color: String) {
        viewModelScope.launch {
            userDataRepository.setBubbleBackgroundColor(color)
        }
    }

    fun setBubbleBorderColor(color: String) {
        viewModelScope.launch {
            userDataRepository.setBubbleBorderColor(color)
        }
    }

    fun setBubbleIconColor(color: String) {
        viewModelScope.launch {
            userDataRepository.setBubbleIconColor(color)
        }
    }

    fun setBubbleSizeDp(sizeDp: Int) {
        viewModelScope.launch {
            userDataRepository.setBubbleSizeDp(sizeDp)
        }
    }

    fun setBubbleSnapToEdge(snapToEdge: Boolean) {
        viewModelScope.launch {
            userDataRepository.setBubbleSnapToEdge(snapToEdge)
        }
    }

    fun setTileActionMode(mode: TileActionMode) {
        viewModelScope.launch {
            userDataRepository.setTileActionMode(mode)
        }
    }

    fun setTranslatorDismissAction(action: TranslatorDismissAction) {
        viewModelScope.launch {
            userDataRepository.setTranslatorDismissAction(action)
        }
    }
}

data class SettingsData(
    val themeMode: ThemeMode,
    val translateApp: TranslateApp,
    val notificationShadeCollapseDelayDuration: Long,
    val bubbleBackgroundColor: String,
    val bubbleBorderColor: String,
    val bubbleIconColor: String,
    val bubbleSizeDp: Int,
    val bubbleSnapToEdge: Boolean,
    val tileActionMode: TileActionMode,
    val translatorDismissAction: TranslatorDismissAction,
)

sealed interface SettingsUiState {
    object Loading : SettingsUiState
    data class Success(val settingsData: SettingsData) : SettingsUiState
}
