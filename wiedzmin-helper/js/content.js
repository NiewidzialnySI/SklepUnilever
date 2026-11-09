/** Treść przewodnika — Wiedźmin: Stary Świat, sama podstawka (PL). */

export const START_RESOURCES = {
  1: [{ cards: 5, gold: 3 }],
  2: [
    { cards: 3, gold: 2 },
    { cards: 5, gold: 4 },
  ],
  3: [
    { cards: 3, gold: 2 },
    { cards: 4, gold: 4 },
    { cards: 5, gold: 6 },
  ],
  4: [
    { cards: 2, gold: 4 },
    { cards: 3, gold: 5 },
    { cards: 4, gold: 6 },
    { cards: 5, gold: 7 },
  ],
  5: [
    { cards: 2, gold: 5 },
    { cards: 3, gold: 5 },
    { cards: 4, gold: 5 },
    { cards: 4, gold: 7 },
    { cards: 5, gold: 7 },
  ],
};

export const PLAYER_LABELS = [
  "Pierwszy gracz",
  "Drugi gracz",
  "Trzeci gracz",
  "Czwarty gracz",
  "Piąty gracz",
];

export const SOLO_POKER_TABLE = `
  <table class="table">
    <thead><tr><th>Układ</th><th>Nagroda ze banku</th></tr></thead>
    <tbody>
      <tr><td>Para</td><td>0 złota</td></tr>
      <tr><td>Dwie pary</td><td>1 złoto</td></tr>
      <tr><td>Trójka</td><td>2 złota</td></tr>
      <tr><td>Mały strit</td><td>3 złota</td></tr>
      <tr><td>Duży strit</td><td>3 złota</td></tr>
      <tr><td>Full</td><td>4 złota</td></tr>
      <tr><td>Kareta</td><td>5 złota</td></tr>
      <tr><td>Poker</td><td>6 złota</td></tr>
    </tbody>
  </table>
`;

export const SOLO_SCORE_TABLE = `
  <table class="table">
    <thead><tr><th>Liczba tur</th><th>Wynik</th></tr></thead>
    <tbody>
      <tr><td>13+</td><td>Nieźle jak na żółtodzioba…</td></tr>
      <tr><td>11–12</td><td>Można na Tobie polegać.</td></tr>
      <tr><td>9–10</td><td>Profesjonalista — świat Cię potrzebuje.</td></tr>
      <tr><td>7–8</td><td>Na drodze do legendy.</td></tr>
      <tr><td>6 i mniej</td><td>Żywa legenda!</td></tr>
    </tbody>
  </table>
`;

export function buildSetupSteps(playerCount, roster = []) {
  if (playerCount === 1) return buildSoloSetupSteps(roster[0]);
  return buildMultiSetupSteps(playerCount, roster);
}

