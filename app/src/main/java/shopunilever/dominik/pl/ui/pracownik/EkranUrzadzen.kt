package shopunilever.dominik.pl.ui.pracownik

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Phonelink
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import shopunilever.dominik.pl.dane.PreferencjeParowania
import shopunilever.dominik.pl.firebase.OdbiorcaPolaczony
import shopunilever.dominik.pl.firebase.ProsbaDostepu
import shopunilever.dominik.pl.firebase.SerwisParowania
import shopunilever.dominik.pl.firebase.StaleFirebase
import shopunilever.dominik.pl.ui.components.SzkloKarta
import shopunilever.dominik.pl.ui.theme.AkcentMorski
import shopunilever.dominik.pl.ui.theme.AkcentNeon
import shopunilever.dominik.pl.ui.theme.Blad
import shopunilever.dominik.pl.ui.theme.Ostrzezenie
import shopunilever.dominik.pl.ui.theme.Sukces
import shopunilever.dominik.pl.ui.theme.TekstDrugi
import shopunilever.dominik.pl.ui.theme.TekstGlowny
import shopunilever.dominik.pl.ui.theme.TekstNaAkcencie

@Composable
fun EkranUrzadzen(
    modifier: Modifier = Modifier,
    onParowanieZmienione: () -> Unit = {}
) {
    val kontekst = LocalContext.current
    val zakres = rememberCoroutineScope()

    var kod by remember { mutableStateOf(PreferencjeParowania.kod(kontekst)) }
    var aktywne by remember { mutableStateOf(PreferencjeParowania.czyAktywne(kontekst)) }
    var ladowanie by remember { mutableStateOf(false) }
    var blad by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var prosby by remember { mutableStateOf<List<ProsbaDostepu>>(emptyList()) }
    var odbiorcy by remember { mutableStateOf<List<OdbiorcaPolaczony>>(emptyList()) }
    var akcjaProsby by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(aktywne, kod) {
        if (kod.isBlank()) return@LaunchedEffect
        runCatching { SerwisParowania.upewnijSesjeNadajnika(kontekst) }
        launch {
            SerwisParowania.obserwujProsby(kontekst).collectLatest { prosby = it }
        }
        launch {
            SerwisParowania.obserwujOdbiorcow(kontekst).collectLatest { odbiorcy = it }
        }
    }

    fun uruchomParowanie(nowyKod: Boolean) {
        zakres.launch {
            ladowanie = true
            blad = null
            status = null
            runCatching {
                if (nowyKod) PreferencjeParowania.wyczysc(kontekst)
                SerwisParowania.uruchomJakoNadajnik(kontekst)
            }.onSuccess {
                kod = it
                aktywne = true
                status = "Parowanie aktywne — kody SMS będą przekazywane bezpiecznie."
                onParowanieZmienione()
            }.onFailure {
                blad = it.message ?: "Nie udało się uruchomić parowania."
            }
            ladowanie = false
        }
    }

    val oczekujace = prosby.filter { it.status == "pending" }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "URZĄDZENIA",
            color = AkcentNeon,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Parowanie",
            color = TekstGlowny,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(18.dp))

        // Prośby NA GÓRZE — zawsze widoczne gdy ktoś czeka.
        if (oczekujace.isNotEmpty()) {
            SzkloKarta {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.NotificationsActive,
                        contentDescription = null,
                        tint = Ostrzezenie
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Nowa prośba o dostęp",
                            color = TekstGlowny,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = if (oczekujace.size == 1) {
                                "Zaakceptuj lub odrzuć urządzenie rodziny"
                            } else {
                                "${oczekujace.size} urządzenia czekają na decyzję"
                            },
                            color = Ostrzezenie,
                            fontSize = 13.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                oczekujace.forEach { p ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .padding(14.dp)
                    ) {
                        Text(
                            p.nazwaUrzadzenia,
                            color = TekstGlowny,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Text(
                            "Oczekuje na Twoją decyzję",
                            color = TekstDrugi,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Sukces.copy(alpha = 0.2f))
                                    .clickable(enabled = akcjaProsby == null) {
                                        zakres.launch {
                                            akcjaProsby = p.id
                                            runCatching {
                                                SerwisParowania.zatwierdzProsbe(kontekst, p.id)
                                            }.onSuccess {
                                                status = "Połączono: ${p.nazwaUrzadzenia}"
                                            }.onFailure {
                                                blad = it.message ?: "Nie udało się zaakceptować."
                                            }
                                            akcjaProsby = null
                                        }
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (akcjaProsby == p.id) {
                                    CircularProgressIndicator(
                                        color = Sukces,
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Akceptuj", color = Sukces, fontWeight = FontWeight.Bold)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Blad.copy(alpha = 0.15f))
                                    .clickable(enabled = akcjaProsby == null) {
                                        zakres.launch {
                                            akcjaProsby = p.id
                                            runCatching {
                                                SerwisParowania.odrzucProsbe(kontekst, p.id)
                                            }.onFailure {
                                                blad = it.message ?: "Nie udało się odrzucić."
                                            }
                                            akcjaProsby = null
                                        }
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Odrzuć", color = Blad, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        } else if (aktywne) {
            SzkloKarta {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = Sukces)
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(
                        "Brak oczekujących próśb",
                        color = TekstGlowny,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Gdy ktoś zeskanuje QR, pojawi się tu przycisk Akceptuj / Odrzuć.",
                    color = TekstDrugi,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        SzkloKarta {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Link, contentDescription = null, tint = AkcentNeon)
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = "Kod parowania",
                    color = TekstGlowny,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (kod.isNotBlank()) {
                val trescQr = "${StaleFirebase.PREFIX_QR}$kod"
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    KodQrParowania(tresc = trescQr, rozmiar = 220.dp)
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = kod.chunked(4).joinToString(" "),
                    color = TekstGlowny,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = {
                        val cm = kontekst.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("kod", kod))
                        status = "Skopiowano kod."
                    }) {
                        Icon(Icons.Rounded.ContentCopy, null, tint = AkcentNeon, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Kopiuj", color = AkcentNeon)
                    }
                    TextButton(onClick = { uruchomParowanie(nowyKod = true) }) {
                        Text("Nowy kod", color = TekstDrugi)
                    }
                }
            } else {
                Text(
                    text = "Wygeneruj kod i zeskanuj go na drugim telefonie.",
                    color = TekstDrugi,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            if (ladowanie) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AkcentNeon, modifier = Modifier.size(28.dp))
                }
            } else if (kod.isBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.horizontalGradient(listOf(AkcentNeon, AkcentMorski)))
                        .clickable { uruchomParowanie(nowyKod = false) }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Uruchom parowanie", color = TekstNaAkcencie, fontWeight = FontWeight.Bold)
                }
            }

            blad?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = Blad, fontSize = 12.sp)
            }
            status?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = Sukces, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SzkloKarta {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Phonelink, null, tint = AkcentMorski)
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = "Przekaz SMS",
                    color = TekstGlowny,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (aktywne) {
                    "Aktywny — przy logowaniu kod SMS trafia tylko do sparowanych urządzeń (sam kod, bez treści wiadomości)."
                } else {
                    "Włącz parowanie, żeby sparowane urządzenia dostawały kody SMS automatycznie."
                },
                color = TekstDrugi,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }

        if (odbiorcy.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            SzkloKarta {
                Text("Sparowane urządzenia", color = TekstGlowny, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(10.dp))
                odbiorcy.forEach { o ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(o.nazwa, color = TekstGlowny, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (o.zablokowany) "Zablokowane" else "Połączone",
                                color = if (o.zablokowany) TekstDrugi else Sukces,
                                fontSize = 12.sp
                            )
                        }
                        TextButton(onClick = {
                            zakres.launch {
                                runCatching { SerwisParowania.usunOdbiorce(kontekst, o.id) }
                            }
                        }) { Text("Odłącz", color = TekstDrugi) }
                    }
                }
            }
        }
    }
}
