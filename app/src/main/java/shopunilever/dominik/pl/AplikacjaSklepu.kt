package shopunilever.dominik.pl

import android.app.Application
import shopunilever.dominik.pl.firebase.FirebaseSklepik

class AplikacjaSklepu : Application() {
    override fun onCreate() {
        super.onCreate()
        // Wczesna inicjalizacja z projectId — zanim Auth/RTDB dostaną „Missing project id”
        runCatching { FirebaseSklepik.upewnijSie(this) }
    }
}
