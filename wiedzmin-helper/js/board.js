/**
 * Uproszczona mapa-schemat + helpery trackera przy stole.
 * Nie jest kopią oficjalnej grafiki — tylko nazwy lokacji i przybliżone pozycje.
 */
import { LOCATIONS, ROADS, TERRAIN, locById, fatigueForReputation } from "./map-data.js";
import { ICO, SCHOOL_ICON } from "./icons.js";

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
    monsters: {},
  };
}

export function ensurePlayerTrack(p, startCards = 5, startGold = 3) {
  if (typeof p.reputation !== "number") p.reputation = 0;
  if (typeof p.cards !== "number") p.cards = startCards;
  if (typeof p.gold !== "number") p.gold = startGold;
  if (!p.locationId) p.locationId = null;
  if (!p.nowDoing) p.nowDoing = "czekam";
  if (typeof p.elixirs !== "number") p.elixirs = 0;
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

export function mapSvgHtml(board, roster, { schoolById }) {
  const locMap = Object.fromEntries(LOCATIONS.map((l) => [l.id, l]));
  const roads = ROADS.map(([a, b]) => {
    const A = locMap[a];
    const B = locMap[b];
    if (!A || !B) return "";
    return `<line x1="${A.x}" y1="${A.y}" x2="${B.x}" y2="${B.y}" class="map-road" />`;
  }).join("");

  const rot = board.rotation || 0;
  const labelUpright = -rot;

  const nodes = LOCATIONS.map((loc) => {
    const t = TERRAIN[loc.terrain] || TERRAIN.forest;
    const selected = board.selectedLoc === loc.id ? " map-node-selected" : "";
    const monsters = board.monsters[loc.id];
    const witches = roster
      .map((p, i) => ({ p, i }))
      .filter(({ p }) => p.locationId === loc.id);

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
      <g class="map-node${selected}" data-loc="${loc.id}" style="cursor:pointer">
        <circle cx="${loc.x}" cy="${loc.y}" r="3.4" fill="${t.color}" stroke="#f0e6d2" stroke-width="0.45"/>
        <text x="${loc.x}" y="${loc.y - 4.8}" text-anchor="middle" class="map-label" transform="rotate(${labelUpright} ${loc.x} ${loc.y - 4.8})">${escapeXml(loc.name)}</text>
        ${monsterBadge}
        ${witchBadges}
      </g>`;
  }).join("");

  return `
    <div class="map-stage">
      <div class="map-wrap" style="--map-rot:${rot}deg">
        <svg class="map-svg" viewBox="0 0 100 85" role="img" aria-label="Uproszczona mapa schemat obszarów">
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
    </div>
    <p class="note">Schemat pomocniczy (nazwy z podstawki, pozycje przybliżone). <strong>Nie</strong> jest to kopia oficjalnej mapy z pudełka — służy tylko do oznaczania, gdzie kto stoi i gdzie są potwory.</p>
  `;
}

export function mapToolbarHtml(board, roster) {
  const tools = [
    ["select", "Wybierz"],
    ["monster", `${ICO.monster()} Potwór +`],
    ["monster-", `${ICO.monster()} Potwór −`],
    ["clear-m", "Wyczyść potwory tu"],
    ["player", "Ustaw wiedźmina"],
  ];
  return `
    <div class="map-toolbar">
      ${tools
        .map(
          ([id, label]) =>
            `<button type="button" class="btn ${board.tool === id ? "btn-primary" : "btn-secondary"} map-tool" data-tool="${id}">${label}</button>`
        )
        .join("")}
      <button type="button" class="btn btn-secondary" id="map-rotate-ccw">↺ Obróć</button>
      <button type="button" class="btn btn-secondary" id="map-rotate-cw">Obróć ↻</button>
      <button type="button" class="btn btn-secondary" id="map-rotate-reset">0°</button>
    </div>
    ${
      board.tool === "player"
        ? `<div class="choice-grid" style="margin-top:0.6rem">
            ${roster
              .map(
                (p, i) => `
              <button type="button" class="choice ${board.placePlayer === i ? "active" : ""}" data-place-player="${i}">
                <strong>${i + 1}. ${escapeHtml(p.name)}</strong>
                <span>Kliknij lokację na mapie</span>
              </button>`
              )
              .join("")}
          </div>`
        : ""
    }
  `;
}

function escapeHtml(s) {
  return String(s)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");
}

export function selectedLocPanel(board, roster) {
  const loc = locById(board.selectedLoc);
  if (!loc) {
    return `<p class="lead">Kliknij lokację na mapie, potem użyj narzędzi: potwór / wiedźmin.</p>`;
  }
  const t = TERRAIN[loc.terrain];
  const m = board.monsters[loc.id];
  const here = roster
    .map((p, i) => ({ p, i }))
    .filter(({ p }) => p.locationId === loc.id);

  return `
    <div class="loc-panel">
      <h3>${escapeHtml(loc.name)}</h3>
      <p class="lead" style="margin:0">Teren: <strong>${t.label}</strong>${
        loc.school ? ` · szkoła` : ""
      }</p>
      <p>${ICO.monster()} Potwory tu: <strong>${m ? m.count : 0}</strong>${
        m?.label ? ` (${escapeHtml(m.label)})` : ""
      }</p>
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

export function playerTrackerHtml(roster, { schoolById, activeIndex, START_RESOURCES, players }) {
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
            loc ? escapeHtml(loc.name) : "nie ustawiona — kliknij na mapie (narzędzie „Ustaw wiedźmina”)"
          }</p>

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

export { fatigueForReputation, locById, LOCATIONS, TERRAIN };
