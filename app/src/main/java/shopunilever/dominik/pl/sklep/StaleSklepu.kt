package shopunilever.dominik.pl.sklep

object StaleSklepu {
    const val URL_SKLEPU = "https://sklepfirmowyunilever.pl"
    const val URL_LOGOWANIA = "$URL_SKLEPU/access-email"
    const val URL_MARKET = "$URL_SKLEPU/market"
    const val HOST_SKLEPU = "sklepfirmowyunilever.pl"

    fun czyUrlSklepu(url: String?): Boolean {
        val u = url?.lowercase().orEmpty()
        return u.contains(HOST_SKLEPU)
    }

    fun czyGlownaSklepu(url: String?): Boolean {
        val u = url?.lowercase()?.substringBefore('?')?.trimEnd('/').orEmpty()
        return u == URL_SKLEPU.lowercase() ||
            u == URL_MARKET.lowercase() ||
            u == "${URL_SKLEPU.lowercase()}/" ||
            u.endsWith("/market")
    }
}
