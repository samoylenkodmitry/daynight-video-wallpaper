package com.archstarter.core.common.wallpaper

/**
 * Represents a part of the day used to schedule wallpaper videos.
 */
enum class DaySlot {
    MORNING,
    DAY,
    EVENING,
    NIGHT;

    val displayName: String
        get() = when (this) {
            MORNING -> "Morning"
            DAY -> "Day"
            EVENING -> "Evening"
            NIGHT -> "Night"
        }
}
