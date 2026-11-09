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
import { ICO, SCHOOL_ICON } from "./icons.js";
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
    /** Poziom ustawiany przyciskiem I/II/III przed Potwór + */
    pendingMonsterLevel: null,
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

export function monsterEncounterHtml(loc, monster) {
  if (!loc || !monster) return "";
  const lvl = monster.level ? ` poziom ${escapeHtml(String(monster.level))}` : "";
  const label = monster.label ? ` — ${escapeHtml(monster.label)}` : "";
  return `
    <div class="guide-box guide-monster-hit" role="alert">
      <p class="step-kicker" style="margin:0">${ICO.monster()} Potwór na polu</p>
      <h3 style="margin:0.25rem 0 0.4rem">Wszedłeś na <strong>${escapeHtml(loc.name)}</strong></h3>
      <p class="lead" style="margin:0">Tu stoi potwór${lvl}${label} (×${monster.count}). Helper prowadzi Cię do <strong>walki</strong>.</p>
      <p class="note" style="margin:0.45rem 0 0">Na stole: nie musisz walczyć w fazie I — ale jak już wszedłeś na żeton, zwykle idziesz w fazę II → walka. Tu skracamy drogę.</p>
    </div>
  `;
}

export function monstersPanelHtml(board) {
  const rows = monstersList(board);
  return `
    <div class="monsters-panel">
      <h3>${ICO.monster()} Potwory na mapie</h3>
      <p class="lead" style="margin:0">Ustaw jak na stole: narzędzie <strong>Potwór +</strong>, kliknij lokację z żetonu obszaru. Po wejściu gracza na to pole helper odpali walkę.</p>
      <div class="actions" style="margin:0.55rem 0">
        <button type="button" class="btn btn-secondary ${board.pendingMonsterLevel === "I" ? "btn-primary" : ""}" data-monster-level="I">Poz. I</button>
        <button type="button" class="btn btn-secondary ${board.pendingMonsterLevel === "II" ? "btn-primary" : ""}" data-monster-level="II">Poz. II</button>
        <button type="button" class="btn btn-secondary ${board.pendingMonsterLevel === "III" ? "btn-primary" : ""}" data-monster-level="III">Poz. III</button>
      </div>
      <p class="note">Aktywny poziom: <strong>${escapeHtml(board.pendingMonsterLevel || "—")}</strong> → narzędzie mapy przełącza się na <em>Potwór +</em>, potem kliknij lokację.</p>
      ${
        rows.length
          ? `<ul class="checklist monsters-list">
              ${rows
                .map(
                  (r) => `
                <li>
                  <strong>${escapeHtml(r.loc.name)}</strong> (#${r.loc.number})
                  — ×${r.count}${r.level ? ` · poz. ${escapeHtml(String(r.level))}` : ""}${
                    r.label ? ` · ${escapeHtml(r.label)}` : ""
                  }
                  <button type="button" class="btn btn-secondary" data-monster-clear="${r.id}" style="margin-left:0.35rem">Usuń</button>
                </li>`
                )
                .join("")}
            </ul>`
          : `<p class="warn">Brak potworów na schemacie — dodaj je po setupie (3 żetony na start).</p>`
      }
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

export function mapSvgHtml(board, roster, { schoolById, highlightFromId } = {}) {
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
    <p class="note">Schemat wg zdjęć planszy: numery 0–18, tereny (pierścień = las/góry/woda) i drogi. Szkoły = złoty punkt. <strong>Nie</strong> jest to kopia grafiki z pudełka.</p>
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

function terrainIcon(id) {
  if (id === "forest") return ICO.forest();
  if (id === "mountain") return ICO.mountain();
  if (id === "water") return ICO.water();
  return "";
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
  const hint = actionHintFor(loc);
  const neigh = neighborsOf(loc.id)
    .map((id) => locById(id))
    .filter(Boolean)
    .map((n) => `${n.name}${typeof n.number === "number" ? ` (${n.number})` : ""}`)
    .join(", ");

  return `
    <div class="loc-panel">
      <h3>${escapeHtml(loc.name)}${
        typeof loc.number === "number" ? ` <span class="loc-num">#${loc.number}</span>` : ""
      }</h3>
      <p class="lead" style="margin:0">${terrainIcon(loc.terrain)} Teren ruchu: <strong>${
        t.label
      }</strong>${loc.school ? ` · szkoła wiedźmińska` : ""}</p>
      ${hint ? `<p class="loc-action"><strong>Znak na planszy:</strong> ${escapeHtml(hint)}</p>` : ""}
      <p class="note" style="margin:0.4rem 0 0">Sąsiedzi (ścieżki): ${
        neigh ? escapeHtml(neigh) : "—"
      }</p>
      <p class="tip" style="margin:0.5rem 0 0">Po ruchu: przycisk „Faza I” / „Eksploracja” w trackerze pokaże kroki za rękę wg tego znaku.</p>
      <p>${ICO.monster()} Potwory tu: <strong>${m ? m.count : 0}</strong>${
        m?.level ? ` · poz. ${escapeHtml(String(m.level))}` : ""
      }${m?.label ? ` (${escapeHtml(m.label)})` : ""}${
        m?.count
          ? ` — <em>wejście gracza = start walki w helperze</em>`
          : ""
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
            loc ? escapeHtml(loc.name) : "nie ustawiona — kliknij na mapie (narzędzie „Ustaw wiedźmina”)"
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
