package shopunilever.dominik.pl.ui.pracownik

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import shopunilever.dominik.pl.ui.theme.AkcentNeon
import shopunilever.dominik.pl.ui.theme.TekstDrugi
import shopunilever.dominik.pl.ui.theme.TekstGlowny
import shopunilever.dominik.pl.ui.theme.TloWarstwa

@Composable
fun DialogDanychSluzbowych(
    emailPoczatkowy: String,
    hasloPoczatkowe: String,
    onAnuluj: () -> Unit,
    onZapisz: (email: String, haslo: String) -> Unit
) {
    var email by remember { mutableStateOf(emailPoczatkowy) }
    var haslo by remember { mutableStateOf(hasloPoczatkowe) }
    var pokazHaslo by remember { mutableStateOf(false) }
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

    Dialog(onDismissRequest = onAnuluj) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(TloWarstwa)
                .padding(20.dp)
        ) {
            Text(
                text = "DANE SŁUŻBOWE",
                color = AkcentNeon,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.6.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Konto sklepiku",
                color = TekstGlowny,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Email z konta służbowego oraz hasło do Sklepiku Fabrycznego.",
                color = TekstDrugi,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; blad = null },
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
                onValueChange = { haslo = it; blad = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Hasło Sklepiku Fabrycznego") },
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

            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onAnuluj) {
                    Text("Anuluj", color = TekstDrugi)
                }
                TextButton(
                    onClick = {
                        if (email.isBlank() || haslo.isBlank()) {
                            blad = "Podaj email służbowy i hasło."
                        } else {
                            onZapisz(email.trim(), haslo)
                        }
                    }
                ) {
                    Text("Zapisz", color = AkcentNeon, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
