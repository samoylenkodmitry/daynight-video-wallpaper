package com.archstarter.feature.settings.impl.language

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archstarter.core.common.scope.ScreenComponent
import com.archstarter.core.common.viewmodel.AssistedVmFactory
import com.archstarter.core.common.viewmodel.VmKey
import com.archstarter.feature.settings.api.LanguageChooserParams
import com.archstarter.feature.settings.api.LanguageChooserPresenter
import com.archstarter.feature.settings.api.LanguageChooserState
import com.archstarter.feature.settings.api.LanguageSelectionEvent
import dagger.Binds
import dagger.Module
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.InstallIn
import dagger.multibindings.IntoMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LanguageChooserViewModel @AssistedInject constructor(
    private val repository: LanguageRepository,
    private val selectionBus: LanguageSelectionBus,
    @Suppress("unused") @Assisted private val savedStateHandle: SavedStateHandle,
) : ViewModel(), LanguageChooserPresenter {

    private val _state = MutableStateFlow(LanguageChooserState())
    override val state: StateFlow<LanguageChooserState> = _state.asStateFlow()

    private var params: LanguageChooserParams? = null
    private var allLanguages: List<String> = emptyList()
    private var filterJob: Job? = null

    override fun initOnce(params: LanguageChooserParams?) {
        val actual = params ?: return
        this.params = actual
        _state.update { it.copy(selectedLanguage = actual.selectedLanguage) }
        if (allLanguages.isEmpty()) {
            loadLanguages(initial = true)
        }
    }

    override fun onToggleExpanded() {
        val expanded = _state.value.isExpanded
        if (expanded) {
            collapse()
        } else {
            _state.update { it.copy(isExpanded = true, query = "", errorMessage = null) }
            if (allLanguages.isEmpty()) {
                loadLanguages(initial = false)
            } else {
                filterLanguages("")
            }
        }
    }

    override fun onDismiss() {
        collapse()
    }

    override fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
        if (allLanguages.isEmpty()) {
            loadLanguages(initial = false)
        } else {
            filterLanguages(query)
        }
    }

    override fun onSelect(language: String) {
        val role = params?.role ?: return
        _state.update {
            it.copy(
                selectedLanguage = language,
                isExpanded = false,
                query = "",
            )
        }
        selectionBus.publish(LanguageSelectionEvent(role = role, language = language))
    }

    override fun onRetry() {
        loadLanguages(initial = false)
    }

    override fun onCleared() {
        filterJob?.cancel()
        super.onCleared()
    }

    private fun collapse() {
        filterJob?.cancel()
        _state.update { it.copy(isExpanded = false, query = "") }
    }

    private fun loadLanguages(initial: Boolean) {
        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { repository.loadLanguages() }
                .onSuccess { languages ->
                    allLanguages = languages
                    val query = if (initial) "" else _state.value.query
                    val filtered = filterSync(query, languages)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            results = filtered,
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "Unable to load languages",
                            results = emptyList(),
                        )
                    }
                }
        }
    }

    private fun filterLanguages(query: String) {
        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val languages = allLanguages
            val filtered = withContext(Dispatchers.Default) { filterSync(query, languages) }
            _state.update {
                it.copy(
                    isLoading = false,
                    results = filtered,
                )
            }
        }
    }

    private fun filterSync(query: String, languages: List<String>): List<String> {
        if (query.isBlank()) return languages
        return languages.filter { language -> language.contains(query, ignoreCase = true) }
    }

    @AssistedFactory
    interface Factory : AssistedVmFactory<LanguageChooserViewModel>
}

@Module
@InstallIn(ScreenComponent::class)
abstract class LanguageChooserBindingModule {
    @Binds
    @IntoMap
    @VmKey(LanguageChooserViewModel::class)
    abstract fun languageChooserFactory(factory: LanguageChooserViewModel.Factory): AssistedVmFactory<out ViewModel>
}
