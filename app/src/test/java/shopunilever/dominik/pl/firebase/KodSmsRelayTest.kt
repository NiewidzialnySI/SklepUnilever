package shopunilever.dominik.pl.firebase

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Reguła relay SMS dla rodziny/znajomych:
 * kod jest ważny tylko gdy jego ts jest nowszy niż kliknięcie „Zaloguj do sklepu”.
 */
class KodSmsRelayTest {

    @Test
    fun staryKodPracownikaJestOdrzucany() {
        val klikZaloguj = 1_000_000L
        val staryKodTs = klikZaloguj - 60_000L
        assertFalse(SerwisParowania.czyKodSmsNowszyNiz(staryKodTs, klikZaloguj))
    }

    @Test
    fun kodZTejSamejChwiliCoKlikJestOdrzucany() {
        val klikZaloguj = 1_000_000L
        assertFalse(SerwisParowania.czyKodSmsNowszyNiz(klikZaloguj, klikZaloguj))
    }

    @Test
    fun swiezyKodPoKliknieciuJestAkceptowany() {
        val klikZaloguj = 1_000_000L
        val nowyKodTs = klikZaloguj + 5_000L
        assertTrue(SerwisParowania.czyKodSmsNowszyNiz(nowyKodTs, klikZaloguj))
    }
}
