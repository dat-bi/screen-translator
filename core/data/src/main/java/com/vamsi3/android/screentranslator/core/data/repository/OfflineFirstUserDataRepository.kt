package com.vamsi3.android.screentranslator.core.data.repository

import com.vamsi3.android.screentranslator.core.data.model.ThemeMode
import com.vamsi3.android.screentranslator.core.data.model.TranslateApp
import com.vamsi3.android.screentranslator.core.datastore.UserPreferencesDataSource
import javax.inject.Inject
import kotlin.time.Duration

class OfflineFirstUserDataRepository @Inject constructor(
    private val userPreferencesDataSource: UserPreferencesDataSource
) : UserDataRepository {

    override val userData = userPreferencesDataSource.userData

    override suspend fun setThemeMode(themeMode: ThemeMode) =
        userPreferencesDataSource.setThemeMode(themeMode)

    override suspend fun setTranslateApp(translateApp: TranslateApp) =
        userPreferencesDataSource.setTranslateApp(translateApp)

    override suspend fun setNotificationShadeCollapseDelayDuration(notificationShadeCollapseDelayDuration: Duration) =
        userPreferencesDataSource.setNotificationShadeCollapseDelayDuration(notificationShadeCollapseDelayDuration)

    override suspend fun setBubbleBackgroundColor(color: String) =
        userPreferencesDataSource.setBubbleBackgroundColor(color)

    override suspend fun setBubbleBorderColor(color: String) =
        userPreferencesDataSource.setBubbleBorderColor(color)

    override suspend fun setBubbleIconColor(color: String) =
        userPreferencesDataSource.setBubbleIconColor(color)

    override suspend fun setBubbleSizeDp(sizeDp: Int) =
        userPreferencesDataSource.setBubbleSizeDp(sizeDp)

    override suspend fun setBubbleSnapToEdge(snapToEdge: Boolean) =
        userPreferencesDataSource.setBubbleSnapToEdge(snapToEdge)

    override suspend fun setBubblePosition(x: Int, y: Int) =
        userPreferencesDataSource.setBubblePosition(x, y)

    override suspend fun setTileActionMode(mode: com.vamsi3.android.screentranslator.core.data.model.TileActionMode) =
        userPreferencesDataSource.setTileActionMode(mode)

    override suspend fun setTranslatorDismissAction(action: com.vamsi3.android.screentranslator.core.data.model.TranslatorDismissAction) =
        userPreferencesDataSource.setTranslatorDismissAction(action)

}
