package com.archstarter.feature.onboarding.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archstarter.core.common.presenter.MocksMap
import com.archstarter.core.common.presenter.PresenterMockKey
import com.archstarter.core.common.presenter.rememberPresenter
import com.archstarter.core.designsystem.AppTheme
import com.archstarter.feature.onboarding.api.OnboardingPresenter
import com.archstarter.feature.onboarding.api.OnboardingState
import com.archstarter.feature.onboarding.api.DefaultOnboardingPages
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

@Suppress("unused")
private val ensureOnboardingMocks = OnboardingPresenterMocks

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
) {
    val presenter = rememberPresenter<OnboardingPresenter, Unit>()
    val state by presenter.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.completed) {
        if (state.completed == true) {
            onFinished()
        }
    }

    val pagerState = rememberPagerState(initialPage = 0) { state.pages.size }
    val scope = rememberCoroutineScope()
    val hasPages = state.pages.isNotEmpty()
    val lastIndex = if (hasPages) state.pages.lastIndex else 0
    val isLastPage = hasPages && pagerState.currentPage >= lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(Modifier.height(8.dp))
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            if (page < state.pages.size) {
                val pageData = state.pages[page]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = pageData.title,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = pageData.message,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(state.pages.size) { index ->
                val selected = pagerState.currentPage == index
                val size = if (selected) 12.dp else 8.dp
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(size)
                        .clip(CircleShape)
                        .background(
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = presenter::onContinue) {
                Text("Skip")
            }
            Button(
                onClick = {
                    if (isLastPage) {
                        presenter.onContinue()
                    } else {
                        scope.launch {
                            val next = (pagerState.currentPage + 1).coerceAtMost(lastIndex)
                            pagerState.animateScrollToPage(next)
                        }
                    }
                }
            ) {
                Text(if (isLastPage) "Let's go" else "Next")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingScreenPreview() {
    AppTheme { OnboardingScreen(onFinished = {}) }
}

private object OnboardingPresenterMocks {
    private val presenter = FakeOnboardingPresenter()

    init {
        if (BuildConfig.DEBUG) {
            MocksMap[PresenterMockKey(OnboardingPresenter::class, null)] = presenter
        }
    }
}

private class FakeOnboardingPresenter : OnboardingPresenter {
    private val pages = DefaultOnboardingPages
    private val _state = MutableStateFlow(OnboardingState(completed = false, pages = pages))
    override val state: StateFlow<OnboardingState> = _state

    override fun onContinue() {
        _state.update { current -> current.copy(completed = true) }
    }

    override fun initOnce(params: Unit?) {}
}
