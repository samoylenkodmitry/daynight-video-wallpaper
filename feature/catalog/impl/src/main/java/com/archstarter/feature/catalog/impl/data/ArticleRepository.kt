package com.archstarter.feature.catalog.impl.data

import android.content.Context
import androidx.room.Room
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

interface ArticleRepo {
  val articles: Flow<List<ArticleEntity>>
  suspend fun refresh()
  suspend fun article(id: Int): ArticleEntity?
  fun articleFlow(id: Int): Flow<ArticleEntity?>
  suspend fun translateSummary(
    article: ArticleEntity,
    fromLanguage: String,
    toLanguage: String,
  ): String?
  suspend fun translateContent(
    content: String,
    fromLanguage: String,
    toLanguage: String,
  ): String?
  suspend fun translate(
    word: String,
    fromLanguage: String,
    toLanguage: String,
  ): String?
}

@Singleton
class ArticleRepository @Inject constructor(
  private val wiki: WikipediaService,
  private val dao: ArticleDao,
  private val translator: TranslatorService,
  private val translationDao: TranslationDao,
) : ArticleRepo {
  override val articles: Flow<List<ArticleEntity>> = dao.getArticles()

  override suspend fun refresh() {
    val summary = runCatching { wiki.randomSummary() }.getOrElse { return }
    val trimmedSummary = summary.extract.trim().ifBlank { summary.title }

    val entity = ArticleEntity(
      id = summary.pageid,
      title = summary.title,
      summary = trimmedSummary,
      summaryLanguage = null,
      content = trimmedSummary,
      sourceUrl = summary.contentUrls.desktop.page,
      originalWord = "",
      translatedWord = "",
      ipa = null,
      createdAt = System.currentTimeMillis(),
    )
    dao.insert(entity)
  }

  override suspend fun article(id: Int): ArticleEntity? = dao.getArticle(id)

  override fun articleFlow(id: Int): Flow<ArticleEntity?> = dao.observeArticle(id)

  override suspend fun translateSummary(
    article: ArticleEntity,
    fromLanguage: String,
    toLanguage: String,
  ): String? = translateText(article.summary, fromLanguage, toLanguage)

  override suspend fun translateContent(
    content: String,
    fromLanguage: String,
    toLanguage: String,
  ): String? = translateText(content, fromLanguage, toLanguage)

  override suspend fun translate(
    word: String,
    fromLanguage: String,
    toLanguage: String,
  ): String? = translateText(word, fromLanguage, toLanguage)

  private suspend fun translateText(
    text: String,
    fromLanguage: String,
    toLanguage: String,
  ): String? {
    val normalized = text.trim()
    if (normalized.isEmpty()) return null
    val langPair = buildLangPair(fromLanguage, toLanguage) ?: return normalized
    val cacheKey = normalized.lowercase(Locale.ROOT)
    translationDao.translation(langPair, cacheKey)?.let { return it.translation }

    val response = runCatching { translator.translate(normalized, langPair) }.getOrNull()
    val translation = response
      ?.takeIf { it.responseStatus == 200 }
      ?.responseData
      ?.translatedText
      ?.trim()
      ?.takeIf { it.isNotEmpty() }
      ?: return normalized

    translationDao.insert(
      TranslationEntity(
        langPair = langPair,
        normalizedText = cacheKey,
        translation = translation,
        updatedAt = System.currentTimeMillis(),
      ),
    )
    return translation
  }

  private fun buildLangPair(fromLanguage: String, toLanguage: String): String? {
    val from = fromLanguage.trim().lowercase(Locale.ROOT)
    val to = toLanguage.trim().lowercase(Locale.ROOT)
    if (from.isEmpty() || to.isEmpty()) return null
    if (from == to) return null
    return "$from|$to"
  }
}

@Module
@InstallIn(SingletonComponent::class)
object ArticleDataModule {
  private val json = Json { ignoreUnknownKeys = true }

  @Provides
  @Singleton
  fun provideOkHttp(): OkHttpClient =
    OkHttpClient.Builder()
      .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
      .addInterceptor { chain ->
        chain.proceed(chain.request().newBuilder().header("User-Agent", "android-compose-arch-starter").build())
      }
      .build()

  @Provides
  @Singleton
  fun provideWikipediaService(client: OkHttpClient): WikipediaService =
    Retrofit.Builder()
      .baseUrl("https://en.wikipedia.org/api/rest_v1/")
      .client(client)
      .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
      .build()
      .create(WikipediaService::class.java)

  @Provides
  @Singleton
  fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
    Room.databaseBuilder(context, AppDatabase::class.java, "articles.db")
      .fallbackToDestructiveMigration(dropAllTables = true)
      .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
      .build()

  @Provides
  fun provideArticleDao(db: AppDatabase): ArticleDao = db.articleDao()

  @Provides
  fun provideTranslationDao(db: AppDatabase): TranslationDao = db.translationDao()

  @Provides
  @Singleton
  fun provideTranslatorService(client: OkHttpClient): TranslatorService =
    Retrofit.Builder()
      .baseUrl("https://api.mymemory.translated.net/")
      .client(client)
      .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
      .build()
      .create(TranslatorService::class.java)

  @Provides
  @Singleton
  fun provideArticleRepo(
    wiki: WikipediaService,
    dao: ArticleDao,
    translator: TranslatorService,
    translationDao: TranslationDao,
  ): ArticleRepo =
    ArticleRepository(wiki, dao, translator, translationDao)
}
