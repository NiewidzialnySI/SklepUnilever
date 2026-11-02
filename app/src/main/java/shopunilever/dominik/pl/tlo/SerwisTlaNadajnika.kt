package shopunilever.dominik.pl.tlo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import shopunilever.dominik.pl.MainActivity
import shopunilever.dominik.pl.R
import shopunilever.dominik.pl.dane.PreferencjeParowania
import shopunilever.dominik.pl.firebase.SerwisParowania
import shopunilever.dominik.pl.sklep.NasluchSms

/**
 * Tło pracownika przy aktywnym parowaniu:
 * nasłuch SMS → publikacja kodu do Firebase (rodzina loguje się bez otwierania apki).
 * Cicha, stała notyfikacja (wymóg FGS) — bez spamowania o każdej prośbie.
 */
class SerwisTlaNadajnika : Service() {

    private val zakres = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var nasluchSms: NasluchSms? = null
    private var ostatniKod: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == AKCJA_STOP || !PreferencjeParowania.czyAktywne(this)) {
            stopSelfSafe()
            return START_NOT_STICKY
        }

        startForegroundBezpiecznie(zbudujNotyfikacje())

        zakres.launch {
            runCatching { SerwisParowania.upewnijSesjeNadajnika(this@SerwisTlaNadajnika) }
        }

        if (nasluchSms == null) {
            val od = System.currentTimeMillis() - 15_000L
            nasluchSms = NasluchSms(
                kontekst = applicationContext,
                odKiedyMs = od,
                onKod = { kod -> naKodSms(kod) }
            ).also { it.start() }
            Log.i(TAG, "Nasłuch SMS w tle uruchomiony")
        }

        return START_STICKY
    }

    private fun naKodSms(kod: String) {
        if (kod.length < 4 || kod == ostatniKod) return
        ostatniKod = kod
        zakres.launch {
            runCatching {
                SerwisParowania.publikujKodSms(applicationContext, kod)
                Log.i(TAG, "Opublikowano kod SMS do relay")
            }.onFailure {
                Log.w(TAG, "Publikacja SMS: ${it.message}")
            }
        }
    }

    private fun startForegroundBezpiecznie(notyfikacja: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                ID_NOTYFIKACJI,
                notyfikacja,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(ID_NOTYFIKACJI, notyfikacja)
        } else {
            @Suppress("DEPRECATION")
            startForeground(ID_NOTYFIKACJI, notyfikacja)
        }
    }

    private fun zbudujNotyfikacje(): Notification {
        upewnijKanal(this)
        val pending = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, KANAL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Parowanie")
            .setContentText("SMS w tle")
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(pending)
            .build()
    }

    private fun stopSelfSafe() {
        nasluchSms?.stop()
        nasluchSms = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        nasluchSms?.stop()
        nasluchSms = null
        zakres.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "SerwisTlaNadajnika"
        private const val KANAL = "parowanie_tlo"
        private const val ID_NOTYFIKACJI = 7201
        private const val AKCJA_STOP = "shopunilever.STOP_TLO_NADAJNIKA"

        fun upewnijKanal(context: Context) {
            val mgr = context.getSystemService(NotificationManager::class.java) ?: return
            val kanal = NotificationChannel(
                KANAL,
                "Parowanie w tle",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Cicha usługa przekazywania kodów SMS"
                setShowBadge(false)
            }
            mgr.createNotificationChannel(kanal)
        }

        fun uruchomJesliTrzeba(context: Context) {
            if (!PreferencjeParowania.czyAktywne(context)) {
                zatrzymaj(context)
                return
            }
            upewnijKanal(context)
            val intent = Intent(context, SerwisTlaNadajnika::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun zatrzymaj(context: Context) {
            val intent = Intent(context, SerwisTlaNadajnika::class.java).apply {
                action = AKCJA_STOP
            }
            runCatching { context.startService(intent) }
            runCatching { context.stopService(Intent(context, SerwisTlaNadajnika::class.java)) }
        }
    }
}
