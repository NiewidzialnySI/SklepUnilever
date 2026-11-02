package shopunilever.dominik.pl.update

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import shopunilever.dominik.pl.BuildConfig
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Autoaktualizacja (jak SelfUpdatingApp / hermes-mobile / riffle):
 * start aplikacji → update.json → okno → jeden przycisk → pobierz APK →
 * systemowy chooser instalatorów (Package Installer / menedżer plików / …).
 *
 * Działa niezależnie od roli (pracownik / rodzina).
 */
object AktualizacjaAplikacji {

    const val DOMYSLNY_MANIFEST =
        "https://raw.githubusercontent.com/NiewidzialnySI/SklepUnilever/main/update.json"

    private const val PREFS = "aktualizacja_prefs"
    private const val K_AUTO = "auto_sprawdzaj"
    private const val K_TOKEN = "github_token"

    data class InfoWersji(
        val versionCode: Int,
        val versionName: String,
        val apkUrl: String,
        val changelog: String
    )

    sealed class Stan {
        data object Bezczynny : Stan()
        data object Sprawdzanie : Stan()
        data class Dostepna(val info: InfoWersji) : Stan()
        data object Aktualna : Stan()
        data class Pobieranie(val postep01: Float, val info: InfoWersji) : Stan()
        data class GotowaDoInstalacji(val plik: File, val info: InfoWersji) : Stan()
        data class WymagaUprawnienia(val plik: File?, val info: InfoWersji) : Stan()
        data class Blad(val komunikat: String) : Stan()
    }

    private const val NAZWA_APK_POBRANE = "sklep-unilever-update.apk"

    private val _stan = MutableStateFlow<Stan>(Stan.Bezczynny)
    val stan: StateFlow<Stan> = _stan.asStateFlow()
    private val mutex = Mutex()
    @Volatile private var ostatnieSprawdzenieMs = 0L

    fun lokalnaWersja(): String =
        "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"

    fun autoSprawdzaj(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(K_AUTO, true)

    fun ustawAutoSprawdzaj(context: Context, wlacz: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putBoolean(K_AUTO, wlacz)
        }
    }

