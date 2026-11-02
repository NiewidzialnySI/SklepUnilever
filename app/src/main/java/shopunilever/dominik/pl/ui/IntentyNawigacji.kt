package shopunilever.dominik.pl.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Jednorazowe sygnały nawigacji z powiadomień / deep-linków. */
object IntentyNawigacji {
    private val _otworzUrzadzenia = MutableStateFlow(false)
    val otworzUrzadzenia: StateFlow<Boolean> = _otworzUrzadzenia.asStateFlow()

    fun zadajOtworzUrzadzenia() {
        _otworzUrzadzenia.value = true
    }

    fun skonsumujOtworzUrzadzenia() {
        _otworzUrzadzenia.value = false
    }
}
