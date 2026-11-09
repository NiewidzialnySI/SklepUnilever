# Audyt zasad — AI #1 (weryfikator)

**Zakres:** Wiedźmin: Stary Świat — **tylko podstawka** (bez dodatków).  
**Pliki helpera:** `content.js`, `sources.js`, `lexicon.js`, `board.js`, `map-data.js`, `app.js` (+ ikony).  
**Źródła odniesienia:** oficjalna instrukcja EN/PL (Rebel / Go On Board), FAQ Rebel PL v1.0 (jesień 2023), FAQ społeczności The Die Never Dies, skrót Order of Gamers.  
**Uwaga:** `/tmp/rebel-faq.txt` i `/tmp/rebel-rules.txt` **nie były obecne** w środowisku; treść FAQ/instrukcji pobrano z oficjalnych PDF-ów Rebel / EN rulebook.

---

## 1) Twierdzenia helpera, które są POPRAWNE

| Temat | Co helper mówi | Źródło |
| --- | --- | --- |
| Struktura tury I→II→III | Ruch/akcje → walka/medytacja/eksploracja → dobór i zakup | Instrukcja (Player’s Turn) |
| Najpierw ruch albo od razu faza II | `app.js` / `phase1-start`: obowiązkowy ruch **albo** przejście do II; nie zaczynasz akcją na starcie | Instrukcja + Die Never Dies FAQ #16 |
| Można pominąć fazę I | „obowiązkowy ruch albo od razu faza II” | Instrukcja: *may skip this Phase entirely* |
| Koszt ruchu | Pasujący teren / 2 dowolne / 1+1 złoto / wild | Instrukcja Phase I |
| Poker z wiedźminem blokuje walkę | „Poker z kimś = nie walczysz z nim w fazie II tej tury” | Instrukcja Dice Poker Clarifications |
| Bez walki PvP na szkole / zamkniętej karczmie | `FIGHT_BRANCH` | Instrukcja + Die Never Dies FAQ #14 |
| Faza II: dokładnie 1 opcja | Walka / medytacja / eksploracja | Instrukcja |
| Medytacja nie kończy gry jako 4. reputacja | `meditate()` + cleanup setup | Instrukcja Meditate Exception + Die Never Dies FAQ #12 |
| Solo: trofeum atrybutu przed 3. potworem | Setup + `meditate(isSolo)` | Instrukcja Solo Mode |
| Limit eliksirów 4; w walce ≤ alchemii | `lexicon` / `sources` / tracker | Instrukcja Potions + Die Never Dies FAQ #15 |
| Tarcza ≤ obrona; po walce = obrona; +1 obrona → +1 tarcza | `lexicon` / `RULE_CORRECTIONS` | Instrukcja + FAQ Rebel #21 + Die Never Dies FAQ #10 |
| Limit ręki 7 (w walce: stop) | `lexicon` COMBAT_STEPS + FAQ w źródłach | Instrukcja Hand-Limit + FAQ Rebel #25 + Die Never Dies FAQ #13 |
| Faza III: odrzuć do max 3, dobierz do 3 | `content.js` → `phase3()` | Instrukcja Phase III kroki 1–2 |
| Kupno w III obowiązkowe; wyjątek gdy obiektywnie nie możesz | `phase3` + FAQ #26 | Instrukcja + FAQ Rebel #26 |
| Modyfikatory puli −1 / +1 | Setup + faza III | Instrukcja Action Cards Cost |
| Kupiona karta trafia na rękę | „Weź na rękę” | Instrukcja: *goes directly into your hand* |
| Trash/burn/zmęczenie = usunięcie z gry | `sources` / ikony | Instrukcja Trash + Die Never Dies FAQ #3 |
| Zmęczenie 1/2/3/4 przy reputacji 1–4 | `FATIGUE_ON_REP` | Instrukcja Fatigue value |
| Startowe karty/złoto 2–5 graczy | `START_RESOURCES` | Tabela setupu w instrukcji |
| Solo start: 5 kart, 3 złota; potwory I+II+III; 1 odkryte trofeum atrybutu | Setup solo | Instrukcja Solo |
| Solo: kość 1–3 szarża / 4–6 ugryzienie; bez nowego potwora po zabiciu; po odpędzeniu nowy tego poziomu; faza III odrzut prawej karty | `fightMonster` / `phase3` / quickRef | Instrukcja Solo Mode |
| Awans: wszystkie atrybuty > poziomu; II/III +1 karta; IV/V +2; V nie obniżasz | `LEVEL_UP` / quickRef | Instrukcja Level Up + Die Never Dies FAQ #11–12 |
| Szkoła: koszt = poziom docelowy (aktualny+1); zdolność tylko w swojej | `LOCATION_ACTIONS` | Instrukcja Location Actions (Schools) |
| Darmowy trening: atrybut ≤ poziomu wiedźmina | `LOCATION_ACTIONS` | Instrukcja / społeczność (Reddit) |
| Tropy: max 1 na rodzaj terenu | `RULE_CORRECTIONS` | FAQ Rebel #10 |
| Koszt karty z eksploracji = nadruk, nie −1/+1 | `RULE_CORRECTIONS` | FAQ Rebel #11 |
| Zdarzenia: nie tasować, rosnąco | Setup | Instrukcja |
| 5 szkół podstawki: Wilk, Gryf, Niedźwiedź, Kot, Żmija | `sources.js` SCHOOLS | Instrukcja / komponenty |
| Tekst karty ma pierwszeństwo | quickRef | Instrukcja |
| Tabela pokera solo (0–6 złota) | `SOLO_POKER_TABLE` | Karta pomocy solo (zgodna ze skrótem w helperze) |

