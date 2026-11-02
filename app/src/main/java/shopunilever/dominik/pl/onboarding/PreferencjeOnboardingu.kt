package shopunilever.dominik.pl.onboarding

import android.content.Context
import androidx.core.content.edit

object PreferencjeOnboardingu {
    private const val PREFS = "sklep_onboarding"
    private const val K_WITAMY = "witamy_zakonczone"
    private const val K_ROLA = "rola"
    private const val K_UPRAWNIENIA = "uprawnienia_zakonczone"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun czyWitamyZakonczone(context: Context): Boolean =
        prefs(context).getBoolean(K_WITAMY, false)

    fun ustawWitamyZakonczone(context: Context, wartosc: Boolean = true) {
        prefs(context).edit { putBoolean(K_WITAMY, wartosc) }
    }

    fun rola(context: Context): RolaUzytkownika? =
        RolaUzytkownika.zKlucza(prefs(context).getString(K_ROLA, null))

    fun ustawRole(context: Context, rola: RolaUzytkownika) {
        prefs(context).edit { putString(K_ROLA, rola.klucz) }
    }

    fun czyUprawnieniaZakonczone(context: Context): Boolean =
        prefs(context).getBoolean(K_UPRAWNIENIA, false)

    fun ustawUprawnieniaZakonczone(context: Context, wartosc: Boolean = true) {
        prefs(context).edit { putBoolean(K_UPRAWNIENIA, wartosc) }
    }
}
