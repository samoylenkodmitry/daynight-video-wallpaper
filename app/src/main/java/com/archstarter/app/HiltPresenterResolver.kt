package com.archstarter.app

import androidx.compose.runtime.Composable
import com.archstarter.core.common.presenter.ParamInit
import com.archstarter.core.common.presenter.PresenterProvider
import com.archstarter.core.common.presenter.PresenterResolver
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass

@Singleton
class HiltPresenterResolver @Inject constructor(
    private val presenterProviders: Map<Class<*>, @JvmSuppressWildcards PresenterProvider<*>>
) : PresenterResolver {

  @Composable
  override fun <T : ParamInit<*>> resolve(klass: KClass<T>, key: String?): T {
    println("HiltPresenterResolver: Looking for ${klass.simpleName}")
    println("HiltPresenterResolver: Available keys: ${presenterProviders.keys.map { it.simpleName }}")
    val provider = presenterProviders[klass.java]
        ?: error("No presenter binding for ${klass.simpleName}, map: $presenterProviders")
    
    @Suppress("UNCHECKED_CAST")
    return provider.provide(key) as T
  }
}
