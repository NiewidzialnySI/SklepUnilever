package shopunilever.dominik.pl.dane

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Dane służbowe do auto-logowania sklepiku (jak SecureStore w Harmonogramie).
 */
object PreferencjeKonta {
    private const val PREFS = "sklep_konto_secure"
    private const val K_EMAIL = "email_sluzbowy"
    private const val K_HASLO = "haslo_sklepiku"
    private const val K_DANE_ZAPISANE = "dane_zapisane"

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

    fun czyDaneZapisane(context: Context): Boolean {
        val p = prefs(context)
        return p.getBoolean(K_DANE_ZAPISANE, false) &&
            !p.getString(K_EMAIL, null).isNullOrBlank() &&
            !p.getString(K_HASLO, null).isNullOrBlank()
    }

    fun email(context: Context): String =
        prefs(context).getString(K_EMAIL, "") ?: ""

    fun haslo(context: Context): String =
        prefs(context).getString(K_HASLO, "") ?: ""

    fun zapiszDane(context: Context, email: String, haslo: String) {
        prefs(context).edit {
            putString(K_EMAIL, email.trim())
            putString(K_HASLO, haslo)
            putBoolean(K_DANE_ZAPISANE, email.isNotBlank() && haslo.isNotBlank())
        }
    }

    fun wyczysc(context: Context) {
        prefs(context).edit { clear() }
    }
}
