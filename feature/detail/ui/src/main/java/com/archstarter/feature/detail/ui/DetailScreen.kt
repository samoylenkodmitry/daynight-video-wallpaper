package com.archstarter.feature.detail.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archstarter.core.common.presenter.MocksMap
import com.archstarter.core.common.presenter.PresenterMockKey
import com.archstarter.core.common.presenter.rememberPresenter
import com.archstarter.core.designsystem.AppTheme
import com.archstarter.core.designsystem.LiquidGlassRect
import com.archstarter.core.designsystem.LiquidGlassRectOverlay
import com.archstarter.feature.detail.api.DetailPresenter
import com.archstarter.feature.detail.api.DetailState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.max
import kotlin.math.min
import java.util.LinkedHashMap
import java.util.Locale

internal const val DETAIL_DISPLAY_PAD_CHAR: Char = '\u00A0'

@Suppress("unused")
private val ensureDetailMocks = DetailPresenterMocks

@Composable
fun DetailScreen(id: Int) {
  val presenter = rememberPresenter<DetailPresenter, Int>(params = id)
  val state by presenter.state.collectAsStateWithLifecycle()
  val density = LocalDensity.current
  val hapticFeedback = LocalHapticFeedback.current
  val viewConfiguration = LocalViewConfiguration.current
  val highlightPaddingX = 20.dp
  val highlightPaddingY = 10.dp
  val highlightPaddingXPx = with(density) { highlightPaddingX.toPx() }
  val highlightPaddingYPx = with(density) { highlightPaddingY.toPx() }
  val longPressPointerOffset = 56.dp
  val longPressPointerOffsetPx = with(density) { longPressPointerOffset.toPx() }
  var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
  var highlightWordIndex by remember { mutableStateOf<Int?>(null) }
  var currentNormalizedWord by remember { mutableStateOf<String?>(null) }
  var targetLocalRect by remember { mutableStateOf<LiquidRectPx?>(null) }
  var targetRect by remember { mutableStateOf<LiquidRectPx?>(null) }
  var textPositionInRoot by remember { mutableStateOf(Offset.Zero) }
  val animLeft = remember { Animatable(0f) }
  val animTop = remember { Animatable(0f) }
  val animWidth = remember { Animatable(0f) }
  val animHeight = remember { Animatable(0f) }

  LaunchedEffect(targetLocalRect, textPositionInRoot) {
    targetRect = targetLocalRect?.offsetBy(textPositionInRoot.x, textPositionInRoot.y)
  }

  LaunchedEffect(targetRect) {
    targetRect?.let { rect ->
      coroutineScope {
        launch { animLeft.animateTo(rect.left) }
        launch { animTop.animateTo(rect.top) }
        launch { animWidth.animateTo(rect.width) }
        launch { animHeight.animateTo(rect.height) }
      }
    }
  }

  Column(Modifier.padding(16.dp)) {
    Text(state.title, style = MaterialTheme.typography.headlineSmall)
    val content = state.content
    val words = remember(content) { content.toWordEntries() }
    LaunchedEffect(content) {
      highlightWordIndex = null
      currentNormalizedWord = null
      targetLocalRect = null
      targetRect = null
      animLeft.snapTo(0f)
      animTop.snapTo(0f)
      animWidth.snapTo(0f)
      animHeight.snapTo(0f)
      val candidates = words.filter { it.normalized.isNotBlank() }
      if (candidates.isNotEmpty()) {
        val randomWord = candidates.random()
        highlightWordIndex = randomWord.index
        val normalized = randomWord.normalized
        if (currentNormalizedWord != normalized) {
          currentNormalizedWord = normalized
          presenter.translate(normalized)
        }
      }
    }
    val translations = state.wordTranslations
    val textStyle: TextStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp)
    val textMeasurer = rememberTextMeasurer()
    val measureTextWidth = remember(textMeasurer, textStyle) {
      { text: String ->
        if (text.isEmpty()) 0f else textMeasurer.measure(AnnotatedString(text), style = textStyle).size.width.toFloat()
      }
    }
    val activeTranslation = state.highlightedTranslation?.takeIf {
      val normalized = currentNormalizedWord
      normalized != null && normalized == state.highlightedWord
    }
    val display = remember(content, words, highlightWordIndex, activeTranslation, translations, measureTextWidth) {
      buildDisplayContent(content, words, highlightWordIndex, activeTranslation, translations, measureTextWidth)
    }
    val displayContent = display.text
    val displayBounds = display.bounds
    val highlightBounds = highlightWordIndex?.let { index ->
      displayBounds.firstOrNull { it.index == index }?.textBounds
    }
    val highlightRect = if (highlightWordIndex != null) {
      val width = animWidth.value
      val height = animHeight.value
      if (width > 0f && height > 0f) {
        with(density) {
          LiquidGlassRect(
            left = animLeft.value.toDp(),
            top = animTop.value.toDp(),
            width = width.toDp(),
            height = height.toDp()
          )
        }
      } else {
        null
      }
    } else {
      null
    }

    val layoutState = rememberUpdatedState(layout)
    val displayContentState = rememberUpdatedState(displayContent)
    val displayBoundsState = rememberUpdatedState(displayBounds)
    val wordsState = rememberUpdatedState(words)
    val translateState = rememberUpdatedState<(String) -> Unit> { normalized ->
      presenter.translate(normalized)
    }
    val hapticState = rememberUpdatedState(hapticFeedback)
    val viewConfigurationState = rememberUpdatedState(viewConfiguration)
    val highlightPaddingXState = rememberUpdatedState(highlightPaddingXPx)
    val highlightPaddingYState = rememberUpdatedState(highlightPaddingYPx)
    val pointerOffsetState = rememberUpdatedState(longPressPointerOffsetPx)

    LiquidGlassRectOverlay(rect = highlightRect) {
      Text(
        text = displayContent,
        style = textStyle,
        onTextLayout = { result ->
          layout = result
          val range = highlightBounds
          if (range != null) {
            result.toLiquidRect(range, highlightPaddingXPx, highlightPaddingYPx)?.let {
              targetLocalRect = it
            }
          }
        },
        modifier = Modifier
          .onGloballyPositioned { coordinates ->
            textPositionInRoot = coordinates.positionInRoot()
          }
          .pointerInput(Unit) {
            awaitEachGesture {
              var offsetActivated = false
              var lastRawPosition: Offset? = null

              fun process(rawPosition: Offset) {
                val layoutResult = layoutState.value ?: return
                val displayText = displayContentState.value
                if (displayText.isEmpty()) return
                val pointerOffset = pointerOffsetState.value
                val adjustedPosition = if (offsetActivated) {
                  rawPosition.copy(y = max(0f, rawPosition.y - pointerOffset))
                } else {
                  rawPosition
                }
                val textLength = displayText.length
                val rawOffset = layoutResult
                  .getOffsetForPosition(adjustedPosition)
                  .coerceIn(0, textLength)
                val searchOffset = if (rawOffset == textLength) rawOffset - 1 else rawOffset
                if (searchOffset < 0) return
                val bounds = displayBoundsState.value
                val wordBounds = bounds.firstOrNull { it.contains(searchOffset) } ?: return
                val entries = wordsState.value
                val entry = entries.getOrNull(wordBounds.index) ?: return
                val normalized = entry.normalized
                if (normalized.isBlank()) return
                val paddingX = highlightPaddingXState.value
                val paddingY = highlightPaddingYState.value
                layoutResult.toLiquidRect(
                  wordBounds.textBounds,
                  paddingX,
                  paddingY
                )?.let { targetLocalRect = it }
                if (highlightWordIndex != wordBounds.index) {
                  highlightWordIndex = wordBounds.index
                }
                if (currentNormalizedWord != normalized) {
                  currentNormalizedWord = normalized
                  translateState.value(normalized)
                }
              }

              val down = awaitFirstDown(requireUnconsumed = false)
              offsetActivated = false
              lastRawPosition = down.position
              process(down.position)
              val longPressTimeout = viewConfigurationState.value.longPressTimeoutMillis
              val longPressDeadline = down.uptimeMillis + longPressTimeout
              var longPressTriggered = false
              var lastEventTime = down.uptimeMillis

              try {
                while (true) {
                  val timeRemaining = if (longPressTriggered) {
                    null
                  } else {
                    (longPressDeadline - lastEventTime).coerceAtLeast(0L)
                  }
                  val event = if (timeRemaining == null) {
                    awaitPointerEvent()
                  } else {
                    withTimeoutOrNull(timeRemaining) { awaitPointerEvent() }
                  }

                  if (event == null) {
                    if (!longPressTriggered) {
                      longPressTriggered = true
                      offsetActivated = true
                      hapticState.value.performHapticFeedback(HapticFeedbackType.LongPress)
                      lastRawPosition?.let { process(it) }
                    }
                    continue
                  }

                  val change = event.changes.firstOrNull { it.id == down.id } ?: continue
                  lastEventTime = change.uptimeMillis
                  if (!change.pressed) {
                    break
                  }
                  lastRawPosition = change.position
                  if (!longPressTriggered && change.uptimeMillis >= longPressDeadline) {
                    longPressTriggered = true
                    offsetActivated = true
                    hapticState.value.performHapticFeedback(HapticFeedbackType.LongPress)
                  }
                  process(change.position)
                }
              } finally {
                offsetActivated = false
                lastRawPosition = null
              }
            }
          }
      )
    }
    if (state.ipa != null) {
      Text("IPA: ${state.ipa}", style = MaterialTheme.typography.bodyMedium)
    }
    val sourceUrl = state.sourceUrl
    Text(
      text = "Source: $sourceUrl",
      style = MaterialTheme.typography.bodyMedium,
      modifier = Modifier.clickable(enabled = sourceUrl.isNotBlank()) {
        presenter.onSourceClick(sourceUrl)
      }
    )
  }
}

