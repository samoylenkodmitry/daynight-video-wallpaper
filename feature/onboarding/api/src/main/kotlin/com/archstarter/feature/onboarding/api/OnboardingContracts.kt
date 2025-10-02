package com.archstarter.feature.onboarding.api

import com.archstarter.core.common.presenter.ParamInit
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
data object Onboarding

data class OnboardingPage(val title: String, val message: String)

val DefaultOnboardingPages = listOf(
    OnboardingPage(
        title = "Pick your pair",
        message = "Choose your from and to languages in Settings."
    ),
    OnboardingPage(
        title = "Refresh stories",
        message = "Press refresh to grab a random article."
    ),
    OnboardingPage(
        title = "Open and read",
        message = "Tap any card to open the full story."
    ),
    OnboardingPage(
        title = "Tap to translate",
        message = "Touch a word to see it in your learning language."
    )
)

data class OnboardingState(
    val pages: List<OnboardingPage> = DefaultOnboardingPages,
    val completed: Boolean? = null,
)

interface OnboardingPresenter : ParamInit<Unit> {
    val state: StateFlow<OnboardingState>
    fun onContinue()
}

interface OnboardingStatusProvider {
    val hasCompleted: StateFlow<Boolean?>
}
