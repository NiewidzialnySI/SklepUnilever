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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import shopunilever.dominik.pl.ui.components.TloAplikacji

@Composable
fun EkranPracownika(modifier: Modifier = Modifier) {
    var zakladka by remember { mutableStateOf(ZakladkaPracownika.DOM) }
    var pokazSklep by remember { mutableStateOf(false) }

    AnimatedContent(
        targetState = pokazSklep,
        transitionSpec = {
            if (targetState) {
                // Wejście do sklepu
                (
                    fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 8 } +
                        scaleIn(initialScale = 0.97f, animationSpec = tween(260))
                    ) togetherWith (
                    fadeOut(tween(160)) + scaleOut(targetScale = 0.98f, animationSpec = tween(160))
                    )
            } else {
                // Wyjście ze sklepu — w dół + fade
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
                                ZakladkaPracownika.URZADZENIA -> EkranUrzadzen()
                                ZakladkaPracownika.USTAWIENIA -> EkranUstawien()
                            }
                        }
                    }
                    DolnyPasekPracownika(
                        aktywna = zakladka,
                        onZmiana = { zakladka = it }
                    )
                }
            }
        }
    }
}
