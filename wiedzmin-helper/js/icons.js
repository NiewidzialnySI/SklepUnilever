/** Oryginalne ikony-ściągi (nie assety z pudełka) — ułatwiają rozpoznawanie symboli przy stole. */

const NS = 'xmlns="http://www.w3.org/2000/svg"';

function svg(inner, { size = 22, className = "", view = "0 0 24 24" } = {}) {
  return `<span class="ico ${className}" aria-hidden="true"><svg ${NS} width="${size}" height="${size}" viewBox="${view}" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">${inner}</svg></span>`;
}

export const ICO = {
  forest: () =>
    svg(
      `<path d="M12 21V11"/><path d="M12 11L7 16H17L12 11Z"/><path d="M12 8L8.5 12.5H15.5L12 8Z"/><path d="M12 5L9.5 8.5H14.5L12 5Z"/>`,
      { className: "ico-forest" }
    ),
  mountain: () =>
    svg(
      `<path d="M3 19L9 8L12 13L15 7L21 19H3Z"/><path d="M10.5 13.5L12 11L14 14"/>`,
      { className: "ico-mountain" }
    ),
  water: () =>
    svg(
      `<path d="M4 14c2-2 4-2 6 0s4 2 6 0 4-2 6 0"/><path d="M4 18c2-2 4-2 6 0s4 2 6 0 4-2 6 0"/><path d="M12 4c-2 3-4 5.5-4 8a4 4 0 008 0c0-2.5-2-5-4-8Z"/>`,
      { className: "ico-water" }
    ),
  wild: () =>
    svg(
      `<circle cx="12" cy="12" r="8"/><path d="M12 6v12M6 12h12M8.5 8.5l7 7M15.5 8.5l-7 7"/>`,
      { className: "ico-wild" }
    ),
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
  schoolWilk: () =>
    svg(
      `<circle cx="12" cy="12" r="9"/><path d="M7 13c1-4 3-6 5-6s4 2 5 6"/><path d="M9 11h.01M15 11h.01"/><path d="M10 15c.8.8 3.2.8 4 0"/>`,
      { className: "ico-school" }
    ),
  schoolGryf: () =>
    svg(
      `<circle cx="12" cy="12" r="9"/><path d="M8 14c2-5 6-5 8 0"/><path d="M12 7v3"/><path d="M9 10l3-2 3 2"/>`,
      { className: "ico-school" }
    ),
  schoolNiedzwiedz: () =>
    svg(
      `<circle cx="12" cy="12" r="9"/><circle cx="8" cy="9" r="1.6"/><circle cx="16" cy="9" r="1.6"/><circle cx="12" cy="13" r="3.2"/><path d="M10.5 13.5h3"/>`,
      { className: "ico-school" }
    ),
  schoolKot: () =>
    svg(
      `<circle cx="12" cy="12" r="9"/><path d="M8 14c0-3 8-3 8 0"/><path d="M8 10l-1.5-3 3 1"/><path d="M16 10l1.5-3-3 1"/><path d="M10 13h.01M14 13h.01"/>`,
      { className: "ico-school" }
    ),
  schoolZmija: () =>
    svg(
      `<circle cx="12" cy="12" r="9"/><path d="M8 15c2-1 3-4 4-4s2 3 4 4"/><path d="M12 7v4"/><circle cx="12" cy="15.5" r="1" fill="currentColor" stroke="none"/>`,
      { className: "ico-school" }
    ),
};

export const SCHOOL_ICON = {
  wilk: ICO.schoolWilk,
  gryf: ICO.schoolGryf,
  niedzwiedz: ICO.schoolNiedzwiedz,
  kot: ICO.schoolKot,
  zmija: ICO.schoolZmija,
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
    <p class="lead">Ikony poniżej to <strong>ściąga symboli</strong> z planszówki (uproszczone, własne SVG — nie grafiki z pudełka). Mają pomóc szybciej ogarnąć ruch, karty i atrybuty przy stole.</p>
    <p class="note">To nadal pomocnik, nie cyfrowa kopia gry. Karty i ilustracje zostają fizycznie przed Wami.</p>

    <h3>Teren (ruch)</h3>
    ${row([
      [ICO.forest, "Las", "Pasuje do symbolu lasu na karcie / obszarze"],
      [ICO.mountain, "Góry", "Symbol górskiego terenu"],
      [ICO.water, "Woda", "Symbol wodnego / morskiego"],
      [ICO.wild, "Uniwersalny", "Dziki symbol — dowolny sąsiadujący teren"],
    ])}

    <h3>Kolory kart akcji (walka / talia)</h3>
    ${row([
      [ICO.cardBlue, "Szybki atak", "Niebieski"],
      [ICO.cardRed, "Silny atak", "Czerwony"],
      [ICO.cardGreen, "Unik", "Zielony"],
      [ICO.cardYellow, "Znak defensywny", "Żółty"],
      [ICO.cardPurple, "Znak ofensywny", "Fioletowy"],
    ])}

    <h3>Atrybuty i zasoby</h3>
    ${row([
      [ICO.attack, "Atak", "Dobór kart w walce"],
      [ICO.defense, "Obrona", "Limit tarczy"],
      [ICO.alchemy, "Alchemia", "Eliksiry w walce"],
      [ICO.specialty, "Zdolność szkoły", "Raz na walkę"],
      [ICO.gold, "Złoto", "Bank / koszty"],
      [ICO.shield, "Tarcza", "Blokuje obrażenia"],
      [ICO.trophy, "Trofeum", "Droga do 4 reputacji"],
    ])}

    <h3>Inne przy stole</h3>
    ${row([
      [ICO.move, "Ruch", "Faza I — przesunięcie"],
      [ICO.dice, "Kościany poker", "Vs wiedźmin / miejscowi / solo"],
      [ICO.monster, "Potwór", "Walka / trop"],
      [ICO.quest, "Zadanie", "Eksploracja ze słowem „zadanie”"],
    ])}

    <h3>Szkoły</h3>
    ${row([
      [ICO.schoolWilk, "Wilk", "Szermierka"],
      [ICO.schoolGryf, "Gryf", "Magia"],
      [ICO.schoolNiedzwiedz, "Niedźwiedź", "Zbroja"],
      [ICO.schoolKot, "Kot", "Szybkość"],
      [ICO.schoolZmija, "Żmija", "Zatruta stal"],
    ])}
  `;
}