function buildSoloSetupSteps(player) {
  const who = player
    ? `<strong>${escapeLite(player.name)}</strong>`
    : "Ty";
  return [
    {
      id: "intro",
      title: "Solo — zaczynamy",
      body: `
        <p class="lead">Wariant <strong>solo</strong> (1 osoba). Przygotowanie jak dla 2–3 graczy, z kilku ważnymi zmianami z instrukcji (str. 33).</p>
        <div class="badge-row">
          <span class="badge">Solo</span>
          <span class="badge">Tylko podstawka</span>
          <span class="badge">Cel: 3 potwory + trofeum atrybutu</span>
        </div>
        <p class="tip">Gracz: ${who}. <strong>Notuj liczbę tur</strong>.</p>
      `,
    },
    {
      id: "board",
      title: "Plansza na środku",
      body: `<p>Połóż <strong>planszę</strong> na środku stołu.</p>`,
    },
    {
      id: "action-deck",
      title: "Wspólna talia akcji + pula",
      body: `
        <ol class="checklist">
          <li><strong>1.</strong> Potasuj wszystkie wspólne karty akcji (nie startowe wiedźmina).</li>
          <li><strong>2.</strong> Odkrywaj karty, aż znajdziesz <strong>3 karty o koszcie 0</strong>.</li>
          <li><strong>3.</strong> Połóż je na <strong>3 pierwszych polach od prawej</strong> (losowa kolejność).</li>
          <li><strong>4.</strong> Resztę odkrytych (jeśli były) wtasuj z powrotem. Talię zakrytą na polu.</li>
          <li><strong>5.</strong> Odkryj kolejne <strong>3 karty</strong> na pozostałe pola puli.</li>
        </ol>
      `,
    },
    {
      id: "potions",
      title: "Talia eliksirów",
      body: `<p>Potasuj eliksiry i połóż zakrytą talię obok planszy.</p>`,
    },
    {
      id: "attr-trophies",
      title: "Trofeum atrybutu (solo!)",
      body: `
        <ol class="checklist">
          <li>Weź <strong>po 1 karcie</strong> trofeum dla każdego atrybutu (atak, obrona, alchemia, zdolność) — 4 karty.</li>
          <li>Potasuj ten zestaw, <strong>wylosuj 1</strong> i połóż <strong>odkrytą</strong> obok planszy.</li>
          <li>Pozostałe 3 odłóż do pudełka.</li>
        </ol>
        <p class="warn">To trofeum <strong>musisz zdobyć medytacją</strong> zanim pokonasz 3. potwora — inaczej nie domkniesz wygranej.</p>
      `,
    },
    {
      id: "events-explore",
      title: "Zdarzenia i eksploracja",
      body: `
        <ol class="checklist">
          <li><strong>Zdarzenia:</strong> <em>NIE tasować</em> — rosnąco wg numerów, zakryte (numery do góry).</li>
          <li><strong>Miasta</strong> i <strong>dzikie ostępy</strong>: potasuj osobno, połóż zakryte.</li>
        </ol>
      `,
    },
    {
      id: "gold-dice",
      title: "Bank i kości",
      body: `<p>Zrób <strong>bank</strong> ze złota. Obok połóż kości (wystarczy 1 zestaw 5 kości — grasz sam).</p>`,
    },
    {
      id: "locations",
      title: "Żetony obszarów",
      body: `
        <ol class="checklist">
          <li>3 stosy: las / góry / woda — pomieszaj, zakryte.</li>
          <li>Po 1 żetonie z każdego — odkryte na 3 polach sekcji potworów.</li>
        </ol>
      `,
    },
    {
      id: "monsters",
      title: "Potwory na start (solo!)",
      body: `
        <ol class="checklist">
          <li>Karty potworów: 3 odkryte stosy I / II / III.</li>
          <li>Żetony: 3 zakryte stosy I / II / III.</li>
          <li><strong>Zamiast 3× poziom I:</strong> dobierz <strong>po 1 żetonie</strong> z wierzchu stosów <strong>I, II i III</strong>.</li>
          <li>Odkryj żetony → przenieś na obszary z żetonów obszarów → dopasuj karty potworów.</li>
          <li>Potasuj karty walki potworów — zakryty stos obok planszy.</li>
        </ol>
        <p class="note">Reszta przygotowania potworów jak w multi.</p>
      `,
    },
    {
      id: "solo-help",
      title: "Karta pomocy solo",
      body: `
        <p>Weź <strong>kartę pomocy do wariantu solo</strong> i połóż obok planszetki. Przyda się do kościanego pokera.</p>
        <p>Weź też zwykłe karty pomocy (przebieg tury itd.), jeśli chcesz mieć ściągę przy ręce.</p>
      `,
    },
    {
      id: "player-solo",
      title: player ? `Twój wiedźmin (${escapeLite(player.name)})` : "Twój wiedźmin",
      body: `
        <ol class="checklist">
          <li><strong>1.</strong> Weź planszetkę szkoły, którą wybrałeś w helperze (albo inną — zaktualizuj potem w zakładce Gracze).</li>
          <li><strong>2.</strong> Żeton reputacji szkoły na 0 na torze reputacji.</li>
          <li><strong>3.</strong> Figurka na obszarze <strong>swojej szkoły</strong>.</li>
          <li><strong>4.</strong> 5 znaczników na najniższych polach atrybutów + poziomu wiedźmina.</li>
          <li><strong>5.</strong> Trofea szkoły przeciwników — <strong>pomijasz</strong> (nie ma pojedynków PvP).</li>
          <li><strong>6.</strong> Tarcza na polu <strong>„1”</strong>.</li>
          <li><strong>7.</strong> Potasuj 10 startowych kart akcji swojej szkoły → talia z lewej.</li>
          <li><strong>8. Start solo:</strong> <strong>3 złota</strong> z banku + <strong>5 kart</strong> na rękę.</li>
        </ol>
        <p class="tip">Niewykorzystane zestawy wróć do pudełka.</p>
      `,
    },
    {
      id: "cleanup",
      title: "Cel i zasady wygranej (solo)",
      body: `
        <p class="lead">Wygrywasz, gdy masz <strong>4 reputacji</strong>, ale musisz spełnić oba warunki:</p>
        <ol class="checklist">
          <li>Zdobyć <strong>3 trofea za pokonane potwory</strong> (nie odpędzone).</li>
          <li>Zdobyć <strong>odkryte trofeum atrybutu</strong> z przygotowania (medytacja przy V).</li>
        </ol>
        <p class="warn">Trofeum atrybutu <strong>nie może być ostatnim</strong> — najpierw medytacja, potem 3. potwór. Notuj liczbę tur!</p>
        <h3>Tabela wyniku po wygranej</h3>
        ${SOLO_SCORE_TABLE}
      `,
    },
    {
      id: "ready",
      title: "Gotowy — ruszamy",
      body: `
        <p class="lead">Plansza gotowa. Prowadzę Cię turę po turze w trybie solo.</p>
        <p>Pamiętaj o różnicach: poker vs tabela, atak potwora z kości, bez nowego potwora po zabiciu, specjalna faza III.</p>
      `,
      nextMode: "turn",
    },
  ];
}

