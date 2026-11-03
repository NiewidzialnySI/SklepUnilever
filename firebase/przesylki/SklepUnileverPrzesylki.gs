/**
 * Sklep Unilever - maile UPS/DHL -> Firebase RTDB
 *
 * ============================================================
 * JAK USTAWIC KLUCZ (WAZNE - NIE WKLEJAJ JSON DO TEGO PLIKU)
 * ============================================================
 * 1. Pobierz JSON z Firebase (Service accounts -> Generuj klucz).
 * 2. W Apps Script kliknij IKONE ZEBATKI (Ustawienia projektu) PO LEWEJ.
 * 3. Przewin w dol do "Wlasciwosci skryptu" -> "Dodaj wlasciwosc skryptu".
 * 4. Pole "Wlasciwosc" (nazwa):   FIREBASE_SA_JSON
 * 5. Pole "Wartosc":              wklej CALY plik JSON (Ctrl+A w pliku .json)
 * 6. Zapisz wlasciwosci.
 * 7. Uruchom funkcje:  KROK1_sprawdzKonfiguracje
 * 8. Jak log = OK, uruchom:      KROK2_zainstalujTrigger
 * 9. Potem: testReczny
 *    Flow: mail na konto+KOD@... -> pairing_codes/KOD -> shipments u tego nadajnika.
 *
 * NIE wklejaj JSON obok var / function - bedzie blad skladni.
 * NIE wysylaj JSON na czat.
 * ============================================================
 */

var ETYKIETA_PRZETWORZONE = 'SklepUnileverPrzetworzone';
var RTDB_URL = 'https://sklepunilever-app-default-rtdb.europe-west1.firebasedatabase.app';

/** Nazwa wlasciwosci w Ustawieniach projektu - TO NIE JEST JSON. */
var NAZWA_WLASCIWOSCI_SA = 'FIREBASE_SA_JSON';
/** Gdy +KOD z maila nie ma wpisu w pairing_codes (stare maile) - UID nadajnika z apki. */
var NAZWA_WLASCIWOSCI_DOMYSLNY_UID = 'DOMYSLNY_TRANSMITTER_UID';

/** Uruchom TO NAJPIERW - sprawdzi czy klucz jest we wlasciwosciach. */
function KROK1_sprawdzKonfiguracje() {
  var raw = PropertiesService.getScriptProperties().getProperty(NAZWA_WLASCIWOSCI_SA);
  if (!raw) {
    Logger.log('BRAK KLUCZA.');
    Logger.log('Zrob tak:');
    Logger.log('1) Zebatka (Ustawienia projektu) po lewej');
    Logger.log('2) Wlasciwosci skryptu -> Dodaj wlasciwosc skryptu');
    Logger.log('3) Wlasciwosc = FIREBASE_SA_JSON');
    Logger.log('4) Wartosc = caly JSON z Firebase (wklej tutaj, NIE do Kod.gs)');
    Logger.log('5) Zapisz, potem uruchom KROK1_sprawdzKonfiguracje jeszcze raz');
    return;
  }
  try {
    var sa = JSON.parse(raw);
    if (!sa.private_key || !sa.client_email) {
      Logger.log('JSON jest, ale brak private_key lub client_email - wklej CALY plik JSON do Wartosci.');
      return;
    }
    if (sa.project_id && sa.project_id !== 'sklepunilever-app') {
      Logger.log('UWAGA: project_id=' + sa.project_id + ' (oczekiwane: sklepunilever-app)');
    }
    Logger.log('OK - klucz SA znaleziony we wlasciwosciach.');
    Logger.log('client_email: ' + sa.client_email);
    var uid = PropertiesService.getScriptProperties().getProperty(NAZWA_WLASCIWOSCI_DOMYSLNY_UID);
    if (uid) {
      Logger.log('DOMYSLNY_TRANSMITTER_UID = ' + uid);
    } else {
      Logger.log('Brak DOMYSLNY_TRANSMITTER_UID (opcjonalne).');
      Logger.log('Jesli log mowi "Brak pairing_codes/XXXX" - dodaj wlasciwosc:');
      Logger.log('  DOMYSLNY_TRANSMITTER_UID = Twoj UID z Firebase /transmitters');
      Logger.log('Albo uruchom powiazStaryKod("XXXX") po ustawieniu UID.');
    }
    Logger.log('Teraz uruchom: KROK2_zainstalujTrigger');
  } catch (e) {
    Logger.log('Wartosc FIREBASE_SA_JSON nie jest poprawnym JSON: ' + e);
    Logger.log('Wklej caly plik .json do pola Wartosc (nie do Kod.gs).');
  }
}

