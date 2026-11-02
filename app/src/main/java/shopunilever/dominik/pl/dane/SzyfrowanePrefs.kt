package shopunilever.dominik.pl.dane

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.concurrent.ConcurrentHashMap

/**
 * Cache EncryptedSharedPreferences — tworzenie MasterKey przy każdym odczycie
 * mocno spowalnia UI (zakładka Parowanie / Firebase).
 */
internal object SzyfrowanePrefs {
    private val cache = ConcurrentHashMap<String, SharedPreferences>()

    fun pobierz(context: Context, nazwa: String): SharedPreferences {
        val app = context.applicationContext
        return cache[nazwa] ?: synchronized(cache) {
            cache[nazwa] ?: utworz(app, nazwa).also { cache[nazwa] = it }
        }
    }

    private fun utworz(context: Context, nazwa: String): SharedPreferences = try {
        val master = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            nazwa,
            master,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (_: Exception) {
        context.getSharedPreferences(nazwa, Context.MODE_PRIVATE)
    }
}
