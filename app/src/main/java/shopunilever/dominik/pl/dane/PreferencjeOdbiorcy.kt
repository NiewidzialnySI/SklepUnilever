package shopunilever.dominik.pl.dane

import android.content.Context
import androidx.core.content.edit

object PreferencjeOdbiorcy {
    private const val PREFS = "sklep_odbiorca_secure"
    private const val K_KOD = "kod_parowania"
    private const val K_UID = "uid_odbiorcy"
    private const val K_TX = "uid_nadajnika"
    private const val K_STATUS = "status" // pending | approved | rejected
    private const val K_NAZWA = "nazwa_urzadzenia"

    private fun prefs(context: Context) = SzyfrowanePrefs.pobierz(context, PREFS)

    fun kod(context: Context): String = prefs(context).getString(K_KOD, "") ?: ""
    fun uid(context: Context): String = prefs(context).getString(K_UID, "") ?: ""
    fun uidNadajnika(context: Context): String = prefs(context).getString(K_TX, "") ?: ""
    fun status(context: Context): String = prefs(context).getString(K_STATUS, "") ?: ""
    fun nazwaUrzadzenia(context: Context): String =
        prefs(context).getString(K_NAZWA, "") ?: ""

    fun czyOczekuje(context: Context): Boolean = status(context) == "pending"
    fun czyZatwierdzony(context: Context): Boolean = status(context) == "approved" &&
        uidNadajnika(context).isNotBlank()

    fun zapiszProsbe(
        context: Context,
        kod: String,
        uid: String,
        uidNadajnika: String,
        nazwa: String
    ) {
        prefs(context).edit {
            putString(K_KOD, kod)
            putString(K_UID, uid)
            putString(K_TX, uidNadajnika)
            putString(K_STATUS, "pending")
            putString(K_NAZWA, nazwa)
        }
    }

    fun ustawStatus(context: Context, status: String) {
        prefs(context).edit { putString(K_STATUS, status) }
    }

    fun wyczysc(context: Context) {
        prefs(context).edit { clear() }
    }
}
