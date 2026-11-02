package shopunilever.dominik.pl.ui.ekrany

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.core.content.ContextCompat
import shopunilever.dominik.pl.onboarding.RolaUzytkownika
import shopunilever.dominik.pl.ui.components.SzkloKarta
import shopunilever.dominik.pl.ui.components.TloAplikacji
import shopunilever.dominik.pl.ui.theme.AkcentMorski
import shopunilever.dominik.pl.ui.theme.AkcentNeon
import shopunilever.dominik.pl.ui.theme.Sukces
import shopunilever.dominik.pl.ui.theme.TekstDrugi
import shopunilever.dominik.pl.ui.theme.TekstGlowny
import shopunilever.dominik.pl.ui.theme.TekstNaAkcencie

data class PozycjaUprawnienia(
    val id: String,
    val tytul: String,
    val opis: String,
    val ikona: ImageVector,
    val uprawnienia: List<String>
)

fun uprawnieniaDlaRoli(rola: RolaUzytkownika): List<PozycjaUprawnienia> {
    val powiadomienia = if (Build.VERSION.SDK_INT >= 33) {
        listOf(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        emptyList()
    }

    return when (rola) {
        RolaUzytkownika.PRACOWNIK -> listOf(
            PozycjaUprawnienia(
                id = "sms",
                tytul = "SMS (auto-logowanie)",
                opis = "Odczyt kodu logowania do sklepu firmowego.",
                ikona = Icons.Rounded.Sms,
                uprawnienia = listOf(
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_SMS
                )
            ),
            PozycjaUprawnienia(
                id = "powiadomienia",
                tytul = "Powiadomienia",
                opis = "Status zamówień i synchronizacji między telefonami.",
                ikona = Icons.Rounded.Notifications,
                uprawnienia = powiadomienia
            )
        )

        RolaUzytkownika.RODZINA -> listOf(
            PozycjaUprawnienia(
                id = "powiadomienia",
                tytul = "Powiadomienia",
                opis = "Informacje o statusie dostępu i zakupach.",
                ikona = Icons.Rounded.Notifications,
                uprawnienia = powiadomienia
            )
        )
    }
}

@Composable
fun EkranUprawnien(
    rola: RolaUzytkownika,
    onGotowe: () -> Unit,
    modifier: Modifier = Modifier
) {
    val kontekst = LocalContext.current
    val lista = remember(rola) { uprawnieniaDlaRoli(rola) }

    fun czyPrzyznane(perms: List<String>): Boolean {
        if (perms.isEmpty()) return true
        return perms.all {
            ContextCompat.checkSelfPermission(kontekst, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    var stan by remember {
        mutableStateOf(lista.associate { it.id to czyPrzyznane(it.uprawnienia) })
    }

    var oczekujaceId by remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { wynik ->
        val id = oczekujaceId
        if (id != null) {
            stan = stan + (id to wynik.values.all { it })
        }
        oczekujaceId = null
    }

    TloAplikacji(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
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
                    text = "Włącz tylko to, czego potrzebujesz. Możesz też pominąć i wrócić później.",
                    color = TekstDrugi,
                    fontSize = 15.sp,
                    lineHeight = 21.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                lista.forEach { pozycja ->
                    val ok = stan[pozycja.id] == true
                    SzkloKarta(
                        zaznaczona = ok,
                        onClick = {
                            if (pozycja.uprawnienia.isEmpty()) {
                                stan = stan + (pozycja.id to true)
                            } else {
                                oczekujaceId = pozycja.id
                                launcher.launch(pozycja.uprawnienia.toTypedArray())
                            }
                        }
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
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.horizontalGradient(listOf(AkcentNeon, AkcentMorski)))
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
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Możesz kontynuować bez wszystkich zgód.",
                    color = TekstDrugi,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}
