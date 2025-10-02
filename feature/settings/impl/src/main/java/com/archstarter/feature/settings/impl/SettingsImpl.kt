package com.archstarter.feature.settings.impl

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archstarter.core.common.scope.ScreenBus
import com.archstarter.core.common.scope.ScreenComponent
import com.archstarter.core.common.viewmodel.AssistedVmFactory
import com.archstarter.core.common.viewmodel.VmKey
import com.archstarter.feature.settings.api.LanguageChooserRole
import com.archstarter.feature.settings.api.SettingsPresenter
import com.archstarter.feature.settings.api.SettingsState
import com.archstarter.feature.settings.impl.data.SettingsRepository
import com.archstarter.feature.settings.impl.language.LanguageSelectionBus
import dagger.Binds
import dagger.Module
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.InstallIn
import dagger.multibindings.IntoMap
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class SettingsViewModel @AssistedInject constructor(
    private val repo: SettingsRepository,
    private val screenBus: ScreenBus, // from Screen/Subscreen (inherited)
    private val languageSelectionBus: LanguageSelectionBus,
    @Assisted private val handle: SavedStateHandle
) : ViewModel(), SettingsPresenter {
    override val state: StateFlow<SettingsState> = repo.state

    init {
        println("SettingsViewModel created vm=${System.identityHashCode(this)}, bus=${System.identityHashCode(screenBus)}")
        languageSelectionBus.selections
            .onEach { event ->
                when (event.role) {
                    LanguageChooserRole.Native -> {
                        repo.updateNative(event.language)
                        screenBus.send("Native language changed to ${event.language}")
                    }
                    LanguageChooserRole.Learning -> {
                        repo.updateLearning(event.language)
                        screenBus.send("Learning language changed to ${event.language}")
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    override fun onCleared() {
        super.onCleared()
        println("SettingsViewModel clear vm=${System.identityHashCode(this)}, bus=${System.identityHashCode(screenBus)}")
    }

    override fun onNativeSelected(language: String) {
        viewModelScope.launch {
            repo.updateNative(language)
            screenBus.send("Native language changed to $language")
        }
    }

    override fun onLearningSelected(language: String) {
        viewModelScope.launch {
            repo.updateLearning(language)
            screenBus.send("Learning language changed to $language")
        }
    }

    override fun initOnce(params: Unit?) {
    }

    @AssistedFactory
    interface Factory : AssistedVmFactory<SettingsViewModel>
}

@Module
@InstallIn(ScreenComponent::class)
abstract class SettingsVmBindingModule {

    @Binds
    @IntoMap
    @VmKey(SettingsViewModel::class)
    abstract fun settingsFactory(f: SettingsViewModel.Factory): AssistedVmFactory<out ViewModel>
}
