package com.archstarter.feature.settings.api

import com.archstarter.core.common.presenter.ParamInit
import kotlinx.coroutines.flow.StateFlow

/** Identifies which language slot is being configured by the chooser. */
enum class LanguageChooserRole {
    Native,
    Learning,
}

/** Parameters supplied when instantiating a language chooser presenter. */
data class LanguageChooserParams(
    val role: LanguageChooserRole,
    val selectedLanguage: String,
)

/** UI state exposed by the language chooser presenter. */
data class LanguageChooserState(
    val selectedLanguage: String = "",
    val query: String = "",
    val isExpanded: Boolean = false,
    val isLoading: Boolean = false,
    val results: List<String> = emptyList(),
    val errorMessage: String? = null,
)

/** Events emitted whenever the chooser produces a new selection. */
data class LanguageSelectionEvent(
    val role: LanguageChooserRole,
    val language: String,
)

interface LanguageChooserPresenter : ParamInit<LanguageChooserParams> {
    val state: StateFlow<LanguageChooserState>
    fun onToggleExpanded()
    fun onDismiss()
    fun onQueryChange(query: String)
    fun onSelect(language: String)
    fun onRetry()
}
