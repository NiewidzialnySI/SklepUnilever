/**
 * Atrybuty, tropy, medytacja, startowe zasoby.
 */
import { ICO } from "./icons.js";
import { locById, LOCATIONS, TERRAIN, actionHintFor } from "./map-data.js";
import { START_RESOURCES } from "./content.js";

export const ATTR_KEYS = [
  { id: "attack", label: "Atak" },
  { id: "defense", label: "Obrona" },
  { id: "alchemy", label: "Alchemia" },
  { id: "specialty", label: "Specjalność" },
];

export const TRAIL_TERRAINS = ["forest", "mountain", "water"];

export function ensureCombatTrack(p) {
  if (!p.attrs || typeof p.attrs !== "object") {
    p.attrs = { attack: 1, defense: 1, alchemy: 1, specialty: 1, level: 1 };
  }
  for (const k of ["attack", "defense", "alchemy", "specialty", "level"]) {
    if (typeof p.attrs[k] !== "number") p.attrs[k] = 1;
    p.attrs[k] = Math.max(1, Math.min(5, p.attrs[k]));
  }
  if (typeof p.shield !== "number") p.shield = p.attrs.defense;
  if (!Array.isArray(p.attrTrophies)) p.attrTrophies = [];
  if (!Array.isArray(p.quests)) p.quests = [];
  if (!p.trails || typeof p.trails !== "object") {
    p.trails = { forest: null, mountain: null, water: null };
  }
  for (const t of TRAIL_TERRAINS) {
    if (p.trails[t] === undefined) p.trails[t] = null;
    // Stare „zadanie tropienia” na żetonie → zadanie u góry (quests), żeton czysty
    const slot = p.trails[t];
    if (slot && slot.kind === "quest") {
      const loc = slot.locId ? locById(slot.locId) : null;
      const label = TERRAIN[t]?.label || t;
      const already = p.quests.some(
        (q) => q.locationId === slot.locId && String(q.title || "").includes("tropienia")
      );
      if (slot.locId && !already) {
        p.quests.push({
          id: `q-trail-${t}-${slot.locId}`,
          title: `Zadanie tropienia (${label})`,
          locationId: slot.locId,
          eventCard: "",
          note: loc
            ? `Wejdź na ${loc.name}: zbierz złoto z żetonu i odwróć → żeton tropu.`
            : "Wejdź na nazwę z żetonu: zbierz złoto i odwróć → żeton tropu.",
        });
      }
      p.trails[t] = null;
    }
  }
  if (typeof p.resourcesApplied !== "boolean") p.resourcesApplied = false;
  return p;
}

export function applyStartResources(roster, players, { force = false } = {}) {
  const n = players;
  (roster || []).forEach((p, i) => {
    ensureCombatTrack(p);
    const start = START_RESOURCES[n]?.[i] || { cards: 5, gold: 3 };
    if (force || !p.resourcesApplied) {
      p.cards = start.cards;
      p.gold = start.gold;
      p.resourcesApplied = true;
    }
  });
}

export function startResourcesFor(players, index) {
  return START_RESOURCES[players]?.[index] || { cards: 5, gold: 3 };
}

export function meditationStatus(player, { isSolo = false } = {}) {
  ensureCombatTrack(player);
  const readyAttrs = ATTR_KEYS.filter((a) => (player.attrs[a.id] || 1) >= 5).map((a) => a.id);
  const owned = new Set(player.attrTrophies || []);
  const available = readyAttrs.filter((id) => !owned.has(id));
  const can = available.length > 0;
  return {
    can,
    readyAttrs,
    available,
    owned: [...owned],
    reason: can
      ? `Możesz medytować: ${available.map((id) => ATTR_KEYS.find((a) => a.id === id)?.label || id).join(", ")} na V.`
      : readyAttrs.length
        ? "Masz atrybut na V, ale to trofeum już masz — medytacja zablokowana."
        : "Medytacja zablokowana: żaden atrybut nie jest na poziomie V.",
    soloNote: isSolo
      ? "Solo: weź odkryte trofeum atrybutu z przygotowania, zanim pokonasz 3. potwora."
      : "Multi: trofeum ze stosu atrybutów. 4. reputacja z medytacji nie kończy gry.",
  };
}