/**
 * Rejestruje stary kod z maila (np. WCYNEV47) w pairing_codes -> DOMYSLNY_TRANSMITTER_UID.
 * Najpierw ustaw wlasciwosc DOMYSLNY_TRANSMITTER_UID, potem:
 *   powiazStaryKod('WCYNEV47')
 */
function powiazStaryKod(kod) {
  kod = String(kod || '').toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 8);
  if (kod.length !== 8) {
    Logger.log('Podaj 8-znakowy kod, np. powiazStaryKod("WCYNEV47")');
    return;
  }
  var uid = PropertiesService.getScriptProperties().getProperty(NAZWA_WLASCIWOSCI_DOMYSLNY_UID);
  if (!uid) {
    Logger.log('Najpierw ustaw wlasciwosc DOMYSLNY_TRANSMITTER_UID (UID z /transmitters w Firebase).');
    return;
  }
  var token = pobierzTokenFirebase();
  var url = RTDB_URL + '/pairing_codes/' + encodeURIComponent(kod) + '.json?access_token=' + token;
  var resp = UrlFetchApp.fetch(url, {
    method: 'put',
    contentType: 'application/json',
    muteHttpExceptions: true,
    payload: JSON.stringify({
      transmitterUid: uid,
      createdAt: Date.now(),
      zrodlo: 'alias_stary_kod'
    })
  });
  if (resp.getResponseCode() !== 200) {
    Logger.log('Blad zapisu pairing_codes: ' + resp.getContentText());
    return;
  }
  Logger.log('OK - pairing_codes/' + kod + ' -> ' + uid);
  Logger.log('Uruchom testReczny ponownie.');
}

/** Szybkie powiazanie kodu z Twoich maili UPS (widoczny w logu testReczny). */
function powiazWCYNEV47() {
  powiazStaryKod('WCYNEV47');
}

/** Uruchom PO KROK1 (gdy log = OK). */
function KROK2_zainstalujTrigger() {
  if (!PropertiesService.getScriptProperties().getProperty(NAZWA_WLASCIWOSCI_SA)) {
    Logger.log('Najpierw ustaw klucz - uruchom KROK1_sprawdzKonfiguracje');
    return;
  }
  zainstalujTrigger();
}

function zainstalujTrigger() {
  var istniejace = ScriptApp.getProjectTriggers();
  for (var i = 0; i < istniejace.length; i++) {
    if (istniejace[i].getHandlerFunction() === 'przetworzNoweMaile') {
      ScriptApp.deleteTrigger(istniejace[i]);
    }
  }
  ScriptApp.newTrigger('przetworzNoweMaile')
    .timeBased()
    .everyMinutes(5)
    .create();
  utworzEtykieteJesliBrak();
  Logger.log('Trigger co 5 min - OK.');
}

function testReczny() {
  przetworzNoweMaile(true);
}

function przetworzNoweMaile(reczny) {
  if (!PropertiesService.getScriptProperties().getProperty(NAZWA_WLASCIWOSCI_SA)) {
    Logger.log('Brak FIREBASE_SA_JSON we Wlasciwosciach skryptu. Uruchom KROK1_sprawdzKonfiguracje');
    return;
  }

  utworzEtykieteJesliBrak();
  var etykieta = GmailApp.getUserLabelByName(ETYKIETA_PRZETWORZONE);
  // Forwardy (From=pracownik) + polskie "przesyłce" — stare from:dhl/subject:przesylce ich gubiło
  var zapytanie =
    'in:inbox -label:' +
    ETYKIETA_PRZETWORZONE +
    ' (from:ups OR from:dhl OR from:unilever.com OR to:sklepik.unilever.paczki OR ' +
    'subject:DHL OR subject:UPS OR subject:Powiadomienie OR subject:External OR ' +
    'subject:przesylce OR subject:przesyłce OR subject:Status OR subject:Aktualizacja OR ' +
    'subject:"nr " OR "PIN do odbioru" OR "ARTERIA LOGISTICS")';
  var watki = GmailApp.search(zapytanie, 0, reczny ? 25 : 40);
  Logger.log('Szukam: ' + zapytanie + ' -> watkow: ' + watki.length);

  for (var i = 0; i < watki.length; i++) {
    var watek = watki[i];
    var wiadomosci = watek.getMessages();
    var watekOk = false;
    var watekPomin = false;

    for (var j = 0; j < wiadomosci.length; j++) {
      var wiad = wiadomosci[j];
      try {
        var wynik = przetworzWiadomosc(wiad);
        if (wynik.ok) watekOk = true;
        if (wynik.pomin) watekPomin = true;
        Logger.log(wiad.getSubject() + ' -> ' + JSON.stringify(wynik));
      } catch (e) {
        Logger.log('Blad: ' + wiad.getSubject() + ' ' + e);
      }
    }

    if (watekOk || watekPomin) {
      watek.markRead();
      watek.addLabel(etykieta);
    }
  }
}

