package com.archstarter.core.common.presenter

import kotlin.reflect.KClass

data class PresenterMockKey(
  val klass: KClass<*>,
  val key: String?,
)

val MocksMap: MutableMap<PresenterMockKey, ParamInit<*>> = linkedMapOf()
