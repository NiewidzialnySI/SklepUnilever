# Audyt roboczy — wygląd / optymalizacja / security / idiotoodporność

(Wstępna lista; agent audytowy może uzupełnić.)

## Security
- **XSS:** imiona przez `innerHTML` — używane `escapeHtml` / `escapeAttr` w większości miejsc; w sync roster dodano strip `<>`.
- Linki zewnętrzne: `rel="noopener"`.
- Brak backendu / sekretów — ryzyko niskie.

## Idiotoodporność
- Confirm przed „Wyczyść zapis” i „Wyczyść tylko mapę”.
- Faza III: wyraźnie „do 3”, nie „do 7” (naprawione).
- Przyciski „co robi teraz” + tracker reputacji.
- Brakujące: wizard „nie możesz kupić” / blokada walki na szkole (na razie tekstowo w przewodniku).

## Optymalizacja
- Pełny `render()` przy każdym kliknięciu mapy — OK przy małym DOM; ewentualnie partial update później (P2).
- `localStorage` v4 — OK.

## Wygląd
- Mapa schematyczna czytelna; obrót z etykietami counter-rotate.
- Czeka na fotki użytkownika do dokładniejszych ścieżek/startów szkół.
