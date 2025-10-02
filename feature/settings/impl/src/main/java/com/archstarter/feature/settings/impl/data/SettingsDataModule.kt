package com.archstarter.feature.settings.impl.data

import com.archstarter.feature.settings.api.SettingsStateProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SettingsDataModule {
  @Provides
  @Singleton
  fun provideSettingsStateProvider(
    repository: SettingsRepository,
  ): SettingsStateProvider = repository
}
