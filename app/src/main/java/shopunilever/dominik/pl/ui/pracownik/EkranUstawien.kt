package shopunilever.dominik.pl.ui.pracownik

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import shopunilever.dominik.pl.dane.PreferencjeKonta
import shopunilever.dominik.pl.ui.components.SzkloKarta
import shopunilever.dominik.pl.ui.theme.AkcentNeon
import shopunilever.dominik.pl.ui.theme.TekstDrugi
import shopunilever.dominik.pl.ui.theme.TekstGlowny

@Composable
fun EkranUstawien(modifier: Modifier = Modifier) {
    val kontekst = LocalContext.current
    var maDane by remember { mutableStateOf(PreferencjeKonta.czyDaneZapisane(kontekst)) }
    var pokazDialog by remember { mutableStateOf(false) }

    if (pokazDialog) {
        DialogDanychSluzbowych(
            emailPoczatkowy = PreferencjeKonta.email(kontekst),
            hasloPoczatkowe = PreferencjeKonta.haslo(kontekst),
            onAnuluj = { pokazDialog = false },
            onZapisz = { email, haslo ->
                PreferencjeKonta.zapiszDane(kontekst, email, haslo)
                maDane = true
                pokazDialog = false
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "USTAWIENIA",
            color = AkcentNeon,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Konto",
            color = TekstGlowny,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(22.dp))

        SzkloKarta {
            Text(
                text = "Dane służbowe",
                color = TekstGlowny,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (maDane) {
                    PreferencjeKonta.email(kontekst)
                } else {
                    "Brak zapisanych danych logowania do sklepiku."
                },
                color = TekstDrugi,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(onClick = { pokazDialog = true }) {
                Text(
                    text = if (maDane) "Zmień dane" else "Dodaj dane",
                    color = AkcentNeon,
                    fontWeight = FontWeight.Bold
                )
            }
            if (maDane) {
                TextButton(onClick = {
                    PreferencjeKonta.wyczysc(kontekst)
                    maDane = false
                }) {
                    Text("Wyczyść dane", color = TekstDrugi)
                }
            }
        }
    }
}
