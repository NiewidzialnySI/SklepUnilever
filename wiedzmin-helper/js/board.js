/**
 * Uproszczona mapa-schemat + helpery trackera przy stole.
 * Nie jest kopią oficjalnej grafiki — tylko nazwy lokacji i przybliżone pozycje.
 */
import {
  LOCATIONS,
  ROADS,
  TERRAIN,
  locById,
  neighborsOf,
  actionHintFor,
  fatigueForReputation,
  schoolStartId,
  placeAtSchool,
} from "./map-data.js";
import { ICO, SCHOOL_ICON, actionIconHtml } from "./icons.js";
import { nowDoingGuideHtml, moveCostChooserHtml } from "./guide.js";

export const NOW_ACTIONS = [
  { id: "czekam", label: "Czeka" },
  { id: "faza1", label: "Faza I — ruch" },
  { id: "poker", label: "Poker / akcja obszaru" },
  { id: "walka", label: "Walka" },
  { id: "medytacja", label: "Medytacja" },
  { id: "eksploracja", label: "Eksploracja" },
  { id: "faza3", label: "Faza III — dobór/kupno" },
];

export function defaultBoard() {
  return {
    rotation: 0,
    tool: "select",
    selectedLoc: null,
    placePlayer: 0,
    /** locId → { count, label?, level? } */
    monsters: {},
    /** { playerIdx, fromId, toId, costId? } — komunikat po przesunięciu pionka */
    movePrompt: null,
    /** Podświetl sąsiadów z tej lokacji (ruch) */
    highlightFrom: null,
    /** Poziom ustawiany przyciskiem I/II/III przed oznaczeniem */
    pendingMonsterLevel: "I",
    /** Komunikat gdy kliknięcie nie może postawić potwora (ten sam teren itd.) */
    placeError: null,
  };
}

export function monsterAt(board, locId) {
  const m = board?.monsters?.[locId];
  if (!m || !(m.count > 0)) return null;
  return m;
}

export function monstersList(board) {
  return Object.entries(board?.monsters || {})
    .filter(([, m]) => m && m.count > 0)
    .map(([id, m]) => ({ id, loc: locById(id), ...m }))
    .filter((row) => row.loc);
}

export const MONSTER_TERRAINS = ["forest", "mountain", "water"];

/** Lokacje danego terenu (numery z planszy) — do wyboru żetonu obszaru. */
export function locationsForTerrain(terrain) {
  return LOCATIONS.filter((l) => l.terrain === terrain).sort(
    (a, b) => (a.number ?? 99) - (b.number ?? 99)
  );
}

/** Inny potwór już zajmuje ten sam rodzaj terenu (las / góry / woda). */
export function monsterOccupyingTerrain(board, terrain, exceptLocId = null) {
  if (!terrain || !MONSTER_TERRAINS.includes(terrain)) return null;
  for (const row of monstersList(board)) {
    if (exceptLocId && row.id === exceptLocId) continue;
    if (row.loc.terrain === terrain) return row;
  }
  return null;
}

/**
 * Ustaw potwora dla slotu terenu (max 1 na las/góry/wodę).
 * locId = null → wyczyść slot. Zawsze jeden żeton na teren.
 */
export function setMonsterForTerrain(board, terrain, locId, { level } = {}) {
  if (!MONSTER_TERRAINS.includes(terrain)) {
    return { ok: false, reason: "Nieznany teren." };
  }
  for (const row of monstersList(board)) {
    if (row.loc.terrain === terrain) delete board.monsters[row.id];
  }
  if (!locId) {
    board.placeError = null;
    return { ok: true, cleared: true };
  }
  const loc = locById(locId);
  if (!loc) return { ok: false, reason: "Nieznana lokacja." };
  if (loc.terrain !== terrain) {
    return {
      ok: false,
      reason: `Lokacja ${loc.name} to ${TERRAIN[loc.terrain]?.label || loc.terrain}, nie ${
        TERRAIN[terrain]?.label || terrain
      }.`,
    };
  }
  const lv = level || "I";
  board.monsters[locId] = { count: 1, level: lv, label: `Poz. ${lv}` };
  board.placeError = null;
  board.selectedLoc = locId;
  return { ok: true, placed: true, level: lv, loc };
}

