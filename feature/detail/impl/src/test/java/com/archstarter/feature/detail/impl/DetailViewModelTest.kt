package com.archstarter.feature.detail.impl

import androidx.lifecycle.SavedStateHandle
import com.archstarter.core.common.app.App
import com.archstarter.core.common.app.NavigationActions
import com.archstarter.core.common.scope.ScreenBus
import com.archstarter.feature.catalog.impl.data.ArticleEntity
import com.archstarter.feature.catalog.impl.data.ArticleRepo
import com.archstarter.feature.settings.api.SettingsState
import com.archstarter.feature.settings.api.SettingsStateProvider
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {

  @get:Rule val dispatcherRule = MainDispatcherRule()

  @Test
  fun translateReusesCachedTranslation() = runTest {
    val article = sampleArticle()
    val repo = FakeArticleRepo(article, wordProvider = { word, _, _, call -> "$word-$call" })
    val settings = FakeSettingsStateProvider()
    val vm = DetailViewModel(repo, App(RecordingNavigationActions()), ScreenBus(), settings, SavedStateHandle())

    vm.initOnce(article.id)
    advanceUntilIdle()
    val baselineCalls = repo.translateCalls

    vm.translate("Hover")
    advanceUntilIdle()

    val first = requireNotNull(vm.state.value.highlightedTranslation)
    assertEquals(baselineCalls + 1, repo.translateCalls)
    assertEquals(first, vm.state.value.highlightedTranslation)

    vm.translate("Hover")
    advanceUntilIdle()

    assertEquals(first, vm.state.value.highlightedTranslation)
    assertEquals(baselineCalls + 1, repo.translateCalls)
  }

  @Test
  fun translateUsesCachedArticleTranslation() = runTest {
    val article = sampleArticle(original = "Original", translated = "Translated")
    val repo = FakeArticleRepo(article, wordProvider = { word, _, _, call -> "$word-$call" })
    val settings = FakeSettingsStateProvider()
    val vm = DetailViewModel(repo, App(RecordingNavigationActions()), ScreenBus(), settings, SavedStateHandle())

    vm.initOnce(article.id)
    advanceUntilIdle()
    val baselineCalls = repo.translateCalls

    vm.translate("Original")
    advanceUntilIdle()

    assertEquals(baselineCalls, repo.translateCalls)
    assertEquals("Original", vm.state.value.highlightedWord)
    assertEquals("Translated", vm.state.value.highlightedTranslation)
  }

  @Test
  fun translateCachesAcrossWordCaseDifferences() = runTest {
    val article = sampleArticle()
    val repo = FakeArticleRepo(article, wordProvider = { word, _, _, call -> "$word-$call" })
    val settings = FakeSettingsStateProvider()
    val vm = DetailViewModel(repo, App(RecordingNavigationActions()), ScreenBus(), settings, SavedStateHandle())

    vm.initOnce(article.id)
    advanceUntilIdle()
    val baselineCalls = repo.translateCalls

    vm.translate("Word")
    advanceUntilIdle()

    val first = requireNotNull(vm.state.value.highlightedTranslation)
    assertEquals(baselineCalls + 1, repo.translateCalls)
    assertEquals(first, vm.state.value.highlightedTranslation)

    vm.translate("word")
    advanceUntilIdle()

    assertEquals(baselineCalls + 1, repo.translateCalls)
    assertEquals(first, vm.state.value.highlightedTranslation)
  }

  @Test
  fun contentTranslatesToNativeLanguage() = runTest {
    val article = sampleArticle(content = "Original")
    val translationRequest = CompletableDeferred<Pair<String, String>>()
    val translationResult = CompletableDeferred<String>()
    val repo = FakeArticleRepo(
      article,
      contentProvider = { _, from, to, call ->
        if (call == 1) {
          translationRequest.complete(from to to)
          translationResult.await()
        } else {
          error("Unexpected translation call $call")
        }
      },
      wordProvider = { _, _, _, _ -> null },
    )
    val settings = FakeSettingsStateProvider()
    val vm = DetailViewModel(repo, App(RecordingNavigationActions()), ScreenBus(), settings, SavedStateHandle())

    vm.initOnce(article.id)
    advanceUntilIdle()

    assertEquals("Original", vm.state.value.content)
    val languages = translationRequest.await()
    assertEquals("es" to "en", languages)

    translationResult.complete("es->en#1")
    advanceUntilIdle()

    assertEquals("es->en#1", vm.state.value.content)
    assertEquals(1, repo.contentTranslateCalls)
  }

  @Test
  fun prefetchPopulatesWordTranslations() = runTest {
    val article = sampleArticle(content = "Alpha beta alpha", original = "seed", translated = "sprout")
    val repo = FakeArticleRepo(article, wordProvider = { word, _, _, _ -> "${word.lowercase(Locale.ROOT)}-t" })
    val settings = FakeSettingsStateProvider()
    val vm = DetailViewModel(repo, App(RecordingNavigationActions()), ScreenBus(), settings, SavedStateHandle())

    vm.initOnce(article.id)
    advanceUntilIdle()

    val translations = vm.state.value.wordTranslations
    assertEquals("sprout", translations["seed"])
    assertEquals("alpha-t", translations["alpha"])
    assertEquals("beta-t", translations["beta"])
  }

  @Test
  fun onSourceClickOpensLink() = runTest {
    val repo = FakeArticleRepo(null, wordProvider = { _, _, _, _ -> null })
    val nav = RecordingNavigationActions()
    val settings = FakeSettingsStateProvider()
    val vm = DetailViewModel(repo, App(nav), ScreenBus(), settings, SavedStateHandle())

    vm.onSourceClick("")
    vm.onSourceClick("https://example.com")

    assertEquals(listOf("https://example.com"), nav.openedLinks)
  }

  private class RecordingNavigationActions : NavigationActions {
    val openedLinks = mutableListOf<String>()

    override fun openDetail(id: Int) {}

    override fun openSettings() {}

    override fun openLink(url: String) {
      openedLinks += url
    }
  }

  private fun sampleArticle(
    id: Int = 1,
    title: String = "Title",
    summary: String = "Summary",
    summaryLanguage: String? = null,
    content: String = "Content",
    sourceUrl: String = "https://example.com",
    original: String = "Original",
    translated: String = "Translated",
    ipa: String? = null,
    createdAt: Long = 0L,
  ) = ArticleEntity(
    id = id,
    title = title,
    summary = summary,
    summaryLanguage = summaryLanguage,
    content = content,
    sourceUrl = sourceUrl,
    originalWord = original,
    translatedWord = translated,
    ipa = ipa,
    createdAt = createdAt,
  )

  private class FakeArticleRepo(
    private val article: ArticleEntity?,
    private val summaryProvider: (ArticleEntity, String, String, Int) -> String? = { entity, _, _, _ -> entity.summary },
    private val contentProvider: suspend (String, String, String, Int) -> String? = { content, _, _, _ -> content },
    private val wordProvider: (String, String, String, Int) -> String?,
  ) : ArticleRepo {
    override val articles: StateFlow<List<ArticleEntity>> = MutableStateFlow(emptyList())
    var translateCalls: Int = 0
    var summaryTranslateCalls: Int = 0
    var contentTranslateCalls: Int = 0

    override suspend fun refresh() {}

    override suspend fun article(id: Int): ArticleEntity? = article

    override fun articleFlow(id: Int): Flow<ArticleEntity?> = flowOf(article)

    override suspend fun translateSummary(
      article: ArticleEntity,
      fromLanguage: String,
      toLanguage: String,
    ): String? {
      summaryTranslateCalls += 1
      return summaryProvider(article, fromLanguage, toLanguage, summaryTranslateCalls)
    }

    override suspend fun translateContent(
      content: String,
      fromLanguage: String,
      toLanguage: String,
    ): String? {
      contentTranslateCalls += 1
      return contentProvider(content, fromLanguage, toLanguage, contentTranslateCalls)
    }

    override suspend fun translate(
      word: String,
      fromLanguage: String,
      toLanguage: String,
    ): String? {
      translateCalls += 1
      return wordProvider(word, fromLanguage, toLanguage, translateCalls)
    }
  }

  private class FakeSettingsStateProvider(
    initial: SettingsState = SettingsState(),
  ) : SettingsStateProvider {
    private val backing = MutableStateFlow(initial)
    override val state: StateFlow<SettingsState> = backing
  }
}