function escapeLite(s) {
  return String(s || "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");
}

function buildMultiSetupSteps(n, roster = []) {
  const steps = [
    {
      id: "intro",
      title: "Zaczynamy przygotowanie",
      body: `
        <p class="lead">Prowadzę Was krok po kroku przez przygotowanie <strong>podstawki</strong> (bez dodatków).</p>
        <div class="badge-row">
          <span class="badge">${n} graczy</span>
          <span class="badge">Tylko podstawka</span>
          <span class="badge">Cel: 4 reputacji</span>
        </div>
        <p class="tip"><strong>Wskazówka:</strong> pierwsza partia najlepiej w max. 3 osoby.</p>
      `,
    },
    {
      id: "board",
      title: "Plansza na środku",
      body: `<p>Połóżcie <strong>planszę</strong> na środku stołu.</p>`,
    },
    {
      id: "action-deck",
      title: "Wspólna talia akcji + pula",
      body: `
        <ol class="checklist">
          <li><strong>1.</strong> Potasujcie wszystkie wspólne karty akcji (nie startowe wiedźminów).</li>
          <li><strong>2.</strong> Odkrywajcie, aż znajdziecie <strong>3 karty o koszcie 0</strong>.</li>
          <li><strong>3.</strong> Połóżcie je na <strong>3 pierwszych polach od prawej</strong>.</li>
          <li><strong>4.</strong> Resztę odkrytych wtasujcie z powrotem. Talię zakrytą na polu.</li>
          <li><strong>5.</strong> Odkryjcie kolejne <strong>3 karty</strong> na pozostałe pola puli.</li>
        </ol>
        <p class="note">Skrajne prawe pole: −1 do kosztu; dwa skrajne lewe: +1.</p>
      `,
    },
    {
      id: "potions",
      title: "Talia eliksirów",
      body: `<p>Potasujcie eliksiry i połóżcie zakrytą talię obok planszy.</p>`,
    },
    {
      id: "attr-trophies",
      title: "Trofea atrybutów",
      body:
        n <= 3
          ? `<p>Dla <strong>2–3 graczy</strong>: po 1 karcie trofeum na każdy atrybut (4 karty) — zakryty stos. Reszta do pudełka.</p>`
          : `<p>Dla <strong>4–5 graczy</strong>: <strong>wszystkie</strong> karty trofeów atrybutów w zakrytym stosie.</p>`,
    },
    {
      id: "events-explore",
      title: "Zdarzenia i eksploracja",
      body: `
        <ol class="checklist">
          <li><strong>Zdarzenia:</strong> <em>NIE tasować!</em> Rosnąco wg numerów.</li>
          <li><strong>Miasta</strong> i <strong>dzikie ostępy</strong>: potasujcie osobno.</li>
        </ol>
      `,
    },
    {
      id: "gold-dice",
      title: "Bank złota i kości",
      body: `<p>Wspólny <strong>bank</strong> + <strong>2 zestawy po 5 kości</strong>.</p>`,
    },
    {
      id: "locations",
      title: "Żetony obszarów",
      body: `
        <ol class="checklist">
          <li>3 stosy: las / góry / woda.</li>
          <li>Po 1 odkrytym żetonie na 3 polach sekcji potworów.</li>
        </ol>
      `,
    },
    {
      id: "monsters",
      title: "Potwory na start",
      body: buildMonsterSetupHtml(n),
    },
    {
      id: "first-player",
      title: "Pierwszy gracz i karty pomocy",
      body: `
        <p>Pierwszy gracz: kto <strong>ostatnio czytał książkę o wiedźminie</strong> (albo inaczej).</p>
        <p>Każdy bierze <strong>po 1 karcie pomocy każdego rodzaju</strong>.</p>
        <p class="tip">Przygotowanie: od osoby <strong>po prawej</strong> pierwszego, <strong>przeciwnie do zegara</strong>. Pierwszy wybiera wiedźmina jako <strong>ostatni</strong>.</p>
      `,
    },
  ];

  for (let i = 0; i < n; i++) {
    const turnOrderIndex = n - 1 - i;
    steps.push(buildPlayerSetupStep(n, turnOrderIndex, i + 1, roster[turnOrderIndex]));
  }

  if (n >= 4) {
    steps.push({
      id: "extra-4-5",
      title: "Dodatki dla 4–5 graczy",
      body: `
        <ol class="checklist">
          <li><strong>Dodatkowy stos I:</strong> ${n === 4 ? "1 żeton" : "2 żetony"} z poziomu I — przed poziomem II.</li>
          <li><strong>Atrybut +1:</strong> każdy podnosi 1 atrybut o 1 (od pierwszego, zgodnie z zegarem).</li>
        </ol>
      `,
    });
  }

  steps.push({
    id: "cleanup",
    title: "Schować nadmiar",
    body: `
      <p>Niewykorzystane zestawy → do pudełka.</p>
      <p class="tip"><strong>Cel:</strong> 4 reputacji. Trofea: potwór / walka z wiedźminem / medytacja. Medytacja <em>nie kończy</em> gry przy 4. reputacji.</p>
    `,
  });

  steps.push({
    id: "ready",
    title: "Gotowi do gry!",
    body: `
      <p class="lead">Plansza gotowa. Prowadzę turę po turze.</p>
      <p>Kolejność: zgodnie z zegarem, zaczyna <strong>pierwszy gracz</strong>.</p>
    `,
    nextMode: "turn",
  });

  return steps;
}

