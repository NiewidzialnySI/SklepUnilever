package shopunilever.dominik.pl.sklep

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StaleSklepuTest {

    @Test
    fun bramkaPayU_jestRozpoznawana() {
        assertTrue(
            StaleSklepu.czyUrlBramkiPlatnosci(
                "https://secure.payu.com/pay/?orderId=ABC"
            )
        )
        assertTrue(
            StaleSklepu.czyUrlBramkiPlatnosci(
                "https://www.payu.pl/payment"
            )
        )
    }

    @Test
    fun domenaSklepu_nieJestBramka() {
        assertFalse(
            StaleSklepu.czyUrlBramkiPlatnosci(
                "https://sklepfirmowyunilever.pl/checkout/cart/"
            )
        )
    }

    @Test
    fun zaplac_nieWracaNaGlowna_dlaPayU() {
        assertTrue(
            StaleSklepu.czyDozwolonaNawigacja(
                "https://secure.payu.com/pay/?orderId=1",
                sesjaPlatnosci = false
            )
        )
    }

    @Test
    fun bankWSesjiPlatnosci_jestDozwolony() {
        assertTrue(
            StaleSklepu.czyDozwolonaNawigacja(
                "https://login.bank.pl/blik/confirm",
                sesjaPlatnosci = true
            )
        )
        assertFalse(
            StaleSklepu.czyDozwolonaNawigacja(
                "https://login.bank.pl/blik/confirm",
                sesjaPlatnosci = false
            )
        )
    }

    @Test
    fun obcyLinkPozaPlatnoscia_jestZablokowany() {
        assertFalse(
            StaleSklepu.czyDozwolonaNawigacja(
                "https://reklama.example.com/promo",
                sesjaPlatnosci = false
            )
        )
    }
}
