import {
  START_RESOURCES,
  PLAYER_LABELS,
  buildSetupSteps,
  phase1AfterMove,
  phase2Choices,
  fightBranchHtml,
  fightMonster,
  FIGHT_WITCHER,
  meditate,
  phase3,
  LOCATION_ACTIONS,
  quickRef,
} from "./content.js";
import { SCHOOLS, sourcesHtml } from "./sources.js";
import { lexiconHtml, SCHOOL_SPECIALTIES } from "./lexicon.js";
import { ICO, SCHOOL_ICON, iconsLegendHtml } from "./icons.js";
import {
  defaultBoard,
  ensurePlayerTrack,
  mapSvgHtml,
  mapToolbarHtml,
  selectedLocPanel,
  playerTrackerHtml,
  movePromptHtml,
  bindMapLocPopup,
  fatigueForReputation,
  phase3DrawHint,
  locById,
  neighborsOf,
  placeAtSchool,
  schoolStartId,
  monsterAt,
  monsterEncounterHtml,
  monstersPanelHtml,
  LOCATIONS,
} from "./board.js";
import {
  neighborPickerHtml,
  moveCostChooserHtml,
  applyMoveCost,
  locationActionGuideHtml,
  exploreGuideHtml,
  destinationPreviewHtml,
  mapLocPopupHtml,
} from "./guide.js";
import {
  exploreDeckChooserHtml,
  exploreResolveHtml,
  resourceAdjustHtml,
  questArrivalHtml,
  turnSummaryHtml,
  ensureQuests,
  newQuestId,
} from "./quests.js";
import {
  ensureCombatTrack,
  applyStartResources,
  startResourcesFor,
  meditationStatus,
  phase2FightOptions,
  trailsHtml,
  attrsAdjustHtml,
  hasTrailForMonster,
  ATTR_KEYS,
} from "./player-state.js";

const STORAGE_KEY = "wiedzmin-stary-swiat-helper-v4";
const UI_SCALE_KEY = "wiedzmin-stary-swiat-ui-scale";
const UI_SCALES = ["sm", "md", "lg"];

const appEl = document.getElementById("app");

function loadUiScale() {
  try {
    let raw = localStorage.getItem(UI_SCALE_KEY) || "md";
    if (raw === "xl") raw = "lg";
    return UI_SCALES.includes(raw) ? raw : "md";
  } catch {
    return "md";
  }
}

function applyUiScale(scale) {
  let next = scale === "xl" ? "lg" : scale;
  next = UI_SCALES.includes(next) ? next : "md";
  document.documentElement.setAttribute("data-ui-scale", next);
  try {
    localStorage.setItem(UI_SCALE_KEY, next);
  } catch {
    /* ignore */
  }
  return next;
}

applyUiScale(loadUiScale());

if (!window.__wiedzminEnterBound) {
  window.__wiedzminEnterBound = true;
  document.addEventListener("keydown", (e) => {
    if (e.key !== "Enter" || e.shiftKey || e.ctrlKey || e.metaKey || e.altKey) return;
    if (e.target && e.target.closest && e.target.closest("textarea")) return;
    if (e.target && e.target.matches && e.target.matches("button, [contenteditable=true]")) return;
    const primary =
      appEl?.querySelector(".btn-primary:not([disabled])") ||
      appEl?.querySelector("#next:not([disabled]), #to-roster, #to-setup, #to-turn, #confirm-order");
    if (!primary) return;
    e.preventDefault();
    primary.click();
  });
}

function defaultState() {
  return {
    view: "home",
    players: 3,
    roster: [],
    activeIndex: 0,
    setupIndex: 0,
    turn: { phase: "hub", screen: "hub", moveTo: null, moveCost: null, exploreDeck: null },
    board: defaultBoard(),
    orderDice: [],
  };
}

function isSolo() {
  return state.players === 1;
}

function normalizeState(raw) {
  const base = defaultState();
  if (!raw || typeof raw !== "object") return base;
  const players = Math.min(5, Math.max(1, Number(raw.players) || 3));
  const turn =
    raw.turn && typeof raw.turn === "object"
      ? {
          phase: String(raw.turn.phase || "hub"),
          screen: String(raw.turn.screen || "hub"),
          moveTo: raw.turn.moveTo ? String(raw.turn.moveTo) : null,
          moveCost: raw.turn.moveCost ? String(raw.turn.moveCost) : null,
          exploreDeck: raw.turn.exploreDeck ? String(raw.turn.exploreDeck) : null,
        }
      : base.turn;
  const board =
    raw.board && typeof raw.board === "object"
      ? {
          ...defaultBoard(),
          ...raw.board,
          monsters: raw.board.monsters || {},
          movePrompt: raw.board.movePrompt || null,
          highlightFrom: raw.board.highlightFrom || null,
        }
      : defaultBoard();
  // migracja Oxenfurt → Loc Ichaer
  if (board.monsters?.oxenfurt) {
    board.monsters["loc-ichaer"] = board.monsters.oxenfurt;
    delete board.monsters.oxenfurt;
  }
  if (board.selectedLoc === "oxenfurt") board.selectedLoc = "loc-ichaer";
  const roster = Array.isArray(raw.roster)
    ? raw.roster.map((p) => {
        const copy = { ...(p || {}) };
        if (copy.locationId === "oxenfurt") copy.locationId = "loc-ichaer";
        if (copy.locationId && !locByIdSafe(copy.locationId)) copy.locationId = null;
        return copy;
      })
    : [];
  const views = new Set([
    "home",
    "roster",
    "order",
    "setup",
    "board",
    "turn",
    "ref",
    "icons",
    "lexicon",
    "sources",
  ]);
  roster.forEach((p) => ensureCombatTrack(ensurePlayerTrack(p)));
  return {
    ...base,
    view: views.has(raw.view) ? raw.view : "home",
    players,
    roster,
    activeIndex: Math.max(0, Number(raw.activeIndex) || 0),
    setupIndex: Math.max(0, Number(raw.setupIndex) || 0),
    turn,
    board,
    orderDice: Array.isArray(raw.orderDice) ? raw.orderDice.map(Number) : [],
  };
}

function locByIdSafe(id) {
  return Boolean(locById(id));
}

function loadState() {
  try {
    const raw = JSON.parse(localStorage.getItem(STORAGE_KEY) || "null");
    return normalizeState(raw);
  } catch {
    return defaultState();
  }
}

let saveTimer = null;
function saveState() {
  clearTimeout(saveTimer);
  saveTimer = setTimeout(() => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
    } catch {
      /* quota / private mode */
    }
  }, 200);
}

const state = loadState();

function ensureRoster() {
  const n = state.players;
  if (!Array.isArray(state.roster)) state.roster = [];
  if (!state.board) state.board = defaultBoard();
  while (state.roster.length < n) {
    const i = state.roster.length;
    const start = START_RESOURCES[n]?.[i] || { cards: 5, gold: 3 };
    const p = ensurePlayerTrack(
      {
        name: n === 1 ? "Ja" : `Gracz ${i + 1}`,
        schoolId: SCHOOLS[i % SCHOOLS.length].id,
      },
      start.cards,
      start.gold
    );
    placeAtSchool(p, { force: true });
    p._prevSchoolId = p.schoolId;
    state.roster.push(p);
  }
  if (state.roster.length > n) state.roster = state.roster.slice(0, n);
  state.roster.forEach((p, i) => {
    const start = START_RESOURCES[n]?.[i] || { cards: 5, gold: 3 };
    ensurePlayerTrack(p, start.cards, start.gold);
    ensureCombatTrack(p);
    if (!p.locationId) {
      placeAtSchool(p, { force: true });
      p._prevSchoolId = p.schoolId;
    }
  });
  if (state.activeIndex >= n) state.activeIndex = 0;
  if (state.board.placePlayer >= n) state.board.placePlayer = 0;
}

/** Po wyborze szkoły — pionek na startowej lokacji szkoły. */
function applySchoolStarts() {
  state.roster.forEach((p) => {
    const schoolChanged = p._prevSchoolId && p._prevSchoolId !== p.schoolId;
    if (!p.locationId || schoolChanged) {
      placeAtSchool(p, { force: true });
    }
    p._prevSchoolId = p.schoolId;
  });
}

function schoolById(id) {
  return SCHOOLS.find((s) => s.id === id) || SCHOOLS[0];
}

function activePlayer() {
  ensureRoster();
  return state.roster[state.activeIndex] || state.roster[0];
}

function playerLabel(p) {
  const school = schoolById(p.schoolId);
  return `${p.name} · ${school.short}`;
}

function setPlayers(n) {
  if (n < state.roster.length) {
    if (
      !window.confirm(
        `Zmniejszenie do ${n} graczy usunie dane końcówki składu. Kontynuować?`
      )
    ) {
      return;
    }
  }
  state.players = n;
  state.setupIndex = 0;
  state.activeIndex = 0;
  ensureRoster();
  saveState();
  render();
}