function buildMonsterSetupHtml(n) {
  const special2 =
    n === 2
      ? `<li><strong>3.</strong> <strong>2× poziom I</strong> + <strong>1× poziom II</strong>. Pierwszy gracz wybiera miejsce dla II.</li>`
      : `<li><strong>3.</strong> <strong>3 żetony poziomu I</strong> — po 1 przy każdym żetonie obszaru.</li>`;

  return `
    <ol class="checklist">
      <li><strong>1.</strong> Karty potworów: stosy I / II / III (odkryte).</li>
      <li><strong>2.</strong> Żetony: stosy I / II / III (zakryte, pomieszane).</li>
      ${special2}
      <li><strong>4–6.</strong> Odkryj → przenieś na obszary → dopasuj karty.</li>
      <li><strong>7.</strong> Potasuj karty walki potworów.</li>
    </ol>
  `;
}

function buildPlayerSetupStep(playerCount, turnOrderIndex, prepNumber, rosterEntry) {
  const label = PLAYER_LABELS[turnOrderIndex];
  const res = START_RESOURCES[playerCount][turnOrderIndex];
  const schoolTrophies = playerCount - 1;
  const isFirst = turnOrderIndex === 0;
  const isLastPrep = prepNumber === playerCount;
  const named = rosterEntry
    ? ` <strong>${escapeLite(rosterEntry.name)}</strong>`
    : "";

  return {
    id: `player-${turnOrderIndex}`,
    title: `Gracz ${prepNumber}/${playerCount}: ${label}${rosterEntry ? ` — ${escapeLite(rosterEntry.name)}` : ""}`,
    body: `
      <p class="lead">Teraz przygotowuje się:${named} (${label})${isFirst ? " — wybiera wiedźmina jako ostatni" : ""}.</p>
      <ol class="checklist">
        <li><strong>1. Planszetka:</strong> ${
          rosterEntry
            ? `Weź szkołę przypisaną w helperze (albo zgodnie z wyborem przy stole).`
            : isLastPrep && playerCount === 5
              ? "Bierze ostatnią niewybraną."
              : "Wybiera; potem los 2 z niewybranych, zatrzymuje 1."
        }</li>
        <li><strong>2.</strong> Żeton reputacji na 0.</li>
        <li><strong>3.</strong> Figurka na obszarze swojej szkoły.</li>
        <li><strong>4.</strong> 5 znaczników na torach atrybutów + poziomu.</li>
        <li><strong>5.</strong> <strong>${schoolTrophies}</strong> trofeów szkoły (tylu przeciwników) — zakryte pod górną krawędzią.</li>
        <li><strong>6.</strong> Tarcza na „1”.</li>
        <li><strong>7.</strong> Talia 10 startowych kart szkoły.</li>
        <li><strong>8.</strong> <strong>${res.gold} złota</strong> + <strong>${res.cards} kart(y)</strong> na rękę.</li>
      </ol>
    `,
  };
}

