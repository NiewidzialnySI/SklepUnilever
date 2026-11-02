package shopunilever.dominik.pl.ui.pracownik

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import shopunilever.dominik.pl.dane.PreferencjeParowania
import shopunilever.dominik.pl.firebase.PowiadomieniaParowania
import shopunilever.dominik.pl.firebase.ProsbaDostepu
import shopunilever.dominik.pl.firebase.ProsbaLogowania
import shopunilever.dominik.pl.firebase.SerwisParowania
import shopunilever.dominik.pl.tlo.SerwisTlaNadajnika
import shopunilever.dominik.pl.ui.IntentyNawigacji
import shopunilever.dominik.pl.ui.components.TloAplikacji

@Composable
fun EkranPracownika(modifier: Modifier = Modifier) {
    val kontekst = LocalContext.current
    var zakladka by remember { mutableStateOf(ZakladkaPracownika.DOM) }
    var pokazSklep by remember { mutableStateOf(false) }
    var prosbyOczekujace by remember { mutableIntStateOf(0) }
    var restartNasluchu by remember { mutableIntStateOf(0) }
    val juzPowiadomione = remember { mutableSetOf<String>() }

    val chceUrzadzenia by IntentyNawigacji.otworzUrzadzenia.collectAsState()
    LaunchedEffect(chceUrzadzenia) {
        if (chceUrzadzenia) {
            pokazSklep = false
            zakladka = ZakladkaPracownika.URZADZENIA
            IntentyNawigacji.skonsumujOtworzUrzadzenia()
        }
    }

    // Tło SMS + badge w aplikacji (bez spamowania powiadomieniami / skoków zakładek).
    LaunchedEffect(restartNasluchu) {
        if (PreferencjeParowania.czyAktywne(kontekst)) {
            runCatching { SerwisTlaNadajnika.uruchomJesliTrzeba(kontekst) }
        } else {
            runCatching { SerwisTlaNadajnika.zatrzymaj(kontekst) }
            prosbyOczekujace = 0
            return@LaunchedEffect
        }
        val kod = PreferencjeParowania.kod(kontekst)
        if (kod.isBlank()) {
            prosbyOczekujace = 0
            return@LaunchedEffect
        }
        runCatching { SerwisParowania.upewnijSesjeNadajnika(kontekst) }
        var pendingParowanie = 0
        var pendingLogin = 0
        fun odswiezBadge() {
            prosbyOczekujace = pendingParowanie + pendingLogin
        }
        launch {
            SerwisParowania.obserwujProsby(kontekst).collectLatest { lista: List<ProsbaDostepu> ->
                val pending = lista.filter { it.status == "pending" }
                pendingParowanie = pending.size
                odswiezBadge()
                // Tylko pierwsze pojawienie się — ciche przypomnienie, bez wymuszania ekranu.
                pending.forEach { p ->
                    if (juzPowiadomione.add("pair:${p.id}")) {
                        PowiadomieniaParowania.pokazCichePrzypomnienie(
                            kontekst,
                            p.nazwaUrzadzenia,
                            p.id
                        )
                    }
                }
                juzPowiadomione.removeAll { klucz ->
                    klucz.startsWith("pair:") && pending.none { "pair:${it.id}" == klucz }
                }
            }
        }
        launch {
            SerwisParowania.obserwujProsbyLogowania(kontekst).collectLatest { lista: List<ProsbaLogowania> ->
                val pending = lista.filter { it.status == "pending" }
                pendingLogin = pending.size
                odswiezBadge()
                pending.forEach { p ->
                    if (juzPowiadomione.add("login:${p.id}")) {
                        PowiadomieniaParowania.pokazCichePrzypomnienie(
                            kontekst,
                            p.nazwaUrzadzenia,
                            "login-${p.id}"
                        )
                    }
                }
                juzPowiadomione.removeAll { klucz ->
                    klucz.startsWith("login:") && pending.none { "login:${it.id}" == klucz }
                }
            }
        }
    }

    AnimatedContent(
        targetState = pokazSklep,
        transitionSpec = {
            if (targetState) {
                (
                    fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 8 } +
                        scaleIn(initialScale = 0.97f, animationSpec = tween(260))
                    ) togetherWith (
                    fadeOut(tween(160)) + scaleOut(targetScale = 0.98f, animationSpec = tween(160))
                    )
            } else {
                (
                    fadeIn(tween(200)) + scaleIn(initialScale = 0.98f, animationSpec = tween(220))
                    ) togetherWith (
                    fadeOut(tween(200)) + slideOutVertically(tween(280)) { it / 5 } +
                        scaleOut(targetScale = 0.96f, animationSpec = tween(260))
                    )
            }
        },
        label = "animacja_sklepu",
        modifier = modifier.fillMaxSize()
    ) { sklep ->
        if (sklep) {
            EkranWebViewSklepu(
                onZamknij = { pokazSklep = false },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            TloAplikacji(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
                        AnimatedContent(
                            targetState = zakladka,
                            transitionSpec = {
                                val wPrawo = targetState.ordinal > initialState.ordinal
                                val enterOffset = { szer: Int -> if (wPrawo) szer / 6 else -szer / 6 }
                                val exitOffset = { szer: Int -> if (wPrawo) -szer / 8 else szer / 8 }
                                (
                                    slideInHorizontally(tween(150), enterOffset) + fadeIn(tween(120))
                                    ) togetherWith (
                                    slideOutHorizontally(tween(130), exitOffset) + fadeOut(tween(100))
                                    )
                            },
                            label = "przelaczenie_zakladek",
                            modifier = Modifier.fillMaxSize()
                        ) { cel ->
                            when (cel) {
                                ZakladkaPracownika.DOM -> EkranHomePracownika(
                                    onOtworzSklep = { pokazSklep = true }
                                )
                                ZakladkaPracownika.URZADZENIA -> EkranUrzadzen(
                                    onParowanieZmienione = { restartNasluchu++ }
                                )
                                ZakladkaPracownika.USTAWIENIA -> EkranUstawien()
                            }
                        }
                    }
                    DolnyPasekPracownika(
                        aktywna = zakladka,
                        onZmiana = { zakladka = it },
                        badgeUrzadzenia = prosbyOczekujace
                    )
                }
            }
        }
    }
}
