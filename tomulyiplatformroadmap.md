# Compose Multiplatform Migration Roadmap

This roadmap grows **android-compose-arch-starter** from an Android-only experience into a Compose Multiplatform product that reuses one presenter-driven codebase for **androidcompose**, **desktopcompose**, **ioscompose**, and **webcompose**.

## Verified current state

| Area | What exists today |
| --- | --- |
| Android entry point | `app/src/main/java/com/dmitriisamoilenko/daynightwallpaper/MainActivity` drives `AppNavHost` with `androidx.navigation.compose` and wires presenters via `HiltPresenterResolver`, `AppScopeManager`, and the Hilt multibindings in `PresenterModule.kt`.
| Presenter plumbing | `core/common` packages (`presenter/`, `scope/`, `app/`, `viewmodel/`) expose `ParamInit`, Hilt-friendly `PresenterProvider`, and Android `ViewModel` bridges such as `MagicViewModel` and `ScreenVmFactory`.
| UI system | `core/designsystem/src/main/java/com/archstarter/core/designsystem/Theme.kt` and `LiquidGlass.kt` provide Compose theming, but the module is Android-only.
| Feature split | Each feature keeps `api`, `ui`, and `impl` Android library modules. For example, `feature/catalog/ui/CatalogScreen.kt` consumes `collectAsStateWithLifecycle` while `feature/catalog/impl/data/ArticleRepository.kt` builds Retrofit + Room stacks and registers them through Hilt modules inside `CatalogImpl.kt`.
| Persistence & networking | Retrofit/OkHttp clients, Room DAOs, and DataStore live inside feature `impl` modules (e.g., `SettingsRepository.kt` uses `preferencesDataStore`).
| Tooling | Gradle uses Kotlin 2.0.21, Compose 1.9.0, Hilt 2.57.1, and Android-only plugins defined in `gradle/libs.versions.toml`.

These modules are tightly coupled to Hilt, Android Lifecycle, and Android-specific storage APIs. Presenter resolution currently assumes Hilt multibindings and Android `ComponentActivity` lifecycles.

## Target module & folder layout

```
root
├── app/ (Android wrapper – retains Hilt only while bridging)
├── shared/
│   ├── foundation/ (KMP, owns presenters, navigation, scope abstractions)
│   ├── designsystem/ (KMP Compose theme + tokens)
│   ├── data/ (KMP networking, persistence, serialization)
│   └── platform/ (expect/actual entry points, system services)
├── feature/
│   ├── catalog/ (single KMP module with commonMain/androidMain/desktopMain/iosMain/wasmJsMain)
│   ├── detail/ (same pattern)
│   └── settings/ (same pattern)
├── desktopApp/ (Compose JVM entry)
├── iosApp/ (Gradle Xcode integration)
└── webApp/ (Compose WASM host)
```

* `shared/foundation` supersedes `core/common`. `commonMain` hosts presenter contracts, navigation actions, scope helpers, and Kotlin Inject component definitions. Platform source sets provide lifecycle adapters—`androidMain` keeps `ComponentActivity` helpers until Android code switches fully to the shared navigation host.
* `shared/designsystem` replaces `core/designsystem`, splitting visual tokens into `commonMain` while delegating font loading or platform colors to source-set actuals.
* `shared/data` centralises KMP-friendly networking (Ktor) and persistence (SQLDelight, Multiplatform Settings). Feature modules depend on it instead of bundling Retrofit/Room/DataStore directly.
* Each feature collapses `api/ui/impl` into one multiplatform module. Inside `commonMain`, keep contracts, presenters, and composables. Android-specific bindings (e.g., Activity intents) move to `androidMain`, and temporary adapters can live in `androidMain` until replacements exist for other targets.

## Shared vs. platform responsibilities

| Layer | `commonMain` | `androidMain` | `desktopMain` | `iosMain` | `wasmJsMain` |
| --- | --- | --- | --- | --- | --- |
| Presenter lifecycle | Coroutine-based `PresenterScope`, state holders, parameter initialization | Bridge to lifecycle-aware components using `LifecycleOwner` and `SavedStateHandle` shims | Window lifecycle hooks, clipboard integration | `UIViewController` retention hooks | Browser visibility / history integration |
| Navigation | Route models, stack abstraction, link intents | Backed by `NavHostController` until replaced, `Intent` deep links | `AppWindowNavigator` integration | URL handling through UIKit | `window.history` adapters |
| Persistence | Interfaces for settings, database access, file I/O | Uses AndroidX DataStore / SQLite wrappers only while migrating | File-based caches via JVM APIs | Keychain/UserDefaults actuals | `localStorage`/IndexedDB actuals |
| Design system | Color/spacing/type tokens | Material 3 dynamic colors, fonts from resources | Desktop-specific window chrome metrics | iOS typography scaling | Browser theming and viewport metrics |

## Migration stages

