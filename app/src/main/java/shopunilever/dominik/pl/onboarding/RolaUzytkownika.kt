package shopunilever.dominik.pl.onboarding

/**
 * Wybór przy pierwszym uruchomieniu — determinuje pakiet uprawnień.
 */
enum class RolaUzytkownika(val klucz: String, val etykieta: String, val opis: String) {
    PRACOWNIK(
        klucz = "pracownik",
        etykieta = "Pracownik",
        opis = "Pełny dostęp do sklepu firmowego, auto-logowanie i synchronizacja SMS."
    ),
    RODZINA(
        klucz = "rodzina",
        etykieta = "Rodzina / znajomi",
        opis = "Dostęp gościa — zakupy i powiadomienia bez uprawnień służbowych."
    );

    companion object {
        fun zKlucza(klucz: String?): RolaUzytkownika? =
            entries.firstOrNull { it.klucz == klucz }
    }
}
