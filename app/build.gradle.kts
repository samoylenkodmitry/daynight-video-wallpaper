plugins {
  alias(libs.plugins.android.app)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.hilt)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.ksp)
}

android {
  namespace = "com.archstarter.app"
  compileSdk = 35
  defaultConfig {
    applicationId = "com.archstarter.app"
    minSdk = 33
    targetSdk = 35
    versionCode = 1
    versionName = "0.1.0"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }
  buildFeatures { compose = true }
  composeOptions { kotlinCompilerExtensionVersion = libs.versions.compose.get() }
  packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }
  kotlinOptions { jvmTarget = "21" }

  signingConfigs {
    getByName("debug")
  }
  buildTypes {
    getByName("release") {
      signingConfig = signingConfigs.getByName("debug")
    }
  }

}

dependencies {
  implementation(project(":core:designsystem"))
  implementation(project(":core:common"))
  implementation(project(":feature:onboarding:api"))
  implementation(project(":feature:onboarding:ui"))
  implementation(project(":feature:onboarding:impl"))
  implementation(project(":feature:catalog:api"))
  implementation(project(":feature:catalog:ui"))
  implementation(project(":feature:catalog:impl"))
  implementation(project(":feature:settings:api"))
  implementation(project(":feature:settings:ui"))
  implementation(project(":feature:settings:impl"))

  implementation(libs.activity.compose)
  implementation(libs.compose.ui)
  implementation(libs.compose.material3)
  implementation(libs.material)
  implementation(libs.compose.preview)
  debugImplementation(libs.compose.tooling)

  implementation(libs.lifecycle.runtime.compose)
  implementation(libs.lifecycle.viewmodel.compose)
  implementation(libs.navigation.compose)
  implementation(libs.media3.exoplayer)

  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)
  implementation(libs.hilt.nav.compose)

  androidTestImplementation(libs.compose.ui.test.junit4)
  debugImplementation(libs.compose.ui.test.manifest)
  androidTestImplementation(libs.test.ext.junit)
  androidTestImplementation(libs.test.espresso.core)
}
