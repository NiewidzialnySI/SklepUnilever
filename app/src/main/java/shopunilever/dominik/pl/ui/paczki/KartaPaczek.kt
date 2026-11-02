package shopunilever.dominik.pl.ui.paczki

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import shopunilever.dominik.pl.firebase.PrzesylkaPaczki
import shopunilever.dominik.pl.firebase.SerwisPrzesylek
import shopunilever.dominik.pl.ui.components.SzkloKarta
import shopunilever.dominik.pl.ui.theme.AkcentMorski
import shopunilever.dominik.pl.ui.theme.AkcentNeon
import shopunilever.dominik.pl.ui.theme.Blad
import shopunilever.dominik.pl.ui.theme.Ostrzezenie
import shopunilever.dominik.pl.ui.theme.RamkaKarty
import shopunilever.dominik.pl.ui.theme.TekstDrugi
import shopunilever.dominik.pl.ui.theme.TekstGlowny

/**
 * Kafelek PACZKI — aktywne + archiwum (pracownik i znajomi).
 * Usunięcie = przeniesienie do archiwum.
 */
@Composable
fun KartaPaczek(
    aktywneParowanie: Boolean,
    podpowiedzKod: String = "",
    modifier: Modifier = Modifier
) {
    val kontekst = LocalContext.current
    val zakres = rememberCoroutineScope()
    var paczki by remember { mutableStateOf<List<PrzesylkaPaczki>>(emptyList()) }
    var archiwum by remember { mutableStateOf<List<PrzesylkaPaczki>>(emptyList()) }
    var pokazArchiwum by remember { mutableStateOf(false) }

    LaunchedEffect(aktywneParowanie) {
        if (!aktywneParowanie) {
            paczki = emptyList()
            archiwum = emptyList()
            return@LaunchedEffect
        }
        launch {
            SerwisPrzesylek.obserwujPrzesylki(kontekst).collectLatest { paczki = it }
        }
        launch {
            SerwisPrzesylek.obserwujArchiwum(kontekst).collectLatest { archiwum = it }
        }
    }

    if (pokazArchiwum) {
        DialogArchiwumPaczek(
            lista = archiwum,
            onZamknij = { pokazArchiwum = false },
            onPrzywroc = { id ->
                zakres.launch { runCatching { SerwisPrzesylek.przywroc(kontekst, id) } }
            },
            onUsunNaZawsze = { id ->
                zakres.launch { runCatching { SerwisPrzesylek.usunZArchiwum(kontekst, id) } }
            },
            onOtworz = { p -> otworzSledzenie(kontekst, p) }
        )
    }

    SzkloKarta(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 40.dp),
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
                        text = when {
                            !aktywneParowanie -> "Brak połączenia"
                            paczki.isEmpty() -> "Brak aktywnych paczek"
                            else -> "${paczki.size} · ${paczki.count { !it.odczytano }} nowych"
                        },
                        color = TekstGlowny,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
            IconButton(
                onClick = { pokazArchiwum = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(40.dp)
            ) {
                Icon(
                    Icons.Rounded.Unarchive,
                    contentDescription = "Archiwum",
                    tint = if (archiwum.isNotEmpty()) AkcentNeon else TekstDrugi,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        when {
            !aktywneParowanie -> {
                Text(
                    text = "Połącz urządzenia, aby widzieć przesyłki UPS/DHL.",
                    color = TekstDrugi,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
            paczki.isEmpty() -> {
                Text(
                    text = if (podpowiedzKod.isNotBlank()) {
                        "Brak paczek. Kurier: adres Gmail z +$podpowiedzKod."
                    } else {
                        "Brak aktywnych paczek"
                    },
                    color = TekstDrugi,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
            else -> {
                paczki.take(12).forEachIndexed { indeks, p ->
                    if (indeks > 0) {
                        HorizontalDivider(
                            color = RamkaKarty,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                    WierszPaczki(
                        przesylka = p,
                        archiwum = false,
                        onOtworz = {
                            otworzSledzenie(kontekst, p)
                            if (!p.odczytano) {
                                zakres.launch {
                                    runCatching { SerwisPrzesylek.oznaczOdczytana(kontekst, p.id) }
                                }
                            }
                        },
                        onAkcja = {
                            zakres.launch {
                                runCatching { SerwisPrzesylek.archiwizuj(kontekst, p.id) }
                            }
                        }
                    )
                }
                if (paczki.size > 12) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "+${paczki.size - 12} starszych",
                        color = TekstDrugi,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogArchiwumPaczek(
    lista: List<PrzesylkaPaczki>,
    onZamknij: () -> Unit,
    onPrzywroc: (String) -> Unit,
    onUsunNaZawsze: (String) -> Unit,
    onOtworz: (PrzesylkaPaczki) -> Unit
) {
    Dialog(onDismissRequest = onZamknij) {
        SzkloKarta {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Archiwum",
                    color = TekstGlowny,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onZamknij) {
                    Icon(Icons.Rounded.Close, "Zamknij", tint = TekstDrugi)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (lista.isEmpty()) {
                Text("Pusto", color = TekstDrugi, fontSize = 14.sp)
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    items(lista, key = { it.id }) { p ->
                        WierszPaczki(
                            przesylka = p,
                            archiwum = true,
                            onOtworz = { onOtworz(p) },
                            onAkcja = { onPrzywroc(p.id) },
                            onUsunNaZawsze = { onUsunNaZawsze(p.id) }
                        )
                        HorizontalDivider(color = RamkaKarty, modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
            TextButton(onClick = onZamknij) {
                Text("Zamknij", color = AkcentNeon)
            }
        }
    }
}

@Composable
private fun WierszPaczki(
    przesylka: PrzesylkaPaczki,
    archiwum: Boolean,
    onOtworz: () -> Unit,
    onAkcja: () -> Unit,
    onUsunNaZawsze: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onOtworz)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (przesylka.odczytano) AkcentMorski.copy(alpha = 0.12f)
                    else Ostrzezenie.copy(alpha = 0.18f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.LocalShipping,
                null,
                tint = if (przesylka.odczytano) AkcentMorski else Ostrzezenie,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.size(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${przesylka.kurier.uppercase()} · ${przesylka.numer}",
                color = TekstGlowny,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val podtytul = listOfNotNull(
                przesylka.statusTekst?.takeIf { it.isNotBlank() },
                przesylka.szacowanaDostawa?.takeIf { it.isNotBlank() }?.let { "Dostawa: $it" },
                przesylka.nadawcaPaczki?.takeIf { it.isNotBlank() }
            ).firstOrNull() ?: przesylka.temat.ifBlank { "Śledzenie" }
            Text(
                text = podtytul,
                color = TekstDrugi,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )
        }
        Icon(
            Icons.Rounded.OpenInNew,
            contentDescription = "Śledzenie",
            tint = TekstDrugi,
            modifier = Modifier.size(16.dp)
        )
        IconButton(onClick = onAkcja, modifier = Modifier.size(36.dp)) {
            Icon(
                if (archiwum) Icons.Rounded.Restore else Icons.Rounded.DeleteOutline,
                contentDescription = if (archiwum) "Przywróć" else "Do archiwum",
                tint = if (archiwum) AkcentMorski else Blad,
                modifier = Modifier.size(20.dp)
            )
        }
        if (archiwum && onUsunNaZawsze != null) {
            IconButton(onClick = onUsunNaZawsze, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = "Usuń na zawsze",
                    tint = Blad,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun otworzSledzenie(kontekst: android.content.Context, p: PrzesylkaPaczki) {
    val link = p.linkDoOtwarcia().takeIf { it.isNotBlank() } ?: return
    runCatching {
        kontekst.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(link)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