function przetworzWiadomosc(wiadomosc) {
  var from = wiadomosc.getFrom() || '';
  var subject = wiadomosc.getSubject() || '';
  var body = pobierzTrescMaila(wiadomosc);

  if (!czyMailKurier(from, subject, body)) {
    return { ok: false, pomin: true, powod: 'Nie UPS/DHL' };
  }

  // Wymagane: email na adres z +KOD (np. sklepik+L8M65AE6@gmail.com)
  var pairingCode = wyciagnijKodParowania(wiadomosc);
  if (!pairingCode) {
    return {
      ok: false,
      pomin: false,
      powod: 'Brak +KOD w To/Cc/Delivered-To - wyslij na adres z +kodem parowania z apki'
    };
  }

  if (!body || body.length < 10) {
    return { ok: false, pomin: false, powod: 'Pusta tresc' };
  }

  var przesylki = parsujMailPrzesylki({
    from: from,
    subject: subject,
    dateMs: wiadomosc.getDate() ? wiadomosc.getDate().getTime() : Date.now(),
    body: body.slice(0, 50000)
  });

  if (przesylki.length === 0) {
    return { ok: false, pomin: true, powod: 'Brak numeru w mailu' };
  }

  var token = pobierzTokenFirebase();
  var transmitterUid = rozwiazTransmitterUid(token, pairingCode);
  if (!transmitterUid) {
    return {
      ok: false,
      pomin: false,
      powod: 'Nieznany kod parowania ' + pairingCode + ' - uzyj aktualnego kodu z zakladki Urzadzenia w apce'
    };
  }

  var istniejace = pobierzIstniejacePrzesylki(token, transmitterUid);
  var zapisane = [];
  var zaktualizowane = [];

  for (var i = 0; i < przesylki.length; i++) {
    var p = przesylki[i];
    var klucz = p.kurier + '_' + p.numer.replace(/[^A-Za-z0-9]/g, '').toUpperCase();
    if (istniejace[klucz]) {
      aktualizujPrzesylke(token, transmitterUid, istniejace[klucz], p);
      zaktualizowane.push(p.numer);
      continue;
    }
    zapiszPrzesylke(token, transmitterUid, p, pairingCode);
    istniejace[klucz] = 'nowy';
    zapisane.push(p.numer);
  }

  if (zapisane.length === 0 && zaktualizowane.length === 0) {
    return { ok: false, pomin: true, powod: 'Duplikaty' };
  }

  return {
    ok: true,
    pairingCode: pairingCode,
    transmitterUid: transmitterUid,
    zapisane: zapisane,
    zaktualizowane: zaktualizowane
  };
}

