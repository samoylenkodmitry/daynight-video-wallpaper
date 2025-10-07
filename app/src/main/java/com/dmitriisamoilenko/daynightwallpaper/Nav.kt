package com.dmitriisamoilenko.daynightwallpaper

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.archstarter.core.common.app.App
import com.archstarter.core.common.presenter.LocalPresenterResolver
import com.archstarter.core.common.scope.LocalScreenBuilder
import com.archstarter.core.common.scope.ScreenComponent
import com.archstarter.core.common.scope.ScreenScope
import com.archstarter.core.designsystem.AppTheme
import com.archstarter.feature.catalog.api.WallpaperHome
import com.archstarter.feature.catalog.ui.WallpaperHomeScreen
import com.archstarter.feature.onboarding.api.Onboarding
import com.archstarter.feature.onboarding.api.OnboardingStatusProvider
import com.archstarter.feature.onboarding.ui.OnboardingScreen
import com.archstarter.feature.settings.api.Settings
import com.archstarter.feature.settings.ui.SettingsScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var resolver: HiltPresenterResolver

    @Inject
    lateinit var appManager: AppScopeManager

    @Inject
    lateinit var screenBuilder: ScreenComponent.Builder

    @Inject
    lateinit var onboardingStatus: OnboardingStatusProvider

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                val nav = rememberNavController()
                val app = remember(nav) {
                    val actions = NavigationActions(nav)
                    App(actions).also { appManager.create(it) }
                }
                DisposableEffect(app) {
                    onDispose { appManager.clear() }
                }
                CompositionLocalProvider(
                    LocalPresenterResolver provides resolver,
                    LocalScreenBuilder provides screenBuilder
                ) {
                    val onboardingCompleted by onboardingStatus.hasCompleted.collectAsStateWithLifecycle(initialValue = null)
                    val startDestinationState = remember { mutableStateOf<Any?>(null) }
                    if (startDestinationState.value == null && onboardingCompleted != null) {
                        startDestinationState.value = if (onboardingCompleted == true) WallpaperHome else Onboarding
                    }
                    Box(
                        modifier = Modifier
                            .imePadding()
                            .navigationBarsPadding()
                            .systemBarsPadding()
                            .safeContentPadding()
                            .fillMaxSize()
                    ) {
                        val startDestination = startDestinationState.value
                        if (startDestination != null) {
                            AppNavHost(
                                nav = nav,
                                startDestination = startDestination,
                                onOnboardingFinished = {
                                    nav.navigate(WallpaperHome) {
                                        popUpTo(Onboarding) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavHost(
    nav: NavHostController,
    startDestination: Any,
    onOnboardingFinished: () -> Unit,
) {
    NavHost(nav, startDestination = startDestination) {
        composable<Onboarding> {
            ScreenScope {
                OnboardingScreen(onFinished = onOnboardingFinished)
            }
        }
        composable<WallpaperHome> {
            ScreenScope {
                WallpaperHomeScreen()
            }
        }
        composable<Settings> {
            ScreenScope {
                SettingsScreen(onExit = { nav.popBackStack() })
            }
        }
    }
}