/**
 * Faza II — walka opcjonalna.
 * Walka z potworem: tylko gdy na mapie helpera stoi żeton na Twoim polu.
 * Walka z wiedźminem: tylko gdy ktoś inny stoi na tym samym polu.
 */
export function phase2FightOptions(player, { board, roster = [], isSolo = false } = {}) {
  const locId = player?.locationId || null;
  const m = locId ? board?.monsters?.[locId] : null;
  const canMonster = Boolean(locId && m && m.count > 0);
  const others = (roster || []).filter(
    (r) => r && r !== player && r.locationId && locId && r.locationId === locId
  );
  const canWitcher = !isSolo && others.length > 0;
  const can = canMonster || canWitcher;
  let reason;
  if (!locId) {
    reason = "Walka zablokowana: brak lokacji (wybierz szkołę w Graczach).";
  } else if (canMonster && canWitcher) {
    reason = `Możesz walczyć z potworem albo z: ${others.map((o) => o.name).join(", ")}.`;
  } else if (canMonster) {
    reason = "Na Twoim polu stoi potwór — walka opcjonalna (możesz też medytować / eksplorować).";
  } else if (canWitcher) {
    reason = `Brak potwora na polu. Możesz walczyć z: ${others.map((o) => o.name).join(", ")}.`;
  } else {
    reason = "Walka zablokowana: na tym polu nie ma potwora (oznacz go na mapie, jeśli stoi na stole).";
  }
  return {
    can,
    canMonster,
    canWitcher,
    others,
    monster: canMonster ? m : null,
    reason,
  };
}

const TRAIL_ICON = {
  forest: () => ICO.forest(),
  mountain: () => ICO.mountain(),
  water: () => ICO.water(),
};

export function trailsHtml(player, { editable = false, board = null } = {}) {
  ensureCombatTrack(player);
  const monsters = monstersForTrails(board);

  if (!editable) {
    const rows = TRAIL_TERRAINS.map((t) => {
      const slot = player.trails[t];
      const label = TERRAIN[t]?.label || t;
      const loc = slot?.locId ? locById(slot.locId) : null;
      const status = slot?.kind === "trail" ? `trop${loc ? ` · ${loc.name}` : ""}` : "brak";
      return `<li>${TRAIL_ICON[t]?.() || ""} <strong>${label}:</strong> ${status}</li>`;
    }).join("");
    return `
      <div class="guide-box">
        <p class="step-kicker" style="margin:0">${ICO.monster()} Żetony tropu</p>
        <ul class="checklist">${rows}</ul>
      </div>`;
  }

  const buttons = TRAIL_TERRAINS.map((t) => {
    const label = TERRAIN[t]?.label || t;
    const opts = monsters[t] || [];
    const slot = player.trails[t];
    const primary = opts[0] || null;

    if (!primary?.loc) {
      return `
        <button type="button" class="trail-token-btn is-empty" disabled data-trail-terrain="${t}" title="Brak potwora na tym terenie">
          <span class="trail-token-ico">${TRAIL_ICON[t]?.() || ""}</span>
          <strong>${escapeHtmlLite(label)}</strong>
          <span class="trail-token-meta">niedostępne</span>
          <span class="trail-token-status">ustaw potwora w panelu</span>
        </button>`;
    }

    const loc = primary.loc;
    const hasTrop = slot?.kind === "trail";
    const nextKind = hasTrop ? "clear" : "trail";
    const statusTxt = hasTrop ? "trop · klik → wyczyść" : "klik → weź trop";
    const meta =
      primary.level != null && primary.level !== ""
        ? `poz. ${escapeHtmlLite(String(primary.level))}`
        : escapeHtmlLite(label);

    return `
      <button type="button" class="trail-token-btn ${hasTrop ? "active is-trail" : ""}" data-trail-monster="${t}" data-loc="${escapeHtmlLite(
        loc.id
      )}" data-kind="${nextKind}" title="${escapeHtmlLite(label)}">
        <span class="trail-token-ico">${TRAIL_ICON[t]?.() || ""}</span>
        <strong>${escapeHtmlLite(loc.name || label)}</strong>
        <span class="trail-token-meta">${meta}</span>
        <span class="trail-token-status">${statusTxt}</span>
      </button>`;
  }).join("");

  return `
    <div class="guide-box trail-tokens-box">
      <p class="step-kicker" style="margin:0">${ICO.monster()} Żetony tropu</p>
      <p class="lead" style="margin:0.3rem 0 0">Przy <strong>akcji tropienia</strong> wybierasz <strong>1</strong> teren potwora ze stołu. Klik = masz trop (zaczynasz walkę). Dojście z żetonu zapisuj jako zadanie u góry.</p>
      <div class="trail-token-grid" role="group" aria-label="Żetony tropu">${buttons}</div>
    </div>`;
}