function go(view, extras = {}) {
  state.view = view;
  Object.assign(state, extras);
  saveState();
  render();
  window.scrollTo({ top: 0, behavior: "smooth" });
}

function render() {
  ensureRoster();
  if (state.view === "home") renderHome();
  else if (state.view === "roster") renderRoster();
  else if (state.view === "order") renderOrder();
  else if (state.view === "setup") renderSetup();
  else if (state.view === "board") renderBoard();
  else if (state.view === "turn") renderTurn();
  else if (state.view === "ref") renderRef();
  else if (state.view === "icons") renderIcons();
  else if (state.view === "lexicon") renderLexicon();
  else if (state.view === "sources") renderSources();
  else renderHome();
}

function schoolIcon(schoolId) {
  const fn = SCHOOL_ICON[schoolId] || ICO.specialty;
  return fn();
}

function playersLabel(n) {
  if (n === 1) return "Solo";
  return `${n} graczy`;
}

function shell(inner) {
  const scale = loadUiScale();
  appEl.innerHTML = `
    <header class="topbar">
      <div class="brand">
        Wiedźmin: Stary Świat
        ${isSolo() ? `<small>Solo</small>` : ""}
      </div>
      <div class="top-actions">
        <div class="ui-scale" role="group" aria-label="Rozmiar tekstu">
          ${[
            ["sm", "A−", "Mniejszy"],
            ["md", "A", "Domyślny"],
            ["lg", "A+", "Większy"],
          ]
            .map(
              ([id, label, title]) =>
                `<button type="button" class="btn btn-ghost ${scale === id ? "nav-active" : ""}" data-ui-scale="${id}" title="${title}">${label}</button>`
            )
            .join("")}
        </div>
        ${[
          ["home", "Start"],
          ["roster", "Gracze"],
          ["order", "Kolejność"],
          ["setup", "Przygotowanie"],
          ["board", "Stół"],
          ["turn", "Tura"],
          ["ref", "Ściąga"],
          ["icons", "Ikony"],
          ["lexicon", "Leksykon"],
          ["sources", "Źródła"],
        ]
          .map(
            ([id, label]) =>
              `<button class="btn btn-ghost ${state.view === id ? "nav-active" : ""}" data-nav="${id}">${label}</button>`
          )
          .join("")}
      </div>
    </header>
    ${inner}
    <p class="footer-note">Fanowski przewodnik (instrukcja + FAQ Rebel). Nie jest produktem CD PROJEKT RED / Go On Board.</p>
  `;

  appEl.querySelectorAll("[data-ui-scale]").forEach((btn) => {
    btn.addEventListener("click", () => {
      applyUiScale(btn.getAttribute("data-ui-scale"));
      render();
    });
  });

  appEl.querySelectorAll("[data-nav]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const view = btn.getAttribute("data-nav");
      if (view === "turn") {
        // Nie resetuj mid-fazy — wróć do bieżącego ekranu tury
        if (!state.turn || typeof state.turn !== "object") {
          state.turn = { phase: "hub", screen: "hub" };
        }
        go("turn");
      } else go(view);
    });
  });
}

function renderHome() {
  const options = [
    { n: 1, title: "Solo", hint: "1 osoba" },
    { n: 2, title: "2 graczy", hint: "Szybka partia" },
    { n: 3, title: "3 graczy", hint: "Polecane na start" },
    { n: 4, title: "4 graczy", hint: "Po poznaniu zasad" },
    { n: 5, title: "5 graczy", hint: "Pełny stół" },
  ];

  shell(`
    <section class="panel home-hero">
      <p class="step-kicker">Pomocnik przy stole</p>
      <h1>Prowadzę Cię przez grę</h1>
      <p class="lead">Wybierz liczbę graczy, potem imiona i szkoły — dalej prowadzę przygotowanie i tury.</p>
      <h3>Ile osób gra?</h3>
      <div class="choice-grid">
        ${options
          .map(
            (o) => `
          <button class="choice ${state.players === o.n ? "active" : ""}" data-players="${o.n}">
            <strong>${o.title}</strong>
            <span>${o.hint}</span>
          </button>`
          )
          .join("")}
      </div>
      <div class="actions">
        <button class="btn btn-primary" id="to-roster">Dalej</button>
      </div>
    </section>
  `);

  appEl.querySelectorAll("[data-players]").forEach((btn) => {
    btn.addEventListener("click", () => setPlayers(Number(btn.dataset.players)));
  });
  appEl.querySelector("#to-roster").addEventListener("click", () => go("roster"));
}

function renderRoster() {
  ensureRoster();
  const used = new Set();

  shell(`
    <section class="panel">
      <p class="step-kicker">Skład stołu · ${playersLabel(state.players)}</p>
      <h1>Imiona i szkoły</h1>
      <p class="lead">Wpisz imię i wybierz szkołę — <strong>pionek od razu staje na szkole startowej</strong> (Wilk→Kaer Morhen, Gryf→Kaer Seren, Niedźwiedź→Haern Caduch, Kot→Stygga, Żmija→Gorthur Gvaed).</p>
      <div id="roster-forms">
        ${state.roster
          .map((p, i) => {
            const orderHint = state.players === 1 ? "Ty" : `Gracz ${i + 1}`;
            return `
            <div class="roster-card" data-idx="${i}">
              <strong>${orderHint}</strong>
              <label>Imię
                <input type="text" data-field="name" value="${escapeAttr(p.name)}" maxlength="24" />
              </label>
              <label>Szkoła
                <select data-field="schoolId">
                  ${SCHOOLS.map(
                    (s) =>
                      `<option value="${s.id}" ${p.schoolId === s.id ? "selected" : ""}>${s.name}</option>`
                  ).join("")}
                </select>
              </label>
              <p class="note school-start-hint">Start: <strong>${escapeHtml(
                locById(schoolStartId(p.schoolId))?.name || "—"
              )}</strong></p>
            </div>`;
          })
          .join("")}
      </div>
      <p class="warn" id="roster-warn" hidden>Każda szkoła może być tylko raz (w multi). Popraw wybór.</p>
      <div class="actions">
        <button class="btn btn-secondary" id="back-home">Wstecz</button>
        <button class="btn btn-primary" id="to-order">${isSolo() ? "Dalej — przygotowanie" : "Dalej — kolejność"}</button>
      </div>
    </section>
  `);

  const warn = appEl.querySelector("#roster-warn");

  function syncFromDom() {
    appEl.querySelectorAll(".roster-card").forEach((card) => {
      const i = Number(card.dataset.idx);
      const name =
        card
          .querySelector('[data-field="name"]')
          .value.replace(/[<>]/g, "")
          .trim() || `Gracz ${i + 1}`;
      const schoolId = card.querySelector('[data-field="schoolId"]').value;
      const prev = state.roster[i] || {};
      const start = START_RESOURCES[state.players]?.[i] || { cards: 5, gold: 3 };
      const next = ensurePlayerTrack(
        { ...prev, name, schoolId, _prevSchoolId: prev.schoolId || prev._prevSchoolId },
        start.cards,
        start.gold
      );
      ensureCombatTrack(next);
      if (!prev.locationId || prev.schoolId !== schoolId) {
        placeAtSchool(next, { force: true });
      }
      next._prevSchoolId = schoolId;
      state.roster[i] = next;
      const hintStrong = card.querySelector(".school-start-hint strong");
      if (hintStrong) {
        hintStrong.textContent = locById(schoolStartId(schoolId))?.name || "—";
      }
    });
    used.clear();
    let dup = false;
    if (!isSolo()) {
      for (const p of state.roster) {
        if (used.has(p.schoolId)) dup = true;
        used.add(p.schoolId);
      }
    }
    warn.hidden = !dup;
    applySchoolStarts();
    saveState();
    return !dup;
  }

  appEl.querySelectorAll(".roster-card input, .roster-card select").forEach((el) => {
    el.addEventListener("change", syncFromDom);
    el.addEventListener("input", syncFromDom);
  });

  appEl.querySelector("#back-home").addEventListener("click", () => go("home"));
  appEl.querySelector("#to-order").addEventListener("click", () => {
    if (!syncFromDom()) return;
    applySchoolStarts();
    if (isSolo()) {
      applyStartResources(state.roster, state.players, { force: true });
      state.setupIndex = 0;
      go("setup");
    } else {
      state.orderDice = [];
      go("order");
    }
  });
}

