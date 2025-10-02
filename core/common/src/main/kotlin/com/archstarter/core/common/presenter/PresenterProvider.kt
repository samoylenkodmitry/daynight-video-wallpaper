package com.archstarter.core.common.presenter

import androidx.compose.runtime.Composable

/**
 * Interface for providing presenters. Each feature implements this to provide
 * their specific presenter instances.
 */
interface PresenterProvider<T> {
    @Composable
    fun provide(key: String?): T
}