/** Ikony-ściągi przy stole. Teren = PNG z Gemini; reszta = proste SVG (nie assety z pudełka). */

const NS = 'xmlns="http://www.w3.org/2000/svg"';

function svg(inner, { size = 28, className = "", view = "0 0 24 24" } = {}) {
  return `<span class="ico ${className}" aria-hidden="true"><svg ${NS} width="${size}" height="${size}" viewBox="${view}" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">${inner}</svg></span>`;
}

/** Żetony terenu / uniwersalny — gotowe PNG (jedna ikona, bez tła). */
function terrainPng(file, className, size = 28) {
  return `<span class="ico ico-terrain ${className}" aria-hidden="true"><img class="ico-img" src="./img/terrain/${file}.png" alt="" width="${size}" height="${size}" decoding="async" /></span>`;
}

/** Medale szkół — PNG z Gemini (bez tła). */
function schoolPng(file, className, size = 28) {
  return `<span class="ico ico-school-img ${className}" aria-hidden="true"><img class="ico-img" src="./img/schools/${file}.png" alt="" width="${size}" height="${size}" decoding="async" /></span>`;
}

/** Znaki pod nazwami miejscowości — PNG z Gemini (bez tła). */
function actionPng(file, className = "", { width = 72, height = 28 } = {}) {
  return `<span class="ico ico-action ${className}" aria-hidden="true"><img class="ico-img ico-action-img" src="./img/actions/${file}.png" alt="" width="${width}" height="${height}" decoding="async" /></span>`;
}

/** actionKey z map-data → plik PNG znaku lokacji. */
export const ACTION_ICON_FILE = {
  schoolTrain: "action-school-train-b",
  trailGold: "action-trail-gold",
  dicePoker: "action-dice-poker",
  cleanPool: "action-clean-pool",
  trashDraw: "action-trash-draw",
  defenseLevel: "action-defense-level",
  alchemyLevel: "action-alchemy-level",
  specialtyLevel: "action-specialty-level",
  attackLevel: "action-attack-level",
  gainElixir: "action-gain-elixir",
  cintraElixir: "action-gain-elixir",
};

export function actionIconHtml(actionKey, opts = {}) {
  const file = ACTION_ICON_FILE[actionKey];
  if (!file) return "";
  const wide = ["schoolTrain", "trailGold", "cleanPool", "trashDraw"].includes(actionKey);
  return actionPng(file, `ico-action-${actionKey}`, {
    width: opts.width ?? (wide ? 110 : 72),
    height: opts.height ?? 32,
  });
}