function pobierzTokenFirebase() {
  var sa = JSON.parse(PropertiesService.getScriptProperties().getProperty(NAZWA_WLASCIWOSCI_SA));
  var header = Utilities.base64EncodeWebSafe(JSON.stringify({ alg: 'RS256', typ: 'JWT' })).replace(/=+$/, '');
  var now = Math.floor(Date.now() / 1000);
  var claim = Utilities.base64EncodeWebSafe(JSON.stringify({
    iss: sa.client_email,
    sub: sa.client_email,
    aud: 'https://oauth2.googleapis.com/token',
    iat: now,
    exp: now + 3600,
    scope: 'https://www.googleapis.com/auth/firebase.database https://www.googleapis.com/auth/userinfo.email'
  })).replace(/=+$/, '');
  var input = header + '.' + claim;
  var sig = Utilities.base64EncodeWebSafe(
    Utilities.computeRsaSha256Signature(input, sa.private_key)
  ).replace(/=+$/, '');
  var jwt = input + '.' + sig;

  var resp = UrlFetchApp.fetch('https://oauth2.googleapis.com/token', {
    method: 'post',
    muteHttpExceptions: true,
    payload: {
      grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer',
      assertion: jwt
    }
  });

  if (resp.getResponseCode() !== 200) {
    throw new Error('Token OAuth: ' + resp.getContentText());
  }
  return JSON.parse(resp.getContentText()).access_token;
}

/**
 * Mail: ...+KOD@gmail.com  ->  pairing_codes/KOD.transmitterUid
 * (zapasowo: transmitters gdzie syncCode == KOD)
 * Bez zgadywania "pierwszego sklepu" - kod z maila decyduje gdzie trafia paczka.
 */
function rozwiazTransmitterUid(token, pairingCode) {
  if (!pairingCode) return null;

  // 1) Mapa z aplikacji (powstaje przy generowaniu QR)
  var url = RTDB_URL + '/pairing_codes/' + encodeURIComponent(pairingCode) + '.json?access_token=' + token;
  var resp = UrlFetchApp.fetch(url, { muteHttpExceptions: true });
  if (resp.getResponseCode() === 200) {
    var dane = JSON.parse(resp.getContentText());
    if (dane && dane.transmitterUid) {
      Logger.log('Kod ' + pairingCode + ' -> UID ' + dane.transmitterUid + ' (pairing_codes)');
      return dane.transmitterUid;
    }
  } else if (resp.getResponseCode() !== 404) {
    throw new Error('Odczyt pairing_codes: ' + resp.getContentText());
  }

  // 2) Aktualny syncCode na wezle nadajnika
  var q =
    RTDB_URL +
    '/transmitters.json?orderBy="syncCode"&equalTo="' +
    encodeURIComponent(pairingCode) +
    '"&access_token=' +
    token;
  var resp2 = UrlFetchApp.fetch(q, { muteHttpExceptions: true });
  if (resp2.getResponseCode() === 200) {
    var mapa = JSON.parse(resp2.getContentText());
    if (mapa) {
      for (var uid in mapa) {
        if (Object.prototype.hasOwnProperty.call(mapa, uid)) {
          Logger.log('Kod ' + pairingCode + ' -> UID ' + uid + ' (syncCode)');
          return uid;
        }
      }
    }
  }

  return null;
}

function pobierzIstniejacePrzesylki(token, transmitterUid) {
  var url = RTDB_URL + '/transmitters/' + encodeURIComponent(transmitterUid) + '/shipments.json?access_token=' + token;
  var resp = UrlFetchApp.fetch(url, { muteHttpExceptions: true });
  if (resp.getResponseCode() === 404) return {};
  if (resp.getResponseCode() !== 200) {
    throw new Error('Odczyt RTDB: ' + resp.getContentText());
  }
  var dane = JSON.parse(resp.getContentText());
  if (!dane) return {};
  var map = {};
  for (var id in dane) {
    if (!Object.prototype.hasOwnProperty.call(dane, id)) continue;
    var w = dane[id];
    if (w.numer && w.kurier) {
      map[w.kurier + '_' + w.numer.replace(/[^A-Za-z0-9]/g, '').toUpperCase()] = id;
    }
  }
  return map;
}

