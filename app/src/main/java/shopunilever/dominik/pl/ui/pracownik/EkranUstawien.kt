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
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import shopunilever.dominik.pl.dane.PreferencjeKonta
import shopunilever.dominik.pl.dane.PreferencjeParowania
import shopunilever.dominik.pl.firebase.LogWejsciaUrzadzenia
import shopunilever.dominik.pl.firebase.SerwisParowania
import shopunilever.dominik.pl.ui.components.SzkloKarta
import shopunilever.dominik.pl.ui.theme.AkcentMorski
import shopunilever.dominik.pl.ui.theme.AkcentNeon
import shopunilever.dominik.pl.ui.theme.Blad
import shopunilever.dominik.pl.ui.theme.RamkaKarty
import shopunilever.dominik.pl.ui.theme.TekstDrugi
import shopunilever.dominik.pl.ui.theme.TekstGlowny
import shopunilever.dominik.pl.ui.theme.TekstNaAkcencie
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EkranUstawien(modifier: Modifier = Modifier) {
    val kontekst = LocalContext.current
    val zakres = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val klawiatura = LocalSoftwareKeyboardController.current
    val focusHasla = remember { FocusRequester() }

    var email by remember { mutableStateOf(PreferencjeKonta.email(kontekst)) }
    var haslo by remember { mutableStateOf("") }
    var pokazHaslo by remember { mutableStateOf(false) }
    var edycjaHasla by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var blad by remember { mutableStateOf<String?>(null) }
    var logi by remember { mutableStateOf<List<LogWejsciaUrzadzenia>>(emptyList()) }
    var emailZapisany by remember { mutableStateOf(PreferencjeKonta.email(kontekst)) }
    val emailZmieniony = email.trim() != emailZapisany
    val parowanieAktywne = remember { PreferencjeParowania.czyAktywne(kontekst) }
    val formatCzasu = remember {
        SimpleDateFormat("dd.MM HH:mm", Locale("pl", "PL"))
    }

    LaunchedEffect(parowanieAktywne) {
        if (!parowanieAktywne) {
            logi = emptyList()
            return@LaunchedEffect
        }
        runCatching { SerwisParowania.upewnijSesjeNadajnika(kontekst) }
        SerwisParowania.obserwujLogiWejsc(kontekst).collectLatest { logi = it }
    }

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

    fun zamknijEdycjeHasla() {
        if (!edycjaHasla) return
        edycjaHasla = false
        haslo = ""
        pokazHaslo = false
        blad = null
        focusManager.clearFocus(force = true)
        klawiatura?.hide()
    }

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
        emailZapisany = email.trim()
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
                focusManager.clearFocus(force = true)
                klawiatura?.hide()
                syncFirebaseJesliTrzeba()
            }
        }
    }

    LaunchedEffect(edycjaHasla) {
        if (edycjaHasla) {
            // Po animacji rozwinięcia — fokus + klawiatura
            delay(300)
            runCatching {
                focusHasla.requestFocus()
                klawiatura?.show()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (edycjaHasla) zamknijEdycjeHasla()
            }
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top
    ) {
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
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { stan ->
                        if (stan.isFocused && edycjaHasla) {
                            zamknijEdycjeHasla()
                        }
                    },
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusHasla),
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
                            status = null
                            blad = null
                            edycjaHasla = true
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
        }

        Spacer(modifier = Modifier.height(16.dp))

        SzkloKarta {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.History, null, tint = AkcentNeon, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    "Logi",
                    color = TekstGlowny,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    modifier = Modifier.weight(1f)
                )
                if (logi.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            zakres.launch {
                                runCatching { SerwisParowania.wyczyscLogiWejsc(kontekst) }
                            }
                        }
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, "Wyczyść", tint = Blad, modifier = Modifier.size(20.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (logi.isEmpty()) {
                Text("Brak", color = TekstDrugi, fontSize = 13.sp)
            } else {
                logi.forEachIndexed { indeks, wpis ->
                    if (indeks > 0) {
                        HorizontalDivider(
                            color = RamkaKarty,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AkcentMorski.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.PhoneAndroid,
                                null,
                                tint = AkcentMorski,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.size(12.dp))
                        Text(
                            wpis.nazwa,
                            color = TekstGlowny,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            if (wpis.ts > 0L) formatCzasu.format(Date(wpis.ts)) else "—",
                            color = TekstDrugi,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
