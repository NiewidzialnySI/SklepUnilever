/**
 * Prowadzenie za rękę: koszt ruchu (klikany wybór) + kroki wg znaków na polu.
 */
import { ICO, actionIconHtml } from "./icons.js";
import {
  TERRAIN,
  locById,
  neighborsOf,
  actionHintFor,
  LOCATION_ACTION_HINTS,
} from "./map-data.js";


export const MOVE_COSTS = [
  {
    id: "match",
    title: "1 karta terenu",
    short: "Pasujący las / góry / woda",
    cards: 1,
    gold: 0,
    tip: "Odrzuć 1 kartę z ikoną terenu takiego jak miejsce, na które idziesz.",
  },
  {
    id: "wild",
    title: "1 karta uniwersalna",
    short: "Woda+las+góry na karcie",
    cards: 1,
    gold: 0,
    tip: "Symbol uniwersalny = woda + las + góry w jednym okręgu. Liczy się jak dowolny teren. To nie żeton z planszy.",
  },
  {
    id: "two",
    title: "2 dowolne karty",
    short: "Bez dopasowania terenu",
    cards: 2,
    gold: 0,
    tip: "Odrzuć dowolne 2 karty → możesz wejść na dowolnego sąsiada.",
  },
  {
    id: "card_gold",
    title: "1 karta + 1 złoto",
    short: "Mieszany koszt",
    cards: 1,
    gold: 1,
    tip: "Odrzuć 1 dowolną kartę i zapłać 1 złoto → dowolny sąsiad.",
  },
];

export function moveCostById(id) {
  return MOVE_COSTS.find((c) => c.id === id);
}

/** Zastosuj koszt ruchu do trackera gracza. Zwraca { ok, reason }. */
export function applyMoveCost(player, costId) {
  const cost = moveCostById(costId);
  if (!player || !cost) return { ok: false, reason: "Brak gracza / kosztu." };
  if ((player.cards || 0) < cost.cards) {
    return { ok: false, reason: `Za mało kart na ręce (potrzeba ${cost.cards}).` };
  }
  if ((player.gold || 0) < cost.gold) {
    return { ok: false, reason: `Za mało złota (potrzeba ${cost.gold}).` };
  }
  player.cards = Math.max(0, (player.cards || 0) - cost.cards);
  player.gold = Math.max(0, (player.gold || 0) - cost.gold);
  player.lastMoveCost = costId;
  return { ok: true, cost };
}

function escapeHtml(s) {
  return String(s)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");
}

function terrainChip(terrainId) {
  const t = TERRAIN[terrainId];
  if (!t) return "";
  const ico =
    terrainId === "forest"
      ? ICO.forest()
      : terrainId === "mountain"
        ? ICO.mountain()
        : terrainId === "water"
          ? ICO.water()
          : "";
  return `${ico} <strong>${escapeHtml(t.label)}</strong>`;
}

/** Panel wyboru kosztu ruchu (przyciski data-move-cost). */
export function moveCostChooserHtml({ fromLoc, toLoc, playerName, selectedCostId }) {
  const to = toLoc;
  const from = fromLoc;
  const terrainLine = to
    ? `Cel: <strong>${escapeHtml(to.name)}</strong> (#${to.number}) — teren ${terrainChip(to.terrain)}`
    : "Wybierz sąsiada na mapie / liście.";
  const fromLine = from
    ? `Z: <strong>${escapeHtml(from.name)}</strong> (#${from.number})`
    : "";

  return `
    <div class="guide-box guide-move" role="region" aria-label="Koszt ruchu">
      <p class="step-kicker" style="margin:0">Ruch — prowadzenie</p>
      <h3 style="margin:0.25rem 0 0.4rem">Dokąd idziesz${playerName ? `, ${escapeHtml(playerName)}` : ""}?</h3>
      <p class="lead" style="margin:0">${terrainLine}</p>
      ${fromLine ? `<p class="note" style="margin:0.35rem 0 0">${fromLine}</p>` : ""}
      <p class="tip" style="margin:0.65rem 0 0.4rem"><strong>Za ten ruch musisz coś oddać.</strong> Kliknij, co wybrałeś z ręki:</p>
      <div class="choice-grid move-cost-grid">
        ${MOVE_COSTS.map(
          (c) => `
          <button type="button" class="choice ${selectedCostId === c.id ? "active" : ""}" data-move-cost="${c.id}">
            <strong>${escapeHtml(c.title)}</strong>
            <span>${escapeHtml(c.short)}</span>
            <span class="note">${escapeHtml(c.tip)}</span>
          </button>`
        ).join("")}
      </div>
      <p class="note" style="margin:0.55rem 0 0">Po kliknięciu helper odejmie karty / złoto w trackerze (możesz potem poprawić ręcznie).</p>
    </div>
  `;
}

