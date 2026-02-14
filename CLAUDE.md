# WealthMate - Project Context

## Overview
Kotlin Multiplatform (KMP) personal finance tracker for households/couples. Targets **Android**, **iOS**, **Desktop (JVM)**, and **Web (WASM)**.

## Build & Run

```bash
# Android
./gradlew :composeApp:assembleDebug
adb install composeApp/build/outputs/apk/debug/composeApp-debug.apk

# Desktop
./gradlew :composeApp:run

# Web (WASM)
./gradlew wasmJsBrowserDevelopmentRun

# All targets compile check
./gradlew :composeApp:compileKotlinDesktop :composeApp:compileKotlinWasmJs
```

## Tech Stack

| Tech | Version |
|------|---------|
| Kotlin | 2.3.10 |
| Compose Multiplatform | 1.10.1 |
| AGP | 8.13.2 |
| kotlinx-serialization | 1.10.0 |
| Ktor | 3.4.0 |
| Coroutines | 1.10.2 |
| MaterialKolor | 4.1.1 |
| ComposeCharts | 0.2.5 |

## Directory Structure

```
composeApp/src/commonMain/kotlin/com/waliahimanshu/wealthmate/
├── App.kt                    # Main app composable, responsive layout, navigation
├── FinanceData.kt            # All data models (HouseholdFinances, Investment, etc.)
├── Theme.kt                  # Material3 theming with MaterialKolor
├── components/
│   ├── StatCard.kt           # StatCard + responsive StatCardRow (FlowRow-based)
│   ├── CommonComponents.kt   # formatCurrency(), SyncStatusBar, DarkModeSwitch
│   └── Charts.kt             # PieChart/DonutChart helpers (Canvas-based)
├── navigation/
│   └── Screen.kt             # Screen enum with mobile/desktop nav classification
├── dashboard/
│   └── DashboardScreen.kt    # Dashboard with charts (only screen with charts)
├── income/
│   └── IncomeScreen.kt       # Read-only income display
├── expenses/
│   └── ExpensesScreen.kt     # Expense management with tabs, add/edit dialogs
├── investments/
│   └── InvestmentsScreen.kt  # Investment tracking with tabs, add/edit dialogs
├── savings/
│   └── SavingsScreen.kt      # Savings accounts management
├── goals/
│   └── GoalsScreen.kt        # Financial goals with progress tracking
├── settings/
│   └── SettingsScreen.kt     # Member management, sync config
└── storage/
    ├── FinanceRepository.kt  # Repository pattern with local + cloud sync
    ├── GistStorage.kt        # GitHub Gist cloud sync via Ktor
    └── LocalStorage.kt       # Platform-specific storage interface
```

## Architecture

### Responsive Layout (App.kt)
Three-tier breakpoint system using `BoxWithConstraints`:
- **Desktop (>840dp):** Sidebar navigation (240dp) — all 6 screens
- **Tablet (600-840dp):** NavigationRail — all 6 screens
- **Mobile (<600dp):** Bottom bar (4 screens: Dashboard, Income, Expenses, Savings) + ModalNavigationDrawer (Goals, Settings). Hamburger menu in top bar opens the drawer.

### StatCardRow
Uses `FlowRow` with `BoxWithConstraints`:
- Mobile (<600dp): 2 cards per row (2x2 grid)
- Desktop/tablet: all cards in single row

### Charts
Charts (PieChart/donut) are **only on DashboardScreen** — removed from Expenses and Investments. Uses `ir.ehsannarmani.compose_charts` library.

### Data Layer
- **HouseholdFinances** — root aggregate with members, shared accounts, investments, goals, mortgage
- **FinanceRepository** — local-first with optional GitHub Gist cloud sync
- **StateFlow-based** reactivity for data, syncStatus, isLoading
- Platform storage: File+Preferences (Desktop), localStorage (Web), SharedPreferences (Android)

### Screen Enum (Screen.kt)
Each screen has:
- `displayName` — full name for desktop/tablet
- `mobileDisplayName` — shorter name for mobile bottom bar (e.g., "Savings" instead of "Savings & Investments")
- `showInMobileBottomBar` — true for 4 main screens, false for Goals/Settings (drawer)

### Key Conventions
- All screens receive `data: HouseholdFinances` as a parameter
- Updates use callbacks like `onUpdateX` that call `repository.updateData { it.copy(...) }`
- Currency formatting: `formatCurrency()` in CommonComponents.kt
- Color scheme: Green = income, Red = expenses, Blue = savings/primary, Orange = investments
- Material3 with dark mode default, dynamic colors via MaterialKolor (Vibrant style)

## Data Models (FinanceData.kt)

Key types: `HouseholdFinances`, `HouseholdMember`, `Outgoing`, `SavingsAccount`, `Investment`, `SharedGoal`, `MortgageInfo`

Key enums: `OutgoingCategory` (26+), `UKAccountType`, `AssetClass`, `PaymentFrequency`, `InvestmentFrequency`, `GoalCategory`

Computed properties on `HouseholdFinances`: `totalHouseholdIncome`, `totalOutgoings`, `totalSavings`, `netMonthlyHousehold`, `totalPortfolioValue`
