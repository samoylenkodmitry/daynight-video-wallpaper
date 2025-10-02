package com.archstarter.core.common.scope

import androidx.lifecycle.ViewModel
import com.archstarter.core.common.viewmodel.AssistedVmFactory
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent

@EntryPoint
@InstallIn(ActivityRetainedComponent::class)
interface AppEntryPoint {
  fun screenBuilder(): ScreenComponent.Builder
}

@EntryPoint
@InstallIn(ScreenComponent::class)
interface SubscreenBuilderEntryPoint {
  fun subBuilder(): SubscreenComponent.Builder
}

@EntryPoint
@InstallIn(ScreenComponent::class) // parent provides the VM map
interface VmMapEntryPoint {
  fun vmFactories(): Map<Class<out ViewModel>, @JvmSuppressWildcards AssistedVmFactory<out ViewModel>>
}

@EntryPoint
@InstallIn(SubscreenComponent::class) // child also provides the VM map (inherited)
interface SubVmMapEntryPoint {
  fun vmFactories(): Map<Class<out ViewModel>, @JvmSuppressWildcards AssistedVmFactory<out ViewModel>>
}