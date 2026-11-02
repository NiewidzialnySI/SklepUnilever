package shopunilever.dominik.pl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import shopunilever.dominik.pl.ui.theme.AkcentFiolet
import shopunilever.dominik.pl.ui.theme.AkcentMorski
import shopunilever.dominik.pl.ui.theme.AkcentNeon
import shopunilever.dominik.pl.ui.theme.TloAmoled

@Composable
fun TloAplikacji(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val brushNeon = remember {
        Brush.radialGradient(
            colors = listOf(AkcentNeon.copy(alpha = 0.20f), TloAmoled.copy(alpha = 0f)),
            center = Offset(80f, 120f),
            radius = 700f
        )
    }
    val brushFiolet = remember {
        Brush.radialGradient(
            colors = listOf(AkcentFiolet.copy(alpha = 0.14f), TloAmoled.copy(alpha = 0f)),
            center = Offset(920f, 260f),
            radius = 650f
        )
    }
    val brushPion = remember {
        Brush.verticalGradient(
            colors = listOf(
                TloAmoled.copy(alpha = 0f),
                AkcentMorski.copy(alpha = 0.08f),
                TloAmoled
            )
        )
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TloAmoled)
            .background(brushNeon)
            .background(brushFiolet)
            .background(brushPion),
        content = content
    )
}
