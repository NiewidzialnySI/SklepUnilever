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
  explore,
  phase3,
  LOCATION_ACTIONS,
  quickRef,
  MOVE_HELP,
} from "./content.js";
import { SCHOOLS, sourcesHtml } from "./sources.js";
import { lexiconHtml, SCHOOL_SPECIALTIES } from "./lexicon.js";

const STORAGE_KEY = "wiedzmin-stary-swiat-helper-v3";

const state = loadState() || defaultState();

const appEl = document.getElementById("app");

function defaultState() {
  return {
    view: "home",
    players: 3,
    roster: [],
    activeIndex: 0,
    setupIndex: 0,
    turn: { phase: "hub", screen: "hub" },
  };
}

function isSolo() {
  return state.players === 1;
}

function loadState() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || "null");
  } catch {
    return null;
  }
}

function saveState() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
}

function ensureRoster() {
  const n = state.players;
  if (!Array.isArray(state.roster)) state.roster = [];
  while (state.roster.length < n) {
    const i = state.roster.length;
    state.roster.push({
      name: n === 1 ? "Ja" : `Gracz ${i + 1}`,
      schoolId: SCHOOLS[i % SCHOOLS.length].id,
    });
  }
  if (state.roster.length > n) state.roster = state.roster.slice(0, n);
  if (state.activeIndex >= n) state.activeIndex = 0;
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
  else if (state.view === "turn") renderTurn();
  else if (state.view === "ref") renderRef();
  else if (state.view === "lexicon") renderLexicon();
  else if (state.view === "sources") renderSources();
  else renderHome();
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
        <button class="btn btn-ghost" data-nav="home">Start</button>
        <button class="btn btn-ghost" data-nav="roster">Gracze</button>
        <button class="btn btn-ghost" data-nav="setup">Przygotowanie</button>
        <button class="btn btn-ghost" data-nav="turn">Tura</button>
        <button class="btn btn-ghost" data-nav="ref">Ściąga</button>
        <button class="btn btn-ghost" data-nav="lexicon">Leksykon</button>
        <button class="btn btn-ghost" data-nav="sources">Źródła</button>
      </div>
    </header>
    ${inner}
    <p class="footer-note">Fanowski przewodnik (instrukcja + FAQ Rebel). Nie jest produktem CD PROJEKT RED / Go On Board.</p>
  `;

  appEl.querySelectorAll("[data-nav]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const view = btn.getAttribute("data-nav");
      if (view === "turn") go("turn", { turn: { phase: "hub", screen: "hub" } });
      else go(view);
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
      <p class="lead">Wybierz liczbę graczy, przypisz <strong>imiona i szkoły</strong>, potem idziemy przez przygotowanie i tury — zgodnie z instrukcją i FAQ (tylko podstawka).</p>
      <div class="badge-row">
        <span class="badge">Podstawka</span>
        <span class="badge">Solo + 2–5</span>
        <span class="badge">Imiona + szkoły</span>
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
        <button class="btn btn-secondary" id="skip-setup">Mam rozłożone — prowadź turę</button>
      </div>
    </section>
  `);

  appEl.querySelectorAll("[data-players]").forEach((btn) => {
    btn.addEventListener("click", () => setPlayers(Number(btn.dataset.players)));
  });
  appEl.querySelector("#to-roster").addEventListener("click", () => go("roster"));
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
      <p class="lead">Dla każdego gracza wpisz imię i wybierz szkołę wiedźmińską (jak na planszetce).</p>
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
      const name = card.querySelector('[data-field="name"]').value.trim() || `Gracz ${i + 1}`;
      const schoolId = card.querySelector('[data-field="schoolId"]').value;
      state.roster[i] = { name, schoolId };
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
    state.setupIndex = 0;
    go("setup");
  });
  appEl.querySelector("#to-turn").addEventListener("click", () => {
    if (!syncFromDom()) return;
    go("turn", { turn: { phase: "hub", screen: "hub" } });
  });
}

