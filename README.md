# Shopzy Haiti Android App

Android WebView application for **https://shopzyhaiti.lovable.app** featuring:
- 4-second black & orange **Shopzy** splash screen
- Full-screen WebView with pull-to-refresh
- Web-to-Android push notification bridge and customizable alerts
- French offline fallback page (`Vous n'êtes pas connecté à internet 😪`)
- Custom minimalist **Shopzy** app launcher icon

---

## How to Export & Install the APK (No Google Play Console Required)

> **Important:** Pick **APK** (for direct Android phone installation), **not AAB** (which is only for Google Play Console uploads).

### Path A: Direct APK via Google AI Studio Settings Gear (`⚙`)
1. Open the **Settings gear (`⚙`)** icon in your Google AI Studio window.
2. Look for **Generate APK** / **Build APK** (skip any **AAB** or Play Store publishing step).
3. Once generated, download the `.apk` file directly to your phone and tap it to install.

---

### Path B: AI Studio → GitHub → Direct Phone APK Release (No PC Needed)
> **Why GitHub only showed `README`:** Creating an empty repository on GitHub.com only creates a `README` file—GitHub **Releases** only appear *after* you push this AI Studio workspace into GitHub so the cloud APK builder runs.

1. In Google AI Studio (turn on **Desktop site** in Chrome `⋮` if on a phone so the top-right buttons appear), open the **Settings gear (`⚙`)** or **GitHub** menu and select **Push to GitHub**.
2. Choose **Create new repository** inside AI Studio (avoid targeting a repo that already has an initial `README` commit) and push.
3. Open that new GitHub repository's **Actions** tab and wait ~2–3 minutes for **Build Shopzy Haiti APK** to turn green.
4. Open **Releases** (`latest-apk`) on your GitHub repo and tap **`Shopzy-Haiti.apk`** to download straight to your phone.

---

### Path C: Download ZIP → Android Studio (Computer)
1. Click **Download / Export ZIP** in Google AI Studio (not GitHub's empty-repo Download ZIP) and extract the folder on your computer.
2. Restore the debug keystore inside the extracted folder:
   - **Windows:** `certutil -decode debug.keystore.base64 debug.keystore`
   - **macOS:** `base64 -D -i debug.keystore.base64 -o debug.keystore`
   - **Linux:** `base64 -d debug.keystore.base64 > debug.keystore`
3. Open the folder in **Android Studio**, wait for Gradle sync (`9.3.1`), and click **Build → Build App Bundle(s) / APK(s) → Build APK(s)** to generate `app/build/outputs/apk/debug/app-debug.apk`.

---

### Installing the APK on Your Android Phone
1. Tap the downloaded `.apk` file in your phone's **Files** app.
2. If prompted, enable **Allow from this source** for your browser or Files app.
3. Tap **Install** *(if Google Play Protect shows a notice for a sideloaded debug APK, tap **More details → Install anyway**)*.
