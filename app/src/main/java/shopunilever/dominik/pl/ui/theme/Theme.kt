package shopunilever.dominik.pl.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SchematCiemny = darkColorScheme(
    primary = AkcentNeon,
    onPrimary = TekstNaAkcencie,
    secondary = AkcentMorski,
    onSecondary = TekstNaAkcencie,
    tertiary = AkcentFiolet,
    background = TloAmoled,
    onBackground = TekstGlowny,
    surface = TloWarstwa,
    onSurface = TekstGlowny,
    surfaceVariant = TloKarty,
    onSurfaceVariant = TekstDrugi,
    outline = RamkaKarty,
    error = Blad
)

@Composable
fun SklepUnileverTheme(content: @Composable () -> Unit) {
    val widok = LocalView.current
    if (!widok.isInEditMode) {
        SideEffect {
            val okno = (widok.context as Activity).window
            okno.statusBarColor = TloAmoled.toArgb()
            okno.navigationBarColor = TloAmoled.toArgb()
            WindowCompat.getInsetsController(okno, widok).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = SchematCiemny,
        typography = Typography,
        content = content
    )
}