    fun githubToken(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(K_TOKEN, "")?.trim().orEmpty()

    fun manifestUrl(): String =
        BuildConfig.UPDATE_MANIFEST_URL.ifBlank { DOMYSLNY_MANIFEST }

    fun zamknijNaTeSesje() {
        _stan.value = Stan.Bezczynny
    }

    /**
     * Zawsze przy wejściu do aplikacji (onCreate / onResume).
     * Nie zależy od roli użytkownika.
     */
    suspend fun sprawdzPrzyStarcie(context: Context) {
        // Domyślnie włączone; wyłączenie tylko jawne w prefs
        if (!autoSprawdzaj(context)) return
        val teraz = System.currentTimeMillis()
        // Nie spamuj przy szybkim onResume (np. powrót z ustawień instalacji)
        if (teraz - ostatnieSprawdzenieMs < 8_000L) {
            when (val s = _stan.value) {
                is Stan.GotowaDoInstalacji -> {
                    if (czyMoznaInstalowac(context)) {
                        zainstalujZWyborem(context, s.plik)
                    }
                }
                is Stan.WymagaUprawnienia -> {
                    if (czyMoznaInstalowac(context)) {
                        val plik = s.plik
                        if (plik != null && plik.exists()) {
                            _stan.value = Stan.GotowaDoInstalacji(plik, s.info)
                            zainstalujZWyborem(context, plik)
                        } else {
                            sprawdz(context, cichyGdyAktualna = true)
                        }
                    }
                }
                else -> Unit
            }
            return
        }
        ostatnieSprawdzenieMs = teraz
        sprawdz(context, cichyGdyAktualna = true)
    }

    suspend fun sprawdz(
        context: Context,
        cichyGdyAktualna: Boolean = false
    ): Stan = withContext(Dispatchers.IO) {
        mutex.withLock {
            when (_stan.value) {
                is Stan.Sprawdzanie, is Stan.Pobieranie -> return@withLock _stan.value
                else -> Unit
            }
            _stan.value = Stan.Sprawdzanie
            try {
                val bazowy = manifestUrl()
                val sep = if (bazowy.contains('?')) '&' else '?'
                val url = "$bazowy${sep}_=${System.currentTimeMillis()}"
                val json = pobierzTekst(url, githubToken(context))
                val info = parsujManifest(json)
                val wynik = if (info.versionCode > BuildConfig.VERSION_CODE) {
                    Stan.Dostepna(info)
                } else {
                    Stan.Aktualna
                }
                if (cichyGdyAktualna && wynik is Stan.Aktualna) {
                    _stan.value = Stan.Bezczynny
                } else {
                    _stan.value = wynik
                }
                wynik
            } catch (e: Exception) {
                val blad = Stan.Blad(e.message?.takeIf { it.isNotBlank() } ?: "Błąd sprawdzania")
                // Przy starcie nie zasłaniaj UI błędem sieci — cicho
                _stan.value = if (cichyGdyAktualna) Stan.Bezczynny else blad
                blad
            }
        }
    }

    /** Tylko pobierz i zapisz APK — bez instalatora / uprawnień. */
    suspend fun pobierz(context: Context, info: InfoWersji): Boolean =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                if (_stan.value is Stan.Pobieranie) return@withLock false
                try {
                    _stan.value = Stan.Pobieranie(0.02f, info)
                    val plik = pobierzDoCache(context, info) { p ->
                        _stan.value = Stan.Pobieranie(p, info)
                    }
                    weryfikujPodpisApk(context, plik)
                    _stan.value = Stan.GotowaDoInstalacji(plik, info)
                    true
                } catch (e: Exception) {
                    _stan.value = Stan.Blad(e.message?.takeIf { it.isNotBlank() } ?: "Błąd pobierania")
                    false
                }
            }
        }

    private fun pobierzDoCache(
        context: Context,
        info: InfoWersji,
        postep: (Float) -> Unit
    ): File {
        val dir = File(context.cacheDir, "apk").apply { mkdirs() }
        val plik = File(dir, NAZWA_APK_POBRANE)
        if (plik.exists()) plik.delete()
        pobierzPlik(info.apkUrl, plik, githubToken(context), postep)
        return plik
    }

    /**
     * Ikona terminala: pobierz APK (jeśli trzeba) i od razu pokaż wybór instalatora.
     * Bez sprawdzania uprawnień „instaluj nieznane” — użytkownik wybiera apkę, która ogarnie instalację.
     */
    suspend fun otworzWyborInstalatora(context: Context, info: InfoWersji): Boolean {
        val aktualny = _stan.value
        val istniejacy = when (aktualny) {
            is Stan.GotowaDoInstalacji -> aktualny.plik
            is Stan.WymagaUprawnienia -> aktualny.plik
            else -> null
        }?.takeIf { it.exists() && it.length() > 1024L }

        val plik = if (istniejacy != null) {
            istniejacy
        } else {
            val ok = pobierz(context, info)
            if (!ok) return false
            (_stan.value as? Stan.GotowaDoInstalacji)?.plik ?: return false
        }

        withContext(Dispatchers.Main) {
            zainstalujZWyborem(context, plik, pominSprawdzenieUprawnien = true)
        }
        return true
    }

    fun czyMoznaInstalowac(context: Context): Boolean =
        context.packageManager.canRequestPackageInstalls()

    fun intentUstawienNieznaneZrodla(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /**
     * Otwiera systemowy chooser: Package Installer / menedżer plików / inny instalator.
     * @param pominSprawdzenieUprawnien true = nie blokuj oknem uprawnień (ikona terminala).
     */
    fun zainstalujZWyborem(
        context: Context,
        plik: File,
        pominSprawdzenieUprawnien: Boolean = false
    ) {
        if (!plik.exists() || plik.length() < 1024L) {
            _stan.value = Stan.Blad("Brak pobranego APK — pobierz ponownie")
            return
        }
        if (!pominSprawdzenieUprawnien && !czyMoznaInstalowac(context)) {
            val info = (_stan.value as? Stan.GotowaDoInstalacji)?.info
                ?: (_stan.value as? Stan.WymagaUprawnienia)?.info
                ?: InfoWersji(BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME, "", "")
            _stan.value = Stan.WymagaUprawnienia(plik, info)
            return
        }
        try {
            weryfikujPodpisApk(context, plik)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                plik
            )
            val bazowy = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                )
                clipData = ClipData.newRawUri("apk", uri)
            }

            val pm = context.packageManager
            @Suppress("DEPRECATION")
            val handlery = pm.queryIntentActivities(bazowy, PackageManager.MATCH_DEFAULT_ONLY)
            for (ri in handlery) {
                context.grantUriPermission(
                    ri.activityInfo.packageName,
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            val chooser = Intent.createChooser(bazowy, "Wybierz aplikację do instalacji").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                clipData = ClipData.newRawUri("apk", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(chooser)
            _stan.value = Stan.GotowaDoInstalacji(plik, when (val s = _stan.value) {
                is Stan.GotowaDoInstalacji -> s.info
                is Stan.WymagaUprawnienia -> s.info
                is Stan.Dostepna -> s.info
                is Stan.Pobieranie -> s.info
                else -> InfoWersji(BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME, "", "")
            })
        } catch (e: Exception) {
            _stan.value = Stan.Blad(
                e.message?.takeIf { it.isNotBlank() } ?: "Nie udało się otworzyć instalatora"
            )
        }
    }

    /** Alias pod stare wywołania. */
    fun zainstalujSystemowo(context: Context, plik: File) = zainstalujZWyborem(context, plik)

    private fun parsujManifest(json: String): InfoWersji {
        val o = JSONObject(json)
        val code = o.optInt("versionCode", -1)
        require(code > 0) { "Niepoprawny manifest — brak versionCode" }
        val name = o.optString("versionName", code.toString()).ifBlank { code.toString() }
        val apk = o.optString("apkUrl", "").trim()
        require(apk.startsWith("https://")) { "apkUrl musi być https://" }
        val host = runCatching { URL(apk).host.lowercase() }.getOrNull().orEmpty()
        require(
            host == "github.com" || host.endsWith(".github.com") ||
                host == "objects.githubusercontent.com" ||
                host.endsWith(".githubusercontent.com")
        ) { "apkUrl spoza zaufanego hosta GitHub" }
        return InfoWersji(code, name, apk, o.optString("changelog", ""))
    }

    private fun weryfikujPodpisApk(context: Context, apk: File) {
        val pm = context.packageManager
        @Suppress("DEPRECATION")
        val archiwum = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pm.getPackageArchiveInfo(apk.absolutePath, GET_SIGNING_CERTIFICATES)
        } else {
            pm.getPackageArchiveInfo(apk.absolutePath, PackageManager.GET_SIGNATURES)
        } ?: error("Nie można odczytać APK")
        require(archiwum.packageName == context.packageName) {
            "APK ma inny package (${archiwum.packageName})"
        }
        val nasze = podpisaneCerty(pm, context.packageName)
        val zApk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            archiwum.signingInfo?.apkContentsSigners?.map { it.toByteArray() }.orEmpty()
        } else {
            @Suppress("DEPRECATION")
            archiwum.signatures?.map { it.toByteArray() }.orEmpty()
        }
        require(nasze.isNotEmpty() && zApk.isNotEmpty()) { "Brak certyfikatów podpisu" }
        require(nasze.any { a -> zApk.any { b -> a.contentEquals(b) } }) {
            "Podpis APK nie pasuje do zainstalowanej aplikacji"
        }
    }

    private fun podpisaneCerty(pm: PackageManager, pkg: String): List<ByteArray> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info = pm.getPackageInfo(pkg, GET_SIGNING_CERTIFICATES)
            info.signingInfo?.apkContentsSigners?.map { it.toByteArray() }.orEmpty()
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES)
                .signatures?.map { it.toByteArray() }.orEmpty()
        }
    }

    private fun pobierzTekst(url: String, token: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 20_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json")
            if (token.isNotBlank()) setRequestProperty("Authorization", "Bearer $token")
        }
        conn.inputStream.bufferedReader().use { return it.readText() }
    }

    private fun pobierzPlik(
        url: String,
        cel: File,
        token: String,
        postep: (Float) -> Unit
    ) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 120_000
            instanceFollowRedirects = true
            if (token.isNotBlank()) setRequestProperty("Authorization", "Bearer $token")
        }
        val total = conn.contentLengthLong.coerceAtLeast(1L)
        conn.inputStream.use { wejscie ->
            FileOutputStream(cel).use { wyjscie ->
                val buf = ByteArray(64 * 1024)
                var suma = 0L
                while (true) {
                    val n = wejscie.read(buf)
                    if (n <= 0) break
                    wyjscie.write(buf, 0, n)
                    suma += n
                    postep((suma.toFloat() / total.toFloat()).coerceIn(0.02f, 0.99f))
                }
            }
        }
        postep(1f)
    }
}
