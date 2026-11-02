package shopunilever.dominik.pl.ui.pracownik

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
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

    // Email można podpowiedzieć; hasła NIGDY nie pokazujemy z pamięci
    var email by remember { mutableStateOf(PreferencjeKonta.email(kontekst)) }
    var haslo by remember { mutableStateOf("") }
    var pokazHaslo by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var blad by remember { mutableStateOf<String?>(null) }

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
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Hasło nie jest wyświetlane — wpisz nowe, żeby je zmienić.",
                color = TekstDrugi,
                fontSize = 13.sp,
                lineHeight = 18.sp
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
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = haslo,
                onValueChange = {
                    haslo = it
                    status = null
                    blad = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Hasło Sklepiku Fabrycznego") },
                placeholder = { Text("Wpisz hasło") },
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

            if (blad != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = blad!!, color = Color(0xFFF87171), fontSize = 12.sp)
            }
            if (status != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = status!!, color = AkcentMorski, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.horizontalGradient(listOf(AkcentNeon, AkcentMorski)))
                    .clickable {
                        when {
                            email.isBlank() -> blad = "Podaj email służbowy."
                            haslo.isBlank() -> blad = "Podaj hasło (nie pokazujemy poprzedniego)."
                            else -> {
                                PreferencjeKonta.zapiszDane(kontekst, email.trim(), haslo)
                                haslo = ""
                                pokazHaslo = false
                                status = "Zapisano."
                                blad = null
                                if (PreferencjeParowania.czyAktywne(kontekst)) {
                                    zakres.launch {
                                        runCatching { SerwisParowania.publikujDaneLogowania(kontekst) }
                                    }
                                }
                            }
                        }
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Zapisz",
                    color = TekstNaAkcencie,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}
