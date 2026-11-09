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

const STORAGE_KEY = "wiedzmin-stary-swiat-helper-v2";

const state = loadState() || {
  view: "home",
  players: 3,
  setupIndex: 0,
  turn: {
    phase: "hub",
    screen: "hub",
  },
};

const appEl = document.getElementById("app");

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

function setPlayers(n) {
  state.players = n;
  state.setupIndex = 0;
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
  if (state.view === "home") renderHome();
  else if (state.view === "setup") renderSetup();
  else if (state.view === "turn") renderTurn();
  else if (state.view === "ref") renderRef();
  else renderHome();
}

function playersLabel(n) {
  if (n === 1) return "Solo";
  return `${n} graczy`;
}

function shell(inner, { showNav = true } = {}) {
  const nav = showNav
    ? `
    <div class="top-actions">
      <button class="btn btn-ghost" data-nav="home">Start</button>
      <button class="btn btn-ghost" data-nav="setup">Przygotowanie</button>
      <button class="btn btn-ghost" data-nav="turn">Tura</button>
      <button class="btn btn-ghost" data-nav="ref">Ściąga</button>
    </div>`
    : "";

  appEl.innerHTML = `
    <header class="topbar">
      <div class="brand">
        Wiedźmin: Stary Świat
        <small>Pomocnik rozgrywki · podstawka${isSolo() ? " · solo" : ""}</small>
      </div>
      ${nav}
    </header>
    ${inner}
    <p class="footer-note">Fanowski przewodnik na podstawie oficjalnej instrukcji. Nie jest produktem CD PROJEKT RED / Go On Board.</p>
  `;

  appEl.querySelectorAll("[data-nav]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const view = btn.getAttribute("data-nav");
      if (view === "setup") go("setup");
      else if (view === "turn") go("turn", { turn: { phase: "hub", screen: "hub" } });
      else if (view === "ref") go("ref");
      else go("home");
    });
  });
}

function renderHome() {
  const options = [
    { n: 1, title: "Solo", hint: "Wariant 1-osobowy" },
    { n: 2, title: "2 graczy", hint: "Szybka partia" },
    { n: 3, title: "3 graczy", hint: "Polecane na start" },
    { n: 4, title: "4 graczy", hint: "Po poznaniu zasad" },
    { n: 5, title: "5 graczy", hint: "Pełny stół" },
  ];

  shell(
    `
    <section class="panel home-hero">
      <p class="step-kicker">Pomocnik przy stole</p>
      <h1>Prowadzę Cię przez grę</h1>
      <p class="lead">Najpierw przygotowanie, potem tura po turze: co robić, która faza, co wolno. Działa też w <strong>solo</strong>.</p>
      <div class="badge-row">
        <span class="badge">Tylko podstawka</span>
        <span class="badge">Solo + 2–5</span>
        <span class="badge">Po polsku</span>
      </div>
      <h3>Ile osób gra?</h3>
      <div class="choice-grid" id="player-pick">
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
        <button class="btn btn-primary" id="start-setup">Zaczynamy przygotowanie</button>
        <button class="btn btn-secondary" id="skip-to-turn">Mam już rozłożone — prowadź turę</button>
      </div>
      <p class="tip">Postęp zapisuje się w tej przeglądarce.</p>
    </section>
  `,
    { showNav: true }
  );

  appEl.querySelectorAll("[data-players]").forEach((btn) => {
    btn.addEventListener("click", () => setPlayers(Number(btn.dataset.players)));
  });
  appEl.querySelector("#start-setup").addEventListener("click", () => {
    state.setupIndex = 0;
    go("setup");
  });
  appEl.querySelector("#skip-to-turn").addEventListener("click", () => {
    go("turn", { turn: { phase: "hub", screen: "hub" } });
  });
}

