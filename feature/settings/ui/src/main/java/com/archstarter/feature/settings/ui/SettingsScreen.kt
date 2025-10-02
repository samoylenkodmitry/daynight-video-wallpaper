package com.archstarter.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archstarter.core.common.presenter.MocksMap
import com.archstarter.core.common.presenter.PresenterMockKey
import com.archstarter.core.common.presenter.rememberPresenter
import com.archstarter.core.designsystem.LiquidGlassRect
import com.archstarter.core.designsystem.LiquidGlassRectOverlay
import com.archstarter.core.designsystem.AppTheme
import com.archstarter.feature.settings.api.LanguageChooserParams
import com.archstarter.feature.settings.api.LanguageChooserPresenter
import com.archstarter.feature.settings.api.LanguageChooserRole
import com.archstarter.feature.settings.api.LanguageChooserState
import com.archstarter.feature.settings.api.SettingsPresenter
import com.archstarter.feature.settings.api.SettingsState
import com.archstarter.feature.settings.api.supportedLanguages
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    onExit: () -> Unit = {},
) {
    val presenter = rememberPresenter<SettingsPresenter, Unit>()
    val state by presenter.state.collectAsStateWithLifecycle()
    var expandedRole by remember { mutableStateOf<LanguageChooserRole?>(null) }
    val blurModifier = if (expandedRole != null) Modifier.blur(20.dp) else Modifier

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(blurModifier),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Settings", style = MaterialTheme.typography.titleLarge)
                OutlinedButton(onClick = onExit) {
                    Text("Exit", color = MaterialTheme.colorScheme.onSurface)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Native language")
            Spacer(Modifier.height(4.dp))
            LanguageChooserField(
                role = LanguageChooserRole.Native,
                selected = state.nativeLanguage,
                onExpandedChange = { role, expanded ->
                    expandedRole = when {
                        expanded -> role
                        expandedRole == role -> null
                        else -> expandedRole
                    }
                },
            )
            Spacer(Modifier.height(16.dp))
            Text("Learning language")
            Spacer(Modifier.height(4.dp))
            LanguageChooserField(
                role = LanguageChooserRole.Learning,
                selected = state.learningLanguage,
                onExpandedChange = { role, expanded ->
                    expandedRole = when {
                        expanded -> role
                        expandedRole == role -> null
                        else -> expandedRole
                    }
                },
            )
        }
    }
}

internal fun languagePresenterKey(role: LanguageChooserRole): String = "language_${role.name}"

@Suppress("unused")
private val ensureSettingsMocks = SettingsPresenterMocks

@Composable
private fun LanguageChooserField(
    role: LanguageChooserRole,
    selected: String,
    onExpandedChange: (LanguageChooserRole, Boolean) -> Unit,
) {
    val presenter = rememberPresenter<LanguageChooserPresenter, LanguageChooserParams>(
        key = languagePresenterKey(role),
        params = LanguageChooserParams(role = role, selectedLanguage = selected),
    )
    LanguageChooserContent(
        presenter = presenter,
        onExpandedChange = { expanded -> onExpandedChange(role, expanded) },
    )
}

@Composable
private fun LanguageChooserContent(
    presenter: LanguageChooserPresenter,
    onExpandedChange: (Boolean) -> Unit,
) {
    val state by presenter.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var anchorBounds by remember { mutableStateOf<IntRect?>(null) }
    val expandedCallback by rememberUpdatedState(onExpandedChange)

    LaunchedEffect(state.isExpanded, state.query) {
        if (state.isExpanded) {
            listState.scrollToItem(0)
        }
    }

    LaunchedEffect(state.isExpanded) {
        expandedCallback(state.isExpanded)
    }

    Box {
        OutlinedButton(
            onClick = presenter::onToggleExpanded,
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    anchorBounds = coordinates.boundsInWindow().toIntRect()
                },
        ) {
            val label = if (state.selectedLanguage.isBlank()) "Select language" else state.selectedLanguage
            Text(label, color = MaterialTheme.colorScheme.onSurface)
        }
        LanguageDropdownMenu(
            expanded = state.isExpanded,
            anchorBounds = anchorBounds,
            state = state,
            listState = listState,
            onQueryChange = presenter::onQueryChange,
            onRetry = presenter::onRetry,
            onSelect = presenter::onSelect,
            onDismiss = presenter::onDismiss,
        )
    }
}

