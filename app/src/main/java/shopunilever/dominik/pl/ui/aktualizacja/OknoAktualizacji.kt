package shopunilever.dominik.pl.ui.aktualizacja

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import shopunilever.dominik.pl.ui.components.SzkloKarta
import shopunilever.dominik.pl.ui.theme.AkcentNeon
import shopunilever.dominik.pl.ui.theme.TekstDrugi
import shopunilever.dominik.pl.ui.theme.TekstGlowny
import shopunilever.dominik.pl.ui.theme.TekstNaAkcencie
import shopunilever.dominik.pl.update.AktualizacjaAplikacji

/**
 * Centralne okno aktualizacji. Zamknięcie tylko na tę sesję —
 * przy kolejnym starcie pojawi się ponownie, gdy nadal jest nowsza wersja.
 */
@Composable
fun OknoAktualizacji(modifier: Modifier = Modifier) {
    val kontekst = LocalContext.current
    val stan by AktualizacjaAplikacji.stan.collectAsState()
    val zakres = rememberCoroutineScope()

    val widoczne = when (stan) {
        is AktualizacjaAplikacji.Stan.Dostepna,
        is AktualizacjaAplikacji.Stan.Pobieranie,
        is AktualizacjaAplikacji.Stan.GotowaDoInstalacji,
        is AktualizacjaAplikacji.Stan.Blad -> true
        else -> false
    }

    AnimatedVisibility(
        visible = widoczne,
        enter = fadeIn() + scaleIn(initialScale = 0.94f),
        exit = fadeOut() + scaleOut(targetScale = 0.96f),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.62f))
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            SzkloKarta(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(AkcentNeon.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SystemUpdate,
                            contentDescription = null,
                            tint = AkcentNeon
                        )
                    }
                    Spacer(modifier = Modifier.size(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AKTUALIZACJA",
                            color = AkcentNeon,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.6.sp
                        )
                        Text(
                            text = "Dostępna nowa wersja",
                            color = TekstGlowny,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = { AktualizacjaAplikacji.zamknijNaTeSesje() }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Zamknij",
                            tint = TekstDrugi
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                when (val s = stan) {
                    is AktualizacjaAplikacji.Stan.Dostepna -> {
                        TekstWersji(s.info)
                        Spacer(modifier = Modifier.height(16.dp))
                        PrzyciskAkcji("Pobierz aktualizację") {
                            zakres.launch { AktualizacjaAplikacji.pobierz(kontekst, s.info) }
                        }
                    }

                    is AktualizacjaAplikacji.Stan.Pobieranie -> {
                        TekstWersji(s.info)
                        Spacer(modifier = Modifier.height(14.dp))
                        LinearProgressIndicator(
                            progress = { s.postep01 },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(99.dp)),
                            color = AkcentNeon,
                            trackColor = Color.White.copy(alpha = 0.08f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Pobieranie… ${(s.postep01 * 100).toInt()}%",
                            color = TekstDrugi,
                            fontSize = 13.sp
                        )
                    }

                    is AktualizacjaAplikacji.Stan.GotowaDoInstalacji -> {
                        TekstWersji(s.info)
                        Spacer(modifier = Modifier.height(16.dp))
                        PrzyciskAkcji("Zainstaluj") {
                            if (!AktualizacjaAplikacji.czyMoznaInstalowac(kontekst)) {
                                kontekst.startActivity(
                                    AktualizacjaAplikacji.intentUstawienNieznaneZrodla(kontekst)
                                )
                            } else {
                                AktualizacjaAplikacji.zainstalujSystemowo(kontekst, s.plik)
                            }
                        }
                    }

                    is AktualizacjaAplikacji.Stan.Blad -> {
                        Text(
                            text = s.komunikat,
                            color = TekstDrugi,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(onClick = {
                            zakres.launch {
                                AktualizacjaAplikacji.sprawdz(kontekst, cichyGdyAktualna = false)
                            }
                        }) {
                            Text("Spróbuj ponownie", color = AkcentNeon)
                        }
                    }

                    else -> CircularProgressIndicator(color = AkcentNeon)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Możesz zamknąć okno — przypomnimy przy następnym uruchomieniu.",
                    color = TekstDrugi.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun TekstWersji(info: AktualizacjaAplikacji.InfoWersji) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "v${AktualizacjaAplikacji.lokalnaWersja()} → v${info.versionName} (${info.versionCode})",
            color = TekstGlowny,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
        if (info.changelog.isNotBlank()) {
            Text(
                text = info.changelog,
                color = TekstDrugi,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun PrzyciskAkcji(etykieta: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AkcentNeon)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = etykieta,
            color = TekstNaAkcencie,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }
}
