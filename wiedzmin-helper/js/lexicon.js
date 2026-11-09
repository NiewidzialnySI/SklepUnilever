/** Leksykon podstawki — reguły z instrukcji/FAQ (nie pełne teksty wszystkich kart). */

export const ATTRIBUTES = [
  {
    id: "atak",
    name: "Atak",
    body: "Ile kart dobierasz na koniec swojej tury walki (po kombinacji), ± modyfikatory z kart.",
  },
  {
    id: "obrona",
    name: "Obrona",
    body: "Limit tarczy. +1 obrona → natychmiast +1 tarcza (max 5). Tarcza nigdy > obrona. Po walce ustaw tarczę = obrona.",
  },
  {
    id: "alchemia",
    name: "Alchemia",
    body: "+1 alchemia → dobierz 1 eliksir. W całej walce możesz zużyć łącznie tyle eliksirów, ile wynosi alchemia. Limit posiadania: 4 eliksiry.",
  },
  {
    id: "zdolnosc",
    name: "Zdolność specjalna (szkoła)",
    body: "Unikalna dla szkoły. Raz na walkę (chyba że inaczej). Po podniesieniu poziomu nie używasz efektów niższych poziomów. Trening tylko w swojej szkole.",
  },
];

/** Skrót zdolności — patrz też ikony poziomów na planszetce. */
export const SCHOOL_SPECIALTIES = [
  {
    id: "wilk",
    name: "Szkoła Wilka — szermierka",
    body: "Raz na walkę, gdy zagrasz kombinację 3+ kart: możesz zadać dodatkowe obrażenia; od II poziomu także dobrać dodatkowe karty (wg ikon na torze).",
  },
  {
    id: "gryf",
    name: "Szkoła Gryfa — magia",
    body: "Raz na walkę: dobierz 1 kartę z wierzchu stosu odrzuconych, potem możesz odrzucić część kart z ręki. Na IV–V: przejrzyj cały stos odrzuconych i weź dowolną 1 na rękę.",
  },
  {
    id: "niedzwiedz",
    name: "Szkoła Niedźwiedzia — zbroja",
    body: "Raz na walkę, w Twojej turze walki, gdy tarcza = 0: natychmiast dobierz z talii wytrzymałości; od II także podnieś tarczę (wg ikon).",
  },
  {
    id: "kot",
    name: "Szkoła Kota — szybkość",
    body: "Raz na walkę, w pierwszej turze walki: podejrzyj wierzchnie karty talii wytrzymałości; część odłóż na wierzch w wybranej kolejności, resztę weź na rękę. Na I, IV i V: wszystkie podejrzane zostają na ręce.",
  },
  {
    id: "zmija",
    name: "Szkoła Żmii — zatruta stal",
    body: "Raz na walkę, gdy przeciwnik odrzuca karty w wyniku Twojego ataku: podejrzyj/przestaw wierzchnie karty jego talii (liczba z toru); od II możesz też odrzucić wskazaną liczbę — wtedy musisz to zrobić.",
  },
];

export const COMBAT_STEPS = `
  <h3>Tura walki wiedźmina (4 kroki)</h3>
  <ol class="checklist">
    <li><strong>1. Eliksiry / zdolność / ekwipunek i trofea</strong> — eliksiry łącznie ≤ alchemii w całej walce; zdolność raz na walkę; efekty „raz na walkę” opcjonalne.</li>
    <li><strong>2. Kombinacja</strong> — zagraj ≥1 kartę z ręki (obowiązkowe, jeśli masz karty). Kolejne muszą pasować przedłużeniami. 1 karta na ręce = grasz tę jedną.</li>
    <li><strong>3. Rozpatrzenie</strong> — suma symboli obrażeń: potwór zrzuca tyle kart ze swojej talii walki. Inne symbole (tarcza, ±dobór…) wg ikon.</li>
    <li><strong>4. Dobór</strong> — z talii wytrzymałości dobierz = poziom <strong>Ataku</strong> ± modyfikatory. Na rękę. Limit 7 w walce = stop.</li>
  </ol>
  <p class="tip"><strong>Co liczyć?</strong> Obrażenia = liczba mieczy/symboli obrażeń na zagranych kartach (i eliksirach). Atak na planszetce = ile kart dobierasz po swoim ataku. Talia potwora = jego życie.</p>
  <p class="tip">Potwór: szarża albo ugryzienie → odkryj 1 kartę walki → efekty. Solo: kość 1–3 szarża / 4–6 ugryzienie.</p>
  <p class="warn">Obrażenia na Ciebie: najpierw tarcza, potem wierzch Twojej talii wytrzymałości, potem ręka (typowo; wyjątki na karcie potwora).</p>
`;

