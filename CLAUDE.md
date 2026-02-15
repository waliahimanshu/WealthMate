# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

WealthMate is a Kotlin Multiplatform (KMP) personal finance tracker for UK households/couples. It targets Android, iOS, Desktop (JVM), and Web (WASM) using Compose Multiplatform for shared UI.

## Build & Run

```bash
# Desktop (fastest for development)
./gradlew :composeApp:run

# Desktop — build macOS .dmg installer (output: composeApp/build/compose/binaries/main/dmg/)
./gradlew :composeApp:packageDmg

# Web (WASM)
./gradlew wasmJsBrowserDevelopmentRun

# Android
./gradlew :composeApp:assembleDebug
adb install composeApp/build/outputs/apk/debug/composeApp-debug.apk

# Compile check all targets (no tests exist yet)
./gradlew :composeApp:compileKotlinDesktop :composeApp:compileKotlinWasmJs
```

There are no unit tests in this project. Verify changes by compiling all targets.

## Tech Stack

Kotlin 2.3.10, Compose Multiplatform 1.10.1, AGP 8.13.2, kotlinx-serialization 1.10.0, Ktor 3.4.0, Coroutines 1.10.2, MaterialKolor 4.1.1, ComposeCharts 0.2.5. Versions are managed in `gradle/libs.versions.toml`.

## Architecture

### Project Modules
- **composeApp** — all app code (UI + data + storage). Source sets: `commonMain`, `androidMain`, `desktopMain`, `wasmJsMain`, `iosMain`.
- **shared** — placeholder module (unused, contains only `Platform` expect/actual stubs).

All app code lives under `composeApp/src/commonMain/kotlin/com/waliahimanshu/wealthmate/`.

### Responsive Layout (App.kt)
`BoxWithConstraints` drives a three-tier adaptive layout:
- **Desktop (>840dp):** Permanent sidebar (240dp) — all 6 screens
- **Tablet (600-840dp):** NavigationRail — all 6 screens
- **Mobile (<600dp):** Bottom bar (4 screens) + `ModalNavigationDrawer` for Goals/Settings

### Data Flow
Single root aggregate `HouseholdFinances` holds all state. `FinanceRepository` exposes it as `StateFlow<HouseholdFinances?>`. Updates use a transform pattern:
```kotlin
scope.launch { repository.updateData { it.copy(members = ...) } }
```
No ViewModel layer — screens receive data and update callbacks directly from `App.kt`.

### Cloud Sync
`GistStorage` syncs data to a private GitHub Gist via Ktor. Conflict resolution is last-write-wins based on `updatedAt` timestamp. Gist ID is cached: memory -> local storage -> API search.

### Platform Entry Points
Each platform has its own `main` (or `App` composable for Android) that:
1. Creates a `LocalStorage` implementation
2. Loads the GitHub token from storage
3. Creates `GistStorage` + `FinanceRepository`
4. Calls `WealthMateApp()` — the shared entry point

Platform-specific `LocalStorage` implementations:
- Desktop: `~/.wealthmate/data.json` + `java.util.prefs.Preferences`
- Web: `window.localStorage`
- Android: `SharedPreferences`
- iOS: `NSUserDefaults`

### expect/actual Pattern
`currentTimeMillis()` is declared `expect` in `FinanceData.kt` with `actual` implementations per platform. `LocalStorage` interface is defined in `commonMain` with platform implementations.

## Key Conventions

- All screens receive `data: HouseholdFinances` as a parameter
- Currency: `formatCurrency()` returns `£X.XX` — UK-centric, no locale switching
- Color semantics: Green = income, Red = expenses, Blue = savings/primary, Orange = investments
- Theme: MaterialKolor `DynamicMaterialTheme` with seed color `#1A5FB4` (Deep Sapphire Vibrant), dark mode default
- Show/Demo mode: `LocalShowMode` composition local masks real values with `maskAmount()` for screenshots/demos
- `StatCardRow` uses `FlowRow`: 2 cards/row on mobile, all in one row on desktop
- Charts (`ir.ehsannarmani.compose_charts`) appear **only on DashboardScreen**
- Screen enum (`Screen.kt`) has `showInMobileBottomBar` and `mobileDisplayName` for adaptive nav
- All data models use `@Serializable` and `generateId()` for IDs (random Long in base-36)

## Data Models (FinanceData.kt)

Root: `HouseholdFinances` → `HouseholdMember`, `Outgoing`, `SavingsAccount`, `Investment`, `SharedGoal`, `MortgageInfo`

Key computed properties on `HouseholdFinances`: `totalHouseholdIncome`, `totalOutgoings`, `totalSavings`, `netMonthlyHousehold`, `totalPortfolioValue`, `easyAccessSavings`, `lockedSavings`

Key enums: `OutgoingCategory` (26+), `UKAccountType` (17, includes ISA types, pensions), `AssetClass`, `GoalCategory`

`FinanceData` is a legacy type kept only for backward-compatible migration via `toHouseholdFinances()`.