export const ICO = {
  /** Żeton las — asset Gemini (3 świerki). */
  forest: () => terrainPng("forest", "ico-forest"),
  /** Żeton góry — asset Gemini (szczyty ze śniegiem). */
  mountain: () => terrainPng("mountain", "ico-mountain"),
  /** Żeton woda — asset Gemini (fale). */
  water: () => terrainPng("water", "ico-water"),
  /** Symbol na karcie: las + góry + woda w jednym okręgu (asset Gemini). */
  wild: () => terrainPng("wild", "ico-wild"),
  gold: () =>
    svg(
      `<circle cx="12" cy="12" r="8"/><circle cx="12" cy="12" r="4.5"/><path d="M12 8.5v7M9.5 10.5c.6-.7 1.5-1 2.5-1s2 .4 2.5 1M9.5 13.5c.6.7 1.5 1 2.5 1s2-.4 2.5-1"/>`,
      { className: "ico-gold" }
    ),
  shield: () =>
    svg(
      `<path d="M12 3l7 3v5c0 4.5-3 7.5-7 9-4-1.5-7-4.5-7-9V6l7-3Z"/>`,
      { className: "ico-shield" }
    ),
  attack: () =>
    svg(
      `<path d="M14.5 4.5l5 5"/><path d="M16 6l2.5 2.5"/><path d="M13 7L6 14l-1.5 5.5L10 18l7-7"/><path d="M9 15l-2 2"/>`,
      { className: "ico-attack" }
    ),
  defense: () =>
    svg(
      `<path d="M12 3l7 3v5c0 4.5-3 7.5-7 9-4-1.5-7-4.5-7-9V6l7-3Z"/><path d="M9 12l2 2 4-4"/>`,
      { className: "ico-defense" }
    ),
  alchemy: () =>
    svg(
      `<path d="M9 3h6"/><path d="M10 3v4L6 17a4 4 0 004 4h4a4 4 0 004-4l-4-10V3"/><path d="M8.5 14h7"/>`,
      { className: "ico-alchemy" }
    ),
  specialty: () =>
    svg(
      `<path d="M12 3l2.2 4.5L19 8.5l-3.5 3.4.8 4.9L12 14.8 7.7 16.8l.8-4.9L5 8.5l4.8-1L12 3Z"/>`,
      { className: "ico-specialty" }
    ),
  cardBlue: () =>
    svg(
      `<rect x="5" y="3" width="14" height="18" rx="2"/><path d="M9 8h6M9 12h6M9 16h4"/>`,
      { className: "ico-card-blue" }
    ),
  cardRed: () =>
    svg(
      `<rect x="5" y="3" width="14" height="18" rx="2"/><path d="M12 8v8M9 11h6"/>`,
      { className: "ico-card-red" }
    ),
  cardGreen: () =>
    svg(
      `<rect x="5" y="3" width="14" height="18" rx="2"/><path d="M8 14c2-4 6-4 8 0"/><circle cx="12" cy="9" r="1.5"/>`,
      { className: "ico-card-green" }
    ),
  cardYellow: () =>
    svg(
      `<rect x="5" y="3" width="14" height="18" rx="2"/><path d="M12 7v5l3 2"/><circle cx="12" cy="17" r="1"/>`,
      { className: "ico-card-yellow" }
    ),
  cardPurple: () =>
    svg(
      `<rect x="5" y="3" width="14" height="18" rx="2"/><path d="M9 16c0-3 6-3 6-7a3 3 0 10-6 0"/>`,
      { className: "ico-card-purple" }
    ),
  monster: () =>
    svg(
      `<circle cx="12" cy="10" r="5"/><path d="M8 14c0 3 2.5 5 4 5s4-2 4-5"/><path d="M9 9h.01M15 9h.01"/><path d="M6 7l2 1M18 7l-2 1"/>`,
      { className: "ico-monster" }
    ),
  quest: () =>
    svg(
      `<path d="M8 4h8a2 2 0 012 2v14l-6-3-6 3V6a2 2 0 012-2Z"/><path d="M10 9h4M10 13h3"/>`,
      { className: "ico-quest" }
    ),
  dice: () =>
    svg(
      `<rect x="4" y="4" width="16" height="16" rx="3"/><circle cx="9" cy="9" r="1.2" fill="currentColor" stroke="none"/><circle cx="15" cy="9" r="1.2" fill="currentColor" stroke="none"/><circle cx="12" cy="12" r="1.2" fill="currentColor" stroke="none"/><circle cx="9" cy="15" r="1.2" fill="currentColor" stroke="none"/><circle cx="15" cy="15" r="1.2" fill="currentColor" stroke="none"/>`,
      { className: "ico-dice" }
    ),
  move: () =>
    svg(
      `<path d="M5 12h11"/><path d="M12 7l5 5-5 5"/><circle cx="5" cy="12" r="1.5" fill="currentColor" stroke="none"/>`,
      { className: "ico-move" }
    ),
  trophy: () =>
    svg(
      `<path d="M8 5h8v3a4 4 0 01-8 0V5Z"/><path d="M8 6H5a2 2 0 002 3"/><path d="M16 6h3a2 2 0 01-2 3"/><path d="M12 12v3"/><path d="M9 20h6"/><path d="M10 17h4v3h-4z"/>`,
      { className: "ico-trophy" }
    ),
  phase1: () =>
    svg(`<circle cx="12" cy="12" r="9"/><path d="M11 8v8"/><path d="M9.5 8H13"/>`, { className: "ico-phase" }),
  phase2: () =>
    svg(
      `<circle cx="12" cy="12" r="9"/><path d="M9 9.5c0-1.5 1.2-2.5 3-2.5s3 1 3 2.5-1.2 2-3 2.5-3 1.2-3 2.5 1.2 2.5 3 2.5 3-1 3-2.5"/>`,
      { className: "ico-phase" }
    ),
  phase3: () =>
    svg(
      `<circle cx="12" cy="12" r="9"/><path d="M9 9.5c.4-1.4 1.5-2.5 3.2-2.5 1.7 0 2.8 1 2.8 2.4S14 12 12.2 12H11"/><path d="M9 14.5c.4 1.4 1.5 2.5 3.2 2.5 1.7 0 2.8-1 2.8-2.4"/>`,
      { className: "ico-phase" }
    ),
  schoolWilk: () => schoolPng("wilk", "ico-school"),
  schoolGryf: () => schoolPng("gryf", "ico-school"),
  schoolNiedzwiedz: () => schoolPng("niedzwiedz", "ico-school"),
  schoolKot: () => schoolPng("kot", "ico-school"),
  schoolZmija: () => schoolPng("zmija", "ico-school"),
  schoolMantikora: () => schoolPng("mantikora", "ico-school"),
  /** Znaki akcji obszarów (Gemini). */
  actionAttack: () => actionPng("action-attack-level", "ico-loc-action", { width: 72, height: 32 }),
  actionDefense: () => actionPng("action-defense-level", "ico-loc-action", { width: 72, height: 32 }),
  actionAlchemy: () => actionPng("action-alchemy-level", "ico-loc-action", { width: 72, height: 32 }),
  actionSpecialty: () => actionPng("action-specialty-level", "ico-loc-action", { width: 72, height: 32 }),
  actionSchool: () => actionPng("action-school-train-b", "ico-loc-action", { width: 110, height: 32 }),
  actionElixir: () => actionPng("action-gain-elixir", "ico-loc-action", { width: 72, height: 32 }),
  actionDice: () => actionPng("action-dice-poker", "ico-loc-action", { width: 56, height: 32 }),
  actionTrail: () => actionPng("action-trail-gold", "ico-loc-action", { width: 110, height: 32 }),
  actionTrash: () => actionPng("action-trash-draw", "ico-loc-action", { width: 110, height: 32 }),
  actionClean: () => actionPng("action-clean-pool", "ico-loc-action", { width: 110, height: 32 }),
  actionQuest: () => actionPng("action-quest-mark", "ico-loc-action", { width: 56, height: 32 }),
  attrAttackPng: () => actionPng("attr-attack", "", { width: 28, height: 28 }),
  attrDefensePng: () => actionPng("attr-defense", "", { width: 28, height: 28 }),
  attrAlchemyPng: () => actionPng("attr-alchemy", "", { width: 28, height: 28 }),
  attrSpecialtyPng: () => actionPng("attr-specialty", "", { width: 28, height: 28 }),
  elixir: () =>
    svg(
      `<path d="M10 3h4"/><path d="M11 3v3.5L7.5 14a4.2 4.2 0 003.8 6h1.4a4.2 4.2 0 003.8-6L13 6.5V3"/><path d="M9 15c1.5 1 4.5 1 6 0"/><circle cx="12" cy="17" r="0.9" fill="currentColor" stroke="none"/>`,
      { className: "ico-elixir" }
    ),
  burn: () =>
    svg(
      `<path d="M12 21c3.5 0 5.5-2.2 5.5-5.2 0-2.6-1.6-4.2-3.2-5.6.2 1.6-.4 2.6-1.3 3.2.1-2.4-1.1-4.4-3.5-6.4 0 2.8-1.2 4.4-2.4 5.6C5.8 14.2 6.5 21 12 21Z"/>`,
      { className: "ico-burn" }
    ),
  discard: () =>
    svg(
      `<rect x="6" y="4" width="12" height="16" rx="2"/><path d="M9 9l6 6M15 9l-6 6"/>`,
      { className: "ico-discard" }
    ),
  fatigue: () =>
    svg(
      `<path d="M5 19h14"/><path d="M8 19V9l4-4 4 4v10"/><path d="M10 13h4M10 16h4"/>`,
      { className: "ico-fatigue" }
    ),
  reputation: () =>
    svg(
      `<circle cx="12" cy="12" r="9"/><path d="M8 14l2.5-5L12 12l1.5-3L16 14"/><path d="M7 17h10"/>`,
      { className: "ico-reputation" }
    ),
  hand: () =>
    svg(
      `<path d="M7 11V8.5a1.5 1.5 0 013 0V11"/><path d="M10 11V7.5a1.5 1.5 0 013 0V11"/><path d="M13 11V8a1.5 1.5 0 013 0v6.5a4.5 4.5 0 01-4.5 4.5H12a5 5 0 01-5-5V12"/><path d="M7 12H5.5A1.5 1.5 0 004 13.5v.5"/>`,
      { className: "ico-hand" }
    ),
};

