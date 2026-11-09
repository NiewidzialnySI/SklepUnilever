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
  const row = (items) =>
    `<div class="ico-legend-row">${items
      .map(([fn, label, hint]) => `<div class="ico-legend-item">${fn()}<div><strong>${label}</strong><span>${hint}</span></div></div>`)
      .join("")}</div>`;

  return `
    <p class="lead">Ikony poniżej to <strong>ściąga symboli</strong> przy stole. Żetony terenu to grafiki wygenerowane pod ten helper; reszta to uproszczone SVG — nie oryginalne assety z pudełka. Widzisz na karcie butelkę, kości albo „ogień”? Tu jest co to znaczy.</p>
    <p class="note">To nadal pomocnik, nie cyfrowa kopia gry. Tekstów i ilustracji kart nie kopiujemy — czytasz je z fizycznych kart.</p>

    <h3>Żetony terenu (3 sztuki — jak w pudełku)</h3>
    <p class="note">Na planszy / w stosach żetonów obszarów są tylko: <strong>las</strong>, <strong>góry</strong>, <strong>woda</strong>. Nie ma osobnego żetonu „wild”.</p>
    ${row([
      [ICO.forest, "Las", "Żeton: 3 świerki. Ruch: odrzuć kartę z lasem na sąsiada-las."],
      [ICO.mountain, "Góry", "Żeton: szczyty. Ruch: karta z górami."],
      [ICO.water, "Woda", "Żeton: fale. Ruch: karta z wodą (też ścieżki morskie)."],
    ])}
    <h3>Symbol na karcie (nie żeton)</h3>
    ${row([
      [ICO.wild, "Uniwersalny", "Na karcie: woda + las + góry w jednym okręgu = dowolny teren przy ruchu. To NIE jest 4. żeton z pudełka."],
    ])}

    <h3>Kolory kart akcji (walka / talia)</h3>
    ${row([
      [ICO.cardBlue, "Szybki atak", "Niebieski — często inicjatywa / szybkie uderzenie"],
      [ICO.cardRed, "Silny atak", "Czerwony — mocniejsze obrażenia"],
      [ICO.cardGreen, "Unik", "Zielony — unikanie / manewr"],
      [ICO.cardYellow, "Znak defensywny", "Żółty — obrona znakiem"],
      [ICO.cardPurple, "Znak ofensywny", "Fioletowy — atak znakiem"],
    ])}

    <h3>Eliksir, alchemia, kości</h3>
    ${row([
      [ICO.elixir, "Eliksir (butelka)", "Karta / żeton eliksiru. Masz max 4 na stole. Zużywasz w walce."],
      [ICO.alchemy, "Alchemia", "Atrybut: w całej walce łącznie ≤ tyle eliksirów ile alchemii. +1 alchemia → dobierz 1 eliksir."],
      [ICO.dice, "Kości / poker", "Kościany poker: 5 kości, układy jak w pokerze. Vs wiedźmin, miejscowi albo tabela solo."],
      [ICO.hand, "Ręka", "Karty na ręce. Limit 7 poza walką."],
    ])}
    <div class="tip">
      <strong>Gdy na karcie widać butelkę + kości:</strong> zwykle efekt związany z eliksirem albo z wynikiem kościanego pokera — <em>czytaj tekst karty</em> (ma pierwszeństwo). Alchemia mówi tylko <em>ile</em> eliksirów możesz łącznie wypić w tej walce; sam efekt jest na karcie eliksiru.
    </div>
    <details>
      <summary><strong>Układy kościanego pokera (nagrody solo)</strong></summary>
      <table class="table">
        <thead><tr><th>Układ</th><th>Co to jest</th><th>Solo — złoto z banku</th></tr></thead>
        <tbody>
          <tr><td>Para</td><td>2 takie same</td><td>0</td></tr>
          <tr><td>Dwie pary</td><td>2+2</td><td>1</td></tr>
          <tr><td>Trójka</td><td>3 takie same</td><td>2</td></tr>
          <tr><td>Mały strit</td><td>4 kolejne</td><td>3</td></tr>
          <tr><td>Duży strit</td><td>5 kolejnych</td><td>3</td></tr>
          <tr><td>Full</td><td>3+2</td><td>4</td></tr>
          <tr><td>Kareta</td><td>4 takie same</td><td>5</td></tr>
          <tr><td>Poker</td><td>5 takich samych</td><td>6</td></tr>
        </tbody>
      </table>
      <p class="note">Multi: poker z miejscowymi = 1 złoto + 2 z banku (sprawdź obszar). Poker z wiedźminem w fazie I → w tej turze <strong>nie walczysz</strong> z nim w fazie II.</p>
    </details>

    <h3>Usuwanie kart vs odrzucenie</h3>
    ${row([
      [ICO.burn, "Usuń z gry / burn / trash", "Karta wraca do pudełka na stałe (też zmęczenie)."],
      [ICO.discard, "Odrzuć", "Na Twój stos odrzuconych. Wraca do talii przy tasowaniu (np. pusta talia poza walką / po walce) — nie przy medytacji."],
      [ICO.fatigue, "Zmęczenie", "Przy wejściu na pole reputacji: usuń z gry tyle kart, ile mówi symbol przy polu (1→1, 2→2…)."],
      [ICO.reputation, "Reputacja", "Tor 0→4. 4. z walki wygrywa; medytacja jako „ostatnie” nie kończy gry."],
    ])}

    <h3>Znaki pod nazwami miejscowości (Gemini)</h3>
    <p class="note">To akcje <strong>fazy I</strong> — opcjonalne, raz na turę każdy konkretny znak. Wejście na pole z potworem <em>nie</em> wymusza walki.</p>
    ${row([
      [ICO.actionSchool, "Szkoła", "Zapłać złoto = poziom docelowy (2–5) → +1 atak/obrona/alchemia; specjalność tylko we własnej szkole."],
      [ICO.actionAttack, "+ Atak", "Darmowy trening: atak ≤ poziom wiedźmina → +1 atak (Ard Modron)."],
      [ICO.actionDefense, "+ Obrona", "Obrona ≤ poziom → +1 obrona (+1 tarcza) (Cidaris)."],
      [ICO.actionAlchemy, "+ Alchemia", "Alchemia ≤ poziom → +1 alchemia i dobierz 1 eliksir (Ban Ard)."],
      [ICO.actionSpecialty, "+ Specjalność", "Specjalność ≤ poziom → +1 specjalność (Glenmore)."],
      [ICO.actionElixir, "+ Eliksir", "Dobierz 1 kartę eliksiru (max 4) — Beauclair, Dhuwod, Cintra."],
      [ICO.actionDice, "Poker", "Kościany poker z miejscowymi (Novigrad, Loc Ichaer)."],
      [ICO.actionTrail, "Tropienie", "+1 złoto; opcja: 1 złoto → zadanie tropienia (Hengfors, Doldeth)."],
      [ICO.actionClean, "Wyzima", "0 złota → +1 złoto; możesz spalić karty z puli na polach z płomieniem."],
      [ICO.actionTrash, "Vengerberg", "Usuń 1 kartę z ręki → kup 1 z puli (koszt ≤+1 względem usuniętej)."],
    ])}

    <h3>Atrybuty i zasoby</h3>
    ${row([
      [ICO.attrAttackPng, "Atak", "Dobór kart w walce"],
      [ICO.attrDefensePng, "Obrona", "Limit tarczy; +1 obrona → +1 tarcza"],
      [ICO.attrAlchemyPng, "Alchemia", "Limit eliksirów w walce; +1 alchemia → +1 eliksir"],
      [ICO.attrSpecialtyPng, "Zdolność szkoły", "Użycie w walce: raz na walkę. Trening zdolności: tylko w swojej szkole."],
      [ICO.gold, "Złoto", "Bank / koszty / nagrody"],
      [ICO.shield, "Tarcza", "Blokuje obrażenia; po walce reset do obrony"],
      [ICO.trophy, "Trofeum", "Potwór / wiedźmin / medytacja → reputacja"],
    ])}

    <h3>Inne przy stole</h3>
    ${row([
      [ICO.move, "Ruch", "Faza I — odrzucasz karty z symbolem terenu i idziesz"],
      [ICO.monster, "Potwór", "Walka / figurka na mapie"],
      [ICO.quest, "Zadanie / trop", "Zadanie tropienia = żeton obszaru + złoto; po dojściu → żeton tropu (teren)"],
    ])}
    <div class="tip">
      <strong>Żeton tropu ≠ nowe żetony z pudełka.</strong> To zwykły <em>żeton obszaru</em> (las/góry/woda). Najpierw leży u Ciebie <em>nazwą lokacji do góry</em> (zadanie), potem po wejściu na tę lokację odwracasz go na symbol terenu = masz trop. Szczegóły: Ściąga / Leksykon.
    </div>

    <h3>Szkoły (medale Gemini)</h3>
    ${row([
      [ICO.schoolWilk, "Wilk", "Szermierka · start Kaer Morhen"],
      [ICO.schoolGryf, "Gryf", "Magia · start Kaer Seren"],
      [ICO.schoolNiedzwiedz, "Niedźwiedź", "Zbroja · start Haern Caduch"],
      [ICO.schoolKot, "Kot", "Szybkość · start Stygga"],
      [ICO.schoolZmija, "Żmija", "Zatruta stal · start Gorthur Gvaed"],
      [ICO.schoolMantikora, "Mantikora", "Behelt Nar (#0) — na mapie podstawki"],
    ])}
  `;
}
