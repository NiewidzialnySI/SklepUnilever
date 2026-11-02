package shopunilever.dominik.pl.ui.aktualizacja

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import shopunilever.dominik.pl.ui.theme.AkcentMorski
import shopunilever.dominik.pl.ui.theme.AkcentNeon
import shopunilever.dominik.pl.ui.theme.TekstDrugi
import shopunilever.dominik.pl.ui.theme.TekstGlowny
import shopunilever.dominik.pl.ui.theme.TekstNaAkcencie
import shopunilever.dominik.pl.ui.theme.TloWarstwa
import shopunilever.dominik.pl.update.AktualizacjaAplikacji

/**
 * Proste okno: Aktualizuj (pobierz) → Zainstaluj.
 * Ikona terminala = od razu wybór instalatora (bez wymuszania uprawnień).
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
        is AktualizacjaAplikacji.Stan.WymagaUprawnienia,
        is AktualizacjaAplikacji.Stan.Blad -> true
        else -> false
    }

    val infoWersji = when (val s = stan) {
        is AktualizacjaAplikacji.Stan.Dostepna -> s.info
        is AktualizacjaAplikacji.Stan.Pobieranie -> s.info
        is AktualizacjaAplikacji.Stan.GotowaDoInstalacji -> s.info
        is AktualizacjaAplikacji.Stan.WymagaUprawnienia -> s.info
        else -> null
    }

    AnimatedVisibility(
        visible = widoczne,
        enter = fadeIn() + scaleIn(initialScale = 0.96f),
        exit = fadeOut() + scaleOut(targetScale = 0.98f),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(TloWarstwa)
                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(22.dp))
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Aktualizacja",
                        color = TekstGlowny,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            val info = infoWersji ?: return@IconButton
                            zakres.launch {
                                AktualizacjaAplikacji.otworzWyborInstalatora(kontekst, info)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Terminal,
                            contentDescription = "Wybierz instalator",
                            tint = AkcentNeon
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

                infoWersji?.let {
                    Text(
                        text = "v${it.versionName}",
                        color = TekstDrugi,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                when (val s = stan) {
                    is AktualizacjaAplikacji.Stan.Dostepna -> {
                        PrzyciskGlowny("Aktualizuj") {
                            zakres.launch {
                                AktualizacjaAplikacji.pobierz(kontekst, s.info)
                            }
                        }
                    }

                    is AktualizacjaAplikacji.Stan.Pobieranie -> {
                        LinearProgressIndicator(
                            progress = { s.postep01 },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(99.dp)),
                            color = AkcentNeon,
                            trackColor = Color.White.copy(alpha = 0.10f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "${(s.postep01 * 100).toInt()}%",
                            color = TekstDrugi,
                            fontSize = 14.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }

                    is AktualizacjaAplikacji.Stan.GotowaDoInstalacji -> {
                        PrzyciskGlowny("Zainstaluj") {
                            AktualizacjaAplikacji.zainstalujZWyborem(kontekst, s.plik)
                        }
                    }

                    is AktualizacjaAplikacji.Stan.WymagaUprawnienia -> {
                        PrzyciskGlowny("Zainstaluj") {
                            val plik = s.plik
                            if (plik != null && plik.exists()) {
                                AktualizacjaAplikacji.zainstalujZWyborem(
                                    kontekst,
                                    plik,
                                    pominSprawdzenieUprawnien = true
                                )
                            } else {
                                kontekst.startActivity(
                                    AktualizacjaAplikacji.intentUstawienNieznaneZrodla(kontekst)
                                )
                            }
                        }
                    }

                    is AktualizacjaAplikacji.Stan.Blad -> {
                        Text(text = s.komunikat, color = TekstDrugi, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(14.dp))
                        PrzyciskGlowny("Spróbuj ponownie") {
                            zakres.launch {
                                AktualizacjaAplikacji.sprawdz(kontekst, cichyGdyAktualna = false)
                            }
                        }
                    }

                    else -> {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AkcentNeon, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrzyciskGlowny(etykieta: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.horizontalGradient(listOf(AkcentNeon, AkcentMorski)))
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = etykieta,
            color = TekstNaAkcencie,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}