/** Przewiń numer obszaru w slocie terenu (mysz: ◀ ▶). */
export function cycleMonsterTerrain(board, terrain, delta) {
  const opts = locationsForTerrain(terrain);
  if (!opts.length) return { ok: false, reason: "Brak lokacji." };
  const cur = monsterOccupyingTerrain(board, terrain);
  let idx = cur ? opts.findIndex((l) => l.id === cur.id) : -1;
  if (idx < 0) idx = delta > 0 ? -1 : 0;
  const next = opts[(idx + delta + opts.length) % opts.length];
  const level = cur?.level || "I";
  return setMonsterForTerrain(board, terrain, next.id, { level });
}

/** Ustaw slot terenu po numerze z planszy (0–18). */
export function setMonsterTerrainByNumber(board, terrain, number, { level } = {}) {
  const n = Number(number);
  if (!Number.isFinite(n)) return { ok: false, reason: "Podaj numer obszaru." };
  const loc = locationsForTerrain(terrain).find((l) => l.number === n);
  if (!loc) {
    const nums = locationsForTerrain(terrain)
      .map((l) => l.number)
      .join(", ");
    return {
      ok: false,
      reason: `Numer ${n} nie należy do terenu „${TERRAIN[terrain]?.label || terrain}”. Dostępne: ${nums}.`,
    };
  }
  const cur = monsterOccupyingTerrain(board, terrain);
  return setMonsterForTerrain(board, terrain, loc.id, {
    level: level || cur?.level || "I",
  });
}

export function setMonsterTerrainLevel(board, terrain, level) {
  const cur = monsterOccupyingTerrain(board, terrain);
  if (!cur) {
    return { ok: false, reason: "Najpierw wybierz numer obszaru dla tego terenu." };
  }
  board.monsters[cur.id] = {
    count: 1,
    level,
    label: `Poz. ${level}`,
  };
  board.placeError = null;
  return { ok: true };
}

/** Stare zapisy: max 1 potwór na teren, count zawsze 1. */
export function sanitizeMonstersUniqueTerrain(board) {
  if (!board?.monsters) return board;
  const seen = Object.create(null);
  for (const id of Object.keys(board.monsters)) {
    const m = board.monsters[id];
    if (!m || !(m.count > 0)) {
      delete board.monsters[id];
      continue;
    }
    const loc = locById(id);
    if (!loc || !MONSTER_TERRAINS.includes(loc.terrain)) {
      delete board.monsters[id];
      continue;
    }
    m.count = 1;
    if (seen[loc.terrain]) {
      delete board.monsters[id];
      continue;
    }
    seen[loc.terrain] = id;
  }
  return board;
}

export function monsterEncounterHtml(loc, monster) {
  if (!loc || !monster) return "";
  const lvl = monster.level ? ` poziom ${escapeHtml(String(monster.level))}` : "";
  const label = monster.label ? ` — ${escapeHtml(monster.label)}` : "";
  return `
    <div class="guide-box guide-monster-hit" role="status">
      <p class="step-kicker" style="margin:0">${ICO.monster()} Potwór na polu</p>
      <h3 style="margin:0.25rem 0 0.4rem">Na <strong>${escapeHtml(loc.name)}</strong> stoi potwór${lvl}${label}</h3>
      <p class="lead" style="margin:0"><strong>Nie musisz walczyć.</strong> Instrukcja: w fazie II <em>możesz</em> walczyć z potworem na tym samym obszarze — albo wybrać medytację albo eksplorację.</p>
      <p class="note" style="margin:0.45rem 0 0">Teraz jesteś w fazie I: akcje obszaru / zadanie / kolejny ruch. Walka dopiero gdy w fazie II wybierzesz „Walka”.</p>
    </div>
  `;
}

const LEVEL_ORDER = ["I", "II", "III"];

export function normalizeMonsterLevel(level) {
  const s = String(level || "I").toUpperCase();
  if (s === "1" || s === "I") return "I";
  if (s === "2" || s === "II") return "II";
  if (s === "3" || s === "III") return "III";
  return LEVEL_ORDER.includes(s) ? s : "I";
}

/** Po pokonaniu: I→II, II→III, III→III. */
export function nextMonsterLevelAfterDefeat(level) {
  const lv = normalizeMonsterLevel(level);
  if (lv === "I") return "II";
  return "III";
}

/**
 * Wynik walki z potworem → co zrobić z żetonem (instrukcja + solo).
 * kind: defeat | drive | loss
 */
