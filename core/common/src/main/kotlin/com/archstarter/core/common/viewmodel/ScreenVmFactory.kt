package com.archstarter.core.common.viewmodel

import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.savedstate.SavedStateRegistryOwner
import androidx.lifecycle.ViewModel
import com.archstarter.core.common.scope.ScreenComponent
import com.archstarter.core.common.scope.SubscreenComponent
import com.archstarter.core.common.scope.VmMapEntryPoint
import com.archstarter.core.common.scope.SubVmMapEntryPoint
import dagger.hilt.EntryPoints

class ScreenVmFactory(
  owner: SavedStateRegistryOwner,
  defaultArgs: Bundle?,
  private val component: Any // ScreenComponent or SubscreenComponent
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {

  // Get the VM map from whichever component this is
  private val map: Map<Class<out ViewModel>, AssistedVmFactory<out ViewModel>> by lazy {
    when (component) {
      is ScreenComponent -> EntryPoints.get(component, VmMapEntryPoint::class.java).vmFactories()
      is SubscreenComponent -> EntryPoints.get(component, SubVmMapEntryPoint::class.java).vmFactories()
      else -> error("Unsupported component type")
    }
  }

  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(key: String, modelClass: Class<T>, handle: SavedStateHandle): T {
    val raw = map[modelClass] ?: error("No AssistedVmFactory bound for ${modelClass.name}")
    return (raw as AssistedVmFactory<T>).create(handle)
  }
}