private data class LiquidRectPx(val left: Float, val top: Float, val width: Float, val height: Float) {
  fun offsetBy(dx: Float, dy: Float): LiquidRectPx = copy(left = left + dx, top = top + dy)
}

internal data class TextBounds(val start: Int, val end: Int) {
  val length: Int get() = end - start
}

internal data class WordEntry(
  val index: Int,
  val start: Int,
  val end: Int,
  val text: String,
  val normalized: String,
  val prefix: String,
  val suffix: String
)

internal data class DisplayContent(
  val text: String,
  val bounds: List<DisplayWordBounds>
)

internal data class DisplayWordBounds(val index: Int, val start: Int, val end: Int) {
  fun contains(offset: Int): Boolean = offset in start until end
  val textBounds: TextBounds get() = TextBounds(start, end)
}

internal fun String.toWordEntries(): List<WordEntry> {
  if (isEmpty()) return emptyList()
  val entries = mutableListOf<WordEntry>()
  var index = 0
  var pos = 0
  while (pos < length) {
    while (pos < length && this[pos].isWhitespace()) {
      pos++
    }
    if (pos >= length) break
    val start = pos
    while (pos < length && !this[pos].isWhitespace()) {
      pos++
    }
    val end = pos
    val word = substring(start, end)
    val firstLetter = word.indexOfFirst { it.isLetterOrDigit() }
    if (firstLetter == -1) {
      entries += WordEntry(
        index = index++,
        start = start,
        end = end,
        text = word,
        normalized = "",
        prefix = word,
        suffix = ""
      )
    } else {
      val lastLetter = word.indexOfLast { it.isLetterOrDigit() }
      val prefix = if (firstLetter > 0) word.substring(0, firstLetter) else ""
      val suffix = if (lastLetter + 1 < word.length) word.substring(lastLetter + 1) else ""
      val normalized = word.substring(firstLetter, lastLetter + 1)
      entries += WordEntry(
        index = index++,
        start = start,
        end = end,
        text = word,
        normalized = normalized,
        prefix = prefix,
        suffix = suffix
      )
    }
  }
  return entries
}

