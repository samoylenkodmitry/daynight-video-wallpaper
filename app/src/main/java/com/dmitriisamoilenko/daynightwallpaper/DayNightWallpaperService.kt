package com.dmitriisamoilenko.daynightwallpaper

import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.archstarter.core.common.wallpaper.DaySlot
import com.archstarter.core.common.wallpaper.WallpaperPreferencesRepository
import com.archstarter.core.common.wallpaper.WallpaperScheduleMode
import com.archstarter.core.common.wallpaper.WallpaperSettings
import com.archstarter.core.common.wallpaper.defaultSlotSchedule
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime

@AndroidEntryPoint
class DayNightWallpaperService : WallpaperService() {

    @Inject lateinit var preferences: WallpaperPreferencesRepository

    override fun onCreateEngine(): Engine = VideoEngine()

    inner class VideoEngine : Engine(), Player.Listener {
        private val scope = CoroutineScope(Dispatchers.Main.immediate + Job())
        private var exoPlayer: ExoPlayer? = null
        private var currentSlot: DaySlot? = null
        private var currentSettings: WallpaperSettings = WallpaperSettings()
        private var isVisible: Boolean = false

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            exoPlayer = ExoPlayer.Builder(this@DayNightWallpaperService).build().apply {
                repeatMode = Player.REPEAT_MODE_ALL
                volume = if (currentSettings.mutePlayback) 0f else 1f
                addListener(this@VideoEngine)
            }
            scope.launch {
                preferences.settings.collectLatest { settings ->
                    currentSettings = settings
                    exoPlayer?.let { player ->
                        player.volume = if (settings.mutePlayback) 0f else 1f
                        player.repeatMode = if (settings.loopPlayback) {
                            Player.REPEAT_MODE_ALL
                        } else {
                            Player.REPEAT_MODE_OFF
                        }
                    }
                    updateSlot(computeSlot(settings))
                }
            }
            scope.launch {
                while (true) {
                    delay(1.minutes)
                    updateSlot(computeSlot(currentSettings))
                }
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            isVisible = visible
            updateSlot(computeSlot(currentSettings))
            exoPlayer?.playWhenReady = visible && currentSettings.slotConfigurations[currentSlot]?.videoUri != null
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            exoPlayer?.setVideoSurface(holder.surface)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            exoPlayer?.clearVideoSurface()
        }

        override fun onDestroy() {
            super.onDestroy()
            scope.cancel()
            exoPlayer?.release()
            exoPlayer = null
        }

        private fun updateSlot(slot: DaySlot) {
            if (slot == currentSlot) return
            currentSlot = slot
            val uri = currentSettings.slotConfigurations[slot]?.videoUri
            val player = exoPlayer ?: return
            if (uri != null) {
                val mediaItem = MediaItem.fromUri(uri)
                player.setMediaItem(mediaItem)
                player.prepare()
                player.playWhenReady = isVisible
            } else {
                player.stop()
                player.clearMediaItems()
            }
        }

        private fun computeSlot(settings: WallpaperSettings): DaySlot {
            val now = LocalTime.now()
            val minutes = now.hour * 60 + now.minute
            return when (settings.scheduleMode) {
                WallpaperScheduleMode.SOLAR -> {
                    val schedule = defaultSlotSchedule
                    val ordered = DaySlot.values().sortedBy { schedule.getValue(it) }
                    ordered.lastOrNull { schedule.getValue(it) <= minutes } ?: ordered.last()
                }
                WallpaperScheduleMode.FIXED -> {
                    val schedule = settings.slotSchedules
                    val ordered = DaySlot.values().sortedBy { schedule.getValue(it) }
                    ordered.lastOrNull { schedule.getValue(it) <= minutes } ?: ordered.last()
                }
                WallpaperScheduleMode.ROTATING -> {
                    val intervalMinutes = settings.rotationInterval.minutes.coerceAtLeast(1)
                    val elapsedMinutes = Instant.now().epochSecond / 60
                    val slotIndex = ((elapsedMinutes / intervalMinutes) % DaySlot.values().size).toInt()
                    DaySlot.values()[slotIndex]
                }
            }
        }
    }
}