function aktualizujPrzesylke(token, transmitterUid, shipmentId, p) {
  if (shipmentId === 'nowy') return;
  var url =
    RTDB_URL +
    '/transmitters/' +
    encodeURIComponent(transmitterUid) +
    '/shipments/' +
    encodeURIComponent(shipmentId) +
    '.json?access_token=' +
    token;
  // Nie nadpisuj PIN/telefonu null-em gdy nowszy mail (np. "doręczona") ich nie ma
  var patch = {
    temat: p.temat,
    dataMaila: p.dataMaila,
    statusTekst: p.statusTekst,
    linkSledzenia: p.linkSledzenia,
    zaktualizowano: Date.now(),
    odczytano: false
  };
  if (p.szacowanaDostawa) patch.szacowanaDostawa = p.szacowanaDostawa;
  if (p.nadawcaPaczki) patch.nadawcaPaczki = p.nadawcaPaczki;
  if (p.pinOdbioru) patch.pinOdbioru = p.pinOdbioru;
  if (p.telefonKuriera) patch.telefonKuriera = p.telefonKuriera;
  var resp = UrlFetchApp.fetch(url, {
    method: 'patch',
    contentType: 'application/json',
    muteHttpExceptions: true,
    payload: JSON.stringify(patch)
  });
  if (resp.getResponseCode() !== 200) {
    throw new Error('Aktualizacja RTDB: ' + resp.getContentText());
  }
}

function zapiszPrzesylke(token, transmitterUid, p, pairingCode) {
  var url =
    RTDB_URL +
    '/transmitters/' +
    encodeURIComponent(transmitterUid) +
    '/shipments.json?access_token=' +
    token;
  var resp = UrlFetchApp.fetch(url, {
    method: 'post',
    contentType: 'application/json',
    muteHttpExceptions: true,
    payload: JSON.stringify({
      numer: p.numer,
      kurier: p.kurier,
      temat: p.temat,
      dataMaila: p.dataMaila,
      szacowanaDostawa: p.szacowanaDostawa,
      statusTekst: p.statusTekst,
      linkSledzenia: p.linkSledzenia,
      nadawca: p.nadawca,
      nadawcaPaczki: p.nadawcaPaczki || null,
      pinOdbioru: p.pinOdbioru || null,
      telefonKuriera: p.telefonKuriera || null,
      syncCode: pairingCode || null,
      zrodlo: 'email_inbound',
      dodano: Date.now(),
      odczytano: false
    })
  });
  if (resp.getResponseCode() !== 200) {
    throw new Error('Zapis RTDB: ' + resp.getContentText());
  }
}

function normalizujTrescMaila(tekst) {
  return tekst
    .replace(/=\r?\n/g, '')
    .replace(/=3D/gi, '=')
    .replace(/\s+/g, ' ');
}

function parsujMailPrzesylki(input) {
  var bodyNorm = normalizujTrescMaila(input.body);
  var preferowany = rozpoznajKurierZMaila(input.from, input.subject, bodyNorm);
  var numery = wyciagnijNumeryPrzesylek(bodyNorm, preferowany, input.subject);
  var szacowanaDostawa = wyciagnijSzacowanaDostawe(bodyNorm);
  var statusTekst = wyciagnijFragmentStatusu(bodyNorm, input.subject);
  var nadawcaPaczki = wyciagnijNadawcePaczki(bodyNorm);
  var pinOdbioru = wyciagnijPinOdbioru(bodyNorm);
  var telefonKuriera = wyciagnijTelefonKuriera(bodyNorm);
  var nadawcaDhl = input.body.match(/From:\s*DHL[^<\n]*<([^>]+@dhl\.com)>/i);
  var nadawcaUps = input.body.match(/From:\s*UPS[^<\n]*<([^>]+@ups\.com)>/i);
  var nadawca = nadawcaDhl
    ? nadawcaDhl[1]
    : nadawcaUps
      ? nadawcaUps[1]
      : String(input.from).trim().slice(0, 200);
  var wynik = [];

  for (var i = 0; i < numery.length; i++) {
    var n = numery[i];
    wynik.push({
      numer: n.numer,
      kurier: n.kurier,
      temat: String(input.subject).trim().slice(0, 300),
      dataMaila: input.dateMs,
      szacowanaDostawa: szacowanaDostawa,
      statusTekst: statusTekst,
      linkSledzenia: wyciagnijLinkSledzenia(bodyNorm, n.kurier, n.numer),
      nadawca: nadawca,
      nadawcaPaczki: nadawcaPaczki,
      pinOdbioru: pinOdbioru,
      telefonKuriera: telefonKuriera
    });
  }
  return wynik;
}

function czyMailKurier(from, subject, body) {
  return rozpoznajKurierZMaila(from, subject, body) != null;
}