internal fun buildDisplayContent(
  content: String,
  words: List<WordEntry>,
  highlightIndex: Int?,
  translation: String?,
  translations: Map<String, String>,
  measureWidth: (String) -> Float = ::defaultMeasureWidth,
): DisplayContent {
  if (words.isEmpty()) return DisplayContent(content, emptyList())
  val builder = StringBuilder(content.length + (translation?.length ?: 0))
  val bounds = ArrayList<DisplayWordBounds>(words.size)
  var cursor = 0
  for (entry in words) {
    if (cursor < entry.start) {
      builder.append(content, cursor, entry.start)
    }
    val start = builder.length
    val normalized = entry.normalized
    val displayWord = if (normalized.isEmpty()) {
      entry.text
    } else {
      val key = normalized.lowercase(Locale.ROOT)
      val cachedTranslation = translations[key]?.takeIf { it.isNotEmpty() }
      val isHighlighted = highlightIndex != null && translation != null && entry.index == highlightIndex
      val highlightedTranslation = translation?.takeIf { isHighlighted && it.isNotEmpty() }
      val variantCandidates = buildList {
        add(
          VariantCandidate(
            kind = VariantKind.BASE,
            prefix = entry.prefix,
            core = normalized,
            suffix = entry.suffix,
          )
        )
        cachedTranslation?.let { translation ->
          add(
            VariantCandidate(
              kind = VariantKind.CACHED,
              prefix = entry.prefix,
              core = translation,
              suffix = entry.suffix,
            )
          )
        }
        highlightedTranslation?.let { translation ->
          add(
            VariantCandidate(
              kind = VariantKind.HIGHLIGHT,
              prefix = entry.prefix,
              core = translation,
              suffix = entry.suffix,
            )
          )
        }
      }
      val paddedVariants = padVariantsToWidth(variantCandidates, measureWidth)
      when {
        highlightedTranslation != null ->
          paddedVariants[VariantKind.HIGHLIGHT]?.text
            ?: paddedVariants[VariantKind.CACHED]?.text
            ?: paddedVariants.getValue(VariantKind.BASE).text
        else -> paddedVariants.getValue(VariantKind.BASE).text
      }
    }
    builder.append(displayWord)
    val end = builder.length
    bounds += DisplayWordBounds(entry.index, start, end)
    cursor = entry.end
  }
  if (cursor < content.length) {
    builder.append(content, cursor, content.length)
  }
  return DisplayContent(builder.toString(), bounds)
}

