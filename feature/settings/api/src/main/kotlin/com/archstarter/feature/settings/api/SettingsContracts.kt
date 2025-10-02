package com.archstarter.feature.settings.api

import com.archstarter.core.common.presenter.ParamInit
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import java.util.LinkedHashMap
import java.util.Locale

@Serializable
data object Settings

val languageCodes: Map<String, String> =
    Locale.getISOLanguages()
        .map { code ->
            val name = Locale(code).getDisplayLanguage(Locale.ENGLISH)
            name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString() } to code
        }
        .sortedBy { it.first }
        .toMap(LinkedHashMap())

val supportedLanguages = languageCodes.keys.toList()

data class SettingsState(
    val nativeLanguage: String = "English",
    val learningLanguage: String = "Spanish"
)

interface SettingsStateProvider {
    val state: StateFlow<SettingsState>
}

interface SettingsPresenter: ParamInit<Unit> {
    val state: StateFlow<SettingsState>
    fun onNativeSelected(language: String)
    fun onLearningSelected(language: String)
}
