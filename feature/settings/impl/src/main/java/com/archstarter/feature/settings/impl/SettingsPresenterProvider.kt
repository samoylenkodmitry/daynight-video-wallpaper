package com.archstarter.feature.settings.impl

import androidx.compose.runtime.Composable
import com.archstarter.core.common.presenter.PresenterProvider
import com.archstarter.core.common.viewmodel.scopedViewModel
import com.archstarter.feature.settings.api.LanguageChooserPresenter
import com.archstarter.feature.settings.api.SettingsPresenter
import com.archstarter.feature.settings.impl.language.LanguageChooserViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap


@Module
@InstallIn(SingletonComponent::class)
object SettingsPresenterBindings {
    @Provides
    @IntoMap
    @ClassKey(SettingsPresenter::class)
    fun provideSettingsPresenterProvider(): PresenterProvider<*> {
        return object : PresenterProvider<SettingsPresenter> {
            @Composable
            override fun provide(key: String?): SettingsPresenter {
                return scopedViewModel<SettingsViewModel>(key)
            }
        }
    }

    @Provides
    @IntoMap
    @ClassKey(LanguageChooserPresenter::class)
    fun provideLanguageChooserPresenterProvider(): PresenterProvider<*> {
        return object : PresenterProvider<LanguageChooserPresenter> {
            @Composable
            override fun provide(key: String?): LanguageChooserPresenter {
                return scopedViewModel<LanguageChooserViewModel>(key)
            }
        }
    }
}