function renderSetup() {
  const steps = buildSetupSteps(state.players);
  const index = Math.min(state.setupIndex, steps.length - 1);
  const step = steps[index];
  const pct = Math.round(((index + 1) / steps.length) * 100);

  shell(`
    <section class="panel">
      <div class="progress">
        <div class="progress-meta">
          <span>Przygotowanie · ${playersLabel(state.players)}</span>
          <span>Krok ${index + 1} / ${steps.length}</span>
        </div>
        <div class="progress-bar"><span style="width:${pct}%"></span></div>
      </div>
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
      go("turn", { turn: { phase: "hub", screen: "hub" } });
    });
  }
}

function turnShell(title, body) {
  const tabs = [
    ["hub", "Start tury"],
    ["I", "Faza I"],
    ["II", "Faza II"],
    ["III", "Faza III"],
  ];
  const active = state.turn.phase;

  shell(`
    <section class="panel">
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

function bindChoices() {
  appEl.querySelectorAll(".choice[data-goto]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const goto = btn.getAttribute("data-goto");
      if (!goto) return;
      if (goto === "phase2") setTurn("II", "phase2");
      else if (goto === "move") setTurn("I", "after-move");
      else if (goto === "fight") setTurn("II", "fight");
      else if (goto === "meditate") setTurn("II", "meditate");
      else if (goto === "explore") setTurn("II", "explore");
      else if (goto === "fight-monster") setTurn("II", "fight-monster");
      else if (goto === "fight-witcher") setTurn("II", "fight-witcher");
      else if (goto === "phase3") setTurn("III", "phase3");
      else if (goto === "next-player") setTurn("hub", "hub");
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
    const id = btn.getAttribute("data-act");
    const conf = buttons.find((b) => b.id === id);
    if (conf) btn.addEventListener("click", conf.onClick);
  });
}

function renderTurn() {
  const { screen } = state.turn;
  const solo = isSolo();

  if (screen === "hub") {
    turnShell(
      solo ? "Twoja tura" : "Kto jest aktywnym graczem?",
      solo
        ? `
        <p class="lead">Rozgrywasz fazy I → II → III. <strong>Notuj numer tury</strong> na kartce.</p>
        <p>Start: <strong>5 kart</strong>, <strong>3 złota</strong>. Cel: 3 pokonane potwory + trofeum atrybutu.</p>
        <p class="tip">Kliknij <strong>Zaczynam fazę I</strong>.</p>
      `
        : `
        <p class="lead">Aktywny gracz: fazy I → II → III.</p>
        <h3>Startowe zasoby</h3>
        ${resourcesTable(state.players)}
        <p class="tip">Kliknij <strong>Faza I</strong>, gdy aktywny zaczyna.</p>
      `
    );
    addTurnButtons([
      {
        id: "go-i",
        label: "Zaczynam fazę I",
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
        <p class="lead">Na początku: <strong>1 obowiązkowy ruch</strong> albo od razu faza II.</p>
        <div class="choice-grid">
          <button class="choice" data-goto="move"><strong>Ruszam się</strong><span>Odrzucam karty i idę dalej</span></button>
          <button class="choice" data-goto="phase2"><strong>Idę do fazy II</strong><span>Bez ruchu w tej turze</span></button>
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
          <button class="choice" data-goto="move"><strong>Ruszam się znowu</strong><span>Mam karty i chcę iść dalej</span></button>
          <button class="choice" data-goto="phase2"><strong>Przechodzę do fazy II</strong><span>Zostawiam karty na ręce</span></button>
        </div>
        <p class="note">Brak kart na ręce → musisz iść do fazy II.</p>
        <details style="margin-top:1rem">
          <summary><strong>Ściąga akcji obszarów</strong></summary>
          ${LOCATION_ACTIONS}
        </details>
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
    turnShell("Walka — typ przeciwnika", FIGHT_BRANCH);
    bindChoices();
    addTurnButtons([
      { id: "back", label: "Wróć do wyboru fazy II", onClick: () => setTurn("II", "phase2") },
    ]);
    return;
  }

  if (screen === "fight-monster") {
    turnShell("Walka z potworem", fightMonster(solo));
    addTurnButtons([
      {
        id: "to3",
        label: "Koniec walki → faza III",
        primary: true,
        onClick: () => setTurn("III", "phase3"),
      },
      {
        id: "back",
        label: "Wróć",
        onClick: () => setTurn("II", solo ? "phase2" : "fight"),
      },
    ]);
    return;
  }

  if (screen === "fight-witcher") {
    turnShell("Walka z wiedźminem", FIGHT_WITCHER);
    addTurnButtons([
      {
        id: "to3",
        label: "Koniec walki → faza III",
        primary: true,
        onClick: () => setTurn("III", "phase3"),
      },
      { id: "back", label: "Wróć", onClick: () => setTurn("II", "fight") },
    ]);
    return;
  }

  if (screen === "meditate") {
    turnShell("Medytacja", meditate(solo));
    addTurnButtons([
      {
        id: "to3",
        label: "Dalej → faza III",
        primary: true,
        onClick: () => setTurn("III", "phase3"),
      },
      { id: "back", label: "Wróć", onClick: () => setTurn("II", "phase2") },
    ]);
    return;
  }

  if (screen === "explore") {
    turnShell("Eksploracja", explore(solo));
    addTurnButtons([
      {
        id: "to3",
        label: "Dalej → faza III",
        primary: true,
        onClick: () => setTurn("III", "phase3"),
      },
      { id: "back", label: "Wróć", onClick: () => setTurn("II", "phase2") },
    ]);
    return;
  }

  if (screen === "phase3") {
    turnShell("Faza III — dobór i zakup", phase3(solo));
    addTurnButtons([
      {
        id: "next",
        label: solo ? "Koniec tury — kolejna tura (+1)" : "Koniec tury — następny gracz",
        primary: true,
        onClick: () => setTurn("hub", "hub"),
      },
    ]);
  }
}

function resourcesTable(n) {
  const rows = START_RESOURCES[n]
    .map((r, i) => {
      const label = n === 1 ? "Ty (solo)" : PLAYER_LABELS[i];
      return `<tr><th>${label}</th><td>${r.cards} kart(y)</td><td>${r.gold} złota</td></tr>`;
    })
    .join("");
  return `<table class="table"><thead><tr><th>Gracz</th><th>Karty na start</th><th>Złoto</th></tr></thead><tbody>${rows}</tbody></table>`;
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
        <button class="btn btn-secondary" id="reset">Wyczyść zapis postępu</button>
      </div>
    </section>
  `);
  appEl.querySelector("#to-turn").addEventListener("click", () =>
    go("turn", { turn: { phase: "hub", screen: "hub" } })
  );
  appEl.querySelector("#reset").addEventListener("click", () => {
    localStorage.removeItem(STORAGE_KEY);
    Object.assign(state, {
      view: "home",
      players: 3,
      setupIndex: 0,
      turn: { phase: "hub", screen: "hub" },
    });
    render();
  });
}

render();
