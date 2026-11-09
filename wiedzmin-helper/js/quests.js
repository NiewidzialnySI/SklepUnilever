/**
 * Zadania z eksploracji + podsumowanie tury / korekta zasobów po karcie.
 */
import { ICO } from "./icons.js";
import { locById, LOCATIONS } from "./map-data.js";

function escapeHtml(s) {
  return String(s)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

export function ensureQuests(player) {
  if (!player) return;
  if (!Array.isArray(player.quests)) player.quests = [];
  return player.quests;
}

export function newQuestId() {
  return `q-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 6)}`;
}

/** Wybór talii eksploracji: miasto / dzikie ostępy — karty Gemini. */
export function exploreDeckChooserHtml({ selected = null } = {}) {
  return `
    <div class="explore-chooser">
      <h2 class="explore-chooser-title">Co eksplorujesz?</h2>
      <p class="lead" style="margin:0">Kliknij kartę: <strong>dzikie ostępy</strong> albo <strong>miasto</strong>.</p>
      <div class="explore-card-grid" role="group" aria-label="Talia eksploracji">
        <div class="explore-card-slot">
          <button type="button" class="explore-card ${selected === "wilds" ? "active" : ""}" data-explore-deck="wilds" aria-label="Dzikie ostępy" aria-pressed="${
            selected === "wilds" ? "true" : "false"
          }">
            <img src="./img/explore/wilds.png" alt="" width="180" height="272" decoding="async" draggable="false" />
          </button>
          <span class="explore-card-label"><strong>Dzikie ostępy</strong></span>
        </div>
        <div class="explore-card-slot">
          <button type="button" class="explore-card ${selected === "city" ? "active" : ""}" data-explore-deck="city" aria-label="Miasto" aria-pressed="${
            selected === "city" ? "true" : "false"
          }">
            <img src="./img/explore/city.png" alt="" width="180" height="272" decoding="async" draggable="false" />
          </button>
          <span class="explore-card-label"><strong>Miasto</strong></span>
        </div>
      </div>
    </div>
  `;
}

export function exploreResolveHtml({ deck, isSolo, player }) {
  const deckLabel = deck === "wilds" ? "dzikie ostępy" : "miasto";
  const steps = isSolo
    ? `
      <ol class="checklist">
        <li>Dobierz kartę z talii <strong>${escapeHtml(deckLabel)}</strong> (sam).</li>
        <li>Zakryj rezultaty — czytaj tylko wstęp i 2 opcje.</li>
        <li>Wybierz opcję A albo B; odczytaj <strong>tylko jej</strong> rezultat.</li>
        <li>Efekt natychmiastowy → zastosuj poniżej (złoto / karty). Potem karta do pudełka.</li>
        <li>Jeśli wynik ma słowo <strong>Zadanie / Quest</strong> → dodaj zadanie poniżej (lokacja + nr karty zdarzenia).</li>
      </ol>`
    : `
      <ol class="checklist">
        <li>Gracz po prawej dobiera z talii <strong>${escapeHtml(deckLabel)}</strong>.</li>
        <li>Czyta wstęp + opcje <strong>bez</strong> rezultatów.</li>
        <li>Ty wybierasz; słyszysz tylko wybrany rezultat.</li>
        <li>Efekt natychmiastowy → dostosuj złoto/karty poniżej. Albo zapisz <strong>zadanie</strong>.</li>
      </ol>`;

  return `
    ${questAddFormHtml()}
    ${questsListHtml(player)}
    <div class="guide-box">
      <p class="step-kicker" style="margin:0">Karta przeczytana · ${escapeHtml(deckLabel)}</p>
      <h3 style="margin:0.25rem 0 0.4rem">Zastosuj efekt</h3>
      ${steps}
      <p class="note">Brak złota na opcję = nie możesz jej wybrać. Robisz tyle efektu, ile da się.</p>
      <p class="tip">Jeśli karta daje <strong>żeton tropu</strong> — zaznacz 1 trop poniżej. <strong>Zadanie</strong> (lokacja + nr Event) zapisuj <strong>u góry</strong>. Brak żetonów w stosie = ten fragment efektu przepada.</p>
    </div>
    ${resourceAdjustHtml(player, { title: "Po karcie — popraw tracker" })}
  `;
}

export function resourceAdjustHtml(player, { title = "Zasoby" } = {}) {
  if (!player) return "";
  return `
    <div class="guide-box resource-adjust">
      <p class="step-kicker" style="margin:0">${escapeHtml(title)}</p>
      <p class="lead" style="margin:0.3rem 0 0">Teraz: <strong>${player.cards ?? 0}</strong> kart · <strong>${player.gold ?? 0}</strong> złota · <strong>${player.elixirs ?? 0}</strong> eliksirów</p>
      <div class="tracker-grid" style="margin-top:0.65rem">
        <label>Karty
          <div class="num-row">
            <button type="button" class="btn btn-secondary" data-adj="cards" data-delta="-1">−</button>
            <strong class="num-val">${player.cards ?? 0}</strong>
            <button type="button" class="btn btn-secondary" data-adj="cards" data-delta="1">+</button>
          </div>
        </label>
        <label>Złoto (monety)
          <div class="num-row">
            <button type="button" class="btn btn-secondary" data-adj="gold" data-delta="-1">−</button>
            <strong class="num-val">${player.gold ?? 0}</strong>
            <button type="button" class="btn btn-secondary" data-adj="gold" data-delta="1">+</button>
          </div>
        </label>
        <label>Eliksiry
          <div class="num-row">
            <button type="button" class="btn btn-secondary" data-adj="elixirs" data-delta="-1">−</button>
            <strong class="num-val">${player.elixirs ?? 0}</strong>
            <button type="button" class="btn btn-secondary" data-adj="elixirs" data-delta="1">+</button>
          </div>
        </label>
      </div>
    </div>
  `;
}

export function questAddFormHtml() {
  const opts = LOCATIONS.map(
    (l) =>
      `<option value="${escapeHtml(l.id)}">${escapeHtml(l.name)} (#${l.number})</option>`
  ).join("");
  return `
    <div class="guide-box quest-add">
      <p class="step-kicker" style="margin:0">${ICO.quest()} Zadania (u góry)</p>
      <p class="lead" style="margin:0.3rem 0 0">Tu zapisujesz zadania z kart (lokacja + nr Event) oraz dojście z żetonu tropienia. Po wejściu na pole helper przypomni o karcie zdarzenia.</p>
      <label>Tytuł / skrót z karty
        <input type="text" id="quest-title" maxlength="80" placeholder="np. Quest: Kaer Seren / trop" />
      </label>
      <label>Lokacja do dojścia
        <select id="quest-loc">${opts}</select>
      </label>
      <label>Nr karty zdarzenia (Event) do przeczytania na miejscu
        <input type="text" id="quest-event" maxlength="12" placeholder="np. 33" />
      </label>
      <label>Notatka (opcjonalnie)
        <input type="text" id="quest-note" maxlength="120" placeholder="koszt wejścia, teren żetonu…" />
      </label>
      <div class="actions" style="margin-top:0.55rem">
        <button type="button" class="btn btn-primary" id="quest-add-btn">Dodaj zadanie</button>
      </div>
    </div>
  `;
}

export function questsListHtml(player) {
  const list = ensureQuests(player);
  if (!list.length) {
    return `<p class="note">Brak aktywnych zadań.</p>`;
  }
  return `
    <div class="quests-list">
      <h3>${ICO.quest()} Twoje zadania</h3>
      <ul class="checklist">
        ${list
          .map((q) => {
            const loc = locById(q.locationId);
            return `<li data-quest-id="${escapeHtml(q.id)}">
              <strong>${escapeHtml(q.title || "Zadanie")}</strong>
              → ${loc ? escapeHtml(loc.name) : "?"}
              ${q.eventCard ? ` · Event <strong>#${escapeHtml(String(q.eventCard))}</strong>` : ""}
              ${q.note ? `<div class="note">${escapeHtml(q.note)}</div>` : ""}
              <button type="button" class="btn btn-secondary" data-quest-done="${escapeHtml(q.id)}">Rozwiązane / usuń</button>
            </li>`;
          })
          .join("")}
      </ul>
    </div>
  `;
}

/** Baner gdy gracz wszedł na lokację zadania. */
export function questArrivalHtml(player, locId) {
  const list = ensureQuests(player).filter((q) => q.locationId === locId);
  if (!list.length) return "";
  const loc = locById(locId);
  return `
    <div class="guide-box guide-quest-hit" role="alert">
      <p class="step-kicker" style="margin:0">${ICO.quest()} Zadanie na tym polu</p>
      <h3 style="margin:0.25rem 0 0.4rem">Jesteś na <strong>${escapeHtml(loc?.name || "?")}</strong></h3>
      <p class="lead" style="margin:0"><strong>Faza I</strong> — możesz <em>opcjonalnie</em> rozwiązać zadanie teraz (Event z nr poniżej). To <strong>nie</strong> jest faza II.</p>
      <p class="note" style="margin:0.35rem 0 0">Jeśli na polu jest też potwór: zadanie rozwiązujesz w fazie I (opcjonalnie). W fazie II wybierasz <strong>tylko 1</strong> — walka jest <em>opcjonalna</em> (możesz zamiast niej medytować albo eksplorować).</p>
      <ul class="checklist">
        ${list
          .map(
            (q) => `
          <li>
            <strong>${escapeHtml(q.title || "Zadanie")}</strong>
            ${
              q.eventCard
                ? ` — gracz po prawej bierze kartę zdarzenia <strong>#${escapeHtml(String(q.eventCard))}</strong> i czyta.`
                : " — sprawdź kartę zadania (nr zdarzenia)."
            }
            ${q.note ? `<div class="note">${escapeHtml(q.note)}</div>` : ""}
            <button type="button" class="btn btn-primary" data-quest-done="${escapeHtml(q.id)}">Rozwiązane — usuń zadanie</button>
          </li>`
          )
          .join("")}
      </ul>
    </div>
  `;
}

/** Podsumowanie po fazie III. */
export function turnSummaryHtml(player, { phase3Hint, schoolName, turnNumber, isSolo } = {}) {
  if (!player) return "";
  const loc = player.locationId ? locById(player.locationId) : null;
  const quests = ensureQuests(player);
  const cards = player.cards ?? 0;
  const gold = player.gold ?? 0;
  const elixirs = player.elixirs ?? 0;
  const hint = phase3Hint || {};
  const turnLine =
    turnNumber != null
      ? `<li><strong>Tura:</strong> ${Number(turnNumber)}${isSolo ? " <span class=\"note\">(helper liczy automatycznie)</span>" : ""}</li>`
      : "";
  return `
    <div class="guide-box turn-summary">
      <p class="step-kicker" style="margin:0">Koniec tury — podsumowanie</p>
      <h3 style="margin:0.25rem 0 0.45rem">${escapeHtml(player.name)}${schoolName ? ` · ${escapeHtml(schoolName)}` : ""}</h3>
      <ul class="checklist">
        ${turnLine}
        <li><strong>Lokalizacja:</strong> ${loc ? `${escapeHtml(loc.name)} (#${loc.number})` : "—"}</li>
        <li><strong>Karty na ręce:</strong> ${cards} <span class="note">(po fazie III zwykle 3 przed/po kupnie — limit 7)</span></li>
        <li><strong>Złoto:</strong> ${gold}</li>
        <li><strong>Eliksiry:</strong> ${elixirs} / 4</li>
        <li><strong>Reputacja:</strong> ${player.reputation ?? 0}</li>
        <li><strong>Zadania otwarte:</strong> ${quests.length}${
          quests.length
            ? ` — ${quests
                .map((q) => {
                  const L = locById(q.locationId);
                  return `${escapeHtml(q.title || "zadanie")}@${L ? escapeHtml(L.name) : "?"}`;
                })
                .join("; ")}`
            : ""
        }</li>
      </ul>
      ${
        hint.note
          ? `<p class="tip"><strong>Faza III (przypomnienie):</strong> ${escapeHtml(hint.note)}</p>`
          : ""
      }
      <p class="note">Jeśli coś nie gra po efekcie karty — wróć do Stołu / trackerów i popraw −/+.</p>
      ${questsListHtml(player)}
    </div>
  `;
}
