plugins {
  alias(libs.plugins.android.lib)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.hilt)
  alias(libs.plugins.ksp)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "com.archstarter.core.common"
  compileSdk = 35
  defaultConfig { minSdk = 33 }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  composeOptions { kotlinCompilerExtensionVersion = libs.versions.compose.get() }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }
  kotlinOptions { jvmTarget = "21" }
}

dependencies {
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.compose.runtime)
  implementation(libs.compose.ui)
  implementation(libs.compose.material3)
  implementation(libs.lifecycle.viewmodel.compose)
  implementation(libs.lifecycle.runtime.compose)
  implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.9.3")
  implementation("androidx.savedstate:savedstate:1.2.1")
  implementation(libs.activity.compose)
  implementation(libs.javax.inject)
  implementation(libs.hilt.android)
  implementation(libs.navigation.compose)
  ksp(libs.hilt.compiler)
  
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
}

