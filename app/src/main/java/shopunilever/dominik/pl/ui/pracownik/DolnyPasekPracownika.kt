package shopunilever.dominik.pl.ui.pracownik

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import shopunilever.dominik.pl.ui.theme.AkcentNeon
import shopunilever.dominik.pl.ui.theme.Blad
import shopunilever.dominik.pl.ui.theme.RamkaKarty
import shopunilever.dominik.pl.ui.theme.TekstDrugi
import shopunilever.dominik.pl.ui.theme.TekstNaAkcencie
import shopunilever.dominik.pl.ui.theme.TloKarty
import kotlin.math.roundToInt

/** Ikony-only bottom bar — szybki sliding pill, badge na Urządzeniach. */
@Composable
fun DolnyPasekPracownika(
    aktywna: ZakladkaPracownika,
    onZmiana: (ZakladkaPracownika) -> Unit,
    modifier: Modifier = Modifier,
    badgeUrzadzenia: Int = 0
) {
    val ksztalt = RoundedCornerShape(22.dp)
    val zakladki = ZakladkaPracownika.entries
    val indeksAnim by animateFloatAsState(
        targetValue = aktywna.ordinal.toFloat(),
        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
        label = "wskaznik_zakladki"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(ksztalt)
                .background(TloKarty)
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0.22f), RamkaKarty)
                    ),
                    shape = ksztalt
                )
                .padding(horizontal = 6.dp, vertical = 5.dp)
        ) {
            val szerokoscSlotu = maxWidth / zakladki.size
            val gestosc = LocalDensity.current
            val offsetPx = with(gestosc) { (szerokoscSlotu * indeksAnim).toPx() }

            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetPx.roundToInt(), 0) }
                    .width(szerokoscSlotu)
                    .fillMaxHeight()
                    .padding(horizontal = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AkcentNeon.copy(alpha = 0.16f))
            )

            Row(modifier = Modifier.fillMaxSize()) {
                zakladki.forEach { zakladka ->
                    val zaznaczona = zakladka == aktywna
                    val skala by animateFloatAsState(
                        targetValue = if (zaznaczona) 1.08f else 1f,
                        animationSpec = tween(140, easing = FastOutSlowInEasing),
                        label = "skala_${zakladka.name}"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = AkcentNeon),
                                role = Role.Tab,
                                onClick = { onZmiana(zakladka) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            Icon(
                                imageVector = zakladka.ikona,
                                contentDescription = zakladka.etykieta,
                                tint = if (zaznaczona) AkcentNeon else TekstDrugi,
                                modifier = Modifier
                                    .size(24.dp)
                                    .graphicsLayer {
                                        scaleX = skala
                                        scaleY = skala
                                    }
                            )
                            if (zakladka == ZakladkaPracownika.URZADZENIA && badgeUrzadzenia > 0) {
                                Box(
                                    modifier = Modifier
                                        .offset(x = 6.dp, y = (-4).dp)
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Blad),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (badgeUrzadzenia > 9) "9+" else badgeUrzadzenia.toString(),
                                        color = TekstNaAkcencie,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
