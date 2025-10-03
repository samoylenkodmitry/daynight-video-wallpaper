pluginManagement {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
    maven("https://maven.pkg.jetbrains.space/kotlin/p/compose/dev")
  }
}
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}
rootProject.name = "daynight-video-wallpaper"

include(
  ":app",
  ":core:designsystem",
  ":core:common",
  ":feature:onboarding:api",
  ":feature:onboarding:ui",
  ":feature:onboarding:impl",
  ":feature:catalog:api",
  ":feature:catalog:ui",
  ":feature:catalog:impl",
  ":feature:settings:api",
  ":feature:settings:ui",
  ":feature:settings:impl"
)
