package shopunilever.dominik.pl.firebase

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

/**
 * Własna instancja Firebase projektu sklepunilever-app
 * (inicjalizacja ręczna, bez google-services plugin).
 */
object FirebaseSklepik {

    @Synchronized
    fun upewnijSie(context: Context): FirebaseApp {
        val istniejaca = FirebaseApp.getApps(context).firstOrNull {
            it.name == StaleFirebase.NAZWA_APKI
        }
        if (istniejaca != null) return istniejaca

        val opcje = FirebaseOptions.Builder()
            .setApiKey(StaleFirebase.API_KEY_ANDROID)
            .setApplicationId(StaleFirebase.APP_ID_ANDROID)
            .setProjectId(StaleFirebase.PROJECT_ID)
            .setDatabaseUrl(StaleFirebase.DATABASE_URL)
            .setStorageBucket(StaleFirebase.STORAGE_BUCKET)
            .setGcmSenderId(StaleFirebase.MESSAGING_SENDER_ID)
            .build()

        return FirebaseApp.initializeApp(context, opcje, StaleFirebase.NAZWA_APKI)
            ?: error("Nie udało się zainicjalizować Firebase sklepik")
    }

    fun auth(context: Context): FirebaseAuth =
        FirebaseAuth.getInstance(upewnijSie(context))

    fun baza(context: Context): FirebaseDatabase =
        FirebaseDatabase.getInstance(upewnijSie(context))

    suspend fun zalogujAnonimowo(context: Context): String {
        val a = auth(context)
        a.currentUser?.uid?.let { return it }
        val wynik = a.signInAnonymously().await()
        return wynik.user?.uid ?: error("Brak UID po logowaniu anonimowym")
    }
}
