package shopunilever.dominik.pl.sklep

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.util.Log

/**
 * Odczyt kodu logowania z przychodzących SMS (jak useSklepikSms w Harmonogramie).
 * Broadcast RECEIVE_SMS + polling skrzynki przez ContentResolver.
 */
class NasluchSms(
    private val kontekst: Context,
    private val odKiedyMs: Long,
    private val onKod: (String) -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private var aktywny = false
    private var ostatniZgloszony: String? = null

    private val odbiorca = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (!aktywny || intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
            val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
            val tresc = msgs.joinToString("") { it.messageBody.orEmpty() }
            val czas = msgs.maxOfOrNull { it.timestampMillis } ?: System.currentTimeMillis()
            if (czas < odKiedyMs - 5_000L) return
            wyciagnijKod(tresc)?.let { zglos(it) }
        }
    }

    private val obserwator = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            if (aktywny) skanujSkrzynke()
        }
    }

    private val polling = object : Runnable {
        override fun run() {
            if (!aktywny) return
            skanujSkrzynke()
            handler.postDelayed(this, 2_000L)
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
        handler.postDelayed(polling, 800L)
        skanujSkrzynke()
    }

    fun stop() {
        if (!aktywny) return
        aktywny = false
        handler.removeCallbacks(polling)
        try {
            kontekst.unregisterReceiver(odbiorca)
        } catch (_: Exception) {
        }
        try {
            kontekst.contentResolver.unregisterContentObserver(obserwator)
        } catch (_: Exception) {
        }
    }

    private fun skanujSkrzynke() {
        try {
            val uri = Uri.parse("content://sms/inbox")
            kontekst.contentResolver.query(
                uri,
                arrayOf("body", "date"),
                "date >= ?",
                arrayOf((odKiedyMs - 5_000L).toString()),
                "date DESC"
            )?.use { c ->
                val iBody = c.getColumnIndex("body")
                if (iBody < 0) return
                var odczytane = 0
                while (c.moveToNext() && odczytane < 12) {
                    odczytane++
                    val body = c.getString(iBody).orEmpty()
                    wyciagnijKod(body)?.let {
                        zglos(it)
                        return
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "skanujSkrzynke: ${e.message}")
        }
    }

    private fun zglos(kod: String) {
        if (kod == ostatniZgloszony) return
        ostatniZgloszony = kod
        Log.i(TAG, "Odebrano kod SMS: $kod")
        handler.post { onKod(kod) }
    }

    companion object {
        private const val TAG = "NasluchSms"

        fun wyciagnijKod(tresc: String): String? {
            val m = Regex("""\b(\d{4,8})\b""").find(tresc)
            return m?.groupValues?.getOrNull(1)
        }
    }
}
