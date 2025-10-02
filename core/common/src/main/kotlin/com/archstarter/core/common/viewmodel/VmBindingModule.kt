package com.archstarter.core.common.viewmodel

import androidx.lifecycle.ViewModel
import com.archstarter.core.common.scope.ScreenComponent
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.Multibinds

@Module
@InstallIn(ScreenComponent::class) // parent only
abstract class VmBindingModule {

  // Base module that establishes the VM factory map
  // Individual feature modules will add their own bindings to this map
  @Multibinds
  abstract fun vmFactories(): Map<Class<out ViewModel>, AssistedVmFactory<out ViewModel>>
}