export function monsterFightOutcomePlan({ kind, isSolo, loc, monster }) {
  const level = normalizeMonsterLevel(monster?.level);
  const terrain = loc?.terrain || null;
  const terrainLabel = terrain ? TERRAIN[terrain]?.label || terrain : "?";
  const locName = loc?.name || "?";

  if (kind === "loss") {
    return {
      kind,
      removeMonster: false,
      needsSpawn: false,
      spawnLevel: null,
      terrain,
      terrainLabel,
      oldLevel: level,
      title: "Klęska",
      summary: `Potwór zostaje na <strong>${escapeHtml(locName)}</strong> (poz. ${escapeHtml(
        level
      )}). Przy następnej walce ma pełną talię. Weź trop terenu „${escapeHtml(
        terrainLabel
      )}” (jeśli go nie masz) i 1 kartę kosztu 0 na odrzucone; w tej turze w fazie III dobierasz o 1 mniej.`,
      spawnHint: null,
    };
  }

  if (kind === "drive") {
    return {
      kind,
      removeMonster: true,
      needsSpawn: true,
      spawnLevel: level,
      terrain,
      terrainLabel,
      oldLevel: level,
      title: "Odpędzenie",
      summary: `Zdejmij żeton z <strong>${escapeHtml(locName)}</strong>. +2 złota; żeton na stos odpędzonych; 1 karta kosztu 0 na odrzucone.`,
      spawnHint: `Nowy potwór: <strong>poziom ${escapeHtml(
        level
      )}</strong> (ten sam), teren <strong>${escapeHtml(
        terrainLabel
      )}</strong> — dobierz żeton obszaru tego terenu i wpisz jego numer poniżej.`,
    };
  }

  // defeat
  if (isSolo) {
    return {
      kind,
      removeMonster: true,
      needsSpawn: false,
      spawnLevel: null,
      terrain,
      terrainLabel,
      oldLevel: level,
      title: "Pokonanie (solo)",
      summary: `Zdejmij żeton z <strong>${escapeHtml(
        locName
      )}</strong>. +2 złota, trofeum, +reputacja, zmęczenie. <strong>Solo: nie dobierasz nowego potwora</strong> po pokonaniu.`,
      spawnHint: null,
    };
  }

  const nextLv = nextMonsterLevelAfterDefeat(level);
  return {
    kind,
    removeMonster: true,
    needsSpawn: true,
    spawnLevel: nextLv,
    terrain,
    terrainLabel,
    oldLevel: level,
    title: "Pokonanie",
    summary: `Zdejmij żeton z <strong>${escapeHtml(
      locName
    )}</strong>. +2 złota, trofeum, +reputacja, zmęczenie.`,
    spawnHint: `Nowy potwór: <strong>poziom ${escapeHtml(
      nextLv
    )}</strong> (${escapeHtml(level)}→${escapeHtml(
      nextLv
    )}), teren <strong>${escapeHtml(
      terrainLabel
    )}</strong> — dobierz żeton obszaru tego samego terenu i wpisz numer poniżej.`,
  };
}

/** Jednoslotowy picker numeru dla nowego potwora po walce. */
export function monsterSpawnSlotHtml(board, { terrain, level }) {
  if (!terrain || !MONSTER_TERRAINS.includes(terrain)) return "";
  const t = TERRAIN[terrain];
  const opts = locationsForTerrain(terrain);
  const cur = monsterOccupyingTerrain(board, terrain);
  const num = cur?.loc?.number;
  const name = cur?.loc?.name || "— ustaw numer z żetonu —";
  const lv = normalizeMonsterLevel(level || cur?.level || "I");
  return `
    <div class="monster-terrain-slot is-filled monster-spawn-slot" data-terrain-slot="${terrain}">
      <div class="monster-terrain-head">
        <span class="monster-terrain-ico">${terrainIcon(terrain)}</span>
        <strong>${escapeHtml(t.label)}</strong>
        <span class="monster-terrain-meta">${escapeHtml(name)} · poz. ${escapeHtml(lv)}</span>
      </div>
      <div class="monster-terrain-pick" role="group" aria-label="Numer nowego obszaru">
        <button type="button" class="btn btn-secondary monster-num-btn" data-monster-cycle="${terrain}" data-delta="-1" title="Poprzedni">◀</button>
        <input type="number" class="monster-num-input" data-monster-num="${terrain}" min="0" max="18" inputmode="numeric" value="${
          num != null ? escapeHtml(String(num)) : ""
        }" placeholder="#" aria-label="Numer obszaru" />
        <button type="button" class="btn btn-secondary monster-num-btn" data-monster-cycle="${terrain}" data-delta="1" title="Następny">▶</button>
      </div>
      <p class="note monster-terrain-hint">Poziom ustawiony na <strong>${escapeHtml(
        lv
      )}</strong> · numery: ${opts.map((l) => l.number).join(", ")}</p>
    </div>
  `;
}

