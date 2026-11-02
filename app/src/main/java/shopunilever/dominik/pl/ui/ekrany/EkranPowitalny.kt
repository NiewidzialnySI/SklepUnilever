package shopunilever.dominik.pl.ui.ekrany

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import shopunilever.dominik.pl.R
import shopunilever.dominik.pl.ui.components.TloAplikacji
import shopunilever.dominik.pl.ui.theme.AkcentMorski
import shopunilever.dominik.pl.ui.theme.AkcentNeon
import shopunilever.dominik.pl.ui.theme.TekstGlowny
import shopunilever.dominik.pl.ui.theme.TekstNaAkcencie

@Composable
fun EkranPowitalny(
    onDalej: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alfa = remember { Animatable(0f) }
    val skala = remember { Animatable(0.85f) }
    val alfaPrzycisku = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "animacja_logo")
    val pulsujacaSkala by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skala_pulse"
    )

    LaunchedEffect(Unit) {
        alfa.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        skala.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        alfaPrzycisku.animateTo(1f, tween(500, delayMillis = 200))
    }

    TloAplikacji(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
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
                        .scale(pulsujacaSkala)
                        .size(130.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_background),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "SKLEP FIRMOWY",
                    color = AkcentNeon,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Unilever",
                    color = TekstGlowny,
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1.0).sp,
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
            }
        }
    }
}
