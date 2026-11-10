# Audyt + pętla napraw — status

**Data:** 2026-11-10 (Europe/Warsaw)  
**Branch:** `cursor/roster-szkoly-swap-0aca`  
**Źródła:** rulebook EN, Order of Gamers, Die Never Dies FAQ, research/01–02

## Reguły potwierdzone zewnętrznie
- Tura I → II → III; w II dokładnie 1 z: walka / medytacja / eksploracja
- Wygrana tylko trofeum z walki (4. na torze); medytacja przy „final trophy” bierze kartę bez +reputacji/zmęczenia
- Faza III: dobór do 3, kupno = odrzucenie kart z ręki

## Naprawione w tej pętli
| Severity | Problem | Fix |
| --- | --- | --- |
| CRITICAL | Multi bez kroku potworów przed lockiem | `content.js` krok `monsters` |
| HIGH | Faza II wielokrotnie przez zakładki / hub | `phase2Done` + blokada I/II; hub nie czyści flagi |
| HIGH | „Pomiń spawn” | wymóg spawn przed fazą III |
| HIGH | Fake-win z trackera / +reputacja+zmęczenie | tylko księgowość, bez `declareVictory` |
| MED | Setup panel editable po starcie | `editable: !isGameStarted()` |
| MED | Teksty medytacji / FIGHT_WITCHER | content + guide |
| MED | selectedLoc „ustaw w panelu” po locku | `monstersEditable` |

## Automatyczne sprawdzenia (Node ESM)
- 18/18 logika setup/walka/spawn/Behelt/victory
- 18/18 symulacja 3p (szkoły, potwory, fight options, fatigue×4, witcher co-location)

## UI headless (Puppeteer, 3p)
- 16/16: setup potwory → tura → eksploracja → blokada fazy II → panel locked → tracker bez win → mobile 390 → persist
- Fight path: po pokonaniu brak „Pomiń spawn”, alert wymusza numer, potem faza III

## PASS (wcześniejsze + potwierdzone)
- phase3DrawHint → 3
- Behelt Nar blocked, szkoły OK
- Soft school swap
- Victory screen + gameOver lock tur
- Hub nie omija `phase2Done`

## Re-audyt + follow-up (explore agent)
Naprawione po drugim audycie:
- CRITICAL: usunięty auto-skok phase2→explore (pętla Wróć)
- HIGH: specialty tylko własna szkoła; copy „walka opcjonalna”; clear phase2Done przy zmianie gracza ze stołu; gameStarted dopiero z setupu; multi 3× I
- MED: trop po klęsce; bez zbędnego „wybiera wiedźmina”; witcher fight zablokowany na szkole

Pozostałe MED/LOW (level-up draw, penalty doboru po klęsce, closed inn tracking, CSS nav mobile) — świadome uproszczenia / backlog.
