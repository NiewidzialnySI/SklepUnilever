package shopunilever.dominik.pl.dane

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import shopunilever.dominik.pl.firebase.StaleFirebase
import kotlin.random.Random

object PreferencjeParowania {
    private const val PREFS = "sklep_parowanie_secure"
    private const val K_KOD = "kod_parowania"
    private const val K_UID = "firebase_uid"
    private const val K_AKTYWNY = "parowanie_aktywne"

    private fun prefs(context: Context) = try {
        val master = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS,
            master,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (_: Exception) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    fun kod(context: Context): String =
        prefs(context).getString(K_KOD, "") ?: ""

    fun uid(context: Context): String =
        prefs(context).getString(K_UID, "") ?: ""

    fun czyAktywne(context: Context): Boolean =
        prefs(context).getBoolean(K_AKTYWNY, false) && kod(context).isNotBlank()

    fun zapisz(context: Context, kod: String, uid: String) {
        prefs(context).edit {
            putString(K_KOD, kod)
            putString(K_UID, uid)
            putBoolean(K_AKTYWNY, true)
        }
    }

    fun wyczysc(context: Context) {
        prefs(context).edit { clear() }
    }

    fun generujKod(): String {
        val alfabet = StaleFirebase.ALFABET
        return buildString(StaleFirebase.DLUGOSC_KODU) {
            repeat(StaleFirebase.DLUGOSC_KODU) {
                append(alfabet[Random.nextInt(alfabet.length)])
            }
        }
    }

    fun normalizuj(kod: String): String =
        kod.uppercase()
            .replace(Regex("[^A-Z0-9]"), "")
            .take(StaleFirebase.DLUGOSC_KODU)
}
