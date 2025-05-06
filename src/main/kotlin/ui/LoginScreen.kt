package org.example.comptable.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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



@Composable
@Preview

fun LoginScreen(
    onLogin: (String, String) -> Unit,
    loginFailed: Boolean =false,
    onCreateUser: (String,String,String,String,String)->Unit)
{
    var login by remember { mutableStateOf(TextFieldValue("")) }
    var password by remember { mutableStateOf(TextFieldValue("")) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isCreatingAccount by remember { mutableStateOf(false) }

    var prenom by remember { mutableStateOf(TextFieldValue("")) }
    var nom by remember { mutableStateOf(TextFieldValue("")) }
    var email by remember { mutableStateOf(TextFieldValue("")) }

    LaunchedEffect(loginFailed) {
        if (loginFailed) {
            errorMessage = "Identifiant ou mot de passe incorrect"
        }
    }

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

        Card(
            elevation = 8.dp,
            shape = RoundedCornerShape(12.dp),
            backgroundColor = Color(0xFFf7f7e1),
            modifier = Modifier
                .width(if (isCreatingAccount) 450.dp else 350.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
                    .onPreviewKeyEvent {
                        if (it.type == KeyEventType.KeyDown && it.key == Key.Enter) {
                            if (!isCreatingAccount && login.text.isNotEmpty() && password.text.isNotEmpty()) {
                                onLogin(login.text, password.text)
                                true
                            } else {
                                false
                            }
                        } else {
                            false
                        }
                    },
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isCreatingAccount) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Bouton Retour avec icône
                        IconButton(
                            onClick = {
                                isCreatingAccount = false
                                errorMessage = null
                                // Réinitialiser les champs
                                nom = TextFieldValue("")
                                prenom = TextFieldValue("")
                                email = TextFieldValue("")
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Retour",
                                tint = Color(0xFFb61431)
                            )
                        }

                        // Titre
                        Text(
                            "Créer un compte",
                            fontSize = 24.sp,
                            modifier = Modifier.padding(end = 40.dp)
                        )

                        // Spacer pour équilibrer la mise en page
                        Spacer(modifier = Modifier.width(40.dp))
                    }
                } else {
                    Text("Connexion", fontSize = 24.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Afficher les champs supplémentaires uniquement en mode création de compte
                if (isCreatingAccount) {
                    OutlinedTextField(
                        singleLine = true,
                        value = nom,
                        onValueChange = { nom = it },
                        label = { Text("Nom") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFF1dbc7c),
                            focusedLabelColor = Color(0xff1dbc7c)
                        )
                    )

                    OutlinedTextField(
                        singleLine = true,
                        value = prenom,
                        onValueChange = { prenom = it },
                        label = { Text("Prénom") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFF1dbc7c),
                            focusedLabelColor = Color(0xff1dbc7c)
                        )
                    )

                    OutlinedTextField(
                        singleLine = true,
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFF1dbc7c),
                            focusedLabelColor = Color(0xff1dbc7c)
                        )
                    )
                }

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

                Spacer(modifier = Modifier.height(30.dp))

                Button(
                    onClick = {
                        errorMessage = null // Réinitialiser le message d'erreur

                        if (isCreatingAccount) {
                            // Vérifier que tous les champs sont remplis pour la création
                            if (nom.text.isNotEmpty() && prenom.text.isNotEmpty() &&
                                email.text.isNotEmpty() && login.text.isNotEmpty() &&
                                password.text.isNotEmpty()) {
                                onCreateUser(nom.text, prenom.text, login.text, password.text, email.text)
                            } else {
                                errorMessage = "Veuillez remplir tous les champs"
                            }
                        } else {
                            // Mode connexion
                            if (login.text.isNotEmpty() && password.text.isNotEmpty()) {
                                onLogin(login.text, password.text)
                            } else {
                                errorMessage = "Veuillez remplir tous les champs"
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF1dbc7c),
                        contentColor = Color(0xfff5f5dc)
                    )
                ) {
                    Text(if (isCreatingAccount) "Créer le compte" else "Se connecter")
                }

                Spacer(modifier = Modifier.height(15.dp))

                // Bouton pour basculer entre les modes
                TextButton(
                    onClick = {
                        isCreatingAccount = !isCreatingAccount
                        errorMessage = null // Réinitialiser le message d'erreur
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        if (isCreatingAccount) "Déjà un compte ? Se connecter" else "Créer un nouveau compte",
                        color = Color(0xFF1dbc7c)
                    )
                }
            }
        }
    }
}