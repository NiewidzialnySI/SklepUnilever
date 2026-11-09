import {
  START_RESOURCES,
  PLAYER_LABELS,
  buildSetupSteps,
  phase1AfterMove,
  phase2Choices,
  FIGHT_BRANCH,
  fightMonster,
  FIGHT_WITCHER,
  meditate,
  phase3,
  LOCATION_ACTIONS,
  quickRef,
  MOVE_HELP,
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
  fatigueForReputation,
  phase3DrawHint,
  locById,
  neighborsOf,
  placeAtSchool,
  schoolStartId,
} from "./board.js";
import {
  neighborPickerHtml,
  moveCostChooserHtml,
  applyMoveCost,
  locationActionGuideHtml,
  exploreGuideHtml,
} from "./guide.js";

const STORAGE_KEY = "wiedzmin-stary-swiat-helper-v4";

const appEl = document.getElementById("app");

function defaultState() {
  return {
    view: "home",
    players: 3,
    roster: [],
    activeIndex: 0,
    setupIndex: 0,
    turn: { phase: "hub", screen: "hub", moveTo: null, moveCost: null },
    board: defaultBoard(),
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
    "setup",
    "board",
    "turn",
    "ref",
    "icons",
    "lexicon",
    "sources",
  ]);
  return {
    ...base,
    view: views.has(raw.view) ? raw.view : "home",
    players,
    roster,
    activeIndex: Math.max(0, Number(raw.activeIndex) || 0),
    setupIndex: Math.max(0, Number(raw.setupIndex) || 0),
    turn,
    board,
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
  appEl.innerHTML = `
    <header class="topbar">
      <div class="brand">
        Wiedźmin: Stary Świat
        <small>Pomocnik · podstawka${isSolo() ? " · solo" : ""}</small>
      </div>
      <div class="top-actions">
        ${[
          ["home", "Start"],
          ["roster", "Gracze"],
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
      <p class="lead">Wybierz liczbę graczy, przypisz <strong>imiona i szkoły</strong>, potem idziemy przez przygotowanie i tury. Na <strong>Stole</strong> masz schemat mapy, potwory, reputację i licznik kart. Ikony tłumaczą symbole (eliksir, kości…).</p>
      <div class="badge-row">
        <span class="badge">Podstawka</span>
        <span class="badge">Solo + 2–5</span>
        <span class="badge">Mapa + tracker</span>
        <span class="badge">${ICO.elixir()}${ICO.dice()} Symbole</span>
      </div>
      <h3>1. Ile osób gra?</h3>
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
        <button class="btn btn-primary" id="to-roster">Dalej — imiona i szkoły</button>
        <button class="btn btn-secondary" id="to-board">Stół — mapa i reputacja</button>
        <button class="btn btn-secondary" id="skip-setup">Mam rozłożone — prowadź turę</button>
      </div>
    </section>
  `);

  appEl.querySelectorAll("[data-players]").forEach((btn) => {
    btn.addEventListener("click", () => setPlayers(Number(btn.dataset.players)));
  });
  appEl.querySelector("#to-roster").addEventListener("click", () => go("roster"));
  appEl.querySelector("#to-board").addEventListener("click", () => go("board"));
  appEl.querySelector("#skip-setup").addEventListener("click", () => {
    go("turn", { turn: { phase: "hub", screen: "hub" } });
  });
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
            const orderHint =
              state.players === 1
                ? "Ty"
                : PLAYER_LABELS[i] + (i === 0 ? " (zaczyna)" : "");
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
        <button class="btn btn-primary" id="to-setup">Dalej — przygotowanie</button>
        <button class="btn btn-secondary" id="to-turn">Od razu do tury</button>
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
      if (!prev.locationId || prev.schoolId !== schoolId) {
        placeAtSchool(next, { force: true });
      }
      next._prevSchoolId = schoolId;
      state.roster[i] = next;
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
  appEl.querySelector("#to-setup").addEventListener("click", () => {
    if (!syncFromDom()) return;
    applySchoolStarts();
    state.setupIndex = 0;
    go("setup");
  });
  appEl.querySelector("#to-turn").addEventListener("click", () => {
    if (!syncFromDom()) return;
    applySchoolStarts();
    go("turn", { turn: { phase: "hub", screen: "hub", moveTo: null, moveCost: null } });
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
    .map(
      (p) =>
        `<span class="player-chip">${schoolIcon(p.schoolId)}${escapeHtml(p.name)} <span>${schoolById(p.schoolId).short}</span></span>`
    )
    .join(" ");

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
  if (extra.clearMove) {
    moveTo = null;
    moveCost = null;
  }
  state.turn = { phase, screen, moveTo, moveCost };
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
      p.nowDoing = "faza1";
      setTurn("I", "after-move", { moveTo: toId, moveCost: costId });
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
        <p class="lead"><strong>${escapeHtml(p.name)}</strong>: instrukcja — <em>1 obowiązkowy ruch albo przejście do fazy II</em>. Nie zaczynasz akcją na obszarze, na którym już stoisz.</p>
        <div class="choice-grid">
          <button class="choice" data-goto="move"><strong>Ruszam się</strong><span>Wybierz sąsiada + koszt kart/złota</span></button>
          <button class="choice" data-goto="phase2"><strong>Idę do fazy II</strong><span>Bez ruchu i bez akcji obszaru</span></button>
        </div>
        ${MOVE_HELP}
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
    turnShell(
      "Faza I — dokąd możesz iść?",
      `
        <p class="lead"><strong>${escapeHtml(p.name)}</strong> stoi na szkole/polu startowym. Poniżej widać <em>wszystkich sąsiadów</em> i jaki koszt pasuje do każdego celu.</p>
        ${neighborPickerHtml(fromId, { player: p })}
        <p class="tip">Po kliknięciu celu wybierzesz dokładnie: 1 kartę terenu / wild / 2 karty / 1+złoto — tracker odejmie zasoby.</p>
        <div class="map-mini" style="margin-top:1rem">
          <h3>Mapa — podświetleni sąsiedzi</h3>
          ${mapSvgHtml(state.board, state.roster, { schoolById, highlightFromId: fromId })}
        </div>
        ${MOVE_HELP}
      `
    );
    bindMovePick();
    addTurnButtons([
      {
        id: "back",
        label: "Wróć",
        onClick: () => {
          state.board.highlightFrom = null;
          setTurn("I", "phase1-start", { clearMove: true });
        },
      },
      { id: "to-board", label: "Pełny stół", onClick: () => go("board") },
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
        ${moveCostChooserHtml({
          fromLoc: from,
          toLoc: to,
          playerName: p.name,
          selectedCostId: state.turn.moveCost,
        })}
        <p class="tip">Tracker: masz <strong>${p.cards ?? 0}</strong> kart i <strong>${p.gold ?? 0}</strong> złota — po wyborze odejmiemy koszt.</p>
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
    turnShell(
      "Faza I — po ruchu",
      `
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
    return;
  }

  if (screen === "phase2") {
    turnShell("Faza II — wybór", phase2Choices(solo));
    bindChoices();
    return;
  }

  if (screen === "fight") {
    turnShell("Walka — typ", FIGHT_BRANCH);
    bindChoices();
    addTurnButtons([
      { id: "back", label: "Wróć", onClick: () => setTurn("II", "phase2") },
    ]);
    return;
  }

  if (screen === "fight-monster") {
    turnShell("Walka z potworem — co teraz?", fightMonster(solo));
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
    turnShell("Walka z wiedźminem", FIGHT_WITCHER);
    addTurnButtons([
      { id: "to3", label: "Koniec walki → faza III", primary: true, onClick: () => setTurn("III", "phase3") },
      { id: "back", label: "Wróć", onClick: () => setTurn("II", "fight") },
    ]);
    return;
  }

  if (screen === "meditate") {
    turnShell("Medytacja", meditate(solo));
    addTurnButtons([
      { id: "to3", label: "Dalej → faza III", primary: true, onClick: () => setTurn("III", "phase3") },
      { id: "back", label: "Wróć", onClick: () => setTurn("II", "phase2") },
    ]);
    return;
  }

  if (screen === "explore") {
    const here = locById(p.locationId);
    turnShell("Eksploracja", exploreGuideHtml(here, solo));
    addTurnButtons([
      { id: "to3", label: "Dalej → faza III", primary: true, onClick: () => setTurn("III", "phase3") },
      { id: "back", label: "Wróć", onClick: () => setTurn("II", "phase2") },
    ]);
    return;
  }

  if (screen === "phase3") {
    turnShell("Faza III — dobór i zakup", phase3(solo));
    const next = state.roster[(state.activeIndex + 1) % state.players];
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
        id: "to-board",
        label: "Stół — mapa / reputacja / karty",
        onClick: () => go("board"),
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
        state.board.monsters[locId] = { count: (cur.count || 0) + 1 };
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
      pl.nowDoing = "faza1";
      state.board.movePrompt = null;
      state.board.highlightFrom = null;
      state.board.selectedLoc = mp.toId;
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
