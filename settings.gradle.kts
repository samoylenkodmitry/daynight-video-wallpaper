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
rootProject.name = "android-compose-arch-starter"

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
  ":feature:detail:api",
  ":feature:detail:ui",
  ":feature:detail:impl",
  ":feature:settings:api",
  ":feature:settings:ui",
  ":feature:settings:impl"
)