export function monstersPanelHtml(board) {
  const levels = ["I", "II", "III"];
  const slots = MONSTER_TERRAINS.map((terrain) => {
    const t = TERRAIN[terrain];
    const opts = locationsForTerrain(terrain);
    const cur = monsterOccupyingTerrain(board, terrain);
    const num = cur?.loc?.number;
    const name = cur?.loc?.name || "— wybierz numer —";
    const level = cur?.level || "I";
    const filled = Boolean(cur);
    return `
      <div class="monster-terrain-slot ${filled ? "is-filled" : "is-empty"}" data-terrain-slot="${terrain}">
        <div class="monster-terrain-head">
          <span class="monster-terrain-ico">${terrainIcon(terrain)}</span>
          <strong>${escapeHtml(t.label)}</strong>
          <span class="monster-terrain-meta">${filled ? escapeHtml(name) : "brak żetonu"}</span>
        </div>
        <div class="monster-terrain-pick" role="group" aria-label="Numer obszaru: ${escapeHtml(t.label)}">
          <button type="button" class="btn btn-secondary monster-num-btn" data-monster-cycle="${terrain}" data-delta="-1" title="Poprzedni obszar">◀</button>
          <input type="number" class="monster-num-input" data-monster-num="${terrain}" min="0" max="18" inputmode="numeric" value="${
            num != null ? escapeHtml(String(num)) : ""
          }" placeholder="#" aria-label="Numer obszaru ${escapeHtml(t.label)}" />
          <button type="button" class="btn btn-secondary monster-num-btn" data-monster-cycle="${terrain}" data-delta="1" title="Następny obszar">▶</button>
          <button type="button" class="btn btn-secondary" data-monster-terrain-clear="${terrain}" ${
            filled ? "" : "disabled"
          }>Usuń</button>
        </div>
        <div class="monster-terrain-levels" role="group" aria-label="Poziom potwora ${escapeHtml(t.label)}">
          ${levels
            .map(
              (lv) =>
                `<button type="button" class="btn ${
                  filled && level === lv ? "btn-primary" : "btn-secondary"
                }" data-monster-terrain-level="${terrain}" data-level="${lv}" ${
                  filled ? "" : "disabled"
                }>Poz. ${lv}</button>`
            )
            .join("")}
        </div>
        <p class="note monster-terrain-hint">Numery ${opts
          .map((l) => l.number)
          .join(", ")}</p>
      </div>`;
  }).join("");

  return `
    <div class="monsters-panel">
      <h3>${ICO.monster()} Potwory na mapie</h3>
      <p class="lead" style="margin:0">Trzy żetony obszarów = <strong>3 różne tereny</strong>. Podaj <strong>numer</strong> z żetonu (strzałki albo wpisz) — drugi potwór na ten sam teren jest niemożliwy.</p>
      ${
        board.placeError
          ? `<p class="warn" style="margin:0.45rem 0 0" role="alert">${escapeHtml(board.placeError)}</p>`
          : ""
      }
      <div class="monster-terrain-grid">${slots}</div>
      <p class="note" style="margin:0.45rem 0 0">Walka w fazie II jest <strong>opcjonalna</strong> — tylko gdy żeton stoi na Twoim polu.</p>
    </div>
  `;
}

export function ensurePlayerTrack(p, startCards = 5, startGold = 3) {
  if (typeof p.reputation !== "number") p.reputation = 0;
  if (typeof p.cards !== "number") p.cards = startCards;
  if (typeof p.gold !== "number") p.gold = startGold;
  if (!p.locationId) p.locationId = null;
  if (!p.nowDoing) p.nowDoing = "czekam";
  if (typeof p.elixirs !== "number") p.elixirs = 0;
  if (!Array.isArray(p.quests)) p.quests = [];
  if (!p.attrs || typeof p.attrs !== "object") {
    p.attrs = { attack: 1, defense: 1, alchemy: 1, specialty: 1, level: 1 };
  }
  if (typeof p.shield !== "number") p.shield = p.attrs.defense || 1;
  if (!Array.isArray(p.attrTrophies)) p.attrTrophies = [];
  if (!p.trails || typeof p.trails !== "object") {
    p.trails = { forest: null, mountain: null, water: null };
  }
  if (typeof p.resourcesApplied !== "boolean") p.resourcesApplied = false;
  return p;
}

