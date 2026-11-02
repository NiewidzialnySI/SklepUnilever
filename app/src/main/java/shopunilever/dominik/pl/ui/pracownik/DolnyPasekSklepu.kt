package shopunilever.dominik.pl.ui.pracownik

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import shopunilever.dominik.pl.dane.PozycjaKoszyka
import shopunilever.dominik.pl.dane.PreferencjeKoszykow
import shopunilever.dominik.pl.dane.ZapisanyKoszyk
import shopunilever.dominik.pl.ui.theme.AkcentNeon
import shopunilever.dominik.pl.ui.theme.RamkaKarty
import shopunilever.dominik.pl.ui.theme.TekstDrugi
import shopunilever.dominik.pl.ui.theme.TekstGlowny
import shopunilever.dominik.pl.ui.theme.TloKarty
import shopunilever.dominik.pl.ui.theme.TloWarstwa
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DolnyPasekSklepu(
    onZapiszKoszyk: () -> Unit,
    onOtworzKoszykSklepu: () -> Unit,
    onDodajPozycje: (List<PozycjaKoszyka>) -> Unit,
    modifier: Modifier = Modifier
) {
    val kontekst = LocalContext.current
    var pokazListe by remember { mutableStateOf(false) }
    var koszyki by remember { mutableStateOf(PreferencjeKoszykow.lista(kontekst)) }
    var wybrany by remember { mutableStateOf<ZapisanyKoszyk?>(null) }

    if (pokazListe) {
        DialogListaKoszykow(
            koszyki = koszyki,
            onZamknij = { pokazListe = false; wybrany = null },
            onOdswiez = { koszyki = PreferencjeKoszykow.lista(kontekst) },
            onUsun = { id ->
                PreferencjeKoszykow.usun(kontekst, id)
                koszyki = PreferencjeKoszykow.lista(kontekst)
                if (wybrany?.id == id) wybrany = null
            },
            onWybierz = { wybrany = it },
            wybrany = wybrany,
            onDodajZaznaczone = { pozycje ->
                pokazListe = false
                wybrany = null
                onDodajPozycje(pozycje)
            }
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(TloWarstwa)
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        KafelekPaska(
            modifier = Modifier.weight(1f),
            ikona = Icons.Rounded.BookmarkAdd,
            etykieta = "Zapisz",
            onClick = onZapiszKoszyk
        )
        KafelekPaska(
            modifier = Modifier.weight(1f),
            ikona = Icons.Rounded.FolderOpen,
            etykieta = "Zapisane",
            onClick = {
                koszyki = PreferencjeKoszykow.lista(kontekst)
                pokazListe = true
            }
        )
        KafelekPaska(
            modifier = Modifier.weight(1f),
            ikona = Icons.Rounded.ShoppingCart,
            etykieta = "Koszyk",
            onClick = onOtworzKoszykSklepu
        )
    }
}

@Composable
private fun KafelekPaska(
    ikona: ImageVectorLike,
    etykieta: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ksztalt = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .clip(ksztalt)
            .background(TloKarty)
            .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(0.18f), RamkaKarty)), ksztalt)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(ikona, contentDescription = etykieta, tint = AkcentNeon, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(etykieta, color = TekstGlowny, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

// typealias-ish for cleaner signature without importing ImageVector in private overload confusion
private typealias ImageVectorLike = androidx.compose.ui.graphics.vector.ImageVector

@Composable
private fun DialogListaKoszykow(
    koszyki: List<ZapisanyKoszyk>,
    wybrany: ZapisanyKoszyk?,
    onZamknij: () -> Unit,
    onOdswiez: () -> Unit,
    onUsun: (String) -> Unit,
    onWybierz: (ZapisanyKoszyk) -> Unit,
    onDodajZaznaczone: (List<PozycjaKoszyka>) -> Unit
) {
    var zaznaczone by remember(wybrany?.id) {
        mutableStateOf(wybrany?.pozycje?.map { it.url.ifBlank { it.nazwa } }?.toSet() ?: emptySet())
    }
    val fmt = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    Dialog(onDismissRequest = onZamknij) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(TloWarstwa)
                .padding(18.dp)
        ) {
            Text("Zapisane koszyki", color = TekstGlowny, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Wybierz koszyk, zaznacz produkty do dodania albo usuń zapis.",
                color = TekstDrugi,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (koszyki.isEmpty()) {
                Text("Brak zapisanych koszyków.", color = TekstDrugi, fontSize = 14.sp)
            } else {
                LazyColumn(modifier = Modifier.height(280.dp)) {
                    items(koszyki, key = { it.id }) { k ->
                        val aktywny = wybrany?.id == k.id
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (aktywny) AkcentNeon.copy(0.14f) else TloKarty)
                                .clickable {
                                    onWybierz(k)
                                    zaznaczone = k.pozycje.map { it.url.ifBlank { it.nazwa } }.toSet()
                                }
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(k.nazwa, color = TekstGlowny, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "${k.pozycje.size} poz. · ${fmt.format(Date(k.utworzono))}",
                                        color = TekstDrugi,
                                        fontSize = 11.sp
                                    )
                                }
                                Icon(
                                    Icons.Rounded.DeleteOutline,
                                    contentDescription = "Usuń",
                                    tint = TekstDrugi,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clickable { onUsun(k.id); onOdswiez() }
                                )
                            }
                            if (aktywny) {
                                Spacer(modifier = Modifier.height(8.dp))
                                k.pozycje.forEach { p ->
                                    val klucz = p.url.ifBlank { p.nazwa }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = zaznaczone.contains(klucz),
                                            onCheckedChange = { checked ->
                                                zaznaczone = if (checked) zaznaczone + klucz else zaznaczone - klucz
                                            },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = AkcentNeon,
                                                uncheckedColor = TekstDrugi
                                            )
                                        )
                                        Column {
                                            Text(p.nazwa, color = TekstGlowny, fontSize = 13.sp, maxLines = 2)
                                            if (p.cena.isNotBlank() || p.ilosc > 1) {
                                                Text(
                                                    buildString {
                                                        if (p.ilosc > 1) append("×${p.ilosc} ")
                                                        append(p.cena)
                                                    },
                                                    color = TekstDrugi,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }
                                TextButton(
                                    onClick = {
                                        val doDodania = k.pozycje.filter {
                                            zaznaczone.contains(it.url.ifBlank { it.nazwa })
                                        }
                                        if (doDodania.isNotEmpty()) onDodajZaznaczone(doDodania)
                                    }
                                ) {
                                    Icon(Icons.Rounded.AddShoppingCart, null, tint = AkcentNeon, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text("Dodaj zaznaczone", color = AkcentNeon, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onZamknij, modifier = Modifier.align(Alignment.End)) {
                Text("Zamknij", color = TekstDrugi)
            }
        }
    }
}
