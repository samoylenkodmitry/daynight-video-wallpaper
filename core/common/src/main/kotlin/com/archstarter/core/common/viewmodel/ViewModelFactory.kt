package com.archstarter.core.common.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.MapKey
import kotlin.reflect.KClass

interface AssistedVmFactory<T : ViewModel> { 
  fun create(handle: SavedStateHandle): T 
}

@MapKey
@Target(AnnotationTarget.FUNCTION)
annotation class VmKey(val value: KClass<out ViewModel>)