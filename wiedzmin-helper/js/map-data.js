/**
 * Schemat mapy wg zdjęć planszy (podstawka) — nie kopia grafiki z pudełka.
 * Numery, tereny i drogi zbliżone do fizycznej planszy; pozycje % pod SVG.
 */

export const TERRAIN = {
  forest: { id: "forest", label: "Las", color: "#2f6b45" },
  mountain: { id: "mountain", label: "Góry", color: "#6b5b45" },
  water: { id: "water", label: "Woda", color: "#2f5f8b" },
  school: { id: "school", label: "Szkoła", color: "#b8923e" },
};

/**
 * Akcje obszarów — skróty ze znaków na planszy (pomoc przy stole).
 * Szkoły: trening atrybutu za złoto (−2/−3/−4/−5 = koszt poziomu docelowego).
 */
export const LOCATION_ACTION_HINTS = {
  schoolTrain: "Szkoła: +1 atak / obrona / alchemia / specjalność (koszt złota = poziom docelowy: 2–5).",
  trailGold: "+1 złoto. Opcja: zadanie tropienia (1 złoto → żeton terenu potwora).",
  dicePoker: "Poker kośćmi z miejscowymi.",
  cleanPool: "0 złota → +1 złoto. Możesz spalić karty z puli na polach z płomieniem.",
  trashDraw: "Usuń 1 kartę z ręki → dobierz +1 kartę.",
  defenseLevel: "Obrona ≤ poziom wiedźmina → +1 obrona.",
  alchemyLevel: "Alchemia ≤ poziom wiedźmina → +1 eliksir.",
  specialtyLevel: "Zdolność specjalna ≤ poziom → +1 specjalność.",
  attackLevel: "Atak ≤ poziom wiedźmina → +1 atak.",
  gainElixir: "+1 karta eliksiru (limit 4).",
  cintraElixir: "+1 karta eliksiru.",
};

/** id → lokacja. `number` = numer na planszy; `terrain` = ikona terenu do ruchu. */
export const LOCATIONS = [
  {
    id: "kaer-seren",
    name: "Kaer Seren",
    number: 1,
    terrain: "water",
    school: "gryf",
    x: 22,
    y: 10,
    actionKey: "schoolTrain",
  },
  {
    id: "hengfors",
    name: "Hengfors",
    number: 2,
    terrain: "mountain",
    x: 48,
    y: 16,
    actionKey: "trailGold",
  },
  {
    id: "kaer-morhen",
    name: "Kaer Morhen",
    number: 3,
    terrain: "mountain",
    school: "wilk",
    x: 72,
    y: 12,
    actionKey: "schoolTrain",
  },
  {
    id: "ban-ard",
    name: "Ban Ard",
    number: 4,
    terrain: "water",
    x: 78,
    y: 28,
    actionKey: "alchemyLevel",
  },
  {
    id: "cidaris",
    name: "Cidaris",
    number: 5,
    terrain: "water",
    x: 14,
    y: 28,
    actionKey: "defenseLevel",
  },
  {
    id: "novigrad",
    name: "Novigrad",
    number: 6,
    terrain: "forest",
    x: 32,
    y: 30,
    actionKey: "dicePoker",
  },
  {
    id: "wyzima",
    name: "Wyzima",
    number: 7,
    terrain: "forest",
    x: 48,
    y: 34,
    actionKey: "cleanPool",
  },
  {
    id: "vengerberg",
    name: "Vengerberg",
    number: 8,
    terrain: "forest",
    x: 68,
    y: 38,
    actionKey: "trashDraw",
  },
  {
    id: "cintra",
    name: "Cintra",
    number: 9,
    terrain: "water",
    x: 20,
    y: 46,
    actionKey: "cintraElixir",
  },
  {
    id: "haern-caduch",
    name: "Haern Caduch",
    number: 10,
    terrain: "mountain",
    school: "niedzwiedz",
    x: 42,
    y: 48,
    actionKey: "schoolTrain",
  },
  {
    id: "beauclair",
    name: "Beauclair",
    number: 11,
    terrain: "mountain",
    x: 62,
    y: 50,
    actionKey: "gainElixir",
  },
  {
    id: "glenmore",
    name: "Glenmore",
    number: 12,
    terrain: "water",
    x: 28,
    y: 62,
    actionKey: "specialtyLevel",
  },
  {
    id: "doldeth",
    name: "Doldeth",
    number: 13,
    terrain: "mountain",
    x: 44,
    y: 68,
    actionKey: "trailGold",
  },
  {
    id: "loc-ichaer",
    name: "Loc Ichaer",
    number: 14,
    terrain: "forest",
    x: 56,
    y: 62,
    actionKey: "dicePoker",
  },
  {
    id: "gorthur-gvaed",
    name: "Gorthur Gvaed",
    number: 15,
    terrain: "water",
    school: "zmija",
    x: 82,
    y: 58,
    actionKey: "schoolTrain",
  },
  {
    id: "dhuwod",
    name: "Dhuwod",
    number: 16,
    terrain: "forest",
    x: 48,
    y: 78,
    actionKey: "gainElixir",
  },
  {
    id: "stygga",
    name: "Stygga",
    number: 17,
    terrain: "forest",
    school: "kot",
    x: 66,
    y: 74,
    actionKey: "schoolTrain",
  },
  {
    id: "ard-modron",
    name: "Ard Modron",
    number: 18,
    terrain: "mountain",
    x: 86,
    y: 74,
    actionKey: "attackLevel",
  },
  {
    id: "mantikora",
    name: "Szkoła Mantikory",
    number: 0,
    terrain: "forest",
    school: "mantikora",
    x: 88,
    y: 44,
    actionKey: "schoolTrain",
  },
];

