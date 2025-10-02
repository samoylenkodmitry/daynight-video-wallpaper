package com.archstarter.app

import com.archstarter.core.common.app.App
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn

@EntryPoint
@InstallIn(AppComponent::class)
interface AppEntryPoint {
    @InternalApp fun app(): App
}