function rozpoznajKurierZMaila(from, subject, body) {
  var zNadawcy = rozpoznajKurierZNadawcy(from);
  if (zNadawcy) return zNadawcy;
  var tekst = (subject + '\n' + body).toLowerCase();
  if (
    /\b1z[a-z0-9]{16}\b/i.test(tekst) ||
    tekst.indexOf('ups.com') >= 0 ||
    tekst.indexOf('pkginfo@ups.com') >= 0 ||
    tekst.indexOf('mcinfo@ups.com') >= 0 ||
    /status ups|aktualizacja ups|numer monitorowania/i.test(tekst)
  ) {
    return 'ups';
  }
  if (
    tekst.indexOf('dhl.com') >= 0 ||
    tekst.indexOf('mojdhl.pl') >= 0 ||
    tekst.indexOf('dhlecommerce') >= 0 ||
    tekst.indexOf('pl.no.reply@dhl.com') >= 0 ||
    tekst.indexOf('dhl ecommerce') >= 0 ||
    /powiadomienie o przesy/i.test(tekst) ||
    /pin do odbioru/i.test(tekst) ||
    /arteria logistics/i.test(tekst) ||
    /paczka dor[e\u0119]czona/i.test(tekst)
  ) {
    return 'dhl';
  }
  return null;
}

function rozpoznajKurierZNadawcy(from) {
  var lower = from.toLowerCase();
  if (lower.indexOf('ups.com') >= 0 || lower.indexOf('ups.pl') >= 0) return 'ups';
  if (lower.indexOf('dhl') >= 0) return 'dhl';
  return null;
}

function czyKontekstDhl(tekst) {
  var lower = tekst.toLowerCase();
  return (
    lower.indexOf('dhl.com') >= 0 ||
    lower.indexOf('mojdhl.pl') >= 0 ||
    lower.indexOf('powiadomienie o przesy') >= 0
  );
}

function wyciagnijNumeryPrzesylek(tekst, preferowany, subject) {
  var wyniki = [];
  var t = normalizujTrescMaila(tekst);
  var m;

  var reTracknum = /tracknum=(1Z[A-Z0-9]{16})/gi;
  while ((m = reTracknum.exec(t)) !== null) {
    wyniki.push({ numer: m[1].toUpperCase(), kurier: 'ups' });
  }

  var reUps = /\b(1Z[A-Z0-9]{16})\b/gi;
  while ((m = reUps.exec(t)) !== null) {
    wyniki.push({ numer: m[1].toUpperCase(), kurier: 'ups' });
  }

  var reJjd = /\b(JJD[A-Z0-9]{16,22})\b/gi;
  while ((m = reJjd.exec(t)) !== null) {
    wyniki.push({ numer: m[1].toUpperCase(), kurier: 'dhl' });
  }

  var reMojDhl = /mojdhl\.pl\/paczki\/dostawa\/(\d{10,14})/gi;
  while ((m = reMojDhl.exec(t)) !== null) {
    wyniki.push({ numer: m[1], kurier: 'dhl' });
  }

  if (subject) {
    // "przesyłce nr 317..." / "przesylce nr" / "nr 317..."
    var zTematu =
      subject.match(/przesy[l\u0142](?:k|c)\w*[^\d]{0,30}(\d{10,14})/i) ||
      subject.match(/\bnr\s*(\d{10,14})\b/i);
    if (zTematu) wyniki.push({ numer: zTematu[1], kurier: preferowany || 'dhl' });
  }

  var reNumerze = /(?:o numerze|numer paczki|numer przesy[l\u0142]ki)\s*[:.]?\s*(\d{10,14})/gi;
  while ((m = reNumerze.exec(t)) !== null) {
    wyniki.push({ numer: m[1], kurier: preferowany || 'dhl' });
  }

  var reParam = /(?:external_key|nz)=(\d{10,14})/gi;
  while ((m = reParam.exec(t)) !== null) {
    wyniki.push({ numer: m[1], kurier: 'dhl' });
  }

  if ((preferowany === 'dhl' || wyniki.length === 0) && czyKontekstDhl(t + ' ' + (subject || ''))) {
    var reCyfry = /\b(\d{10,14})\b/g;
    while ((m = reCyfry.exec(t)) !== null) {
      wyniki.push({ numer: m[1], kurier: 'dhl' });
    }
  }

  return unikalneNumery(wyniki);
}

