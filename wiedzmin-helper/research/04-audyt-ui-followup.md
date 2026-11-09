# Follow-up po audycie UI/security

Źródło: [Audyt UI security helper](bc-614ce9cf-31ef-5bb4-a91e-c7ebf34b2849) + [AI1 weryfikacja zasad](bc-cf54fba6-61cb-53b1-9b39-4308c3aef5e7).

## Wdrożone

- `escapeAttr`: `&` najpierw + `'` `>`
- `normalizeState` / walidacja `localStorage`
- Nav „Tura” i powroty ze Ściągi/Ikon/Leksykonu/Stołu **nie resetują** mid-fazy
- Confirm: wipe, mapa, zmęczenie, zmniejszenie liczby graczy
- Debounce `saveState` 200 ms + try/catch
- Aktywna zakładka nav
- Faza III: koszt = odrzucenie kart (nie złoto)
- Ikona „Odrzuć” bez mylenia z medytacją
- Mapa: Oxenfurt → Loc Ichaer + Dhuwod
- Wyniki walki z potworem (pokonanie 2 złota / odpędzenie / klęska)

## Odłożone (P2)

- Partial DOM update mapy
- Map a11y keyboard
- Solo turn counter
- Phase-tab guards z confirm
