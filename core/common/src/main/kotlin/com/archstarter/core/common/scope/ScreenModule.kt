package com.archstarter.core.common.scope

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@Module
@InstallIn(ScreenComponent::class)
object ScreenModule {
  @Provides 
  @ScreenScope 
  fun provideScreenBus(): ScreenBus = ScreenBus()
}

class ScreenBus @Inject constructor() {
  val text = MutableStateFlow("screen bus initialized")
  fun send(message: String) { 
    text.value = message 
  }
}