package com.archstarter.core.common.app

interface NavigationActions {
    fun openDetail(id: Int)
    fun openSettings()
    fun openLink(url: String)
}
