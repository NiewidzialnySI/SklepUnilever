package shopunilever.dominik.pl.ui.pracownik

import android.annotation.SuppressLint
import android.graphics.Bitmap
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
import shopunilever.dominik.pl.dane.PreferencjeKonta
import shopunilever.dominik.pl.dane.PreferencjeKoszykow
import shopunilever.dominik.pl.dane.PreferencjeParowania
import shopunilever.dominik.pl.firebase.SerwisParowania
import shopunilever.dominik.pl.sklep.NasluchSms
import shopunilever.dominik.pl.sklep.SkryptyKoszyka
import shopunilever.dominik.pl.sklep.SkryptySklepu
import shopunilever.dominik.pl.sklep.StaleSklepu
import shopunilever.dominik.pl.ui.theme.AkcentNeon
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

/**
 * Maszyna stanów auto-logowania.
 * Decyzje po DOM (WYKRYJ), nie po samym URL — jak w Harmonogramie.
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

    var etap by mutableStateOf(EtapLogowania.LADOWANIE)
    var status by mutableStateOf("Ładowanie sklepu…")
    var blad by mutableStateOf<String?>(null)
    var oczekujeSms by mutableStateOf(false)
    var kodSms by mutableStateOf("")
    var ukryjNakladke by mutableStateOf(false)
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

    fun zapiszAktualnyKoszyk() {
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
        val urlki = pozycje.map { it.url.trim() }.filter { it.isNotBlank() }
        if (urlki.isEmpty()) {
            toast("Brak linków produktów — nie da się dodać automatycznie")
            return
        }
        kolejkaDodawania.clear()
        kolejkaDodawania.addAll(urlki)
        dodawanieTrwa.set(true)
        toast("Dodaję ${urlki.size} produkt(ów) do koszyka…")
        nastepnyProduktZKolejki()
    }

    private fun nastepnyProduktZKolejki() {
        val wv = webView ?: return
        val url = kolejkaDodawania.pollFirst()
        if (url == null) {
            dodawanieTrwa.set(false)
            toast("Gotowe — otwieram koszyk")
            wv.loadUrl(SkryptyKoszyka.URL_KOSZYKA)
            return
        }
        wv.loadUrl(url)
        wv.postDelayed({
            wstrzyknij(SkryptySklepu.MOSTEK)
            wstrzyknij(SkryptyKoszyka.KLIKNIJ_DODAJ_DO_KOSZYKA)
            wv.postDelayed({ nastepnyProduktZKolejki() }, 1600)
        }, 1100)
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
        if (emailWyslany.get() || !emailWstrzykniety.compareAndSet(false, true)) return
        etap = EtapLogowania.EMAIL
        status = "Wpisuję email służbowy…"
        oczekujeSms = false
        wstrzyknij(SkryptySklepu.skryptWypelnijEmail(email))
    }

    fun wstrzyknijHaslo() {
        if (!hasloWstrzykniete.compareAndSet(false, true)) return
        oczekujeSms = false
        etap = EtapLogowania.HASLO
        status = "Wpisuję hasło…"
        wstrzyknij(SkryptySklepu.skryptWypelnijHaslo(haslo))
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

    /** Po załadowaniu: cookies, potem szybkie sprawdzenie sesji (już zalogowany?). */
    fun poZaladowaniuStrony(url: String?) {
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
        status = if (toBramka) "Sprawdzam bramkę logowania…" else "Sprawdzam, czy jesteś już zalogowany…"

        // Szybkie sprawdzenie sesji (nie czekaj 4 s na nakładce)
        webView?.postDelayed({
            if (zalogowany.get()) return@postDelayed
            wstrzyknij(SkryptySklepu.CIASTECZKA)
            wstrzyknij(SkryptySklepu.SESJA)
            wstrzyknij(SkryptySklepu.WYKRYJ)
        }, 500)

        // Amasty / Knockout — powtórka cookies + sesja
        listOf(1200L, 2500L).forEach { opoznienie ->
            webView?.postDelayed({
                if (zalogowany.get()) return@postDelayed
                wstrzyknij(SkryptySklepu.CIASTECZKA)
                wstrzyknij(SkryptySklepu.SESJA)
                wstrzyknij(SkryptySklepu.WYKRYJ)
            }, opoznienie)
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
                    val zapis = PreferencjeKoszykow.zapisz(
                        kontekstAplikacji,
                        nazwa = "Koszyk ${PreferencjeKoszykow.lista(kontekstAplikacji).size + 1}",
                        pozycje = pozycje
                    )
                    toast("Zapisano „${zapis.nazwa}” (${pozycje.size} poz.)")
                    return
                }

                "KOSZYK_DODANO" -> {
                    // Informacyjnie — kolejka i tak idzie dalej po timeoutach
                    return
                }

                "LOG" -> { /* debug */ }
            }
        } catch (_: Exception) {
            return
        }

        if (zalogowany.get()) return

        try {
            val obj = JSONObject(json)
            when (obj.optString("type")) {
                "EMAIL_WYNIK" -> {
                    if (obj.optString("wynik") == "EMAIL_SENT" && emailWyslany.compareAndSet(false, true)) {
                        smsOdKiedyMs.set(System.currentTimeMillis())
                        zacznijOczekiwanieSms()
                    }
                }

                "WYKRYJ" -> when (obj.optString("wynik")) {
                    "COOKIE_BANNER" -> {
                        status = "Akceptuję cookies — Akceptuj wszystkie / Przejdź do sklepu…"
                        wstrzyknij(SkryptySklepu.CIASTECZKA)
                        webView?.postDelayed({
                            wstrzyknij(SkryptySklepu.CIASTECZKA)
                            wstrzyknij(SkryptySklepu.WYKRYJ)
                        }, 1500)
                    }
                    "EMAIL_FIELD" -> {
                        if (!emailWyslany.get()) wstrzyknijEmail()
                    }
                    "SMS_TOKEN_FIELD" -> {
                        // Nawet bez wcześniejszego EMAIL_SENT — jesteśmy na ekranie tokenu
                        emailWyslany.set(true)
                        zacznijOczekiwanieSms()
                    }
                    "PASSWORD_FIELD" -> {
                        // Powrót sesji / po SMS: od razu hasło → checkbox → captcha → Zaloguj
                        wstrzyknijHaslo()
                    }
                    "ALREADY_LOGGED_IN" -> {
                        status = "Jesteś już zalogowany…"
                        poZalogowaniu()
                    }
                    "NO_RECOGNIZED_ELEMENTS" -> {
                        val naSklepie = StaleSklepu.czyUrlSklepu(aktualnyUrl) &&
                            !aktualnyUrl.contains("access", ignoreCase = true) &&
                            !aktualnyUrl.contains("login", ignoreCase = true)
                        if (naSklepie) {
                            status = "Sesja aktywna — otwieram sklep…"
                            poZalogowaniu()
                        } else {
                            status = "Nie rozpoznano formularza — sprawdzam sesję…"
                            wstrzyknij(SkryptySklepu.SESJA)
                        }
                    }
                }

                "SESJA" -> when (obj.optString("wynik")) {
                    "LOGGED_IN" -> {
                        status = "Jesteś już zalogowany…"
                        poZalogowaniu()
                    }
                    "NOT_FOUND" -> {
                        status = "Strona nie istnieje — wracam na główną sklepu…"
                        webView?.loadUrl(StaleSklepu.URL_GLOWNA)
                    }
                    "NEEDS_LOGIN" -> {
                        if (!aktualnyUrl.contains("access", ignoreCase = true)) {
                            status = "Brak sesji — przechodzę do logowania…"
                            wstrzyknij(SkryptySklepu.skryptPrzekieruj(StaleSklepu.URL_LOGOWANIA))
                        } else {
                            wstrzyknij(SkryptySklepu.WYKRYJ)
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

                "KROK" -> {
                    val krok = obj.optString("krok")
                    val sukces = obj.optBoolean("sukces", true)
                    val info = obj.optString("info")
                    if (!sukces) {
                        if (info in setOf(
                                "PASS_FIELD_NOT_FOUND",
                                "EMAIL_FIELD_NOT_FOUND",
                                "SMS_FIELD_NOT_FOUND"
                            )
                        ) {
                            // Strona mogła się zmienić — ponów wykrycie
                            if (info == "SMS_FIELD_NOT_FOUND") {
                                smsWstrzykniety.set(false)
                                wstrzyknij(SkryptySklepu.WYKRYJ)
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
                        "sms" -> {
                            status = "Kod SMS wpisany — czekam na hasło…"
                            oczekujeSms = false
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

    // Auto-odczyt SMS gdy czekamy na token
    DisposableEffect(stan.oczekujeSms) {
        if (!stan.oczekujeSms) {
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

    BackHandler {
        stan.obsluzWstecz()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TloAmoled)
    ) {
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
                text = "Sklep firmowy",
                color = TekstGlowny,
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
                                text = "Kod z SMS wpisze się automatycznie. Pole poniżej tylko awaryjnie:",
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

        DolnyPasekSklepu(
            onZapiszKoszyk = { stan.zapiszAktualnyKoszyk() },
            onOtworzKoszykSklepu = { stan.otworzKoszykSklepu() },
            onDodajPozycje = { pozycje -> stan.dodajZapisaneDoKoszyka(pozycje) }
        )
    }
}
