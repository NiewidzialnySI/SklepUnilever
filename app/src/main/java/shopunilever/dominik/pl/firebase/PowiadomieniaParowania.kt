package shopunilever.dominik.pl.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import shopunilever.dominik.pl.MainActivity
import shopunilever.dominik.pl.R

object PowiadomieniaParowania {
    private const val KANAL = "parowanie_prosby"
    private const val ID_BAZA = 7100

    fun upewnijKanal(context: Context) {
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return
        val kanal = NotificationChannel(
            KANAL,
            "Prośby o dostęp",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Ciche przypomnienie o oczekującej prośbie (bez dźwięku)"
            setShowBadge(true)
        }
        mgr.createNotificationChannel(kanal)
    }

    /** Ciche — bez dźwięku/wibracji, bez wymuszania otwarcia ekranu. */
    fun pokazCichePrzypomnienie(context: Context, nazwaUrzadzenia: String, prosbaId: String) {
        upewnijKanal(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("otworz_urzadzenia", true)
        }
        val pending = PendingIntent.getActivity(
            context,
            prosbaId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notyfikacja = NotificationCompat.Builder(context, KANAL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(nazwaUrzadzenia)
            .setContentText("Czeka na akceptację")
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        runCatching {
            NotificationManagerCompat.from(context)
                .notify(ID_BAZA + (prosbaId.hashCode() and 0xFF), notyfikacja)
        }
    }

    @Deprecated("Użyj pokazCichePrzypomnienie", ReplaceWith("pokazCichePrzypomnienie(context, nazwaUrzadzenia, prosbaId)"))
    fun pokazanowaProsbe(context: Context, nazwaUrzadzenia: String, prosbaId: String) {
        pokazCichePrzypomnienie(context, nazwaUrzadzenia, prosbaId)
    }
}