export const SCHOOL_ICON = {
  wilk: ICO.schoolWilk,
  gryf: ICO.schoolGryf,
  niedzwiedz: ICO.schoolNiedzwiedz,
  kot: ICO.schoolKot,
  zmija: ICO.schoolZmija,
  mantikora: ICO.schoolMantikora,
};

export function chip(iconFn, label) {
  return `<span class="ico-chip">${iconFn()}${label}</span>`;
}

export function iconsLegendHtml() {
  const row = (items, rowClass = "") =>
    `<div class="ico-legend-row${rowClass ? ` ${rowClass}` : ""}">${items
      .map(([fn, label, hint]) => {
        const text = hint
          ? `<div class="ico-legend-text"><strong>${label}</strong><span>${hint}</span></div>`
          : `<div class="ico-legend-text"><strong>${label}</strong></div>`;
        return `<div class="ico-legend-item"><div class="ico-legend-ico">${fn()}</div>${text}</div>`;
      })
      .join("")}</div>`;

  return `
    <h3>Teren</h3>
    ${row(
      [
        [ICO.forest, "Las"],
        [ICO.mountain, "Góry"],
        [ICO.water, "Woda"],
        [ICO.wild, "Uniwersalny"],
      ],
      "ico-legend-row--terrain"
    )}

    <h3>Znaki lokacji</h3>
    <p class="note">Akcje fazy I — opcjonalne, raz na turę. Walka z potworem to osobny wybór w fazie II.</p>
    ${row([
      [ICO.actionSchool, "Szkoła", "Złoto = poziom docelowy → +1 atak/obrona/alchemia; specjalność tylko we własnej szkole."],
      [ICO.actionAttack, "+ Atak", "Atak ≤ poziom → +1 atak (Ard Modron)."],
      [ICO.actionDefense, "+ Obrona", "Obrona ≤ poziom → +1 obrona (Cidaris)."],
      [ICO.actionAlchemy, "+ Alchemia", "Alchemia ≤ poziom → +1 alchemia + eliksir (Ban Ard)."],
      [ICO.actionSpecialty, "+ Specjalność", "Specjalność ≤ poziom → +1 (Glenmore)."],
      [ICO.actionElixir, "+ Eliksir", "Dobierz 1 eliksir, max 4 (Beauclair, Dhuwod, Cintra)."],
      [ICO.actionDice, "Poker", "Kościany poker z miejscowymi (Novigrad, Loc Ichaer)."],
      [ICO.actionTrail, "Tropienie", "+1 złoto; opcja 1 złoto → zadanie tropienia (Hengfors, Doldeth)."],
      [ICO.actionClean, "Wyzima", "0 złota → +1; możesz spalić karty z puli (płomienie)."],
      [ICO.actionTrash, "Vengerberg", "Usuń 1 z ręki → kup 1 z puli (koszt ≤+1)."],
    ])}

    <h3>Atrybuty i zasoby</h3>
    ${row([
      [ICO.attrAttackPng, "Atak", "Dobór kart w walce"],
      [ICO.attrDefensePng, "Obrona", "+1 obrona → +1 tarcza"],
      [ICO.attrAlchemyPng, "Alchemia", "Limit eliksirów w walce"],
      [ICO.attrSpecialtyPng, "Zdolność szkoły", "Raz na walkę; trening tylko w swojej szkole"],
      [ICO.gold, "Złoto"],
      [ICO.shield, "Tarcza", "Po walce reset do obrony"],
      [ICO.trophy, "Trofeum"],
      [ICO.elixir, "Eliksir", "Max 4"],
      [ICO.dice, "Kości"],
      [ICO.hand, "Ręka", "Limit 7"],
    ])}

    <h3>Karty i reputacja</h3>
    ${row([
      [ICO.burn, "Usuń z gry", "Do pudełka na stałe"],
      [ICO.discard, "Odrzuć", "Na stos odrzuconych"],
      [ICO.fatigue, "Zmęczenie", "Usuń karty wg toru reputacji"],
      [ICO.reputation, "Reputacja", "4 z walki = wygrana"],
      [ICO.move, "Ruch"],
      [ICO.monster, "Potwór"],
      [ICO.quest, "Zadanie"],
    ])}

    <h3>Szkoły</h3>
    ${row(
      [
        [ICO.schoolWilk, "Wilk", "Szermierka · Kaer Morhen"],
        [ICO.schoolGryf, "Gryf", "Magia · Kaer Seren"],
        [ICO.schoolNiedzwiedz, "Niedźwiedź", "Zbroja · Haern Caduch"],
        [ICO.schoolKot, "Kot", "Szybkość · Stygga"],
        [ICO.schoolZmija, "Żmija", "Zatruta stal · Gorthur Gvaed"],
        [ICO.schoolMantikora, "Mantikory", "Behelt Nar (#0)"],
      ],
      "ico-legend-row--schools"
    )}
  `;
}
