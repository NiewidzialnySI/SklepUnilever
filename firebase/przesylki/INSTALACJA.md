# Paczki UPS/DHL → Sklep Unilever

Skrypt Gmail (Apps Script) czyta maile kurierskie i zapisuje je do Firebase RTDB aplikacji.

## Co się zmienia względem starego Sklepik

| | Stary skrypt | Sklep Unilever |
|---|---|---|
| RTDB | `sklepik-unilever-…` | `sklepunilever-app-…` |
| Ścieżka | `/transmitters/{KOD}/shipments` | `/transmitters/{UID}/shipments` |
| Mapowanie | kod = klucz węzła | `pairing_codes/{KOD}.transmitterUid` |
| Etykieta Gmail | `SklepikPrzetworzone` | `SklepUnileverPrzetworzone` |

## Wymagania

1. Konto Gmail (to samo, na które lecą powiadomienia UPS/DHL).
2. Service Account w projekcie Firebase **sklepunilever-app** z dostępem do RTDB (JSON klucza).
3. Aktywne parowanie w aplikacji (wygenerowany kod QR) — inaczej `/pairing_codes/{KOD}` nie istnieje.

## Adresowanie maili (+KOD)

Skrypt wyciąga 8-znakowy kod z adresu:

`twoj.gmail+ABCD23EF@gmail.com`

Ustaw w panelu UPS/DHL (lub regule przekierowania) adres z `+` i aktualnym kodem parowania z zakładki **Urządzenia** w aplikacji (bez spacji).

Po wygenerowaniu **nowego** kodu QR trzeba zaktualizować adres u kuriera — stary `+KOD` przestanie mapować się na nadajnik.

## Instalacja skryptu

1. Otwórz https://script.google.com na koncie Gmail Sklepik.
2. Nowy projekt → wklej treść `SklepUnileverPrzesylki.gs`.
3. **Ustawienia projektu → Właściwości skryptu** → dodaj:
   - Klucz: `FIREBASE_SA_JSON`
   - Wartość: cały JSON service account (jedna linia).
4. Uruchom raz `zainstalujTrigger` (zatwierdź uprawnienia Gmail + UrlFetch).
5. Opcjonalnie: `testReczny` na kilku mailach w skrzynce.

Trigger działa co **5 minut**.

## Reguły RTDB

W `database.rules.json` jest węzeł `shipments` (odczyt: nadajnik + odbiorcy, zapis z apki: nadajnik).  
Zapis ze skryptu idzie tokenem service account (omija reguły klienta).

Wdróż reguły:

```bash
firebase deploy --only database
```

## Aplikacja

Na ekranie **Dom** (pracownik) sekcja **PACZKI** pokazuje listę z RTDB na żywo.  
Tap = otwarcie linku śledzenia; nieprzeczytane oznaczane po otwarciu.
