package org.example.comptable.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.comptable.routing.Routes


@Composable
@Preview

fun LoginScreen(onLogin: (String, String) -> Unit) {
    var login by remember { mutableStateOf(TextFieldValue("")) }
    var password by remember { mutableStateOf(TextFieldValue("")) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
            Image(
                painter = painterResource("Comptable.png"),
                contentDescription = "Logo de l'application Comptable",
                modifier = Modifier.size(400.dp).padding(bottom = 16.dp)
            )

        Card(elevation = 8.dp, // Ombre
            shape = RoundedCornerShape(12.dp), // Coins arrondis
            backgroundColor = Color(0xFFf7f7e1), // Fond blanc
            modifier = Modifier
                .width(350.dp)
                .padding(16.dp))
        {

            Column(
                modifier = Modifier.padding(24.dp)
                    .onPreviewKeyEvent {
                        if (it.type == KeyEventType.KeyDown && it.key == Key.Enter) {
                            if (login.text.isNotEmpty() && password.text.isNotEmpty()) {
                                onLogin(login.text, password.text)
                            } else {
                                errorMessage = "Veuillez remplir tous les champs"
                            }
                            true
                        } else {
                            false
                        }
                    },
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Connexion", fontSize = 24.sp)

                Spacer(modifier = Modifier.height(30.dp))

                OutlinedTextField(
                    singleLine = true,
                    value = login,
                    onValueChange = { login = it },
                    label = { Text("Login") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF1dbc7c),
                        focusedLabelColor = Color(0xff1dbc7c)
                    )
                )
                Spacer(modifier = Modifier.height(15.dp))

                OutlinedTextField(
                    singleLine = true,
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Mot de passe") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF1dbc7c),
                        focusedLabelColor = Color(0xff1dbc7c)
                    )
                )
                errorMessage?.let {
                    Text(it, color = MaterialTheme.colors.error)
                }

                Spacer(modifier = Modifier.height(100.dp))

                Button(
                    onClick = {
                        if (login.text.isNotEmpty() && password.text.isNotEmpty()) {
                            onLogin(login.text, password.text)
                        } else {
                            errorMessage = "Veuillez remplir tous les champs"
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF1dbc7c), // Couleur de fond du bouton
                        contentColor = Color(0xfff5f5dc)         // Couleur du texte (et icône)
                    )
                ) {
                    Text("Se connecter")
                }
            }
        }
    }
}