export const MOVE_HELP = `
  <h3>Jak się ruszyć?</h3>
  <ul class="checklist">
    <li>Odrzuć kartę z <strong>pasującym terenem</strong> do sąsiedniego obszaru.</li>
    <li>Albo <strong>2 dowolne</strong> karty → dowolny sąsiad.</li>
    <li>Albo <strong>1 karta + 1 złoto</strong> → dowolny sąsiad.</li>
    <li><strong>Uniwersalny</strong> teren = 1 karta na dowolny sąsiad.</li>
  </ul>
`;

export function phase1AfterMove(isSolo) {
  return `
  <h3>Po ruchu możesz (w dowolnej kolejności)</h3>
  <ul class="checklist">
    <li><strong>Akcja obszaru</strong> — raz na dany typ w tej turze.</li>
    <li><strong>Poker</strong> — ${
      isSolo
        ? "z miejscowymi (solo: vs tabela na karcie pomocy)"
        : "z wiedźminem na tym samym obszarze (raz z każdym; oboje ≥1 złoto)"
    }.</li>
    <li><strong>Zadanie</strong> — jeśli weszłeś na obszar zadania.</li>
  </ul>
  ${
    isSolo
      ? `<details style="margin-top:0.8rem"><summary><strong>Poker solo — jak grać</strong></summary>
        <ol class="checklist">
          <li>Wpłać <strong>1 złoto</strong> do puli.</li>
          <li>Rzuć 5 kośćmi, raz przerzuć wybrane (opcjonalnie).</li>
          <li>Porównaj układ z kartą pomocy solo / tabelą poniżej — weź nagrodę z banku.</li>
        </ol>
        ${SOLO_POKER_TABLE}
      </details>`
      : `<p class="warn">Poker z kimś = <strong>nie walczysz</strong> z nim w fazie II tej tury.</p>`
  }
  ${MOVE_HELP}
`;
}

