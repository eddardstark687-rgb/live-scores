# Live Scores ⚽

![Android](https://img.shields.io/badge/Platform-Android-brightgreen.svg)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)
![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-orange.svg)
![License](https://img.shields.io/badge/License-MIT-lightgrey.svg)

**Live Scores** is a production-grade Android application designed for elite football enthusiasts. It provides a focused, high-performance experience by strictly prioritizing top-tier European leagues and major international tournaments, stripping away the noise of minor global competitions.

---

## ⬇️Download the App here 👇
https://github.com/eddardstark687-rgb/live-scores/releases/download/v0.1/LiveScores.apk

## 🌟 Features

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

### 2. Build & Run
Clone the repository and build the project in Android Studio.

---

## 🛡️ Privacy Policy
This app is designed with privacy in mind. It does not collect any personal information. All your favorite teams and settings are stored locally on your device. For more details, see [PRIVACY_POLICY.md](./PRIVACY_POLICY.md).

---

## 📜 License
This project is licensed under the MIT License - see the LICENSE file for details.

---

*Made with ❤️ for Football Fans.*
