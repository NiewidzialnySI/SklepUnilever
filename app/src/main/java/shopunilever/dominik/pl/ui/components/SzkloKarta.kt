package shopunilever.dominik.pl.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import shopunilever.dominik.pl.ui.theme.AkcentNeon
import shopunilever.dominik.pl.ui.theme.RamkaKarty
import shopunilever.dominik.pl.ui.theme.TloKarty

@Composable
fun SzkloKarta(
    modifier: Modifier = Modifier,
    zaznaczona: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val ksztalt = RoundedCornerShape(22.dp)
    val ramka = if (zaznaczona) {
        Brush.linearGradient(listOf(AkcentNeon, AkcentNeon.copy(alpha = 0.25f)))
    } else {
        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.28f), RamkaKarty))
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(ksztalt)
            .border(1.dp, ramka, ksztalt)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            ),
        color = if (zaznaczona) AkcentNeon.copy(alpha = 0.12f) else TloKarty,
        content = {
            Column(modifier = Modifier.padding(18.dp), content = content)
        }
    )
}
