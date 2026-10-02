# Gemini Sözlük

EN <-> TR sözlük/çeviri uygulaması (Kotlin + Jetpack Compose + Gemini API).

## Yerelde çalıştırma
`local.properties` içine `GEMINI_API_KEY=...` ekle, Android Studio'da aç.

## GitHub Actions
Repo > Settings > Secrets and variables > Actions > New repository secret
Ad: `GEMINI_API_KEY`. Push sonrası Actions sekmesinde "Artifacts" altından APK indirilir.
