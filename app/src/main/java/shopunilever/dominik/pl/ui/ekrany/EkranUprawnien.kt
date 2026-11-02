package shopunilever.dominik.pl.ui.ekrany

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import shopunilever.dominik.pl.onboarding.KatalogUprawnien
import shopunilever.dominik.pl.onboarding.PozycjaUprawnienia
import shopunilever.dominik.pl.onboarding.RodzajProsby
import shopunilever.dominik.pl.onboarding.RolaUzytkownika
import shopunilever.dominik.pl.ui.components.SzkloKarta
import shopunilever.dominik.pl.ui.components.TloAplikacji
import shopunilever.dominik.pl.ui.theme.AkcentMorski
import shopunilever.dominik.pl.ui.theme.AkcentNeon
import shopunilever.dominik.pl.ui.theme.Sukces
import shopunilever.dominik.pl.ui.theme.TekstDrugi
import shopunilever.dominik.pl.ui.theme.TekstGlowny
import shopunilever.dominik.pl.ui.theme.TekstNaAkcencie
import shopunilever.dominik.pl.update.AktualizacjaAplikacji

@Composable
fun EkranUprawnien(
    rola: RolaUzytkownika,
    onGotowe: () -> Unit,
    modifier: Modifier = Modifier
) {
    val kontekst = LocalContext.current
    val lista = remember(rola) { KatalogUprawnien.dlaRoli(rola) }

    fun sprawdz(pozycja: PozycjaUprawnienia): Boolean = when (pozycja.rodzaj) {
        RodzajProsby.RUNTIME -> {
            if (pozycja.uprawnieniaRuntime.isEmpty()) true
            else pozycja.uprawnieniaRuntime.all {
                ContextCompat.checkSelfPermission(kontekst, it) == PackageManager.PERMISSION_GRANTED
            }
        }
        RodzajProsby.INSTALACJA_APK -> AktualizacjaAplikacji.czyMoznaInstalowac(kontekst)
        RodzajProsby.BATERIA -> {
            val pm = kontekst.getSystemService(PowerManager::class.java)
            pm?.isIgnoringBatteryOptimizations(kontekst.packageName) == true
        }
    }

    var stan by remember {
        mutableStateOf(lista.associate { it.id to sprawdz(it) })
    }

    fun odswiezWszystkie() {
        stan = lista.associate { it.id to sprawdz(it) }
    }

    // Po powrocie z ustawień systemowych odśwież status.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, lista) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) odswiezWszystkie()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var oczekujaceId by remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        odswiezWszystkie()
        oczekujaceId = null
    }

    fun popros(pozycja: PozycjaUprawnienia) {
        when (pozycja.rodzaj) {
            RodzajProsby.RUNTIME -> {
                if (pozycja.uprawnieniaRuntime.isEmpty()) {
                    stan = stan + (pozycja.id to true)
                } else {
                    oczekujaceId = pozycja.id
                    launcher.launch(pozycja.uprawnieniaRuntime.toTypedArray())
                }
            }
            RodzajProsby.INSTALACJA_APK -> {
                kontekst.startActivity(AktualizacjaAplikacji.intentUstawienNieznaneZrodla(kontekst))
            }
            RodzajProsby.BATERIA -> {
                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:${kontekst.packageName}")
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                } else {
                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                kontekst.startActivity(intent)
            }
        }
    }

    val wszystkoOk = lista.all { stan[it.id] == true }

    TloAplikacji(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "UPRAWNIENIA",
                    color = AkcentNeon,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (rola == RolaUzytkownika.PRACOWNIK) {
                        "Profil pracownika"
                    } else {
                        "Profil rodzinny"
                    },
                    color = TekstGlowny,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (rola == RolaUzytkownika.PRACOWNIK) {
                        "Te zgody są potrzebne do auto-logowania, syncu SMS i aktualizacji."
                    } else {
                        "Te zgody są potrzebne do powiadomień o dostępie i aktualizacji aplikacji."
                    },
                    color = TekstDrugi,
                    fontSize = 15.sp,
                    lineHeight = 21.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                lista.forEach { pozycja ->
                    val ok = stan[pozycja.id] == true
                    SzkloKarta(
                        zaznaczona = ok,
                        onClick = { if (!ok) popros(pozycja) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(AkcentNeon.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (ok) Icons.Rounded.CheckCircle else pozycja.ikona,
                                    contentDescription = null,
                                    tint = if (ok) Sukces else AkcentNeon
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pozycja.tytul,
                                    color = TekstGlowny,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = pozycja.opis,
                                    color = TekstDrugi,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (ok) "Włączone" else "Dotknij, aby włączyć",
                                    color = if (ok) Sukces else AkcentNeon,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!wszystkoOk) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(AkcentNeon.copy(alpha = 0.14f))
                        .clickable {
                            lista.firstOrNull { stan[it.id] != true }?.let { popros(it) }
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Włącz brakujące",
                        color = AkcentNeon,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (wszystkoOk) {
                            Brush.horizontalGradient(listOf(AkcentNeon, AkcentMorski))
                        } else {
                            Brush.horizontalGradient(
                                listOf(
                                    TekstDrugi.copy(alpha = 0.28f),
                                    TekstDrugi.copy(alpha = 0.18f)
                                )
                            )
                        }
                    )
                    .clickable(enabled = wszystkoOk, onClick = onGotowe)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Wejdź do sklepu",
                    color = if (wszystkoOk) TekstNaAkcencie else TekstDrugi,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (wszystkoOk) {
                    "Wszystkie uprawnienia włączone."
                } else {
                    "Aby wejść dalej, włącz wszystkie uprawnienia z listy."
                },
                color = TekstDrugi,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}
