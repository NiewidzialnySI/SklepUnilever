package shopunilever.dominik.pl.ui.pracownik

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Log
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import shopunilever.dominik.pl.dane.PozycjaKoszyka
import kotlinx.coroutines.flow.collectLatest
import shopunilever.dominik.pl.dane.PreferencjeKonta
import shopunilever.dominik.pl.dane.PreferencjeKoszykow
import shopunilever.dominik.pl.dane.PreferencjeOdbiorcy
import shopunilever.dominik.pl.dane.PreferencjeParowania
import shopunilever.dominik.pl.firebase.SerwisParowania
import shopunilever.dominik.pl.sklep.NasluchSms
import shopunilever.dominik.pl.sklep.SkryptyKoszyka
import shopunilever.dominik.pl.sklep.SkryptySklepu
import shopunilever.dominik.pl.sklep.StaleSklepu
import androidx.compose.runtime.LaunchedEffect
import shopunilever.dominik.pl.ui.theme.AkcentNeon
import shopunilever.dominik.pl.ui.theme.Ostrzezenie
import shopunilever.dominik.pl.ui.theme.TekstDrugi
import shopunilever.dominik.pl.ui.theme.TekstGlowny
import shopunilever.dominik.pl.ui.theme.TloAmoled
import shopunilever.dominik.pl.ui.theme.TloWarstwa
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

private enum class EtapLogowania {
    LADOWANIE,
    EMAIL,
    SMS,
    HASLO,
    GOTOWE,
    BLAD
}

/** Tag Logcat — filtr: `SklepLogin` */
private const val TAG_LOGIN = "SklepLogin"

/**
 * Maszyna stanów auto-logowania.
 * Decyzje po DOM (WYKRYJ), nie po samym URL — jak w Harmonogramie.
 *
 * trybReczny = true → nie klika / nie wypełnia; tylko WYKRYJ + DIAG do Logcat
 * (przechodzimy flow ręcznie i patrzymy co się odpala).
 */
