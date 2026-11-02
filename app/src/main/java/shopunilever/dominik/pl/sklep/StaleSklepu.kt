package shopunilever.dominik.pl.sklep

object StaleSklepu {
    const val URL_SKLEPU = "https://sklepfirmowyunilever.pl"
    const val URL_LOGOWANIA = "$URL_SKLEPU/access-email"
    /** Po zalogowaniu — root sklepu. `/market` na żywej stronie kończy się 404. */
    const val URL_GLOWNA = "$URL_SKLEPU/"
    @Deprecated("Używaj URL_GLOWNA — /market nie istnieje na sklepie")
    const val URL_MARKET = URL_GLOWNA
    const val HOST_SKLEPU = "sklepfirmowyunilever.pl"

    fun czyUrlSklepu(url: String?): Boolean {
        val u = url?.lowercase().orEmpty()
        return u.contains(HOST_SKLEPU)
    }

    /** Magento po SMS: /customer/account/login?access-email=… — ekran hasła. */
    fun czyStronaHasla(url: String?): Boolean {
        val u = url?.lowercase().orEmpty()
        return u.contains("customer/account/login") ||
            (u.contains("/login") && u.contains("access-email"))
    }

    /** Bramka tożsamości (wybór metody / email / kod SMS). */
    fun czyBramkaAccess(url: String?): Boolean {
        val u = url?.lowercase().orEmpty()
        if (u.contains("customer/account/login")) return false
        return u.contains("/access") ||
            u.contains("access-email") ||
            u.contains("access-code") ||
            u.contains("validate-token-sms")
    }

    /** Strona z polem kodu SMS (żywy Magento: /validate-token-sms). */
    fun czyStronaKoduSms(url: String?): Boolean {
        val u = url?.lowercase().orEmpty()
        return u.contains("validate-token-sms") || u.contains("access-code")
    }

    fun czyGlownaSklepu(url: String?): Boolean {
        val u = url?.lowercase()?.substringBefore('?')?.trimEnd('/').orEmpty()
        return u == URL_SKLEPU.lowercase() ||
            u == "${URL_SKLEPU.lowercase()}/" ||
            u.endsWith("/access") ||
            u.endsWith("/access-email")
    }

    /** Czy URL wygląda na „martwą” stronę CMS / 404 sklepu. */
    fun czyPodejrzany404(url: String?): Boolean {
        val u = url?.lowercase().orEmpty()
        return u.contains("/market") ||
            u.contains("noroute") ||
            u.contains("page-not-found") ||
            u.contains("/404")
    }
}