private enum class VariantKind { BASE, CACHED, HIGHLIGHT }

private data class VariantCandidate(
  val kind: VariantKind,
  val prefix: String,
  val core: String,
  val suffix: String,
) {
  val text: String = prefix + core + suffix
}

private data class VariantResult(
  val prefix: String,
  val core: String,
  val suffix: String,
  val width: Float,
) {
  val text: String = prefix + core + suffix
}

private fun padVariantsToWidth(
  variants: List<VariantCandidate>,
  measureWidth: (String) -> Float,
): Map<VariantKind, VariantResult> {
  if (variants.isEmpty()) return emptyMap()
  val results = LinkedHashMap<VariantKind, VariantResult>(variants.size)
  var targetWidth = 0f
  for (variant in variants) {
    val text = variant.text
    val width = measureWidth(text)
    results[variant.kind] = VariantResult(
      prefix = variant.prefix,
      core = variant.core,
      suffix = variant.suffix,
      width = width,
    )
    if (width > targetWidth) {
      targetWidth = width
    }
  }
  var iterations = 0
  var updated: Boolean
  do {
    updated = false
    for ((kind, result) in results) {
      if (result.width + PAD_WIDTH_EPSILON >= targetWidth) continue
      val padded = padToWidth(result, targetWidth, measureWidth)
      if (padded.width > result.width + PAD_WIDTH_EPSILON) {
        results[kind] = padded
        updated = true
      }
    }
    val newTarget = results.values.maxOf { it.width }
    if (newTarget > targetWidth + PAD_WIDTH_EPSILON) {
      targetWidth = newTarget
      updated = true
    }
    iterations++
  } while (updated && iterations < MAX_VARIANT_ALIGNMENT_PASSES)
  return results
}

