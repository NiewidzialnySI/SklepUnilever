/**
 * Sklep Unilever — przetwarzanie maili UPS/DHL z Gmaila
 * Tryb SPARK (bez Cloud Function) — zapis bezpośrednio do Firebase RTDB.
 *
 * Wklej w https://script.google.com (konto Gmail Sklepik / Unilever).
 * Instrukcja: firebase/przesylki/INSTALACJA.md
 *
 * Ścieżka zapisu (dopasowana do aplikacji):
 *   /transmitters/{transmitterUid}/shipments/{id}
 * Kod z adresu +XXXXXXXX mapowany przez:
 *   /pairing_codes/{KOD}.transmitterUid
 */

var ETYKIETA_PRZETWORZONE = 'SklepUnileverPrzetworzone';
var RTDB_URL = 'https://sklepunilever-app-default-rtdb.europe-west1.firebasedatabase.app';

var WLASCIWOSCI = {
  FIREBASE_SA_JSON: 'FIREBASE_SA_JSON',
};

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
  Logger.log('Trigger co 5 min — OK. Ustaw FIREBASE_SA_JSON w Właściwościach skryptu.');
}

function testReczny() {
  przetworzNoweMaile(true);
}

function przetworzNoweMaile(reczny) {
  if (!PropertiesService.getScriptProperties().getProperty(WLASCIWOSCI.FIREBASE_SA_JSON)) {
    Logger.log('Brak FIREBASE_SA_JSON w Właściwościach skryptu.');
    return;
  }

  utworzEtykieteJesliBrak();
  var etykieta = GmailApp.getUserLabelByName(ETYKIETA_PRZETWORZONE);
  var zapytanie =
    'in:inbox -label:' +
    ETYKIETA_PRZETWORZONE +
    ' (from:ups OR from:dhl OR subject:przesylce OR subject:przesyłce OR subject:DHL OR subject:UPS)';
  var watki = GmailApp.search(zapytanie, 0, reczny ? 10 : 30);

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

  var pairingCode = wyciagnijKodParowania(wiadomosc);
  if (!pairingCode) {
    return { ok: false, pomin: false, powod: 'Brak +KOD w adresie To' };
  }

  if (!body || body.length < 10) {
    return { ok: false, pomin: false, powod: 'Pusta tresc' };
  }

  var przesylki = parsujMailPrzesylki({
    from: from,
    subject: subject,
    dateMs: wiadomosc.getDate() ? wiadomosc.getDate().getTime() : Date.now(),
    body: body.slice(0, 50000),
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
      powod: 'Brak pairing_codes/' + pairingCode + ' (nadajnik nieaktywny?)',
    };
  }

  var istniejace = pobierzIstniejacePrzesylki(token, transmitterUid);
  var zapisane = [];
  var zaktualizowane = [];
  var pominiete = [];

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
    return { ok: false, pomin: true, powod: 'Duplikaty', pominiete: pominiete };
  }

  return {
    ok: true,
    pairingCode: pairingCode,
    transmitterUid: transmitterUid,
    zapisane: zapisane,
    zaktualizowane: zaktualizowane,
    pominiete: pominiete,
  };
}

function pobierzTokenFirebase() {
  var sa = JSON.parse(PropertiesService.getScriptProperties().getProperty(WLASCIWOSCI.FIREBASE_SA_JSON));
  var header = Utilities.base64EncodeWebSafe(JSON.stringify({ alg: 'RS256', typ: 'JWT' })).replace(/=+$/, '');
  var now = Math.floor(Date.now() / 1000);
  var claim = Utilities.base64EncodeWebSafe(JSON.stringify({
    iss: sa.client_email,
    sub: sa.client_email,
    aud: 'https://oauth2.googleapis.com/token',
    iat: now,
    exp: now + 3600,
    scope: 'https://www.googleapis.com/auth/firebase.database https://www.googleapis.com/auth/userinfo.email',
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
      assertion: jwt,
    },
  });

  if (resp.getResponseCode() !== 200) {
    throw new Error('Token OAuth: ' + resp.getContentText());
  }
  return JSON.parse(resp.getContentText()).access_token;
}

