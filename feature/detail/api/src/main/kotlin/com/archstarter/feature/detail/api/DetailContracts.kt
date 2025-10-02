package com.archstarter.feature.detail.api

import com.archstarter.core.common.presenter.ParamInit
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
data class Detail(val id: Int)

data class DetailState(
  val title: String = "",
  val content: String = "",
  val sourceUrl: String = "",
  val originalWord: String = "",
  val translatedWord: String = "",
  val ipa: String? = null,
  val highlightedWord: String? = null,
  val highlightedTranslation: String? = null,
  val wordTranslations: Map<String, String> = emptyMap(),
)

interface DetailPresenter : ParamInit<Int> {
  val state: StateFlow<DetailState>
  fun translate(word: String)
  fun onSourceClick(url: String)
}
