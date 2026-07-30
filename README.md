# DAC Pressure Manager (Native Android)

This is an offline native Android rewrite of
[DAC-Pressure-Manager.html](https://apps.9527857.xyz/DAC-Pressure-Manager.html).
The project contains no HTML, JavaScript, Capacitor, WebView, or network
functionality. The original HTML lives outside this project and is used only as
a functional reference.

## Development Environment

- Android Studio (open this directory directly)
- JDK 17
- Gradle 8.14.3 / Android Gradle Plugin 8.13.2
- Kotlin 2.2.20
- `minSdk 24`, `compileSdk/targetSdk 36`

The application ID remains `com.iyes.dacpressuremanager`. The UI is built with
Jetpack Compose and Material 3, while structured offline data is stored with
Room 2.8.4.

The UI automatically follows the Android system light or dark theme. The app
bundles the Roboto variable font to prevent manufacturer themes or user font
replacements from disrupting the instrument layout. See `ROBOTO-OFL.txt` for
the font license.

## Project Structure

```text
app/src/main/java/com/iyes/dacpressuremanager/
├── data/       Room, DAOs, repositories, and persistence rules
├── domain/     Modes, profile models, pressure formulas, and exact rounding
├── export/     CSV generation, Storage Access Framework, and system sharing
└── ui/         ViewModels, unidirectional state, Compose dashboard, and history dialogs
```

Dependencies are managed centrally in `gradle/libs.versions.toml`.
`AppContainer` provides manual injection for the database and repositories.
The project intentionally avoids Hilt, Retrofit, WorkManager, DataStore, and
image-loading frameworks.

## Key Data Conventions

- Reference, measured, and historical pressure values are stored as `Int`
  values in hundredths.
- The Diamond range is `1000.00–2999.99 cm⁻¹`, with a default of
  `1333.00 cm⁻¹`.
- The Ruby range is `600.00–799.99 nm`, with a default of `694.24 nm`.
- Diamond uses the AK2006 formula and only allows records whose raw pressure is
  within `0–310 GPa`.
- Ruby uses `A=1904` and `B=7.665`, and allows negative pressure values.
- Rounding matches JavaScript `Math.round`: exact half values round toward
  positive infinity.
- The Room database is version 1, with schemas stored in `app/schemas/`. Future
  table changes must add a migration instead of overwriting an existing schema.

## Data and Export

- The mode, active profile, profile names and order, and value changes are all
  saved automatically.
- The UI responds immediately with optimistic state, while write commands are
  committed to Room serially in operation order.
- The main dashboard fills the available screen without an outer card, shadow,
  or vertical scrolling. Records always remain to the right of the counters.
- Dark mode uses layered black and gray surfaces with high-contrast light text,
  while retaining Diamond blue, Ruby red, and the green digital displays.
- Long-press profile reordering uses Compose system gesture detection and
  list-movement animation. Dragging previews only the placeholder order,
  supports crossing multiple profiles in one gesture, and commits the final
  order to Room only after release.
- Full history is shown in a dialog over the main screen, with a scrollable
  record list inside the dialog.
- Each mode retains at least one profile, and each profile stores at most 50
  history records.
- CSV export includes only the current profile, uses a UTF-8 BOM, CRLF, nine
  columns, and oldest-to-newest order, and protects against spreadsheet formula
  injection.
- “Save as” uses the system document picker, while “Share” opens the system
  share sheet through `FileProvider`.
- The manifest declares no network or storage permissions, and system backup
  and device migration are disabled.

## Build and Checks

Run the following from the project root:

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assembleRelease
```

After connecting a device or starting an emulator, run:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

Debug APKs are written to `app/build/outputs/apk/debug/`. Release builds enable
R8 and resource shrinking. Before an official release, configure your own
signing locally or in CI, and never commit signing keys to the repository.

## Guidelines for Future Changes

- The UI must only read `MainUiState` / `HistoryUiState` and dispatch actions.
  Business rules must not be placed in Composables.
- All data mutations must go through `DacRepository`; the UI must not access
  DAOs directly.
- Add new user-facing text to `res/values/strings.xml` to support future
  localization.
- New interactions must retain at least a 48dp touch target and include
  TalkBack semantics and corresponding tests.
- Before adding a dependency, confirm that native APIs cannot reasonably
  implement the requirement so the project remains lightweight.