private class StanLogowaniaSklepu(
    val kontekstAplikacji: android.content.Context,
    val email: String,
    val haslo: String
) {
    private val zakresTla = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    var webView: WebView? = null
    val emailWyslany = AtomicBoolean(false)
    val emailWstrzykniety = AtomicBoolean(false)
    val hasloWstrzykniete = AtomicBoolean(false)
    val smsWstrzykniety = AtomicBoolean(false)
    val zalogowany = AtomicBoolean(false)
    val smsOdKiedyMs = AtomicLong(0L)
    val oczekujeEksportuKoszyka = AtomicBoolean(false)
    private val kolejkaDodawania = ArrayDeque<String>()
    private val dodawanieTrwa = AtomicBoolean(false)
    private val oczekujeDodaniaDoKoszyka = AtomicBoolean(false)

    /** Tymczasowo: Ty wpisujesz, ja loguję DOM. Ustaw false żeby wrócić auto-login. */
    val trybReczny = true

    var etap by mutableStateOf(EtapLogowania.LADOWANIE)
    var status by mutableStateOf("Ładowanie sklepu…")
    var blad by mutableStateOf<String?>(null)
    var oczekujeSms by mutableStateOf(false)
    var kodSms by mutableStateOf("")
    var ukryjNakladke by mutableStateOf(true) // tryb ręczny: od razu widać stronę
    var aktualnyUrl by mutableStateOf(StaleSklepu.URL_LOGOWANIA)

    fun toast(tekst: String) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            Toast.makeText(kontekstAplikacji, tekst, Toast.LENGTH_SHORT).show()
        }
    }

    fun wstrzyknij(skrypt: String) {
        webView?.evaluateJavascript(skrypt, null)
    }

    fun odswiez() {
        webView?.reload()
    }

    /** Wstecz w obrębie sklepu — nigdy nie zamyka ekranu. */
    fun obsluzWstecz() {
        val wv = webView ?: return
        if (wv.canGoBack()) {
            wv.goBack()
            wv.postDelayed({
                val url = wv.url
                if (!StaleSklepu.czyUrlSklepu(url) || StaleSklepu.czyPodejrzany404(url)) {
                    wv.loadUrl(StaleSklepu.URL_GLOWNA)
                }
            }, 350)
            return
        }
        if (!StaleSklepu.czyGlownaSklepu(aktualnyUrl)) {
            wv.loadUrl(StaleSklepu.URL_GLOWNA)
        }
    }

    @Volatile
    private var nazwaKoszykaDoZapisu: String = ""

    private val pominięteProdukty = mutableListOf<String>()
    private var aktualnyProduktUrl: String = ""

    fun zapiszAktualnyKoszyk(nazwa: String) {
        nazwaKoszykaDoZapisu = nazwa.trim().ifBlank {
            "Koszyk ${PreferencjeKoszykow.lista(kontekstAplikacji).size + 1}"
        }
        oczekujeEksportuKoszyka.set(true)
        toast("Zbieram produkty z koszyka…")
        wstrzyknij(SkryptySklepu.MOSTEK)
        wstrzyknij(SkryptyKoszyka.EKSPORT)
        // Awaria: jeśli JS nic nie odesłał, otwórz stronę koszyka i spróbuj ponownie
        webView?.postDelayed({
            if (!oczekujeEksportuKoszyka.get()) return@postDelayed
            webView?.loadUrl(SkryptyKoszyka.URL_KOSZYKA)
            webView?.postDelayed({
                if (oczekujeEksportuKoszyka.get()) {
                    wstrzyknij(SkryptySklepu.MOSTEK)
                    wstrzyknij(SkryptyKoszyka.EKSPORT)
                }
            }, 1200)
        }, 2000)
    }

    fun otworzKoszykSklepu() {
        webView?.loadUrl(SkryptyKoszyka.URL_KOSZYKA)
    }

    fun dodajZapisaneDoKoszyka(pozycje: List<PozycjaKoszyka>) {
        if (dodawanieTrwa.get()) {
            toast("Dodawanie już trwa…")
            return
        }
        val urlki = pozycje.map { it.url.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        if (urlki.isEmpty()) {
            toast("Brak linków produktów — nie da się dodać automatycznie")
            return
        }
        kolejkaDodawania.clear()
        kolejkaDodawania.addAll(urlki)
        pominięteProdukty.clear()
        dodawanieTrwa.set(true)
        toast("Dodaję ${urlki.size} produkt(ów) do koszyka…")
        nastepnyProduktZKolejki()
    }

    private fun nastepnyProduktZKolejki() {
        val wv = webView ?: return
        if (!dodawanieTrwa.get()) return
        val url = kolejkaDodawania.pollFirst()
        if (url == null) {
            dodawanieTrwa.set(false)
            oczekujeDodaniaDoKoszyka.set(false)
            val pominięte = pominięteProdukty.toList()
            if (pominięte.isNotEmpty()) {
                toast(
                    "Gotowe. Pominięto ${pominięte.size} niedostępnych: " +
                        pominięte.take(2).joinToString(", ") +
                        if (pominięte.size > 2) "…" else ""
                )
            } else {
                toast("Gotowe — otwieram koszyk")
            }
            wv.loadUrl(SkryptyKoszyka.URL_KOSZYKA)
            return
        }
        aktualnyProduktUrl = url
        oczekujeDodaniaDoKoszyka.set(true)
        wv.loadUrl(url)
        wv.postDelayed({
            if (!dodawanieTrwa.get() || !oczekujeDodaniaDoKoszyka.get()) return@postDelayed
            wstrzyknij(SkryptySklepu.MOSTEK)
            wstrzyknij(SkryptyKoszyka.KLIKNIJ_DODAJ_DO_KOSZYKA)
            // Awaryjne przejście dalej, jeśli JS nie odpowie
            wv.postDelayed({
                if (oczekujeDodaniaDoKoszyka.compareAndSet(true, false)) {
                    nastepnyProduktZKolejki()
                }
            }, 2800)
        }, 1400)
    }

    fun poDodaniuDoKoszyka() {
        if (!oczekujeDodaniaDoKoszyka.compareAndSet(true, false)) return
        webView?.postDelayed({ nastepnyProduktZKolejki() }, 900)
    }

    fun poZalogowaniu() {
        if (!zalogowany.compareAndSet(false, true)) return
        oczekujeSms = false
        etap = EtapLogowania.GOTOWE
        status = "Zalogowano"
        ukryjNakladke = true
        // Nie pchamy na /market (404). Zostaw aktualną stronę albo wejdź na root sklepu.
        val teraz = aktualnyUrl.lowercase()
        if (StaleSklepu.czyPodejrzany404(teraz) ||
            teraz.contains("access") ||
            teraz.contains("login")
        ) {
            wstrzyknij(SkryptySklepu.skryptPrzekieruj(StaleSklepu.URL_GLOWNA))
        }
    }

    fun napraw404JesliTrzeba(url: String?) {
        if (!StaleSklepu.czyPodejrzany404(url)) return
        status = "Błędny adres sklepu — wracam na stronę główną…"
        webView?.loadUrl(StaleSklepu.URL_GLOWNA)
    }

    fun wstrzyknijEmail() {
        if (trybReczny) {
            Log.i(TAG_LOGIN, "POMINIĘTO wstrzyknijEmail (trybReczny) url=$aktualnyUrl")
            return
        }
        if (emailWyslany.get() || !emailWstrzykniety.compareAndSet(false, true)) return
        etap = EtapLogowania.EMAIL
        status = "Wpisuję email służbowy…"
        oczekujeSms = false
        wstrzyknij(SkryptySklepu.skryptWypelnijEmail(email))
    }

    fun wstrzyknijHaslo() {
        if (trybReczny) {
            Log.i(TAG_LOGIN, "POMINIĘTO wstrzyknijHaslo (trybReczny) url=$aktualnyUrl")
            return
        }
        if (!hasloWstrzykniete.compareAndSet(false, true)) return
        oczekujeSms = false
        emailWyslany.set(true)
        smsWstrzykniety.set(true)
        etap = EtapLogowania.HASLO
        status = "Wpisuję hasło…"
        wstrzyknij(SkryptySklepu.skryptWypelnijHaslo(haslo, email))
    }

    /** Po SMS Magento → /customer/account/login?access-email=… */
    fun naStronieHaslaWymusz(url: String? = aktualnyUrl) {
        if (!StaleSklepu.czyStronaHasla(url) || zalogowany.get()) return
        emailWyslany.set(true)
        smsWstrzykniety.set(true)
        if (trybReczny) {
            Log.i(TAG_LOGIN, "POMINIĘTO naStronieHaslaWymusz (trybReczny) url=$url")
            return
        }
        if (haslo.isBlank()) {
            etap = EtapLogowania.BLAD
            blad = "Brak hasła w aplikacji"
            status = "Brak danych logowania"
            return
        }
        if (hasloWstrzykniete.get()) return
        status = "Strona hasła — wpisuję dane…"
        wstrzyknijHaslo()
    }

    fun zacznijOczekiwanieSms() {
        if (smsOdKiedyMs.get() == 0L) {
            smsOdKiedyMs.set(System.currentTimeMillis())
        }
        smsWstrzykniety.set(false)
        oczekujeSms = true
        etap = EtapLogowania.SMS
        status = "Odczytuję kod z SMS — wpisze się sam…"
    }

    fun naKodSms(kod: String) {
        if (kod.length < 4 || zalogowany.get()) return
        if (trybReczny) {
            Log.i(TAG_LOGIN, "POMINIĘTO naKodSms (trybReczny) kodLen=${kod.length} url=$aktualnyUrl")
            // Relay do pary nadal OK — sam kod, bez auto-wpisywania w WebView
            if (PreferencjeParowania.czyAktywne(kontekstAplikacji)) {
                zakresTla.launch {
                    runCatching { SerwisParowania.publikujKodSms(kontekstAplikacji, kod) }
                }
            }
            return
        }
        if (!smsWstrzykniety.compareAndSet(false, true)) return
        kodSms = kod
        status = "Wpisuję kod SMS ($kod)…"
        wstrzyknij(SkryptySklepu.skryptWypelnijSms(kod))
        // Bezpieczny relay do sparowanych urządzeń (tylko kod, bez treści SMS)
        if (PreferencjeParowania.czyAktywne(kontekstAplikacji)) {
            zakresTla.launch {
                runCatching { SerwisParowania.publikujKodSms(kontekstAplikacji, kod) }
            }
        }
    }

    /** Po załadowaniu: cookies + diagnostyka; auto-akcje tylko gdy !trybReczny. */
    fun poZaladowaniuStrony(url: String?) {
        Log.i(TAG_LOGIN, "onPageFinished url=$url trybReczny=$trybReczny")
        wstrzyknij(SkryptySklepu.MOSTEK)
        wstrzyknij(SkryptySklepu.RESPONSYWNOSC)
        wstrzyknij(SkryptySklepu.CIASTECZKA)

        if (zalogowany.get()) {
            ukryjNakladke = true
            etap = EtapLogowania.GOTOWE
            return
        }

        val u = url?.lowercase().orEmpty()
        val toBramka = u.contains("access") || u.contains("login")
        status = when {
            trybReczny -> "Tryb ręczny — wpisuj sam (Logcat: SklepLogin)"
            toBramka -> "Sprawdzam bramkę logowania…"
            else -> "Sprawdzam, czy jesteś już zalogowany…"
        }

        // Zawsze: WYKRYJ + DIAG (obserwacja) — w trybie ręcznym BEZ klikania/wypełniania
        fun skanDiagnostyczny() {
            if (zalogowany.get()) return
            wstrzyknij(SkryptySklepu.CIASTECZKA)
            wstrzyknij(SkryptySklepu.SESJA)
            wstrzyknij(SkryptySklepu.WYKRYJ)
            wstrzyknij(SkryptySklepu.DIAG_STRONY)
        }

        if (trybReczny) {
            listOf(500L, 1500L, 3000L).forEach { opoznienie ->
                webView?.postDelayed({ skanDiagnostyczny() }, opoznienie)
            }
            return
        }

        // Po SMS od razu celuj w hasło — nie czekaj na fałszywe LOGGED_IN z headera Magento
        if (StaleSklepu.czyStronaHasla(u)) {
            status = "Strona hasła — przygotowuję logowanie…"
            emailWyslany.set(true)
            smsWstrzykniety.set(true)
            listOf(400L, 1000L, 2000L, 3500L).forEach { opoznienie ->
                webView?.postDelayed({
                    if (zalogowany.get() || hasloWstrzykniete.get()) return@postDelayed
                    wstrzyknij(SkryptySklepu.CIASTECZKA)
                    wstrzyknij(SkryptySklepu.DIAG_STRONY)
                    naStronieHaslaWymusz(aktualnyUrl)
                }, opoznienie)
            }
            return
        }

        webView?.postDelayed({ skanDiagnostyczny() }, 500)
        listOf(1200L, 2500L).forEach { opoznienie ->
            webView?.postDelayed({ skanDiagnostyczny() }, opoznienie)
        }
    }

    fun obsluzWiadomosc(json: String) {
        try {
            val obj = JSONObject(json)
            when (obj.optString("type")) {
                "KOSZYK_EXPORT" -> {
                    // Tylko gdy użytkownik kliknął „Zapisz” (flaga ustawiona)
                    if (!oczekujeEksportuKoszyka.getAndSet(false)) return
                    val arr = obj.optJSONArray("pozycje") ?: JSONArray()
                    val pozycje = buildList {
                        for (i in 0 until arr.length()) {
                            val p = arr.optJSONObject(i) ?: continue
                            add(
                                PozycjaKoszyka(
                                    nazwa = p.optString("nazwa").ifBlank { "Produkt ${i + 1}" },
                                    sku = p.optString("sku"),
                                    ilosc = p.optInt("ilosc", 1).coerceAtLeast(1),
                                    url = p.optString("url"),
                                    cena = p.optString("cena")
                                )
                            )
                        }
                    }
                    if (pozycje.isEmpty()) {
                        toast("Koszyk pusty albo nie znaleziono produktów — otwórz koszyk i spróbuj ponownie")
                        return
                    }
                    val nazwa = nazwaKoszykaDoZapisu.ifBlank {
                        "Koszyk ${PreferencjeKoszykow.lista(kontekstAplikacji).size + 1}"
                    }
                    nazwaKoszykaDoZapisu = ""
                    val zapis = PreferencjeKoszykow.zapisz(
                        kontekstAplikacji,
                        nazwa = nazwa,
                        pozycje = pozycje
                    )
                    toast("Zapisano „${zapis.nazwa}” (${pozycje.size} poz.)")
                    return
                }

                "KOSZYK_DODANO" -> {
                    val sukces = obj.optBoolean("sukces", true)
                    val info = obj.optString("info")
                    if (!sukces && info in setOf("BRAK_PRODUKTU", "BRAK_PRZYCISKU", "STRONA_404")) {
                        val skrot = aktualnyProduktUrl
                            .substringAfterLast('/')
                            .substringBefore('?')
                            .ifBlank { "produkt" }
                        pominięteProdukty.add(skrot)
                    }
                    if (info != "SKIP_DUP") {
                        poDodaniuDoKoszyka()
                    }
                    return
                }

                "LOG" -> {
                    Log.d(TAG_LOGIN, "JS: ${obj.optString("msg")}")
                }
            }
        } catch (_: Exception) {
            return
        }

        if (zalogowany.get()) return

        try {
            val obj = JSONObject(json)
            val typ = obj.optString("type")
            when (typ) {
                "DIAG" -> {
                    Log.i(TAG_LOGIN, "DIAG ${obj.toString(2)}")
                    return
                }
                "EMAIL_WYNIK" -> {
                    Log.i(TAG_LOGIN, "EMAIL_WYNIK wynik=${obj.optString("wynik")} trybReczny=$trybReczny")
                    if (trybReczny) return
                    if (obj.optString("wynik") == "EMAIL_SENT" && emailWyslany.compareAndSet(false, true)) {
                        smsOdKiedyMs.set(System.currentTimeMillis())
                        zacznijOczekiwanieSms()
                    }
                }

                "WYKRYJ" -> {
                    val wynik = obj.optString("wynik")
                    Log.i(TAG_LOGIN, "WYKRYJ=$wynik url=$aktualnyUrl etap=$etap trybReczny=$trybReczny")
                    if (trybReczny) {
                        // Tylko obserwacja — cookies nadal zamykamy, reszty nie ruszamy
                        if (wynik == "COOKIE_BANNER") {
                            wstrzyknij(SkryptySklepu.CIASTECZKA)
                        }
                        return
                    }
                    when (wynik) {
                    "COOKIE_BANNER" -> {
                        status = "Akceptuję cookies…"
                        wstrzyknij(SkryptySklepu.CIASTECZKA)
                        webView?.postDelayed({
                            wstrzyknij(SkryptySklepu.CIASTECZKA)
                            wstrzyknij(SkryptySklepu.WYKRYJ)
                        }, 1500)
                    }
                    "ACCESS_METHOD_CHOICE" -> {
                        if (StaleSklepu.czyStronaHasla(aktualnyUrl)) {
                            naStronieHaslaWymusz()
                            return
                        }
                        // Po SMS Magento bywa chwilowo z powrotem na /access —
                        // nie klikaj ponownie „Kod SMS”, tylko szukaj hasła.
                        if (smsWstrzykniety.get() || hasloWstrzykniete.get() ||
                            etap == EtapLogowania.HASLO || emailWyslany.get()
                        ) {
                            status = "Szukam formularza hasła…"
                            webView?.postDelayed({
                                if (!zalogowany.get()) {
                                    if (StaleSklepu.czyStronaHasla(aktualnyUrl)) naStronieHaslaWymusz()
                                    else wstrzyknij(SkryptySklepu.WYKRYJ)
                                }
                            }, 800)
                            return
                        }
                        // /access: Autologowanie | Kod SMS | Karta → zawsze Kod SMS
                        status = "Wybieram logowanie kodem SMS…"
                        wstrzyknij(SkryptySklepu.WYBIERZ_KOD_SMS)
                        webView?.postDelayed({
                            if (!zalogowany.get() && !emailWyslany.get() &&
                                !smsWstrzykniety.get() && !hasloWstrzykniete.get()
                            ) {
                                val u = aktualnyUrl.lowercase()
                                if (!u.contains("access-email")) {
                                    webView?.loadUrl(StaleSklepu.URL_LOGOWANIA)
                                }
                            }
                        }, 2500)
                    }
                    "EMAIL_FIELD" -> {
                        if (email.isBlank() || haslo.isBlank()) {
                            etap = EtapLogowania.BLAD
                            blad = "Brak email/hasła — zapisz dane u pracownika w Ustawieniach"
                            status = "Brak danych logowania"
                            return
                        }
                        if (!emailWyslany.get()) wstrzyknijEmail()
                    }
                    "SMS_TOKEN_FIELD" -> {
                        // Nawet bez wcześniejszego EMAIL_SENT — jesteśmy na ekranie tokenu
                        emailWyslany.set(true)
                        zacznijOczekiwanieSms()
                    }
                    "PASSWORD_FIELD" -> {
                        if (haslo.isBlank()) {
                            etap = EtapLogowania.BLAD
                            blad = "Brak hasła w aplikacji"
                            status = "Brak danych logowania"
                            return
                        }
                        emailWyslany.set(true)
                        smsWstrzykniety.set(true)
                        wstrzyknijHaslo()
                    }
                    "ALREADY_LOGGED_IN" -> {
                        status = "Jesteś już zalogowany…"
                        poZalogowaniu()
                    }
                    "NO_RECOGNIZED_ELEMENTS" -> {
                        val u = aktualnyUrl.lowercase()
                        val poSmsLubHasle = smsWstrzykniety.get() ||
                            hasloWstrzykniete.get() ||
                            etap == EtapLogowania.HASLO ||
                            emailWyslany.get()
                        val naWyborze = u.contains("/access") &&
                            !u.contains("access-email") &&
                            !u.contains("access-code") &&
                            !u.contains("customer/account")
                        when {
                            StaleSklepu.czyStronaHasla(u) -> naStronieHaslaWymusz()
                            poSmsLubHasle -> {
                                // Nie wracaj do wyboru metody / URL_LOGOWANIA
                                status = "Szukam formularza hasła…"
                                webView?.postDelayed({
                                    if (!zalogowany.get()) wstrzyknij(SkryptySklepu.WYKRYJ)
                                }, 800)
                            }
                            naWyborze -> {
                                status = "Wybieram logowanie kodem SMS…"
                                wstrzyknij(SkryptySklepu.WYBIERZ_KOD_SMS)
                            }
                            StaleSklepu.czyUrlSklepu(aktualnyUrl) &&
                                !u.contains("access") &&
                                !u.contains("login") -> {
                                status = "Sesja aktywna — otwieram sklep…"
                                poZalogowaniu()
                            }
                            else -> {
                                status = "Przechodzę do logowania SMS…"
                                wstrzyknij(SkryptySklepu.skryptPrzekieruj(StaleSklepu.URL_LOGOWANIA))
                            }
                        }
                    }
                    } // when (wynik)
                } // WYKRYJ

                "SESJA" -> {
                    val wynik = obj.optString("wynik")
                    Log.i(TAG_LOGIN, "SESJA=$wynik url=$aktualnyUrl trybReczny=$trybReczny")
                    if (trybReczny) return
                    when (wynik) {
                    "LOGGED_IN" -> {
                        status = "Jesteś już zalogowany…"
                        poZalogowaniu()
                    }
                    "NOT_FOUND" -> {
                        status = "Strona nie istnieje — wracam na główną sklepu…"
                        webView?.loadUrl(StaleSklepu.URL_GLOWNA)
                    }
                    "NEEDS_LOGIN" -> {
                        val u = aktualnyUrl.lowercase()
                        val poSmsLubHasle = smsWstrzykniety.get() ||
                            hasloWstrzykniete.get() ||
                            etap == EtapLogowania.HASLO ||
                            emailWyslany.get()
                        when {
                            StaleSklepu.czyStronaHasla(u) -> naStronieHaslaWymusz()
                            u.contains("access-email") || u.contains("access-code") -> {
                                wstrzyknij(SkryptySklepu.WYKRYJ)
                            }
                            poSmsLubHasle -> {
                                // Po SMS URL często nadal ma „access” — nie restartuj wyboru metody
                                status = "Szukam formularza hasła…"
                                wstrzyknij(SkryptySklepu.WYKRYJ)
                            }
                            u.contains("/access") && !u.contains("customer/account") -> {
                                status = "Wybieram logowanie kodem SMS…"
                                wstrzyknij(SkryptySklepu.WYBIERZ_KOD_SMS)
                            }
                            else -> {
                                status = "Brak sesji — logowanie kodem SMS…"
                                wstrzyknij(SkryptySklepu.skryptPrzekieruj(StaleSklepu.URL_LOGOWANIA))
                            }
                        }
                    }
                    "UNKNOWN" -> {
                        // Na katalogu bez jasnych znaczników — traktuj jako zalogowany
                        val naSklepie = StaleSklepu.czyUrlSklepu(aktualnyUrl) &&
                            !aktualnyUrl.contains("access", ignoreCase = true)
                        if (naSklepie) poZalogowaniu() else wstrzyknij(SkryptySklepu.WYKRYJ)
                    }
                    else -> wstrzyknij(SkryptySklepu.WYKRYJ)
                    }
                }

                "KROK" -> {
                    val krok = obj.optString("krok")
                    val sukces = obj.optBoolean("sukces", true)
                    val info = obj.optString("info")
                    Log.i(TAG_LOGIN, "KROK krok=$krok sukces=$sukces info=$info")
                    if (trybReczny) return
                    if (!sukces) {
                        if (info in setOf(
                                "PASS_FIELD_NOT_FOUND",
                                "EMAIL_FIELD_NOT_FOUND",
                                "SMS_FIELD_NOT_FOUND"
                            )
                        ) {
                            // Strona mogła się zmienić — zwolnij flagi i ponów wykrycie
                            when (info) {
                                "PASS_FIELD_NOT_FOUND" -> {
                                    hasloWstrzykniete.set(false)
                                    wstrzyknij(SkryptySklepu.WYKRYJ)
                                }
                                "SMS_FIELD_NOT_FOUND" -> {
                                    smsWstrzykniety.set(false)
                                    wstrzyknij(SkryptySklepu.WYKRYJ)
                                }
                                "EMAIL_FIELD_NOT_FOUND" -> {
                                    emailWstrzykniety.set(false)
                                    wstrzyknij(SkryptySklepu.WYKRYJ)
                                }
                            }
                            return
                        }
                        etap = EtapLogowania.BLAD
                        blad = info.ifBlank { "Błąd kroku: $krok" }
                        status = "Nie udało się dokończyć logowania"
                    } else when (krok) {
                        "ciasteczka" -> {
                            if (info.contains("PRZEJDZ") || info.contains("TEKST:przejd")) {
                                status = "Kliknięto Przejdź do sklepu…"
                            }
                        }
                        "wybor" -> {
                            status = "Przechodzę do formularza email / SMS…"
                        }
                        "sms" -> {
                            status = "Kod SMS wpisany — czekam na hasło…"
                            oczekujeSms = false
                            // Nowa strona z hasłem — pozwól wstrzyknąć hasło od zera
                            hasloWstrzykniete.set(false)
                            webView?.postDelayed({
                                if (!zalogowany.get() && !hasloWstrzykniete.get()) {
                                    wstrzyknij(SkryptySklepu.WYKRYJ)
                                }
                            }, 900)
                        }
                        "haslo" -> status = when (info) {
                            "HASLO_WPISANE" -> "Hasło wpisane…"
                            "CHECKBOX_ZAZNACZONY" -> "Zaznaczam zgodę / politykę…"
                            "CAPTCHA_GOTOWA_CZEKAM" -> "Captcha załadowana — czekam przed Zaloguj…"
                            "PRZYCISK_ZALOGUJ" -> "Klikam Zaloguj się…"
                            "HASLO_WYSLANE" -> "Hasło wysłane — kończę logowanie…"
                            else -> "Finalizowanie logowania…"
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EkranWebViewSklepu(
    onZamknij: () -> Unit,
    trybOdbiorcy: Boolean = false,
    modifier: Modifier = Modifier
) {
    val kontekst = LocalContext.current
    val stan = remember {
        StanLogowaniaSklepu(
            kontekstAplikacji = kontekst.applicationContext,
            email = PreferencjeKonta.email(kontekst),
            haslo = PreferencjeKonta.haslo(kontekst)
        )
    }

    // Pracownik: lokalny SMS. Rodzina: kod z Firebase (relay z telefonu pracownika).
    DisposableEffect(stan.oczekujeSms, trybOdbiorcy) {
        if (!stan.oczekujeSms || trybOdbiorcy) {
            return@DisposableEffect onDispose { }
        }
        val od = stan.smsOdKiedyMs.get().takeIf { it > 0L } ?: System.currentTimeMillis()
        val nasluch = NasluchSms(
            kontekst = kontekst.applicationContext,
            odKiedyMs = od,
            onKod = { kod -> stan.naKodSms(kod) }
        )
        nasluch.start()
        onDispose { nasluch.stop() }
    }

    LaunchedEffect(stan.oczekujeSms, trybOdbiorcy) {
        if (!trybOdbiorcy || !stan.oczekujeSms) return@LaunchedEffect
        if (!PreferencjeOdbiorcy.czyZatwierdzony(kontekst) &&
            PreferencjeOdbiorcy.uidNadajnika(kontekst).isBlank()
        ) return@LaunchedEffect
        SerwisParowania.obserwujKodSms(kontekst).collectLatest { kod ->
            stan.naKodSms(kod)
        }
    }

    BackHandler {
        val wv = stan.webView
        when {
            wv != null && wv.canGoBack() -> stan.obsluzWstecz()
            !StaleSklepu.czyGlownaSklepu(stan.aktualnyUrl) -> stan.obsluzWstecz()
            // Na stronie głównej sklepu — wróć do apki, nie zamykaj procesu
            else -> onZamknij()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TloAmoled)
    ) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Pasek pod status/wycięciem — spycha WebView w dół
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TloWarstwa)
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PrzyciskAnimowanySklepu(
                ikona = IkonaWyjsciaSklepu,
                opis = "Wyjdź ze sklepu",
                kolor = TekstGlowny,
                animacja = AnimacjaPrzyciskuSklepu.PULSE,
                onClick = onZamknij
            )
            PrzyciskAnimowanySklepu(
                ikona = Icons.AutoMirrored.Rounded.ArrowBack,
                opis = "Wstecz",
                kolor = TekstDrugi,
                animacja = AnimacjaPrzyciskuSklepu.COFNIECIE,
                onClick = { stan.obsluzWstecz() }
            )
            Text(
                text = if (stan.trybReczny) "Sklep — tryb ręczny" else "Sklep firmowy",
                color = if (stan.trybReczny) Ostrzezenie else TekstGlowny,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            PrzyciskAnimowanySklepu(
                ikona = Icons.Rounded.Refresh,
                opis = "Odśwież",
                kolor = AkcentNeon,
                animacja = AnimacjaPrzyciskuSklepu.OBROT,
                onClick = { stan.odswiez() }
            )
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        settings.setSupportZoom(true)
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.textZoom = 100
                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        @Suppress("DEPRECATION")
                        settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
                        settings.mediaPlaybackRequiresUserGesture = true
                        isVerticalScrollBarEnabled = true
                        isHorizontalScrollBarEnabled = false
                        webChromeClient = WebChromeClient()
                        addJavascriptInterface(
                            object {
                                @JavascriptInterface
                                fun onWiadomosc(dane: String) {
                                    post { stan.obsluzWiadomosc(dane) }
                                }
                            },
                            "AndroidBridge"
                        )
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val url = request?.url?.toString().orEmpty()
                                if (url.isBlank()) return false
                                if (!StaleSklepu.czyUrlSklepu(url)) {
                                    view?.loadUrl(StaleSklepu.URL_GLOWNA)
                                    return true
                                }
                                if (StaleSklepu.czyPodejrzany404(url)) {
                                    view?.loadUrl(StaleSklepu.URL_GLOWNA)
                                    return true
                                }
                                return false
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                if (!url.isNullOrBlank()) stan.aktualnyUrl = url
                                if (!stan.zalogowany.get() &&
                                    stan.etap != EtapLogowania.SMS &&
                                    stan.etap != EtapLogowania.HASLO
                                ) {
                                    stan.etap = EtapLogowania.LADOWANIE
                                    stan.status = "Ładowanie…"
                                }
                                view?.evaluateJavascript(SkryptySklepu.MOSTEK, null)
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                if (!url.isNullOrBlank()) stan.aktualnyUrl = url
                                if (StaleSklepu.czyPodejrzany404(url)) {
                                    stan.napraw404JesliTrzeba(url)
                                    return
                                }
                                if (!StaleSklepu.czyUrlSklepu(url) && stan.zalogowany.get()) {
                                    view?.loadUrl(StaleSklepu.URL_GLOWNA)
                                    return
                                }
                                // Po udanym logowaniu Magento samo prowadzi na sklep —
                                // jeśli jesteśmy poza access/login, schowaj nakładkę.
                                val u = url?.lowercase().orEmpty()
                                if (stan.hasloWstrzykniete.get() &&
                                    StaleSklepu.czyUrlSklepu(u) &&
                                    !u.contains("access") &&
                                    !u.contains("login")
                                ) {
                                    stan.poZalogowaniu()
                                }
                                stan.poZaladowaniuStrony(url)
                            }
                        }
                        stan.webView = this
                        // Najpierw strona główna — jeśli sesja żyje, nakładka znika od razu
                        loadUrl(StaleSklepu.URL_GLOWNA)
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { stan.webView = it }
            )

            if (!stan.ukryjNakladke && stan.etap != EtapLogowania.GOTOWE) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.72f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(TloWarstwa)
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (stan.etap != EtapLogowania.BLAD) {
                            CircularProgressIndicator(color = AkcentNeon)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        Text(
                            text = "Auto-logowanie",
                            color = TekstGlowny,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = stan.status, color = TekstDrugi, fontSize = 13.sp)
                        stan.blad?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = it, color = Color(0xFFF87171), fontSize = 12.sp)
                        }

                        if (stan.oczekujeSms) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (trybOdbiorcy) {
                                    "Czekam na kod z telefonu pracownika. Awaryjnie wpisz poniżej:"
                                } else {
                                    "Kod z SMS wpisze się automatycznie. Awaryjnie wpisz poniżej:"
                                },
                                color = TekstDrugi,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = stan.kodSms,
                                onValueChange = { nowy ->
                                    stan.kodSms = nowy.filter { c -> c.isDigit() }.take(8)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Kod SMS") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AkcentNeon,
                                    unfocusedBorderColor = TekstDrugi.copy(alpha = 0.35f),
                                    focusedTextColor = TekstGlowny,
                                    unfocusedTextColor = TekstGlowny,
                                    cursorColor = AkcentNeon,
                                    focusedLabelColor = AkcentNeon,
                                    unfocusedLabelColor = TekstDrugi
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )
                            TextButton(
                                onClick = {
                                    if (stan.kodSms.length >= 4) {
                                        stan.smsWstrzykniety.set(false)
                                        stan.naKodSms(stan.kodSms)
                                    }
                                }
                            ) {
                                Text("Wyślij kod", color = AkcentNeon, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { stan.ukryjNakladke = true }) {
                            Text("Pokaż stronę", color = TekstDrugi)
                        }
                    }
                }
            }
        }

        // miejsce na wysokość dolnego paska (panel nakłada się overlayem)
        Spacer(modifier = Modifier.height(72.dp))
    }

        DolnyPasekSklepu(
            onZapiszKoszyk = { nazwa -> stan.zapiszAktualnyKoszyk(nazwa) },
            onOtworzKoszykSklepu = { stan.otworzKoszykSklepu() },
            onDodajPozycje = { pozycje -> stan.dodajZapisaneDoKoszyka(pozycje) },
            modifier = Modifier.fillMaxSize()
        )
    }
}
