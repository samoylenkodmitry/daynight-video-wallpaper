package com.archstarter.core.common.wallpaper

import android.net.Uri

/**
 * Information about a single slot in the day-night rotation.
 */
data class SlotConfiguration(
    val slot: DaySlot,
    val videoUri: Uri?,
) {
    val videoLabel: String?
        get() = videoUri?.lastPathSegment
}

/**
 * Schedule entry representing when a [DaySlot] starts, in minutes from midnight.
 */
data class SlotSchedule(
    val slot: DaySlot,
    val startMinutes: Int,
)

/**
 * User configurable wallpaper behaviour stored in DataStore.
 */
data class WallpaperSettings(
    val scheduleMode: WallpaperScheduleMode = WallpaperScheduleMode.SOLAR,
    val slotConfigurations: Map<DaySlot, SlotConfiguration> = DaySlot.values().associateWith { SlotConfiguration(it, null) },
    val slotSchedules: Map<DaySlot, Int> = defaultSlotSchedule,
    val mutePlayback: Boolean = true,
    val loopPlayback: Boolean = true,
) {
    companion object {
        val defaultSlotSchedule: Map<DaySlot, Int> = mapOf(
            DaySlot.MORNING to 6 * 60,
            DaySlot.DAY to 12 * 60,
            DaySlot.EVENING to 18 * 60,
            DaySlot.NIGHT to 21 * 60,
        )
    }
}

val defaultSlotSchedule: Map<DaySlot, Int> = WallpaperSettings.defaultSlotSchedule