export function handAfterFatigue(cards, rep) {
  const lose = fatigueForReputation(rep);
  const after = Math.max(0, cards - lose);
  return { lose, after, before: cards };
}

/** Faza III (instrukcja): opcjonalnie odrzuć dowolną liczbę, potem dobierz do 3 na ręce, kup 1.
 *  Limit 7 to osobny sufit (FAQ #25), nie cel doboru w fazie III. */
export function phase3DrawHint(cardsInHand) {
  const phase3Target = 3;
  const hardLimit = 7;
  const afterDiscardAssume = Math.min(cardsInHand, phase3Target);
  const need = Math.max(0, phase3Target - afterDiscardAssume);
  return {
    limit: hardLimit,
    phase3Target,
    drawUpTo: need,
    afterDraw: phase3Target,
    note:
      cardsInHand > phase3Target
        ? `Masz ${cardsInHand} → w fazie III zwykle odrzucasz (dowolną liczbę), potem dobierasz do 3, potem kupujesz 1 (limit ręki nadal 7).`
        : cardsInHand < phase3Target
          ? `Dobierz do 3: +${need} kart(y), potem kup 1 z puli (limit ręki 7).`
          : "Masz 3 — w fazie III możesz odrzucić część i dobrać z powrotem do 3, potem kup 1.",
  };
}