export function phase2Choices(isSolo) {
  if (isSolo) {
    return `
      <p class="lead">Wybierz <strong>dokładnie 1</strong> opcję. Potem nie wracasz do ruchu.</p>
      <div class="choice-grid" data-choices="phase2">
        <button class="choice" data-goto="fight-monster"><strong>Walka z potworem</strong><span>Na Twoim obszarze</span></button>
        <button class="choice" data-goto="meditate"><strong>Medytacja</strong><span>Trofeum atrybutu z przygotowania (przy V)</span></button>
        <button class="choice" data-goto="explore"><strong>Eksploracja</strong><span>Sam czytasz kartę ostrożnie</span></button>
      </div>
      <p class="note">W solo nie ma walki z innymi wiedźminami.</p>
    `;
  }
  return `
    <p class="lead">Wybierz <strong>dokładnie 1</strong> opcję. Potem nie wracasz do ruchu w tej turze.</p>
    <div class="choice-grid" data-choices="phase2">
      <button class="choice" data-goto="fight"><strong>Walka</strong><span>Z potworem lub wiedźminem</span></button>
      <button class="choice" data-goto="meditate"><strong>Medytacja</strong><span>Trofeum za atrybut na V</span></button>
      <button class="choice" data-goto="explore"><strong>Eksploracja</strong><span>Miasto albo dzikie ostępy</span></button>
    </div>
  `;
}

export const FIGHT_BRANCH = `
  <div class="choice-grid" data-choices="fight-type">
    <button class="choice" data-goto="fight-monster"><strong>Walka z potworem</strong><span>Żeton na Twoim obszarze</span></button>
    <button class="choice" data-goto="fight-witcher"><strong>Walka z wiedźminem</strong><span>Na tym samym obszarze</span></button>
  </div>
  <p class="note">Tylko 1 walka na turę. Bez walki na szkole / zamkniętej karczmie. Bez walki z kimś po pokerze w tej turze.</p>
`;

export function fightMonster(isSolo) {
  return `
  <ol class="checklist">
    <li>Sprawdź zdolność specjalną potwora (może być koszt wejścia).</li>
    <li><strong>Wytrzymałość:</strong> tasujesz talię + odrzucone → talia wytrzymałości (ręka zostaje).</li>
    <li>Talia potwora z kart walki (PW z karty).</li>
    <li>Trop tego potwora → Ty zaczynasz; inaczej potwór.</li>
    ${
      isSolo
        ? `<li><strong>Solo — atak potwora:</strong> rzuć kością. <strong>1–3</strong> = szarża, <strong>4–6</strong> = ugryzienie.</li>`
        : `<li>Kontrolujący potwora wybiera: szarża albo ugryzienie.</li>`
    }
    <li>Twoja tura: eliksiry / zdolność → kombinacja → obrażenia → dobór wg ataku.</li>
  </ol>
  <p class="tip"><strong>Pokonanie:</strong> złoto, odwróć kartę, +reputacja, zmęczenie, trofeum.
  ${
    isSolo
      ? ` <strong>Solo: NIE dobieraj nowego potwora</strong> po zabiciu.`
      : ` Potem zwykle nowy potwór.`
  }
  → Faza III.</p>
  <p class="tip"><strong>Odpędzenie:</strong> ${
    isSolo
      ? `umieść <strong>nowy żeton tego samego poziomu</strong> (jak w multi).`
      : `nagrody wg instrukcji.`
  } → Faza III.</p>
`;
}

