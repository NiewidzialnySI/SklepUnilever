package shopunilever.dominik.pl.ui.ekrany

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import shopunilever.dominik.pl.ui.components.TloAplikacji
import shopunilever.dominik.pl.ui.theme.AkcentMorski
import shopunilever.dominik.pl.ui.theme.AkcentNeon
import shopunilever.dominik.pl.ui.theme.TekstDrugi
import shopunilever.dominik.pl.ui.theme.TekstGlowny
import shopunilever.dominik.pl.ui.theme.TekstNaAkcencie
import androidx.compose.foundation.clickable

@Composable
fun EkranPowitalny(
    onDalej: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alfa = remember { Animatable(0f) }
    val skala = remember { Animatable(0.92f) }
    val alfaPrzycisku = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alfa.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        skala.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        alfaPrzycisku.animateTo(1f, tween(500, delayMillis = 180))
    }

    TloAplikacji(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(alfa.value)
                    .scale(skala.value),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(AkcentNeon.copy(alpha = 0.35f), AkcentMorski.copy(alpha = 0.12f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Storefront,
                        contentDescription = null,
                        tint = AkcentNeon,
                        modifier = Modifier.size(46.dp)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "SKLEP FIRMÓWY",
                    color = AkcentNeon,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Unilever",
                    color = TekstGlowny,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.8).sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Szybkie zakupy pracownicze.\nBezpiecznie, lokalnie, bez zbędnego hałasu.",
                    color = TekstDrugi,
                    fontSize = 16.sp,
                    lineHeight = 23.sp,
                    textAlign = TextAlign.Center
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(alfaPrzycisku.value),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.horizontalGradient(listOf(AkcentNeon, AkcentMorski))
                        )
                        .clickable(onClick = onDalej)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Zaczynamy",
                        color = TekstNaAkcencie,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Dalej wybierzesz profil i uprawnienia",
                    color = TekstDrugi,
                    fontSize = 12.sp
                )
            }
        }
    }
}
