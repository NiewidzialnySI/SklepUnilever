package shopunilever.dominik.pl.ui.pracownik

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import shopunilever.dominik.pl.ui.theme.AkcentNeon
import shopunilever.dominik.pl.ui.theme.RamkaKarty
import shopunilever.dominik.pl.ui.theme.TekstDrugi
import shopunilever.dominik.pl.ui.theme.TekstGlowny
import shopunilever.dominik.pl.ui.theme.TloKarty

@Composable
fun DolnyPasekPracownika(
    aktywna: ZakladkaPracownika,
    onZmiana: (ZakladkaPracownika) -> Unit,
    modifier: Modifier = Modifier
) {
    val ksztalt = RoundedCornerShape(22.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(ksztalt)
                .background(TloKarty)
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0.22f), RamkaKarty)
                    ),
                    shape = ksztalt
                )
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ZakladkaPracownika.entries.forEach { zakladka ->
                val zaznaczona = zakladka == aktywna
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (zaznaczona) AkcentNeon.copy(alpha = 0.16f)
                            else Color.Transparent
                        )
                        .clickable { onZmiana(zakladka) }
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = zakladka.ikona,
                        contentDescription = zakladka.etykieta,
                        tint = if (zaznaczona) AkcentNeon else TekstDrugi,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = zakladka.etykieta,
                        color = if (zaznaczona) TekstGlowny else TekstDrugi,
                        fontSize = 11.sp,
                        fontWeight = if (zaznaczona) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