/** Kod QR/parowania (8 znaków) → UID nadajnika z /pairing_codes. */
function rozwiazTransmitterUid(token, pairingCode) {
  var url = RTDB_URL + '/pairing_codes/' + encodeURIComponent(pairingCode) + '.json?access_token=' + token;
  var resp = UrlFetchApp.fetch(url, { muteHttpExceptions: true });
  if (resp.getResponseCode() === 404) return null;
  if (resp.getResponseCode() !== 200) {
    throw new Error('Odczyt pairing_codes: ' + resp.getContentText());
  }
  var dane = JSON.parse(resp.getContentText());
  if (!dane) return null;
  return dane.transmitterUid || null;
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
    if (!dane.hasOwnProperty(id)) continue;
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
  var resp = UrlFetchApp.fetch(url, {
    method: 'patch',
    contentType: 'application/json',
    muteHttpExceptions: true,
    payload: JSON.stringify({
      temat: p.temat,
      dataMaila: p.dataMaila,
      szacowanaDostawa: p.szacowanaDostawa,
      statusTekst: p.statusTekst,
      linkSledzenia: p.linkSledzenia,
      nadawcaPaczki: p.nadawcaPaczki || null,
      zaktualizowano: Date.now(),
      odczytano: false,
    }),
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
      syncCode: pairingCode || null,
      zrodlo: 'email_inbound',
      dodano: Date.now(),
      odczytano: false,
    }),
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
  var nadawcaDhl = input.body.match(/From:\s*DHL[^<\n]*<([^>]+@dhl\.com)>/i);
  var nadawcaUps = input.body.match(/From:\s*UPS[^<\n]*<([^>]+@ups\.com)>/i);
  var nadawca = nadawcaDhl ? nadawcaDhl[1] : nadawcaUps ? nadawcaUps[1] : String(input.from).trim().slice(0, 200);
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
    /powiadomienie o przesy[lł]k/i.test(tekst)
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
  return lower.indexOf('dhl.com') >= 0 || lower.indexOf('mojdhl.pl') >= 0 || lower.indexOf('powiadomienie o przesy') >= 0;
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
    var zTematu = subject.match(/przesy[lł]k[^\d]{0,20}(\d{10,14})/i);
    if (zTematu) wyniki.push({ numer: zTematu[1], kurier: 'dhl' });
  }

  var reParam = /(?:external_key|nz)=(\d{10,14})/gi;
  while ((m = reParam.exec(t)) !== null) {
    wyniki.push({ numer: m[1], kurier: 'dhl' });
  }

  if ((preferowany === 'dhl' || wyniki.length === 0) && czyKontekstDhl(t)) {
    var reCyfry = /\b(\d{10,11})\b/g;
    while ((m = reCyfry.exec(t)) !== null) {
      wyniki.push({ numer: m[1], kurier: 'dhl' });
    }
  }

  return unikalneNumery(wyniki);
}

function wyciagnijNadawcePaczki(tekst) {
  var t = normalizujTrescMaila(tekst);
  var wzorce = [
    /paczka od\s+(.+?)\s+o numerze/i,
    /Z\s+([A-Z0-9\s.]+?)\s+Dor[eę]czenie/i,
    /(ARTERIA LOGISTICS[^\n]{0,80})/i,
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

/** Usuwa krótsze numery będące fragmentem dłuższego (np. 0975738226 z 30975738226). */
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
  var norm = normalizujTrescMaila(tekst);
  var urls = norm.match(/https?:\/\/[^\s"'<>]+/gi);
  if (urls) {
    for (var i = 0; i < urls.length; i++) {
      var u = urls[i].toLowerCase();
      if (kurier === 'ups' && u.indexOf('ups.com') >= 0 && u.indexOf('track') >= 0 && u.indexOf(numer.toLowerCase()) >= 0) {
        return urls[i];
      }
      if (kurier === 'dhl' && u.indexOf('mojdhl.pl') >= 0 && u.indexOf(numer) >= 0) {
        return urls[i];
      }
    }
  }
  if (kurier === 'ups') {
    return 'https://www.ups.com/track?tracknum=' + encodeURIComponent(numer) + '&loc=pl_PL';
  }
  return 'https://mojdhl.pl/paczki/dostawa/' + encodeURIComponent(numer);
}

function wyciagnijSzacowanaDostawe(tekst) {
  var t = normalizujTrescMaila(tekst);
  var wzorce = [
    /Dor[eę]czenie\s+\S+\s+(\d{2}\/\d{2}\/\d{4})/i,
    /(?:delivery|dostaw[aę])\s*(?:on|date|:|\-)?\s*(\d{1,2}[./\-]\d{1,2}[./\-]\d{2,4})/i,
    /(\d{4}-\d{2}-\d{2})/,
  ];
  for (var i = 0; i < wzorce.length; i++) {
    var m = t.match(wzorce[i]);
    if (m && m[1]) return m[1].replace(/\//g, '-');
  }
  return null;
}

function wyciagnijFragmentStatusu(tekst, subject) {
  var wzorce = [
    /przesy[lł]ka\s+dor[eę]czona/i,
    /zosta[lł]a\s+dor[eę]czona/i,
    /zmiana\s+w\s+zaplanowanej\s+dostawie/i,
    /zosta[lł]a\s+odebrana\s+z\s+punktu\s+DHL\s+POP/i,
    /przeka[zż]emy\s+paczk[eę]/i,
    /out for delivery/i,
    /w drodze/i,
    /delivered/i,
    /shipped/i,
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

function wyciagnijKodParowania(wiadomosc) {
  var adresy = [wiadomosc.getTo() || '', wiadomosc.getCc() || ''];
  var raw = wiadomosc.getRawContent();
  if (raw) {
    var d = raw.match(/Delivered-To:\s*(.+)/i);
    if (d) adresy.push(d[1]);
  }
  for (var i = 0; i < adresy.length; i++) {
    var plus = adresy[i].match(/\+([A-Za-z0-9]{8})@/);
    if (plus) return plus[1].toUpperCase();
  }
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
