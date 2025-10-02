package com.archstarter.feature.catalog.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archstarter.core.common.presenter.MocksMap
import com.archstarter.core.common.presenter.PresenterMockKey
import com.archstarter.core.common.presenter.rememberPresenter
import com.archstarter.core.designsystem.AppTheme
import com.archstarter.core.designsystem.GwernDecoratedSpacer
import com.archstarter.core.designsystem.LiquidGlassRect
import com.archstarter.core.designsystem.LiquidGlassRectOverlay
import com.archstarter.feature.catalog.api.CatalogPresenter
import com.archstarter.feature.catalog.api.CatalogState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs

private const val TOP_SPACER_KEY = "catalog_top_spacer"
private const val BOTTOM_SPACER_KEY = "catalog_bottom_spacer"

@Suppress("unused")
private val ensureCatalogScreenMocks = CatalogScreenPresenterMocks

@Composable
fun CatalogScreen() {
  val presenter = rememberPresenter<CatalogPresenter, Unit>()
  val state by presenter.state.collectAsStateWithLifecycle()
  val listState = rememberLazyListState()
  var previousItems by remember { mutableStateOf<List<Int>>(emptyList()) }
  LaunchedEffect(state.items) {
    val newId = state.items.firstOrNull { id -> !previousItems.contains(id) }
    previousItems = state.items
    if (newId != null) {
      val index = state.items.indexOf(newId)
      if (index >= 0) {
        listState.animateScrollToItem(index + 1) // +1 for the top spacer item
      }
    }
  }
  val flingBehavior = rememberSnapFlingBehavior(
    lazyListState = listState,
    snapPosition = SnapPosition.Center
  )
  val density = LocalDensity.current
  var viewportSize by remember { mutableStateOf(IntSize.Zero) }
  var viewportOffset by remember { mutableStateOf(Offset.Zero) }
  var bottomControlsHeight by remember { mutableStateOf(0.dp) }
  var settingsButtonOffset by remember { mutableStateOf<Offset?>(null) }
  var refreshButtonOffset by remember { mutableStateOf<Offset?>(null) }
  var settingsButtonSize by remember { mutableStateOf<IntSize?>(null) }
  var refreshButtonSize by remember { mutableStateOf<IntSize?>(null) }
  val centeredItemInfo by remember {
    derivedStateOf {
      val info = listState.layoutInfo
      val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2
      val visibleItems = info.visibleItemsInfo.filterNot { item ->
        item.key == TOP_SPACER_KEY || item.key == BOTTOM_SPACER_KEY
      }
      if (visibleItems.isEmpty()) {
        null
      } else {
        visibleItems.minByOrNull {
          abs((it.offset + it.size / 2) - viewportCenter)
        }
      }
    }
  }
  val hapticFeedback = LocalHapticFeedback.current
  LaunchedEffect(centeredItemInfo?.key) {
    val centeredKey = centeredItemInfo?.key ?: return@LaunchedEffect
    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
  }
  val centerGlassTint = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
  val targetGlassRect by remember(centerGlassTint) {
    derivedStateOf {
      val info = centeredItemInfo ?: return@derivedStateOf null
      if (viewportSize.width == 0 || viewportSize.height == 0) return@derivedStateOf null
      val layoutInfo = listState.layoutInfo
      val viewportStart = layoutInfo.viewportStartOffset
      val viewportEnd = layoutInfo.viewportEndOffset
      val viewportHeightPx = viewportEnd - viewportStart
      if (viewportHeightPx <= 0) return@derivedStateOf null
      val rawTopPx = (info.offset - viewportStart).toFloat()
      val maxTopPx = (viewportHeightPx - info.size).coerceAtLeast(0)
      val clampedTopPx = rawTopPx.coerceIn(0f, maxTopPx.toFloat())
      with(density) {
        LiquidGlassRect(
          left = 0.dp,
          top = clampedTopPx.toDp(),
          width = viewportSize.width.toDp(),
          height = info.size.toDp(),
          tintColor = centerGlassTint,
        )
      }
    }
  }
  val glassTop by animateDpAsState(
    targetValue = targetGlassRect?.top ?: 0.dp,
    animationSpec = tween(durationMillis = 300, delayMillis = 100),
    label = "glassTop"
  )
  val glassHeight by animateDpAsState(
    targetValue = targetGlassRect?.height ?: 0.dp,
    animationSpec = tween(durationMillis = 300, delayMillis = 100),
    label = "glassHeight"
  )
  val glassWidth by animateDpAsState(
    targetValue = targetGlassRect?.width ?: 0.dp,
    animationSpec = tween(durationMillis = 300, delayMillis = 100),
    label = "glassWidth"
  )
  val glassRect = targetGlassRect?.let { rect ->
    if (glassWidth > 0.dp && glassHeight > 0.dp) {
      rect.copy(top = glassTop, width = glassWidth, height = glassHeight)
    } else {
      null
    }
  }
  val centerGlassRect = remember(glassRect, viewportOffset, density) {
    glassRect?.let { rect ->
      val offsetX = with(density) { viewportOffset.x.toDp() }
      val offsetY = with(density) { viewportOffset.y.toDp() }
      rect.copy(left = rect.left + offsetX, top = rect.top + offsetY)
    }
  }
  val buttonGlassTint = Color(0xFF98A9CF)
  val settingsGlassRect = remember(settingsButtonOffset, settingsButtonSize, density, buttonGlassTint) {
    val buttonOffset = settingsButtonOffset
    val buttonSize = settingsButtonSize
    if (buttonOffset == null || buttonSize == null) {
      null
    } else {
      with(density) {
        LiquidGlassRect(
          left = buttonOffset.x.toDp(),
          top = buttonOffset.y.toDp(),
          width = buttonSize.width.toDp(),
          height = buttonSize.height.toDp(),
          tintColor = buttonGlassTint,
        )
      }
    }
  }
  val refreshGlassRect = remember(refreshButtonOffset, refreshButtonSize, density, buttonGlassTint) {
    val buttonOffset = refreshButtonOffset
    val buttonSize = refreshButtonSize
    if (buttonOffset == null || buttonSize == null) {
      null
    } else {
      with(density) {
        LiquidGlassRect(
          left = buttonOffset.x.toDp(),
          top = buttonOffset.y.toDp(),
          width = buttonSize.width.toDp(),
          height = buttonSize.height.toDp(),
          tintColor = buttonGlassTint,
        )
      }
    }
  }
  val listBottomPadding = 32.dp + bottomControlsHeight
  val itemSpacing = 8.dp
  val centerItemPadding = run {
    val viewportHeightPx = viewportSize.height
    val itemHeightPx = centeredItemInfo?.size ?: 0
    if (viewportHeightPx <= 0 || itemHeightPx <= 0) {
      0.dp
    } else {
      val extraPx = (viewportHeightPx / 2f - itemHeightPx / 2f).coerceAtLeast(0f)
      with(density) { extraPx.toDp() }
    }
  }
  val edgeSpacerHeight = if (centerItemPadding > itemSpacing) {
    centerItemPadding - itemSpacing
  } else {
    0.dp
  }
  val glassRects = remember(centerGlassRect, settingsGlassRect, refreshGlassRect) {
    listOfNotNull(centerGlassRect, settingsGlassRect, refreshGlassRect)
  }

  Box(Modifier.fillMaxSize()) {
    LiquidGlassRectOverlay(
      rects = glassRects,
      modifier = Modifier.fillMaxSize()
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
      ) {
        Box(
          modifier = Modifier
            .weight(1f)
            .onGloballyPositioned { coords -> viewportOffset = coords.positionInRoot() }
            .onSizeChanged { viewportSize = it }
        ) {
          LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(bottom = listBottomPadding),
            verticalArrangement = Arrangement.spacedBy(itemSpacing)
          ) {
            item(key = TOP_SPACER_KEY) {
              GwernDecoratedSpacer(
                height = edgeSpacerHeight,
                isTop = true,
              )
            }
            items(state.items, key = { it }) { id ->
              CatalogItemCard(id = id)
            }
            item(key = BOTTOM_SPACER_KEY) {
              GwernDecoratedSpacer(
                height = edgeSpacerHeight,
                isTop = false,
              )
            }
          }

          if (state.items.isEmpty() && state.isRefreshing) {
            Box(
              modifier = Modifier.fillMaxSize(),
              contentAlignment = Alignment.Center,
            ) {
              CircularProgressIndicator()
            }
          }
        }
      }
    }

    val glassPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
    val transparentButtonColors = ButtonDefaults.buttonColors(
      containerColor = Color.Transparent,
      contentColor = MaterialTheme.colorScheme.onSurface,
      disabledContainerColor = Color.Transparent,
      disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
    )
    val transparentButtonElevation = ButtonDefaults.buttonElevation(
      defaultElevation = 0.dp,
      pressedElevation = 0.dp,
      focusedElevation = 0.dp,
      hoveredElevation = 0.dp,
    )
    Row(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .navigationBarsPadding()
        .onSizeChanged { size ->
          bottomControlsHeight = with(density) { size.height.toDp() }
        },
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier.onGloballyPositioned { coords ->
          settingsButtonOffset = coords.positionInRoot()
          settingsButtonSize = coords.size
        }
      ) {
        Box(modifier = Modifier.padding(glassPadding)) {
          Button(
            onClick = presenter::onSettingsClick,
            colors = transparentButtonColors,
            elevation = transparentButtonElevation,
          ) { Text("Settings", color = MaterialTheme.colorScheme.inverseOnSurface) }
        }
      }
      Box(
        modifier = Modifier.onGloballyPositioned { coords ->
          refreshButtonOffset = coords.positionInRoot()
          refreshButtonSize = coords.size
        }
      ) {
        Box(modifier = Modifier.padding(glassPadding)) {
          Button(
            onClick = presenter::onRefresh,
            enabled = !state.isRefreshing,
            colors = transparentButtonColors,
            elevation = transparentButtonElevation,
          ) {
            if (state.isRefreshing) {
              CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.inverseOnSurface,
              )
              Spacer(Modifier.width(8.dp))
            }
            Text("Random (${state.items.size})", color = MaterialTheme.colorScheme.inverseOnSurface)
          }
        }
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun PreviewCatalog() {
  AppTheme { CatalogScreen() }
}

private object CatalogScreenPresenterMocks {
  private val presenter = FakeCatalogPresenter()

  init {
    if (BuildConfig.DEBUG) {
      MocksMap[PresenterMockKey(CatalogPresenter::class, null)] = presenter
    }
  }
}

private class FakeCatalogPresenter : CatalogPresenter {
  private val _state = MutableStateFlow(CatalogState(items = listOf(1, 2, 3)))
  override val state: StateFlow<CatalogState> = _state

  override fun onRefresh() {
    val current = _state.value
    if (current.items.isNotEmpty()) {
      val rotated = current.items.drop(1) + current.items.first()
      _state.value = current.copy(items = rotated)
    }
  }

  override fun onItemClick(id: Int) {}

  override fun onSettingsClick() {}

  override fun initOnce(params: Unit?) {}
}