export function attrsAdjustHtml(player) {
  ensureCombatTrack(player);
  const a = player.attrs;
  return `
    <div class="guide-box resource-adjust">
      <p class="step-kicker" style="margin:0">Planszetka — atrybuty</p>
      <p class="lead" style="margin:0.3rem 0 0">Poziom <strong>${a.level}</strong> · tarcza <strong>${player.shield ?? a.defense}</strong> (max = obrona)</p>
      <div class="tracker-grid" style="margin-top:0.65rem">
        ${ATTR_KEYS.map(
          (k) => `
          <label>${k.label}
            <div class="num-row">
              <button type="button" class="btn btn-secondary" data-attr="${k.id}" data-delta="-1">−</button>
              <strong class="num-val">${a[k.id]}</strong>
              <button type="button" class="btn btn-secondary" data-attr="${k.id}" data-delta="1">+</button>
            </div>
          </label>`
        ).join("")}
        <label>Poziom
          <div class="num-row">
            <button type="button" class="btn btn-secondary" data-attr="level" data-delta="-1">−</button>
            <strong class="num-val">${a.level}</strong>
            <button type="button" class="btn btn-secondary" data-attr="level" data-delta="1">+</button>
          </div>
        </label>
        <label>Tarcza
          <div class="num-row">
            <button type="button" class="btn btn-secondary" data-shield-delta="-1">−</button>
            <strong class="num-val">${player.shield ?? a.defense}</strong>
            <button type="button" class="btn btn-secondary" data-shield-delta="1">+</button>
          </div>
        </label>
      </div>
    </div>`;
}

export function hasTrailForMonster(player, monsterLoc) {
  ensureCombatTrack(player);
  if (!monsterLoc) return false;
  const terrain = monsterLoc.terrain;
  const slot = player.trails?.[terrain];
  return Boolean(slot && slot.kind === "trail");
}

/** Potwory z mapy → możliwe tereny tropu (+ lokacje). */
export function monstersForTrails(board) {
  const byTerrain = { forest: [], mountain: [], water: [] };
  for (const [locId, m] of Object.entries(board?.monsters || {})) {
    if (!m || !(m.count > 0)) continue;
    const loc = locById(locId);
    if (!loc || !byTerrain[loc.terrain]) continue;
    byTerrain[loc.terrain].push({
      locId,
      loc,
      count: m.count,
      level: m.level || null,
      label: m.label || null,
    });
  }
  return byTerrain;
}

/** Awans poziomu: wszystkie 4 atrybuty > aktualny poziom → możesz +1 poziom (max 5). */
export function levelUpStatus(player) {
  ensureCombatTrack(player);
  const level = player.attrs.level || 1;
  const attrs = ATTR_KEYS.map((a) => ({
    id: a.id,
    label: a.label,
    value: player.attrs[a.id] || 1,
  }));
  const allAbove = attrs.every((a) => a.value > level);
  const can = allAbove && level < 5;
  return {
    level,
    attrs,
    can,
    reason: can
      ? `Wszystkie atrybuty > poziom ${level} → podnieś poziom wiedźmina do ${level + 1}.`
      : level >= 5
        ? "Poziom już na maksimum (5)."
        : `Awans zablokowany: potrzebujesz wszystkich atrybutów > ${level} (teraz: ${attrs
            .map((a) => `${a.label} ${a.value}`)
            .join(", ")}).`,
  };
}

