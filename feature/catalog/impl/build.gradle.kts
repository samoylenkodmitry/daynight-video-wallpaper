plugins {
  alias(libs.plugins.android.lib)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.hilt)
  alias(libs.plugins.ksp)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "com.archstarter.feature.catalog.impl"
  compileSdk = 35
  defaultConfig { minSdk = 33 }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }
  kotlinOptions { jvmTarget = "21" }
  buildFeatures { compose = true }
}

dependencies {
  implementation(project(":feature:catalog:api"))
  implementation(project(":core:common"))
  implementation(project(":feature:settings:api"))
  implementation(project(":feature:settings:impl"))

  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)
  implementation(libs.hilt.nav.compose)
  implementation(libs.lifecycle.viewmodel.compose)
  implementation(libs.retrofit.core)
  implementation(libs.retrofit.kotlinx)
  implementation(libs.retrofit.scalars)
  implementation(libs.okhttp.logging)
  implementation(libs.room.runtime)
  implementation(libs.room.ktx)
  implementation(libs.kotlinx.serialization.json)
  ksp(libs.room.compiler)

  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
}
