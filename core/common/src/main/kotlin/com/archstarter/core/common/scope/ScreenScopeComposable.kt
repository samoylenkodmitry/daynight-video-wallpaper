package com.archstarter.core.common.scope

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import dagger.hilt.EntryPoints

private val LocalScreenComponent = staticCompositionLocalOf<Any?> {
  null // Allow null default instead of error
}

// This will be provided by MainActivity
val LocalScreenBuilder = staticCompositionLocalOf<ScreenComponent.Builder> {
  error("LocalScreenBuilder not provided")
}

@Composable
fun ScreenScope(
  nested: Boolean = false,
  content: @Composable () -> Unit
) {
  val screenBuilder = LocalScreenBuilder.current

  // Find nearest component (if any)
  val parentAny = LocalScreenComponent.current

  // Decide which component to provide to children
  val provided: Any = remember(parentAny, nested) {
    when {
      parentAny == null -> screenBuilder.build() // top-level screen component
      nested -> {
        // build a Subscreen from the nearest ScreenComponent
        val parentScreen: ScreenComponent = when (parentAny) {
          is ScreenComponent -> parentAny
          is SubscreenComponent -> error("Cannot nest inside another SubscreenComponent")
          else -> error("Unsupported parent type")
        }
        val subBuilder = EntryPoints.get(parentScreen, SubscreenBuilderEntryPoint::class.java).subBuilder()
        subBuilder.build()
      }
      else -> parentAny // reuse nearest component if not nesting
    }
  }

  DisposableEffect(provided) {
    onDispose {
      // if you introduce Closeable deps, call close() here by EntryPoint
    }
  }

  CompositionLocalProvider(LocalScreenComponent provides provided) {
    content()
  }
}

// Expose LocalScreenComponent for the ViewModel factory
val LocalScreenComponentProvider = LocalScreenComponent