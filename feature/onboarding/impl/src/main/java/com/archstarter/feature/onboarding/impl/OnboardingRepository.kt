package com.archstarter.feature.onboarding.impl

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.archstarter.feature.onboarding.api.OnboardingStatusProvider
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

private val Context.onboardingDataStore: DataStore<Preferences> by preferencesDataStore(name = "onboarding")

@Singleton
class OnboardingRepository @Inject constructor(
    @ApplicationContext context: Context,
) : OnboardingStatusProvider {
    private val dataStore: DataStore<Preferences> = context.onboardingDataStore
    private val _hasCompleted = MutableStateFlow<Boolean?>(null)
    override val hasCompleted: StateFlow<Boolean?> = _hasCompleted.asStateFlow()

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
                val completed = preferences[COMPLETED_KEY] ?: false
                _hasCompleted.value = completed
            }
            .launchIn(repositoryScope)
    }

    suspend fun markCompleted() {
        _hasCompleted.update { true }
        dataStore.edit { preferences ->
            preferences[COMPLETED_KEY] = true
        }
    }

    private companion object {
        val COMPLETED_KEY = booleanPreferencesKey("completed")
    }
}