/**
 * Co gracz powinien zmienić po wejściu na pole (znak obszaru + ewentualny awans).
 * Zwraca listę „działań do potwierdzenia”.
 */
export function fieldChangePrompt(player, loc) {
  ensureCombatTrack(player);
  const level = player.attrs.level || 1;
  const actions = [];
  const key = loc?.actionKey;

  const freeAttr = (attrId, label) => {
    const cur = player.attrs[attrId] || 1;
    const ok = cur <= level && cur < 5;
    actions.push({
      id: `attr:${attrId}`,
      kind: "attr",
      attrId,
      label: `+1 ${label}`,
      detail: ok
        ? `${label} ${cur} ≤ poziom ${level} → możesz podnieść do ${cur + 1}.`
        : cur >= 5
          ? `${label} już na V.`
          : `${label} ${cur} > poziom ${level} — ten darmowy trening nie działa.`,
      enabled: ok,
      defaultOn: ok,
    });
  };

  if (key === "schoolTrain") {
    for (const a of ATTR_KEYS) {
      const cur = player.attrs[a.id] || 1;
      const target = cur + 1;
      const cost = target;
      const ok = cur < 5 && (player.gold || 0) >= cost;
      actions.push({
        id: `school:${a.id}`,
        kind: "school",
        attrId: a.id,
        goldCost: cost,
        label: `Szkoła: +1 ${a.label} (−${cost} zł)`,
        detail: ok
          ? `Zapłać ${cost} zł (poziom docelowy) → ${a.label} ${cur}→${target}.`
          : cur >= 5
            ? `${a.label} już na V.`
            : `Za mało złota (${player.gold || 0}/${cost}) albo atrybut max.`,
        enabled: ok,
        defaultOn: false,
      });
    }
  } else if (key === "defenseLevel") freeAttr("defense", "Obrona");
  else if (key === "attackLevel") freeAttr("attack", "Atak");
  else if (key === "specialtyLevel") freeAttr("specialty", "Specjalność");
  else if (key === "alchemyLevel") {
    const ok = (player.elixirs || 0) < 4;
    actions.push({
      id: "elixir:1",
      kind: "elixir",
      label: "+1 eliksir",
      detail: ok
        ? `Alchemia ≤ poziom? Sprawdź na planszy. Eliksiry: ${player.elixirs || 0}/4.`
        : "Masz już 4 eliksiry (limit).",
      enabled: ok && (player.attrs.alchemy || 1) <= level,
      defaultOn: ok && (player.attrs.alchemy || 1) <= level,
    });
  } else if (key === "gainElixir" || key === "cintraElixir") {
    const ok = (player.elixirs || 0) < 4;
    actions.push({
      id: "elixir:1",
      kind: "elixir",
      label: "+1 eliksir",
      detail: ok ? `Eliksiry: ${player.elixirs || 0}/4.` : "Limit 4 eliksiry.",
      enabled: ok,
      defaultOn: ok,
    });
  } else if (key === "trailGold") {
    actions.push({
      id: "gold:1",
      kind: "gold",
      label: "+1 złoto (znak pola)",
      detail: "Weź 1 złoto z banku. Opcja tropienia — osobno w żetonach tropu.",
      enabled: true,
      defaultOn: true,
    });
  }

  const lvl = levelUpStatus(player);
  actions.push({
    id: "level:up",
    kind: "level",
    label: lvl.can ? `+1 poziom wiedźmina (${lvl.level}→${lvl.level + 1})` : "Awans poziomu",
    detail: lvl.reason,
    enabled: lvl.can,
    defaultOn: lvl.can,
  });

  return {
    loc,
    level,
    attrs: { ...player.attrs },
    gold: player.gold || 0,
    elixirs: player.elixirs || 0,
    actions,
    hint: actionHintFor(loc) || "",
  };
}

