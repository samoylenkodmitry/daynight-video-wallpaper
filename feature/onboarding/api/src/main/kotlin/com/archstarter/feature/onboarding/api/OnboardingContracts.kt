package com.archstarter.feature.onboarding.api

import com.archstarter.core.common.presenter.ParamInit
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
data object Onboarding

data class OnboardingPage(val title: String, val message: String)

val DefaultOnboardingPages = listOf(
    OnboardingPage(
        title = "Welcome to DayNight",
        message = "Assign a unique video to every part of the day and create a living wallpaper that changes with time."
    ),
    OnboardingPage(
        title = "Set the schedule",
        message = "Use solar events or define custom start times for morning, day, evening and night."
    ),
    OnboardingPage(
        title = "Pick your ambience",
        message = "Choose immersive clips from your library. We'll loop them smoothly and keep the audio muted by default."
    ),
    OnboardingPage(
        title = "Apply the wallpaper",
        message = "When you're ready, tap Set Live Wallpaper on the home screen to preview and apply DayNight."
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
