package com.archstarter.feature.catalog.api

import android.net.Uri
import com.archstarter.core.common.presenter.ParamInit
import com.archstarter.core.common.wallpaper.DaySlot
import com.archstarter.core.common.wallpaper.WallpaperScheduleMode
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
data object WallpaperHome

data class SlotCardState(
    val slot: DaySlot,
    val title: String,
    val videoLabel: String?,
    val description: String,
)

data class WallpaperHomeState(
    val slots: List<SlotCardState> = emptyList(),
    val scheduleMode: WallpaperScheduleMode = WallpaperScheduleMode.SOLAR,
    val scheduleSummary: String = "",
    val mutePlayback: Boolean = true,
    val loopPlayback: Boolean = true,
    val isLoading: Boolean = true,
)

interface WallpaperHomePresenter : ParamInit<Unit> {
    val state: StateFlow<WallpaperHomeState>
    fun onPickVideo(slot: DaySlot, uri: Uri)
    fun onRemoveVideo(slot: DaySlot)
    fun onToggleMute()
    fun onToggleLoop()
    fun onOpenSettings()
    fun onSetWallpaper()
}
