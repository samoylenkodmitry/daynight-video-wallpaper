package com.archstarter.feature.settings.api

import com.archstarter.core.common.presenter.ParamInit
import com.archstarter.core.common.wallpaper.DaySlot
import com.archstarter.core.common.wallpaper.WallpaperScheduleMode
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
data object Settings

data class ScheduleSlotUi(
    val slot: DaySlot,
    val title: String,
    val startMinutes: Int,
)

data class SettingsState(
    val scheduleMode: WallpaperScheduleMode = WallpaperScheduleMode.SOLAR,
    val slots: List<ScheduleSlotUi> = emptyList(),
    val description: String = "",
)

interface SettingsPresenter : ParamInit<Unit> {
    val state: StateFlow<SettingsState>
    fun onScheduleModeSelected(mode: WallpaperScheduleMode)
    fun onSlotTimeSelected(slot: DaySlot, minutes: Int)
    fun onResetDefaults()
}
