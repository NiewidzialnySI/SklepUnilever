package shopunilever.dominik.pl.dane

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class PozycjaKoszyka(
    val nazwa: String,
    val sku: String = "",
    val ilosc: Int = 1,
    val url: String = "",
    val cena: String = ""
)

data class ZapisanyKoszyk(
    val id: String,
    val nazwa: String,
    val utworzono: Long,
    val pozycje: List<PozycjaKoszyka>
)

object PreferencjeKoszykow {
    private const val PREFS = "sklep_koszyki"
    private const val K_LISTA = "lista_json"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun lista(context: Context): List<ZapisanyKoszyk> {
        val raw = prefs(context).getString(K_LISTA, "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val poz = o.getJSONArray("pozycje")
                    val pozycje = buildList {
                        for (j in 0 until poz.length()) {
                            val p = poz.getJSONObject(j)
                            add(
                                PozycjaKoszyka(
                                    nazwa = p.optString("nazwa"),
                                    sku = p.optString("sku"),
                                    ilosc = p.optInt("ilosc", 1),
                                    url = p.optString("url"),
                                    cena = p.optString("cena")
                                )
                            )
                        }
                    }
                    add(
                        ZapisanyKoszyk(
                            id = o.getString("id"),
                            nazwa = o.optString("nazwa", "Koszyk"),
                            utworzono = o.optLong("utworzono", 0L),
                            pozycje = pozycje
                        )
                    )
                }
            }.sortedByDescending { it.utworzono }
        }.getOrDefault(emptyList())
    }

    fun zapisz(context: Context, nazwa: String, pozycje: List<PozycjaKoszyka>): ZapisanyKoszyk {
        val nowy = ZapisanyKoszyk(
            id = UUID.randomUUID().toString(),
            nazwa = nazwa.ifBlank { "Koszyk ${lista(context).size + 1}" },
            utworzono = System.currentTimeMillis(),
            pozycje = pozycje
        )
        val wszystkie = lista(context) + nowy
        zapiszListe(context, wszystkie)
        return nowy
    }

    fun usun(context: Context, id: String) {
        zapiszListe(context, lista(context).filterNot { it.id == id })
    }

    private fun zapiszListe(context: Context, koszyki: List<ZapisanyKoszyk>) {
        val arr = JSONArray()
        koszyki.forEach { k ->
            val poz = JSONArray()
            k.pozycje.forEach { p ->
                poz.put(
                    JSONObject()
                        .put("nazwa", p.nazwa)
                        .put("sku", p.sku)
                        .put("ilosc", p.ilosc)
                        .put("url", p.url)
                        .put("cena", p.cena)
                )
            }
            arr.put(
                JSONObject()
                    .put("id", k.id)
                    .put("nazwa", k.nazwa)
                    .put("utworzono", k.utworzono)
                    .put("pozycje", poz)
            )
        }
        prefs(context).edit { putString(K_LISTA, arr.toString()) }
    }
}
