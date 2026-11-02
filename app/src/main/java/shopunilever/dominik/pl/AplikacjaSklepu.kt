package shopunilever.dominik.pl

import android.app.Application
import shopunilever.dominik.pl.firebase.FirebaseSklepik
import shopunilever.dominik.pl.firebase.PowiadomieniaParowania

class AplikacjaSklepu : Application() {
    override fun onCreate() {
        super.onCreate()
        // Wczesna inicjalizacja z projectId — zanim Auth/RTDB dostaną „Missing project id”
        runCatching { FirebaseSklepik.upewnijSie(this) }
        runCatching { PowiadomieniaParowania.upewnijKanal(this) }
    }
}
