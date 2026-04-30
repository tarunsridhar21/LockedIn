# CLAUDE.md — TimeTrack Project Guidance

This file gives any Claude session working in this repo the context it needs to be productive immediately. Read it before doing anything.

## What this project is

**TimeTrack** is a single-user, local-only Android timer app. The headline feature is a home-screen widget that lets the user start and stop the timer without opening the app. Sessions are saved unlabeled by default, then the user assigns categories later (Sleeping, Shower, Studying, Break, Working, Exercise, Reading, Eating, Commute, plus user-defined custom categories). The in-app experience is a dynamic, animated dashboard with charts. The widget is intentionally minimalistic.

There is no authentication, no cloud sync, no multi-user support. All data lives in a local Room database.

## Tech stack (locked, do not substitute)

| Concern | Choice | Version (April 2026) |
|---|---|---|
| Build | Android Gradle Plugin | 9.0.1 |
| Build | Gradle wrapper | 8.11+ |
| Language | Kotlin | 2.3.0 |
| Codegen | KSP | 2.3.6 (matches Kotlin) |
| UI | Jetpack Compose BOM | 2026.04.01 |
| UI | Material 3 (Expressive APIs included) | 1.4.0 |
| Widget | Jetpack Glance | 1.1.x latest |
| DB | Room | 2.8.4 |
| Async | Coroutines + Flow | latest stable |
| Storage | DataStore Preferences | latest stable |
| Background | WorkManager | latest stable |
| DI | Hilt | 2.56 |
| Charts | Vico | 3.1.0 (use compose module; compose-glance for widget chart if needed) |
| Min SDK | 26 | (~95% device coverage) |
| Target/Compile SDK | 35 | |

Use a Gradle Version Catalog (`gradle/libs.versions.toml`) for ALL dependencies. No hardcoded versions in `build.gradle.kts` files.

## Architecture (MVVM + Repository + Use Cases)

```
TimeTrackApp (Application, @HiltAndroidApp)
 └─ MainActivity (single-activity, hosts NavHost)
     └─ NavGraph: Home, History, Stats, Settings, LabelDialog (modal)

Layers (strict, dependencies point inward):
 ui/ ──→ domain/ ──→ data/
                 \
                  ──→ service/ (TimerService is source of truth for live timer state)
                  ──→ widget/  (Glance widget reads state via DataStore)
```

The **TimerService** (foreground service, type `specialUse`) is the single source of truth for the running-timer state. The widget never holds timer state in memory — it reads `TimerStateStore` (DataStore) on each render. The main app UI also subscribes to the service's `SharedFlow<TimerState>` for live updates.

When the timer stops, the session is committed to Room as an unlabeled session. The user can label it later from the History screen or via a deep link from the widget.

## File layout

```
app/
├─ src/main/
│  ├─ AndroidManifest.xml
│  ├─ java/com/timetrack/app/
│  │  ├─ TimeTrackApp.kt
│  │  ├─ MainActivity.kt
│  │  ├─ data/
│  │  │  ├─ local/
│  │  │  │  ├─ TimeTrackDatabase.kt
│  │  │  │  ├─ entity/{SessionEntity, CategoryEntity}.kt
│  │  │  │  └─ dao/{SessionDao, CategoryDao}.kt
│  │  │  ├─ datastore/TimerStateStore.kt
│  │  │  └─ repository/{SessionRepository, CategoryRepository}.kt
│  │  ├─ domain/
│  │  │  ├─ model/{Session, Category, TimerState}.kt
│  │  │  └─ usecase/{StartTimer, StopTimer, LabelSession, GetStats, ExportData}.kt
│  │  ├─ service/
│  │  │  └─ TimerService.kt
│  │  ├─ widget/
│  │  │  ├─ TimerGlanceWidget.kt
│  │  │  ├─ TimerWidgetReceiver.kt
│  │  │  ├─ actions/{StartTimerAction, StopTimerAction, LabelLastSessionAction}.kt
│  │  │  └─ WidgetUpdateWorker.kt
│  │  ├─ ui/
│  │  │  ├─ theme/{Color, Type, Shape, Theme}.kt
│  │  │  ├─ navigation/AppNav.kt
│  │  │  ├─ components/ (reusable composables)
│  │  │  └─ screens/
│  │  │     ├─ home/{HomeScreen, HomeViewModel}.kt
│  │  │     ├─ history/{HistoryScreen, HistoryViewModel}.kt
│  │  │     ├─ label/{LabelDialog, LabelViewModel}.kt
│  │  │     ├─ stats/{StatsScreen, StatsViewModel}.kt
│  │  │     └─ settings/{SettingsScreen, SettingsViewModel}.kt
│  │  └─ di/{DatabaseModule, RepositoryModule, DataStoreModule}.kt
│  └─ res/
│     ├─ xml/timer_widget_info.xml
│     ├─ values/{colors, strings, themes}.xml
│     └─ drawable/ (icons, widget previews)
└─ build.gradle.kts

gradle/libs.versions.toml
build.gradle.kts (root)
settings.gradle.kts
```

## Build commands

```bash
./gradlew assembleDebug          # debug APK at app/build/outputs/apk/debug/
./gradlew installDebug           # install on connected device/emulator
./gradlew test                   # unit tests
./gradlew connectedAndroidTest   # instrumented tests (need device/emulator)
./gradlew lint                   # lint
./gradlew :app:dependencies      # see resolved versions
```

Always run `./gradlew assembleDebug` after each major step to catch issues early. If the build fails, fix it before moving on.

## Critical pitfalls (read these — do not skip)

