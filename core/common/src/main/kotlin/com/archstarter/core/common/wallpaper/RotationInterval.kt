package com.archstarter.core.common.wallpaper

/**
 * Defines how frequently the wallpaper should advance to the next [DaySlot] when using
 * [WallpaperScheduleMode.ROTATING].
 */
data class RotationInterval(
    val value: Int = 6,
    val unit: RotationIntervalUnit = RotationIntervalUnit.HOURS,
) {
    init {
        require(value > 0) { "Rotation interval value must be greater than zero" }
    }

    /**
     * Converts this interval to minutes.
     */
    val minutes: Int
        get() = unit.toMinutes(value)
}

/**
 * Units that a [RotationInterval] can be expressed in.
 */
enum class RotationIntervalUnit {
    MINUTES,
    HOURS,
    DAYS;

    internal fun toMinutes(value: Int): Int {
        val minutes = when (this) {
            MINUTES -> value.toLong()
            HOURS -> value.toLong() * 60
            DAYS -> value.toLong() * 24 * 60
        }
        return minutes.coerceIn(1L, Int.MAX_VALUE.toLong()).toInt()
    }

    val displayName: String
        get() = when (this) {
            MINUTES -> "minutes"
            HOURS -> "hours"
            DAYS -> "days"
        }
}
