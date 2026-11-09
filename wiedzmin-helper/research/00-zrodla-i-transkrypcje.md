# Pakiet źródeł — Wiedźmin: Stary Świat (tylko podstawka)

Data: 2026-11-09 (Europe/Warsaw).  
Cel: zebrać materiał z YT/forów/FAQ/instrukcji do weryfikacji helpera.

## Ograniczenie środowiska

YouTube **blokuje** pobieranie napisów z IP chmurowych (`youtube-transcript-api` → `RequestBlocked`).  
Dlatego zamiast pełnych napisów: **spisy rozdziałów** filmów + oficjalne PDF Rebel + FAQ społeczności.

Oficjalne PDF pobrane lokalnie do `/tmp` przy budowie (nie commitowane binaria):
- `Instrukcja_Wiedzmin-Stary-Swiat.pdf` (Rebel)
- `Wiedzmin-FAQ_PL_02-Q4_2023.pdf` (Rebel)

---

## 1. Oficjalna instrukcja Rebel (PDF) — kluczowe cytaty

### Faza I
- Na początku fazy gracz **musi** wykonać 1 **obowiązkowy ruch**.
- Po obowiązkowym ruchu może w dowolnej kolejności wykonywać akcje i kolejne ruchy.
- Albo (z praktyki / FAQ społeczności): przejście od razu do fazy II bez akcji na starcie — **nie** zaczynasz tury akcją obszaru, na którym już stoisz.

### Faza II
Wybór 1 z: walka / medytacja / eksploracja.

### Faza III (krytyczne)
> Możesz odrzucić dowolną liczbę kart ze swojej ręki. Następnie dobierz tyle kart ze swojej talii, **aby mieć 3 karty na ręce**, i kup 1 kartę.

Limit ręki ogólny: **7** (to sufit, nie cel doboru w fazie III).

### Zmęczenie
Gracz usuwa z gry tyle kart akcji, ile wskazuje symbol z lewej strony toru reputacji między aktualnym a poprzednim polem. Karty z ręki / talii / odrzuconych → do pudełka, potem tasowanie talii.  
Przykład: pierwsze trofeum → usuń **1** kartę.

### Zwycięstwo
Reputacja 4 = wygrana **natychmiast**, ale **rozgrywka nie kończy się po medytacji**.

### Eliksiry
Max **4** niezależnie od alchemii. W walce łącznie ≤ poziom alchemii.

### Start wiedźmina
Figurka na obszarze **szkoły** ze swojej planszetki.

---

## 2. FAQ Rebel PL v1.0 — wybrane

| # | Temat | Werdykt |
|---|--------|---------|
| 10 | Tropy | Max 1 żeton tropu na rodzaj terenu; konflikt → odrzuć stare bez złota |
| 21 | Tarcza + obrona | +1 obrona zawsze daje +1 tarczę (nawet z 0) |
| 25 | Limit ręki 7 | Poza walką: odrzuć najpierw, by po dobraniu ≤7. W walce: przy 7 nie dobierasz |
| 26 | Kupno fazy III | Jeśli obiektywnie nie możesz — nie kupujesz |

---

## 3. On Table — wideoinstrukcja PL (rozdziały)

Źródło: https://ontable.pl/wiedzmin-stary-swiat%E2%94%82-zasady-gry-%E2%94%82-jak-grac/  
(film YT osadzony na stronie)

### Przygotowanie
- 00:22 Mapa · 00:33 Karty akcji · 01:41 Eliksiry · 01:55 Trofea · 02:20 Zdarzenia · 02:33 Eksploracja  
- 02:57 Złoto · 03:06 Kości · 03:10 Żetony obszarów · 03:41–05:17 Potwory / rozmieszczenie / walka potworów  
- 05:40 Wybór wiedźmina · 06:27 Reputacja · 07:15 Trofea szkół · 07:43 Figurka · 08:02 Startowe karty · 08:27 Złoto/karty · 08:54 Wygrana

### Faza I (10:26–20:39)
Ruch → akcje obszarów (poker miejscowi, szkoła, eliksiry, wymiana kart, atrybuty, tropienie, karczma) → poker vs wiedźmin.

### Faza II (21:07–46:16)
Walka z potworem (kombinacje, tura potwora, pokonanie/odpędzenie/porażka) → walka wiedźminów (zakłady) → eksploracja → medytacja → warunek zwycięstwa.

### Faza III (46:46–49:00)
Dobór i kupno.

---

## 4. Board Game Shortcut — How to Play (EN, rozdziały)

BGG video / YT: How to Play The Witcher: the Old World  
Struktura: Setup → Phase 1 (move, dice poker, schools, potions, trash, trails) → Phase 2 (explore, meditate, fight) → Phase 3 → victory.  
Bez Monster Trail w tym filmie.

---

## 5. The Die Never Dies — FAQ społeczności (skrót Q/A)

Źródło: https://thedieneverdies.net/faq-the-witcher-old-world/

1. Start deck = 10 kart ze symbolem szkoły.  
2. Startowe karty/złoto = tabela wg liczby graczy.  
3. Trash/burn = usuń z gry (często zmęczenie).  
4–5. Eksploracja A/B; zadania → złoto/ekwipunek/towarzysze.  
10. Tarcza ≤ obrona; po walce reset do obrony.  
11. Awans gdy wszystkie atrybuty > poziomu.  
12. Atrybut V nie spada; medytacja na trofeum atrybutu — **nie wygrywasz** medytacją jako 4. reputacją.  
13. Limit ręki 7.  
14. Nie walczysz na szkołach / zamkniętych karczmach.  
15. Eliksiry max 4; w walce ≤ alchemii.  
16. **Must move** — nie zaczynasz tury akcją na lokacji startowej.  
17. Trop: +1 złoto, żeton terenu potwora → lokacja z numeru (≠ Twoja).

---

## 6. Inne YT (bez napisów — tylko metadane)

| ID / link | Opis |
|-----------|------|
| `HKzLXVLM53M` | Zawartość + setup (PL) |
| `Rw3qTgcxuIw` | Fighting in the Old World |
| `0Ol-AOjO2sk` | Rozgrywka 3-osobowa |
| `yPgv1Lq0rao` | Unboxing / jak wygląda |
| `3SO9ZXzqETY` | How to play ENG video instruction |

---

## 7. Błędy helpera wykryte przy pierwszym przejściu źródeł

1. **P0 — tracker Stołu:** `phase3DrawHint` w `board.js` mówi „dobierz do 7”. Instrukcja: **dobierz do 3**, limit 7 to osobny sufit.  
2. **P1 — zmęczenie:** karty mogą pochodzić z ręki **lub** talii **lub** odrzuconych (nie tylko ręka). Tracker odejmuje tylko z licznika ręki — OK jako uproszczenie, ale tekst musi to mówić.  
3. **P1 — mapa:** schemat przybliżony; brak zdjęć użytkownika w środowisku — do poprawy po uploadzie fotek (starty szkół na własnych obszarach szkoły).

---

## 8. Pipeline AI (ten run)

1. **AI #1** — audyt zasad vs helper → `01-rules-audit-ai1.md`  
2. **AI #2** — re-check interpretacji → `02-rules-recheck-ai2.md`  
3. **AI #3** — wdrożenie poprawek w kodzie  
4. Równolegle: audyt UI / security / idiotoodporność