function escapeAttr(s) {
  return String(s).replace(/"/g, "&quot;").replace(/</g, "&lt;");
}

function renderSetup() {
  const steps = buildSetupSteps(state.players, state.roster);
  const index = Math.min(state.setupIndex, steps.length - 1);
  const step = steps[index];
  const pct = Math.round(((index + 1) / steps.length) * 100);
  const rosterHint = state.roster
    .map((p) => `<span class="player-chip">${escapeHtml(p.name)} <span>${schoolById(p.schoolId).short}</span></span>`)
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
    ["hub", "Start tury"],
    ["I", "Faza I"],
    ["II", "Faza II"],
    ["III", "Faza III"],
  ];
  const active = state.turn.phase;
  const p = activePlayer();
  const school = schoolById(p.schoolId);

  shell(`
    <section class="panel">
      <div class="badge-row">
        <span class="player-chip">${escapeHtml(p.name)} <span>${school.name}</span></span>
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

function setTurn(phase, screen) {
  state.turn = { phase, screen };
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
      if (goto === "phase2") setTurn("II", "phase2");
      else if (goto === "move") setTurn("I", "after-move");
      else if (goto === "fight") setTurn("II", "fight");
      else if (goto === "meditate") setTurn("II", "meditate");
      else if (goto === "explore") setTurn("II", "explore");
      else if (goto === "fight-monster") setTurn("II", "fight-monster");
      else if (goto === "fight-witcher") setTurn("II", "fight-witcher");
      else if (goto === "phase3") setTurn("III", "phase3");
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
        label: `Zaczynam fazę I (${p.name})`,
        primary: true,
        onClick: () => setTurn("I", "phase1-start"),
      },
    ]);
    return;
  }

  if (screen === "phase1-start") {
    turnShell(
      "Faza I — start",
      `
        <p class="lead"><strong>${escapeHtml(p.name)}</strong>: obowiązkowy ruch albo od razu faza II.</p>
        <div class="choice-grid">
          <button class="choice" data-goto="move"><strong>Ruszam się</strong><span>Odrzucam karty i idę</span></button>
          <button class="choice" data-goto="phase2"><strong>Idę do fazy II</strong><span>Bez ruchu</span></button>
        </div>
        ${MOVE_HELP}
      `
    );
    bindChoices();
    return;
  }

  if (screen === "after-move") {
    turnShell(
      "Faza I — po ruchu",
      `
        ${phase1AfterMove(solo)}
        <div class="choice-grid">
          <button class="choice" data-goto="move"><strong>Ruszam się znowu</strong><span>Mam karty</span></button>
          <button class="choice" data-goto="phase2"><strong>Przechodzę do fazy II</strong><span>Zostawiam rękę</span></button>
        </div>
        <details style="margin-top:1rem"><summary><strong>Akcje obszarów</strong></summary>${LOCATION_ACTIONS}</details>
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
    turnShell("Walka z potworem", fightMonster(solo));
    addTurnButtons([
      { id: "to3", label: "Koniec walki → faza III", primary: true, onClick: () => setTurn("III", "phase3") },
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
    turnShell("Eksploracja", explore(solo));
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
          : `Koniec tury → ${next.name}`,
        primary: true,
        onClick: () => nextPlayer(),
      },
    ]);
  }
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
  appEl.querySelector("#to-turn").addEventListener("click", () =>
    go("turn", { turn: { phase: "hub", screen: "hub" } })
  );
  appEl.querySelector("#to-sources").addEventListener("click", () => go("sources"));
  appEl.querySelector("#reset").addEventListener("click", () => {
    localStorage.removeItem(STORAGE_KEY);
    Object.assign(state, defaultState());
    render();
  });
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
  appEl.querySelector("#to-turn").addEventListener("click", () =>
    go("turn", { turn: { phase: "hub", screen: "hub" } })
  );
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
