# Sunflower Land Farm Alerts (SFL Alerts) 🌻

A reliable, native Android companion application for **Sunflower Land** farm timers and notifications. 

Unlike web push notifications or browser service workers that often get throttled or discarded by mobile OS power-savers, **SFL Alerts** uses Android's exact hardware clock (`AlarmManager.RTC_WAKEUP`) to trigger notifications precisely on time — even in deep sleep (Doze mode), offline, or with the app closed.

---

## ✨ Features

- ⏰ **100% Guaranteed Offline Alarms:** Uses hardware RTC timers. Once synced, alarms will ring on time even in airplane mode or with no network connection.
- 🔄 **Reboot Persistence (`RECEIVE_BOOT_COMPLETED`):** Automatically reschedules all pending farm timers when the device restarts.
- 🌾 **Comprehensive Game Support:**
  - **Crops:** From Sunflowers to Radishes and exotic crops.
  - **Trees:** Wood regeneration timers with replenishment tracking.
  - **Mining:** Stone, Iron, Gold, Crimson Stones, Sunstone, and Oil reserves.
  - **Composters:** Warding Compost, Sprout Mix, and Rapid Root.
  - **Cooking:** Kitchen, Bakery, Deli, and Smoothie Shack queues.
  - **Animals & Greenhouse:** Chickens, Greenhouse pots, and Beehives honey collection.
- 🔕 **Selective Notification Categories:** Toggle alerts per category (e.g. only notify for Trees or Mining).
- ⚡ **Anti-Spam Clustering:** Multiple items finishing in close proximity are grouped into a clean, single notification rather than continuous alert spam.
- 🌐 **Intelligent Background Sync (`WorkManager`):** Automatically polls farm state when connected to Wi-Fi/cellular and updates pending alarms without user intervention.
- 🎨 **Modern Material 3 UI:** Dark mode optimized, live countdown timers, pull-to-refresh, and category filter chips.

---

## 📱 Screenshots & Architecture

```
┌───────────────────────────────────────────────┐
│              SFL Notif App                    │
├───────────────────────┬───────────────────────┤
│   UI Layer            │ MainActivity          │
│                       │ TimerAdapter          │
│                       │ SettingsBottomSheet   │
├───────────────────────┼───────────────────────┤
│   Domain / Logic      │ SflTimerEngine        │
│                       │ FarmTimerItem         │
├───────────────────────┼───────────────────────┤
│   System & Schedulers │ AlarmScheduler        │
│                       │ AlarmReceiver         │
│                       │ BootReceiver          │
│                       │ FarmSyncWorker        │
├───────────────────────┼───────────────────────┤
│   Data & Network      │ SflApiClient          │
│                       │ PreferencesManager    │
└───────────────────────┴───────────────────────┘
```

---

## 🛠️ Tech Stack & Requirements

- **Language:** Kotlin 1.9+
- **Min SDK:** Android 8.0 (API 26)
- **Target SDK:** Android 14 (API 34)
- **Architecture:** Android Architecture Components, ViewBinding, Coroutines
- **Networking:** OkHttp 4, Gson
- **Background Tasks:** Android Jetpack WorkManager & AlarmManager

---

## 🚀 Building From Source

### Prerequisites
1. Android Studio Hedgehog (2023.1.1) or newer
2. JDK 17
3. Android SDK 34

### Build Steps

```bash
# Clone the repository
git clone https://github.com/Unknown37841/sfl-farm-alerts.git
cd sfl-farm-alerts

# Build Debug APK
./gradlew assembleDebug

# Output APK path:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 📜 Permissions

- `POST_NOTIFICATIONS`: Android 13+ permission to post notifications.
- `SCHEDULE_EXACT_ALARM` & `USE_EXACT_ALARM`: Required for precise millisecond timer triggers.
- `RECEIVE_BOOT_COMPLETED`: Restores alarms after reboot.
- `WAKE_LOCK` & `VIBRATE`: Ensures audio/vibration triggers when screen is off.
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`: Recommended to prevent aggressive OEM battery managers from delaying alerts.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
