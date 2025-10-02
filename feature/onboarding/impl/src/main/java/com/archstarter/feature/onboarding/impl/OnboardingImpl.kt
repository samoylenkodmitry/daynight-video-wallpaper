package com.archstarter.feature.onboarding.impl

import androidx.compose.runtime.Composable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archstarter.core.common.presenter.PresenterProvider
import com.archstarter.core.common.scope.ScreenComponent
import com.archstarter.core.common.viewmodel.AssistedVmFactory
import com.archstarter.core.common.viewmodel.VmKey
import com.archstarter.core.common.viewmodel.scopedViewModel
import com.archstarter.feature.onboarding.api.OnboardingPresenter
import com.archstarter.feature.onboarding.api.OnboardingState
import com.archstarter.feature.onboarding.api.OnboardingStatusProvider
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OnboardingViewModel @AssistedInject constructor(
    private val repository: OnboardingRepository,
    @Assisted private val handle: SavedStateHandle,
) : ViewModel(), OnboardingPresenter {
    private val _state = MutableStateFlow(OnboardingState())
    override val state: StateFlow<OnboardingState> = _state.asStateFlow()

    init {
        repository.hasCompleted
            .onEach { completed ->
                _state.update { current -> current.copy(completed = completed) }
            }
            .launchIn(viewModelScope)
    }

    override fun onContinue() {
        if (_state.value.completed == true) return
        viewModelScope.launch {
            repository.markCompleted()
        }
    }

    override fun initOnce(params: Unit?) = Unit

    @AssistedFactory
    interface Factory : AssistedVmFactory<OnboardingViewModel>
}

@Module
@InstallIn(SingletonComponent::class)
object OnboardingBindings {
    @Provides
    @IntoMap
    @ClassKey(OnboardingPresenter::class)
    fun provideOnboardingPresenter(): PresenterProvider<*> {
        return object : PresenterProvider<OnboardingPresenter> {
            @Composable
            override fun provide(key: String?): OnboardingPresenter {
                return scopedViewModel<OnboardingViewModel>(key)
            }
        }
    }
}

@Module
@InstallIn(ScreenComponent::class)
abstract class OnboardingBindingModule {
    @Binds
    @IntoMap
    @VmKey(OnboardingViewModel::class)
    abstract fun onboardingFactory(factory: OnboardingViewModel.Factory): AssistedVmFactory<out ViewModel>
}

@Module
@InstallIn(SingletonComponent::class)
object OnboardingRepositoryModule {
    @Provides
    fun provideOnboardingStatus(repo: OnboardingRepository): OnboardingStatusProvider = repo
}