function escapeXml(s) {
  return String(s)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

export function mapSvgHtml(
  board,
  roster,
  { schoolById, highlightFromId, showDisclaimer = false, popupHtml = null } = {}
) {
  const locMap = Object.fromEntries(LOCATIONS.map((l) => [l.id, l]));
  const fromId = highlightFromId || board.highlightFrom || null;
  const reachable = new Set(fromId ? neighborsOf(fromId) : []);

  const roads = ROADS.map(([a, b]) => {
    const A = locMap[a];
    const B = locMap[b];
    if (!A || !B) return "";
    const lit =
      fromId && ((a === fromId && reachable.has(b)) || (b === fromId && reachable.has(a)));
    return `<line x1="${A.x}" y1="${A.y}" x2="${B.x}" y2="${B.y}" class="map-road${lit ? " map-road-lit" : ""}" />`;
  }).join("");

  const rot = board.rotation || 0;
  const labelUpright = -rot;

  const nodes = LOCATIONS.map((loc) => {
    const t = TERRAIN[loc.terrain] || TERRAIN.forest;
    const fill = loc.school ? TERRAIN.school.color : t.color;
    const selected = board.selectedLoc === loc.id ? " map-node-selected" : "";
    const here = fromId === loc.id ? " map-node-here" : "";
    const reach = reachable.has(loc.id) ? " map-node-reach" : "";
    const monsters = board.monsters[loc.id];
    const witches = roster
      .map((p, i) => ({ p, i }))
      .filter(({ p }) => p.locationId === loc.id);
    const numLabel =
      typeof loc.number === "number" ? String(loc.number) : "";

    const monsterBadge = monsters
      ? `<g class="map-monster-badge" transform="translate(${loc.x + 3.2},${loc.y - 3.8}) rotate(${labelUpright})">
           <circle r="2.2" fill="#6b3a2f" stroke="#f0e6d2" stroke-width="0.35"/>
           <text y="0.85" text-anchor="middle" class="map-badge-text">${escapeXml(String(monsters.count || 1))}</text>
         </g>`
      : "";

    const witchBadges = witches
      .map(({ i }, idx) => {
        const ox = -3.2 + idx * 2.4;
        return `<g transform="translate(${loc.x + ox},${loc.y + 4.2}) rotate(${labelUpright})">
          <circle r="1.7" fill="#244833" stroke="#d4b05a" stroke-width="0.35"/>
          <text y="0.7" text-anchor="middle" class="map-badge-text">${i + 1}</text>
        </g>`;
      })
      .join("");

    return `
      <g class="map-node${selected}${here}${reach}" data-loc="${loc.id}" style="cursor:pointer">
        ${
          reach
            ? `<circle cx="${loc.x}" cy="${loc.y}" r="4.6" fill="none" stroke="#d4b05a" stroke-width="0.55" stroke-dasharray="1.2 0.8" opacity="0.95"/>`
            : ""
        }
        <circle cx="${loc.x}" cy="${loc.y}" r="3.4" fill="${fill}" stroke="#f0e6d2" stroke-width="0.45"/>
        <circle cx="${loc.x}" cy="${loc.y}" r="2.35" fill="none" stroke="${t.color}" stroke-width="0.55"/>
        <text x="${loc.x}" y="${loc.y + 1.05}" text-anchor="middle" class="map-badge-text">${escapeXml(numLabel)}</text>
        <text x="${loc.x}" y="${loc.y - 4.8}" text-anchor="middle" class="map-label" transform="rotate(${labelUpright} ${loc.x} ${loc.y - 4.8})">${escapeXml(loc.name)}</text>
        ${monsterBadge}
        ${witchBadges}
      </g>`;
  }).join("");

  return `
    <div class="map-stage${popupHtml ? " map-stage--has-popup" : ""}">
      <div class="map-wrap" style="--map-rot:${rot}deg">
        <svg class="map-svg" viewBox="0 0 100 85" role="img" aria-label="Mapa obszarów">
          <defs>
            <radialGradient id="mapGlow" cx="50%" cy="40%" r="60%">
              <stop offset="0%" stop-color="#2a4032"/>
              <stop offset="100%" stop-color="#152018"/>
            </radialGradient>
          </defs>
          <rect x="0" y="0" width="100" height="85" rx="2" fill="url(#mapGlow)"/>
          <g class="map-roads">${roads}</g>
          <g class="map-nodes">${nodes}</g>
        </svg>
      </div>
      ${
        popupHtml
          ? `<div class="map-loc-popup" id="map-loc-popup" role="dialog" aria-live="polite">${popupHtml}</div>`
          : ""
      }
    </div>
    ${
      showDisclaimer
        ? `<p class="note">Schemat: numery 0–18, tereny i drogi. Szkoły = złoty punkt.</p>`
        : ""
    }
  `;
}

let _mapPopupCleanup = null;

/** Dymek nad mapą: znika po 3 s albo kliku poza oknem (wybór celu zostaje). */
export function bindMapLocPopup(root, { timeoutMs = 3000 } = {}) {
  if (typeof _mapPopupCleanup === "function") {
    _mapPopupCleanup();
    _mapPopupCleanup = null;
  }
  const popup = root?.querySelector?.("#map-loc-popup");
  if (!popup) return () => {};

  let gone = false;
  let outsideTimer = null;
  const hide = () => {
    if (gone) return;
    gone = true;
    popup.classList.add("map-loc-popup--gone");
    window.clearTimeout(timer);
    if (outsideTimer) window.clearTimeout(outsideTimer);
    document.removeEventListener("pointerdown", onOutside, true);
    if (_mapPopupCleanup === hide) _mapPopupCleanup = null;
  };

  const onOutside = (e) => {
    const t = e.target;
    if (!(t instanceof Element)) return;
    if (popup.contains(t)) return;
    if (t.closest(".map-node-reach")) return;
    hide();
  };

  const timer = window.setTimeout(hide, timeoutMs);
  outsideTimer = window.setTimeout(() => {
    if (!gone) document.addEventListener("pointerdown", onOutside, true);
  }, 0);

  _mapPopupCleanup = hide;
  return hide;
}

/** Pasek narzędzi mapy usunięty — oznaczanie potworów jest w panelu Potwory. */
export function mapToolbarHtml() {
  return "";
}

function escapeHtml(s) {
  return String(s)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");
}

function terrainIcon(id) {
  if (id === "forest") return ICO.forest();
  if (id === "mountain") return ICO.mountain();
  if (id === "water") return ICO.water();
  return "";
}

export function selectedLocPanel(board, roster) {
  const loc = locById(board.selectedLoc);
  if (!loc) {
    return `<p class="lead">Kliknij lokację na mapie. Potwory ustawiasz numerami w panelu (1 na teren).</p>`;
  }
  const t = TERRAIN[loc.terrain];
  const m = monsterAt(board, loc.id);
  const here = roster
    .map((p, i) => ({ p, i }))
    .filter(({ p }) => p.locationId === loc.id);
  const hint = actionHintFor(loc);
  const neigh = neighborsOf(loc.id)
    .map((id) => locById(id))
    .filter(Boolean)
    .map((n) => `${n.name}${typeof n.number === "number" ? ` (${n.number})` : ""}`)
    .join(", ");
  const otherSame =
    !m && MONSTER_TERRAINS.includes(loc.terrain)
      ? monsterOccupyingTerrain(board, loc.terrain)
      : null;

  return `
    <div class="loc-panel">
      <h3>${escapeHtml(loc.name)}${
        typeof loc.number === "number" ? ` <span class="loc-num">#${loc.number}</span>` : ""
      }</h3>
      <p class="lead" style="margin:0">${terrainIcon(loc.terrain)} Teren ruchu: <strong>${
        t.label
      }</strong>${loc.school ? ` · szkoła wiedźmińska` : ""}</p>
      ${
        loc.actionKey
          ? `<div class="loc-action-icon">${actionIconHtml(loc.actionKey, { width: 130, height: 40 })}</div>`
          : ""
      }
      ${hint ? `<p class="loc-action"><strong>Znak na planszy:</strong> ${escapeHtml(hint)}</p>` : ""}
      <p class="note" style="margin:0.4rem 0 0">Sąsiedzi (ścieżki): ${
        neigh ? escapeHtml(neigh) : "—"
      }</p>
      <p class="tip" style="margin:0.5rem 0 0">Po ruchu helper pokaże kroki tej akcji (faza I, opcjonalna).</p>
      ${
        m
          ? `<p class="warn" style="margin:0.45rem 0 0">${ICO.monster()} Potwór tu: poz. <strong>${escapeHtml(
              String(m.level || "?")
            )}</strong></p>`
          : otherSame
            ? `<p class="note" style="margin:0.45rem 0 0">${ICO.monster()} Slot „${escapeHtml(
                t.label
              )}” zajęty przez <strong>${escapeHtml(otherSame.loc.name)}</strong> (#${
                otherSame.loc.number
              }).</p>`
            : `<p class="note" style="margin:0.45rem 0 0">${ICO.monster()} Brak potwora na tym polu — ustaw numer w panelu poniżej.</p>`
      }
      <p>Wiedźmini:
        ${
          here.length
            ? here
                .map(
                  ({ p, i }) =>
                    `<span class="player-chip">${i + 1}. ${escapeHtml(p.name)}</span>`
                )
                .join(" ")
            : "—"
        }
      </p>
    </div>
  `;
}

export function movePromptHtml(board, roster) {
  const mp = board.movePrompt;
  if (!mp) return "";
  const p = roster[mp.playerIdx];
  if (!p) return "";
  return moveCostChooserHtml({
    fromLoc: locById(mp.fromId),
    toLoc: locById(mp.toId),
    playerName: p.name,
    selectedCostId: mp.costId || null,
  });
}

export function playerTrackerHtml(roster, { schoolById, activeIndex, START_RESOURCES, players, isSolo }) {
  return roster
    .map((p, i) => {
      const school = schoolById(p.schoolId);
      const iconFn = SCHOOL_ICON[p.schoolId] || ICO.specialty;
      const loc = p.locationId ? locById(p.locationId) : null;
      const fat = fatigueForReputation(Math.min(4, (p.reputation || 0) + 1));
      const nextRep = Math.min(4, (p.reputation || 0) + 1);
      const afterFat = handAfterFatigue(p.cards || 0, nextRep);
      const draw = phase3DrawHint(p.cards || 0);
      const start = START_RESOURCES[players]?.[i] || { cards: 5, gold: 3 };
      const guide = nowDoingGuideHtml(p, { isSolo: !!isSolo });

      return `
        <article class="tracker-card ${i === activeIndex ? "tracker-active" : ""}" data-track="${i}">
          <header>
            <span class="player-chip">${iconFn()}${escapeHtml(p.name)} <span>${school.short}</span></span>
            ${i === activeIndex ? `<span class="badge">Teraz gra</span>` : ""}
          </header>

          <div class="tracker-grid">
            <label>Reputacja (0–4)
              <div class="rep-row">
                ${[0, 1, 2, 3, 4]
                  .map(
                    (n) =>
                      `<button type="button" class="rep-btn ${(p.reputation || 0) === n ? "active" : ""}" data-rep="${n}" data-idx="${i}">${n}</button>`
                  )
                  .join("")}
              </div>
            </label>

            <label>Karty na ręce
              <div class="num-row">
                <button type="button" class="btn btn-secondary" data-cards-delta="-1" data-idx="${i}">−</button>
                <strong class="num-val">${p.cards ?? start.cards}</strong>
                <button type="button" class="btn btn-secondary" data-cards-delta="1" data-idx="${i}">+</button>
              </div>
            </label>

            <label>Złoto
              <div class="num-row">
                <button type="button" class="btn btn-secondary" data-gold-delta="-1" data-idx="${i}">−</button>
                <strong class="num-val">${p.gold ?? start.gold}</strong>
                <button type="button" class="btn btn-secondary" data-gold-delta="1" data-idx="${i}">+</button>
              </div>
            </label>

            <label>Eliksiry (max 4)
              <div class="num-row">
                <button type="button" class="btn btn-secondary" data-elixir-delta="-1" data-idx="${i}">−</button>
                <strong class="num-val">${p.elixirs ?? 0}</strong>
                <button type="button" class="btn btn-secondary" data-elixir-delta="1" data-idx="${i}">+</button>
              </div>
            </label>
          </div>

          <p class="tip"><strong>Lokalizacja:</strong> ${
            loc ? escapeHtml(loc.name) : "szkoła startowa (ustawi się po wyborze szkoły)"
          }</p>

          ${
            (p.quests || []).length
              ? `<div class="tracker-quests">
                  <p class="step-kicker" style="margin:0.35rem 0 0.25rem">${ICO.quest()} Zadania (${p.quests.length})</p>
                  <ul class="checklist">
                    ${p.quests
                      .map((q) => {
                        const ql = q.locationId ? locById(q.locationId) : null;
                        return `<li>
                          <strong>${escapeHtml(q.title || "Zadanie")}</strong>
                          → ${ql ? escapeHtml(ql.name) : "?"}
                          ${q.eventCard ? ` · Event <strong>#${escapeHtml(String(q.eventCard))}</strong>` : ""}
                          <button type="button" class="btn btn-secondary" data-quest-done="${escapeHtml(q.id)}" data-idx="${i}">Usuń</button>
                        </li>`;
                      })
                      .join("")}
                  </ul>
                </div>`
              : ""
          }

          <div class="now-row">
            <span class="step-kicker" style="margin:0">Co robi teraz</span>
            <div class="choice-grid now-actions">
              ${NOW_ACTIONS.map(
                (a) => `
                <button type="button" class="choice ${(p.nowDoing || "czekam") === a.id ? "active" : ""}" data-now="${a.id}" data-idx="${i}">
                  <strong>${a.label}</strong>
                </button>`
              ).join("")}
            </div>
          </div>

          ${guide}

          <details class="tracker-math">
            <summary><strong>Ile kart: traci / zostaje / dobiera</strong></summary>
            <ul class="checklist">
              <li><strong>Teraz na ręce:</strong> ${p.cards ?? 0} (sufit 7; w fazie III cel doboru = <strong>3</strong>)</li>
              <li><strong>Przy +1 reputacji (→ ${nextRep}):</strong> zmęczenie usuwa z gry <strong>${afterFat.lose}</strong> kart(y)
                → ręka orientacyjnie <strong>${afterFat.after}</strong> (jeśli usuwasz z ręki)
                <div>Instrukcja: możesz usuwać z ręki / talii / odrzuconych; potem tasujesz talię. Przykład: 1. trofeum = 1 karta.</div>
              </li>
              <li><strong>Faza III (dobór do 3):</strong> ${escapeHtml(draw.note)}
                <div>Po kroku doboru orientacyjnie: ${draw.afterDraw} na ręce, potem kup 1 z puli.</div>
              </li>
              <li><strong>Start tej pozycji przy stole:</strong> ${start.cards} kart, ${start.gold} złota</li>
            </ul>
            <div class="actions" style="margin-top:0.6rem">
              <button type="button" class="btn btn-primary" data-apply-fatigue data-idx="${i}">
                +1 reputacja i odejmij zmęczenie z ręki (${fat} kart)
              </button>
              <button type="button" class="btn btn-secondary" data-apply-draw7 data-idx="${i}">
                Ustaw rękę jak po doborze fazy III (3)
              </button>
            </div>
          </details>
        </article>
      `;
    })
    .join("");
}

export {
  fatigueForReputation,
  locById,
  LOCATIONS,
  TERRAIN,
  neighborsOf,
  actionHintFor,
  schoolStartId,
  placeAtSchool,
};