function wyciagnijPinOdbioru(tekst) {
  var t = normalizujTrescMaila(tekst);
  var wzorce = [
    /PIN do odbioru paczki\s*[:.]?\s*(\d{4,8})/i,
    /PIN do odbioru\s*[:.]?\s*(\d{4,8})/i,
    /kod (?:PIN|odbioru|do odbioru)\s*[:.]?\s*(\d{4,8})/i,
    /\bPIN\s*[:.]?\s*(\d{6})\b/i
  ];
  for (var i = 0; i < wzorce.length; i++) {
    var m = t.match(wzorce[i]);
    if (m) return m[1];
  }
  return null;
}

function wyciagnijTelefonKuriera(tekst) {
  var t = normalizujTrescMaila(tekst);
  var m =
    t.match(/Kontakt do kuriera\s*[:.]?\s*(\d{9,11})/i) ||
    t.match(/telefon(?:\s+kuriera)?\s*[:.]?\s*(\d{9,11})/i);
  return m ? m[1] : null;
}

function wyciagnijNadawcePaczki(tekst) {
  var t = normalizujTrescMaila(tekst);
  var wzorce = [
    /paczka od\s+(.+?)\s+o numerze/i,
    /Twoja paczka od\s+(.+?)\s+o numerze/i,
    /Paczka od\s+(.+?)\s+dzi[sś]/i,
    /Z\s+([A-Z0-9\s.]+?)\s+Dor[e\u0119]czenie/i,
    /(ARTERIA LOGISTICS[^.]{0,80})/i
  ];
  for (var i = 0; i < wzorce.length; i++) {
    var m = t.match(wzorce[i]);
    if (m) return m[1].replace(/\s+/g, ' ').trim().slice(0, 120);
  }
  return null;
}

function unikalneNumery(wyniki) {
  var map = {};
  var out = [];
  for (var i = 0; i < wyniki.length; i++) {
    var klucz = wyniki[i].kurier + ':' + wyniki[i].numer;
    if (!map[klucz]) {
      map[klucz] = true;
      out.push(wyniki[i]);
    }
  }
  return filtrujZagniezdzoneNumery(out);
}

function filtrujZagniezdzoneNumery(wyniki) {
  return wyniki.filter(function (w) {
    for (var i = 0; i < wyniki.length; i++) {
      var inny = wyniki[i].numer;
      if (inny !== w.numer && inny.length > w.numer.length && inny.indexOf(w.numer) >= 0) {
        return false;
      }
    }
    return true;
  });
}

function wyciagnijLinkSledzenia(tekst, kurier, numer) {
  // Kanoniczne deep-linki PL (Shoper / najszybsza.pl) — apka i tak przebudowuje link przy otwarciu
  if (kurier === 'ups') {
    return 'https://wwwapps.ups.com/WebTracking?loc=pl_PL&tracknum=' + encodeURIComponent(numer);
  }
  if (/^JJD/i.test(numer) || /^\d{10,14}$/.test(numer)) {
    return 'https://mojdhl.pl/paczki/dostawa/' + encodeURIComponent(numer);
  }
  return 'https://sprawdz.dhl.com.pl/szukaj.aspx?m=0&sn=' + encodeURIComponent(numer);
}

