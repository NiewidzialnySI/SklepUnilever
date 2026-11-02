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
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import shopunilever.dominik.pl.firebase.ProsbaLogowania
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
    var prosbyLogowania by remember { mutableStateOf<List<ProsbaLogowania>>(emptyList()) }
    var odbiorcy by remember { mutableStateOf<List<OdbiorcaPolaczony>>(emptyList()) }
    var akcjaProsby by remember { mutableStateOf<String?>(null) }

    var edytowanyOdbiorca by remember { mutableStateOf<OdbiorcaPolaczony?>(null) }
    var nazwaEdycji by remember { mutableStateOf("") }
    var doOdlaczenia by remember { mutableStateOf<OdbiorcaPolaczony?>(null) }

    LaunchedEffect(aktywne, kod) {
        if (kod.isBlank()) return@LaunchedEffect
        runCatching { SerwisParowania.upewnijSesjeNadajnika(kontekst) }
        launch {
            SerwisParowania.obserwujProsby(kontekst).collectLatest { prosby = it }
        }
        launch {
            SerwisParowania.obserwujProsbyLogowania(kontekst).collectLatest { prosbyLogowania = it }
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
                status = "Parowanie aktywne."
                onParowanieZmienione()
            }.onFailure {
                blad = it.message ?: "Nie udało się uruchomić parowania."
            }
            ladowanie = false
        }
    }

    val oczekujace = prosby.filter { it.status == "pending" }
    val logowaniaOczekujace = prosbyLogowania.filter { it.status == "pending" }

    edytowanyOdbiorca?.let { o ->
        AlertDialog(
            onDismissRequest = { edytowanyOdbiorca = null },
            title = { Text("Nazwa urządzenia", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = nazwaEdycji,
                    onValueChange = { nazwaEdycji = it.take(40) },
                    singleLine = true,
                    label = { Text("Nazwa") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AkcentNeon,
                        focusedLabelColor = AkcentNeon,
                        cursorColor = AkcentNeon
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    zakres.launch {
                        runCatching {
                            SerwisParowania.zmienNazweOdbiorcy(kontekst, o.id, nazwaEdycji)
                        }.onSuccess {
                            status = "Zmieniono nazwę."
                        }.onFailure {
                            blad = it.message
                        }
                        edytowanyOdbiorca = null
                    }
                }) { Text("Zapisz", color = AkcentNeon, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { edytowanyOdbiorca = null }) {
                    Text("Anuluj", color = TekstDrugi)
                }
            }
        )
    }

    doOdlaczenia?.let { o ->
        AlertDialog(
            onDismissRequest = { doOdlaczenia = null },
            title = { Text("Odłączyć urządzenie?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "„${o.nazwa}” straci dostęp do sklepu. Żeby połączyć się ponownie, trzeba będzie zeskanować nowy kod."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    zakres.launch {
                        runCatching { SerwisParowania.usunOdbiorce(kontekst, o.id) }
                            .onSuccess { status = "Odłączono: ${o.nazwa}" }
                            .onFailure { blad = it.message }
                        doOdlaczenia = null
                    }
                }) { Text("Odłącz", color = Blad, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { doOdlaczenia = null }) {
                    Text("Anuluj", color = TekstDrugi)
                }
            }
        )
    }

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

        if (oczekujace.isNotEmpty()) {
            SzkloKarta {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.NotificationsActive, null, tint = Ostrzezenie)
                    Spacer(modifier = Modifier.size(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Nowa prośba o dostęp", color = TekstGlowny, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(
                            if (oczekujace.size == 1) "Zaakceptuj lub odrzuć urządzenie rodziny"
                            else "${oczekujace.size} urządzenia czekają na decyzję",
                            color = Ostrzezenie,
                            fontSize = 13.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                oczekujace.forEach { p ->
                    KartaDecyzji(
                        tytul = p.nazwaUrzadzenia,
                        podtytul = "Parowanie — oczekuje",
                        ladowanie = akcjaProsby == "pair:${p.id}",
                        onAkceptuj = {
                            zakres.launch {
                                akcjaProsby = "pair:${p.id}"
                                runCatching { SerwisParowania.zatwierdzProsbe(kontekst, p.id) }
                                    .onSuccess { status = "Połączono: ${p.nazwaUrzadzenia}" }
                                    .onFailure { blad = it.message }
                                akcjaProsby = null
                            }
                        },
                        onOdrzuc = {
                            zakres.launch {
                                akcjaProsby = "pair:${p.id}"
                                runCatching { SerwisParowania.odrzucProsbe(kontekst, p.id) }
                                    .onFailure { blad = it.message }
                                akcjaProsby = null
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (logowaniaOczekujace.isNotEmpty()) {
            SzkloKarta {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.NotificationsActive, null, tint = AkcentMorski)
                    Spacer(modifier = Modifier.size(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Prośba o logowanie", color = TekstGlowny, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(
                            "Urządzenie chce wejść do sklepu",
                            color = TekstDrugi,
                            fontSize = 13.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                logowaniaOczekujace.forEach { p ->
                    KartaDecyzji(
                        tytul = p.nazwaUrzadzenia,
                        podtytul = "Logowanie — oczekuje",
                        ladowanie = akcjaProsby == "login:${p.id}",
                        onAkceptuj = {
                            zakres.launch {
                                akcjaProsby = "login:${p.id}"
                                runCatching { SerwisParowania.zatwierdzLogowanie(kontekst, p.id) }
                                    .onSuccess { status = "Zezwolono na logowanie: ${p.nazwaUrzadzenia}" }
                                    .onFailure { blad = it.message }
                                akcjaProsby = null
                            }
                        },
                        onOdrzuc = {
                            zakres.launch {
                                akcjaProsby = "login:${p.id}"
                                runCatching { SerwisParowania.odrzucLogowanie(kontekst, p.id) }
                                    .onFailure { blad = it.message }
                                akcjaProsby = null
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
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

        if (odbiorcy.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            SzkloKarta {
                Text("Sparowane urządzenia", color = TekstGlowny, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(10.dp))
                odbiorcy.forEach { o ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(o.nazwa, color = TekstGlowny, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                Text(
                                    if (o.zablokowany) "Zablokowane" else "Połączone",
                                    color = if (o.zablokowany) TekstDrugi else Sukces,
                                    fontSize = 12.sp
                                )
                            }
                            TextButton(onClick = {
                                nazwaEdycji = o.nazwa
                                edytowanyOdbiorca = o
                            }) {
                                Icon(Icons.Rounded.Edit, null, tint = AkcentNeon, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.size(4.dp))
                                Text("Nazwa", color = AkcentNeon)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Auto-logowanie",
                                    color = TekstGlowny,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    if (o.autoLogowanie) {
                                        "Może wchodzić do sklepu bez Twojej zgody"
                                    } else {
                                        "Każde logowanie wymaga Twojej akceptacji"
                                    },
                                    color = TekstDrugi,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                            Switch(
                                checked = o.autoLogowanie,
                                onCheckedChange = { wlacz ->
                                    zakres.launch {
                                        runCatching {
                                            SerwisParowania.ustawAutoLogowanie(kontekst, o.id, wlacz)
                                        }.onFailure { blad = it.message }
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = TekstNaAkcencie,
                                    checkedTrackColor = AkcentNeon,
                                    uncheckedThumbColor = TekstDrugi,
                                    uncheckedTrackColor = Color.White.copy(alpha = 0.15f)
                                )
                            )
                        }
                        TextButton(onClick = { doOdlaczenia = o }) {
                            Text("Odłącz", color = Blad)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun KartaDecyzji(
    tytul: String,
    podtytul: String,
    ladowanie: Boolean,
    onAkceptuj: () -> Unit,
    onOdrzuc: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(14.dp)
    ) {
        Text(tytul, color = TekstGlowny, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Text(podtytul, color = TekstDrugi, fontSize = 12.sp)
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
                    .clickable(enabled = !ladowanie, onClick = onAkceptuj)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (ladowanie) {
                    CircularProgressIndicator(color = Sukces, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Akceptuj", color = Sukces, fontWeight = FontWeight.Bold)
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Blad.copy(alpha = 0.15f))
                    .clickable(enabled = !ladowanie, onClick = onOdrzuc)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Odrzuć", color = Blad, fontWeight = FontWeight.Bold)
            }
        }
    }
}
