package com.archstarter.feature.catalog.impl

import com.archstarter.feature.catalog.impl.data.ArticleDao
import com.archstarter.feature.catalog.impl.data.ArticleEntity
import com.archstarter.feature.catalog.impl.data.ArticleRepo
import com.archstarter.feature.catalog.impl.data.ArticleRepository
import com.archstarter.feature.catalog.impl.data.TranslationDao
import com.archstarter.feature.catalog.impl.data.TranslationEntity
import com.archstarter.feature.catalog.impl.data.TranslationResponse
import com.archstarter.feature.catalog.impl.data.TranslationData
import com.archstarter.feature.catalog.impl.data.TranslatorService
import com.archstarter.feature.catalog.impl.data.WikipediaService
import com.archstarter.feature.catalog.impl.data.WikipediaSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArticleRepositoryTest {

  @get:Rule
  val dispatcherRule = MainDispatcherRule()

  private fun summary(
    id: Int = 1,
    title: String = "Title",
    extract: String = " Summary \n ",
    url: String = "url",
  ) = WikipediaSummary(
    pageid = id,
    title = title,
    extract = extract,
    contentUrls = WikipediaSummary.ContentUrls(
      WikipediaSummary.ContentUrls.Desktop(url)
    ),
  )

  private class FakeArticleDao : ArticleDao {
    private val state = MutableStateFlow<List<ArticleEntity>>(emptyList())
    override fun getArticles(): Flow<List<ArticleEntity>> = state
    override fun observeArticle(id: Int): Flow<ArticleEntity?> = state.map { list -> list.firstOrNull { it.id == id } }
    override suspend fun getArticle(id: Int): ArticleEntity? = state.value.firstOrNull { it.id == id }
    override suspend fun insert(article: ArticleEntity) { state.value = state.value + article }
    val inserted: List<ArticleEntity> get() = state.value
  }

  @Test
  fun refreshStoresTrimmedSummaryFromWikipedia() = runTest {
    val dao = FakeArticleDao()
    val repo: ArticleRepo = ArticleRepository(
      wiki = object : WikipediaService {
        override suspend fun randomSummary(): WikipediaSummary = summary()
      },
      dao = dao,
      translator = FakeTranslatorService { text, _ -> text },
      translationDao = FakeTranslationDao(),
    )

    repo.refresh()

    val stored = dao.inserted.single()
    assertEquals(1, stored.id)
    assertEquals("Title", stored.title)
    assertEquals("Summary", stored.summary)
    assertEquals("Summary", stored.content)
    assertEquals("url", stored.sourceUrl)
  }

  @Test
  fun translateSummaryReturnsStoredSummary() = runTest {
    val dao = FakeArticleDao()
    val article = ArticleEntity(
      id = 2,
      title = "Other",
      summary = "Original",
      summaryLanguage = null,
      content = "Original",
      sourceUrl = "url",
      originalWord = "",
      translatedWord = "",
      ipa = null,
      createdAt = 0L,
    )
    dao.insert(article)

    val repo: ArticleRepo = ArticleRepository(
      wiki = object : WikipediaService {
        override suspend fun randomSummary(): WikipediaSummary = summary(id = 2)
      },
      dao = dao,
      translator = FakeTranslatorService { text, _ -> text },
      translationDao = FakeTranslationDao(),
    )

    val translated = repo.translateSummary(article, "en", "en")
    assertEquals("Original", translated)
  }

  @Test
  fun translateReturnsInputWord() = runTest {
    val repo: ArticleRepo = ArticleRepository(
      wiki = object : WikipediaService {
        override suspend fun randomSummary(): WikipediaSummary = summary()
      },
      dao = FakeArticleDao(),
      translator = FakeTranslatorService { text, _ -> text },
      translationDao = FakeTranslationDao(),
    )

    val result = repo.translate("Hola", "en", "en")
    assertEquals("Hola", result)
  }

  @Test
  fun translateSummaryUsesRemoteTranslationWhenLanguagesDiffer() = runTest {
    val dao = FakeArticleDao()
    val article = ArticleEntity(
      id = 3,
      title = "Remote",
      summary = "Original",
      summaryLanguage = null,
      content = "Original",
      sourceUrl = "url",
      originalWord = "",
      translatedWord = "",
      ipa = null,
      createdAt = 0L,
    )
    dao.insert(article)
    val translator = FakeTranslatorService { text, langPair -> "$text-$langPair" }
    val translationDao = FakeTranslationDao()
    val repo: ArticleRepo = ArticleRepository(
      wiki = object : WikipediaService {
        override suspend fun randomSummary(): WikipediaSummary = summary(id = 3)
      },
      dao = dao,
      translator = translator,
      translationDao = translationDao,
    )

    val translated = repo.translateSummary(article, "es", "en")
    assertEquals("Original-es|en", translated)
    assertEquals(1, translator.requests.size)

    val cached = repo.translateSummary(article, "es", "en")
    assertEquals("Original-es|en", cached)
    assertEquals(1, translator.requests.size)
  }

  private class FakeTranslationDao : TranslationDao {
    private val storage = mutableMapOf<Pair<String, String>, TranslationEntity>()
    override suspend fun translation(langPair: String, normalized: String): TranslationEntity? =
      storage[langPair to normalized]

    override suspend fun insert(entity: TranslationEntity) {
      storage[entity.langPair to entity.normalizedText] = entity
    }
  }

  private class FakeTranslatorService(
    private val provider: (String, String) -> String?,
  ) : TranslatorService {
    val requests = mutableListOf<Pair<String, String>>()
    override suspend fun translate(word: String, langPair: String): TranslationResponse {
      requests += langPair to word
      val translation = provider(word, langPair)
      return if (translation != null) {
        TranslationResponse(responseData = TranslationData(translation))
      } else {
        TranslationResponse(responseData = TranslationData(word), responseStatus = 500)
      }
    }
  }
}
