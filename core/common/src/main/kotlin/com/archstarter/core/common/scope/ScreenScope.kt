package com.archstarter.core.common.scope

import dagger.hilt.DefineComponent
import dagger.hilt.android.components.ActivityRetainedComponent
import javax.inject.Scope

@Scope
@Retention(AnnotationRetention.BINARY)
annotation class ScreenScope

@DefineComponent(parent = ActivityRetainedComponent::class)
@ScreenScope
interface ScreenComponent {
  @DefineComponent.Builder 
  interface Builder { 
    fun build(): ScreenComponent 
  }
}

// Child scope (inherits parent bindings)
@Scope
@Retention(AnnotationRetention.BINARY)
annotation class SubscreenScope

@DefineComponent(parent = ScreenComponent::class)
@SubscreenScope
interface SubscreenComponent {
  @DefineComponent.Builder 
  interface Builder { 
    fun build(): SubscreenComponent 
  }
}