### 1. Glance widgets are NOT regular Compose
- Glance uses its own composables (`androidx.glance.*`) with restricted modifiers and layouts. You **cannot** use `androidx.compose.foundation.*` or `androidx.compose.material3.*` inside a widget.
- The widget runs in a separate process from the app. State stored in memory is lost on every update.
- Always use `PreferencesGlanceStateDefinition` and read state from DataStore inside `provideContent()`.
- After mutating widget state with `updateAppWidgetState(...)`, you MUST call `TimerGlanceWidget().updateAll(context)` to trigger re-render.

### 2. Foreground service type
- Use `android:foregroundServiceType="specialUse"` and declare `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` with a clear explanation (e.g. "Personal time tracking timer for productivity and wellness sessions").
- Required permissions: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `POST_NOTIFICATIONS` (runtime on API 33+), `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK`.
- Do **not** use `dataSync` (6-hour limit on Android 15+ would break sleep tracking).
- Do **not** use `shortService` (3-minute limit).

### 3. Service survival
- Service must `START_STICKY`.
- Persist `startTimeMs` to DataStore the moment the timer starts. On service recreate, read DataStore — if a timer was running, restore. If wall-clock time has elapsed beyond reason (>24h), mark as crashed and save partial.
- Some OEMs (Xiaomi, Oppo, Huawei) aggressively kill services. Settings screen must include a "Battery optimization" button that opens `Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` for the user to whitelist the app.

### 4. Widget update cadence
- Set `updatePeriodMillis="0"` in `timer_widget_info.xml` — the system minimum is 30 minutes, which is useless for a timer. Drive updates yourself.
- While timer runs: `WidgetUpdateWorker` (WorkManager periodic, 30s) calls `TimerGlanceWidget().updateAll(context)`. Cancel the worker on stop.
- The widget displays MM:SS (not seconds:ms) so 30s cadence feels live. The widget computes elapsed from `startTimeMs` on each render.
- The TimerService also calls `updateAll()` on every state transition for instant feedback.

### 5. Material 3 Expressive
- M3 Expressive APIs are inside the regular `androidx.compose.material3:material3:1.4.0` artifact (no separate expressive package).
- Use `expressiveLightColorScheme()` / `expressiveDarkColorScheme()` and `MaterialExpressiveTheme { ... }` in the theme file.
- Use `WavyProgressIndicator`, `LoadingIndicator`, `ButtonGroup`, `FloatingToolbar`, `MaterialShapes`, and shape morphing for the in-app experience.
- The widget cannot use any of these — it uses `androidx.glance.material3.ColorProviders` and Glance primitives.

### 6. Compose performance
- Use `derivedStateOf` for computed state in screens.
- Use `remember(key1, key2) { ... }` with stable keys.
- The live-elapsed-time clock on the Home screen should tick at 100ms (centiseconds visible) using `produceState` or `LaunchedEffect`. Do NOT recompose more than necessary — hoist the clock state and pass plain values to children.

## Coding conventions

- **Single activity**, all screens are composables in a NavHost.
- **No XML layouts** except `timer_widget_info.xml` and the launcher icon resources. Everything else is Compose.
- **No callback APIs** — use Coroutines/Flow.
- **No RxJava.**
- **Functions over classes** where reasonable; classes are for stateful objects (services, repos, ViewModels, entities).
- **Hilt** for all injection. ViewModels use `@HiltViewModel`. Use `@Inject constructor(...)` everywhere else.
- **Comments** only when the WHY is non-obvious. Don't narrate WHAT the code does; let names do that. No multi-line block comments unless documenting a public API.
- **No emojis** in code or generated docs unless the user asks for them.
- **Strings** in `res/values/strings.xml` for anything user-visible.
- **Colors and shapes** in the theme — never hardcode hex values in composables.

## UI design philosophy

**App: dynamic, expressive, animated.** Use Material 3 Expressive shape morphing on the start/stop button (a circle that morphs into a rounded square when running). Use the new motion physics tokens for transitions. Use `WavyProgressIndicator` while saving. Animated number counter on the elapsed time. Spring animations on category chips. Bottom-sheet label dialog with a colorful category grid.

**Widget: minimalistic.** Solid background using the device's dynamic color, single circular button, monospace MM:SS, nothing else. No charts, no decorations, no animations beyond the button's pressed state. Three sizes share the same minimalist language; larger sizes only add a "today total" line and (largest only) the last 3 sessions as text rows with category color dots.

**Dashboard: insight-rich.** Stats screen has time-range selector (Today / Week / Month / All), pie chart of time by category (Vico), bar chart of daily totals over the range (Vico), four stat cards (total time, dominant category, longest session, session count), and a streak indicator. Smooth chart entry animations. Tappable categories drill into per-category time series.

## Testing

- Unit tests: ViewModels and Use Cases with JUnit 4, Turbine for Flow assertions, MockK for mocks. Located in `src/test/`.
- Instrumented tests: Room DAO tests in `src/androidTest/`. One end-to-end Compose UI test for the start → stop → label flow.
- Don't write tests for trivial getters or DTO mapping.

## Data export

Settings screen has "Export to CSV" — uses Storage Access Framework (`ACTION_CREATE_DOCUMENT`) so we don't need WRITE_EXTERNAL_STORAGE. CSV columns: `start_time_iso, end_time_iso, duration_minutes, category, notes`.

## When Claude is editing this project

- Read this file fully. Re-read the "Critical pitfalls" section before touching the widget or service.
- Preserve the layered architecture. UI never touches DAOs directly.
- After any change to dependencies, run `./gradlew assembleDebug` to verify it still builds.
- After widget changes, manually test by adding the widget to a launcher and pressing start/stop.
- If a build error mentions Glance, default-check: are you using Glance composables (not Compose ones)? Did you forget `updateAll()` after `updateAppWidgetState()`?