---

## 2) Twierdzenia BŁĘDNE lub WPROWADZAJĄCE W BŁĄD

### HIGH

1. **Tracker Stołu: „dobór do 7” w fazie III**  
   - Helper (`board.js` `phase3DrawHint` + UI): *„Dobierz do 7…”*, *„Faza III (dobór do 7)”*, przycisk *„Ustaw rękę jak po doborze do 7”*.  
   - Reguła: w fazie III odrzucasz do **max 3**, potem dobierasz **do 3** (instrukcja). Limit 7 to globalny hand-limit, nie cel doboru w III.  
   - **Uwaga:** `content.js` `phase3()` ma to dobrze — konflikt wewnętrzny helpera.  
   - **Severity: HIGH**

2. **Koszt karty z puli opisany niejednoznacznie (brak mechaniki zapłaty)**  
   - Helper: *„Kup 1 kartę z puli (koszt nadrukowany ± pole −1/+1)”* — bez wyjaśnienia, że koszt = **odrzucenie tylu kart z ręki**, nie złoto.  
   - Reguła: *cost is how many cards from your hand you must discard* (instrukcja).  
   - Nowy gracz łatwo płaci złotem.  
   - **Severity: HIGH**

3. **Ikona „Odrzuć” — powrót „przy medytacji”**  
   - Helper (`icons.js`): *„Na Twój stos odrzuconych — wróci przy medytacji / tasowaniu.”*  
   - Reguła: medytacja bierze trofeum atrybutu; **nie** odnawia stosu odrzuconych. Karty wracają przy tasowaniu odrzuconych do talii (faza III / dobór poza walką) albo przy przebudowie talii po walce.  
   - **Severity: HIGH** (myli medytację z recyclingiem talii)

### MED

4. **„Akcja obszaru — raz na dany typ w tej turze”**  
   - Helper (`phase1AfterMove`).  
   - Reguła: każda **konkretna** Location Action raz na turę (nawet po wyjściu i powrocie), niekoniecznie „typ terenu”.  
   - **Severity: MED**

5. **Pokonanie potwora: skrót bez „2 złota” i bez 3 wyników walki**  
   - Helper: *„złoto, odwróć kartę…”* — nie podaje **2 złota**; nie rozróżnia jasno: pokonanie / odpędzenie (0–1 karty PW) / klęska (≥2 karty PW) z ich nagrodami.  
   - Reguła: instrukcja Monster Fight Outcomes.  
   - **Severity: MED**

6. **Walka z wiedźminem — uproszczenie nagród/doboru**  
   - Helper `FIGHT_WITCHER`: złoto „wg reputacji”, wspólny reset; brak: dobór 3/4 zależnie od atakujący/broniący i wygrana/przegrana; aktywny przegrany dobiera tylko do 2 w fazie III; max 1 trofeum na szkołę przeciwnika.  
   - **Severity: MED**

7. **„Bez walki na szkole / zamkniętej karczmie”**  
   - Helper uogólnia na „walkę”.  
   - Reguła: zakaz dotyczy **walki z innym wiedźminem**; potwory i tak nie spawnują się typowo na szkołach.  
   - **Severity: MED** (lekko mylące, praktycznie często OK)

8. **Zdolność szkoły w legendzie ikon**  
   - *„Raz na walkę (w swojej szkole / wg karty)”* — miesza **trening** (tylko w swojej szkole) z **użyciem w walce** (gdziekolwiek).  
   - **Severity: MED**

### LOW

9. **Poker z miejscowymi vs z wiedźminem**  
   - Ściąga lokalna: *„1 złoto + 2 z banku”* (OK dla locals). Poker PvP: każdy 1 złoto + **1** z banku. Helper nie rozróżnia w skrócie pokera w fazie I.  
   - **Severity: LOW**

10. **Griffin / inne zdolności** — parafrazy bez dokładnych liczb z planszetki (helper to przyznaje).  
    - **Severity: LOW** (świadome ograniczenie)

11. **Drogi na mapie** — oznaczone jako luźny schemat; nie są oficjalną topologią.  
    - **Severity: LOW** (o ile użytkownik czyta disclaimer)