function renderOrder() {
  ensureRoster();
  if (isSolo()) {
    applyStartResources(state.roster, state.players, { force: true });
    state.setupIndex = 0;
    go("setup");
    return;
  }

  const diceDone = (state.orderDice || []).length;
  const remaining = state.roster.length - diceDone;

  shell(`
    <section class="panel">
      <p class="step-kicker">Kolejność przy stole</p>
      <h1>Kto pierwszy?</h1>
      <p class="lead">Kolejność ustala startowe <strong>karty i złoto</strong> — ustaw ją teraz, zanim rozdamy zasoby. Domyślnie: jak wpisywaliście. Przeciągnij kafelki albo rzuć kością.</p>
      <ul class="order-list" id="order-list">
        ${state.roster
          .map((p, i) => {
            const res = startResourcesFor(state.players, i);
            return `
            <li class="order-item" draggable="true" data-idx="${i}">
              <span class="order-pos">${i + 1}.</span>
              <span class="player-chip">${schoolIcon(p.schoolId)}${escapeHtml(p.name)} <span>${schoolById(p.schoolId).short}</span></span>
              <span class="tip" style="margin:0">→ ${res.cards} kart · ${res.gold} złota</span>
              <button type="button" class="btn btn-secondary" data-order-up="${i}" ${i === 0 ? "disabled" : ""}>↑</button>
              <button type="button" class="btn btn-secondary" data-order-down="${i}" ${i === state.roster.length - 1 ? "disabled" : ""}>↓</button>
            </li>`;
          })
          .join("")}
      </ul>
      <div class="actions">
        <button type="button" class="btn btn-secondary" id="order-dice" ${remaining <= 1 ? "disabled" : ""}>
          ${ICO.dice()} Kość — wylosuj ${diceDone === 0 ? "1. gracza" : diceDone === 1 ? "2. gracza" : "kolejnego"}
        </button>
        <button type="button" class="btn btn-secondary" id="order-reset">Reset kolejności / kości</button>
      </div>
      <p class="note">Po 1. i 2. rzucie reszta ustawia się sama (przy 3 graczach). Przy więcej — rzucaj dalej albo przeciągnij.</p>
      <div class="actions">
        <button class="btn btn-secondary" id="back-roster">Wstecz</button>
        <button class="btn btn-primary" id="confirm-order">Zatwierdź i rozdaj karty/złoto</button>
      </div>
    </section>
  `);

  const list = appEl.querySelector("#order-list");
  let dragFrom = null;
  list.querySelectorAll(".order-item").forEach((item) => {
    item.addEventListener("dragstart", () => {
      dragFrom = Number(item.dataset.idx);
      item.classList.add("dragging");
    });
    item.addEventListener("dragend", () => item.classList.remove("dragging"));
    item.addEventListener("dragover", (e) => e.preventDefault());
    item.addEventListener("drop", (e) => {
      e.preventDefault();
      const to = Number(item.dataset.idx);
      if (dragFrom === null || dragFrom === to) return;
      const next = [...state.roster];
      const [moved] = next.splice(dragFrom, 1);
      next.splice(to, 0, moved);
      state.roster = next;
      state.orderDice = [];
      saveState();
      render();
    });
  });

  appEl.querySelectorAll("[data-order-up]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const i = Number(btn.getAttribute("data-order-up"));
      if (i <= 0) return;
      const next = [...state.roster];
      [next[i - 1], next[i]] = [next[i], next[i - 1]];
      state.roster = next;
      state.orderDice = [];
      saveState();
      render();
    });
  });
  appEl.querySelectorAll("[data-order-down]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const i = Number(btn.getAttribute("data-order-down"));
      if (i >= state.roster.length - 1) return;
      const next = [...state.roster];
      [next[i], next[i + 1]] = [next[i + 1], next[i]];
      state.roster = next;
      state.orderDice = [];
      saveState();
      render();
    });
  });

  appEl.querySelector("#order-dice")?.addEventListener("click", () => {
    const pool = state.roster
      .map((_, i) => i)
      .filter((i) => !(state.orderDice || []).includes(i));
    if (pool.length <= 1) return;
    const pick = pool[Math.floor(Math.random() * pool.length)];
    const assigned = [...(state.orderDice || []), pick];
    state.orderDice = assigned;
    // Rebuild order: assigned first, then remaining in original relative order
    const rest = state.roster
      .map((p, i) => ({ p, i }))
      .filter(({ i }) => !assigned.includes(i))
      .map(({ p }) => p);
    const head = assigned.map((i) => state.roster[i]).filter(Boolean);
    // When only one left after 2 picks for 3p, rest auto-fills
    state.roster = [...head, ...rest];
    // Remap dice indices to new positions 0..k-1
    state.orderDice = head.map((_, i) => i);
    saveState();
    render();
  });

  appEl.querySelector("#order-reset")?.addEventListener("click", () => {
    state.orderDice = [];
    saveState();
    render();
  });

  appEl.querySelector("#back-roster").addEventListener("click", () => go("roster"));
  appEl.querySelector("#confirm-order").addEventListener("click", () => {
    applyStartResources(state.roster, state.players, { force: true });
    state.activeIndex = 0;
    state.setupIndex = 0;
    state.orderDice = [];
    saveState();
    go("setup");
  });
}

function escapeAttr(s) {
  return String(s)
    .replace(/&/g, "&amp;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");
}

function renderSetup() {
  const steps = buildSetupSteps(state.players, state.roster);
  const index = Math.min(state.setupIndex, steps.length - 1);
  const step = steps[index];
  const pct = Math.round(((index + 1) / steps.length) * 100);
  const rosterHint = state.roster
    .map((p, i) => {
      const res = startResourcesFor(state.players, i);
      return `<span class="player-chip">${schoolIcon(p.schoolId)}${escapeHtml(p.name)} <span>${schoolById(p.schoolId).short}</span> · ${res.cards}k/${res.gold}zł</span>`;
    })
    .join(" ");

  // Przy krokach gracza / solo wiedźmina — dopisz karty i złoto do trackera
  if (String(step.id || "").startsWith("player") || step.id === "player-solo") {
    applyStartResources(state.roster, state.players, { force: false });
    const m = String(step.id).match(/player-(\d+)/);
    if (m) {
      const idx = Number(m[1]);
      const start = startResourcesFor(state.players, idx);
      if (state.roster[idx]) {
        state.roster[idx].cards = start.cards;
        state.roster[idx].gold = start.gold;
        state.roster[idx].resourcesApplied = true;
      }
    }
    if (step.id === "player-solo" && state.roster[0]) {
      state.roster[0].cards = 5;
      state.roster[0].gold = 3;
      state.roster[0].resourcesApplied = true;
    }
  }

  const monsterMap =
    step.id === "monsters"
      ? `
      <div class="setup-map-embed">
        <h3>Rozstaw potwory na mapie</h3>
        <p class="lead">Wybierz poziom, potem kliknij lokację (Potwór +). Widać, gdzie stoi każdy żeton.</p>
        ${mapToolbarHtml(state.board, state.roster)}
        <div class="choice-grid" style="margin:0.5rem 0">
          ${[1, 2, 3]
            .map(
              (lv) =>
                `<button type="button" class="choice ${state.board.pendingMonsterLevel === lv ? "active" : ""}" data-mlevel="${lv}"><strong>Poziom ${lv}</strong><span>Następny potwór</span></button>`
            )
            .join("")}
        </div>
        ${mapSvgHtml(state.board, state.roster, { schoolById })}
        ${monstersPanelHtml(state.board)}
      </div>`
      : "";

  shell(`
    <section class="panel">
      <div class="progress">
        <div class="progress-meta">
          <span>Przygotowanie · ${playersLabel(state.players)}</span>
          <span>Krok ${index + 1} / ${steps.length}</span>
        </div>
        <div class="progress-bar"><span style="width:${pct}%"></span></div>
      </div>
      <div class="badge-row" style="margin-bottom:0.8rem">${rosterHint}</div>
      <p class="step-kicker">Krok ${index + 1}</p>
      <h1>${step.title}</h1>
      <div class="step-body">${step.body}</div>
      ${monsterMap}
      <div class="actions">
        <button class="btn btn-secondary" id="prev" ${index === 0 ? "disabled" : ""}>Wstecz</button>
        ${
          step.nextMode === "turn"
            ? `<button class="btn btn-primary" id="to-turn">Prowadź pierwszą turę</button>`
            : `<button class="btn btn-primary" id="next">Zrobione — dalej</button>`
        }
      </div>
    </section>
  `);

  if (step.id === "monsters") {
    state.board.tool = state.board.tool || "monster";
    appEl.querySelectorAll("[data-mlevel]").forEach((btn) => {
      btn.addEventListener("click", () => {
        state.board.pendingMonsterLevel = Number(btn.getAttribute("data-mlevel"));
        state.board.tool = "monster";
        saveState();
        render();
      });
    });
    appEl.querySelectorAll(".map-tool").forEach((btn) => {
      btn.addEventListener("click", () => {
        state.board.tool = btn.getAttribute("data-tool");
        saveState();
        render();
      });
    });
    appEl.querySelectorAll(".map-node").forEach((g) => {
      g.addEventListener("click", () => {
        const locId = g.getAttribute("data-loc");
        const tool = state.board.tool || "monster";
        state.board.selectedLoc = locId;
        if (tool === "monster" || tool === "select") {
          const cur = state.board.monsters[locId] || { count: 0 };
          const level = state.board.pendingMonsterLevel || cur.level || 1;
          state.board.monsters[locId] = {
            count: (cur.count || 0) + 1,
            level,
            label: `Poz. ${level}`,
          };
          state.board.tool = "monster";
        } else if (tool === "monster-") {
          const cur = state.board.monsters[locId];
          if (cur) {
            cur.count = Math.max(0, (cur.count || 0) - 1);
            if (cur.count === 0) delete state.board.monsters[locId];
          }
        } else if (tool === "clear-m") {
          delete state.board.monsters[locId];
        }
        saveState();
        render();
      });
    });
  }

  const prev = appEl.querySelector("#prev");
  const next = appEl.querySelector("#next");
  const toTurn = appEl.querySelector("#to-turn");
  if (prev) {
    prev.addEventListener("click", () => {
      state.setupIndex = Math.max(0, index - 1);
      saveState();
      render();
      window.scrollTo({ top: 0, behavior: "smooth" });
    });
  }
  if (next) {
    next.addEventListener("click", () => {
      state.setupIndex = Math.min(steps.length - 1, index + 1);
      saveState();
      render();
      window.scrollTo({ top: 0, behavior: "smooth" });
    });
  }
  if (toTurn) {
    toTurn.addEventListener("click", () => {
      applyStartResources(state.roster, state.players, { force: true });
      state.activeIndex = 0;
      go("turn", { turn: { phase: "hub", screen: "hub" } });
    });
  }
}

