# WealthMate

A personal finance tracker for UK residents built with Kotlin Multiplatform and Compose Multiplatform.

## Features

- **Salary Tracking**: Record your monthly net salary
- **Outgoings Management**: Track all your monthly expenses by category (bills, groceries, transport, entertainment, subscriptions)
- **UK Savings Accounts**: Manage multiple savings accounts including ISAs, LISAs, Premium Bonds, and more
- **Mortgage Tracking**: Keep track of your mortgage details including remaining balance, monthly payments, and interest rate
- **Dashboard**: View a comprehensive summary of your financial situation

## Platforms

- **Web** (WASM): Hosted on GitHub Pages - data stored in browser localStorage
- **Android**: Native Android app - data stored in SharedPreferences
- **iOS**: Coming soon

## Live Demo

Visit: https://waliahimanshu.github.io/wealthmate/

## Tech Stack

- Kotlin Multiplatform
- Compose Multiplatform
- Kotlin/WASM for web
- Material 3 Design
- kotlinx.serialization for data persistence

## Building

### Web
```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

### Android
```bash
./gradlew :composeApp:assembleDebug
```

## License

MIT
