plugins {
  alias(libs.plugins.android.app) apply false
  alias(libs.plugins.android.lib) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.kotlin) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.kotlin.serialization) apply false
  alias(libs.plugins.ksp) apply false
  alias(libs.plugins.hilt) apply false
}

tasks.register("clean", Delete::class) {
  delete(rootProject.buildDir)
}
