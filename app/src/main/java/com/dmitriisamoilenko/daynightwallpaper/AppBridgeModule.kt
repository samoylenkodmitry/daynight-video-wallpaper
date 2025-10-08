package com.dmitriisamoilenko.daynightwallpaper

import com.archstarter.core.common.app.App
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppBridgeModule {
    @Provides
    fun provideApp(manager: AppScopeManager): App =
        EntryPoints.get(manager.getComponent(), AppEntryPoint::class.java).app()
}
