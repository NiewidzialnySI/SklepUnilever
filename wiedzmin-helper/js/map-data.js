/**
 * Uproszczona mapa-schemat (NIE kopia grafiki z pudełka).
 * Nazwy lokacji z gry podstawowej — pozycje przybliżone, by dało się
 * oznaczać potwory i wiedźminów przy stole.
 */

export const TERRAIN = {
  forest: { id: "forest", label: "Las", color: "#2f6b45" },
  mountain: { id: "mountain", label: "Góry", color: "#6b5b45" },
  water: { id: "water", label: "Woda", color: "#2f5f8b" },
  school: { id: "school", label: "Szkoła", color: "#b8923e" },
};

/** id → { name, terrain, x, y, school? } — współrzędne 0–100 w viewBox */
export const LOCATIONS = [
  { id: "kaer-morhen", name: "Kaer Morhen", terrain: "school", school: "wilk", x: 62, y: 18 },
  { id: "kaer-seren", name: "Kaer Seren", terrain: "school", school: "gryf", x: 48, y: 10 },
  { id: "haern-caduch", name: "Haern Caduch", terrain: "school", school: "niedzwiedz", x: 28, y: 22 },
  { id: "stygga", name: "Stygga", terrain: "school", school: "kot", x: 78, y: 28 },
  { id: "gorthur-gvaed", name: "Gorthur Gvaed", terrain: "school", school: "zmija", x: 72, y: 48 },
  { id: "hengfors", name: "Hengfors", terrain: "mountain", x: 55, y: 28 },
  { id: "ban-ard", name: "Ban Ard", terrain: "forest", x: 58, y: 40 },
  { id: "vengerberg", name: "Vengerberg", terrain: "water", x: 52, y: 52 },
  { id: "novigrad", name: "Novigrad", terrain: "water", x: 40, y: 42 },
  { id: "loc-ichaer", name: "Loc Ichaer", terrain: "water", x: 44, y: 50 },
  { id: "wyzima", name: "Wyzima", terrain: "forest", x: 38, y: 58 },
  { id: "cidaris", name: "Cidaris", terrain: "water", x: 28, y: 48 },
  { id: "cintra", name: "Cintra", terrain: "forest", x: 32, y: 72 },
  { id: "beauclair", name: "Beauclair", terrain: "mountain", x: 22, y: 68 },
  { id: "glenmore", name: "Glenmore", terrain: "forest", x: 48, y: 68 },
  { id: "doldeth", name: "Doldeth", terrain: "mountain", x: 68, y: 62 },
  { id: "ard-modron", name: "Ard Modron", terrain: "mountain", x: 75, y: 72 },
  { id: "dhuwod", name: "Dhuwod", terrain: "forest", x: 58, y: 58 },
  { id: "mantikora", name: "Szkoła Mantikory (0)", terrain: "school", x: 88, y: 55 },
];

/** Luźne połączenia (schemat — nie oficjalna mapa dróg). */
export const ROADS = [
  ["kaer-seren", "kaer-morhen"],
  ["kaer-morhen", "hengfors"],
  ["hengfors", "ban-ard"],
  ["ban-ard", "vengerberg"],
  ["vengerberg", "novigrad"],
  ["novigrad", "loc-ichaer"],
  ["loc-ichaer", "wyzima"],
  ["novigrad", "cidaris"],
  ["cidaris", "beauclair"],
  ["beauclair", "cintra"],
  ["cintra", "glenmore"],
  ["glenmore", "vengerberg"],
  ["glenmore", "doldeth"],
  ["doldeth", "ard-modron"],
  ["doldeth", "gorthur-gvaed"],
  ["gorthur-gvaed", "stygga"],
  ["stygga", "kaer-morhen"],
  ["haern-caduch", "cidaris"],
  ["haern-caduch", "hengfors"],
  ["ard-modron", "mantikora"],
  ["ban-ard", "loc-ichaer"],
  ["ban-ard", "dhuwod"],
  ["dhuwod", "vengerberg"],
  ["dhuwod", "doldeth"],
];

export function locById(id) {
  return LOCATIONS.find((l) => l.id === id);
}

/**
 * Zmęczenie przy wejściu na pole reputacji N (instrukcja: symbol przy polu;
 * przykład: 1. trofeum = usuń 1 kartę).
 */
export const FATIGUE_ON_REP = { 1: 1, 2: 2, 3: 3, 4: 4 };

export function fatigueForReputation(rep) {
  return FATIGUE_ON_REP[rep] ?? 0;
}