function escapeHtml(s) {
  return String(s)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");
}

function turnShell(title, body) {
  const tabs = [
    ["hub", `${ICO.trophy()} Start`],
    ["I", `${ICO.phase1()}${ICO.move()} Faza I`],
    ["II", `${ICO.phase2()}${ICO.monster()} Faza II`],
    ["III", `${ICO.phase3()}${ICO.cardBlue()} Faza III`],
  ];
  const active = state.turn.phase;
  const p = activePlayer();
  const school = schoolById(p.schoolId);

  shell(`
    <section class="panel">
      <div class="badge-row">
        <span class="player-chip">${schoolIcon(p.schoolId)}${escapeHtml(p.name)} <span>${school.name}</span></span>
      </div>
      <div class="phase-tabs">
        ${tabs
          .map(
            ([id, label]) =>
              `<button class="phase-tab ${active === id ? "active" : ""}" data-phase="${id}">${label}</button>`
          )
          .join("")}
      </div>
      <p class="step-kicker">Przewodnik tury${isSolo() ? " · solo" : ""}</p>
      <h1>${title}</h1>
      <div class="step-body">${body}</div>
      <div class="actions" id="turn-actions"></div>
    </section>
  `);

  appEl.querySelectorAll("[data-phase]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const id = btn.getAttribute("data-phase");
      if (id === "hub") setTurn("hub", "hub");
      else if (id === "I") setTurn("I", "phase1-start");
      else if (id === "II") setTurn("II", "phase2");
      else if (id === "III") setTurn("III", "phase3");
    });
  });
}

function setTurn(phase, screen, extra = {}) {
  let moveTo = extra.moveTo !== undefined ? extra.moveTo : state.turn?.moveTo || null;
  let moveCost = extra.moveCost !== undefined ? extra.moveCost : state.turn?.moveCost || null;
  let exploreDeck =
    extra.exploreDeck !== undefined ? extra.exploreDeck : state.turn?.exploreDeck || null;
  if (extra.clearMove) {
    moveTo = null;
    moveCost = null;
  }
  if (extra.clearExplore) exploreDeck = null;
  state.turn = { phase, screen, moveTo, moveCost, exploreDeck };
  const p = activePlayer();
  if (p) {
    if (phase === "I") p.nowDoing = "faza1";
    else if (screen === "fight" || screen === "fight-monster" || screen === "fight-witcher")
      p.nowDoing = "walka";
    else if (screen === "meditate") p.nowDoing = "medytacja";
    else if (screen === "explore") p.nowDoing = "eksploracja";
    else if (phase === "III") p.nowDoing = "faza3";
    else if (phase === "hub") p.nowDoing = "czekam";
  }
  saveState();
  render();
  window.scrollTo({ top: 0, behavior: "smooth" });
}

function nextPlayer() {
  if (isSolo()) {
    state.activeIndex = 0;
  } else {
    state.activeIndex = (state.activeIndex + 1) % state.players;
  }
  setTurn("hub", "hub");
}

function bindChoices() {
  appEl.querySelectorAll(".choice[data-goto]").forEach((btn) => {
    btn.addEventListener("click", () => {
      if (btn.disabled || btn.classList.contains("choice-disabled")) return;
      const goto = btn.getAttribute("data-goto");
      if (goto === "phase2") {
        state.board.highlightFrom = null;
        setTurn("II", "phase2", { clearMove: true });
      } else if (goto === "move") {
        const ap = activePlayer();
        if (ap && !ap.locationId) placeAtSchool(ap, { force: true });
        state.board.highlightFrom = ap?.locationId || null;
        setTurn("I", "move-pick", { clearMove: true });
      }
      else if (goto === "fight") setTurn("II", "fight");
      else if (goto === "meditate") setTurn("II", "meditate");
      else if (goto === "explore") setTurn("II", "explore");
      else if (goto === "fight-monster") setTurn("II", "fight-monster");
      else if (goto === "fight-witcher") setTurn("II", "fight-witcher");
      else if (goto === "phase3") setTurn("III", "phase3");
    });
  });
}

function fightOptionsFor(player) {
  return phase2FightOptions(player, {
    board: state.board,
    roster: state.roster,
    isSolo: isSolo(),
  });
}

function bindMovePick() {
  appEl.querySelectorAll("[data-move-to]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const toId = btn.getAttribute("data-move-to");
      setTurn("I", "move-cost", { moveTo: toId, moveCost: null });
    });
  });
}

function bindMoveCostTurn() {
  appEl.querySelectorAll("[data-move-cost]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const costId = btn.getAttribute("data-move-cost");
      const p = activePlayer();
      const toId = state.turn.moveTo;
      if (!p || !toId) return;
      const result = applyMoveCost(p, costId);
      if (!result.ok) {
        window.alert(result.reason);
        return;
      }
      p.locationId = toId;
      state.board.selectedLoc = toId;
      state.board.movePrompt = null;
      state.board.highlightFrom = null;
      finishMoveTo(p, toId, costId);
    });
  });
}

/** Po opłaceniu ruchu: potwór → walka; inaczej po ruchu (+ ewentualne zadanie). */
function finishMoveTo(p, toId, costId) {
  const mon = monsterAt(state.board, toId);
  if (mon) {
    p.nowDoing = "walka";
    setTurn("II", "fight-monster", { moveTo: toId, moveCost: costId });
    return;
  }
  p.nowDoing = "faza1";
  setTurn("I", "after-move", { moveTo: toId, moveCost: costId });
}

function bindResourceAdjust(player) {
  appEl.querySelectorAll("[data-adj]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const field = btn.getAttribute("data-adj");
      const delta = Number(btn.getAttribute("data-delta"));
      if (!player || !field) return;
      if (field === "cards") player.cards = Math.max(0, Math.min(20, (player.cards || 0) + delta));
      if (field === "gold") player.gold = Math.max(0, Math.min(99, (player.gold || 0) + delta));
      if (field === "elixirs") player.elixirs = Math.max(0, Math.min(4, (player.elixirs || 0) + delta));
      saveState();
      render();
    });
  });
}

function bindAttrAdjust(player) {
  ensureCombatTrack(player);
  appEl.querySelectorAll("[data-attr]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const key = btn.getAttribute("data-attr");
      const delta = Number(btn.getAttribute("data-delta"));
      if (!key || !player.attrs) return;
      player.attrs[key] = Math.max(1, Math.min(5, (player.attrs[key] || 1) + delta));
      if (key === "defense" && (player.shield || 0) > player.attrs.defense) {
        player.shield = player.attrs.defense;
      }
      saveState();
      render();
    });
  });
  appEl.querySelectorAll("[data-shield-delta]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const delta = Number(btn.getAttribute("data-shield-delta"));
      const max = player.attrs?.defense || 1;
      player.shield = Math.max(0, Math.min(max, (player.shield || 0) + delta));
      saveState();
      render();
    });
  });
}

