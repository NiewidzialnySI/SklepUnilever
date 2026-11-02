package shopunilever.dominik.pl.sklep

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Wspólna magistrala kodów SMS — FGS i ekran sklepu nie muszą odpalać
 * dwóch niezależnych [NasluchSms].
 */
object BusSms {
    private val _kody = MutableSharedFlow<String>(extraBufferCapacity = 8, replay = 0)
    val kody: SharedFlow<String> = _kody.asSharedFlow()

    fun emituj(kod: String) {
        if (kod.length < 4) return
        _kody.tryEmit(kod)
    }
}
