package com.dmitriisamoilenko.daynightwallpaper

import com.archstarter.core.common.app.App
import com.archstarter.core.common.app.AppScope
import dagger.BindsInstance
import dagger.hilt.DefineComponent
import dagger.hilt.components.SingletonComponent

@AppScope
@DefineComponent(parent = SingletonComponent::class)
interface AppComponent {
    @DefineComponent.Builder
    interface Builder {
        fun app(@BindsInstance @InternalApp app: App): Builder
        fun build(): AppComponent
    }
}
