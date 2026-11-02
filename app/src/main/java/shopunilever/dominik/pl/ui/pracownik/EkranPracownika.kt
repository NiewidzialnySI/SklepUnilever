package shopunilever.dominik.pl.ui.pracownik

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import shopunilever.dominik.pl.dane.PreferencjeParowania
import shopunilever.dominik.pl.firebase.PowiadomieniaParowania
import shopunilever.dominik.pl.firebase.RepozytoriumParowaniaUi
import shopunilever.dominik.pl.tlo.SerwisTlaNadajnika
import shopunilever.dominik.pl.ui.IntentyNawigacji
import shopunilever.dominik.pl.ui.components.TloAplikacji

@Composable
fun EkranPracownika(modifier: Modifier = Modifier) {
    val kontekst = LocalContext.current
    var zakladka by remember { mutableStateOf(ZakladkaPracownika.DOM) }
    var pokazSklep by remember { mutableStateOf(false) }
    var restartNasluchu by remember { mutableStateOf(0) }
    val juzPowiadomione = remember { mutableSetOf<String>() }
    // Po pierwszym wejściu zakładka zostaje w pamięci — bez zimnego startu (QR + Firebase).
    var odwiedzone by remember { mutableStateOf(setOf(ZakladkaPracownika.DOM)) }

    val onOtworzSklep = remember { { pokazSklep = true } }
    val onZamknijSklep = remember { { pokazSklep = false } }
    val onParowanieZmienione = remember {
        {
            restartNasluchu++
            Unit
        }
    }
    val onZmianaZakladki = remember {
        { nowa: ZakladkaPracownika ->
            zakladka = nowa
        }
    }

    val badge by RepozytoriumParowaniaUi.badgeOczekujace.collectAsState()
    val prosby by RepozytoriumParowaniaUi.prosby.collectAsState()
    val prosbyLogowania by RepozytoriumParowaniaUi.prosbyLogowania.collectAsState()
    val odbiorcy by RepozytoriumParowaniaUi.odbiorcy.collectAsState()

    val chceUrzadzenia by IntentyNawigacji.otworzUrzadzenia.collectAsState()
    LaunchedEffect(chceUrzadzenia) {
        if (chceUrzadzenia) {
            pokazSklep = false
            zakladka = ZakladkaPracownika.URZADZENIA
            IntentyNawigacji.skonsumujOtworzUrzadzenia()
        }
    }

    LaunchedEffect(zakladka) {
        odwiedzone = odwiedzone + zakladka
    }

    // Jeden zestaw listenerów RTDB
    LaunchedEffect(restartNasluchu) {
        if (PreferencjeParowania.czyAktywne(kontekst)) {
            RepozytoriumParowaniaUi.restart(kontekst)
        } else {
            runCatching { SerwisTlaNadajnika.zatrzymaj(kontekst) }
            RepozytoriumParowaniaUi.stop()
        }
    }

    // FGS SMS tylko gdy są sparowane urządzenia (relay dla rodziny)
    LaunchedEffect(restartNasluchu, odbiorcy.size) {
        if (PreferencjeParowania.czyAktywne(kontekst)) {
            runCatching {
                SerwisTlaNadajnika.odswiezWgOdbiorcow(kontekst, odbiorcy.size)
            }
        } else {
            runCatching { SerwisTlaNadajnika.zatrzymaj(kontekst) }
        }
    }

    // Ciche powiadomienia o nowych prośbach (bez drugiego listenera)
    LaunchedEffect(prosby, prosbyLogowania) {
        prosby.filter { it.status == "pending" }.forEach { p ->
            if (juzPowiadomione.add("pair:${p.id}")) {
                PowiadomieniaParowania.pokazCichePrzypomnienie(kontekst, p.nazwaUrzadzenia, p.id)
            }
        }
        prosbyLogowania.filter { it.status == "pending" }.forEach { p ->
            if (juzPowiadomione.add("login:${p.id}")) {
                PowiadomieniaParowania.pokazCichePrzypomnienie(
                    kontekst,
                    p.nazwaUrzadzenia,
                    "login-${p.id}"
                )
            }
        }
        juzPowiadomione.removeAll { klucz ->
            when {
                klucz.startsWith("pair:") ->
                    prosby.none { it.status == "pending" && "pair:${it.id}" == klucz }
                klucz.startsWith("login:") ->
                    prosbyLogowania.none { it.status == "pending" && "login:${it.id}" == klucz }
                else -> false
            }
        }
    }

    // Sklep jako warstwa nad zakładkami — NIE niszczy keep-alive (QR/Firebase)
    Box(modifier = modifier.fillMaxSize()) {
        TloAplikacji(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(0f)
                .graphicsLayer {
                    // Lekkie przyciemnienie gdy sklep otwarty; composition zostaje
                    alpha = if (pokazSklep) 0f else 1f
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) {
                    if (ZakladkaPracownika.DOM in odwiedzone) {
                        ZakladkaWarstwa(widoczna = zakladka == ZakladkaPracownika.DOM && !pokazSklep) {
                            EkranHomePracownika(onOtworzSklep = onOtworzSklep)
                        }
                    }
                    if (ZakladkaPracownika.URZADZENIA in odwiedzone) {
                        ZakladkaWarstwa(widoczna = zakladka == ZakladkaPracownika.URZADZENIA && !pokazSklep) {
                            EkranUrzadzen(onParowanieZmienione = onParowanieZmienione)
                        }
                    }
                    if (ZakladkaPracownika.USTAWIENIA in odwiedzone) {
                        ZakladkaWarstwa(widoczna = zakladka == ZakladkaPracownika.USTAWIENIA && !pokazSklep) {
                            EkranUstawien()
                        }
                    }
                }
                if (!pokazSklep) {
                    DolnyPasekPracownika(
                        aktywna = zakladka,
                        onZmiana = onZmianaZakladki,
                        badgeUrzadzenia = badge
                    )
                }
            }
        }

        if (pokazSklep) {
            EkranWebViewSklepu(
                onZamknij = onZamknijSklep,
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(2f)
            )
        }
    }
}

/** Zachowuje composition zakładki — widoczna warstwa dostaje touch (wyższy zIndex). */
@Composable
private fun ZakladkaWarstwa(
    widoczna: Boolean,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(if (widoczna) 1f else 0f)
            .graphicsLayer { alpha = if (widoczna) 1f else 0f }
    ) {
        content()
    }
}
