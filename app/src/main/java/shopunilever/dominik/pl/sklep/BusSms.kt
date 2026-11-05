package shopunilever.dominik.pl.sklep

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Kod SMS z czasem otrzymania wiadomości (nie czas publikacji). */
data class KodSmsOdebrany(
    val kod: String,
    val otrzymanoMs: Long
)

/**
 * Wspólna magistrala kodów SMS — FGS i ekran sklepu nie muszą odpalać
 * dwóch niezależnych [NasluchSms].
 */
object BusSms {
    private val _kody = MutableSharedFlow<KodSmsOdebrany>(extraBufferCapacity = 8, replay = 0)
    val kody: SharedFlow<KodSmsOdebrany> = _kody.asSharedFlow()

    fun emituj(kod: String, otrzymanoMs: Long = System.currentTimeMillis()) {
        if (kod.length < 4) return
        _kody.tryEmit(KodSmsOdebrany(kod, otrzymanoMs))
    }
}
