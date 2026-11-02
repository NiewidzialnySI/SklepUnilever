package shopunilever.dominik.pl.ui.pracownik

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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

/**
 * Dolny pasek sklepu + wysuwany panel „Zapisane” znad paska.
 * Umieść w [Box] na całym ekranie z [Modifier] = fillMaxSize / align bottom area.
 */
@Composable
fun DolnyPasekSklepu(
    onZapiszKoszyk: (nazwa: String) -> Unit,
    onOtworzKoszykSklepu: () -> Unit,
    onDodajPozycje: (List<PozycjaKoszyka>) -> Unit,
    modifier: Modifier = Modifier
) {
    val kontekst = LocalContext.current
    var pokazListe by remember { mutableStateOf(false) }
    var pokazNazwe by remember { mutableStateOf(false) }
    var koszyki by remember { mutableStateOf(PreferencjeKoszykow.lista(kontekst)) }
    var wybrany by remember { mutableStateOf<ZapisanyKoszyk?>(null) }

    if (pokazNazwe) {
        DialogNazwyKoszyka(
            domyslna = "Koszyk ${PreferencjeKoszykow.lista(kontekst).size + 1}",
            onAnuluj = { pokazNazwe = false },
            onZapisz = { nazwa ->
                pokazNazwe = false
                onZapiszKoszyk(nazwa)
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = pokazListe,
            enter = fadeIn(tween(160)),
            exit = fadeOut(tween(140)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable {
                        pokazListe = false
                        wybrany = null
                    }
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            AnimatedVisibility(
                visible = pokazListe,
                enter = slideInVertically(tween(280)) { it } + fadeIn(tween(200)),
                exit = slideOutVertically(tween(220)) { it } + fadeOut(tween(160))
            ) {
                PanelZapisanychKoszykow(
                    koszyki = koszyki,
                    wybrany = wybrany,
                    onZamknij = { pokazListe = false; wybrany = null },
                    onUsun = { id ->
                        PreferencjeKoszykow.usun(kontekst, id)
                        koszyki = PreferencjeKoszykow.lista(kontekst)
                        if (wybrany?.id == id) wybrany = null
                    },
                    onWybierz = { wybrany = it },
                    onDodajZaznaczone = { pozycje ->
                        pokazListe = false
                        wybrany = null
                        onDodajPozycje(pozycje)
                    }
                )
            }

            Row(
                modifier = Modifier
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
                    onClick = {
                        pokazListe = false
                        pokazNazwe = true
                    }
                )
                KafelekPaska(
                    modifier = Modifier.weight(1f),
                    ikona = Icons.Rounded.FolderOpen,
                    etykieta = "Zapisane",
                    onClick = {
                        koszyki = PreferencjeKoszykow.lista(kontekst)
                        if (!pokazListe) wybrany = null
                        pokazListe = !pokazListe
                    }
                )
                KafelekPaska(
                    modifier = Modifier.weight(1f),
                    ikona = Icons.Rounded.ShoppingCart,
                    etykieta = "Koszyk",
                    onClick = {
                        pokazListe = false
                        onOtworzKoszykSklepu()
                    }
                )
            }
        }
    }
}

@Composable
private fun KafelekPaska(
    ikona: ImageVector,
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

@Composable
private fun DialogNazwyKoszyka(
    domyslna: String,
    onAnuluj: () -> Unit,
    onZapisz: (String) -> Unit
) {
    var nazwa by remember { mutableStateOf(domyslna) }
    Dialog(onDismissRequest = onAnuluj) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(TloWarstwa)
                .padding(20.dp)
        ) {
            Text("Nazwa koszyka", color = TekstGlowny, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Nadaj nazwę, żeby łatwo znaleźć ten zestaw później.",
                color = TekstDrugi,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(
                value = nazwa,
                onValueChange = { nazwa = it.take(40) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Nazwa") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { if (nazwa.isNotBlank()) onZapisz(nazwa.trim()) }
                ),
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
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onAnuluj) {
                    Text("Anuluj", color = TekstDrugi)
                }
                TextButton(onClick = { if (nazwa.isNotBlank()) onZapisz(nazwa.trim()) }) {
                    Text("Zapisz", color = AkcentNeon, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PanelZapisanychKoszykow(
    koszyki: List<ZapisanyKoszyk>,
    wybrany: ZapisanyKoszyk?,
    onZamknij: () -> Unit,
    onUsun: (String) -> Unit,
    onWybierz: (ZapisanyKoszyk) -> Unit,
    onDodajZaznaczone: (List<PozycjaKoszyka>) -> Unit
) {
    var zaznaczone by remember(wybrany?.id) {
        mutableStateOf(wybrany?.pozycje?.map { it.url.ifBlank { it.nazwa } }?.toSet() ?: emptySet())
    }
    val fmt = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    val ksztalt = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ksztalt)
            .background(TloWarstwa)
            .border(
                1.dp,
                Brush.linearGradient(listOf(Color.White.copy(0.18f), RamkaKarty)),
                ksztalt
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 40.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(TekstDrugi.copy(alpha = 0.35f))
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Zapisane koszyki",
                color = TekstGlowny,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onZamknij) {
                Icon(Icons.Rounded.Close, contentDescription = "Zamknij", tint = TekstDrugi)
            }
        }
        Text(
            "Zaznacz produkty. Brakujące w sklepie zostaną pominięte.",
            color = TekstDrugi,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (koszyki.isEmpty()) {
            Text("Brak zapisanych koszyków.", color = TekstDrugi, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
        } else {
            LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
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
                                    .clickable { onUsun(k.id) }
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
                                            zaznaczone =
                                                if (checked) zaznaczone + klucz else zaznaczone - klucz
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
                                Icon(
                                    Icons.Rounded.AddShoppingCart,
                                    null,
                                    tint = AkcentNeon,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                                Text("Dodaj zaznaczone", color = AkcentNeon, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
