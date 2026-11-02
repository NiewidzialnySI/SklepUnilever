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
import shopunilever.dominik.pl.sklep.SkryptySklepu
import shopunilever.dominik.pl.sklep.StaleSklepu
import shopunilever.dominik.pl.ui.theme.AkcentNeon
import shopunilever.dominik.pl.ui.theme.TekstDrugi
import shopunilever.dominik.pl.ui.theme.TekstGlowny
import shopunilever.dominik.pl.ui.theme.TloAmoled
import shopunilever.dominik.pl.ui.theme.TloWarstwa
import java.util.concurrent.atomic.AtomicBoolean

private enum class EtapLogowania {
    LADOWANIE,
    EMAIL,
    SMS,
    HASLO,
    GOTOWE,
    BLAD
}

/** Stan współdzielony z WebViewClient / JS bridge (poza snapshotami Compose). */
private class StanLogowaniaSklepu(
    val email: String,
    val haslo: String
) {
    var webView: WebView? = null
    val emailWyslany = AtomicBoolean(false)
    val emailWstrzykniety = AtomicBoolean(false)
    val hasloWstrzykniete = AtomicBoolean(false)

    var etap by mutableStateOf(EtapLogowania.LADOWANIE)
    var status by mutableStateOf("Ładowanie sklepu…")
    var blad by mutableStateOf<String?>(null)
    var oczekujeSms by mutableStateOf(false)
    var ukryjNakladke by mutableStateOf(false)

    fun wstrzyknij(skrypt: String) {
        webView?.evaluateJavascript(skrypt, null)
    }

    fun obsluzUrl(url: String?) {
        val u = url?.lowercase().orEmpty()
        when {
            u.contains("access-email") ||
                (u.contains("/access") && !u.contains("token") && !u.contains("sms")) -> {
                if (!emailWyslany.get() && emailWstrzykniety.compareAndSet(false, true)) {
                    etap = EtapLogowania.EMAIL
                    status = "Wpisuję email służbowy…"
                    wstrzyknij(SkryptySklepu.skryptWypelnijEmail(email))
                }
            }

            u.contains("access_token") || u.contains("/sms") ||
                (u.contains("token") && u.contains("access")) -> {
                etap = EtapLogowania.SMS
                status = "Oczekiwanie na kod SMS…"
                oczekujeSms = true
            }

            u.contains("login") || u.contains("/customer/account") -> {
                if (hasloWstrzykniete.compareAndSet(false, true)) {
                    etap = EtapLogowania.HASLO
                    status = "Wpisuję hasło…"
                    oczekujeSms = false
                    wstrzyknij(SkryptySklepu.skryptWypelnijHaslo(haslo))
                }
            }

            u.startsWith(StaleSklepu.URL_SKLEPU.lowercase()) &&
                !u.contains("access") &&
                !u.contains("login") -> {
                etap = EtapLogowania.GOTOWE
                status = "Zalogowano"
                oczekujeSms = false
                ukryjNakladke = true
            }
        }
    }

    fun obsluzWiadomosc(json: String) {
        try {
            val obj = JSONObject(json)
            when (obj.optString("type")) {
                "EMAIL_WYNIK" -> {
                    emailWyslany.set(true)
                    etap = EtapLogowania.SMS
                    status = "Email wysłany — czekam na SMS…"
                    oczekujeSms = true
                }

                "KROK" -> {
                    val krok = obj.optString("krok")
                    val sukces = obj.optBoolean("sukces", true)
                    val info = obj.optString("info")
                    if (!sukces) {
                        if (info == "PASS_FIELD_NOT_FOUND" || info == "EMAIL_FIELD_NOT_FOUND") return
                        etap = EtapLogowania.BLAD
                        blad = info.ifBlank { "Błąd kroku: $krok" }
                        status = "Nie udało się dokończyć logowania"
                    } else when (krok) {
                        "sms" -> {
                            status = "Kod SMS wpisany…"
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
    var kodSms by remember { mutableStateOf("") }

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
                    settings.databaseEnabled = true
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
                            if (stan.etap != EtapLogowania.GOTOWE) {
                                stan.etap = EtapLogowania.LADOWANIE
                                stan.status = "Ładowanie…"
                            }
                            view?.evaluateJavascript(SkryptySklepu.MOSTEK, null)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            view?.evaluateJavascript(SkryptySklepu.MOSTEK, null)
                            stan.obsluzUrl(url)
                        }
                    }
                    stan.webView = this
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
                        OutlinedTextField(
                            value = kodSms,
                            onValueChange = { kodSms = it.filter { c -> c.isDigit() }.take(8) },
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
                                if (kodSms.length >= 4) {
                                    stan.status = "Wpisuję kod SMS…"
                                    stan.wstrzyknij(SkryptySklepu.skryptWypelnijSms(kodSms))
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