function wyciagnijSzacowanaDostawe(tekst) {
  var t = normalizujTrescMaila(tekst);
  var zGodzinami = t.match(
    /Planowane dor[e\u0119]czenie\s*(\d{1,2}[./-]\d{1,2}[./-]\d{2,4})\s*w godzinach\s*(\d{1,2}:\d{2}\s*-\s*\d{1,2}:\d{2})/i
  );
  if (zGodzinami) {
    return (zGodzinami[1] + ' ' + zGodzinami[2]).replace(/\//g, '-');
  }
  var wzorce = [
    /Planowane dor[e\u0119]czenie\s*(\d{1,2}[./-]\d{1,2}[./-]\d{2,4})/i,
    /dor[e\u0119]czenie\s*(\d{1,2}[./-]\d{1,2}[./-]\d{2,4})\s*w godzinach\s*(\d{1,2}:\d{2}\s*-\s*\d{1,2}:\d{2})/i,
    /Dor[e\u0119]czenie\s+\S+\s+(\d{2}\/\d{2}\/\d{4})/i,
    /(?:delivery|dostaw[a\u0105])\s*(?:on|date|:|-)?\s*(\d{1,2}[./-]\d{1,2}[./-]\d{2,4})/i,
    /(\d{4}-\d{2}-\d{2})/
  ];
  for (var i = 0; i < wzorce.length; i++) {
    var m = t.match(wzorce[i]);
    if (m && m[1]) {
      var out = m[1].replace(/\//g, '-');
      if (m[2]) out += ' ' + m[2];
      return out;
    }
  }
  return null;
}

function wyciagnijFragmentStatusu(tekst, subject) {
  var wzorce = [
    /paczka\s+dor[e\u0119]czona/i,
    /przesy[l\u0142]ka\s+dor[e\u0119]czona/i,
    /zosta[l\u0142]a\s+dor[e\u0119]czona/i,
    /dzi[sś]\s+zostanie\s+dostarczona/i,
    /kurier\s+ju[zż]\s+do\s+ciebie\s+jedzie/i,
    /dor[e\u0119]czenie\s+w\s+trakcie/i,
    /paczka\s+w\s+drodze/i,
    /zmiana\s+w\s+zaplanowanej\s+dostawie/i,
    /zosta[l\u0142]a\s+odebrana\s+z\s+punktu\s+DHL\s+POP/i,
    /przeka[z\u017c]emy\s+paczk[e\u0119]/i,
    /out for delivery/i,
    /w drodze/i,
    /delivered/i,
    /shipped/i
  ];
  var zrodla = [subject || '', tekst || ''];
  for (var s = 0; s < zrodla.length; s++) {
    for (var i = 0; i < wzorce.length; i++) {
      var m = zrodla[s].match(wzorce[i]);
      if (m) return m[0];
    }
  }
  return null;
}

/** Usuwa etykiete z watkow (gdy wczesniej sie nie zapisaly) i odpala test. */
function odblokujIPrzetworz() {
  utworzEtykieteJesliBrak();
  var etykieta = GmailApp.getUserLabelByName(ETYKIETA_PRZETWORZONE);
  var watki = GmailApp.search('label:' + ETYKIETA_PRZETWORZONE + ' newer_than:14d', 0, 50);
  for (var i = 0; i < watki.length; i++) {
    watki[i].removeLabel(etykieta);
  }
  Logger.log('Zdjeto etykiete z ' + watki.length + ' watkow. Odpalam przetworzNoweMaile...');
  przetworzNoweMaile(true);
}

function wyciagnijKodParowania(wiadomosc) {
  var adresy = [wiadomosc.getTo() || '', wiadomosc.getCc() || ''];
  var raw = wiadomosc.getRawContent();
  if (raw) {
    var d = raw.match(/Delivered-To:\s*(.+)/i);
    if (d) adresy.push(d[1]);
    var xorig = raw.match(/X-Original-To:\s*(.+)/i);
    if (xorig) adresy.push(xorig[1]);
  }
  for (var i = 0; i < adresy.length; i++) {
    var plus = adresy[i].match(/\+([A-Za-z0-9]{8})@/);
    if (plus) return plus[1].toUpperCase();
  }
  // Outlook/Android forward: +KOD bywa tylko w plain body ("do sklepik...+KOD@")
  var body = pobierzTrescMaila(wiadomosc) || '';
  var zTresci = body.match(/sklepik\.unilever\.paczki\+([A-Za-z0-9]{8})@/i);
  if (zTresci) return zTresci[1].toUpperCase();
  var zPlus = body.match(/\+([A-Za-z0-9]{8})@gmail\.com/i);
  if (zPlus) return zPlus[1].toUpperCase();
  return null;
}

function pobierzTrescMaila(wiadomosc) {
  var plain = wiadomosc.getPlainBody();
  if (plain && plain.trim().length > 20) return plain;
  var html = wiadomosc.getBody();
  if (!html) return plain || '';
  return html
    .replace(/<style[^>]*>[\s\S]*?<\/style>/gi, ' ')
    .replace(/<script[^>]*>[\s\S]*?<\/script>/gi, ' ')
    .replace(/<[^>]+>/g, ' ')
    .replace(/&nbsp;/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

function utworzEtykieteJesliBrak() {
  if (!GmailApp.getUserLabelByName(ETYKIETA_PRZETWORZONE)) {
    GmailApp.createLabel(ETYKIETA_PRZETWORZONE);
  }
}
