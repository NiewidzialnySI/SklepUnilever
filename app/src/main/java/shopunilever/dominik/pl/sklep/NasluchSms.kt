package shopunilever.dominik.pl.sklep

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.provider.Telephony
import android.util.Log

/**
 * Odczyt kodu logowania z przychodzących SMS.
 * Broadcast RECEIVE_SMS + ContentObserver; polling skrzynki na wątku IO (nie UI).
 *
 * Tylko SMS nowsze niż [odKiedyMs] — bez „podbierania” starego kodu z inboxu.
 */
class NasluchSms(
    private val kontekst: Context,
    private val odKiedyMs: Long,
    private val onKod: (kod: String, otrzymanoMs: Long) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val watekIo = HandlerThread("NasluchSms-IO").apply { start() }
    private val ioHandler = Handler(watekIo.looper)
    private var aktywny = false
    private var ostatniZgloszony: String? = null

    private val odbiorca = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (!aktywny || intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
            val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
            val tresc = msgs.joinToString("") { it.messageBody.orEmpty() }
            val czas = msgs.maxOfOrNull { it.timestampMillis } ?: System.currentTimeMillis()
            if (czas < odKiedyMs) return
            wyciagnijKod(tresc)?.let { zglos(it, czas) }
        }
    }

    private val obserwator = object : ContentObserver(ioHandler) {
        override fun onChange(selfChange: Boolean) {
            if (aktywny) ioHandler.post { skanujSkrzynke() }
        }
    }

    private val polling = object : Runnable {
        override fun run() {
            if (!aktywny) return
            skanujSkrzynke()
            // Rzadszy poll — Broadcast + Observer pokrywają większość przypadków
            ioHandler.postDelayed(this, 8_000L)
        }
    }

    fun start() {
        if (aktywny) return
        aktywny = true
        try {
            val filtr = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                kontekst.registerReceiver(odbiorca, filtr, Context.RECEIVER_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                kontekst.registerReceiver(odbiorca, filtr)
            }
        } catch (e: Exception) {
            Log.w(TAG, "registerReceiver SMS: ${e.message}")
        }
        try {
            kontekst.contentResolver.registerContentObserver(
                Uri.parse("content://sms"),
                true,
                obserwator
            )
        } catch (e: Exception) {
            Log.w(TAG, "ContentObserver SMS: ${e.message}")
        }
        ioHandler.postDelayed(polling, 1_200L)
        ioHandler.post { skanujSkrzynke() }
    }

    fun stop() {
        if (!aktywny) return
        aktywny = false
        ioHandler.removeCallbacks(polling)
        try {
            kontekst.unregisterReceiver(odbiorca)
        } catch (_: Exception) {
        }
        try {
            kontekst.contentResolver.unregisterContentObserver(obserwator)
        } catch (_: Exception) {
        }
        watekIo.quitSafely()
    }

    private fun skanujSkrzynke() {
        try {
            val uri = Uri.parse("content://sms/inbox")
            kontekst.contentResolver.query(
                uri,
                arrayOf("body", "date"),
                "date >= ?",
                arrayOf(odKiedyMs.toString()),
                "date DESC"
            )?.use { c ->
                val iBody = c.getColumnIndex("body")
                val iDate = c.getColumnIndex("date")
                if (iBody < 0) return
                var odczytane = 0
                while (c.moveToNext() && odczytane < 12) {
                    odczytane++
                    val body = c.getString(iBody).orEmpty()
                    val data = if (iDate >= 0) c.getLong(iDate) else System.currentTimeMillis()
                    if (data < odKiedyMs) continue
                    wyciagnijKod(body)?.let {
                        zglos(it, data)
                        return
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "skanujSkrzynke: ${e.message}")
        }
    }

    private fun zglos(kod: String, otrzymanoMs: Long) {
        if (kod == ostatniZgloszony) return
        ostatniZgloszony = kod
        Log.i(TAG, "Odebrano kod SMS: $kod (ts=$otrzymanoMs)")
        BusSms.emituj(kod, otrzymanoMs)
        mainHandler.post { onKod(kod, otrzymanoMs) }
    }

    companion object {
        private const val TAG = "NasluchSms"

        fun wyciagnijKod(tresc: String): String? {
            val m = Regex("""\b(\d{4,8})\b""").find(tresc)
            return m?.groupValues?.getOrNull(1)
        }
    }
}
