package com.archstarter.app

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import com.archstarter.core.common.app.NavigationActions as NavigationActionsApi
import com.archstarter.feature.settings.api.Settings

class NavigationActions(
    private val navController: NavHostController
) : NavigationActionsApi {

    override fun openSettings() {
        navController.navigate(Settings) {
            launchSingleTop = true
        }
    }

    override fun openLink(url: String) {
        if (url.isBlank()) return
        val context = navController.context
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        context.startActivity(intent)
    }

    override fun openWallpaperPreview() {
        val context = navController.context
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(context, DayNightWallpaperService::class.java)
            )
        }
        context.startActivity(intent)
    }
}