function bindTrailForms(player) {
  ensureCombatTrack(player);
  appEl.querySelectorAll("[data-trail-set]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const terrain = btn.getAttribute("data-trail-set");
      const kind = btn.getAttribute("data-kind") || "trail";
      const raw = window.prompt(
        kind === "quest"
          ? "Numer lokacji z żetonu (0–18), np. 6 = Novigrad:"
          : "Opcjonalnie numer lokacji źródła tropu (Enter = pomiń):",
        ""
      );
      if (raw === null) return;
      const num = String(raw).trim();
      let locId = null;
      if (num !== "") {
        const n = Number(num);
        const loc = LOCATIONS.find((l) => l.number === n) || locById(num);
        if (!loc) {
          window.alert("Nie znam takiej lokacji — sprawdź numer 0–18.");
          return;
        }
        locId = loc.id;
      }
      if (kind === "quest" && !locId) {
        window.alert("Do zadania tropienia podaj numer lokacji z żetonu.");
        return;
      }
      player.trails[terrain] = {
        kind,
        locId,
        goldOnToken: kind === "quest" ? 1 : 0,
      };
      saveState();
      render();
    });
  });
  appEl.querySelectorAll("[data-trail-clear]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const terrain = btn.getAttribute("data-trail-clear");
      player.trails[terrain] = null;
      saveState();
      render();
    });
  });
}

function bindQuestForms(player) {
  const addBtn = appEl.querySelector("#quest-add-btn");
  if (addBtn) {
    addBtn.addEventListener("click", () => {
      ensureQuests(player);
      const title = (appEl.querySelector("#quest-title")?.value || "").trim() || "Zadanie";
      const locationId = appEl.querySelector("#quest-loc")?.value;
      const eventCard = (appEl.querySelector("#quest-event")?.value || "").trim();
      const note = (appEl.querySelector("#quest-note")?.value || "").trim();
      if (!locationId || !locById(locationId)) {
        window.alert("Wybierz lokację zadania.");
        return;
      }
      player.quests.push({
        id: newQuestId(),
        title: title.replace(/[<>]/g, ""),
        locationId,
        eventCard: eventCard.replace(/[<>]/g, ""),
        note: note.replace(/[<>]/g, ""),
      });
      saveState();
      render();
    });
  }
  appEl.querySelectorAll("[data-quest-done]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const id = btn.getAttribute("data-quest-done");
      ensureQuests(player);
      player.quests = player.quests.filter((q) => q.id !== id);
      saveState();
      render();
    });
  });
}

function addTurnButtons(buttons) {
  const wrap = appEl.querySelector("#turn-actions");
  if (!wrap) return;
  wrap.innerHTML = buttons
    .map(
      (b) =>
        `<button class="btn ${b.primary ? "btn-primary" : "btn-secondary"}" data-act="${b.id}">${b.label}</button>`
    )
    .join("");
  wrap.querySelectorAll("[data-act]").forEach((btn) => {
    const conf = buttons.find((b) => b.id === btn.getAttribute("data-act"));
    if (conf) btn.addEventListener("click", conf.onClick);
  });
}