export const FIGHT_WITCHER = `
  <ol class="checklist">
    <li>Obaj: talie wytrzymałości (talia + odrzucone; ręka zostaje).</li>
    <li>Pozostali mogą obstawić (1 złoto).</li>
    <li>Atakujący zaczyna; naprzemiennie aż ktoś padnie.</li>
    <li>Zwycięzca: trofeum szkoły, złoto wg reputacji, +reputacja, zmęczenie.</li>
    <li>Przegrany: karta kosztu 0 na odrzucone.</li>
    <li>Obaj: tarcza = obrona, nowa talia; dobór wg wyniku.</li>
    <li>Żeton <strong>zamkniętej karczmy</strong> na obszarze.</li>
  </ol>
`;

export function meditate(isSolo) {
  return `
  <p>Medytacja tylko gdy:</p>
  <ul class="checklist">
    <li>Atrybut na poziomie <strong>V</strong>,</li>
    <li>dostępne trofeum tego atrybutu${isSolo ? " (to odkryte obok planszy)" : ""},</li>
    <li>nie masz już takiego samego.</li>
  </ul>
  <ol class="checklist">
    <li>Weź trofeum, wsuń odkryte pod dolną krawędź.</li>
    <li>Jeśli reputacja &lt; 3: +1 reputacja i <strong>zmęczenie</strong>.</li>
    <li>Idź do fazy III.</li>
  </ol>
  <p class="warn">${
    isSolo
      ? "W solo musisz wziąć trofeum atrybutu <strong>zanim</strong> pokonasz 3. potwora. Medytacja i tak nie domyka wygranej jako ostatnie trofeum."
      : "Jeśli to byłoby 4. trofeum z medytacji — bierzesz kartę, ale bez +reputacji / zmęczenia i gra się <strong>nie kończy</strong>."
  }</p>
`;
}

export function explore(isSolo) {
  if (isSolo) {
    return `
      <ol class="checklist">
        <li>Wybierz: <strong>miasto</strong> albo <strong>dzikie ostępy</strong>.</li>
        <li>Dobierz kartę sam.</li>
        <li><strong>Zakryj rezultaty</strong> (ręką / kartką) — przeczytaj tylko wprowadzenie i 2 opcje.</li>
        <li>Wybierz opcję; odczytaj <strong>tylko jej</strong> rezultat (staraj się nie widzieć drugiej).</li>
        <li>Karta do pudełka albo zostaje jako <strong>zadanie</strong>.</li>
      </ol>
      <p class="note">Brak złota na opcję = nie możesz jej wybrać.</p>
    `;
  }
  return `
    <ol class="checklist">
      <li>Wybierz: <strong>miasto</strong> albo <strong>dzikie ostępy</strong>.</li>
      <li>Gracz po prawej dobiera kartę i czyta wprowadzenie + opcje <strong>bez rezultatów</strong>.</li>
      <li>Ty wybierasz; słyszysz tylko wybrany rezultat.</li>
      <li>Karta do pudełka albo jako <strong>zadanie</strong>.</li>
    </ol>
  `;
}

