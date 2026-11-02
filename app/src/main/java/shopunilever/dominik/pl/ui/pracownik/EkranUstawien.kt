package shopunilever.dominik.pl.ui.pracownik

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import shopunilever.dominik.pl.dane.PreferencjeKonta
import shopunilever.dominik.pl.dane.PreferencjeParowania
import shopunilever.dominik.pl.firebase.SerwisParowania
import shopunilever.dominik.pl.ui.components.SzkloKarta
import shopunilever.dominik.pl.ui.theme.AkcentMorski
import shopunilever.dominik.pl.ui.theme.AkcentNeon
import shopunilever.dominik.pl.ui.theme.TekstDrugi
import shopunilever.dominik.pl.ui.theme.TekstGlowny
import shopunilever.dominik.pl.ui.theme.TekstNaAkcencie

@Composable
fun EkranUstawien(modifier: Modifier = Modifier) {
    val kontekst = LocalContext.current
    val zakres = rememberCoroutineScope()

    var email by remember { mutableStateOf(PreferencjeKonta.email(kontekst)) }
    var haslo by remember { mutableStateOf("") }
    var pokazHaslo by remember { mutableStateOf(false) }
    var edycjaHasla by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var blad by remember { mutableStateOf<String?>(null) }

    val emailZapisany = PreferencjeKonta.email(kontekst)
    val emailZmieniony = email.trim() != emailZapisany

    val koloryPola = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = AkcentNeon,
        unfocusedBorderColor = TekstDrugi.copy(alpha = 0.35f),
        focusedTextColor = TekstGlowny,
        unfocusedTextColor = TekstGlowny,
        cursorColor = AkcentNeon,
        focusedLabelColor = AkcentNeon,
        unfocusedLabelColor = TekstDrugi,
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent
    )

    fun syncFirebaseJesliTrzeba() {
        if (PreferencjeParowania.czyAktywne(kontekst)) {
            zakres.launch {
                runCatching { SerwisParowania.publikujDaneLogowania(kontekst) }
            }
        }
    }

    fun zapiszEmail(): Boolean {
        if (email.isBlank()) {
            blad = "Podaj email służbowy."
            return false
        }
        val obecneHaslo = PreferencjeKonta.haslo(kontekst)
        PreferencjeKonta.zapiszDane(kontekst, email.trim(), obecneHaslo)
        status = "Zapisano email."
        blad = null
        syncFirebaseJesliTrzeba()
        return true
    }

    fun zapiszHaslo() {
        when {
            email.isBlank() -> blad = "Podaj email służbowy."
            haslo.isBlank() -> blad = "Podaj nowe hasło."
            else -> {
                PreferencjeKonta.zapiszDane(kontekst, email.trim(), haslo)
                haslo = ""
                pokazHaslo = false
                edycjaHasla = false
                status = "Zapisano hasło."
                blad = null
                syncFirebaseJesliTrzeba()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "USTAWIENIA",
            color = AkcentNeon,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Konto",
            color = TekstGlowny,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(22.dp))

        SzkloKarta {
            Text(
                text = "Dane służbowe",
                color = TekstGlowny,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    status = null
                    blad = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Email służbowy") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = koloryPola,
                shape = RoundedCornerShape(14.dp)
            )

            if (emailZmieniony && !edycjaHasla) {
                TextButton(onClick = { zapiszEmail() }) {
                    Text("Zapisz email", color = AkcentNeon, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Animacja z googlecodelabs/android-compose-codelabs (AnimationCodelab):
            // expandVertically + slideInVertically — pole hasła wypycha przycisk w dół.
            AnimatedVisibility(
                visible = edycjaHasla,
                enter = expandVertically(
                    expandFrom = Alignment.Top,
                    animationSpec = tween(280)
                ) + slideInVertically(
                    animationSpec = tween(280),
                    initialOffsetY = { -it / 3 }
                ) + fadeIn(tween(220)),
                exit = shrinkVertically(
                    shrinkTowards = Alignment.Top,
                    animationSpec = tween(220)
                ) + slideOutVertically(
                    animationSpec = tween(220),
                    targetOffsetY = { -it / 4 }
                ) + fadeOut(tween(160))
            ) {
                Column {
                    OutlinedTextField(
                        value = haslo,
                        onValueChange = {
                            haslo = it
                            status = null
                            blad = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nowe hasło") },
                        placeholder = { Text("Wpisz nowe hasło") },
                        singleLine = true,
                        visualTransformation = if (pokazHaslo) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(onClick = { pokazHaslo = !pokazHaslo }) {
                                Icon(
                                    imageVector = if (pokazHaslo) {
                                        Icons.Rounded.VisibilityOff
                                    } else {
                                        Icons.Rounded.Visibility
                                    },
                                    contentDescription = null,
                                    tint = TekstDrugi
                                )
                            }
                        },
                        colors = koloryPola,
                        shape = RoundedCornerShape(14.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            if (blad != null) {
                Text(text = blad!!, color = Color(0xFFF87171), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (status != null) {
                Text(text = status!!, color = AkcentMorski, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.horizontalGradient(listOf(AkcentNeon, AkcentMorski)))
                    .clickable {
                        if (edycjaHasla) {
                            zapiszHaslo()
                        } else {
                            if (emailZmieniony) zapiszEmail()
                            edycjaHasla = true
                            status = null
                            blad = null
                        }
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = edycjaHasla,
                    transitionSpec = {
                        (fadeIn(tween(180)) togetherWith fadeOut(tween(120)))
                    },
                    label = "napis_przycisku_hasla"
                ) { otwarte ->
                    Text(
                        text = if (otwarte) "Zapisz hasło" else "Zmień hasło",
                        color = TekstNaAkcencie,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            AnimatedVisibility(visible = edycjaHasla) {
                TextButton(
                    onClick = {
                        edycjaHasla = false
                        haslo = ""
                        pokazHaslo = false
                        blad = null
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Anuluj", color = TekstDrugi)
                }
            }
        }
    }
}