function renderTurn() {
  const { screen } = state.turn;
  const solo = isSolo();
  const p = activePlayer();

  if (screen === "hub") {
    const picker = solo
      ? ""
      : `<h3>Aktywny gracz</h3>
         <div class="choice-grid">
           ${state.roster
             .map(
               (r, i) => `
             <button class="choice ${i === state.activeIndex ? "active" : ""}" data-active="${i}">
               <strong>${escapeHtml(r.name)}</strong>
               <span>${schoolById(r.schoolId).name}${i === 0 ? " · pierwszy" : ""}</span>
             </button>`
             )
             .join("")}
         </div>`;

    turnShell(
      solo ? "Twoja tura" : `Tura: ${escapeHtml(p.name)}`,
      `
        ${picker}
        <p class="lead">${
          solo
            ? "Fazy I → II → III. Notuj numer tury. Start: 5 kart, 3 złota."
            : "Aktywny rozgrywa I → II → III. Kolejność zgodnie z zegarem (pierwszy = index 0 w składzie)."
        }</p>
        ${solo ? "" : `<h3>Startowe zasoby (przypomnienie)</h3>${resourcesTable(state.players)}`}
      `
    );

    if (!solo) {
      appEl.querySelectorAll("[data-active]").forEach((btn) => {
        btn.addEventListener("click", () => {
          state.activeIndex = Number(btn.dataset.active);
          saveState();
          render();
        });
      });
    }

    addTurnButtons([
      {
        id: "go-i",
        label: `Zaczynam fazę I (${escapeHtml(p.name)})`,
        primary: true,
        onClick: () => {
          p.nowDoing = "faza1";
          saveState();
          setTurn("I", "phase1-start");
        },
      },
      {
        id: "to-board",
        label: "Stół — mapa / reputacja / karty",
        onClick: () => go("board"),
      },
    ]);
    return;
  }

  if (screen === "phase1-start") {
    turnShell(
      "Faza I — start",
      `
        <p class="lead"><strong>${escapeHtml(p.name)}</strong>: <em>obowiązkowy ruch</em> albo od razu faza II. Nie zaczynasz akcją na polu, na którym już stoisz.</p>
        <div class="choice-grid">
          <button class="choice" data-goto="move"><strong>Ruszam się</strong><span>Mapa — kliknij sąsiada</span></button>
          <button class="choice" data-goto="phase2"><strong>Idę do fazy II</strong><span>Bez ruchu</span></button>
        </div>
      `
    );
    bindChoices();
    return;
  }

  if (screen === "move-pick") {
    if (!p.locationId) {
      placeAtSchool(p, { force: true });
      p._prevSchoolId = p.schoolId;
    }
    const fromId = p.locationId;
    state.board.highlightFrom = fromId;
    state.board.selectedLoc = fromId;
    const destOk =
      state.turn.moveTo && neighborsOf(fromId).includes(state.turn.moveTo);
    const popupInner = destOk
      ? mapLocPopupHtml(state.turn.moveTo, {
          player: p,
          roster: state.roster,
          monsters: state.board.monsters,
        })
      : null;
    turnShell(
      "Faza I — ruch na mapie",
      `
        <p class="lead"><strong>${escapeHtml(p.name)}</strong> · kliknij sąsiada (złota obwódka). Opis wyskoczy nad mapą.</p>
        ${mapSvgHtml(state.board, state.roster, {
          schoolById,
          highlightFromId: fromId,
          popupHtml: popupInner,
        })}
        ${
          destOk
            ? `<p class="tip" style="margin:0.55rem 0 0">Wybrano: <strong>${escapeHtml(
                locById(state.turn.moveTo)?.name || "?"
              )}</strong></p>
               <div class="actions"><button type="button" class="btn btn-primary" id="confirm-dest">Idę tutaj — wybór kosztu</button></div>`
            : `<p class="tip">Kliknij <strong>podświetlone</strong> pole — krótki opis pojawi się nad mapą (3 s / klik poza).</p>`
        }
      `
    );
    bindMapLocPopup(appEl);
    appEl.querySelectorAll(".map-node-reach").forEach((g) => {
      g.addEventListener("click", () => {
        const toId = g.getAttribute("data-loc");
        state.board.selectedLoc = toId;
        setTurn("I", "move-pick", { moveTo: toId, moveCost: null });
      });
    });
    const confirm = appEl.querySelector("#confirm-dest");
    if (confirm) {
      confirm.addEventListener("click", () => {
        if (!state.turn.moveTo) return;
        setTurn("I", "move-cost", { moveTo: state.turn.moveTo, moveCost: null });
      });
    }
    addTurnButtons([
      {
        id: "back",
        label: "Wróć",
        onClick: () => {
          state.board.highlightFrom = null;
          setTurn("I", "phase1-start", { clearMove: true });
        },
      },
    ]);
    return;
  }

  if (screen === "move-cost") {
    const to = locById(state.turn.moveTo);
    const from = locById(p.locationId);
    if (!to) {
      setTurn("I", "move-pick", { clearMove: true });
      return;
    }
    turnShell(
      "Faza I — koszt ruchu",
      `
        ${destinationPreviewHtml(to.id, {
          player: p,
          roster: state.roster,
          monsters: state.board.monsters,
        })}
        ${moveCostChooserHtml({
          fromLoc: from,
          toLoc: to,
          playerName: p.name,
          selectedCostId: state.turn.moveCost,
        })}
        <p class="tip">Tracker: <strong>${p.cards ?? 0}</strong> kart · <strong>${p.gold ?? 0}</strong> złota — po wyborze odejmiemy koszt.</p>
      `
    );
    bindMoveCostTurn();
    addTurnButtons([
      { id: "back", label: "Inny cel", onClick: () => setTurn("I", "move-pick", { clearMove: true }) },
    ]);
    return;
  }

  if (screen === "after-move") {
    const here = locById(p.locationId);
    const mon = monsterAt(state.board, p.locationId);
    if (mon && here) {
      turnShell(
        "Potwór na polu!",
        `
          ${monsterEncounterHtml(here, mon)}
          ${questArrivalHtml(p, p.locationId)}
          <div class="choice-grid">
            <button class="choice" data-goto="fight-monster"><strong>Zaczynam walkę</strong><span>Faza II — walka z potworem</span></button>
            <button class="choice" data-goto="move"><strong>Uciekam dalej</strong><span>Kolejny ruch (koszt kart)</span></button>
          </div>
        `
      );
      bindChoices();
      bindQuestForms(p);
      return;
    }
    turnShell(
      "Faza I — po ruchu",
      `
        ${questArrivalHtml(p, p.locationId)}
        <p class="lead">Jesteś na <strong>${here ? escapeHtml(here.name) : "—"}</strong>. Najpierw możesz zrobić <em>akcję ze znaku na tym polu</em>:</p>
        ${locationActionGuideHtml(here)}
        ${phase1AfterMove(solo)}
        <div class="choice-grid">
          <button class="choice" data-goto="move"><strong>Ruszam się znowu</strong><span>Znowu wybór celu + koszt kart</span></button>
          <button class="choice" data-goto="phase2"><strong>Przechodzę do fazy II</strong><span>Zostawiam rękę</span></button>
        </div>
        <details style="margin-top:1rem"><summary><strong>Ściąga wszystkich akcji obszarów</strong></summary>${LOCATION_ACTIONS}</details>
      `
    );
    bindChoices();
    bindQuestForms(p);
    return;
  }

  if (screen === "phase2") {
    ensureCombatTrack(p);
    const med = meditationStatus(p, { isSolo: solo });
    const fight = fightOptionsFor(p);
    // Bez celu walki i bez medytacji → od razu eksploracja
    if (!fight.can && !med.can) {
      p.nowDoing = "eksploracja";
      saveState();
      setTurn("II", "explore");
      return;
    }
    turnShell(
      "Faza II — wybór",
      `
        ${
          med.can
            ? `<p class="tip choice-meditate-ready" style="padding:0.7rem 0.9rem"><strong>Medytacja dostępna!</strong> ${escapeHtml(med.reason)}</p>`
            : `<p class="note">${escapeHtml(med.reason)}</p>`
        }
        ${
          fight.can
            ? `<p class="tip" style="padding:0.55rem 0.9rem">${escapeHtml(fight.reason)}</p>`
            : `<p class="note">${escapeHtml(fight.reason)}</p>`
        }
        ${phase2Choices(solo, {
          meditateEnabled: med.can,
          meditateHint: med.reason,
          fightEnabled: fight.can,
          fightHint: fight.can
            ? solo
              ? "Na Twoim obszarze"
              : "Z potworem lub wiedźminem"
            : fight.reason,
        })}
      `
    );
    bindChoices();
    return;
  }

  if (screen === "fight") {
    const fight = fightOptionsFor(p);
    if (!fight.can) {
      setTurn("II", "phase2");
      return;
    }
    if (solo) {
      setTurn("II", fight.canMonster ? "fight-monster" : "phase2");
      return;
    }
    // Multi: jeden możliwy typ → od razu tam
    if (fight.canMonster && !fight.canWitcher) {
      setTurn("II", "fight-monster");
      return;
    }
    if (!fight.canMonster && fight.canWitcher) {
      setTurn("II", "fight-witcher");
      return;
    }
    turnShell(
      "Walka — typ",
      fightBranchHtml({
        monsterEnabled: fight.canMonster,
        witcherEnabled: fight.canWitcher,
        hint: fight.reason,
      })
    );
    bindChoices();
    addTurnButtons([
      { id: "back", label: "Wróć", onClick: () => setTurn("II", "phase2") },
    ]);
    return;
  }

  if (screen === "fight-monster") {
    const here = locById(p.locationId);
    const mon = monsterAt(state.board, p.locationId);
    if (!mon) {
      // Wejście bez potwora (np. stary stan) — wróć do wyboru fazy II
      setTurn("II", "phase2");
      return;
    }
    ensureCombatTrack(p);
    const hasTrail = hasTrailForMonster(p, here);
    turnShell(
      "Walka z potworem — co teraz?",
      `
        ${mon && here ? monsterEncounterHtml(here, mon) : ""}
        ${questArrivalHtml(p, p.locationId)}
        <p class="${hasTrail ? "tip" : "warn"}"><strong>Żeton tropu:</strong> ${
          hasTrail
            ? `Masz trop terenu tego pola (${here?.terrain || "?"}) → <strong>Ty zaczynasz</strong> walkę.`
            : `Brak tropu na ten teren → <strong>najpierw tura potwora</strong>.`
        }</p>
        ${attrsAdjustHtml(p)}
        ${trailsHtml(p, { editable: true })}
        ${resourceAdjustHtml(p, { title: "Karty / złoto / eliksiry w walce" })}
        ${fightMonster(solo)}
        ${
          mon
            ? `<div class="actions" style="margin-top:0.8rem">
                <button type="button" class="btn btn-secondary" id="clear-monster-after-win">Usuń potwora z mapy (pokonany / odpędzony)</button>
              </div>`
            : ""
        }
      `
    );
    bindQuestForms(p);
    bindResourceAdjust(p);
    bindAttrAdjust(p);
    bindTrailForms(p);
    const clearBtn = appEl.querySelector("#clear-monster-after-win");
    if (clearBtn) {
      clearBtn.addEventListener("click", () => {
        if (p.locationId) delete state.board.monsters[p.locationId];
        saveState();
        render();
      });
    }
    addTurnButtons([
      {
        id: "to3",
        label: "Koniec walki → faza III",
        primary: true,
        onClick: () => {
          p.nowDoing = "faza3";
          saveState();
          setTurn("III", "phase3");
        },
      },
      { id: "back", label: "Wróć", onClick: () => setTurn("II", solo ? "phase2" : "fight") },
    ]);
    return;
  }

  if (screen === "fight-witcher") {
    const fight = fightOptionsFor(p);
    if (!fight.canWitcher) {
      setTurn("II", "phase2");
      return;
    }
    ensureCombatTrack(p);
    turnShell(
      "Walka z wiedźminem",
      `
        ${attrsAdjustHtml(p)}
        ${resourceAdjustHtml(p, { title: "Karty / złoto" })}
        ${FIGHT_WITCHER}
      `
    );
    bindResourceAdjust(p);
    bindAttrAdjust(p);
    addTurnButtons([
      { id: "to3", label: "Koniec walki → faza III", primary: true, onClick: () => setTurn("III", "phase3") },
      { id: "back", label: "Wróć", onClick: () => setTurn("II", "fight") },
    ]);
    return;
  }

  if (screen === "meditate") {
    ensureCombatTrack(p);
    const med = meditationStatus(p, { isSolo: solo });
    if (!med.can) {
      turnShell(
        "Medytacja — niedostępna",
        `
          <p class="warn">${escapeHtml(med.reason)}</p>
          ${meditate(solo)}
          ${attrsAdjustHtml(p)}
          <p class="note">${escapeHtml(med.soloNote)}</p>
        `
      );
      bindAttrAdjust(p);
      addTurnButtons([{ id: "back", label: "Wróć", onClick: () => setTurn("II", "phase2") }]);
      return;
    }
    turnShell(
      "Medytacja",
      `
        <p class="tip"><strong>OK — możesz medytować.</strong> ${escapeHtml(med.reason)}</p>
        ${meditate(solo)}
        ${attrsAdjustHtml(p)}
        <div class="actions" style="margin:0.6rem 0">
          ${med.available
            .map(
              (id) =>
                `<button type="button" class="btn btn-primary" data-take-trophy="${id}">Weź trofeum: ${ATTR_KEYS.find((a) => a.id === id)?.label || id}</button>`
            )
            .join("")}
        </div>
        <p class="note">${escapeHtml(med.soloNote)}</p>
      `
    );
    bindAttrAdjust(p);
    appEl.querySelectorAll("[data-take-trophy]").forEach((btn) => {
      btn.addEventListener("click", () => {
        const id = btn.getAttribute("data-take-trophy");
        ensureCombatTrack(p);
        if (!p.attrTrophies.includes(id)) p.attrTrophies.push(id);
        if ((p.reputation || 0) < 3) {
          p.reputation = Math.min(4, (p.reputation || 0) + 1);
        }
        saveState();
        render();
      });
    });
    addTurnButtons([
      { id: "to3", label: "Dalej → faza III", primary: true, onClick: () => setTurn("III", "phase3") },
      { id: "back", label: "Wróć", onClick: () => setTurn("II", "phase2") },
    ]);
    return;
  }

  if (screen === "explore") {
    const here = locById(p.locationId);
    turnShell(
      "Eksploracja — wybór talii",
      `
        ${exploreDeckChooserHtml({ selected: state.turn.exploreDeck })}
        ${exploreGuideHtml(here, solo)}
      `
    );
    appEl.querySelectorAll("[data-explore-deck]").forEach((btn) => {
      btn.addEventListener("click", () => {
        const deck = btn.getAttribute("data-explore-deck");
        setTurn("II", "explore-resolve", { exploreDeck: deck });
      });
    });
    addTurnButtons([
      { id: "back", label: "Wróć", onClick: () => setTurn("II", "phase2", { clearExplore: true }) },
    ]);
    return;
  }

  if (screen === "explore-resolve") {
    const deck = state.turn.exploreDeck || "city";
    turnShell(
      "Eksploracja — po karcie",
      `
        ${exploreResolveHtml({ deck, isSolo: solo, player: p })}
        ${trailsHtml(p, { editable: true })}
      `
    );
    bindResourceAdjust(p);
    bindQuestForms(p);
    bindTrailForms(p);
    addTurnButtons([
      {
        id: "to3",
        label: "Dalej → faza III",
        primary: true,
        onClick: () => setTurn("III", "phase3", { clearExplore: true }),
      },
      {
        id: "back-deck",
        label: "Inna talia",
        onClick: () => setTurn("II", "explore", { clearExplore: true }),
      },
    ]);
    return;
  }

  if (screen === "phase3") {
    const hint = phase3DrawHint(p.cards || 0);
    turnShell(
      "Faza III — dobór i zakup",
      `
        ${phase3(solo)}
        ${resourceAdjustHtml(p, { title: "Po doborze / kupnie — ustaw tracker" })}
        <p class="tip">${escapeHtml(hint.note)}</p>
        <div class="actions" style="margin:0.5rem 0">
          <button type="button" class="btn btn-secondary" id="phase3-set-hand">Ustaw rękę na 3 (po doborze)</button>
        </div>
      `
    );
    bindResourceAdjust(p);
    const setHand = appEl.querySelector("#phase3-set-hand");
    if (setHand) {
      setHand.addEventListener("click", () => {
        p.cards = 3;
        saveState();
        render();
      });
    }
    addTurnButtons([
      {
        id: "summary",
        label: "Podsumowanie tury",
        primary: true,
        onClick: () => setTurn("III", "turn-summary"),
      },
      {
        id: "to-board",
        label: "Stół — mapa / reputacja / karty",
        onClick: () => go("board"),
      },
    ]);
    return;
  }

  if (screen === "turn-summary") {
    const hint = phase3DrawHint(p.cards || 0);
    const next = state.roster[(state.activeIndex + 1) % state.players];
    turnShell(
      "Podsumowanie tury",
      turnSummaryHtml(p, { phase3Hint: hint, schoolName: schoolById(p.schoolId).short })
    );
    bindQuestForms(p);
    addTurnButtons([
      {
        id: "next",
        label: solo
          ? "Koniec tury (+1) — znów Ty"
          : `Koniec tury → ${escapeHtml(next.name)}`,
        primary: true,
        onClick: () => nextPlayer(),
      },
      {
        id: "back-p3",
        label: "Wróć do fazy III",
        onClick: () => setTurn("III", "phase3"),
      },
    ]);
  }
}