private fun padToWidth(
  variant: VariantResult,
  targetWidth: Float,
  measureWidth: (String) -> Float,
): VariantResult {
  if (variant.text.isEmpty()) return variant
  var width = variant.width
  if (width + PAD_WIDTH_EPSILON >= targetWidth) {
    return variant
  }
  val builder = StringBuilder(variant.core.length + 8)
  builder.append(variant.core)
  var lastWidth = width
  var padCount = 0
  var leftPads = 0
  var rightPads = 0
  while (width + PAD_WIDTH_EPSILON < targetWidth && padCount < MAX_PADDING_PER_VARIANT) {
    if (leftPads <= rightPads) {
      builder.insert(0, DETAIL_DISPLAY_PAD_CHAR)
      leftPads++
    } else {
      builder.append(DETAIL_DISPLAY_PAD_CHAR)
      rightPads++
    }
    val paddedCore = builder.toString()
    val paddedText = variant.prefix + paddedCore + variant.suffix
    val newWidth = measureWidth(paddedText)
    if (newWidth <= lastWidth + MIN_WIDTH_DELTA) {
      return VariantResult(variant.prefix, paddedCore, variant.suffix, newWidth)
    }
    width = newWidth
    lastWidth = newWidth
    padCount++
  }
  val finalCore = builder.toString()
  return VariantResult(variant.prefix, finalCore, variant.suffix, width)
}

private fun defaultMeasureWidth(text: String): Float = text.length.toFloat()

private const val MAX_PADDING_PER_VARIANT = 128
private const val MAX_VARIANT_ALIGNMENT_PASSES = 6
private const val PAD_WIDTH_EPSILON = 0.5f
private const val MIN_WIDTH_DELTA = 0.01f

private fun TextLayoutResult.toLiquidRect(
  range: TextBounds?,
  paddingX: Float,
  paddingY: Float
): LiquidRectPx? {
  if (range == null || range.length <= 0) return null
  val layoutTextLength = layoutInput.text.length
  if (layoutTextLength <= 0) return null
  val start = range.start.coerceAtLeast(0)
  if (start >= layoutTextLength) return null
  val end = min(range.end, layoutTextLength)
  if (end <= start) return null
  val lastIndex = end - 1
  val startBox = getBoundingBox(start)
  val endBox = getBoundingBox(lastIndex)
  val left = startBox.left
  val top = min(startBox.top, endBox.top)
  val right = endBox.right
  val bottom = max(startBox.bottom, endBox.bottom)
  val expandedLeft = left - paddingX
  val expandedTop = top - paddingY
  val expandedWidth = (right - left + paddingX * 2).coerceAtLeast(0f)
  val expandedHeight = (bottom - top + paddingY * 2).coerceAtLeast(0f)
  return LiquidRectPx(expandedLeft, expandedTop, expandedWidth, expandedHeight)
}

@Preview(showBackground = true)
@Composable
private fun PreviewDetail() {
  AppTheme { DetailScreen(id = 1) }
}

private object DetailPresenterMocks {
  private val presenter = FakeDetailPresenter()

  init {
    if (BuildConfig.DEBUG) {
      MocksMap[PresenterMockKey(DetailPresenter::class, null)] = presenter
    }
  }
}

private class FakeDetailPresenter : DetailPresenter {
  private val _state = MutableStateFlow(
    DetailState(title = "Preview", content = "Swipe to translate highlighted words."),
  )
  override val state: StateFlow<DetailState> = _state

  override fun initOnce(params: Int?) {}

  override fun translate(word: String) {}

  override fun onSourceClick(url: String) {}
}
