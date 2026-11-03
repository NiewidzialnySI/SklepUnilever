package shopunilever.dominik.pl.firebase

import android.content.Context
import android.net.Uri
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import shopunilever.dominik.pl.dane.PreferencjeOdbiorcy
import shopunilever.dominik.pl.dane.PreferencjeParowania

data class PrzesylkaPaczki(
    val id: String,
    val numer: String,
    val kurier: String,
    val temat: String,
    val statusTekst: String?,
    val szacowanaDostawa: String?,
    val linkSledzenia: String?,
    val nadawcaPaczki: String?,
    val pinOdbioru: String? = null,
    val telefonKuriera: String? = null,
    val dodano: Long,
    val dataMaila: Long,
    val odczytano: Boolean,
    val archiwumTs: Long = 0L
) {
    /** Zawsze buduje świeży deep-link kuriera (zapisany URL bywa nieaktualny). */
    fun linkDoOtwarcia(): String =
        SerwisPrzesylek.zbudujLinkSledzenia(kurier, numer, linkSledzenia)
}

object SerwisPrzesylek {

    fun uidNadajnika(context: Context): String =
        PreferencjeParowania.uid(context).ifBlank {
            PreferencjeOdbiorcy.uidNadajnika(context)
        }

    fun obserwujPrzesylki(context: Context): Flow<List<PrzesylkaPaczki>> =
        obserwujListe(context, StaleFirebase.Sciezki.SHIPMENTS)

    fun obserwujArchiwum(context: Context): Flow<List<PrzesylkaPaczki>> =
        obserwujListe(context, StaleFirebase.Sciezki.SHIPMENTS_ARCHIWUM)