export const LEVEL_UP = `
  <h3>Awans poziomu wiedźmina</h3>
  <p>Gdy <strong>wszystkie 4</strong> znaczniki atrybutów są <em>powyżej</em> aktualnego poziomu wiedźmina → awans natychmiast.</p>
  <ul class="checklist">
    <li>Na poziom II lub III: dobierz <strong>1</strong> kartę z talii akcji.</li>
    <li>Na poziom IV lub V: dobierz <strong>2</strong> karty.</li>
    <li>Poziom wiedźmina nie spada, nawet gdy atrybuty spadną.</li>
    <li>Atrybut na V nie może być obniżony.</li>
  </ul>
`;

export const TRAIL_LEXICON = `
  <h3>Żeton obszaru vs żeton tropu</h3>
  <ul class="checklist">
    <li><strong>Żeton obszaru</strong> — żeton z puli las/góry/woda obok planszy. Jedna strona: nazwa lokacji, druga: symbol terenu.</li>
    <li><strong>Zadanie tropienia</strong> — żeton leży u Ciebie <em>nazwą do góry</em> + 1 złoto na nim. Cel: dojść ruchem na tę lokację.</li>
    <li><strong>Żeton tropu</strong> — po dojściu zbierasz złoto i odwracasz żeton na symbol terenu. Oznacza, że tropisz potwora z tego terenu → w walce z nim często <strong>zaczynasz pierwszy</strong>.</li>
    <li>Max <strong>1 trop na rodzaj terenu</strong>. Konflikt (nowy tego samego) → stare zadanie/trop znika bez złota z żetonu (FAQ #10).</li>
  </ul>
`;

export const WHAT_WE_DONT_HAVE = `
  <h3>Czego helper NIE zastąpi</h3>
  <p class="lead">Nie mam dostępu do pełnej bazy ilustracji ani pełnych tekstów wszystkich kart z pudełka.</p>
  <ul class="checklist">
    <li><strong>28 kart potworów</strong> — PW i zdolność czytaj z karty na stole (helper prowadzi procedurę walki).</li>
    <li><strong>90 kart akcji + startowe</strong> — teksty i symbole z kart fizycznych.</li>
    <li><strong>28 eliksirów, ekwipunek, towarzysze</strong> — efekty z karty / zdarzenia.</li>
    <li><strong>72 eksploracji + 56 zdarzeń</strong> — fabularne teksty zawsze z karty (helper mówi tylko <em>jak</em> czytać A/B).</li>
  </ul>
  <p class="tip">Tak ma być: przy stole karty = treść, telefon/laptop = kto kiedy co robi i FAQ. Pełne skopiowanie wszystkich kart do sieci łamałoby prawa CDPR/Go On Board.</p>
`;

export function lexiconHtml() {
  const attrs = ATTRIBUTES.map(
    (a) => `<li><strong>${a.name}</strong><div>${a.body}</div></li>`
  ).join("");
  const schools = SCHOOL_SPECIALTIES.map(
    (s) => `<li><strong>${s.name}</strong><div>${s.body}</div></li>`
  ).join("");

  return `
    ${WHAT_WE_DONT_HAVE}
    <h3>Atrybuty (wszyscy)</h3>
    <ul class="checklist">${attrs}</ul>
    ${LEVEL_UP}
    <h3>Zdolności szkół (podstawka)</h3>
    <ul class="checklist">${schools}</ul>
    <p class="note">Dokładne liczby obrażeń/doboru na każdym poziomie I–V są na <strong>planszetce</strong> (ikony obok toru) — helper nie zastępuje ikon.</p>
    ${TRAIL_LEXICON}
    ${COMBAT_STEPS}
  `;
}