export function phase3(isSolo) {
  return `
  <ol class="checklist">
    <li>Odrzuć z ręki, by mieć <strong>max 3</strong> karty.</li>
    <li>Dobierz do <strong>3</strong> kart na ręce (pusta talia → tasuj odrzucone; poza walką zawsze możesz tak odnowić talię).</li>
    <li>Kup <strong>1 kartę</strong> z puli (koszt nadrukowany ± pole −1/+1). Weź na rękę, przesuń pulę w prawo, odkryj nową na skrajne lewe.</li>
    ${
      isSolo
        ? `<li><strong>Solo:</strong> jeśli kupiłeś kartę <em>nie</em> ze skrajnie prawego pola — na koniec fazy <strong>odrzuć kartę ze skrajnie prawego pola</strong> na wspólny stos odrzuconych.</li>`
        : ""
    }
  </ol>
  <p class="tip">Limit ręki: <strong>7</strong>. Kupno jest standardowo obowiązkowe — <em>FAQ Rebel:</em> jeśli obiektywnie nie możesz kupić, pomijasz.</p>
  ${
    isSolo
      ? `<p class="lead">Koniec tury — zaznacz +1 turę, potem znów Faza I.</p>
         <p class="tip">Wygrana: 3 pokonane potwory + trofeum atrybutu → tabela wyniku (Źródła / Ściąga).</p>`
      : `<p class="lead">Koniec tury — następny gracz zgodnie z zegarem.</p>`
  }
`;
}

export const LOCATION_ACTIONS = `
  <h3>Szybka ściąga akcji obszarów</h3>
  <ul class="checklist">
    <li><strong>Szkoły:</strong> koszt w złocie = poziom docelowy (aktualny + 1) → +1 atak/obrona/alchemia; zdolność specjalną tylko w swojej szkole.</li>
    <li><strong>Darmowy trening:</strong> +1 do atrybutu tylko jeśli poziom atrybutu ≤ poziomu wiedźmina.</li>
    <li><strong>Eliksir:</strong> dobierz 1 (limit 4).</li>
    <li><strong>Poker z miejscowymi:</strong> 1 złoto + 2 z banku (multi) / w solo: 1 złoto i tabela nagród.</li>
    <li><strong>Wymiana:</strong> usuń 1 kartę z ręki, kup z puli o koszcie ≤ usuniętej + 1.</li>
    <li><strong>Tropienie:</strong> +1 złoto, żeton obszaru terenu potwora jako zadanie tropienia.</li>
    <li><strong>Czyszczenie puli:</strong> bez złota → 1 złoto; możesz usunąć 1–2 karty z puli.</li>
  </ul>
`;

export function quickRef(isSolo) {
  return `
  <h3>Złota zasada</h3>
  <p>Tekst na kartach ma pierwszeństwo przed instrukcją.</p>
  <h3>Struktura tury</h3>
  <p><strong>I</strong> ruch i akcje → <strong>II</strong> walka / medytacja / eksploracja → <strong>III</strong> dobór i obowiązkowy zakup.</p>
  ${
    isSolo
      ? `<h3>Solo — skrót różnic</h3>
         <ul class="checklist">
           <li>Start: 3 złota, 5 kart; potwory I+II+III; 1 odkryte trofeum atrybutu.</li>
           <li>Poker: vs tabela nagród.</li>
           <li>Atak potwora: kość 1–3 szarża / 4–6 ugryzienie.</li>
           <li>Po zabiciu: bez nowego potwora; po odpędzeniu: nowy tego poziomu.</li>
           <li>Faza III: kupno nie z prawego skraju → odrzuć prawą kartę puli.</li>
           <li>Wygrana: 3 pokonane + trofeum atrybutu; notuj tury.</li>
         </ul>
         <h3>Wynik solo</h3>
         ${SOLO_SCORE_TABLE}
         <h3>Poker solo</h3>
         ${SOLO_POKER_TABLE}`
      : `<h3>Awans</h3>
         <p>Wszystkie atrybuty powyżej poziomu → awans. II/III: +1 karta; IV/V: +2.</p>
         <h3>Powiązania</h3>
         <p>+1 obrona → +1 tarcza. +1 alchemia → +1 eliksir. Atrybutu na V nie obniżasz.</p>`
  }
`;
}
