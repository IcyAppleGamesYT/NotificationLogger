# 📬 Notification Logger

A Discord-themed Android app that logs all your notifications, labels them with keywords, saves them to a folder, and shows word frequency statistics.

![Android](https://img.shields.io/badge/Android-API%2026+-green?logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-purple?logo=kotlin)
![License](https://img.shields.io/badge/License-MIT-blue)

---

## ⬇️ Download APK

**The easiest way:** Go to the [**Releases**](../../releases) tab on GitHub — the APK is automatically built and uploaded on every push to `main`.

Or go to **Actions → latest build → Artifacts** to download the debug APK directly.

---

## ✨ Features

| Feature | Description |
|--------|-------------|
| 🔔 **Auto Logging** | Captures all notifications from every app in real-time |
| 🏷️ **Keywords** | Tag notifications with custom keywords; flagged items get highlighted |
| 📁 **File Export** | Saves daily `.txt` log files to a folder you choose |
| 📊 **Word Stats** | Bar chart of the top 15 most-used words across all notifications |
| ⚡ **Real-Time** | Logs page updates instantly when new notifications arrive |
| 👀 **Smart Section** | "You might want to look at this" — keyword-matched notifications in their own section |
| 🎨 **Discord Theme** | Full Discord dark mode color palette (blurple, dark grays, green/red accents) |

---

## 📲 Installation

### Option 1: Download from GitHub Releases (Recommended)
1. Go to the **Releases** tab on this repo
2. Download `app-debug.apk`
3. Transfer to your Android device (or download directly on the device)
4. Enable **Install from Unknown Sources** in Settings → Apps → Special app access
5. Tap the APK and install

### Option 2: Build from Source
```bash
git clone https://github.com/YOUR_USERNAME/NotificationLogger
cd NotificationLogger
./gradlew assembleDebug
# APK will be at: app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔧 First-Time Setup

1. **Open the app** → tap **Home**
2. **Grant Notification Access** → tap the button → enable "Notification Logger" in system settings
3. **Set folder name** → type a subfolder name (e.g., `MyLogs`) and tap Save
4. **Select root directory** → tap "Select Root Directory" and choose where your logs will live
5. **Add keywords** → type words you want to flag (e.g., `urgent`, `payment`, your name)
6. Done! Notifications are now being logged ✅

---

## 🗂️ App Structure

```
HomeFragment     → Permission status, keyword management, folder settings, stats
LogsFragment     → All notifications + "You might want to look at this" subsection
StatsFragment    → Bar chart of most-used words
```

### Logs Page Sections
- **"You might want to look at this."** (randomized title) — shows only keyword-matched notifications with smaller gray subtitle: *"these are messages with your keyword inside"*
- **All Notifications** — complete chronological log of everything

---

## 🛠️ Tech Stack

- **Language:** Kotlin
- **Architecture:** MVVM with LiveData
- **Database:** Room (SQLite)
- **Charts:** MPAndroidChart
- **Serialization:** Gson
- **Navigation:** Jetpack Navigation Component
- **UI:** Material Components (Discord-themed)
- **File Storage:** Storage Access Framework (SAF)

---

## 🤖 Auto-Build with GitHub Actions

This repo includes a workflow at `.github/workflows/build.yml` that:
1. Triggers on every push to `main`/`master`
2. Builds a debug APK
3. Uploads it as a build artifact
4. Creates a GitHub Release with the APK attached

No setup needed — just push and the APK is ready in ~3-5 minutes.

---

## 🔒 Permissions Required

| Permission | Reason |
|-----------|--------|
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Core: read all notifications |
| `RECEIVE_BOOT_COMPLETED` | Re-enable service after reboot |
| `WRITE_EXTERNAL_STORAGE` (API ≤28) | Save log files |

---

## 📝 Log File Format

Log files are saved as `notif_log_YYYY-MM-DD.txt` inside your chosen folder:

```
[2024-03-15 09:42:11] [WhatsApp] John Doe: Hey, are you coming tonight?
[2024-03-15 09:43:05] [Gmail] Payment received: Your invoice has been paid [🏷 payment]
[2024-03-15 10:01:33] [Slack] urgent: The server is down! [🏷 urgent, server]
```

---

## 🎨 Color Reference

| Name | Hex | Usage |
|------|-----|-------|
| Blurple | `#5865F2` | Primary actions, highlights |
| Dark BG | `#36393F` | Main background |
| Surface | `#40444B` | Cards |
| Text | `#DCDDDE` | Primary text |
| Muted | `#72767D` | Secondary text |
| Green | `#57F287` | Success states |
| Red | `#ED4245` | Danger/delete |
| Yellow | `#FEE75C` | Keyword tags |
