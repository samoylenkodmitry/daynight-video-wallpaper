# DayNight Video Wallpaper

DayNight Video Wallpaper turns your Android home screen into a living canvas that transitions throughout the day. Assign videos for morning, day, evening and night, pick whether playback follows solar events or custom times, and apply the live wallpaper with a single tap.

[live_video_wallpaper.webm](https://github.com/user-attachments/assets/b220c625-c18c-4d4f-89af-b5de850889b9)

<img width="1280" height="2856" alt="scr1" src="https://github.com/user-attachments/assets/d8f59d93-91f3-48c3-ba6c-18362ef34e79" />
<img width="1280" height="2856" alt="scr4" src="https://github.com/user-attachments/assets/4330eb99-7d20-4f6e-b1f2-efbfd3bf90b4" />

## Highlights
- **Time-aware playback.** Choose between a solar-aware schedule (sunrise/sunset when available) or fixed start times for each part of the day.
- **Personal video loops.** Select any video on your device for each slot. DayNight remembers URI permissions and loops clips seamlessly.
- **Mute & loop controls.** Toggle global mute and looping from the home screen to match your ambience.
- **Live wallpaper service.** A Media3-powered `WallpaperService` switches videos as the active slot changes.
- **Onboarding guide.** New users get a quick tour describing the wallpaper flow.

## Module structure
- **app** – Hosts Hilt bindings for presenters, navigation and the live wallpaper service (`DayNightWallpaperService`).
- **core:common** – Shared presenter helpers, state management infrastructure and the `WallpaperPreferencesRepository` backed by DataStore.
- **core:designsystem** – Compose theme and shared UI primitives.
- **feature:onboarding** – Onboarding flow implemented with the architecture's presenter pattern.
- **feature:catalog** – The “home” experience showing slot cards, playback controls and wallpaper entry points.
- **feature:settings** – Controls for scheduling mode and slot start times.

Each feature follows the starter architecture: `api` exposes contracts, `ui` contains Compose screens and `impl` wires ViewModels via Hilt. Presenters are resolved through a custom `ScreenScope` that keeps UI modules DI-free.

## Wallpaper service overview
`DayNightWallpaperService` watches `WallpaperPreferencesRepository` for changes, builds an `ExoPlayer` instance and updates the active media item whenever the time slot changes. Slots are computed every minute using either the user-configured schedule or sensible defaults when solar data isn't available.

## Running the project
The project targets **Android 13+ (API 33)** with **Build Tools 35**. Ensure the following components are installed in your Android SDK:

- Android SDK Platform 35
- Android SDK Build-Tools 35.0.0 (clean install)
- Android Emulator or device running Android 13 or later

Then build and install:

```bash
./gradlew :app:assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

Apply the wallpaper by launching the app and tapping **Set live wallpaper**.

## Customising the schedule
Open **Settings** to switch between **Solar aware** and **Custom times**:
- **Solar aware** uses sunrise/sunset data when available and falls back to the default schedule (Morning 6:00, Day 12:00, Evening 18:00, Night 21:00).
- **Custom times** lets you change each slot’s start time. Times are stored in DataStore and shared between the wallpaper service and the UI.

## Extending DayNight
- Add weather- or location-based sources by enriching `WallpaperPreferencesRepository` and expanding `WallpaperScheduleMode`.
- Introduce additional slots (e.g., dawn, dusk) by extending `DaySlot` and updating home/settings presenters.
- Hook into external media providers by replacing the URI picker with your own browser.

DayNight builds on the Compose architecture starter, so presenters remain testable and UI modules stay dependency-free while still working with Hilt.
