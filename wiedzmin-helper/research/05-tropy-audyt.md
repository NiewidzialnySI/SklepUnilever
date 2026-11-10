# Audyt tropów (2026-11-10)

## Pytanie
Czy tylko jeden gracz może mieć trop danego potwora, czy kilku?

## Werdykt ze źródeł
- **Limit per gracz:** max 1 żeton tropu / zadania tropienia na każdy rodzaj terenu (las / góry / woda). FAQ Rebel EN #10.
- **Wielu graczy:** tak — kilku graczy może jednocześnie mieć trop tego samego terenu/potwora. Przy pokonaniu instrukcja każe zwrócić tropy i zadania tropienia tego potwora **od wszystkich graczy** do puli (Order of Gamers / rulebook spawn after defeat).
- **Odpędzenie:** tropy zostają (nowy potwór tego samego poziomu/terenu).

## Helper
- Tracker per `roster[i].trails` — OK (nie było globalnego „tylko jeden”).
- Brakowało auto-czyszczenia po pokonaniu → dodane `clearTrailsForTerrain`.
- Copy TRAIL_HELP / sources / UI doprecyzowane.
