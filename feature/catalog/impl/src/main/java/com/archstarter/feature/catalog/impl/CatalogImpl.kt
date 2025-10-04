package com.archstarter.feature.catalog.impl

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archstarter.core.common.app.App
import com.archstarter.core.common.presenter.PresenterProvider
import com.archstarter.core.common.scope.ScreenComponent
import com.archstarter.core.common.viewmodel.AssistedVmFactory
import com.archstarter.core.common.viewmodel.VmKey
import com.archstarter.core.common.viewmodel.scopedViewModel
import com.archstarter.core.common.wallpaper.DaySlot
import com.archstarter.core.common.wallpaper.WallpaperPreferencesRepository
import com.archstarter.core.common.wallpaper.WallpaperScheduleMode
import com.archstarter.core.common.wallpaper.WallpaperSettings
import com.archstarter.feature.catalog.api.SlotCardState
import com.archstarter.feature.catalog.api.WallpaperHomePresenter
import com.archstarter.feature.catalog.api.WallpaperHomeState
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Suppress("UnusedPrivateMember")
class WallpaperHomeViewModel @AssistedInject constructor(
    @Assisted private val savedStateHandle: SavedStateHandle,
    private val app: App,
    private val repository: WallpaperPreferencesRepository,
) : ViewModel(), WallpaperHomePresenter {
    private val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

    private val _state = MutableStateFlow(WallpaperHomeState(isLoading = true))
    override val state: StateFlow<WallpaperHomeState> = _state

    init {
        viewModelScope.launch {
            repository.settings.collect { settings ->
                _state.value = toState(settings)
            }
        }
    }

    override fun onPickVideo(slot: DaySlot, uri: Uri) {
        viewModelScope.launch { repository.setVideo(slot, uri) }
    }

    override fun onRemoveVideo(slot: DaySlot) {
        viewModelScope.launch { repository.clearVideo(slot) }
    }

    override fun onToggleMute() {
        val current = state.value
        viewModelScope.launch { repository.setMute(!current.mutePlayback) }
    }

    override fun onToggleLoop() {
        val current = state.value
        viewModelScope.launch { repository.setLoop(!current.loopPlayback) }
    }

    override fun onOpenSettings() {
        app.navigation.openSettings()
    }

    override fun onSetWallpaper() {
        app.navigation.openWallpaperPreview()
    }

    override fun initOnce(params: Unit?) = Unit

    private fun toState(settings: WallpaperSettings): WallpaperHomeState {
        val slots = DaySlot.values().map { slot ->
            val config = settings.slotConfigurations[slot]
            val description = when (settings.scheduleMode) {
                WallpaperScheduleMode.SOLAR -> solarDescription(slot)
                WallpaperScheduleMode.FIXED -> "Starts at ${formatMinutes(settings.slotSchedules.getValue(slot))}"
            }
            SlotCardState(
                slot = slot,
                title = slot.displayName,
                videoLabel = config?.videoLabel,
                description = description,
            )
        }
        val summary = when (settings.scheduleMode) {
            WallpaperScheduleMode.SOLAR ->
                "Matches sunrise and sunset when location access is available. Falls back to your fixed times otherwise."
            WallpaperScheduleMode.FIXED ->
                DaySlot.values().joinToString(separator = " → ") { slot ->
                    "${slot.displayName} ${formatMinutes(settings.slotSchedules.getValue(slot))}"
                }
        }
        return WallpaperHomeState(
            slots = slots,
            scheduleMode = settings.scheduleMode,
            scheduleSummary = summary,
            mutePlayback = settings.mutePlayback,
            loopPlayback = settings.loopPlayback,
            isLoading = false,
        )
    }

    private fun formatMinutes(minutes: Int): String {
        val normalized = ((minutes % (24 * 60)) + (24 * 60)) % (24 * 60)
        val time = LocalTime.of(normalized / 60, normalized % 60)
        return time.format(formatter)
    }

    private fun solarDescription(slot: DaySlot): String = when (slot) {
        DaySlot.MORNING -> "Begins at civil dawn"
        DaySlot.DAY -> "Runs from sunrise to sunset"
        DaySlot.EVENING -> "Follows sunset through twilight"
        DaySlot.NIGHT -> "Covers the darkest hours"
    }

    @AssistedFactory
    interface Factory : AssistedVmFactory<WallpaperHomeViewModel>
}

@Module
@InstallIn(SingletonComponent::class)
object WallpaperHomeBindings {
    @Provides
    @IntoMap
    @ClassKey(WallpaperHomePresenter::class)
    fun provideWallpaperHomePresenter(): PresenterProvider<*> {
        return object : PresenterProvider<WallpaperHomePresenter> {
            @Composable
            override fun provide(key: String?): WallpaperHomePresenter {
                return scopedViewModel<WallpaperHomeViewModel>(key)
            }
        }
    }
}

@Module
@InstallIn(ScreenComponent::class)
abstract class WallpaperHomeBindingModule {
    @Binds
    @IntoMap
    @VmKey(WallpaperHomeViewModel::class)
    abstract fun wallpaperHomeFactory(factory: WallpaperHomeViewModel.Factory): AssistedVmFactory<out ViewModel>
}
