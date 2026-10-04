# Build manual

Proyek ini dapat diunggah langsung ke root repository GitHub.

GitHub Actions memakai:
- Ubuntu 24.04
- Temurin JDK 25
- Android SDK API 36 / Build Tools 36.0.0
- Gradle 9.4.1
- Android Gradle Plugin 9.2.0

Untuk komputer lokal:
- Linux/macOS: `./gradlew :app:assembleDebug`
- Windows: `gradlew.bat :app:assembleDebug`

Release yang dihasilkan tanpa signing key adalah:
`app/build/outputs/apk/release/app-release-unsigned.apk`
