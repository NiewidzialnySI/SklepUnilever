package shopunilever.dominik.pl.firebase

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

/**
 * Firebase projektu sklepunilever-app.
 * Preferuje domyślną instancję (google-services.json + plugin),
 * a przy braku — inicjalizuje ręcznie z projectId.
 */
object FirebaseSklepik {

    @Synchronized
    fun upewnijSie(context: Context): FirebaseApp {
        val appContext = context.applicationContext

        // 1) Domyślna app z google-services / wcześniejszej inicjalizacji
        FirebaseApp.getApps(appContext).firstOrNull {
            it.name == FirebaseApp.DEFAULT_APP_NAME
        }?.let { return it }

        // 2) Ręczna inicjalizacja DEFAULT z pełnymi opcjami (w tym projectId)
        val opcje = FirebaseOptions.Builder()
            .setApiKey(StaleFirebase.API_KEY_ANDROID)
            .setApplicationId(StaleFirebase.APP_ID_ANDROID)
            .setProjectId(StaleFirebase.PROJECT_ID)
            .setDatabaseUrl(StaleFirebase.DATABASE_URL)
            .setStorageBucket(StaleFirebase.STORAGE_BUCKET)
            .setGcmSenderId(StaleFirebase.MESSAGING_SENDER_ID)
            .build()

        return try {
            FirebaseApp.initializeApp(appContext, opcje)
                ?: FirebaseApp.getInstance()
        } catch (_: IllegalStateException) {
            FirebaseApp.getInstance()
        }
    }

    fun auth(context: Context): FirebaseAuth =
        FirebaseAuth.getInstance(upewnijSie(context))

    fun baza(context: Context): FirebaseDatabase =
        FirebaseDatabase.getInstance(upewnijSie(context), StaleFirebase.DATABASE_URL)

    suspend fun zalogujAnonimowo(context: Context): String {
        upewnijSie(context)
        val a = auth(context)
        a.currentUser?.uid?.let { return it }
        val wynik = a.signInAnonymously().await()
        return wynik.user?.uid
            ?: error("Brak UID po logowaniu anonimowym")
    }
}
