package shopunilever.dominik.pl.ui.pracownik

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
                when (zakladka) {
                    ZakladkaPracownika.DOM -> EkranHomePracownika(
                        onOtworzSklep = { pokazSklep = true }
                    )
                    ZakladkaPracownika.URZADZENIA -> EkranUrzadzen()
                    ZakladkaPracownika.USTAWIENIA -> EkranUstawien()
                }
            }
            DolnyPasekPracownika(
                aktywna = zakladka,
                onZmiana = { zakladka = it }
            )
        }
    }
}
