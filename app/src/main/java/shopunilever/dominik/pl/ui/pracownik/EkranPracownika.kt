package shopunilever.dominik.pl.ui.pracownik

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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

    if (pokazSklep) {
        EkranWebViewSklepu(
            onZamknij = { pokazSklep = false },
            modifier = modifier
        )
        return
    }

    TloAplikacji(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = zakladka,
                    transitionSpec = {
                        val wPrawo = targetState.ordinal > initialState.ordinal
                        val enterOffset = { szer: Int -> if (wPrawo) szer / 4 else -szer / 4 }
                        val exitOffset = { szer: Int -> if (wPrawo) -szer / 5 else szer / 5 }
                        (
                            slideInHorizontally(
                                animationSpec = tween(280),
                                initialOffsetX = enterOffset
                            ) + fadeIn(tween(220))
                        ) togetherWith (
                            slideOutHorizontally(
                                animationSpec = tween(240),
                                targetOffsetX = exitOffset
                            ) + fadeOut(tween(180))
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