---

## 3) Brakujące ważne zasady podstawki, których helper powinien uczyć

1. **Płatność za kartę z puli = odrzucenie kart z ręki** (nie złoto) + obowiązek wzięcia karty nawet „niechcianej”.  
2. **Trzy wyniki walki z potworem** (pokonanie / odpędzenie / klęska) z osobnymi nagrodami i limitem doboru do 2 po klęsce.  
3. **Szczegóły pojedynku wiedźminów** (stawki złota z toru, dobór 3/4, −1 do doboru w III dla przegranego atakującego, 0-cost na odrzucone, max 1 trofeum szkoły).  
4. **Obowiązek zagrania ≥1 karty w kombinacji**, jeśli masz karty na ręce w turze walki.  
5. **Kolejność obrażeń** podkreślona dla obu typów walk (tarcza → talia → ręka) — jest w leksykonie, słabo w przewodniku walki.  
6. **Zmęczenie:** przeszukujesz talię+rękę+odrzucone, usuwasz dokładnie N, potem tasujesz resztę w nową talię.  
7. **Akcja obszaru na lokacji startowej:** trzeba najpierw wyjść i wrócić.  
8. **Zadania (quest):** rozwiązywanie w fazie I po wejściu na lokację; zdarzenie z numerem.  
9. **Obstawa (wager)** przy walce wiedźminów (poza 2-osobową).  
10. **Odnowa talii poza walką** zawsze przy pustej talii; **w walce** pustej talii nie tasujesz.  
11. **Spawn potwora** po pokonaniu (poziom +1, III→III) vs po odpędzeniu (ten sam poziom) — w multi skrócone do „zwykle nowy potwór”.  
12. **Zamknięta karczma** zostaje, dopóki nie przeniesie jej kolejny pojedynek.  
13. **Poker z wiedźminem:** nie możesz wybrać kogoś z 0 złota; pula 1+1+1.  
14. **Wymiana w lokacji:** trash z ręki + karta z puli o koszcie ≤ trash+1 (helper ma skrót — OK, ale warto doprecyzować „usuń z gry”).

---

## 4) Problemy map-data (lokacje / szkoły)

**Oczekiwana podstawka:** 5 szkół graczy + 13 miast/obszarów z żetonami lokacji (= **18** lokalizacji z tokenami) oraz specjalna **Szkoła Mantikory (0)** na planszy bez żetonu lokacji (sama lokacja jest na mapie podstawki; wiedźmin Mantikory = dodatek *Monster Trail*).

### Szkoły graczy (5) — OK
Kaer Morhen (Wilk), Kaer Seren (Gryf), Haern Caduch (Niedźwiedź), Stygga (Kot), Gorthur Gvaed (Żmija).

### Błędy / braki nazw

| Helper | Werdykt |
| --- | --- |
| **Oxenfurt** | **BŁĄD** — nie ma na planszy podstawki |
| **Szkoła Mantikory (0)** | **OK jako lokacja na mapie** (instrukcja podstawki); nie jest szkołą startową gracza w podstawce |
| **Loc Ichaer** | **BRAK** w `LOCATIONS` |
| **Dhuwod** | **BRAK** w `LOCATIONS` |

### Zestawienie liczbowe
- Helper ma **18** wpisów (5 szkół + 12 „miast” w tym Oxenfurt + Mantikora).  
- Powinien mieć **19** punktów na schemacie: **18 lokalizacji z tokenami** + Mantikora (0), **bez Oxenfurtu**, **z Loc Ichaer i Dhuwod**.

### Pozostałe 12 poprawnych nazw miast (helper ma je, poza zamianą Oxenfurt→braki)
Hengfors, Ban Ard, Vengerberg, Novigrad, Wyzima, Cidaris, Cintra, Beauclair, Glenmore, Doldeth, Ard Modron — **OK** (plus brakujące Loc Ichaer, Dhuwod).

### Drogi (`ROADS`)
Luźny schemat — **nie weryfikować jako oficjalnej topologii**; helper to deklaruje. Do poprawy dopiero po naprawie listy lokacji.

### Tereny
Szkoły oznaczone `terrain: "school"` — OK na schemacie UI; na prawdziwej planszy szkoły mają też ikonę terenu do ruchu (helper tego nie modeluje).

---

## Werdykt skrótowy

Helper dobrze trzyma szkielet tury, solo, medytację-nie-wygrywa, eliksiry/tarczę/limit 7 oraz setup zasobów.  
**Najpoważniejsze luki do naprawy:** (1) tracker fazy III „do 7” vs reguła „do 3”, (2) brak nauki, że koszt puli to odrzucanie kart, (3) myląca ikona „odrzuć = wraca przy medytacji”, (4) mapa: Oxenfurt zamiast Loc Ichaer/Dhuwod.

*Koniec audytu AI #1 — tylko podstawka.*
