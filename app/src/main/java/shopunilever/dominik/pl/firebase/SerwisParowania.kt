package shopunilever.dominik.pl.firebase

import android.content.Context
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import shopunilever.dominik.pl.dane.PreferencjeKonta
import shopunilever.dominik.pl.dane.PreferencjeParowania

data class ProsbaDostepu(
    val id: String,
    val nazwaUrzadzenia: String,
    val createdAt: Long,
    val status: String
)

data class OdbiorcaPolaczony(
    val id: String,
    val nazwa: String,
    val zablokowany: Boolean
)

/**
 * Pracownik = nadajnik: kod parowania, sync credentials, relay SMS.
 */
object SerwisParowania {

    suspend fun uruchomJakoNadajnik(context: Context): String {
        val uid = FirebaseSklepik.zalogujAnonimowo(context)
        val istniejacy = PreferencjeParowania.kod(context)
        val kod = istniejacy.ifBlank { PreferencjeParowania.generujKod() }
        val db = FirebaseSklepik.baza(context).reference
        val email = PreferencjeKonta.email(context)

        db.child(StaleFirebase.Sciezki.TRANSMITTERS).child(uid).updateChildren(
            mapOf(
                "syncCode" to kod,
                "email" to email.ifBlank { null },
                "active" to true,
                "updatedAt" to System.currentTimeMillis(),
                "role" to "nadajnik"
            )
        ).await()

        db.child(StaleFirebase.Sciezki.PAIRING_CODES).child(kod).setValue(
            mapOf(
                "transmitterUid" to uid,
                "createdAt" to System.currentTimeMillis()
            )
        ).await()

        PreferencjeParowania.zapisz(context, kod, uid)
        publikujDaneLogowania(context)
        return kod
    }

    suspend fun publikujDaneLogowania(context: Context) {
        val uid = PreferencjeParowania.uid(context).ifBlank { return }
        val email = PreferencjeKonta.email(context)
        val haslo = PreferencjeKonta.haslo(context)
        if (email.isBlank() || haslo.isBlank()) return
        val kod = PreferencjeParowania.kod(context)
        FirebaseSklepik.baza(context).reference
            .child(StaleFirebase.Sciezki.TRANSMITTERS)
            .child(uid)
            .child("credentials")
            .setValue(
                mapOf(
                    "email" to email,
                    "haslo" to haslo,
                    "zaktualizowano" to System.currentTimeMillis(),
                    "syncCode" to kod
                )
            ).await()
    }

    /** Bezpieczny relay: tylko krótkotrwały kod SMS + timestamp, bez treści wiadomości. */
    suspend fun publikujKodSms(context: Context, kodSms: String) {
        val uid = PreferencjeParowania.uid(context).ifBlank { return }
        if (!PreferencjeParowania.czyAktywne(context)) return
        val sync = PreferencjeParowania.kod(context)
        FirebaseSklepik.baza(context).reference
            .child(StaleFirebase.Sciezki.TRANSMITTERS)
            .child(uid)
            .child("smsRelay")
            .setValue(
                mapOf(
                    "kod" to kodSms,
                    "ts" to System.currentTimeMillis(),
                    "syncCode" to sync
                )
            ).await()
    }

    suspend fun zatwierdzProsbe(context: Context, prosbaId: String) {
        val uid = PreferencjeParowania.uid(context).ifBlank { return }
        FirebaseSklepik.baza(context).reference
            .child(StaleFirebase.Sciezki.TRANSMITTERS)
            .child(uid)
            .child("accessRequests")
            .child(prosbaId)
            .updateChildren(
                mapOf(
                    "status" to "approved",
                    "approvedAt" to System.currentTimeMillis()
                )
            ).await()
    }

    suspend fun odrzucProsbe(context: Context, prosbaId: String) {
        val uid = PreferencjeParowania.uid(context).ifBlank { return }
        FirebaseSklepik.baza(context).reference
            .child(StaleFirebase.Sciezki.TRANSMITTERS)
            .child(uid)
            .child("accessRequests")
            .child(prosbaId)
            .updateChildren(
                mapOf(
                    "status" to "rejected",
                    "rejectedAt" to System.currentTimeMillis()
                )
            ).await()
    }

    suspend fun usunOdbiorce(context: Context, odbiorcaId: String) {
        val uid = PreferencjeParowania.uid(context).ifBlank { return }
        FirebaseSklepik.baza(context).reference
            .child(StaleFirebase.Sciezki.TRANSMITTERS)
            .child(uid)
            .child("receivers")
            .child(odbiorcaId)
            .removeValue()
            .await()
    }

    fun obserwujProsby(context: Context): Flow<List<ProsbaDostepu>> = callbackFlow {
        val uid = PreferencjeParowania.uid(context)
        if (uid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val ref = FirebaseSklepik.baza(context).reference
            .child(StaleFirebase.Sciezki.TRANSMITTERS)
            .child(uid)
            .child("accessRequests")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lista = snapshot.children.mapNotNull { dziecko ->
                    val id = dziecko.key ?: return@mapNotNull null
                    ProsbaDostepu(
                        id = id,
                        nazwaUrzadzenia = dziecko.child("deviceName").getValue(String::class.java)
                            ?: "Urządzenie",
                        createdAt = dziecko.child("createdAt").getValue(Long::class.java) ?: 0L,
                        status = dziecko.child("status").getValue(String::class.java) ?: "pending"
                    )
                }.sortedByDescending { it.createdAt }
                trySend(lista)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun obserwujOdbiorcow(context: Context): Flow<List<OdbiorcaPolaczony>> = callbackFlow {
        val uid = PreferencjeParowania.uid(context)
        if (uid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val ref = FirebaseSklepik.baza(context).reference
            .child(StaleFirebase.Sciezki.TRANSMITTERS)
            .child(uid)
            .child("receivers")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lista = snapshot.children.mapNotNull { dziecko ->
                    val id = dziecko.key ?: return@mapNotNull null
                    OdbiorcaPolaczony(
                        id = id,
                        nazwa = dziecko.child("deviceName").getValue(String::class.java)
                            ?: dziecko.child("name").getValue(String::class.java)
                            ?: "Urządzenie",
                        zablokowany = dziecko.child("blocked").getValue(Boolean::class.java) == true
                    )
                }
                trySend(lista)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
}
