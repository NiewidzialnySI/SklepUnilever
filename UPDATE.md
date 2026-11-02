# Autoaktualizacja — Sklep Unilever

Przy każdym uruchomieniu apka pobiera `update.json` i — jeśli `versionCode` jest wyższy —
pokazuje okno na środku ekranu. Zamknięcie okna działa tylko w bieżącej sesji;
przy następnym starcie okno wraca.

URL:

`https://raw.githubusercontent.com/NiewidzialnySI/SklepUnilever/main/update.json`

## Jak wypuścić wersję

1. Podnieś `versionCode` / `versionName` w `app/build.gradle.kts`.
2. Zbuduj APK: `.\gradlew.bat :app:assembleRelease` (lub debug).
3. Utwórz GitHub Release z tagiem np. `v1.0.1` i dołącz `.apk`.
4. Zaktualizuj `update.json` w `main`:

```json
{
  "versionCode": 2,
  "versionName": "1.0.1",
  "apkUrl": "https://github.com/NiewidzialnySI/SklepUnilever/releases/download/v1.0.1/sklep-unilever-v1.0.1.apk",
  "changelog": "Co nowego…"
}
```

5. Commit + push.

Repo powinno być **publiczne**, żeby `raw.githubusercontent.com` działał bez tokena
(albo ustaw token GitHub w prefs debug — jak w dialerze).