@Composable
private fun LanguageDropdownMenu(
    expanded: Boolean,
    anchorBounds: IntRect?,
    state: LanguageChooserState,
    listState: LazyListState,
    onQueryChange: (String) -> Unit,
    onRetry: () -> Unit,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!expanded) return

    val anchor = anchorBounds ?: return
    val density = LocalDensity.current
    val widthPx = anchor.width().coerceAtLeast(0)
    val resolvedWidth = if (widthPx > 0) {
        val widthDp = with(density) { widthPx.toDp() }
        maxOf(widthDp, 200.dp)
    } else {
        200.dp
    }
    val verticalMarginPx = with(density) { 8.dp.roundToPx() }
    val positionProvider = remember(anchor, verticalMarginPx) {
        LanguageDropdownPositionProvider(anchor, verticalMarginPx)
    }

    val popupGlassTint = Color(0xFF98A9CF)
    var popupGlassRect by remember { mutableStateOf<LiquidGlassRect?>(null) }

    Popup(
        onDismissRequest = onDismiss,
        popupPositionProvider = positionProvider,
        properties = PopupProperties(focusable = true),
    ) {
        LiquidGlassRectOverlay(
            rect = popupGlassRect,
        ) {
            Surface(
                modifier = Modifier
                    .width(resolvedWidth)
                    .onGloballyPositioned { coordinates ->
                        popupGlassRect = with(density) {
                            val position = coordinates.positionInRoot()
                            LiquidGlassRect(
                                left = position.x.toDp(),
                                top = position.y.toDp(),
                                width = coordinates.size.width.toDp(),
                                height = coordinates.size.height.toDp(),
                                tintColor = popupGlassTint,
                            )
                        }
                    },
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                shape = MaterialTheme.shapes.extraSmall,
                color = Color.Transparent,
            ) {
                Column(Modifier.padding(top = 8.dp)) {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = onQueryChange,
                        placeholder = { Text("Search languages") },
                        singleLine = true,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    val errorMessage = state.errorMessage
                    val results = state.results
                    when {
                        state.isLoading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        errorMessage != null -> {
                            DropdownMenuItem(
                                text = {
                                    Column(Modifier.fillMaxWidth()) {
                                        Text(errorMessage, style = MaterialTheme.typography.bodyMedium)
                                        Spacer(Modifier.height(4.dp))
                                        Text("Tap to retry", style = MaterialTheme.typography.labelSmall)
                                    }
                                },
                                onClick = onRetry,
                            )
                        }

                        results.isEmpty() -> {
                            DropdownMenuItem(
                                text = { Text("No languages found") },
                                enabled = false,
                                onClick = {},
                            )
                        }

                        else -> {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp),
                            ) {
                                items(results) { language ->
                                    DropdownMenuItem(
                                        text = { Text(language) },
                                        onClick = { onSelect(language) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private class LanguageDropdownPositionProvider(
    private val anchor: IntRect,
    private val verticalMarginPx: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val resolvedAnchor = anchor
        val anchorLeft = resolvedAnchor.left
        val anchorRight = resolvedAnchor.right
        val anchorTop = resolvedAnchor.top
        val anchorBottom = resolvedAnchor.bottom

        val horizontalSpace = windowSize.width - popupContentSize.width
        val resolvedX = when (layoutDirection) {
            LayoutDirection.Ltr -> anchorLeft
            LayoutDirection.Rtl -> anchorRight - popupContentSize.width
        }.coerceIn(0, horizontalSpace.coerceAtLeast(0))

        val spaceBelow = windowSize.height - anchorBottom
        val placeBelow = spaceBelow >= popupContentSize.height + verticalMarginPx
        val y = if (placeBelow) {
            (anchorBottom + verticalMarginPx)
        } else {
            val candidate = anchorTop - popupContentSize.height - verticalMarginPx
            if (candidate >= 0) candidate else (windowSize.height - popupContentSize.height).coerceAtLeast(0)
        }

        val clampedY = y.coerceIn(0, (windowSize.height - popupContentSize.height).coerceAtLeast(0))
        return IntOffset(resolvedX, clampedY)
    }
}

private fun Rect.toIntRect(): IntRect {
    return IntRect(
        left = left.roundToInt(),
        top = top.roundToInt(),
        right = right.roundToInt(),
        bottom = bottom.roundToInt(),
    )
}

private fun IntRect.width(): Int = right - left

private object SettingsPresenterMocks {
    private val settingsPresenter = FakeSettingsPresenter()
    private val nativeLanguagePresenter = FakeLanguageChooserPresenter("English")
    private val learningLanguagePresenter = FakeLanguageChooserPresenter("Spanish")

    init {
        if (BuildConfig.DEBUG) {
            MocksMap[PresenterMockKey(SettingsPresenter::class, null)] = settingsPresenter
            MocksMap[
                PresenterMockKey(
                    LanguageChooserPresenter::class,
                    languagePresenterKey(LanguageChooserRole.Native),
                ),
            ] = nativeLanguagePresenter
            MocksMap[
                PresenterMockKey(
                    LanguageChooserPresenter::class,
                    languagePresenterKey(LanguageChooserRole.Learning),
                ),
            ] = learningLanguagePresenter
        }
    }
}

private class FakeSettingsPresenter : SettingsPresenter {
    private val _state = MutableStateFlow(SettingsState())
    override val state: StateFlow<SettingsState> = _state

    override fun onNativeSelected(language: String) {
        _state.update { current -> current.copy(nativeLanguage = language) }
    }

    override fun onLearningSelected(language: String) {
        _state.update { current -> current.copy(learningLanguage = language) }
    }

    override fun initOnce(params: Unit?) {}
}

private class FakeLanguageChooserPresenter(
    initial: String,
) : LanguageChooserPresenter {
    private val languages = supportedLanguages.take(6)
    private val _state = MutableStateFlow(
        LanguageChooserState(
            selectedLanguage = initial,
            results = languages,
        ),
    )
    override val state: StateFlow<LanguageChooserState> = _state

    override fun initOnce(params: LanguageChooserParams?) {
        val actual = params ?: return
        _state.update { it.copy(selectedLanguage = actual.selectedLanguage) }
    }

    override fun onToggleExpanded() {
        _state.update { it.copy(isExpanded = !it.isExpanded) }
    }

    override fun onDismiss() {
        _state.update { it.copy(isExpanded = false) }
    }

    override fun onQueryChange(query: String) {
        _state.update {
            it.copy(
                query = query,
                results = if (query.isBlank()) {
                    languages
                } else {
                    languages.filter { option -> option.contains(query, ignoreCase = true) }
                },
            )
        }
    }

    override fun onSelect(language: String) {
        _state.update { it.copy(selectedLanguage = language, isExpanded = false) }
    }

    override fun onRetry() {}
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun PreviewSettings() {
    AppTheme { SettingsScreen(onExit = {}) }
}
