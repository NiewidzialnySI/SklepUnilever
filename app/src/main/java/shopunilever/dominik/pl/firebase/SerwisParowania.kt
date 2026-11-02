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
    val zablokowany: Boolean,
    val autoLogowanie: Boolean = true
)

data class ProsbaLogowania(
    val id: String,
    val nazwaUrzadzenia: String,
    val createdAt: Long,
    val status: String
)

data class LogWejsciaUrzadzenia(
    val id: String,
    val deviceId: String,
    val nazwa: String,
    val ts: Long
)

data class DaneLogowaniaZdalne(
    val email: String,
    val haslo: String
)

/** Wynik próby wejścia do sklepu z profilu rodziny. */
sealed class WynikWejsciaSklepu {
    data object MoznaWejsc : WynikWejsciaSklepu()
    data object CzekaNaAkceptacje : WynikWejsciaSklepu()
    data class Blad(val komunikat: String) : WynikWejsciaSklepu()
}

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
        runCatching {
            shopunilever.dominik.pl.tlo.SerwisTlaNadajnika.uruchomJesliTrzeba(context.applicationContext)
        }
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
                "blocked" to false,
                "autoLogowanie" to true
            )
        ).await()
        // Upewnij się, że rodzina ma świeże dane logowania
        runCatching { publikujDaneLogowania(context) }
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

    suspend fun zmienNazweOdbiorcy(context: Context, odbiorcaId: String, nazwa: String) {
        val uid = PreferencjeParowania.uid(context).ifBlank { return }
        val czysta = nazwa.trim().ifBlank { "Urządzenie" }
        FirebaseSklepik.baza(context).reference
            .child(StaleFirebase.Sciezki.TRANSMITTERS)
            .child(uid)
            .child("receivers")
            .child(odbiorcaId)
            .child("deviceName")
            .setValue(czysta)
            .await()
    }

    suspend fun ustawAutoLogowanie(context: Context, odbiorcaId: String, wlacz: Boolean) {
        val uid = PreferencjeParowania.uid(context).ifBlank { return }
        FirebaseSklepik.baza(context).reference
            .child(StaleFirebase.Sciezki.TRANSMITTERS)
            .child(uid)
            .child("receivers")
            .child(odbiorcaId)
            .child("autoLogowanie")
            .setValue(wlacz)
            .await()
    }

    suspend fun usunOdbiorce(context: Context, odbiorcaId: String) {
        val uid = PreferencjeParowania.uid(context).ifBlank { return }
        val ref = FirebaseSklepik.baza(context).reference
            .child(StaleFirebase.Sciezki.TRANSMITTERS)
            .child(uid)
        ref.child("receivers").child(odbiorcaId).removeValue().await()
        ref.child("accessRequests").child(odbiorcaId).removeValue().await()
        ref.child("loginRequests").child(odbiorcaId).removeValue().await()
    }

    /** Nowy kod QR — kasuje wszystkie sparowane urządzenia i prośby. */
    suspend fun usunWszystkichOdbiorcow(context: Context) {
        val uid = PreferencjeParowania.uid(context).ifBlank { return }
        val ref = FirebaseSklepik.baza(context).reference
            .child(StaleFirebase.Sciezki.TRANSMITTERS)
            .child(uid)
        ref.child("receivers").removeValue().await()
        ref.child("accessRequests").removeValue().await()
        ref.child("loginRequests").removeValue().await()
    }

    /** Rodzina: zapis wejścia do sklepu (bez urządzenia pracownika). */
    suspend fun zapiszLogWejscia(context: Context) {
        val tx = PreferencjeOdbiorcy.uidNadajnika(context).ifBlank { return }
        val uid = PreferencjeOdbiorcy.uid(context).ifBlank { return }
        // Nadajnik nie loguje siebie — ta ścieżka jest tylko dla odbiorcy.
        if (PreferencjeParowania.czyAktywne(context) && PreferencjeParowania.uid(context) == uid) return
        val nazwa = PreferencjeOdbiorcy.nazwaUrzadzenia(context).ifBlank {
            Build.MODEL.ifBlank { "Urządzenie" }
        }
        FirebaseSklepik.baza(context).reference
            .child(StaleFirebase.Sciezki.TRANSMITTERS)
            .child(tx)
            .child("loginLogs")
            .push()
            .setValue(
                mapOf(
                    "deviceId" to uid,
                    "deviceName" to nazwa,
                    "ts" to System.currentTimeMillis()
                )
            ).await()
    }

    fun obserwujLogiWejsc(context: Context): Flow<List<LogWejsciaUrzadzenia>> = callbackFlow {
        val uid = PreferencjeParowania.uid(context)
        if (uid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val ref = FirebaseSklepik.baza(context).reference
            .child(StaleFirebase.Sciezki.TRANSMITTERS)
            .child(uid)
            .child("loginLogs")
            .orderByChild("ts")
            .limitToLast(40)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lista = snapshot.children.mapNotNull { dziecko ->
                    val id = dziecko.key ?: return@mapNotNull null
                    val deviceId = dziecko.child("deviceId").getValue(String::class.java).orEmpty()
                    // Pomiń ewentualne wpisy samego nadajnika
                    if (deviceId.isNotBlank() && deviceId == uid) return@mapNotNull null
                    LogWejsciaUrzadzenia(
                        id = id,
                        deviceId = deviceId,
                        nazwa = dziecko.child("deviceName").getValue(String::class.java)
                            ?: "Urządzenie",
                        ts = dziecko.child("ts").getValue(Long::class.java) ?: 0L
                    )
                }.sortedByDescending { it.ts }
                trySend(lista)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun wyczyscLogiWejsc(context: Context) {
        val uid = PreferencjeParowania.uid(context).ifBlank { return }
        FirebaseSklepik.baza(context).reference
            .child(StaleFirebase.Sciezki.TRANSMITTERS)
            .child(uid)
            .child("loginLogs")
            .removeValue()
            .await()
    }

    /**
     * Rodzina chce wejść do sklepu.
     * autoLogowanie=true → od razu; false → prośba do pracownika.
     */
    suspend fun przygotujWejscieDoSklepu(context: Context): WynikWejsciaSklepu {
        val tx = PreferencjeOdbiorcy.uidNadajnika(context).ifBlank {
            return WynikWejsciaSklepu.Blad("Brak połączenia z pracownikiem")
        }
        val uid = PreferencjeOdbiorcy.uid(context).ifBlank {
            return WynikWejsciaSklepu.Blad("Brak identyfikatora urządzenia")
        }
        val refOdb = FirebaseSklepik.baza(context).reference
            .child(StaleFirebase.Sciezki.TRANSMITTERS)
            .child(tx)
            .child("receivers")
            .child(uid)
        val snap = runCatching { refOdb.get().await() }.getOrNull()
            ?: return WynikWejsciaSklepu.Blad("Brak dostępu — zeskanuj nowy kod")
        if (!snap.exists() || snap.child("blocked").getValue(Boolean::class.java) == true) {
            return WynikWejsciaSklepu.Blad("Urządzenie odłączone — zeskanuj nowy kod")
        }
        val auto = snap.child("autoLogowanie").getValue(Boolean::class.java) ?: true
        if (auto) return WynikWejsciaSklepu.MoznaWejsc

        val nazwa = PreferencjeOdbiorcy.nazwaUrzadzenia(context).ifBlank {
            snap.child("deviceName").getValue(String::class.java) ?: "Urządzenie rodziny"
        }
        FirebaseSklepik.baza(context).reference
            .child(StaleFirebase.Sciezki.TRANSMITTERS)
            .child(tx)
            .child("loginRequests")
            .child(uid)
            .setValue(
                mapOf(
                    "deviceName" to nazwa,
                    "status" to "pending",
                    "createdAt" to System.currentTimeMillis()
                )
            ).await()
        return WynikWejsciaSklepu.CzekaNaAkceptacje
    }

    suspend fun zatwierdzLogowanie(context: Context, odbiorcaId: String) {
        val uid = PreferencjeParowania.uid(context).ifBlank { return }
        FirebaseSklepik.baza(context).reference
            .child(StaleFirebase.Sciezki.TRANSMITTERS)
            .child(uid)
            .child("loginRequests")
            .child(odbiorcaId)
            .updateChildren(
                mapOf(
                    "status" to "approved",
                    "approvedAt" to System.currentTimeMillis()
                )
            ).await()
        runCatching { publikujDaneLogowania(context) }
    }

    suspend fun odrzucLogowanie(context: Context, odbiorcaId: String) {
        val uid = PreferencjeParowania.uid(context).ifBlank { return }
        FirebaseSklepik.baza(context).reference
            .child(StaleFirebase.Sciezki.TRANSMITTERS)
            .child(uid)
            .child("loginRequests")
            .child(odbiorcaId)
            .updateChildren(
                mapOf(
                    "status" to "rejected",
                    "rejectedAt" to System.currentTimeMillis()
                )
            ).await()
    }

    /** Po wejściu do sklepu — jednorazowa zgoda zużyta. */
    suspend fun zuzyjZgodeLogowania(context: Context) {
        val tx = PreferencjeOdbiorcy.uidNadajnika(context).ifBlank { return }
        val uid = PreferencjeOdbiorcy.uid(context).ifBlank { return }
        FirebaseSklepik.baza(context).reference
            .child(StaleFirebase.Sciezki.TRANSMITTERS)
            .child(tx)
            .child("loginRequests")
            .child(uid)
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
                        zablokowany = dziecko.child("blocked").getValue(Boolean::class.java) == true,
                        autoLogowanie = dziecko.child("autoLogowanie").getValue(Boolean::class.java) ?: true
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

    fun obserwujProsbyLogowania(context: Context): Flow<List<ProsbaLogowania>> = callbackFlow {
        val uid = PreferencjeParowania.uid(context)
        if (uid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val ref = FirebaseSklepik.baza(context).reference
            .child(StaleFirebase.Sciezki.TRANSMITTERS)
            .child(uid)
            .child("loginRequests")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lista = snapshot.children.mapNotNull { dziecko ->
                    val id = dziecko.key ?: return@mapNotNull null
                    ProsbaLogowania(
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

    /** Status prośby o logowanie (rodzina, gdy autoLogowanie=false). */
    fun obserwujStatusLogowania(context: Context): Flow<String> = callbackFlow {
        val tx = PreferencjeOdbiorcy.uidNadajnika(context)
        val uid = PreferencjeOdbiorcy.uid(context)
        if (tx.isBlank() || uid.isBlank()) {
            trySend("brak")
            close()
            return@callbackFlow
        }
        val ref = FirebaseSklepik.baza(context).reference
            .child(StaleFirebase.Sciezki.TRANSMITTERS)
            .child(tx)
            .child("loginRequests")
            .child(uid)
            .child("status")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(String::class.java) ?: "brak")
            }

            override fun onCancelled(error: DatabaseError) {
                trySend("brak")
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /**
     * Status dostępu rodziny.
     * Gdy pracownik kliknie „Odłącz”, węzły znikają — to NIE jest pending, tylko „revoked”.
     */
    fun obserwujStatusProsby(context: Context): Flow<String> = callbackFlow {
        val tx = PreferencjeOdbiorcy.uidNadajnika(context)
        val uid = PreferencjeOdbiorcy.uid(context)
        if (tx.isBlank() || uid.isBlank()) {
            trySend(PreferencjeOdbiorcy.status(context).ifBlank { "brak" })
            close()
            return@callbackFlow
        }
        val baza = FirebaseSklepik.baza(context).reference
            .child(StaleFirebase.Sciezki.TRANSMITTERS)
            .child(tx)
        val refProsba = baza.child("accessRequests").child(uid)
        val refOdbiorca = baza.child("receivers").child(uid)

        var snapProsba: DataSnapshot? = null
        var snapOdbiorca: DataSnapshot? = null
        var mamProsbe = false
        var mamOdbiorce = false

        fun przelicz() {
            if (!mamProsbe || !mamOdbiorce) return
            val prosba = snapProsba
            val odbiorca = snapOdbiorca
            val stProsby = prosba?.child("status")?.getValue(String::class.java)
            val istniejeOdbiorca = odbiorca?.exists() == true
            val zablokowany = odbiorca?.child("blocked")?.getValue(Boolean::class.java) == true

            val wynik = when {
                istniejeOdbiorca && !zablokowany -> "approved"
                istniejeOdbiorca && zablokowany -> "revoked"
                stProsby == "pending" -> "pending"
                stProsby == "rejected" -> "rejected"
                stProsby == "approved" && !istniejeOdbiorca -> "revoked"
                prosba?.exists() != true -> {
                    // Usunięte przez pracownika — nie mylić z „pending”
                    val lokalny = PreferencjeOdbiorcy.status(context)
                    if (lokalny == "approved" || lokalny == "pending" || lokalny == "rejected") {
                        "revoked"
                    } else {
                        "brak"
                    }
                }
                else -> stProsby ?: "brak"
            }
            PreferencjeOdbiorcy.ustawStatus(context, wynik)
            trySend(wynik)
        }

        val listenerProsba = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapProsba = snapshot
                mamProsbe = true
                przelicz()
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(PreferencjeOdbiorcy.status(context))
            }
        }
        val listenerOdbiorca = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapOdbiorca = snapshot
                mamOdbiorce = true
                przelicz()
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(PreferencjeOdbiorcy.status(context))
            }
        }
        refProsba.addValueEventListener(listenerProsba)
        refOdbiorca.addValueEventListener(listenerOdbiorca)
        awaitClose {
            refProsba.removeEventListener(listenerProsba)
            refOdbiorca.removeEventListener(listenerOdbiorca)
        }
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
