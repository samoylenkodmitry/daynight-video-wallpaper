package com.archstarter.core.common.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import com.archstarter.core.common.scope.LocalScreenComponentProvider

@Composable
inline fun <reified VM : ViewModel> scopedViewModel(key: String?): VM {
  val owner = checkNotNull(LocalViewModelStoreOwner.current) {
    "magicViewModel() must be called where a ViewModelStoreOwner exists"
  }
  val savedOwner = owner as? SavedStateRegistryOwner
    ?: error("Owner must implement SavedStateRegistryOwner")

  val component = LocalScreenComponentProvider.current
    ?: error("magicViewModel() must be called within a ScreenScope")
  val defaultArgs = (owner as? NavBackStackEntry)?.arguments

  val factory = remember(component, owner, defaultArgs) {
    ScreenVmFactory(savedOwner, defaultArgs, component)
  }
  return viewModel(viewModelStoreOwner = owner, factory = factory, key = key)
}