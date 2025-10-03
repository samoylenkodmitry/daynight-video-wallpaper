package com.archstarter.core.common.wallpaper

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private const val DATA_STORE_NAME = "daynight_wallpaper"
private val Context.wallpaperDataStore: DataStore<Preferences> by preferencesDataStore(name = DATA_STORE_NAME)

/**
 * Persists wallpaper preferences using DataStore and exposes them as a cold [Flow].
 */
@Singleton
class WallpaperPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore = context.wallpaperDataStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val settingsFlow: Flow<WallpaperSettings> = dataStore.data
        .catch { throwable ->
            if (throwable is java.io.IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { preferences ->
            val scheduleMode = preferences[SCHEDULE_MODE_KEY]?.let(WallpaperScheduleMode::valueOf)
                ?: WallpaperScheduleMode.SOLAR
            val mute = preferences[MUTE_KEY] ?: true
            val loop = preferences[LOOP_KEY] ?: true
            val schedules = DaySlot.values().associateWith { slot ->
                preferences[startKey(slot)] ?: defaultSlotSchedule.getValue(slot)
            }
            val configs = DaySlot.values().associateWith { slot ->
                val uri = preferences[slotKey(slot)]?.let { Uri.parse(it) }
                SlotConfiguration(slot, uri)
            }
            WallpaperSettings(
                scheduleMode = scheduleMode,
                slotConfigurations = configs,
                slotSchedules = schedules,
                mutePlayback = mute,
                loopPlayback = loop,
            )
        }

    val settings = settingsFlow.stateIn(
        scope,
        SharingStarted.Eagerly,
        WallpaperSettings()
    )

    suspend fun setVideo(slot: DaySlot, uri: Uri) {
        persistUriPermission(uri)
        dataStore.edit { prefs ->
            prefs[slotKey(slot)] = uri.toString()
        }
    }

    suspend fun clearVideo(slot: DaySlot) {
        dataStore.edit { prefs ->
            prefs.remove(slotKey(slot))
        }
    }

    suspend fun setMute(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[MUTE_KEY] = enabled
        }
    }

    suspend fun setLoop(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[LOOP_KEY] = enabled
        }
    }

    suspend fun setScheduleMode(mode: WallpaperScheduleMode) {
        dataStore.edit { prefs ->
            prefs[SCHEDULE_MODE_KEY] = mode.name
        }
    }

    suspend fun setStartMinutes(slot: DaySlot, minutes: Int) {
        dataStore.edit { prefs ->
            prefs[startKey(slot)] = minutes.coerceIn(0, 24 * 60 - 1)
        }
    }

    private fun persistUriPermission(uri: Uri) {
        val flags = IntentFlags.READ_PERSISTABLE
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, flags)
        }
    }

    private object IntentFlags {
        const val READ_PERSISTABLE =
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
    }

    private fun slotKey(slot: DaySlot) = stringPreferencesKey("slot_${slot.name.lowercase()}")
    private fun startKey(slot: DaySlot) = intPreferencesKey("start_${slot.name.lowercase()}")

    private companion object Keys {
        val SCHEDULE_MODE_KEY = stringPreferencesKey("schedule_mode")
        val MUTE_KEY = booleanPreferencesKey("mute")
        val LOOP_KEY = booleanPreferencesKey("loop")
    }
}
