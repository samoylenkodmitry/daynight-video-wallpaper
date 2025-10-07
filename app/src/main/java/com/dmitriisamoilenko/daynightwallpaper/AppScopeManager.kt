package com.dmitriisamoilenko.daynightwallpaper

import com.archstarter.core.common.app.App
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppScopeManager @Inject constructor(
    private val builder: AppComponent.Builder
) {
    private var component: AppComponent? = null

    fun create(app: App) {
        check(component == null) { "App scope already active" }
        component = builder.app(app).build()
    }

    fun getComponent(): AppComponent =
        component ?: error("App scope is not initialized")

    fun clear() {
        component = null
    }
}
