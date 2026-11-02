# Autoaktualizacja — Sklep Unilever

Przy **każdym wejściu / powrocie** do aplikacji (pracownik i rodzina) apka pobiera
`update.json`. Gdy `versionCode` jest wyższy, pokazuje okno na środku.

**Aktualizuj aplikację** → pobiera APK → otwiera **chooser instalatorów**
(Package Installer, menedżer plików, inne aplikacje obsługujące APK).

Zamknięcie okna działa tylko w bieżącej sesji; przy następnym starcie wraca,
jeśli nadal jest nowsza wersja.

URL:

`https://raw.githubusercontent.com/NiewidzialnySI/SklepUnilever/main/update.json`

## Jak wypuścić wersję

1. Podnieś `versionCode` / `versionName` w `app/build.gradle.kts`.
2. Zbuduj APK podpisany **tym samym kluczem** co instalacje użytkowników.
3. Utwórz GitHub Release (tag np. `v1.0.1`) i dołącz `.apk`.
4. Zaktualizuj `update.json` w `main`:

```json
{
  "versionCode": 2,
  "versionName": "1.0.1",
  "apkUrl": "https://github.com/NiewidzialnySI/SklepUnilever/releases/download/v1.0.1/sklep-unilever-v1.0.1.apk",
  "changelog": "Co nowego…"
}
```

5. Commit + push na `main` (apka czyta raw z `main`).

Repo powinno być **publiczne**, żeby `raw.githubusercontent.com` działał bez tokena.
