# Android Compose Architecture Starter

This project demonstrates a modular, feature-first architecture for Jetpack Compose apps. It splits functionality into coarse **modules**, separates concerns by **layers**, and wires everything together through **Hilt**.

## Custom ViewModelScope

This project uses a custom ViewModelScope implementation instead of the standard `viewModelScope`. This provides:

- **Single VM Factory Map**: Uses Dagger multibindings to eliminate duplicate ViewModel lists
- **Nested Scopes**: `ScreenScope(nested = true)` creates independent child scopes
- **Scoped Dependencies**: ViewModels can inject screen-scoped dependencies like `ScreenBus`
- **Constructor DI**: ViewModels use `@AssistedInject` for type-safe dependency injection

### Usage

```kotlin
// Wrap screens with ScreenScope
@Composable
fun SomeScreen() {
  ScreenScope {
    val presenter: CatalogPresenter = rememberPresenter<CatalogPresenter, Unit>()
    // ViewModels share ScreenBus instance within this scope
  }
}

// Create nested independent scopes
ScreenScope {
  ParentContent()
  
  ScreenScope(nested = true) {
    ChildContent() // Gets fresh ScreenBus instance
  }
}
```

## Module structure
- **app** – Application module hosting the navigation graph and providing Hilt bindings for presenters and the app scope.
- **core**
  - `core:designsystem` – Compose UI theme and design components.
  - `core:common` – Shared utilities including presenter infrastructure and app-wide classes.
- **feature** – Each feature is composed of three modules:
  - `feature:*:api` – Public contracts (routes, state, presenter interfaces).
  - `feature:*:ui` – Pure Compose screens depending only on the API and core modules.
  - `feature:*:impl` – Implementation with ViewModels, repositories and Hilt bindings.

## Layer structure
Each feature is separated into layers that match the modules above:

| Layer | Responsibility | Module example |
|-------|----------------|----------------|
| **API** | Defines navigation destinations and presenter contracts | `feature/catalog/api` |
| **UI** | UI built with Compose, retrieves a presenter via `rememberPresenter` | `feature/catalog/ui` |
| **Impl** | ViewModels and data sources backing the feature | `feature/catalog/impl` |

The `core:common` module provides `PresenterResolver` and helpers such as `rememberPresenter` used by UI modules to obtain their presenters.

## Hilt structure
- `@HiltAndroidApp` `MyApp` is the entry point for dependency injection.
- A custom `AppComponent` and `AppScopeManager` create an application-scoped component that holds an `App` object with navigation actions.
- Custom `ScreenComponent` and `SubscreenComponent` provide screen-level scoping with nested scope support.
- `HiltPresenterResolver` is injected into the `MainActivity` and uses multibindings to map presenter interfaces to their implementations.
- Each feature implementation module contributes ViewModels to the factory map via `@AssistedFactory` and `@VmKey` annotations.

This structure allows UI modules to remain free of Hilt while still obtaining their presenters through the shared `PresenterResolver`, keeping feature APIs clean and implementations encapsulated.

## Network layer
Feature implementation modules own their network and persistence code. Retrofit and OkHttp service interfaces (e.g., `WikipediaService`, `SummarizerService`, `TranslatorService`, and `DictionaryService`) live beside Room entities and DAOs. Hilt modules provide these services and compose them into repositories, such as `ArticleRepository`. These repositories expose `Flow`-based APIs to the rest of the app. This keeps networking concerns isolated within the `impl` layer.

## Current Features

The starter ships with a few sample features wired through the architecture:

- **Catalog** – fetches random Wikipedia articles, summarizes them and lists translated vocabulary.
- **Detail** – shows the full article content along with translation details and pronunciation when available.
- **Settings** – allows choosing native and learning languages that drive translation.

## User Guide

1. **Choose your languages.** Open the Settings panel from the catalog screen and pick the language you already know for the **Native (from)** field and the language you want to practice for the **Learning (to)** field. Use the dropdown in each field to search and select from the supported languages.
2. **Fetch a random article.** Return to the catalog and press **Refresh** to pull a fresh random article that matches your language choices. Each refresh updates the list with another suggestion to explore.
3. **Open the article.** Tap any article card in the catalog to view the full text along with its summary and additional metadata.
4. **Translate vocabulary on the fly.** While reading, touch any word to highlight it and trigger an instant translation into your selected learning language. Keep your finger on the text and slide to nearby words to see their translations in place.

## Adding a feature
1. Create three modules under `feature/<name>/` (`api`, `ui`, `impl`) and include them in `settings.gradle.kts`.
2. In `feature/<name>/api`, declare the route and presenter contract:
```kotlin
@Serializable data object Foo

data class FooState(val text: String = "")
interface FooPresenter : ParamInit<Unit> {
  val state: StateFlow<FooState>
  fun onAction()
}
```
3. In `feature/<name>/ui`, build the Compose screen and obtain the presenter:
```kotlin
@Composable
fun FooScreen(p: FooPresenter? = null) {
  val presenter = p ?: rememberPresenter<FooPresenter, Unit>()
  val state by presenter.state.collectAsStateWithLifecycle()
  Text(state.text, Modifier.clickable { presenter.onAction() })
}
```
4. In `feature/<name>/impl`, provide the presenter implementation and Hilt bindings:
```kotlin
class FooViewModel @AssistedInject constructor(
  private val screenBus: ScreenBus,
  @Assisted private val handle: SavedStateHandle
) : ViewModel(), FooPresenter {
  @AssistedFactory
  interface Factory : AssistedVmFactory<FooViewModel>
}

@Module
@InstallIn(ScreenComponent::class)
abstract class FooVmBindingModule {
  @Binds @IntoMap @VmKey(FooViewModel::class)
  abstract fun fooFactory(f: FooViewModel.Factory): AssistedVmFactory<out ViewModel>
}

@Module
@InstallIn(SingletonComponent::class)
object FooPresenterBindings {
  @Provides @IntoMap @ClassKey(FooPresenter::class)
  fun provideFooPresenterProvider(): PresenterProvider<*> {
    return object : PresenterProvider<FooPresenter> {
      @Composable
      override fun provide(key: String?): FooPresenter {
        return magicViewModel<FooViewModel>()
      }
    }
  }
}
```
5. Wire the feature into navigation by updating `NavigationActions` and wrapping the screen with `ScreenScope { }` if needed.

## Release
To publish a release APK through GitHub Actions, create and push an annotated tag:

```bash
git tag -a v0.1.0 -m "Release 0.1.0"
git push origin v0.1.0
```

The CI workflow will build and upload the release APK for that tag.