### Stage 0 – Tooling and workspace baseline
* Apply `org.jetbrains.compose` and `kotlin("multiplatform")` plugins in the root build and convert `core` and feature modules to multiplatform Gradle scripts while keeping the Android target active.
* Introduce `:shared:foundation`, `:shared:designsystem`, and `:shared:data` modules with empty `commonMain` source sets and register them in `settings.gradle.kts`.
* Wire Compose Multiplatform dependencies into `gradle/libs.versions.toml`, enabling desktop, iOS, and WASM artifacts, and keep existing Android configurations compiling.
* Add Kotlin Inject (compiler plugin + `me.tatarka.inject:kotlin-inject-runtime`) to the dependency catalog and configure KSP for every target that will participate in dependency graph generation.

### Stage 1 – Shared presenter & UI foundation
* Move `ParamInit`, `PresenterResolver`, `ScreenComponent`, `ScreenScope`, and navigation models from `core/common` into `shared/foundation` `commonMain`.
* Replace `MagicViewModel`/`ScreenVmFactory` with a multiplatform `PresenterScope` backed by `CoroutineScope` and `StateFlow`. Provide Android actuals for lifecycle-aware cancellation so existing Compose screens keep working.
* Port `Theme.kt` and `LiquidGlass.kt` into `shared/designsystem` `commonMain`; create expect/actual entry points for platform-specific resources such as fonts.
* Update `feature/*/ui` composables to use a shared `collectAsStateWithLifecycle` replacement built on `StateFlow.collectAsState` plus platform lifecycle hooks from `shared/foundation`.

### Stage 2 – Dependency injection transition
* Introduce Kotlin Inject components in `shared/foundation` to provide presenter factories and app-level singletons. Model `@AppScope` and `@ScreenScope` annotations that mirror the existing scope abstractions and generate graph entry points with `@Component`.
* Translate `HiltPresenterResolver` into a resolver backed by Kotlin Inject. Supply an Android adapter in `androidMain` that continues to read Hilt-provided implementations until each feature has Kotlin Inject bindings.
* Gradually replace Hilt modules inside feature `impl` code (e.g., `CatalogImpl.kt` and `SettingsRepository.kt` bindings) with Kotlin Inject `@Provides` functions or constructor injection inside the new multiplatform feature modules.
* Once feature bindings compile with Kotlin Inject, reduce `AppComponent`, `AppScopeManager`, and `PresenterModule.kt` to wrappers that bridge the generated Kotlin Inject components into Android. Remove Hilt from Gradle and delete `HiltPresenterResolver` once all presenters resolve through the multiplatform graph.

### Stage 3 – Data & platform service convergence
* Move Retrofit-based services in `feature/catalog/impl/data/ArticleRepository.kt` and related DAO code into `shared/data` using Ktor clients with platform engines. Generate SQLDelight schemas that match existing Room entities and expose multiplatform DAO interfaces.
* Replace DataStore usage in `feature/settings/impl/data/SettingsRepository.kt` with `MultiplatformSettings`. Provide Android actuals that delegate to existing DataStore until the new storage path is validated.
* Define expect/actual interfaces for logging, network reachability, locale utilities, and file storage. Android actuals can wrap existing helpers; other platforms receive minimal implementations to unblock compilation.

### Stage 4 – Feature module convergence
* Merge each `api`, `ui`, and `impl` pair into a single multiplatform module (`feature/catalog`, `feature/detail`, `feature/settings`). Preserve the API surface by re-exporting contracts from `commonMain` and keep platform specifics (e.g., `Intent` builders) in source-set actuals.
* Relocate presenter classes (e.g., `CatalogPresenter`, `DetailPresenter`, `SettingsPresenter`) and their tests into `commonMain`/`commonTest`. Adjust imports to use the new presenter scope and Kotlin Inject entry points.
* Provide Android adapters in `androidMain` for integrations that still need platform-specific plumbing (notifications, share sheets) and stub them elsewhere.

### Stage 5 – Platform entry points & distribution
* Shrink `app/` into an Android launcher that simply installs platform services, hands an Android navigation controller to the shared navigation host, and renders the shared root composable. Kotlin Inject graph creation happens in shared code; Android only provides platform-specific bindings via actual implementations.
* Add `desktopApp/` with a Compose `application {}` entry hosting the shared root and hooking `NavigationActions` into desktop APIs (window title, clipboard, URI handling).
* Configure `iosApp/` with `iosArm64`, `iosX64`, and `iosSimulatorArm64` targets that expose `MainViewController()` returning `ComposeUIViewController`. Provide actual implementations for lifecycle and navigation bridges.
* Create `webApp/` with a WASM target rendering the shared root inside an HTML shell. Map navigation actions onto `window.history` and ensure link handling is wired through the shared abstraction.

### Stage 6 – Quality gates & automation
* Extend CI to build and run tests for Android (`:app:assembleDebug` + unit tests), desktop (`:desktopApp:package`), iOS (`:shared:foundation:linkReleaseFrameworkIosArm64`), and web (`:webApp:wasmJsBrowserDistribution`).
* Add common linting (ktlint or KtLint Gradle plugin) and Compose stability checks for the new multiplatform modules.
* Capture snapshot tests for key composables in `shared/designsystem` and migrated features to guarantee parity across platforms.

By following these stages, the project retains the existing presenter-first architecture while relocating Hilt-bound Android code into shared multiplatform modules and bringing up desktop, iOS, and web targets on the same Compose surface.
