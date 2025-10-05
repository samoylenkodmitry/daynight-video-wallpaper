package com.archstarter.core.common.wallpaper

/**
 * How the wallpaper decides which [DaySlot] is active.
 */
enum class WallpaperScheduleMode {
    /**
     * Use solar events such as sunrise and sunset when available, otherwise fall back to a
     * reasonable default schedule.
     */
    SOLAR,

    /**
     * Use explicit user-defined start times for every slot.
     */
    FIXED,

    /**
     * Rotate through slots at a fixed interval regardless of time of day.
     */
    ROTATING,
}
