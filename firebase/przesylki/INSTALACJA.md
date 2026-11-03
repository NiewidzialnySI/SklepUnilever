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
2. **Nowy projekt** (albo w istniejącym: `Kod.gs` → Ctrl+A → Delete).
3. Otwórz plik `SklepUnileverPrzesylki.gs` z dysku / repo → **skopiuj CAŁOŚĆ** (ok. 500+ linii, kończy się na `utworzEtykieteJesliBrak`) → wklej do `Kod.gs`.
4. **Ctrl+S** (zapisz). Poczekaj aż zniknie „Zapisywanie…”.
5. Lista funkcji (obok Uruchom) → wybierz `zainstalujTrigger` → Uruchom → zatwierdź uprawnienia Gmail + UrlFetch.
6. **Ustawienia projektu (koło zębate) → Właściwości skryptu** → dodaj:
   - Klucz: `FIREBASE_SA_JSON`
   - Wartość: cały JSON service account (jedna linia, projekt **sklepunilever-app**).
7. Opcjonalnie: `testReczny` na kilku mailach w skrzynce.

Trigger działa co **5 minut**.

### Błędy typowe

| Komunikat | Przyczyna | Co zrobić |
|-----------|-----------|-----------|
| `Unexpected end of input` | Ucięte wklejenie (brak końca pliku) | Skasuj `Kod.gs`, wklej **cały** plik z dysku, Ctrl+S |
| `nie udało się zapisać projektu` | Edytor nie zapisał przed Run | Ctrl+S, odśwież kartę, spróbuj w oknie incognito |
| `Brak FIREBASE_SA_JSON` | Brak właściwości | Krok 6 powyżej |

**Nie wklejaj skryptu z czatu / maila** — łatwo ucina się w połowie. Bierz plik z repo: `firebase/przesylki/SklepUnileverPrzesylki.gs`.

## Reguły RTDB

W `database.rules.json` jest węzeł `shipments` (odczyt: nadajnik + odbiorcy, zapis z apki: nadajnik).  
Zapis ze skryptu idzie tokenem service account (omija reguły klienta).

Wdróż reguły:

```bash
firebase deploy --only database
```

## Forwardy (Outlook / Unilever)

Skrypt łapie też **przekazane** maile DHL/UPS (From = pracownik, nie kurier):
`from:unilever.com`, `to:sklepik.unilever.paczki`, tematy `Powiadomienie` / `[External]`, treść `PIN do odbioru`.

Po wklejeniu nowej wersji `.gs` uruchom w edytorze **`odblokujIPrzetworz`** — zdejmie starą etykietę z ostatnich 14 dni i przetworzy maile ponownie (w tym PIN).

## PIN odbioru

Z treści DHL wyciągane są `pinOdbioru` i `telefonKuriera` → RTDB → kafelek PACZKI w apce (linia **PIN: 828078**).

## Aplikacja

Na ekranie **Dom** (pracownik) sekcja **PACZKI** pokazuje listę z RTDB na żywo.  
Tap = otwarcie linku śledzenia; nieprzeczytane oznaczane po otwarciu. PIN z maila DHL wyświetla się na kafelku.
