package shopunilever.dominik.pl.ui.rodzina

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LinkOff
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
import shopunilever.dominik.pl.ui.components.SzkloKarta
import shopunilever.dominik.pl.ui.components.TloAplikacji
import shopunilever.dominik.pl.ui.pracownik.EkranWebViewSklepu
import shopunilever.dominik.pl.ui.theme.AkcentMorski
import shopunilever.dominik.pl.ui.theme.AkcentNeon
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

    LaunchedEffect(PreferencjeOdbiorcy.uidNadajnika(kontekst), status) {
        if (PreferencjeOdbiorcy.uidNadajnika(kontekst).isBlank()) return@LaunchedEffect
        SerwisParowania.obserwujStatusProsby(kontekst).collectLatest { st ->
            status = st
            if (st == "approved") {
                runCatching {
                    SerwisParowania.pobierzDaneLogowania(kontekst)?.let { dane ->
                        PreferencjeKonta.zapiszDane(kontekst, dane.email, dane.haslo)
                    }
                }
                info = "Połączono — możesz zalogować się do sklepu."
            } else if (st == "rejected") {
                blad = "Prośba odrzucona."
            }
        }
    }

    if (pokazSklep) {
        EkranWebViewSklepu(
            onZamknij = { pokazSklep = false },
            trybOdbiorcy = true,
            modifier = modifier
        )
        return
    }

    fun dolacz(kod: String) {
        zakres.launch {
            ladowanie = true
            blad = null
            info = null
            runCatching {
                SerwisParowania.dolaczJakoOdbiorca(kontekst, kod)
            }.onSuccess {
                status = "pending"
                info = "Wysłano prośbę — poczekaj na akceptację pracownika."
            }.onFailure {
                blad = it.message ?: "Nie udało się połączyć."
            }
            ladowanie = false
        }
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
                                .clickable {
                                    zakres.launch {
                                        ladowanie = true
                                        runCatching {
                                            SerwisParowania.pobierzDaneLogowania(kontekst)?.let { dane ->
                                                PreferencjeKonta.zapiszDane(kontekst, dane.email, dane.haslo)
                                                pokazSklep = true
                                            } ?: run {
                                                blad = "Brak danych logowania — poczekaj aż pracownik zapisze konto."
                                            }
                                        }.onFailure {
                                            blad = it.message
                                        }
                                        ladowanie = false
                                    }
                                }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Login, null, tint = TekstNaAkcencie)
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(
                                    "Zaloguj do sklepu",
                                    color = TekstNaAkcencie,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        TextButton(onClick = {
                            PreferencjeOdbiorcy.wyczysc(kontekst)
                            status = ""
                            info = null
                        }) {
                            Icon(Icons.Rounded.LinkOff, null, tint = TekstDrugi, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Odłącz", color = TekstDrugi)
                        }
                    }
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
                        Text("albo wpisz ręcznie", color = TekstDrugi, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
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
                                        dolacz(kod)
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
                Text(it, color = Color(0xFFF87171), fontSize = 13.sp)
            }
            info?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(it, color = Sukces, fontSize = 13.sp)
            }
        }
    }
}
