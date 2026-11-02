package shopunilever.dominik.pl.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import shopunilever.dominik.pl.onboarding.PreferencjeOnboardingu
import shopunilever.dominik.pl.onboarding.RolaUzytkownika
import shopunilever.dominik.pl.ui.aktualizacja.OknoAktualizacji
import shopunilever.dominik.pl.ui.ekrany.EkranGlownyPlaceholder
import shopunilever.dominik.pl.ui.ekrany.EkranPowitalny
import shopunilever.dominik.pl.ui.ekrany.EkranUprawnien
import shopunilever.dominik.pl.ui.ekrany.EkranWyboruRoli
import shopunilever.dominik.pl.ui.pracownik.EkranPracownika

private enum class KrokAplikacji {
    POWITALNY,
    WYBOR_ROLI,
    UPRAWNIENIA,
    GLOWNY
}

@Composable
fun NawigacjaAplikacji() {
    val kontekst = LocalContext.current

    var krok by remember {
        mutableStateOf(
            when {
                !PreferencjeOnboardingu.czyWitamyZakonczone(kontekst) -> KrokAplikacji.POWITALNY
                PreferencjeOnboardingu.rola(kontekst) == null -> KrokAplikacji.WYBOR_ROLI
                !PreferencjeOnboardingu.czyUprawnieniaZakonczone(kontekst) -> KrokAplikacji.UPRAWNIENIA
                else -> KrokAplikacji.GLOWNY
            }
        )
    }
    var rola by remember {
        mutableStateOf(PreferencjeOnboardingu.rola(kontekst))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = krok,
            transitionSpec = {
                (slideInHorizontally(animationSpec = tween(400)) { width -> width } + fadeIn(tween(400)))
                    .togetherWith(
                        slideOutHorizontally(animationSpec = tween(400)) { width -> -width / 3 } + fadeOut(tween(400))
                    )
            },
            label = "nawigacja_ekranow"
        ) { cel ->
            when (cel) {
                KrokAplikacji.POWITALNY -> EkranPowitalny(
                    onDalej = {
                        PreferencjeOnboardingu.ustawWitamyZakonczone(kontekst)
                        krok = KrokAplikacji.WYBOR_ROLI
                    }
                )

                KrokAplikacji.WYBOR_ROLI -> EkranWyboruRoli(
                    onWybrano = { wybrana: RolaUzytkownika ->
                        PreferencjeOnboardingu.ustawRole(kontekst, wybrana)
                        rola = wybrana
                        krok = KrokAplikacji.UPRAWNIENIA
                    }
                )

                KrokAplikacji.UPRAWNIENIA -> {
                    val aktualna = rola ?: RolaUzytkownika.RODZINA
                    EkranUprawnien(
                        rola = aktualna,
                        onGotowe = {
                            PreferencjeOnboardingu.ustawUprawnieniaZakonczone(kontekst)
                            krok = KrokAplikacji.GLOWNY
                        }
                    )
                }

                KrokAplikacji.GLOWNY -> when (rola) {
                    RolaUzytkownika.PRACOWNIK -> EkranPracownika()
                    else -> EkranGlownyPlaceholder(rola = rola)
                }
            }
        }

        OknoAktualizacji()
    }
}
