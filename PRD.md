# PitchPulse - Product Requirements Document (PRD)

## 1. Product Overview & Vision
PitchPulse is a production-grade Android application designed for elite football enthusiasts who demand accurate, real-time, and localized match data. The app provides a focused and high-performance experience by strictly prioritizing the world's top-tier European leagues and major international tournaments, stripping away the "noise" of minor global competitions.

**Mission:** To provide the fastest, cleanest, and most reliable football match-tracking experience, tailored specifically for the Indian market with high-precision IST synchronization.

---

## 2. Target Audience
*   **Elite Football Fans:** Users who follow the Top 5 European leagues and Champions League.
*   **National Team Supporters:** Fans of UEFA, CONCACAF, and CONMEBOL international competitions.
*   **Indian Viewers:** Users requiring high-precision IST match timings and chronological date alignment for late-night fixtures.

---

## 3. Key Features

### 3.1. Personalized Match Center (Home)
*   **My Teams:** Quick horizontal access to the badges of followed clubs.
*   **Next for Your Teams:** A dedicated, prioritized carousel showing the very next fixture for each followed team.
*   **Upcoming and Recent Matches:** A curated list of matches from favorite leagues, sorted chronologically with IST adjusted dates.

### 3.2. Global Match Feed (Matches Tab)
*   **Dynamic Date Ribbon:** A horizontally scrollable calendar spanning -7 to +7 days.
*   **Auto-Focus Logic:** The ribbon automatically centers on "Today" (IST) upon launch.
*   **League Grouping:** Matches are grouped by competition and sorted by status (Live first, then Kickoff time).

### 3.3. Team Search & Personalization
*   **Real-time Search:** Search for any club or national team globally.
*   **One-tap Favorites:** Star teams to influence the Home Screen content.
*   **Strict Filtering:** Search results and team fixture histories respect the global league whitelists.

### 3.4. Match Intelligence
*   **Live Tracking:** Alpha-pulsing neon green badges for matches in progress.
*   **Live Clock:** Real-time match minute updates (e.g., 42', 75').
*   **Match Details:** (Future) Lineups, Statistics, and Standings.

---

## 4. Requirement Specifications

### 4.1. The "Remove All Others" Rule (League Whitelisting)
The app enforces a strict whitelist to maintain high data quality. Any fixture not belonging to these IDs is automatically discarded:
*   **Clubs:** 
    *   England (Premier League, FA Cup, EFL Cup, Community Shield)
    *   Spain (LaLiga, Copa del Rey, Supercopa)
    *   Italy (Serie A, Coppa Italia, Supercoppa)
    *   France (Ligue 1, Coupe de France, Trophée des Champions)
    *   Germany (Bundesliga, DFB Pokal, DFL-Supercup)
    *   Continental (UEFA Champions League, Europa League, Conference League)
*   **National Teams:** UEFA, CONCACAF, and CONMEBOL (Euros, Gold Cup, Copa America, Nations League, and World Cup Qualifiers).

### 4.2. IST Synchronization
*   **Timezone:** All times are converted from UTC to `Asia/Kolkata` (+05:30).
*   **Format:** 12-hour format with AM/PM (e.g., `10:30 PM`).
*   **Chronological Date Alignment:** Matches starting after 12:00 AM IST are correctly mapped to the local Indian calendar day to prevent "Yesterday/Tomorrow" confusion.

### 4.3. High-Resilience Sync Architecture (API Guard System)
To stay efficiently within API quotas (100 calls/day), the app implements a 5-layer guard:
1.  **Past Date Freeze:** Scores for dates before today are never re-fetched once stored.
2.  **Date Mutex:** Prevents multiple simultaneous API calls for the same date.
3.  **Live Throttle:** Live matches only re-fetch every 2 minutes.
4.  **Failure Cooldown:** Prevents API spam if a network failure occurs.
5.  **Stale Threshold:** Finished matches on the current date are updated less frequently than live ones.

---

## 5. Technical Stack
*   **Language:** Kotlin 1.9.0+
*   **UI Framework:** Jetpack Compose (Material 3)
*   **Architecture:** MVVM (Model-View-ViewModel) + Clean Data Layer
*   **Networking:** Retrofit 2.9 + OkHttp3
*   **Local Storage:** Room Persistence Library (Single Source of Truth)
*   **Dependency Injection:** Manual DI (Scalable to Hilt)
*   **Image Loading:** Coil (with crossfade and caching)

---

## 6. Design System Guidelines
*   **Theme:** Deep Dark Mode (Solid surface strategy).
*   **Restrictions:** **NO** Transparency, **NO** Blur, **NO** Glassmorphism.
*   **Typography:** Google Fonts (Inter) with high-contrast weights.
*   **Palette:** Pitch Dark background with Neon Green (PitchAccent) highlights.

---

## 7. Success Metrics
*   **Data Reliability:** 100% successful parsing of API responses (even during partial failures).
*   **Performance:** UI interactions under 16ms; Data sync under 2 seconds on 4G/5G.
*   **Efficiency:** Average of <50 API calls per active user day.
