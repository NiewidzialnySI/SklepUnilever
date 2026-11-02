package shopunilever.dominik.pl.ui.rodzina

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import shopunilever.dominik.pl.dane.PreferencjeKonta
import shopunilever.dominik.pl.dane.PreferencjeOdbiorcy
import shopunilever.dominik.pl.firebase.SerwisParowania
import shopunilever.dominik.pl.firebase.StaleFirebase
import shopunilever.dominik.pl.firebase.WynikWejsciaSklepu
import shopunilever.dominik.pl.ui.components.SzkloKarta
import shopunilever.dominik.pl.ui.components.TloAplikacji
import shopunilever.dominik.pl.ui.pracownik.EkranWebViewSklepu
import shopunilever.dominik.pl.ui.theme.AkcentMorski
import shopunilever.dominik.pl.ui.theme.AkcentNeon
import shopunilever.dominik.pl.ui.theme.Blad
import shopunilever.dominik.pl.ui.theme.Sukces
import shopunilever.dominik.pl.ui.theme.TekstDrugi
import shopunilever.dominik.pl.ui.theme.TekstGlowny
import shopunilever.dominik.pl.ui.theme.TekstNaAkcencie

@Composable
fun EkranRodziny(modifier: Modifier = Modifier) {
    val kontekst = LocalContext.current
    val zakres = rememberCoroutineScope()

    var status by remember { mutableStateOf(PreferencjeOdbiorcy.status(kontekst)) }
    var kodWpisywany by remember { mutableStateOf("") }
    var ladowanie by remember { mutableStateOf(false) }
    var blad by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    var pokazSklep by remember { mutableStateOf(false) }
    var czekaNaLogowanie by remember { mutableStateOf(false) }

    fun poOdlaczeniuPrzezPracownika(komunikat: String) {
        PreferencjeOdbiorcy.wyczysc(kontekst)
        PreferencjeKonta.wyczysc(kontekst)
        pokazSklep = false
        czekaNaLogowanie = false
        status = ""
        kodWpisywany = ""
        info = null
        blad = komunikat
    }

    suspend fun otworzSklepPoZgodzie() {
        val dane = SerwisParowania.pobierzDaneLogowania(kontekst)
        if (dane == null || dane.email.isBlank() || dane.haslo.isBlank()) {
            blad = "Brak danych logowania — pracownik musi zapisać email i hasło w Ustawieniach."
            return
        }
        PreferencjeKonta.zapiszDane(kontekst, dane.email, dane.haslo)
        runCatching { SerwisParowania.zuzyjZgodeLogowania(kontekst) }
        runCatching { SerwisParowania.zapiszLogWejscia(kontekst) }
        czekaNaLogowanie = false
        pokazSklep = true
    }

    LaunchedEffect(status) {
        val tx = PreferencjeOdbiorcy.uidNadajnika(kontekst)
        if (tx.isBlank()) return@LaunchedEffect
        SerwisParowania.obserwujStatusProsby(kontekst).collectLatest { st ->
            when (st) {
                "approved" -> {
                    status = "approved"
                    runCatching {
                        SerwisParowania.pobierzDaneLogowania(kontekst)?.let { dane ->
                            PreferencjeKonta.zapiszDane(kontekst, dane.email, dane.haslo)
                        }
                    }
                    blad = null
                    info = null
                }
                "pending" -> {
                    status = "pending"
                }
                "rejected" -> {
                    poOdlaczeniuPrzezPracownika("Prośba odrzucona — zeskanuj nowy kod od pracownika.")
                }
                "revoked" -> {
                    poOdlaczeniuPrzezPracownika(
                        "Pracownik odłączył to urządzenie — zeskanuj nowy kod, żeby połączyć się ponownie."
                    )
                }
                else -> {
                    if (st == "brak") {
                        status = PreferencjeOdbiorcy.status(kontekst).ifBlank { "" }
                    }
                }
            }
        }
    }

    LaunchedEffect(status, czekaNaLogowanie) {
        if (status != "approved" || !czekaNaLogowanie) return@LaunchedEffect
        SerwisParowania.obserwujStatusLogowania(kontekst).collectLatest { st ->
            when (st) {
                "approved" -> {
                    info = "Pracownik zezwolił — otwieram sklep…"
                    blad = null
                    runCatching { otworzSklepPoZgodzie() }
                        .onFailure { blad = it.message }
                }
                "rejected" -> {
                    czekaNaLogowanie = false
                    blad = "Pracownik odrzucił logowanie."
                    info = null
                }
                "pending" -> {
                    info = "Czekam na zgodę pracownika na logowanie…"
                }
            }
        }
    }

    if (pokazSklep && status == "approved") {
        EkranWebViewSklepu(
            onZamknij = { pokazSklep = false },
            trybOdbiorcy = true,
            modifier = modifier
        )
        return
    }

    suspend fun dolacz(kod: String) {
        ladowanie = true
        blad = null
        info = null
        runCatching {
            SerwisParowania.dolaczJakoOdbiorca(kontekst, kod)
        }.onSuccess {
            status = "pending"
            info = "Wysłano prośbę — poczekaj na akceptację na telefonie pracownika (zakładka Urządzenia)."
        }.onFailure {
            blad = it.message ?: "Nie udało się połączyć."
        }
        ladowanie = false
    }

    fun skanujQr() {
        zakres.launch {
            ladowanie = true
            blad = null
            runCatching {
                val opcje = GmsBarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .enableAutoZoom()
                    .build()
                val skaner = GmsBarcodeScanning.getClient(kontekst, opcje)
                val wynik = skaner.startScan().await()
                val tresc = wynik.rawValue.orEmpty()
                val kod = SerwisParowania.wyodrebnijKodZQr(tresc)
                if (kod.length != StaleFirebase.DLUGOSC_KODU) {
                    error("Nie rozpoznano kodu parowania w QR")
                }
                kodWpisywany = kod.chunked(4).joinToString(" ")
                dolacz(kod)
            }.onFailure {
                if (it.message?.contains("canceled", ignoreCase = true) != true &&
                    it.message?.contains("cancelled", ignoreCase = true) != true
                ) {
                    blad = it.message ?: "Skanowanie nie powiodło się."
                }
                ladowanie = false
            }
        }
    }

    TloAplikacji(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "RODZINA",
                color = AkcentNeon,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Połączenie",
                color = TekstGlowny,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(22.dp))

            when (status) {
                "approved" -> {
                    SzkloKarta {
                        Text(
                            text = "Sparowano",
                            color = Sukces,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Kod: ${PreferencjeOdbiorcy.kod(kontekst).chunked(4).joinToString(" ")}",
                            color = TekstDrugi,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Brush.horizontalGradient(listOf(AkcentNeon, AkcentMorski)))
                                .clickable(enabled = !ladowanie && !czekaNaLogowanie) {
                                    zakres.launch {
                                        ladowanie = true
                                        blad = null
                                        info = null
                                        runCatching {
                                            when (val wynik = SerwisParowania.przygotujWejscieDoSklepu(kontekst)) {
                                                is WynikWejsciaSklepu.MoznaWejsc -> otworzSklepPoZgodzie()
                                                is WynikWejsciaSklepu.CzekaNaAkceptacje -> {
                                                    czekaNaLogowanie = true
                                                    info = "Wysłano prośbę o logowanie — czekaj na akceptację pracownika."
                                                }
                                                is WynikWejsciaSklepu.Blad -> {
                                                    if (wynik.komunikat.contains("odłącz", ignoreCase = true) ||
                                                        wynik.komunikat.contains("kod", ignoreCase = true)
                                                    ) {
                                                        poOdlaczeniuPrzezPracownika(wynik.komunikat)
                                                    } else {
                                                        blad = wynik.komunikat
                                                    }
                                                }
                                            }
                                        }.onFailure {
                                            val msg = it.message.orEmpty()
                                            if (msg.contains("Permission", ignoreCase = true) ||
                                                msg.contains("denied", ignoreCase = true)
                                            ) {
                                                poOdlaczeniuPrzezPracownika(
                                                    "Brak dostępu — zeskanuj nowy kod od pracownika."
                                                )
                                            } else {
                                                blad = it.message
                                            }
                                        }
                                        ladowanie = false
                                    }
                                }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (czekaNaLogowanie) {
                                    CircularProgressIndicator(
                                        color = TekstNaAkcencie,
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.size(8.dp))
                                    Text(
                                        "Czekam na zgodę…",
                                        color = TekstNaAkcencie,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Icon(Icons.Rounded.Login, null, tint = TekstNaAkcencie)
                                    Spacer(modifier = Modifier.size(8.dp))
                                    Text(
                                        "Zaloguj do sklepu",
                                        color = TekstNaAkcencie,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        if (czekaNaLogowanie) {
                            TextButton(onClick = {
                                czekaNaLogowanie = false
                                info = null
                                zakres.launch {
                                    runCatching { SerwisParowania.zuzyjZgodeLogowania(kontekst) }
                                }
                            }) {
                                Text("Anuluj prośbę", color = TekstDrugi)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    KafelekPaczek()
                }

                "pending" -> {
                    SzkloKarta {
                        Text("Oczekiwanie na akceptację", color = TekstGlowny, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AkcentNeon, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Kod ${PreferencjeOdbiorcy.kod(kontekst).chunked(4).joinToString(" ")} — pracownik musi zatwierdzić prośbę w zakładce Urządzenia.",
                            color = TekstDrugi,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextButton(onClick = {
                            PreferencjeOdbiorcy.wyczysc(kontekst)
                            status = ""
                            blad = null
                            info = null
                        }) {
                            Text("Anuluj", color = TekstDrugi)
                        }
                    }
                }

                else -> {
                    SzkloKarta {
                        Text("Zeskanuj lub wpisz kod", color = TekstGlowny, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Spacer(modifier = Modifier.height(14.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Brush.horizontalGradient(listOf(AkcentNeon, AkcentMorski)))
                                .clickable(enabled = !ladowanie) { skanujQr() }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.QrCodeScanner, null, tint = TekstNaAkcencie)
                                Spacer(modifier = Modifier.size(8.dp))
                                Text("Skanuj kod QR", color = TekstNaAkcencie, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "albo wpisz ręcznie",
                            color = TekstDrugi,
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = kodWpisywany,
                            onValueChange = { nowy ->
                                val czysty = SerwisParowania.wyodrebnijKodZQr(nowy)
                                kodWpisywany = czysty.chunked(4).joinToString(" ")
                                blad = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Kod parowania") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
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
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .clickable(enabled = !ladowanie) {
                                    val kod = SerwisParowania.wyodrebnijKodZQr(kodWpisywany)
                                    if (kod.length != StaleFirebase.DLUGOSC_KODU) {
                                        blad = "Kod musi mieć ${StaleFirebase.DLUGOSC_KODU} znaków"
                                    } else {
                                        zakres.launch { dolacz(kod) }
                                    }
                                }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Połącz", color = TekstGlowny, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (ladowanie) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AkcentNeon, modifier = Modifier.size(28.dp))
                }
            }
            blad?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(it, color = Blad, fontSize = 13.sp, lineHeight = 18.sp)
            }
            info?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(it, color = Sukces, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun KafelekPaczek() {
    SzkloKarta {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(AkcentMorski.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Inventory2,
                    contentDescription = null,
                    tint = AkcentMorski,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "PACZKI",
                    color = AkcentNeon,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Informacje o przesyłkach",
                    color = TekstGlowny,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Brak aktywnych paczek",
            color = TekstDrugi,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}