    private fun obserwujListe(context: Context, wezel: String): Flow<List<PrzesylkaPaczki>> =
        callbackFlow {
            val uidNadajnika = uidNadajnika(context)
            if (uidNadajnika.isBlank()) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }
            val ref = FirebaseSklepik.baza(context).reference
                .child(StaleFirebase.Sciezki.TRANSMITTERS)
                .child(uidNadajnika)
                .child(wezel)
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val lista = snapshot.children.mapNotNull { dziecko ->
                        mapujPrzesylke(dziecko)
                    }.sortedByDescending {
                        maxOf(it.archiwumTs, it.dodano, it.dataMaila)
                    }
                    trySend(lista)
                }

                override fun onCancelled(error: DatabaseError) {
                    trySend(emptyList())
                }
            }
            ref.addValueEventListener(listener)
            awaitClose { ref.removeEventListener(listener) }
        }.distinctUntilChanged()

    private fun mapujPrzesylke(dziecko: DataSnapshot): PrzesylkaPaczki? {
        val id = dziecko.key ?: return null
        val numer = dziecko.child("numer").getValue(String::class.java).orEmpty()
        if (numer.isBlank()) return null
        return PrzesylkaPaczki(
            id = id,
            numer = numer,
            kurier = dziecko.child("kurier").getValue(String::class.java).orEmpty()
                .ifBlank { "?" },
            temat = dziecko.child("temat").getValue(String::class.java).orEmpty(),
            statusTekst = dziecko.child("statusTekst").getValue(String::class.java),
            szacowanaDostawa = dziecko.child("szacowanaDostawa").getValue(String::class.java),
            linkSledzenia = dziecko.child("linkSledzenia").getValue(String::class.java),
            nadawcaPaczki = dziecko.child("nadawcaPaczki").getValue(String::class.java),
            pinOdbioru = dziecko.child("pinOdbioru").getValue(String::class.java)
                ?.takeIf { it.isNotBlank() },
            telefonKuriera = dziecko.child("telefonKuriera").getValue(String::class.java)
                ?.takeIf { it.isNotBlank() },
            dodano = dziecko.child("dodano").getValue(Long::class.java) ?: 0L,
            dataMaila = dziecko.child("dataMaila").getValue(Long::class.java) ?: 0L,
            odczytano = dziecko.child("odczytano").getValue(Boolean::class.java) == true,
            archiwumTs = dziecko.child("archiwumTs").getValue(Long::class.java) ?: 0L
        )
    }

    suspend fun oznaczOdczytana(context: Context, id: String) {
        val uid = uidNadajnika(context).ifBlank { return }
        if (id.isBlank()) return
        FirebaseSklepik.baza(context).reference
            .child(StaleFirebase.Sciezki.TRANSMITTERS)
            .child(uid)
            .child(StaleFirebase.Sciezki.SHIPMENTS)
            .child(id)
            .child("odczytano")
            .setValue(true)
            .await()
    }

    /** Przenosi aktywną paczkę do archiwum (soft-delete). */
    suspend fun archiwizuj(context: Context, id: String) {
        przenies(context, id, z = StaleFirebase.Sciezki.SHIPMENTS, do_ = StaleFirebase.Sciezki.SHIPMENTS_ARCHIWUM, archiwum = true)
    }

    /** Przywraca z archiwum na listę aktywną. */
    suspend fun przywroc(context: Context, id: String) {
        przenies(context, id, z = StaleFirebase.Sciezki.SHIPMENTS_ARCHIWUM, do_ = StaleFirebase.Sciezki.SHIPMENTS, archiwum = false)
    }

    /** Trwałe usunięcie z archiwum. */
    suspend fun usunZArchiwum(context: Context, id: String) {
        val uid = uidNadajnika(context).ifBlank { return }
        if (id.isBlank()) return
        FirebaseSklepik.baza(context).reference
            .child(StaleFirebase.Sciezki.TRANSMITTERS)
            .child(uid)
            .child(StaleFirebase.Sciezki.SHIPMENTS_ARCHIWUM)
            .child(id)
            .removeValue()
            .await()
    }

    private suspend fun przenies(
        context: Context,
        id: String,
        z: String,
        do_: String,
        archiwum: Boolean
    ) {
        val uid = uidNadajnika(context).ifBlank { return }
        if (id.isBlank()) return
        val baza = FirebaseSklepik.baza(context).reference
            .child(StaleFirebase.Sciezki.TRANSMITTERS)
            .child(uid)
        val zrodlo = baza.child(z).child(id)
        val snap = zrodlo.get().await()
        if (!snap.exists()) return
        val mapa = snap.value as? Map<*, *> ?: return
        val kopia = mapa.toMutableMap()
        if (archiwum) {
            kopia["archiwumTs"] = System.currentTimeMillis()
            kopia["odczytano"] = true
        } else {
            kopia.remove("archiwumTs")
        }
        baza.child(do_).child(id).setValue(kopia).await()
        zrodlo.removeValue().await()
    }

    /**
     * Kanoniczne deep-linki PL (Shoper / najszybsza.pl / gist przemoc):
     * UPS → wwwapps.ups.com/WebTracking
     * DHL → mojdhl.pl (krajowe) / sprawdz.dhl.com.pl
     */
    fun zbudujLinkSledzenia(kurier: String, numer: String, zapisany: String? = null): String {
        val n = numer.trim()
        if (n.isBlank()) return zapisany.orEmpty()
        val enc = Uri.encode(n)
        return when (kurier.lowercase().trim()) {
            "ups" -> "https://wwwapps.ups.com/WebTracking?loc=pl_PL&tracknum=$enc"
            "dhl" -> {
                val krajowy = n.matches(Regex("""(?i)JJD[A-Z0-9]{10,}""")) ||
                    n.matches(Regex("""\d{10,14}"""))
                if (krajowy) {
                    "https://mojdhl.pl/paczki/dostawa/$enc"
                } else {
                    "https://sprawdz.dhl.com.pl/szukaj.aspx?m=0&sn=$enc"
                }
            }
            else -> zapisany?.takeIf { it.startsWith("http") }.orEmpty()
        }
    }
}
