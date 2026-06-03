# Live Scores ⚽

![Android](https://img.shields.io/badge/Platform-Android-brightgreen.svg)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)
![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-orange.svg)
![License](https://img.shields.io/badge/License-GPL--3.0-lightgrey.svg)

**Live Scores** is a production-grade Android application designed for elite football enthusiasts. It provides a focused, high-performance experience by strictly prioritizing top-tier European leagues and major international tournaments, stripping away the noise of minor global competitions.

---

## ⬇️Download the App here 👇
https://github.com/eddardstark687-rgb/live-scores/releases/download/v1.1/app-release.apk

## 🌟 Features

- **Rich Home Screen**: League match counts for today, football quiz, daily quotes, and facts.
- **Personalized Match Center**: Quick access to your favorite teams and their next fixtures.
- **Global Match Feed**: Real-time scores from the Top 5 European leagues (PL, LaLiga, Serie A, etc.) and UEFA competitions.
- **Smart Quota Management**: 
  - **Multi-Key Rotation**: Automatically switches between backup API keys to ensure 24/7 uptime.
  - **Strict Caching**: Advanced Room-based caching strategy that minimizes API calls for finished matches and team schedules.
- **Live Intelligence**: Real-time match minute updates, live badges, and goal scorer tracking.
- **IST Synchronization**: High-precision India Standard Time (IST) adjustments for late-night fixtures.
- **Deep Dark Mode**: A premium, high-contrast UI designed for night-time viewing.

---

## 🛠️ Tech Stack

- **UI**: Jetpack Compose (Material 3)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Local Database**: Room (Single Source of Truth)
- **Networking**: Retrofit 2 + OkHttp
- **Image Loading**: Coil
- **Threading**: Kotlin Coroutines & Flow
- **Serialization**: Kotlinx Serialization

---

## 🚀 Getting Started

### 1. Prerequisites
- Android Studio Panda 4.
- An API Key from [API-Sports (Football)](https://dashboard.api-football.com/).
- A Gemini API key from [Google AI Studio](https://aistudio.google.com/apikey) (optional; home screen uses bundled fallbacks without it).

### 2. Configuration
Copy `local.properties.example` to `local.properties` in the project root and add your keys:

```properties
sdk.dir=/path/to/Android/Sdk
API_KEY_PRIMARY=your_api_sports_primary_key
API_KEY_BACKUP_1=your_api_sports_backup_key_1
API_KEY_BACKUP_2=your_api_sports_backup_key_2
GEMINI_API_KEY=your_gemini_api_key
```

- **API-Sports keys** power live scores and match sync.
- **Gemini key** powers the home screen quiz, quotes, and facts (falls back to bundled content if missing).

`local.properties` is gitignored — never commit API keys.

### 3. Build & Run
Clone the repository and build the project in Android Studio.

---

## 🛡️ Privacy Policy
This app is designed with privacy in mind. It does not collect any personal information. All your favorite teams and settings are stored locally on your device. For more details, see [PRIVACY_POLICY.md](./PRIVACY_POLICY.md).

---

## 📜 License
This project is licensed under the GPL-3.0 License - see the [LICENSE](LICENSE) file for details.

---

*Made with ❤️ for Football Fans.*