function renderBoard() {
  const board = state.board;
  shell(`
    <section class="panel board-panel">
      <p class="step-kicker">Przy stole · mapa i status</p>
      <h1>Stół</h1>
      <p class="lead">Schemat mapy do oznaczania potworów i wiedźminów (możesz obracać). Jak przesuniesz wiedźmina na sąsiada — wyskoczy wybór kosztu ruchu (1 karta / 2 karty / 1+złoto). Przyciski „co robi teraz” prowadzą za rękę (m.in. eksploracja wg znaków pola).</p>

      <h2>Mapa (schemat)</h2>
      ${mapToolbarHtml(board, state.roster)}
      ${mapSvgHtml(board, state.roster, {
        schoolById,
        highlightFromId:
          board.highlightFrom ||
          board.movePrompt?.fromId ||
          (activePlayer()?.nowDoing === "faza1" ? activePlayer()?.locationId : null),
      })}
      ${selectedLocPanel(board, state.roster)}
      ${
        (board.highlightFrom || activePlayer()?.nowDoing === "faza1") && activePlayer()?.locationId
          ? neighborPickerHtml(board.highlightFrom || activePlayer().locationId, {
              player: activePlayer(),
              monsters: board.monsters,
            })
          : ""
      }
      ${movePromptHtml(board, state.roster)}
      ${
        board.movePrompt
          ? `<div class="actions" style="margin:0.5rem 0 1rem">
              <button type="button" class="btn btn-secondary" id="dismiss-move-prompt">Anuluj ten ruch (cofnij pionek)</button>
            </div>`
          : ""
      }

      ${monstersPanelHtml(board)}

      <h2>Gracze — reputacja i karty</h2>
      <div class="tracker-list">
        ${playerTrackerHtml(state.roster, {
          schoolById,
          isSolo: isSolo(),
          activeIndex: state.activeIndex,
          START_RESOURCES,
          players: state.players,
        })}
      </div>

      <div class="actions">
        <button class="btn btn-primary" id="to-turn">Prowadź turę</button>
        <button class="btn btn-secondary" id="to-icons">Co oznaczają symbole?</button>
        <button class="btn btn-secondary" id="reset-board">Wyczyść tylko mapę</button>
      </div>
    </section>
  `);

  appEl.querySelectorAll(".map-tool").forEach((btn) => {
    btn.addEventListener("click", () => {
      state.board.tool = btn.getAttribute("data-tool");
      saveState();
      render();
    });
  });

  const rotCcw = appEl.querySelector("#map-rotate-ccw");
  const rotCw = appEl.querySelector("#map-rotate-cw");
  const rotReset = appEl.querySelector("#map-rotate-reset");
  if (rotCcw) {
    rotCcw.addEventListener("click", () => {
      state.board.rotation = ((state.board.rotation || 0) - 90 + 360) % 360;
      saveState();
      render();
    });
  }
  if (rotCw) {
    rotCw.addEventListener("click", () => {
      state.board.rotation = ((state.board.rotation || 0) + 90) % 360;
      saveState();
      render();
    });
  }
  if (rotReset) {
    rotReset.addEventListener("click", () => {
      state.board.rotation = 0;
      saveState();
      render();
    });
  }

  appEl.querySelectorAll("[data-place-player]").forEach((btn) => {
    btn.addEventListener("click", () => {
      state.board.placePlayer = Number(btn.dataset.placePlayer);
      state.board.tool = "player";
      saveState();
      render();
    });
  });

  appEl.querySelectorAll(".map-node").forEach((g) => {
    g.addEventListener("click", () => {
      const locId = g.getAttribute("data-loc");
      const tool = state.board.tool || "select";
      state.board.selectedLoc = locId;

      if (tool === "monster") {
        const cur = state.board.monsters[locId] || { count: 0 };
        const level = state.board.pendingMonsterLevel || cur.level || null;
        state.board.monsters[locId] = {
          count: (cur.count || 0) + 1,
          level: level || cur.level,
          label: cur.label || (level ? `Poz. ${level}` : undefined),
        };
      } else if (tool === "monster-") {
        const cur = state.board.monsters[locId];
        if (cur) {
          cur.count = Math.max(0, (cur.count || 0) - 1);
          if (cur.count === 0) delete state.board.monsters[locId];
        }
      } else if (tool === "clear-m") {
        delete state.board.monsters[locId];
      } else if (tool === "player") {
        const idx = state.board.placePlayer || 0;
        const pl = state.roster[idx];
        if (pl) {
          const fromId = pl.locationId;
          // Pierwsze ustawienie albo to samo pole = bez kosztu ruchu
          if (!fromId || fromId === locId) {
            pl.locationId = locId;
            state.board.movePrompt = null;
          } else {
            const adj = neighborsOf(fromId);
            if (!adj.includes(locId)) {
              window.alert(
                "To nie jest sąsiad po ścieżce. Albo kliknij sąsiada, albo najpierw ustaw pionek (pierwszy raz) bez kosztu."
              );
            } else {
              // Przesuń od razu, ale wymagaj wyboru kosztu (anuluj = cofnij)
              pl.locationId = locId;
              pl.nowDoing = "faza1";
              state.activeIndex = idx;
              state.board.movePrompt = {
                playerIdx: idx,
                fromId,
                toId: locId,
                costId: null,
              };
            }
          }
        }
      }

      saveState();
      render();
    });
  });

  const dismissMove = appEl.querySelector("#dismiss-move-prompt");
  if (dismissMove) {
    dismissMove.addEventListener("click", () => {
      const mp = state.board.movePrompt;
      if (mp && state.roster[mp.playerIdx]) {
        state.roster[mp.playerIdx].locationId = mp.fromId;
        state.board.selectedLoc = mp.fromId;
      }
      state.board.movePrompt = null;
      saveState();
      render();
    });
  }

  appEl.querySelectorAll("[data-move-to]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const toId = btn.getAttribute("data-move-to");
      const pl = activePlayer();
      if (!pl?.locationId || !toId) return;
      const fromId = pl.locationId;
      if (!neighborsOf(fromId).includes(toId)) {
        window.alert("To nie jest sąsiad po ścieżce.");
        return;
      }
      pl.locationId = toId;
      pl.nowDoing = "faza1";
      state.board.selectedLoc = toId;
      state.board.highlightFrom = fromId;
      state.board.movePrompt = {
        playerIdx: state.activeIndex,
        fromId,
        toId,
        costId: null,
      };
      saveState();
      render();
    });
  });

  appEl.querySelectorAll("[data-move-cost]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const mp = state.board.movePrompt;
      if (!mp) return;
      const pl = state.roster[mp.playerIdx];
      if (!pl) return;
      const costId = btn.getAttribute("data-move-cost");
      const result = applyMoveCost(pl, costId);
      if (!result.ok) {
        window.alert(result.reason);
        return;
      }
      mp.costId = costId;
      state.board.movePrompt = null;
      state.board.highlightFrom = null;
      state.board.selectedLoc = mp.toId;
      // Na stole: też od razu do walki, jeśli potwór
      const mon = monsterAt(state.board, mp.toId);
      if (mon) {
        pl.nowDoing = "walka";
        state.activeIndex = mp.playerIdx;
        saveState();
        go("turn");
        setTurn("II", "fight-monster", { moveTo: mp.toId, moveCost: costId });
        return;
      }
      pl.nowDoing = "faza1";
      saveState();
      render();
    });
  });

  appEl.querySelectorAll("[data-monster-level]").forEach((btn) => {
    btn.addEventListener("click", () => {
      state.board.pendingMonsterLevel = btn.getAttribute("data-monster-level");
      state.board.tool = "monster";
      saveState();
      render();
    });
  });

  appEl.querySelectorAll("[data-monster-clear]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const id = btn.getAttribute("data-monster-clear");
      delete state.board.monsters[id];
      saveState();
      render();
    });
  });

  appEl.querySelectorAll("[data-rep]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const i = Number(btn.dataset.idx);
      const n = Number(btn.dataset.rep);
      if (state.roster[i]) state.roster[i].reputation = n;
      saveState();
      render();
    });
  });

  appEl.querySelectorAll("[data-cards-delta]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const i = Number(btn.dataset.idx);
      const d = Number(btn.dataset.cardsDelta);
      const p = state.roster[i];
      if (!p) return;
      p.cards = Math.max(0, Math.min(20, (p.cards || 0) + d));
      saveState();
      render();
    });
  });

  appEl.querySelectorAll("[data-gold-delta]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const i = Number(btn.dataset.idx);
      const d = Number(btn.dataset.goldDelta);
      const p = state.roster[i];
      if (!p) return;
      p.gold = Math.max(0, Math.min(99, (p.gold || 0) + d));
      saveState();
      render();
    });
  });

  appEl.querySelectorAll("[data-elixir-delta]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const i = Number(btn.dataset.idx);
      const d = Number(btn.dataset.elixirDelta);
      const p = state.roster[i];
      if (!p) return;
      p.elixirs = Math.max(0, Math.min(4, (p.elixirs || 0) + d));
      saveState();
      render();
    });
  });

  appEl.querySelectorAll(".tracker-card [data-quest-done]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const i = Number(btn.dataset.idx);
      const id = btn.getAttribute("data-quest-done");
      const p = state.roster[i];
      if (!p || !id) return;
      ensureQuests(p);
      p.quests = p.quests.filter((q) => q.id !== id);
      saveState();
      render();
    });
  });

  appEl.querySelectorAll("[data-now]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const i = Number(btn.dataset.idx);
      const now = btn.dataset.now;
      if (state.roster[i]) {
        state.roster[i].nowDoing = now;
        if (now !== "czekam") state.activeIndex = i;
        if (now === "faza1") {
          if (!state.roster[i].locationId) placeAtSchool(state.roster[i], { force: true });
          state.board.highlightFrom = state.roster[i].locationId;
        } else {
          state.board.highlightFrom = null;
        }
      }
      saveState();
      render();
    });
  });

  appEl.querySelectorAll("[data-apply-fatigue]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const i = Number(btn.dataset.idx);
      const p = state.roster[i];
      if (!p || p.reputation >= 4) return;
      const next = p.reputation + 1;
      const lose = fatigueForReputation(next);
      if (
        !window.confirm(
          `+1 reputacja → ${next}. Usunąć z gry ${lose} kart(y) z ręki (uproszczenie trackera)? Na stole możesz brać też z talii/odrzuconych.`
        )
      ) {
        return;
      }
      p.reputation = next;
      p.cards = Math.max(0, (p.cards || 0) - lose);
      saveState();
      render();
    });
  });

  appEl.querySelectorAll("[data-apply-draw7]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const i = Number(btn.dataset.idx);
      const p = state.roster[i];
      if (!p) return;
      const hint = phase3DrawHint(p.cards || 0);
      p.cards = hint.afterDraw;
      saveState();
      render();
    });
  });

  appEl.querySelector("#to-turn").addEventListener("click", () => go("turn"));
  appEl.querySelector("#to-icons").addEventListener("click", () => go("icons"));
  appEl.querySelector("#reset-board").addEventListener("click", () => {
    if (!window.confirm("Wyczyścić tylko mapę (potwory i pozycje wiedźminów)?")) return;
    state.board = defaultBoard();
    state.roster.forEach((p) => {
      p.locationId = null;
    });
    saveState();
    render();
  });
}