function affordNote(player, cardsNeed, goldNeed) {
  if (!player) return "";
  const cardsOk = (player.cards || 0) >= cardsNeed;
  const goldOk = (player.gold || 0) >= goldNeed;
  if (cardsOk && goldOk) return "masz zasoby";
  const miss = [];
  if (!cardsOk) miss.push(`kart ${player.cards || 0}/${cardsNeed}`);
  if (!goldOk) miss.push(`złota ${player.gold || 0}/${goldNeed}`);
  return `uwaga: ${miss.join(", ")}`;
}

/** Podgląd celu ruchu: akcja pola, potwór, inni wiedźmini. */
export function destinationPreviewHtml(toId, { player, roster = [], monsters = {} } = {}) {
  const to = locById(toId);
  if (!to) return `<p class="warn">Nieznane pole.</p>`;
  const mon = monsters?.[toId];
  const others = (roster || []).filter(
    (r) => r && r !== player && r.locationId === toId
  );
  const t = TERRAIN[to.terrain];
  return `
    <div class="guide-box guide-dest-preview">
      <p class="step-kicker" style="margin:0">Cel ruchu</p>
      <h3 style="margin:0.25rem 0 0.4rem">${escapeHtml(to.name)} (#${to.number})</h3>
      <p class="lead" style="margin:0">Teren: ${terrainChip(to.terrain)} ${t ? escapeHtml(t.label) : ""}</p>
      ${
        mon
          ? `<p class="warn" style="margin-top:0.55rem">${ICO.monster()} <strong>Potwór</strong> na polu${
              mon.level ? ` (poz. ${escapeHtml(String(mon.level))})` : ""
            } — w fazie II możesz walczyć (opcjonalnie).</p>`
          : ""
      }
      ${
        others.length
          ? `<p class="warn" style="margin-top:0.55rem">${ICO.specialty()} Inny wiedźmin: <strong>${others
              .map((o) => escapeHtml(o.name))
              .join(", ")}</strong> — możliwa walka / poker.</p>`
          : ""
      }
      ${locationActionGuideHtml(to, { heading: "Co zyskasz / zrobisz na tym polu (znak)" })}
    </div>
  `;
}

/** Krótki opis lokacji do dymekka nad mapą (Faza I — wybór celu). */
export function mapLocPopupHtml(toId, { player, roster = [], monsters = {} } = {}) {
  const to = locById(toId);
  if (!to) return "";
  const mon = monsters?.[toId];
  const others = (roster || []).filter(
    (r) => r && r !== player && r.locationId === toId
  );
  const t = TERRAIN[to.terrain];
  const hint = actionHintFor(to) || LOCATION_ACTION_HINTS[to.actionKey] || "";
  return `
    <p class="step-kicker" style="margin:0">Cel ruchu</p>
    <h3 class="map-loc-popup-title">${escapeHtml(to.name)} <span class="loc-num">#${to.number}</span></h3>
    <p class="map-loc-popup-meta">${terrainChip(to.terrain)} ${t ? escapeHtml(t.label) : ""}${
      to.school ? " · szkoła" : ""
    }</p>
    ${
      to.actionKey
        ? `<div class="map-loc-popup-action">${actionIconHtml(to.actionKey, { width: 100, height: 34 })}${
            hint ? `<p class="map-loc-popup-hint">${escapeHtml(hint)}</p>` : ""
          }</div>`
        : hint
          ? `<p class="map-loc-popup-hint"><strong>Znak:</strong> ${escapeHtml(hint)}</p>`
          : ""
    }
    ${
      mon
        ? `<p class="warn" style="margin:0.45rem 0 0">${ICO.monster()} <strong>Potwór</strong>${
            mon.level ? ` (poz. ${escapeHtml(String(mon.level))})` : ""
          } — walka opcjonalna (faza II)</p>`
        : ""
    }
    ${
      others.length
        ? `<p class="warn" style="margin:0.45rem 0 0">${ICO.specialty()} Inny wiedźmin: <strong>${others
            .map((o) => escapeHtml(o.name))
            .join(", ")}</strong></p>`
        : ""
    }
  `;
}

