package shopunilever.dominik.pl.ui.ekrany

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import shopunilever.dominik.pl.ui.components.TloAplikacji
import shopunilever.dominik.pl.ui.theme.AkcentMorski
import shopunilever.dominik.pl.ui.theme.AkcentNeon
import shopunilever.dominik.pl.ui.theme.Sukces
import shopunilever.dominik.pl.ui.theme.TekstDrugi
import shopunilever.dominik.pl.ui.theme.TekstNaAkcencie
import shopunilever.dominik.pl.ui.theme.TloKarty
import shopunilever.dominik.pl.update.AktualizacjaAplikacji

@Composable
fun EkranUprawnien(
    rola: RolaUzytkownika,
    onGotowe: () -> Unit,
    modifier: Modifier = Modifier
) {
    val kontekst = LocalContext.current
    val lista = remember(rola) { KatalogUprawnien.dlaRoli(rola) }

    val alfa = remember { Animatable(0f) }
    val skala = remember { Animatable(0.95f) }

    LaunchedEffect(Unit) {
        alfa.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
        skala.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
    }

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

    val wymaganeOk = lista.filter { it.wymagane }.all { stan[it.id] == true }
    val brakujaceWymagane = lista.filter { it.wymagane && stan[it.id] != true }

    TloAplikacji(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .alpha(alfa.value)
                    .scale(skala.value)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                lista.forEach { pozycja ->
                    val ok = stan[pozycja.id] == true
                    KartaUprawnienia(
                        pozycja = pozycja,
                        ok = ok,
                        onClick = { if (!ok) popros(pozycja) }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (brakujaceWymagane.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    AkcentNeon.copy(alpha = 0.25f),
                                    AkcentMorski.copy(alpha = 0.15f)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            AkcentNeon.copy(alpha = 0.5f),
                            RoundedCornerShape(18.dp)
                        )
                        .clickable { popros(brakujaceWymagane.first()) }
                        .padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Włącz brakujące",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            AnimatedVisibility(
                visible = wymaganeOk,
                enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 2 }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.horizontalGradient(listOf(AkcentNeon, AkcentMorski))
                        )
                        .clickable(onClick = onGotowe)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Wejdź do sklepu",
                        color = TekstNaAkcencie,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun KartaUprawnienia(
    pozycja: PozycjaUprawnienia,
    ok: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ksztalt = RoundedCornerShape(22.dp)
    val ramkaGlow = if (ok) {
        Brush.verticalGradient(
            listOf(
                Sukces.copy(alpha = 0.7f),
                Sukces.copy(alpha = 0.3f),
                Color.White.copy(alpha = 0.15f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                AkcentNeon.copy(alpha = 0.5f),
                AkcentMorski.copy(alpha = 0.2f),
                Color.White.copy(alpha = 0.10f)
            )
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(ksztalt)
            .border(if (ok) 1.5.dp else 1.dp, ramkaGlow, ksztalt)
            .clickable(onClick = onClick),
        color = if (ok) Sukces.copy(alpha = 0.10f) else TloKarty,
        shape = ksztalt
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            if (ok) Sukces.copy(alpha = 0.15f) else AkcentNeon.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            if (ok) {
                                Brush.linearGradient(
                                    listOf(Sukces.copy(alpha = 0.4f), AkcentMorski.copy(alpha = 0.2f))
                                )
                            } else {
                                Brush.linearGradient(
                                    listOf(AkcentNeon.copy(alpha = 0.45f), AkcentMorski.copy(alpha = 0.20f))
                                )
                            }
                        )
                        .border(
                            1.dp,
                            if (ok) Sukces else AkcentNeon.copy(alpha = 0.6f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (ok) Icons.Rounded.CheckCircle else pozycja.ikona,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pozycja.tytul,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = pozycja.opis,
                        color = TekstDrugi,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = when {
                            ok -> "Włączone"
                            pozycja.wymagane -> "Dotknij, aby włączyć"
                            else -> "Opcjonalne — dotknij, aby włączyć"
                        },
                        color = if (ok) Sukces else AkcentNeon,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
