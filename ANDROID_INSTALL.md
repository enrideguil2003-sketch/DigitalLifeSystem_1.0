# Install on a phone (Android 8.0+)

## Get the APK
**A. GitHub Actions (nothing to install on your PC)**
1. Create a private GitHub repository and upload the *contents* of this folder (keep `.github/`, `androidApp/dls.keystore.b64`, `gradle/`).
2. Actions tab -> *Build Android APK* -> *Run workflow* (first run takes ~10-15 min).
3. Download the artifact **DigitalLifeSystem-apk** -> `DigitalLifeSystem.apk`.

**B. Android Studio (your PC)**
Open this folder, wait for Gradle sync, then *Build -> Build APK(s)* or in a terminal:
`./gradlew :androidApp:assembleRelease` (Windows: `gradlew.bat ...`).
APK: `androidApp/build/outputs/apk/release/androidApp-release.apk`.

**C. Directly onto a connected phone (USB debugging on)**
`./gradlew :androidApp:installDebug`

## Install
1. Copy the APK to the phone and open it from Files/Downloads.
2. Allow "Install unknown apps" for that app when asked; if Play Protect warns, tap *Install anyway*.
3. If an older build signed with a different key is already installed, uninstall it first (Android refuses to update across different keys).

Later builds signed with `androidApp/dls.keystore` install over each other and keep your data.
