package shopunilever.dominik.pl.ui.ekrany

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                // Dynamiczne pulsujące logo Unilever na środku
                Box(
                    modifier = Modifier
                        .scale(pulsujacaSkala)
                        .size(130.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    AkcentNeon.copy(alpha = 0.25f),
                                    AkcentMorski.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            )
                        )
                        .border(
                            2.dp,
                            Brush.sweepGradient(
                                listOf(
                                    AkcentNeon,
                                    AkcentMorski,
                                    Color.White,
                                    AkcentNeon
                                )
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    LogoUnilever(
                        modifier = Modifier.size(76.dp)
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

@Composable
fun LogoUnilever(
    modifier: Modifier = Modifier,
    kolorGlow: Color = AkcentNeon,
    kolorGlow2: Color = AkcentMorski
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Promienista poświata pod logo
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(kolorGlow.copy(alpha = 0.40f), Color.Transparent),
                center = Offset(w / 2, h / 2),
                radius = w * 0.55f
            )
        )

        // Linia 'U' Unilever
        val pathU = Path().apply {
            moveTo(w * 0.28f, h * 0.22f)
            lineTo(w * 0.28f, h * 0.52f)
            cubicTo(
                w * 0.28f, h * 0.84f,
                w * 0.72f, h * 0.84f,
                w * 0.72f, h * 0.52f
            )
            lineTo(w * 0.72f, h * 0.22f)
        }

        drawPath(
            path = pathU,
            brush = Brush.verticalGradient(
                colors = listOf(kolorGlow, kolorGlow2, Color.White)
            ),
            style = Stroke(
                width = w * 0.12f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Akcent kropka/kropla po lewej i prawej stronie u góry U
        drawCircle(
            color = Color.White,
            radius = w * 0.05f,
            center = Offset(w * 0.28f, h * 0.14f)
        )
        drawCircle(
            color = kolorGlow,
            radius = w * 0.05f,
            center = Offset(w * 0.72f, h * 0.14f)
        )
    }
}