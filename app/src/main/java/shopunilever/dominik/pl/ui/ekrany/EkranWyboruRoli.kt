package shopunilever.dominik.pl.ui.ekrany

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import shopunilever.dominik.pl.onboarding.RolaUzytkownika
import shopunilever.dominik.pl.ui.components.SzkloKarta
import shopunilever.dominik.pl.ui.components.TloAplikacji
import shopunilever.dominik.pl.ui.theme.AkcentMorski
import shopunilever.dominik.pl.ui.theme.AkcentNeon
import shopunilever.dominik.pl.ui.theme.TekstDrugi
import shopunilever.dominik.pl.ui.theme.TekstGlowny
import shopunilever.dominik.pl.ui.theme.TekstNaAkcencie

@Composable
fun EkranWyboruRoli(
    onWybrano: (RolaUzytkownika) -> Unit,
    modifier: Modifier = Modifier
) {
    var wybrana by remember { mutableStateOf<RolaUzytkownika?>(null) }

    TloAplikacji(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "PROFIL",
                    color = AkcentNeon,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Kim jesteś?",
                    color = TekstGlowny,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Od tego zależy, o jakie uprawnienia poprosimy.",
                    color = TekstDrugi,
                    fontSize = 15.sp,
                    lineHeight = 21.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                KartaRoli(
                    ikona = Icons.Rounded.Badge,
                    rola = RolaUzytkownika.PRACOWNIK,
                    zaznaczona = wybrana == RolaUzytkownika.PRACOWNIK,
                    onClick = { wybrana = RolaUzytkownika.PRACOWNIK }
                )
                Spacer(modifier = Modifier.height(14.dp))
                KartaRoli(
                    ikona = Icons.Rounded.FamilyRestroom,
                    rola = RolaUzytkownika.RODZINA,
                    zaznaczona = wybrana == RolaUzytkownika.RODZINA,
                    onClick = { wybrana = RolaUzytkownika.RODZINA }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (wybrana != null) {
                            Brush.horizontalGradient(listOf(AkcentNeon, AkcentMorski))
                        } else {
                            Brush.horizontalGradient(
                                listOf(
                                    TekstDrugi.copy(alpha = 0.25f),
                                    TekstDrugi.copy(alpha = 0.18f)
                                )
                            )
                        }
                    )
                    .clickable(enabled = wybrana != null) {
                        wybrana?.let(onWybrano)
                    }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Kontynuuj",
                    color = if (wybrana != null) TekstNaAkcencie else TekstDrugi,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun KartaRoli(
    ikona: ImageVector,
    rola: RolaUzytkownika,
    zaznaczona: Boolean,
    onClick: () -> Unit
) {
    SzkloKarta(zaznaczona = zaznaczona, onClick = onClick) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AkcentNeon.copy(alpha = if (zaznaczona) 0.22f else 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(ikona, contentDescription = null, tint = AkcentNeon)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rola.etykieta,
                    color = TekstGlowny,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = rola.opis,
                    color = TekstDrugi,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
