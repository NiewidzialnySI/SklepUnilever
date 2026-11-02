package shopunilever.dominik.pl.ui.ekrany

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import shopunilever.dominik.pl.onboarding.RolaUzytkownika
import shopunilever.dominik.pl.ui.components.SzkloKarta
import shopunilever.dominik.pl.ui.components.TloAplikacji
import shopunilever.dominik.pl.ui.theme.AkcentNeon
import shopunilever.dominik.pl.ui.theme.TekstDrugi
import shopunilever.dominik.pl.ui.theme.TekstGlowny

@Composable
fun EkranGlownyPlaceholder(
    rola: RolaUzytkownika?,
    modifier: Modifier = Modifier
) {
    TloAplikacji(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SKLEP FIRMÓWY",
                color = AkcentNeon,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Witaj",
                color = TekstGlowny,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(18.dp))
            SzkloKarta {
                Text(
                    text = "Profil: ${rola?.etykieta ?: "—"}",
                    color = TekstGlowny,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tu w kolejnym kroku podepniemy WebView sklepu i auto-logowanie.",
                    color = TekstDrugi,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
