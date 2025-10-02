plugins {
  alias(libs.plugins.android.lib)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "com.archstarter.feature.settings.ui"
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
  implementation(project(":feature:settings:api"))
  implementation(project(":core:designsystem"))
  implementation(project(":core:common"))

  implementation(libs.compose.ui)
  implementation(libs.compose.material3)
  implementation(libs.compose.preview)
  implementation(libs.lifecycle.runtime.compose)
  debugImplementation(libs.compose.tooling)
}