export function applyFieldChange(player, actionId) {
  ensureCombatTrack(player);
  if (!actionId) return { ok: false, reason: "Brak akcji." };
  if (actionId === "level:up") {
    const st = levelUpStatus(player);
    if (!st.can) return { ok: false, reason: st.reason };
    player.attrs.level += 1;
    return { ok: true };
  }
  if (actionId === "gold:1") {
    player.gold = (player.gold || 0) + 1;
    return { ok: true };
  }
  if (actionId === "elixir:1") {
    if ((player.elixirs || 0) >= 4) return { ok: false, reason: "Limit 4 eliksiry." };
    player.elixirs = (player.elixirs || 0) + 1;
    return { ok: true };
  }
  if (actionId.startsWith("attr:")) {
    const attrId = actionId.slice(5);
    const cur = player.attrs[attrId] || 1;
    if (cur >= 5) return { ok: false, reason: "Atrybut max." };
    if (cur > (player.attrs.level || 1)) return { ok: false, reason: "Atrybut > poziom — darmowy trening nie działa." };
    player.attrs[attrId] = cur + 1;
    if (attrId === "defense") player.shield = Math.max(player.shield || 0, player.attrs.defense);
    return { ok: true };
  }
  if (actionId.startsWith("school:")) {
    const attrId = actionId.slice(7);
    const cur = player.attrs[attrId] || 1;
    if (cur >= 5) return { ok: false, reason: "Atrybut max." };
    const cost = cur + 1;
    if ((player.gold || 0) < cost) return { ok: false, reason: `Brak złota (${player.gold || 0}/${cost}).` };
    player.gold -= cost;
    player.attrs[attrId] = cur + 1;
    if (attrId === "defense") player.shield = Math.max(player.shield || 0, player.attrs.defense);
    if (attrId === "alchemy" && (player.elixirs || 0) < 4) {
      player.elixirs = (player.elixirs || 0) + 1;
    }
    return { ok: true };
  }
  return { ok: false, reason: "Nieznana akcja." };
}

export function fieldChangeModalHtml(player, loc) {
  const prompt = fieldChangePrompt(player, loc);
  const a = prompt.attrs;
  return `
    <div class="field-modal" id="field-change-modal" role="dialog" aria-modal="true" aria-labelledby="field-modal-title">
      <div class="field-modal-card">
        <p class="step-kicker" style="margin:0">Po wejściu na pole</p>
        <h2 id="field-modal-title" style="margin:0.25rem 0 0.4rem">${escapeHtmlLite(loc?.name || "?")}${
          typeof loc?.number === "number" ? ` (#${loc.number})` : ""
        }</h2>
        <p class="lead" style="margin:0">Teraz: poziom <strong>${prompt.level}</strong> · atak ${a.attack} · obrona ${a.defense} · alchemia ${a.alchemy} · specjalność ${a.specialty} · złoto ${prompt.gold} · eliksiry ${prompt.elixirs}</p>
        ${prompt.hint ? `<p class="note" style="margin:0.45rem 0 0"><strong>Znak:</strong> ${escapeHtmlLite(prompt.hint)}</p>` : ""}
        <p class="tip" style="margin:0.65rem 0 0.35rem"><strong>Zaznacz, co zmieniasz na planszetce</strong>, potem Potwierdź.</p>
        <div class="field-modal-actions">
          ${prompt.actions
            .map(
              (act) => `
            <label class="field-modal-row ${act.enabled ? "" : "is-disabled"}">
              <input type="checkbox" data-field-action="${escapeHtmlLite(act.id)}" ${
                act.enabled && act.defaultOn ? "checked" : ""
              } ${act.enabled ? "" : "disabled"} />
              <span>
                <strong>${escapeHtmlLite(act.label)}</strong>
                <span class="note" style="display:block;margin:0.15rem 0 0">${escapeHtmlLite(act.detail)}</span>
              </span>
            </label>`
            )
            .join("")}
        </div>
        <p class="note" style="margin:0.65rem 0 0"><strong>Poziom:</strong> awansujesz, gdy <em>wszystkie</em> atrybuty są wyższe niż aktualny poziom wiedźmina (potem +1 poziom).</p>
        <div class="actions" style="margin-top:0.85rem">
          <button type="button" class="btn btn-primary" id="field-modal-confirm">Potwierdź i dalej</button>
        </div>
      </div>
    </div>
  `;
}

export function escapeHtmlLite(s) {
  return String(s ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}
