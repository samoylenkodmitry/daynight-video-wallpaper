package com.archstarter.feature.detail.impl

import androidx.compose.runtime.Composable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archstarter.core.common.app.App
import com.archstarter.core.common.presenter.PresenterProvider
import com.archstarter.core.common.scope.ScreenBus
import com.archstarter.core.common.scope.ScreenComponent
import com.archstarter.core.common.viewmodel.AssistedVmFactory
import com.archstarter.core.common.viewmodel.VmKey
import com.archstarter.core.common.viewmodel.scopedViewModel
import com.archstarter.feature.catalog.impl.data.ArticleEntity
import com.archstarter.feature.catalog.impl.data.ArticleRepo
import com.archstarter.feature.detail.api.DetailPresenter
import com.archstarter.feature.detail.api.DetailState
import com.archstarter.feature.settings.api.SettingsState
import com.archstarter.feature.settings.api.SettingsStateProvider
import com.archstarter.feature.settings.api.languageCodes
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.LinkedHashMap
import java.util.Locale

class DetailViewModel @AssistedInject constructor(
    private val repo: ArticleRepo,
    private val app: App,
    private val screenBus: ScreenBus, // from Screen/Subscreen (inherited)
    private val settingsStateProvider: SettingsStateProvider,
    @Assisted private val handle: SavedStateHandle
) : ViewModel(), DetailPresenter {

    init {
        println("DetailsViewModel created vm=${System.identityHashCode(this)}, bus=${System.identityHashCode(screenBus)}")
    }

    override fun onCleared() {
        super.onCleared()
        prefetchJob?.cancel()
        translationJob?.cancel()
        println("DetailsViewModel clear vm=${System.identityHashCode(this)}, bus=${System.identityHashCode(screenBus)}")
    }
    private val _state = MutableStateFlow(DetailState())
    override val state: StateFlow<DetailState> = _state
    private var initialized = false
    private val translationCache = mutableMapOf<String, String>()
    private var prefetchJob: Job? = null
    private var translationJob: Job? = null
    private val languageState = settingsStateProvider.state
        .map { it.toLanguagePairOrNull() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    override fun initOnce(params: Int?) {
        if (initialized) return
        val articleId = params ?: return
        initialized = true
        viewModelScope.launch {
            repo.article(articleId)?.let {
                _state.value = DetailState(
                    title = it.title,
                    content = it.content,
                    sourceUrl = it.sourceUrl,
                    originalWord = it.originalWord,
                    translatedWord = it.translatedWord,
                    ipa = it.ipa
                )
                cacheTranslation(it.originalWord, it.translatedWord)
                observeArticleTranslations(it)
                screenBus.send("Detail loaded for article $articleId: ${it.title}")
            }
        }
    }

    private fun observeArticleTranslations(article: ArticleEntity) {
        translationJob?.cancel()
        translationJob = viewModelScope.launch {
            languageState.collectLatest { languages ->
                if (languages == null) return@collectLatest
                val translated = repo.translateContent(
                    article.content,
                    languages.learning,
                    languages.native,
                )?.takeIf { it.isNotBlank() }
                val displayContent = translated ?: article.content
                if (_state.value.content != displayContent) {
                    _state.value = _state.value.copy(content = displayContent)
                }
                prefetchContentTranslations(displayContent, languages)
            }
        }
    }

    override fun translate(word: String) {
        viewModelScope.launch {
            val normalizedWord = normalizeWord(word)
            if (normalizedWord.isEmpty()) return@launch

            translationCache[cacheKey(normalizedWord)]?.let { cached ->
                updateHighlighted(normalizedWord, cached)
                return@launch
            }

            val languages = languageState.value ?: return@launch
            val translation = repo.translate(
                normalizedWord,
                languages.native,
                languages.learning,
            )?.let(::normalizeTranslation)
            if (translation.isNullOrEmpty()) return@launch

            cacheTranslation(normalizedWord, translation)
            updateHighlighted(normalizedWord, translation)
        }
    }

    override fun onSourceClick(url: String) {
        if (url.isBlank()) return
        app.navigation.openLink(url)
    }

    private fun updateHighlighted(word: String, translation: String) {
        _state.value = _state.value.copy(
            highlightedWord = word,
            highlightedTranslation = translation
        )
    }

    private fun cacheTranslation(word: String, translation: String) {
        val normalizedWord = normalizeWord(word)
        val normalizedTranslation = normalizeTranslation(translation)
        if (normalizedWord.isEmpty() || normalizedTranslation.isEmpty()) return
        val key = cacheKey(normalizedWord)
        val previous = translationCache.put(key, normalizedTranslation)
        if (previous != normalizedTranslation) {
            updateWordTranslations()
        }
    }

    private fun normalizeWord(word: String): String = word.trim()

    private fun normalizeTranslation(translation: String): String = translation.trim()

    private fun cacheKey(word: String): String = word.lowercase(Locale.ROOT)

    private fun updateWordTranslations() {
        _state.value = _state.value.copy(wordTranslations = translationCache.toMap())
    }

    private fun prefetchContentTranslations(content: String, languages: LanguagePair) {
        if (content.isBlank()) return
        prefetchJob?.cancel()
        prefetchJob = viewModelScope.launch {
            val words = extractWords(content)
            for ((key, candidate) in words) {
                if (translationCache.containsKey(key)) continue
                // The MyMemory translate API accepts only a single query per request,
                // so we translate sequentially.
                val translation = repo.translate(
                    candidate,
                    languages.native,
                    languages.learning,
                )?.let(::normalizeTranslation)
                if (!translation.isNullOrEmpty()) {
                    cacheTranslation(candidate, translation)
                }
            }
        }
    }

    private fun extractWords(content: String): LinkedHashMap<String, String> {
        val map = LinkedHashMap<String, String>()
        WORD_REGEX.findAll(content).forEach { match ->
            val rawWord = normalizeWord(match.value)
            if (rawWord.length <= 1) return@forEach
            val key = cacheKey(rawWord)
            if (key !in map) {
                map[key] = rawWord
            }
        }
        return map
    }

    companion object {
        private val WORD_REGEX = Regex("[\\p{L}\\p{Nd}']+")
    }

    @AssistedFactory
    interface Factory : AssistedVmFactory<DetailViewModel>
}

private data class LanguagePair(val native: String, val learning: String)

private fun SettingsState.toLanguagePairOrNull(): LanguagePair? {
    val native = languageCodes[nativeLanguage]
    val learning = languageCodes[learningLanguage]
    if (native.isNullOrBlank() || learning.isNullOrBlank()) return null
    return LanguagePair(native, learning)
}

@Module
@InstallIn(SingletonComponent::class)
object DetailPresenterBindings {
    @Provides
    @IntoMap
    @ClassKey(DetailPresenter::class)
    fun provideDetailPresenterProvider(): PresenterProvider<*> {
        return object : PresenterProvider<DetailPresenter> {
            @Composable
            override fun provide(key: String?): DetailPresenter {
                return scopedViewModel<DetailViewModel>(key)
            }
        }
    }
}

@Module
@InstallIn(ScreenComponent::class)
abstract class DetailVmBindingModule {

    @Binds
    @IntoMap
    @VmKey(DetailViewModel::class)
    abstract fun detailFactory(f: DetailViewModel.Factory): AssistedVmFactory<out ViewModel>
}
