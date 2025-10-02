package com.archstarter.feature.settings.impl.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.archstarter.feature.settings.api.SettingsState
import com.archstarter.feature.settings.api.SettingsStateProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext context: Context,
) : SettingsStateProvider {
    private val dataStore: DataStore<Preferences> = context.settingsDataStore
    private val defaultState = SettingsState()
    private val _state = MutableStateFlow(defaultState)
    override val state: StateFlow<SettingsState> = _state.asStateFlow()

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .onEach { preferences ->
                val native = preferences[NATIVE_LANGUAGE_KEY] ?: defaultState.nativeLanguage
                val learning = preferences[LEARNING_LANGUAGE_KEY] ?: defaultState.learningLanguage
                _state.value = SettingsState(nativeLanguage = native, learningLanguage = learning)
            }
            .launchIn(repositoryScope)
    }

    suspend fun updateNative(language: String) {
        _state.update { current -> current.copy(nativeLanguage = language) }
        dataStore.edit { preferences ->
            preferences[NATIVE_LANGUAGE_KEY] = language
        }
    }

    suspend fun updateLearning(language: String) {
        _state.update { current -> current.copy(learningLanguage = language) }
        dataStore.edit { preferences ->
            preferences[LEARNING_LANGUAGE_KEY] = language
        }
    }

    private companion object {
        val NATIVE_LANGUAGE_KEY = stringPreferencesKey("native_language")
        val LEARNING_LANGUAGE_KEY = stringPreferencesKey("learning_language")
    }
}