/** Lista sąsiadów z kosztem ruchu per cel (data-move-to). */
export function neighborPickerHtml(fromId, { player, monsters } = {}) {
  const from = locById(fromId);
  if (!from) {
    return `<p class="warn">Brak pozycji startowej — wróć do <strong>Gracze</strong> i wybierz szkołę (pionek stawia się automatycznie na szkole).</p>`;
  }
  const neigh = neighborsOf(fromId)
    .map((id) => locById(id))
    .filter(Boolean);
  if (!neigh.length) {
    return `<p class="warn">Brak sąsiadów w schemacie — sprawdź mapę.</p>`;
  }
  const hasMonster = (locId) => {
    const m = monsters?.[locId];
    return Boolean(m && m.count > 0);
  };
  return `
    <div class="guide-box guide-destinations">
      <p class="step-kicker" style="margin:0">Dokąd możesz iść</p>
      <p class="lead" style="margin:0.35rem 0 0">Stoisz na <strong>${escapeHtml(from.name)}</strong> (#${from.number}). Kliknij cel — potem wybierzesz dokładny koszt z ręki.</p>
      <div class="choice-grid move-dest-grid">
        ${neigh
          .map((n) => {
            const t = TERRAIN[n.terrain];
            const tLabel = t?.label || n.terrain;
            const matchOk = affordNote(player, 1, 0);
            const twoOk = affordNote(player, 2, 0);
            const mixOk = affordNote(player, 1, 1);
            const mon = hasMonster(n.id);
            return `
          <button type="button" class="choice ${mon ? "choice-danger" : ""}" data-move-to="${n.id}">
            <strong>${escapeHtml(n.name)} (#${n.number})${mon ? ` ${ICO.monster()} potwór!` : ""}</strong>
            <span>Teren celu: ${terrainChip(n.terrain)}</span>
            ${
              mon
                ? `<span class="warn" style="margin:0.25rem 0 0">Wejście → helper odpala walkę</span>`
                : ""
            }
            <span class="move-cost-lines">
              <span><strong>Najtaniej:</strong> 1 karta ${escapeHtml(tLabel)} <em>(${escapeHtml(matchOk)})</em></span>
              <span><strong>Albo:</strong> 1× uniwersalna (3 tereny) · 2 dowolne karty <em>(${escapeHtml(twoOk)})</em> · 1 karta + 1 złoto <em>(${escapeHtml(mixOk)})</em></span>
            </span>
          </button>`;
          })
          .join("")}
      </div>
      <p class="note" style="margin:0.55rem 0 0">Na mapie (Stół) sąsiedzi są podświetleni na złoto, gdy ruszasz się w turze.</p>
    </div>
  `;
}

/**
 * Kroki „za rękę” wg znaku na polu (akcja obszaru w fazie I).
 * To NIE jest talia eksploracji z fazy II — ta jest osobno.
 */
export function locationActionSteps(loc) {
  if (!loc) {
    return {
      title: "Brak lokacji",
      steps: ["Wybierz szkołę w zakładce Gracze — pionek staje na szkole startowej."],
      tip: "",
    };
  }
  const key = loc.actionKey;
  const hint = actionHintFor(loc) || LOCATION_ACTION_HINTS[key] || "";
  const base = {
    title: `${loc.name} (#${loc.number})`,
    tip: hint,
  };

  switch (key) {
    case "schoolTrain":
      return {
        ...base,
        steps: [
          "To szkoła wiedźmińska (złoty punkt na schemacie).",
          "Wybierz 1 atrybut: atak / obrona / alchemia / specjalność.",
          "Zapłać złotem tyle, ile wynosi poziom DOCZELOWY (aktualny + 1) — zwykle 2, 3, 4 albo 5.",
          "Podnieś ten atrybut o +1 na swojej planszy.",
          "Zdolność szkoły: tylko we własnej szkole (nie w cudzej).",
          "Tę konkretną akcję szkoły najwyżej raz na turę.",
        ],
      };
    case "trailGold":
      return {
        ...base,
        steps: [
          "Weź od razu +1 złoto do kieszeni (z banku).",
          "Opcja — zadanie tropienia: zapłać 1 złoto.",
          "Dobierz żeton obszaru o terenie takim, jak teren POTWORA, którego tropisz (las/góry/woda).",
          "Połóż żeton nazwą do góry na planszy gracza i połóż na nim 1 złoto z banku.",
          "Gdy wejdziesz później na tę nazwę: zbierz złoto z żetonu i odwróć go → masz żeton tropu.",
          "Limit: max 1 trop na każdy rodzaj terenu.",
        ],
      };
    case "dicePoker":
      return {
        ...base,
        steps: [
          "To pole z ikoną kości — poker z miejscowymi.",
          "Multi: Ty i przeciwnik musicie mieć ≥1 złoto; wpłacacie do puli (zwykle + bank).",
          "Solo: wpłać 1 złoto i graj vs tabela na karcie pomocy solo.",
          "Rzuć 5 kośćmi, raz możesz przerzucić wybrane.",
          "Najlepszy układ wygrywa pulę / nagrodę z tabeli.",
          "Uwaga: po pokerze z kimś nie walczysz z nim w fazie II tej tury.",
        ],
      };
    case "cleanPool":
      return {
        ...base,
        steps: [
          "Góra znaku: masz 0 złota → bierz +1 złoto (darmowy zastrzyk).",
          "Dół znaku: pola puli kart z płomieniem = możesz SPALIĆ (usunąć z gry) kartę z tego slotu.",
          "Na Wyzimie zwykle możesz wyczyścić wskazane sloty (2. i 4.) — spójrz na planszę.",
          "Akcję obszaru (każdy konkretny efekt) najwyżej raz na turę.",
        ],
      };
    case "trashDraw":
      return {
        ...base,
        steps: [
          "Znak Vengerberg: usuń 1 kartę z ręki z gry.",
          "Kup 1 kartę z odkrytej puli na planszy.",
          "Koszt nowej karty może być niższy, równy albo o 1 wyższy od kosztu usuniętej.",
          "Weź kupioną kartę na rękę. Raz na turę.",
        ],
      };
    case "defenseLevel":
      return {
        ...base,
        steps: [
          "Znak: OBRONA ≤ POZIOM wiedźmina.",
          "Sprawdź: Twój poziom obrony ≤ Twój poziom postaci?",
          "Jeśli tak → +1 obrona (i zwykle tarcza dogania obronę przy awansie).",
          "Jeśli nie — nie bierzesz darmowego treningu obrony tutaj.",
        ],
      };
    case "alchemyLevel":
      return {
        ...base,
        steps: [
          "Znak: + alchemia (tylko gdy alchemia ≤ poziom wiedźmina).",
          "Podnieś alchemię o +1.",
          "Przy każdym podniesieniu alchemii dobierasz 1 kartę eliksiru (limit 4).",
        ],
      };
    case "specialtyLevel":
      return {
        ...base,
        steps: [
          "Znak: ZDOLNOŚĆ SPECJALNA ≤ POZIOM.",
          "Jeśli specjalność ≤ poziom wiedźmina → +1 specjalność.",
          "Raz na turę ta akcja.",
        ],
      };
    case "attackLevel":
      return {
        ...base,
        steps: [
          "Znak: ATAK ≤ POZIOM.",
          "Jeśli atak ≤ poziom wiedźmina → +1 atak.",
          "Darmowy trening — bez płacenia złotem (w przeciwieństwie do szkoły).",
        ],
      };
    case "gainElixir":
    case "cintraElixir":
      return {
        ...base,
        steps: [
          "Znak: + eliksir (butelka / karta eliksiru).",
          "Dobierz 1 kartę eliksiru.",
          "Limit: maksymalnie 4 eliksiry.",
          "Raz na turę ta akcja obszaru.",
        ],
      };
    default:
      return {
        ...base,
        steps: [
          hint || "Spójrz na znak pod nazwą lokacji na planszy i wykonaj go raz w tej turze.",
          "Potem możesz iść dalej (kolejny ruch za kartę) albo przejść do fazy II.",
        ],
      };
  }
}

export function locationActionGuideHtml(loc, { heading = "Akcja tego pola (znaki na planszy)" } = {}) {
  const g = locationActionSteps(loc);
  if (!loc) {
    return `<div class="guide-box"><p class="warn">${escapeHtml(g.steps[0])}</p></div>`;
  }
  const icon = actionIconHtml(loc.actionKey, { width: 120, height: 40 });
  return `
    <div class="guide-box guide-loc" role="region" aria-label="Akcja obszaru">
      <p class="step-kicker" style="margin:0">${escapeHtml(heading)}</p>
      <h3 style="margin:0.25rem 0 0.35rem">${escapeHtml(g.title)}</h3>
      ${icon ? `<div class="loc-action-icon" title="Znak pod nazwą miejscowości">${icon}</div>` : ""}
      ${g.tip ? `<p class="lead" style="margin:0.35rem 0 0">${escapeHtml(g.tip)}</p>` : ""}
      <ol class="checklist guide-steps">
        ${g.steps.map((s) => `<li>${escapeHtml(s)}</li>`).join("")}
      </ol>
      <p class="note">Akcja <strong>fazy I</strong> — opcjonalna, raz na turę. Nie myl z fazą II (walka / medytacja / eksploracja).</p>
    </div>
  `;
}

/** Eksploracja fazy II + kontekst pola. */
export function exploreGuideHtml(loc, isSolo) {
  const locBlock = loc
    ? locationActionGuideHtml(loc, {
        heading: "Znaki na Twoim polu (przypomnienie — to była faza I)",
      })
    : `<p class="note">Nie masz ustawionej lokacji — ustaw ją na Stole, żeby zobaczyć znaki pola.</p>`;

  const phase2 = isSolo
    ? `
      <ol class="checklist">
        <li>Wybierz talię: <strong>miasto</strong> albo <strong>dzikie ostępy</strong>.</li>
        <li>Dobierz kartę sam.</li>
        <li>Zakryj rezultaty — czytasz tylko wstęp i 2 opcje.</li>
        <li>Wybierz opcję; odczytaj tylko jej rezultat.</li>
        <li>Karta do pudełka albo zostaje jako <strong>zadanie</strong>.</li>
      </ol>`
    : `
      <ol class="checklist">
        <li>Wybierz: <strong>miasto</strong> albo <strong>dzikie ostępy</strong>.</li>
        <li>Gracz po prawej dobiera i czyta wstęp + opcje <strong>bez</strong> rezultatów.</li>
        <li>Ty wybierasz; słyszysz tylko wybrany rezultat.</li>
        <li>Karta do pudełka albo jako <strong>zadanie</strong>.</li>
      </ol>`;

  return `
    <div class="guide-box guide-explore">
      <p class="step-kicker" style="margin:0">Faza II — eksploracja (karty)</p>
      <h3 style="margin:0.25rem 0 0.4rem">Co robić teraz</h3>
      <p class="lead" style="margin:0">To NIE jest znak pod nazwą miasta — to dobór karty eksploracji.</p>
      ${phase2}
      <p class="note">Brak złota na opcję z karty = nie możesz jej wybrać.</p>
    </div>
    ${locBlock}
  `;
}

/** Panel pod trackerem zależnie od „co robi teraz”. */
export function nowDoingGuideHtml(player, { isSolo = false } = {}) {
  const loc = player?.locationId ? locById(player.locationId) : null;
  const now = player?.nowDoing || "czekam";

  if (now === "faza1") {
    return `
      <div class="guide-inline">
        <p class="tip"><strong>Faza I:</strong> najpierw obowiązkowy ruch (albo od razu faza II). Po ruchu możesz zrobić akcję ze znaku na polu.</p>
        ${loc ? locationActionGuideHtml(loc) : `<p class="warn">Brak lokacji — wróć do Graczy i wybierz szkołę (start automatyczny).</p>`}
      </div>`;
  }
  if (now === "poker") {
    if (loc) {
      return `<div class="guide-inline">${locationActionGuideHtml(loc, { heading: "Poker / akcja obszaru na tym polu" })}</div>`;
    }
    return `
      <div class="guide-inline">
        <div class="guide-box">
          <p class="tip">Jeśli pole ma kości — graj poker. Inaczej wykonaj znak obszaru (patrz panel lokacji).</p>
        </div>
      </div>`;
  }
  if (now === "eksploracja") {
    return `<div class="guide-inline">${exploreGuideHtml(loc, isSolo)}</div>`;
  }
  if (now === "walka") {
    return `<div class="guide-inline"><p class="tip">Idź do zakładki <strong>Tura → Faza II → Walka</strong> — tam jest rozpisane krok po kroku (tarcza ≠ śmierć).</p></div>`;
  }
  if (now === "medytacja") {
    return `<div class="guide-inline"><p class="tip">Medytacja tylko przy atrybucie na <strong>V</strong> + wolne trofeum. Potem zwykle +reputacja i zmęczenie.</p></div>`;
  }
  if (now === "faza3") {
    return `<div class="guide-inline"><p class="tip">Faza III: odrzuć ile chcesz → dobierz do <strong>3</strong> → kup 1 z puli (płacisz odrzuceniem kart, nie złotem).</p></div>`;
  }
  return "";
}
