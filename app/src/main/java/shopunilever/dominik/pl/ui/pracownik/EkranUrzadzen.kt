package shopunilever.dominik.pl.ui.pracownik

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import shopunilever.dominik.pl.tlo.SerwisTlaNadajnika
import shopunilever.dominik.pl.ui.components.SzkloKarta
import shopunilever.dominik.pl.ui.theme.AkcentMorski
import shopunilever.dominik.pl.ui.theme.AkcentNeon
import shopunilever.dominik.pl.ui.theme.Blad
import shopunilever.dominik.pl.ui.theme.Ostrzezenie
import shopunilever.dominik.pl.ui.theme.RamkaKarty
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
    var prosby by remember { mutableStateOf<List<ProsbaDostepu>>(emptyList()) }
    var prosbyLogowania by remember { mutableStateOf<List<ProsbaLogowania>>(emptyList()) }
    var odbiorcy by remember { mutableStateOf<List<OdbiorcaPolaczony>>(emptyList()) }
    var akcjaProsby by remember { mutableStateOf<String?>(null) }

    var edytowanyOdbiorca by remember { mutableStateOf<OdbiorcaPolaczony?>(null) }
    var nazwaEdycji by remember { mutableStateOf("") }
    var doOdlaczenia by remember { mutableStateOf<OdbiorcaPolaczony?>(null) }
    var potwierdzNowyKod by remember { mutableStateOf(false) }

    LaunchedEffect(aktywne, kod) {
        if (kod.isBlank()) return@LaunchedEffect
        runCatching { SerwisParowania.upewnijSesjeNadajnika(kontekst) }
        launch { SerwisParowania.obserwujProsby(kontekst).collectLatest { prosby = it } }
        launch { SerwisParowania.obserwujProsbyLogowania(kontekst).collectLatest { prosbyLogowania = it } }
        launch { SerwisParowania.obserwujOdbiorcow(kontekst).collectLatest { odbiorcy = it } }
    }

    fun uruchomParowanie(nowyKod: Boolean, odlaczWszystkich: Boolean = false) {
        zakres.launch {
            ladowanie = true
            runCatching {
                if (odlaczWszystkich) {
                    runCatching { SerwisParowania.usunWszystkichOdbiorcow(kontekst) }
                }
                if (nowyKod) PreferencjeParowania.wyczysc(kontekst)
                SerwisParowania.uruchomJakoNadajnik(kontekst)
            }.onSuccess {
                kod = it
                aktywne = true
                runCatching { SerwisTlaNadajnika.uruchomJesliTrzeba(kontekst) }
                onParowanieZmienione()
                if (odlaczWszystkich) {
                    Toast.makeText(kontekst, "Nowy kod — urządzenia odłączone", Toast.LENGTH_SHORT).show()
                }
            }.onFailure {
                Toast.makeText(kontekst, it.message ?: "Błąd parowania", Toast.LENGTH_SHORT).show()
            }
            ladowanie = false
        }
    }

    val oczekujace = prosby.filter { it.status == "pending" }
    val logowaniaOczekujace = prosbyLogowania.filter { it.status == "pending" }

    if (potwierdzNowyKod) {
        AlertDialog(
            onDismissRequest = { potwierdzNowyKod = false },
            icon = {
                Icon(Icons.Rounded.Refresh, null, tint = Ostrzezenie, modifier = Modifier.size(28.dp))
            },
            title = { Text("Nowy kod?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (odbiorcy.isEmpty()) {
                        "Wygenerujesz nowy kod QR."
                    } else {
                        "Wszystkie sparowane urządzenia (${odbiorcy.size}) stracą dostęp i muszą zostać dodane ponownie — zeskanują nowy kod."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    potwierdzNowyKod = false
                    uruchomParowanie(nowyKod = true, odlaczWszystkich = true)
                }) {
                    Text("Nowy kod", color = Ostrzezenie, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { potwierdzNowyKod = false }) {
                    Text("Anuluj", color = TekstDrugi)
                }
            }
        )
    }

    edytowanyOdbiorca?.let { o ->
        AlertDialog(
            onDismissRequest = { edytowanyOdbiorca = null },
            title = { Text("Nazwa", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = nazwaEdycji,
                    onValueChange = { nazwaEdycji = it.take(40) },
                    singleLine = true,
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
                        }
                        edytowanyOdbiorca = null
                    }
                }) { Text("OK", color = AkcentNeon, fontWeight = FontWeight.Bold) }
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
            icon = {
                Icon(Icons.Rounded.LinkOff, null, tint = Blad, modifier = Modifier.size(28.dp))
            },
            title = { Text("Odłączyć?", fontWeight = FontWeight.Bold) },
            text = { Text("„${o.nazwa}” straci dostęp.") },
            confirmButton = {
                TextButton(onClick = {
                    zakres.launch {
                        runCatching { SerwisParowania.usunOdbiorce(kontekst, o.id) }
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
                    Text("Parowanie", color = TekstGlowny, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                oczekujace.forEach { p ->
                    KartaDecyzji(
                        tytul = p.nazwaUrzadzenia,
                        ladowanie = akcjaProsby == "pair:${p.id}",
                        onAkceptuj = {
                            zakres.launch {
                                akcjaProsby = "pair:${p.id}"
                                runCatching { SerwisParowania.zatwierdzProsbe(kontekst, p.id) }
                                akcjaProsby = null
                            }
                        },
                        onOdrzuc = {
                            zakres.launch {
                                akcjaProsby = "pair:${p.id}"
                                runCatching { SerwisParowania.odrzucProsbe(kontekst, p.id) }
                                akcjaProsby = null
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (logowaniaOczekujace.isNotEmpty()) {
            SzkloKarta {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.LockOpen, null, tint = AkcentMorski)
                    Spacer(modifier = Modifier.size(10.dp))
                    Text("Logowanie", color = TekstGlowny, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                logowaniaOczekujace.forEach { p ->
                    KartaDecyzji(
                        tytul = p.nazwaUrzadzenia,
                        ladowanie = akcjaProsby == "login:${p.id}",
                        onAkceptuj = {
                            zakres.launch {
                                akcjaProsby = "login:${p.id}"
                                runCatching { SerwisParowania.zatwierdzLogowanie(kontekst, p.id) }
                                akcjaProsby = null
                            }
                        },
                        onOdrzuc = {
                            zakres.launch {
                                akcjaProsby = "login:${p.id}"
                                runCatching { SerwisParowania.odrzucLogowanie(kontekst, p.id) }
                                akcjaProsby = null
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        SzkloKarta {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Link, contentDescription = null, tint = AkcentNeon)
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = "Kod",
                    color = TekstGlowny,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (kod.isNotBlank()) {
                val trescQr = "${StaleFirebase.PREFIX_QR}$kod"
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    KodQrParowania(tresc = trescQr, rozmiar = 200.dp)
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
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                ) {
                    FilledTonalIconButton(onClick = {
                        val cm = kontekst.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("kod", kod))
                        Toast.makeText(kontekst, "Skopiowano", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Rounded.ContentCopy, "Kopiuj")
                    }
                    FilledTonalIconButton(
                        onClick = { potwierdzNowyKod = true },
                        enabled = !ladowanie,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            contentColor = Ostrzezenie
                        )
                    ) {
                        Icon(Icons.Rounded.Refresh, "Nowy kod")
                    }
                }
            } else if (ladowanie) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AkcentNeon, modifier = Modifier.size(28.dp))
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.horizontalGradient(listOf(AkcentNeon, AkcentMorski)))
                        .clickable { uruchomParowanie(nowyKod = false) }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Link, null, tint = TekstNaAkcencie, modifier = Modifier.size(22.dp))
                }
            }

            if (ladowanie && kod.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AkcentNeon, modifier = Modifier.size(22.dp))
                }
            }
        }

        if (odbiorcy.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            SzkloKarta {
                Text(
                    "Urządzenia",
                    color = TekstGlowny,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                odbiorcy.forEachIndexed { indeks, o ->
                    if (indeks > 0) {
                        HorizontalDivider(
                            color = RamkaKarty,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    WierszUrzadzenia(
                        odbiorca = o,
                        onEdytuj = {
                            nazwaEdycji = o.nazwa
                            edytowanyOdbiorca = o
                        },
                        onAutoLogowanie = { wlacz ->
                            zakres.launch {
                                runCatching {
                                    SerwisParowania.ustawAutoLogowanie(kontekst, o.id, wlacz)
                                }
                            }
                        },
                        onOdlacz = { doOdlaczenia = o }
                    )
                }
            }
        }
    }
}

@Composable
private fun WierszUrzadzenia(
    odbiorca: OdbiorcaPolaczony,
    onEdytuj: () -> Unit,
    onAutoLogowanie: (Boolean) -> Unit,
    onOdlacz: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = odbiorca.nazwa,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            Icon(Icons.Rounded.PhoneAndroid, contentDescription = null)
        },
        trailingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                FilledTonalIconButton(
                    onClick = onEdytuj,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Zmień nazwę")
                }
                FilledTonalIconToggleButton(
                    checked = odbiorca.autoLogowanie,
                    onCheckedChange = onAutoLogowanie,
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.filledTonalIconToggleButtonColors(
                        checkedContainerColor = Sukces.copy(alpha = 0.22f),
                        checkedContentColor = Sukces,
                        containerColor = Ostrzezenie.copy(alpha = 0.16f),
                        contentColor = Ostrzezenie
                    )
                ) {
                    Icon(
                        imageVector = if (odbiorca.autoLogowanie) {
                            Icons.Rounded.LockOpen
                        } else {
                            Icons.Rounded.Lock
                        },
                        contentDescription = if (odbiorca.autoLogowanie) {
                            "Auto-logowanie wł."
                        } else {
                            "Wymaga zgody"
                        }
                    )
                }
                FilledTonalIconButton(
                    onClick = onOdlacz,
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Blad.copy(alpha = 0.16f),
                        contentColor = Blad
                    )
                ) {
                    Icon(Icons.Rounded.LinkOff, contentDescription = "Odłącz")
                }
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            headlineColor = TekstGlowny,
            leadingIconColor = AkcentNeon
        )
    )
}

@Composable
private fun KartaDecyzji(
    tytul: String,
    ladowanie: Boolean,
    onAkceptuj: () -> Unit,
    onOdrzuc: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = tytul,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            Icon(Icons.Rounded.PhoneAndroid, contentDescription = null)
        },
        trailingContent = {
            if (ladowanie) {
                CircularProgressIndicator(
                    color = AkcentNeon,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    FilledTonalIconButton(
                        onClick = onAkceptuj,
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Sukces.copy(alpha = 0.18f),
                            contentColor = Sukces
                        )
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = "Akceptuj")
                    }
                    FilledTonalIconButton(
                        onClick = onOdrzuc,
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Blad.copy(alpha = 0.16f),
                            contentColor = Blad
                        )
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Odrzuć")
                    }
                }
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.06f),
            headlineColor = TekstGlowny,
            leadingIconColor = TekstDrugi
        ),
        modifier = Modifier.clip(RoundedCornerShape(14.dp))
    )
}