function resourcesTable(n) {
  const rows = START_RESOURCES[n]
    .map((r, i) => {
      const who = state.roster[i]
        ? `${escapeHtml(state.roster[i].name)} (${schoolById(state.roster[i].schoolId).short})`
        : n === 1
          ? "Ty (solo)"
          : PLAYER_LABELS[i];
      return `<tr><th>${who}</th><td>${r.cards} kart(y)</td><td>${r.gold} złota</td></tr>`;
    })
    .join("");
  return `<table class="table"><thead><tr><th>Gracz</th><th>Karty</th><th>Złoto</th></tr></thead><tbody>${rows}</tbody></table>`;
}

function renderRef() {
  shell(`
    <section class="panel">
      <p class="step-kicker">Na szybko</p>
      <h1>Ściąga${isSolo() ? " · solo" : ""}</h1>
      ${quickRef(isSolo())}
      ${LOCATION_ACTIONS}
      <div class="actions">
        <button class="btn btn-primary" id="to-turn">Wróć do tury</button>
        <button class="btn btn-secondary" id="to-sources">Źródła i FAQ</button>
        <button class="btn btn-secondary" id="reset">Wyczyść zapis</button>
      </div>
    </section>
  `);
  appEl.querySelector("#to-turn").addEventListener("click", () => go("turn"));
  appEl.querySelector("#to-sources").addEventListener("click", () => go("sources"));
  appEl.querySelector("#reset").addEventListener("click", () => {
    if (!window.confirm("Wyczyścić cały zapis helpera (gracze, mapa, reputacja)?")) return;
    localStorage.removeItem(STORAGE_KEY);
    Object.assign(state, defaultState());
    render();
  });
}

function renderIcons() {
  shell(`
    <section class="panel">
      <p class="step-kicker">Ściąga symboli</p>
      <h1>Ikony jak przy stole</h1>
      ${iconsLegendHtml()}
      <div class="actions">
        <button class="btn btn-primary" id="to-turn">Do tury</button>
        <button class="btn btn-secondary" id="to-lexicon">Leksykon szkół</button>
      </div>
    </section>
  `);
  appEl.querySelector("#to-turn").addEventListener("click", () => go("turn"));
  appEl.querySelector("#to-lexicon").addEventListener("click", () => go("lexicon"));
}

function renderLexicon() {
  const schoolId = activePlayer()?.schoolId;
  const mine = SCHOOL_SPECIALTIES.find((s) => s.id === schoolId);
  shell(`
    <section class="panel">
      <p class="step-kicker">Podstawka · przy stole</p>
      <h1>Leksykon</h1>
      ${
        mine
          ? `<p class="tip"><strong>Aktywny (${escapeHtml(activePlayer().name)}):</strong> ${mine.name}<br>${mine.body}</p>`
          : ""
      }
      ${lexiconHtml()}
      <div class="actions">
        <button class="btn btn-primary" id="to-turn">Do tury</button>
        <button class="btn btn-secondary" id="to-sources">Źródła / FAQ</button>
      </div>
    </section>
  `);
  appEl.querySelector("#to-turn").addEventListener("click", () => go("turn"));
  appEl.querySelector("#to-sources").addEventListener("click", () => go("sources"));
}

function renderSources() {
  shell(`
    <section class="panel">
      <p class="step-kicker">Weryfikacja zasad</p>
      <h1>Źródła, filmy, FAQ</h1>
      ${sourcesHtml()}
      <div class="actions">
        <button class="btn btn-primary" id="to-home">Wróć</button>
      </div>
    </section>
  `);
  appEl.querySelector("#to-home").addEventListener("click", () => go("home"));
}

render();
