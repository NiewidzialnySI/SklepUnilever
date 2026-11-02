package shopunilever.dominik.pl.ui.ekrany

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import shopunilever.dominik.pl.onboarding.RolaUzytkownika
import shopunilever.dominik.pl.ui.components.TloAplikacji
import shopunilever.dominik.pl.ui.theme.AkcentMorski
import shopunilever.dominik.pl.ui.theme.AkcentNeon
import shopunilever.dominik.pl.ui.theme.TekstNaAkcencie
import shopunilever.dominik.pl.ui.theme.TloKarty

@Composable
fun EkranWyboruRoli(
    onWybrano: (RolaUzytkownika) -> Unit,
    modifier: Modifier = Modifier
) {
    val alfa = remember { Animatable(0f) }
    val skala = remember { Animatable(0.92f) }
    var wybrana by remember { mutableStateOf<RolaUzytkownika?>(null) }

    LaunchedEffect(Unit) {
        alfa.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
        skala.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
    }

    TloAplikacji(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .align(Alignment.Center)
                    .alpha(alfa.value)
                    .scale(skala.value),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                KartaPrzyciskuRoli(
                    ikona = Icons.Rounded.Badge,
                    tytul = "Pracownik",
                    zaznaczona = wybrana == RolaUzytkownika.PRACOWNIK,
                    onClick = { wybrana = RolaUzytkownika.PRACOWNIK },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )

                KartaPrzyciskuRoli(
                    ikona = Icons.Rounded.FamilyRestroom,
                    tytul = "Rodzina /\nznajomi",
                    zaznaczona = wybrana == RolaUzytkownika.RODZINA,
                    onClick = { wybrana = RolaUzytkownika.RODZINA },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }

            AnimatedVisibility(
                visible = wybrana != null,
                enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 2 },
                exit = fadeOut(tween(250)) + slideOutVertically(tween(250)) { it / 2 },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.horizontalGradient(listOf(AkcentNeon, AkcentMorski))
                        )
                        .clickable {
                            wybrana?.let(onWybrano)
                        }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Dalej",
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
private fun KartaPrzyciskuRoli(
    ikona: ImageVector,
    tytul: String,
    zaznaczona: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ksztalt = RoundedCornerShape(24.dp)
    val ramkaGlow = if (zaznaczona) {
        Brush.verticalGradient(
            listOf(
                AkcentNeon,
                AkcentMorski,
                AkcentNeon.copy(alpha = 0.8f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                AkcentNeon.copy(alpha = 0.45f),
                AkcentMorski.copy(alpha = 0.15f),
                Color.White.copy(alpha = 0.10f)
            )
        )
    }

    Surface(
        modifier = modifier
            .clip(ksztalt)
            .border(if (zaznaczona) 2.dp else 1.dp, ramkaGlow, ksztalt)
            .clickable(onClick = onClick),
        color = if (zaznaczona) AkcentNeon.copy(alpha = 0.16f) else TloKarty,
        shape = ksztalt
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            if (zaznaczona) AkcentNeon.copy(alpha = 0.22f) else AkcentNeon.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .padding(vertical = 16.dp, horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(
                            if (zaznaczona) {
                                Brush.linearGradient(listOf(AkcentNeon, AkcentMorski))
                            } else {
                                Brush.linearGradient(
                                    listOf(
                                        AkcentNeon.copy(alpha = 0.45f),
                                        AkcentMorski.copy(alpha = 0.20f)
                                    )
                                )
                            }
                        )
                        .border(
                            1.dp,
                            if (zaznaczona) Color.White else AkcentNeon.copy(alpha = 0.6f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ikona,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = tytul,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