/**
 * Połączenia (drogi + trasy morskie) — ruch tylko do sąsiada po ścieżce.
 * Ułożone na podstawie zdjęć planszy użytkownika.
 */
export const ROADS = [
  // północ
  ["kaer-seren", "hengfors"],
  ["kaer-seren", "novigrad"],
  ["kaer-seren", "cidaris"], // trasa morska
  ["hengfors", "kaer-morhen"],
  ["hengfors", "wyzima"],
  ["hengfors", "novigrad"],
  ["hengfors", "ban-ard"],
  ["kaer-morhen", "ban-ard"],
  ["ban-ard", "vengerberg"],
  // zachód / centrum
  ["cidaris", "novigrad"],
  ["cidaris", "cintra"], // trasa morska / brzeg
  ["novigrad", "wyzima"],
  ["novigrad", "cintra"],
  ["wyzima", "vengerberg"],
  ["wyzima", "haern-caduch"],
  ["vengerberg", "beauclair"],
  ["vengerberg", "mantikora"],
  // południe / szkoły
  ["cintra", "haern-caduch"],
  ["cintra", "glenmore"],
  ["haern-caduch", "beauclair"],
  ["haern-caduch", "glenmore"],
  ["haern-caduch", "loc-ichaer"],
  ["beauclair", "mantikora"],
  ["beauclair", "gorthur-gvaed"],
  ["beauclair", "loc-ichaer"],
  ["glenmore", "doldeth"],
  ["doldeth", "loc-ichaer"],
  ["doldeth", "dhuwod"],
  ["loc-ichaer", "stygga"],
  ["mantikora", "gorthur-gvaed"],
  ["gorthur-gvaed", "ard-modron"],
  ["gorthur-gvaed", "stygga"],
  ["dhuwod", "stygga"],
  ["stygga", "ard-modron"],
];

/** Szkoła gracza → lokacja startowa na planszy (podstawka). */
export const SCHOOL_START = {
  wilk: "kaer-morhen",
  gryf: "kaer-seren",
  niedzwiedz: "haern-caduch",
  kot: "stygga",
  zmija: "gorthur-gvaed",
};

export function locById(id) {
  return LOCATIONS.find((l) => l.id === id);
}

export function schoolStartId(schoolId) {
  return SCHOOL_START[schoolId] || null;
}

export function isSchoolLocationId(locId) {
  const loc = locById(locId);
  return Boolean(loc?.school && loc.school !== "mantikora");
}

/** Ustaw pionek na szkole startowej. `force` — zawsze (np. zmiana szkoły w składzie). */
export function placeAtSchool(player, { force = false } = {}) {
  if (!player) return null;
  const start = schoolStartId(player.schoolId);
  if (!start) return null;
  if (
    force ||
    !player.locationId ||
    isSchoolLocationId(player.locationId) ||
    player.locationId === schoolStartId(player._prevSchoolId)
  ) {
    player.locationId = start;
  }
  return start;
}

export function neighborsOf(id) {
  const out = [];
  for (const [a, b] of ROADS) {
    if (a === id) out.push(b);
    else if (b === id) out.push(a);
  }
  return out;
}

export function actionHintFor(loc) {
  if (!loc?.actionKey) return "";
  return LOCATION_ACTION_HINTS[loc.actionKey] || "";
}

/**
 * Zmęczenie przy wejściu na pole reputacji N (instrukcja: symbol przy polu;
 * przykład: 1. trofeum = usuń 1 kartę).
 */
export const FATIGUE_ON_REP = { 1: 1, 2: 2, 3: 3, 4: 4 };

export function fatigueForReputation(rep) {
  return FATIGUE_ON_REP[rep] ?? 0;
}
