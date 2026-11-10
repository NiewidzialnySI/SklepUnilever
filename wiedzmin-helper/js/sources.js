/** Skróty i „transkrypcje” źródeł — tylko podstawka (bez dodatków).
 *  Pełne napisy YT niedostępne z tego środowiska (blokada IP YouTube),
 *  więc poniżej: scalone streszczenia z opisów/rozdziałów filmów + oficjalne FAQ.
 */

/** Kolory znaczników / kostek / pierścieni figurek (jak na komponentach przy stole).
 *  Wilk=brązowy, Gryf=żółty, Niedźwiedź=czerwony, Kot=granat, Żmija=zielony.
 */
export const SCHOOLS = [
  { id: "wilk", name: "Szkoła Wilka", short: "Wilk", color: "#6b4a32" },
  { id: "gryf", name: "Szkoła Gryfa", short: "Gryf", color: "#c9a227" },
  { id: "niedzwiedz", name: "Szkoła Niedźwiedzia", short: "Niedźwiedź", color: "#9a3b2f" },
  { id: "kot", name: "Szkoła Kota", short: "Kot", color: "#24344a" },
  { id: "zmija", name: "Szkoła Żmii", short: "Żmija", color: "#3d6b45" },
];

export const SOURCES = [
  {
    id: "rebel-rules",
    title: "Oficjalna instrukcja PL (Rebel)",
    url: "https://files.rebel.pl/files/instrukcje/Instrukcja_Wiedzmin-Stary-Swiat.pdf",
    kind: "instrukcja",
    notes: "Źródło nadrzędne dla setupu, 3 faz tury, walki, solo.",
  },
  {
    id: "rebel-faq",
    title: "Oficjalne FAQ PL v1.0 (jesień 2023) — sekcja podstawki",
    url: "https://files.rebel.pl/files/instrukcje/Wiedzmin-FAQ_PL_02-Q4_2023.pdf",
    kind: "faq",
    notes: "Wyjaśnienia: atak, kombinacje, tropy, limit ręki, kupno w fazie III, tarcza po +obronie.",
  },
  {
    id: "yt-ontable",
    title: "On Table — wideoinstrukcja (rozdziały)",
    url: "https://ontable.pl/wiedzmin-stary-swiat%E2%94%82-zasady-gry-%E2%94%82-jak-grac/",
    kind: "youtube-outline",
    notes:
      "PL: setup 00:22–09:20, faza I 10:26–20:39, faza II walka/eksploracja/medytacja 21:07–46:16, faza III 46:46–49:00. Bez dodatków w głównej ścieżce.",
  },
  {
    id: "yt-hkzl",
    title: "YouTube: Wiedźmin Stary Świat | Gra Planszowa (HKzLXVLM53M)",
    url: "https://www.youtube.com/watch?v=HKzLXVLM53M",
    kind: "youtube-transcript-partial",
    notes:
      "Fragmenty napisów/opisu: 5 szkół, 18 lokacji, 3 poziomy potworów, start od 3 potworów I, zdarzenia nienumerowane tasowaniem, eksploracja miasto/dzikie, karty akcji do ruchu i kombinacji kolorów w walce, trofea = wygrana.",
  },
  {
    id: "yt-fight",
    title: "YouTube: fighting in the old world (Rw3qTgcxuIw)",
    url: "https://www.youtube.com/watch?v=Rw3qTgcxuIw",
    kind: "youtube-transcript-partial",
    notes:
      "Walka: obrażenia zrzucają karty z talii potwora; dobór = atak ± modyfikatory; tarcza/Quen; zdolności potworów (np. harpia obniża atak w 1. turze).",
  },
  {
    id: "yt-bgs",
    title: "Board Game Shortcut — How to Play (EN, rozdziały)",
    url: "https://boardgamegeek.com/video/572652/the-witcher-old-world/how-to-play-the-witcher-the-old-world-board-game-s",
    kind: "youtube-outline",
    notes:
      "Setup → faza I (ruch, poker, szkoły, eliksiry, tropy) → faza II (eksploracja, medytacja, walka) → rozstrzygnięcia walk → faza III i zwycięstwo. Bez Monster Trail w tym filmie.",
  },
  {
    id: "faq-dieneverdies",
    title: "FAQ społeczności (The Die Never Dies)",
    url: "https://thedieneverdies.net/faq-the-witcher-old-world/",
    kind: "forum-faq",
    notes:
      "Must move przed akcją startową; trash/burn = usunięcie z gry; eliksiry max 4, w walce łącznie ≤ alchemii; tarcza ≤ obrona, po walce reset do obrony; awans gdy wszystkie atrybuty > poziomu.",
  },
  {
    id: "reddit-newplayer",
    title: "Reddit r/TheWitcherOldWorld — pytania nowych graczy",
    url: "https://www.reddit.com/r/TheWitcherOldWorld/comments/14io00u/few_questions_from_new_player/",
    kind: "forum",
    notes:
      "Obrażenia: tarcza → talia wytrzymałości → ręka. Szkoła: koszt = poziom docelowy. Darmowy trening tylko gdy atrybut ≤ poziomu wiedźmina.",
  },
];

