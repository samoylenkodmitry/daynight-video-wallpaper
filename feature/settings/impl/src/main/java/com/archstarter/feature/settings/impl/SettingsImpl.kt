package com.archstarter.feature.settings.impl

import androidx.compose.runtime.Composable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archstarter.core.common.presenter.PresenterProvider
import com.archstarter.core.common.scope.ScreenComponent
import com.archstarter.core.common.viewmodel.AssistedVmFactory
import com.archstarter.core.common.viewmodel.VmKey
import com.archstarter.core.common.viewmodel.scopedViewModel
import com.archstarter.core.common.wallpaper.DaySlot
import com.archstarter.core.common.wallpaper.WallpaperPreferencesRepository
import com.archstarter.core.common.wallpaper.WallpaperScheduleMode
import com.archstarter.core.common.wallpaper.defaultSlotSchedule
import com.archstarter.feature.settings.api.ScheduleSlotUi
import com.archstarter.feature.settings.api.SettingsPresenter
import com.archstarter.feature.settings.api.SettingsState
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
class SettingsViewModel @AssistedInject constructor(
    @Assisted private val savedStateHandle: SavedStateHandle,
    private val repository: WallpaperPreferencesRepository,
) : ViewModel(), SettingsPresenter {
    private val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    private val _state = MutableStateFlow(SettingsState())
    override val state: StateFlow<SettingsState> = _state

    init {
        viewModelScope.launch {
            repository.settings.collect { settings ->
                val slots = DaySlot.values().map { slot ->
                    val minutes = settings.slotSchedules.getValue(slot)
                    ScheduleSlotUi(slot, slot.displayName, minutes)
                }
                val description = when (settings.scheduleMode) {
                    WallpaperScheduleMode.SOLAR ->
                        "Uses sunrise and sunset when location access is granted. Falls back to custom times otherwise."
                    WallpaperScheduleMode.FIXED ->
                        DaySlot.values().joinToString(separator = " → ") { slot ->
                            "${slot.displayName} ${formatMinutes(settings.slotSchedules.getValue(slot))}"
                        }
                }
                _state.value = SettingsState(
                    scheduleMode = settings.scheduleMode,
                    slots = slots,
                    description = description,
                )
            }
        }
    }

    override fun onScheduleModeSelected(mode: WallpaperScheduleMode) {
        viewModelScope.launch { repository.setScheduleMode(mode) }
    }

    override fun onSlotTimeSelected(slot: DaySlot, minutes: Int) {
        viewModelScope.launch { repository.setStartMinutes(slot, minutes) }
    }

    override fun onResetDefaults() {
        viewModelScope.launch {
            defaultSlotSchedule.forEach { (slot, minutes) ->
                repository.setStartMinutes(slot, minutes)
            }
        }
    }

    override fun initOnce(params: Unit?) = Unit

    private fun formatMinutes(minutes: Int): String {
        val normalized = ((minutes % (24 * 60)) + (24 * 60)) % (24 * 60)
        val time = LocalTime.of(normalized / 60, normalized % 60)
        return time.format(formatter)
    }

    @AssistedFactory
    interface Factory : AssistedVmFactory<SettingsViewModel>
}

@Module
@InstallIn(SingletonComponent::class)
object SettingsPresenterModule {
    @Provides
    @IntoMap
    @ClassKey(SettingsPresenter::class)
    fun provideSettingsPresenter(): PresenterProvider<*> {
        return object : PresenterProvider<SettingsPresenter> {
            @Composable
            override fun provide(key: String?): SettingsPresenter {
                return scopedViewModel<SettingsViewModel>(key)
            }
        }
    }
}

@Module
@InstallIn(ScreenComponent::class)
abstract class SettingsBindingModule {
    @Binds
    @IntoMap
    @VmKey(SettingsViewModel::class)
    abstract fun settingsFactory(factory: SettingsViewModel.Factory): AssistedVmFactory<out ViewModel>
}
