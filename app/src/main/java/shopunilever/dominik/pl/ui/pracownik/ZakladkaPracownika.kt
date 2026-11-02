package shopunilever.dominik.pl.ui.pracownik

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class ZakladkaPracownika(
    val etykieta: String,
    val ikona: ImageVector
) {
    DOM("Dom", Icons.Rounded.Home),
    URZADZENIA("Urządzenia", Icons.Rounded.Devices),
    USTAWIENIA("Ustawienia", Icons.Rounded.Settings)
}
