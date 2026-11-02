package shopunilever.dominik.pl.onboarding

import android.Manifest
import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.ui.graphics.vector.ImageVector

enum class RodzajProsby {
    /** Klasyczne runtime permissions (Activity Result). */
    RUNTIME,

    /** Ustawienia systemowe: nieznane źródła / instalacja APK. */
    INSTALACJA_APK,

    /** Ustawienia: wyłączenie optymalizacji baterii. */
    BATERIA
}

/**
 * Jedna pozycja na ekranie uprawnień — zależna od roli.
 */
data class PozycjaUprawnienia(
    val id: String,
    val tytul: String,
    val opis: String,
    val ikona: ImageVector,
    val rodzaj: RodzajProsby,
    /** Jeśli false — nie blokuje wejścia do sklepu. */
    val wymagane: Boolean = true,
    val uprawnieniaRuntime: List<String> = emptyList()
)

object KatalogUprawnien {

    fun dlaRoli(rola: RolaUzytkownika): List<PozycjaUprawnienia> = when (rola) {
        RolaUzytkownika.PRACOWNIK -> pracownik()
        RolaUzytkownika.RODZINA -> rodzina()
    }

    /** Pracownik — pełny dostęp, auto-logowanie SMS, sync. */
    private fun pracownik(): List<PozycjaUprawnienia> = listOf(
        PozycjaUprawnienia(
            id = "sms",
            tytul = "SMS",
            opis = "Odczyt kodu logowania ze sklepu firmowego (auto-logowanie) oraz przekazanie kodu urządzeniom rodziny.",
            ikona = Icons.Rounded.Sms,
            rodzaj = RodzajProsby.RUNTIME,
            uprawnieniaRuntime = listOf(
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS
            )
        ),
        pozycjaPowiadomienia(opisRodziny = false),
        pozycjaAktualizacji(),
        PozycjaUprawnienia(
            id = "bateria",
            tytul = "Praca w tle",
            opis = "Wyłączenie optymalizacji baterii, żeby sync i powiadomienia działały także gdy telefon śpi.",
            ikona = Icons.Rounded.BatteryChargingFull,
            rodzaj = RodzajProsby.BATERIA
        )
    )

    /** Rodzina / znajomi — bez SMS, z powiadomieniami i aktualizacjami. */
    private fun rodzina(): List<PozycjaUprawnienia> = listOf(
        pozycjaPowiadomienia(opisRodziny = true),
        pozycjaAktualizacji()
    )

    private fun pozycjaPowiadomienia(opisRodziny: Boolean): PozycjaUprawnienia {
        val runtime = if (Build.VERSION.SDK_INT >= 33) {
            listOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyList()
        }
        return PozycjaUprawnienia(
            id = "powiadomienia",
            tytul = "Powiadomienia",
            opis = if (opisRodziny) {
                "Informacje o dostępie do sklepu, parowaniu z telefonem pracownika i statusie zakupów."
            } else {
                "Alerty o logowaniu, prośbach o dostęp od rodziny oraz statusie synchronizacji."
            },
            ikona = Icons.Rounded.Notifications,
            rodzaj = RodzajProsby.RUNTIME,
            uprawnieniaRuntime = runtime
        )
    }

    private fun pozycjaAktualizacji() = PozycjaUprawnienia(
        id = "instalacja",
        tytul = "Instalacja aktualizacji",
        opis = "Pozwala zainstalować nową wersję aplikacji bezpośrednio z okna aktualizacji. Możesz pominąć i włączyć później.",
        ikona = Icons.Rounded.SystemUpdate,
        rodzaj = RodzajProsby.INSTALACJA_APK,
        wymagane = false
    )
}
