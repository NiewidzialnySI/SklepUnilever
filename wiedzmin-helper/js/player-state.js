/**
 * Atrybuty, tropy, medytacja, startowe zasoby.
 */
import { ICO } from "./icons.js";
import { locById, TERRAIN } from "./map-data.js";
import { START_RESOURCES } from "./content.js";

export const ATTR_KEYS = [
  { id: "attack", label: "Atak" },
  { id: "defense", label: "Obrona" },
  { id: "alchemy", label: "Alchemia" },
  { id: "specialty", label: "Specjalność" },
];

export const TRAIL_TERRAINS = ["forest", "mountain", "water"];

export function ensureCombatTrack(p) {
  if (!p.attrs || typeof p.attrs !== "object") {
    p.attrs = { attack: 1, defense: 1, alchemy: 1, specialty: 1, level: 1 };
  }
  for (const k of ["attack", "defense", "alchemy", "specialty", "level"]) {
    if (typeof p.attrs[k] !== "number") p.attrs[k] = 1;
    p.attrs[k] = Math.max(1, Math.min(5, p.attrs[k]));
  }
  if (typeof p.shield !== "number") p.shield = p.attrs.defense;
  if (!Array.isArray(p.attrTrophies)) p.attrTrophies = [];
  if (!p.trails || typeof p.trails !== "object") {
    p.trails = { forest: null, mountain: null, water: null };
  }
  for (const t of TRAIL_TERRAINS) {
    if (p.trails[t] === undefined) p.trails[t] = null;
  }
  if (typeof p.resourcesApplied !== "boolean") p.resourcesApplied = false;
  return p;
}

export function applyStartResources(roster, players, { force = false } = {}) {
  const n = players;
  (roster || []).forEach((p, i) => {
    ensureCombatTrack(p);
    const start = START_RESOURCES[n]?.[i] || { cards: 5, gold: 3 };
    if (force || !p.resourcesApplied) {
      p.cards = start.cards;
      p.gold = start.gold;
      p.resourcesApplied = true;
    }
  });
}

export function startResourcesFor(players, index) {
  return START_RESOURCES[players]?.[index] || { cards: 5, gold: 3 };
}

export function meditationStatus(player, { isSolo = false } = {}) {
  ensureCombatTrack(player);
  const readyAttrs = ATTR_KEYS.filter((a) => (player.attrs[a.id] || 1) >= 5).map((a) => a.id);
  const owned = new Set(player.attrTrophies || []);
  const available = readyAttrs.filter((id) => !owned.has(id));
  const can = available.length > 0;
  return {
    can,
    readyAttrs,
    available,
    owned: [...owned],
    reason: can
      ? `Możesz medytować: ${available.map((id) => ATTR_KEYS.find((a) => a.id === id)?.label || id).join(", ")} na V.`
      : readyAttrs.length
        ? "Masz atrybut na V, ale to trofeum już masz — medytacja zablokowana."
        : "Medytacja zablokowana: żaden atrybut nie jest na poziomie V.",
    soloNote: isSolo
      ? "Solo: weź odkryte trofeum atrybutu z przygotowania, zanim pokonasz 3. potwora."
      : "Multi: trofeum ze stosu atrybutów. 4. reputacja z medytacji nie kończy gry.",
  };
}

export function trailsHtml(player, { editable = false } = {}) {
  ensureCombatTrack(player);
  const rows = TRAIL_TERRAINS.map((t) => {
    const slot = player.trails[t];
    const label = TERRAIN[t]?.label || t;
    const loc = slot?.locId ? locById(slot.locId) : null;
    const status = !slot
      ? "brak"
      : slot.kind === "quest"
        ? `zadanie → ${loc ? loc.name : "?"} (+${slot.goldOnToken ?? 1} zł na żetonie)`
        : `trop gotowy${loc ? ` (z ${loc.name})` : ""}`;
    if (!editable) {
      return `<li><strong>${label}:</strong> ${status}</li>`;
    }
    return `
      <li data-trail-terrain="${t}">
        <strong>${label}:</strong> ${status}
        <div class="actions" style="margin-top:0.35rem">
          <button type="button" class="btn btn-secondary" data-trail-set="${t}" data-kind="quest">Zadanie (nr lokacji)</button>
          <button type="button" class="btn btn-secondary" data-trail-set="${t}" data-kind="trail">Mam trop</button>
          <button type="button" class="btn btn-secondary" data-trail-clear="${t}">Wyczyść</button>
        </div>
      </li>`;
  }).join("");
  return `
    <div class="guide-box">
      <p class="step-kicker" style="margin:0">${ICO.quest()} Żetony tropu / zadania tropienia</p>
      <p class="lead" style="margin:0.3rem 0 0">Max 1 na teren (las / góry / woda). Pusty stos żetonów = nie bierzesz nowego zadania. Karta „weź żeton tropu” przy braku żetonów: ten fragment efektu przepada — resztę karty stosujesz.</p>
      <ul class="checklist">${rows}</ul>
    </div>`;
}

export function attrsAdjustHtml(player) {
  ensureCombatTrack(player);
  const a = player.attrs;
  return `
    <div class="guide-box resource-adjust">
      <p class="step-kicker" style="margin:0">Planszetka — atrybuty</p>
      <p class="lead" style="margin:0.3rem 0 0">Poziom <strong>${a.level}</strong> · tarcza <strong>${player.shield ?? a.defense}</strong> (max = obrona)</p>
      <div class="tracker-grid" style="margin-top:0.65rem">
        ${ATTR_KEYS.map(
          (k) => `
          <label>${k.label}
            <div class="num-row">
              <button type="button" class="btn btn-secondary" data-attr="${k.id}" data-delta="-1">−</button>
              <strong class="num-val">${a[k.id]}</strong>
              <button type="button" class="btn btn-secondary" data-attr="${k.id}" data-delta="1">+</button>
            </div>
          </label>`
        ).join("")}
        <label>Poziom
          <div class="num-row">
            <button type="button" class="btn btn-secondary" data-attr="level" data-delta="-1">−</button>
            <strong class="num-val">${a.level}</strong>
            <button type="button" class="btn btn-secondary" data-attr="level" data-delta="1">+</button>
          </div>
        </label>
        <label>Tarcza
          <div class="num-row">
            <button type="button" class="btn btn-secondary" data-shield-delta="-1">−</button>
            <strong class="num-val">${player.shield ?? a.defense}</strong>
            <button type="button" class="btn btn-secondary" data-shield-delta="1">+</button>
          </div>
        </label>
      </div>
    </div>`;
}

export function hasTrailForMonster(player, monsterLoc) {
  ensureCombatTrack(player);
  if (!monsterLoc) return false;
  const terrain = monsterLoc.terrain;
  const slot = player.trails?.[terrain];
  return Boolean(slot && slot.kind === "trail");
}

export function escapeHtmlLite(s) {
  return String(s ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}
