package shopunilever.dominik.pl.firebase

import android.content.Context
import android.os.Build
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import shopunilever.dominik.pl.dane.PreferencjeKonta
import shopunilever.dominik.pl.dane.PreferencjeOdbiorcy
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

data class DaneLogowaniaZdalne(
    val email: String,
    val haslo: String
)

/**
 * Nadajnik (pracownik) + odbiorca (rodzina): kod, credentials, SMS relay.
 */
object SerwisParowania {

    suspend fun uruchomJakoNadajnik(context: Context): String {
        FirebaseSklepik.upewnijSie(context)
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

    /**
     * Utrzymuje sesję Auth = UID nadajnika z preferencji.
     * Bez tego reguły RTDB blokują odczyt próśb (auth.uid musi = transmitterUid).
     */
    suspend fun upewnijSesjeNadajnika(context: Context): String? {
        val kod = PreferencjeParowania.kod(context)
        if (kod.isBlank()) return null
        FirebaseSklepik.upewnijSie(context)
        val authUid = FirebaseSklepik.zalogujAnonimowo(context)
        val zapisany = PreferencjeParowania.uid(context)
        if (zapisany.isBlank() || zapisany != authUid) {
            return uruchomJakoNadajnik(context)
        }
        // Odśwież znacznik aktywności — bez zmiany kodu.
        FirebaseSklepik.baza(context).reference
            .child(StaleFirebase.Sciezki.TRANSMITTERS)
            .child(authUid)
            .child("updatedAt")
            .setValue(System.currentTimeMillis())
            .await()
        PreferencjeParowania.zapisz(context, kod, authUid)
        return authUid
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

    /**
     * Rodzina: zeskanuj / wpisz kod → prośba o dostęp u nadajnika.
     */
    suspend fun dolaczJakoOdbiorca(context: Context, surowyKod: String): String {
        FirebaseSklepik.upewnijSie(context)
        val kod = wyodrebnijKodZQr(surowyKod)
        require(kod.length == StaleFirebase.DLUGOSC_KODU) {
            "Kod powinien mieć ${StaleFirebase.DLUGOSC_KODU} znaków"
        }

        val uid = FirebaseSklepik.zalogujAnonimowo(context)
        val snap = FirebaseSklepik.baza(context).reference
            .child(StaleFirebase.Sciezki.PAIRING_CODES)
            .child(kod)
            .get()
            .await()
        val txUid = snap.child("transmitterUid").getValue(String::class.java)
            ?: error("Nie znaleziono kodu parowania — poproś pracownika o nowy QR")

        val nazwa = Build.MODEL.ifBlank { "Telefon rodziny" }
        FirebaseSklepik.baza(context).reference
            .child(StaleFirebase.Sciezki.TRANSMITTERS)
            .child(txUid)
            .child("accessRequests")
            .child(uid)
            .setValue(
                mapOf(
                    "deviceName" to nazwa,
                    "requesterUid" to uid,
                    "status" to "pending",
                    "createdAt" to System.currentTimeMillis(),
                    "syncCode" to kod
                )
            ).await()

        PreferencjeOdbiorcy.zapiszProsbe(context, kod, uid, txUid, nazwa)
        return kod
    }

    suspend fun pobierzDaneLogowania(context: Context): DaneLogowaniaZdalne? {
        val tx = PreferencjeOdbiorcy.uidNadajnika(context).ifBlank { return null }
        val snap = FirebaseSklepik.baza(context).reference
            .child(StaleFirebase.Sciezki.TRANSMITTERS)
            .child(tx)
            .child("credentials")
            .get()
            .await()
        val email = snap.child("email").getValue(String::class.java).orEmpty()
        val haslo = snap.child("haslo").getValue(String::class.java).orEmpty()
        if (email.isBlank() || haslo.isBlank()) return null
        return DaneLogowaniaZdalne(email, haslo)
    }

    suspend fun zatwierdzProsbe(context: Context, prosbaId: String) {
        val uid = PreferencjeParowania.uid(context).ifBlank { return }
        val ref = FirebaseSklepik.baza(context).reference
            .child(StaleFirebase.Sciezki.TRANSMITTERS)
            .child(uid)
        ref.child("accessRequests").child(prosbaId).updateChildren(
            mapOf(
                "status" to "approved",
                "approvedAt" to System.currentTimeMillis()
            )
        ).await()

        val snap = ref.child("accessRequests").child(prosbaId).get().await()
        val nazwa = snap.child("deviceName").getValue(String::class.java) ?: "Urządzenie"
        ref.child("receivers").child(prosbaId).setValue(
            mapOf(
                "deviceName" to nazwa,
                "approvedAt" to System.currentTimeMillis(),
                "blocked" to false
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
        val ref = FirebaseSklepik.baza(context).reference
            .child(StaleFirebase.Sciezki.TRANSMITTERS)
            .child(uid)
        ref.child("receivers").child(odbiorcaId).removeValue().await()
        ref.child("accessRequests").child(odbiorcaId).removeValue().await()
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

    /** Status własnej prośby (rodzina). */
    fun obserwujStatusProsby(context: Context): Flow<String> = callbackFlow {
        val tx = PreferencjeOdbiorcy.uidNadajnika(context)
        val uid = PreferencjeOdbiorcy.uid(context)
        if (tx.isBlank() || uid.isBlank()) {
            trySend(PreferencjeOdbiorcy.status(context).ifBlank { "brak" })
            close()
            return@callbackFlow
        }
        val ref = FirebaseSklepik.baza(context).reference
            .child(StaleFirebase.Sciezki.TRANSMITTERS)
            .child(tx)
            .child("accessRequests")
            .child(uid)
            .child("status")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val st = snapshot.getValue(String::class.java) ?: "pending"
                PreferencjeOdbiorcy.ustawStatus(context, st)
                trySend(st)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(PreferencjeOdbiorcy.status(context))
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /** SMS relay z telefonu pracownika → rodzina. */
    fun obserwujKodSms(context: Context): Flow<String> = callbackFlow {
        val tx = PreferencjeOdbiorcy.uidNadajnika(context)
        if (tx.isBlank()) {
            close()
            return@callbackFlow
        }
        val ref = FirebaseSklepik.baza(context).reference
            .child(StaleFirebase.Sciezki.TRANSMITTERS)
            .child(tx)
            .child("smsRelay")
        var ostatniTs = 0L
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val kod = snapshot.child("kod").getValue(String::class.java).orEmpty()
                val ts = snapshot.child("ts").getValue(Long::class.java) ?: 0L
                if (kod.length >= 4 && ts > ostatniTs) {
                    ostatniTs = ts
                    trySend(kod)
                }
            }

            override fun onCancelled(error: DatabaseError) = Unit
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun wyodrebnijKodZQr(tresc: String): String {
        val raw = tresc.trim()
        val bezPrefiksu = when {
            raw.contains(':') -> raw.substringAfterLast(':')
            else -> raw
        }
        return PreferencjeParowania.normalizuj(bezPrefiksu)
            .take(StaleFirebase.DLUGOSC_KODU)
    }
}
