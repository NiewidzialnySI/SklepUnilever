package shopunilever.dominik.pl.firebase

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import shopunilever.dominik.pl.dane.PreferencjeParowania

/**
 * Jeden zestaw listenerów RTDB dla UI pracownika (badge + zakładka Urządzenia).
 * Unika podwójnego ValueEventListener przy keep-alive zakładek.
 */
object RepozytoriumParowaniaUi {
    private val zakres = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null
    private var aktywnyKod: String? = null

    private val _prosby = MutableStateFlow<List<ProsbaDostepu>>(emptyList())
    private val _prosbyLogowania = MutableStateFlow<List<ProsbaLogowania>>(emptyList())
    private val _odbiorcy = MutableStateFlow<List<OdbiorcaPolaczony>>(emptyList())
    private val _badge = MutableStateFlow(0)

    val prosby: StateFlow<List<ProsbaDostepu>> = _prosby.asStateFlow()
    val prosbyLogowania: StateFlow<List<ProsbaLogowania>> = _prosbyLogowania.asStateFlow()
    val odbiorcy: StateFlow<List<OdbiorcaPolaczony>> = _odbiorcy.asStateFlow()
    val badgeOczekujace: StateFlow<Int> = _badge.asStateFlow()

    fun start(context: Context) {
        if (!PreferencjeParowania.czyAktywne(context)) {
            stop()
            return
        }
        val kod = PreferencjeParowania.kod(context)
        if (kod.isBlank()) {
            stop()
            return
        }
        if (job?.isActive == true && aktywnyKod == kod) return
        stop()
        aktywnyKod = kod
        val app = context.applicationContext
        job = zakres.launch {
            runCatching { SerwisParowania.upewnijSesjeNadajnika(app) }
            launch {
                SerwisParowania.obserwujProsby(app)
                    .distinctUntilChanged()
                    .collect { lista ->
                        _prosby.value = lista
                        odswiezBadge()
                    }
            }
            launch {
                SerwisParowania.obserwujProsbyLogowania(app)
                    .distinctUntilChanged()
                    .collect { lista ->
                        _prosbyLogowania.value = lista
                        odswiezBadge()
                    }
            }
            launch {
                SerwisParowania.obserwujOdbiorcow(app)
                    .distinctUntilChanged()
                    .collect { lista ->
                        _odbiorcy.value = lista
                    }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        aktywnyKod = null
        _prosby.value = emptyList()
        _prosbyLogowania.value = emptyList()
        _odbiorcy.value = emptyList()
        _badge.value = 0
    }

    fun restart(context: Context) {
        stop()
        start(context)
    }

    private fun odswiezBadge() {
        val p = _prosby.value.count { it.status == "pending" }
        val l = _prosbyLogowania.value.count { it.status == "pending" }
        _badge.value = p + l
    }
}
