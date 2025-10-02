package com.archstarter.feature.catalog.impl.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class WikipediaSummary(
  val pageid: Int,
  val title: String,
  val extract: String,
  @SerialName("content_urls") val contentUrls: ContentUrls
) {
  @Serializable
  data class ContentUrls(@SerialName("desktop") val desktop: Desktop) {
    @Serializable
    data class Desktop(val page: String)
  }
}

interface WikipediaService {
  @GET("page/random/summary")
  suspend fun randomSummary(): WikipediaSummary
}

interface SummarizerService {
  @GET("{prompt}")
  suspend fun summarize(@Path("prompt") prompt: String): String
}

@Serializable
data class TranslationResponse(
  @SerialName("responseData") val responseData: TranslationData,
  @SerialName("responseStatus") val responseStatus: Int = 200,
  @SerialName("responseDetails") val responseDetails: String? = null
)
@Serializable
data class TranslationData(@SerialName("translatedText") val translatedText: String)

interface TranslatorService {
  @GET("get")
  suspend fun translate(
    @Query("q") word: String,
    @Query("langpair") langPair: String
  ): TranslationResponse
}

@Serializable
data class DictionaryEntry(val word: String, val phonetics: List<Phonetic> = emptyList())
@Serializable
data class Phonetic(val text: String? = null)

interface DictionaryService {
  @GET("api/v2/entries/en/{word}")
  suspend fun lookup(@Path("word") word: String): List<DictionaryEntry>
}
