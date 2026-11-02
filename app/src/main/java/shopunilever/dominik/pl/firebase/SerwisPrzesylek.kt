package shopunilever.dominik.pl.firebase

import android.content.Context
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
    val dodano: Long,
    val dataMaila: Long,
    val odczytano: Boolean
)

object SerwisPrzesylek {

    /**
     * Paczki nadajnika (pracownik) albo sparowanego nadajnika (rodzina).
     */
    fun obserwujPrzesylki(context: Context): Flow<List<PrzesylkaPaczki>> = callbackFlow {
        val uidNadajnika = PreferencjeParowania.uid(context).ifBlank {
            PreferencjeOdbiorcy.uidNadajnika(context)
        }
        if (uidNadajnika.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val ref = FirebaseSklepik.baza(context).reference
            .child(StaleFirebase.Sciezki.TRANSMITTERS)
            .child(uidNadajnika)
            .child(StaleFirebase.Sciezki.SHIPMENTS)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lista = snapshot.children.mapNotNull { dziecko ->
                    val id = dziecko.key ?: return@mapNotNull null
                    val numer = dziecko.child("numer").getValue(String::class.java).orEmpty()
                    if (numer.isBlank()) return@mapNotNull null
                    PrzesylkaPaczki(
                        id = id,
                        numer = numer,
                        kurier = dziecko.child("kurier").getValue(String::class.java).orEmpty()
                            .ifBlank { "?" },
                        temat = dziecko.child("temat").getValue(String::class.java).orEmpty(),
                        statusTekst = dziecko.child("statusTekst").getValue(String::class.java),
                        szacowanaDostawa = dziecko.child("szacowanaDostawa")
                            .getValue(String::class.java),
                        linkSledzenia = dziecko.child("linkSledzenia").getValue(String::class.java),
                        nadawcaPaczki = dziecko.child("nadawcaPaczki").getValue(String::class.java),
                        dodano = dziecko.child("dodano").getValue(Long::class.java) ?: 0L,
                        dataMaila = dziecko.child("dataMaila").getValue(Long::class.java) ?: 0L,
                        odczytano = dziecko.child("odczytano").getValue(Boolean::class.java) == true
                    )
                }.sortedByDescending { maxOf(it.dodano, it.dataMaila) }
                trySend(lista)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }.distinctUntilChanged()

    suspend fun oznaczOdczytana(context: Context, id: String) {
        val uid = PreferencjeParowania.uid(context).ifBlank { return }
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

    suspend fun usunPrzesylke(context: Context, id: String) {
        val uid = PreferencjeParowania.uid(context).ifBlank { return }
        if (id.isBlank()) return
        FirebaseSklepik.baza(context).reference
            .child(StaleFirebase.Sciezki.TRANSMITTERS)
            .child(uid)
            .child(StaleFirebase.Sciezki.SHIPMENTS)
            .child(id)
            .removeValue()
            .await()
    }
}
