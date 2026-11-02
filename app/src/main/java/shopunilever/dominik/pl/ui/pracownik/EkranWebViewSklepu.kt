package shopunilever.dominik.pl.ui.pracownik

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import org.json.JSONObject
import shopunilever.dominik.pl.dane.PreferencjeKonta
import shopunilever.dominik.pl.sklep.NasluchSms
import shopunilever.dominik.pl.sklep.SkryptySklepu
import shopunilever.dominik.pl.sklep.StaleSklepu
import shopunilever.dominik.pl.ui.theme.AkcentNeon
import shopunilever.dominik.pl.ui.theme.TekstDrugi
import shopunilever.dominik.pl.ui.theme.TekstGlowny
import shopunilever.dominik.pl.ui.theme.TloAmoled
import shopunilever.dominik.pl.ui.theme.TloWarstwa
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
    val email: String,
    val haslo: String
) {
    var webView: WebView? = null
    val emailWyslany = AtomicBoolean(false)
    val emailWstrzykniety = AtomicBoolean(false)
    val hasloWstrzykniete = AtomicBoolean(false)
    val smsWstrzykniety = AtomicBoolean(false)
    val zalogowany = AtomicBoolean(false)
    val smsOdKiedyMs = AtomicLong(0L)

    var etap by mutableStateOf(EtapLogowania.LADOWANIE)
    var status by mutableStateOf("Ładowanie sklepu…")
    var blad by mutableStateOf<String?>(null)
    var oczekujeSms by mutableStateOf(false)
    var kodSms by mutableStateOf("")
    var ukryjNakladke by mutableStateOf(false)

    fun wstrzyknij(skrypt: String) {
        webView?.evaluateJavascript(skrypt, null)
    }

    fun poZalogowaniu() {
        if (!zalogowany.compareAndSet(false, true)) return
        oczekujeSms = false
        etap = EtapLogowania.GOTOWE
        status = "Zalogowano"
        ukryjNakladke = true
        wstrzyknij(SkryptySklepu.skryptPrzekieruj(StaleSklepu.URL_MARKET))
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
        status = "Wpisuję hasło i zaznaczam zgodę…"
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
    }

    /** Po załadowaniu strony — zawsze najpierw wykryj elementy DOM. */
    fun poZaladowaniuStrony(url: String?) {
        if (zalogowany.get()) return
        wstrzyknij(SkryptySklepu.MOSTEK)

        val u = url?.lowercase().orEmpty()
        if (u.startsWith(StaleSklepu.URL_SKLEPU.lowercase()) &&
            !u.contains("access") &&
            !u.contains("login") &&
            !u.contains("customer/account/login")
        ) {
            etap = EtapLogowania.LADOWANIE
            status = "Sprawdzanie sesji…"
            wstrzyknij(SkryptySklepu.SESJA)
            return
        }

        // Główna ścieżka: wykryj formularz na stronie (email / SMS / hasło)
        wstrzyknij(SkryptySklepu.WYKRYJ)
    }

    fun obsluzWiadomosc(json: String) {
        if (zalogowany.get()) return
        try {
            val obj = JSONObject(json)
            when (obj.optString("type")) {
                "LOG" -> { /* debug */ }

                "EMAIL_WYNIK" -> {
                    if (obj.optString("wynik") == "EMAIL_SENT" && emailWyslany.compareAndSet(false, true)) {
                        smsOdKiedyMs.set(System.currentTimeMillis())
                        zacznijOczekiwanieSms()
                    }
                }

                "WYKRYJ" -> when (obj.optString("wynik")) {
                    "EMAIL_FIELD" -> {
                        if (!emailWyslany.get()) wstrzyknijEmail()
                    }
                    "SMS_TOKEN_FIELD" -> {
                        // Nawet bez wcześniejszego EMAIL_SENT — jesteśmy na ekranie tokenu
                        emailWyslany.set(true)
                        zacznijOczekiwanieSms()
                    }
                    "PASSWORD_FIELD" -> {
                        // Powrót sesji / po SMS: od razu hasło + checkbox, bez SMS
                        wstrzyknijHaslo()
                    }
                    "ALREADY_LOGGED_IN" -> poZalogowaniu()
                    "NO_RECOGNIZED_ELEMENTS" -> {
                        if (etap == EtapLogowania.LADOWANIE) {
                            status = "Nie rozpoznano formularza — sprawdzam sesję…"
                            wstrzyknij(SkryptySklepu.SESJA)
                        }
                    }
                }

                "SESJA" -> when (obj.optString("wynik")) {
                    "LOGGED_IN" -> poZalogowaniu()
                    "NEEDS_LOGIN" -> {
                        status = "Przekierowanie do logowania…"
                        wstrzyknij(SkryptySklepu.skryptPrzekieruj(StaleSklepu.URL_LOGOWANIA))
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
                        "sms" -> {
                            status = "Kod SMS wpisany — czekam na hasło…"
                            oczekujeSms = false
                        }
                        "haslo" -> status = "Hasło wysłane — kończę logowanie…"
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
        val wv = stan.webView
        if (wv != null && wv.canGoBack()) {
            wv.goBack()
        } else {
            onZamknij()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(TloAmoled)) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
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
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
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
                            stan.poZaladowaniuStrony(url)
                        }
                    }
                    stan.webView = this
                    // Start od strony logowania — WYKRYJ zdecyduje: email vs hasło (sesja)
                    loadUrl(StaleSklepu.URL_LOGOWANIA)
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { stan.webView = it }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onZamknij) {
                Icon(Icons.Rounded.Close, contentDescription = "Zamknij", tint = TekstGlowny)
            }
        }

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
}