/** Kluczowe poprawki / pułapki ze źródeł (podstawka). */
export const RULE_CORRECTIONS = [
  {
    title: "Start tury — najpierw ruch",
    body: "Instrukcja: na początku fazy I musisz wykonać 1 obowiązkowy ruch albo przejść do fazy II. Nie zaczynasz tury akcją obszaru, na którym już stoisz.",
  },
  {
    title: "Faza III — dobór do 3 (nie do 7)",
    body: "Po opcjonalnym odrzuceniu dobierasz, by mieć 3 karty na ręce, potem kupujesz 1. Limit 7 to osobny sufit (FAQ Rebel #25).",
  },
  {
    title: "Usuwanie kart ≠ odrzucenie",
    body: "„Usuń z gry” / trash / burn / zmęczenie = karta wraca do pudełka na stałe. Odrzucenie = na Twój stos odrzuconych.",
  },
  {
    title: "Limit ręki 7",
    body: "Poza walką: jeśli dobór przekroczyłby 7, najpierw odrzuć z ręki tak, by po dobraniu mieć ≤7 (FAQ Rebel #25). W walce: przy 7 kartach nie dobierasz dalej.",
  },
  {
    title: "Faza III — kupno",
    body: "Koszt karty z puli = odrzucenie kart z ręki (nie złoto), wg nadruku ± pole. Kupno obowiązkowe; FAQ Rebel #26: jeśli obiektywnie nie możesz — pomijasz.",
  },
  {
    title: "Eliksiry",
    body: "Max 4 na stole (niezależnie od alchemii). W całej walce możesz zużyć łącznie tyle eliksirów, ile wynosi poziom alchemii.",
  },
  {
    title: "Tarcza",
    body: "Nigdy powyżej obrony. Po walce ustaw tarczę na poziom obrony. +1 obrona zawsze daje +1 tarczę (nawet gdy tarcza była 0) — FAQ #21.",
  },
  {
    title: "Szkoła — koszt treningu",
    body: "Koszt w złocie = aktualny poziom atrybutu + 1 (czyli tyle, ile wynosi poziom docelowy). Zdolność specjalną rozwijasz tylko w swojej szkole.",
  },
  {
    title: "Tropy / żeton tropu",
    body: "Żeton tropu = żeton obszaru odwrócony na symbol terenu (po zadaniu tropienia). Limit per gracz: max 1 na rodzaj terenu (FAQ #10 — nowy tego samego typu usuwa stary bez złota). Kilku graczy może mieć trop tego samego potwora naraz. Po pokonaniu: tropy/zadania tego terenu u wszystkich wracają do puli.",
  },
  {
    title: "Koszt karty z eksploracji",
    body: "Gdy efekt każe wziąć kartę o koszcie X — liczy się koszt nadrukowany, nie pole −1/+1 (to tylko faza III) — FAQ #11.",
  },
  {
    title: "Medytacja nie kończy gry",
    body: "4. reputacji wygrywa tylko trofeum z walki. Medytacja jako „ostatnie” trofeum: bierzesz kartę, bez +reputacji i bez końca gry.",
  },
];

export function sourcesHtml() {
  const list = SOURCES.map(
    (s) => `
    <li>
      <strong><a href="${s.url}" target="_blank" rel="noopener">${s.title}</a></strong>
      <span class="badge">${s.kind}</span>
      <div>${s.notes}</div>
    </li>`
  ).join("");

  const fixes = RULE_CORRECTIONS.map(
    (f) => `<li><strong>${f.title}</strong><div>${f.body}</div></li>`
  ).join("");

  return `
    <p class="lead">Zasady w helperze są oparte na instrukcji Rebel + FAQ oficjalnym. Poniżej skróty filmów/forów (tylko podstawka).</p>
    <p class="note">YouTube blokuje bezpośrednie pobieranie napisów z serwerów chmurowych — w projekcie jest pakiet <code>research/</code> ze spisami rozdziałów YT, cytatami z PDF Rebel i FAQ społeczności.</p>
    <h3>Źródła</h3>
    <ul class="checklist">${list}</ul>
    <h3>Pułapki i doprecyzowania (zweryfikowane)</h3>
    <ul class="checklist">${fixes}</ul>
    <h3>Faza III — nie mylić z limitem 7</h3>
    <p class="tip">Instrukcja: dobierz do <strong>3</strong> kart na ręce, potem kup 1. Limit <strong>7</strong> to sufit (FAQ #25), nie cel doboru w fazie III.</p>
  `;
}
