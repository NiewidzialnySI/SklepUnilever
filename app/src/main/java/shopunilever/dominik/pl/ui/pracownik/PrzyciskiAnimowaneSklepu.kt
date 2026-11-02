package shopunilever.dominik.pl.ui.pracownik

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun PrzyciskAnimowanySklepu(
    ikona: ImageVector,
    opis: String,
    kolor: Color,
    animacja: AnimacjaPrzyciskuSklepu,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val zakres = rememberCoroutineScope()
    val rotacja = remember { Animatable(0f) }
    val przesuniecieX = remember { Animatable(0f) }
    val skala = remember { Animatable(1f) }

    fun odtworz() {
        zakres.launch {
            when (animacja) {
                AnimacjaPrzyciskuSklepu.OBROT -> {
                    rotacja.snapTo(0f)
                    rotacja.animateTo(360f, tween(520, easing = FastOutSlowInEasing))
                    rotacja.snapTo(0f)
                }
                AnimacjaPrzyciskuSklepu.COFNIECIE -> {
                    przesuniecieX.snapTo(0f)
                    przesuniecieX.animateTo(-10f, tween(90))
                    przesuniecieX.animateTo(0f, tween(180, easing = FastOutSlowInEasing))
                }
                AnimacjaPrzyciskuSklepu.PULSE -> {
                    skala.animateTo(0.86f, tween(80))
                    skala.animateTo(1f, tween(160, easing = FastOutSlowInEasing))
                }
            }
        }
    }

    IconButton(
        onClick = {
            odtworz()
            onClick()
        },
        modifier = modifier
            .graphicsLayer {
                rotationZ = rotacja.value
                translationX = przesuniecieX.value
                scaleX = skala.value
                scaleY = skala.value
            }
    ) {
        Icon(
            imageVector = ikona,
            contentDescription = opis,
            tint = kolor,
            modifier = Modifier.size(22.dp)
        )
    }
}

enum class AnimacjaPrzyciskuSklepu {
    OBROT,
    COFNIECIE,
    PULSE
}
