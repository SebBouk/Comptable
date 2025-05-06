package org.example.comptable.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.example.comptable.config.DatabaseConfig
import org.ktorm.database.Database

@Composable
fun DatabaseConfigDialog(
    currentConfig: DatabaseConfig,
    onConfigSaved: (DatabaseConfig, Database?) -> Unit,
    onDismiss: () -> Unit
) {
    var url by remember { mutableStateOf(TextFieldValue(currentConfig.url)) }
    var user by remember { mutableStateOf(TextFieldValue(currentConfig.user)) }
    var password by remember { mutableStateOf(TextFieldValue(currentConfig.password)) }
    var databaseName by remember { mutableStateOf(TextFieldValue(currentConfig.url.substringAfterLast("/"))) }
    var hostName by remember { mutableStateOf(TextFieldValue(
        currentConfig.url.substringAfter("://").substringBefore(":"))
    ) }
    var port by remember { mutableStateOf(TextFieldValue(
        currentConfig.url.substringAfter("://").substringAfter(":").substringBefore("/"))
    ) }
    var useSSL by remember { mutableStateOf(false) }
    var timezone by remember { mutableStateOf("UTC") }

    var showPassword by remember { mutableStateOf(false) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var connectionStatus by remember { mutableStateOf<ConnectionStatus?>(null) }

    var showAdvancedOptions by remember { mutableStateOf(false) }

    // Générer URL complet à partir des composants
    LaunchedEffect(hostName.text, port.text, databaseName.text, useSSL, timezone) {
        val baseUrl = "jdbc:mysql://${hostName.text}:${port.text}/${databaseName.text}"
        val params = mutableListOf<String>()

        if (!useSSL) params.add("useSSL=false")
        params.add("serverTimezone=$timezone")

        url = TextFieldValue(if (params.isEmpty()) baseUrl else "$baseUrl?${params.joinToString("&")}")
    }

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            elevation = 8.dp,
            shape = RoundedCornerShape(12.dp),
            backgroundColor = Color(0xFFf7f7e1),
            modifier = Modifier
                .width(550.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Titre et bouton de fermeture
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Configuration de la Base de Données",
                        style = MaterialTheme.typography.h6
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = Color(0xFFb61431)
                        )
                    }
                }

                Divider()

                // Informations sur la configuration
                Text(
                    text = "La configuration actuelle sera sauvegardée dans un fichier database.properties. " +
                            "Redémarrez l'application pour appliquer les changements.",
                    style = MaterialTheme.typography.caption,
                    color = Color.Gray
                )

                // Mode simple - séparation des composants
                Text(
                    text = "Informations de connexion",
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold
                )

                // Hôte
                OutlinedTextField(
                    value = hostName,
                    onValueChange = { hostName = it },
                    label = { Text("Hôte") },
                    placeholder = { Text("localhost") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF1dbc7c),
                        focusedLabelColor = Color(0xff1dbc7c)
                    )
                )

                // Port
                OutlinedTextField(
                    value = port,
                    onValueChange = {
                        // Vérifier que c'est un nombre
                        if (it.text.isEmpty() || it.text.all { char -> char.isDigit() }) {
                            port = it
                        }
                    },
                    label = { Text("Port") },
                    placeholder = { Text("3306") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF1dbc7c),
                        focusedLabelColor = Color(0xff1dbc7c)
                    )
                )

                // Nom de la base de données
                OutlinedTextField(
                    value = databaseName,
                    onValueChange = { databaseName = it },
                    label = { Text("Nom de la base de données") },
                    placeholder = { Text("comptable") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF1dbc7c),
                        focusedLabelColor = Color(0xff1dbc7c)
                    )
                )

                // Nom d'utilisateur
                OutlinedTextField(
                    value = user,
                    onValueChange = { user = it },
                    label = { Text("Nom d'utilisateur") },
                    placeholder = { Text("root") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF1dbc7c),
                        focusedLabelColor = Color(0xff1dbc7c)
                    )
                )

                // Mot de passe
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Mot de passe") },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF1dbc7c),
                        focusedLabelColor = Color(0xff1dbc7c)
                    ),
                    trailingIcon = {
                        TextButton(onClick = { showPassword = !showPassword }) {
                            Text(
                                if (showPassword) "Masquer" else "Afficher",
                                color = Color(0xFF1dbc7c)
                            )
                        }
                    }
                )

                // Bouton pour afficher/masquer les options avancées
                TextButton(
                    onClick = { showAdvancedOptions = !showAdvancedOptions },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (showAdvancedOptions) "Masquer les options avancées" else "Afficher les options avancées",
                            color = Color(0xFF1dbc7c)
                        )
                        Icon(
                            imageVector = if (showAdvancedOptions) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color(0xFF1dbc7c)
                        )
                    }
                }

                // Options avancées
                if (showAdvancedOptions) {
                    Divider()

                    Text(
                        text = "Options avancées",
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.Bold
                    )

                    // URL complète
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("URL complète") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFF1dbc7c),
                            focusedLabelColor = Color(0xff1dbc7c)
                        ),
                        singleLine = false,
                        maxLines = 2
                    )

                    // Paramètres additionnels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = !useSSL,
                            onCheckedChange = { useSSL = !it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFF1dbc7c)
                            )
                        )
                        Text("Désactiver SSL (useSSL=false)")
                    }

                    // Sélection du fuseau horaire
                    Text("Fuseau horaire:")
                    var expandedTimezone by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { expandedTimezone = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                backgroundColor = Color.White,
                                contentColor = Color.Black
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(timezone)
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Sélectionner fuseau horaire"
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = expandedTimezone,
                            onDismissRequest = { expandedTimezone = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            listOf("UTC", "Europe/Paris", "Europe/London", "America/New_York", "Asia/Tokyo").forEach { tz ->
                                DropdownMenuItem(onClick = {
                                    timezone = tz
                                    expandedTimezone = false
                                }) {
                                    Text(tz)
                                }
                            }
                        }
                    }
                }

                // Affichage du statut de connexion
                connectionStatus?.let {
                    val (message, color) = when (it) {
                        is ConnectionStatus.Success -> "Connexion réussie !" to Color(0xFF1dbc7c)
                        is ConnectionStatus.Error -> "Erreur : ${it.message}" to Color(0xFFb61431)
                        is ConnectionStatus.Testing -> "Test de connexion en cours..." to Color.Gray
                    }

                    Text(
                        text = message,
                        color = color,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Boutons d'action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = {
                            isTestingConnection = true
                            connectionStatus = ConnectionStatus.Testing

                            try {
                                val config = DatabaseConfig(
                                    url = url.text,
                                    user = user.text,
                                    password = password.text,
                                    driverClassName = "com.mysql.cj.jdbc.Driver"
                                )

                                val testConnection = Database.connect(
                                    url = config.url,
                                    user = config.user,
                                    password = config.password
                                )

                                // Effectuer une requête simple pour vérifier la connexion
                                testConnection.useConnection { conn ->
                                    conn.prepareStatement("SELECT 1").use { stmt ->
                                        stmt.executeQuery().use { rs ->
                                            if (rs.next()) {
                                                connectionStatus = ConnectionStatus.Success
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                connectionStatus = ConnectionStatus.Error(e.message ?: "Erreur inconnue")
                            } finally {
                                isTestingConnection = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF1dbc7c),
                            contentColor = Color(0xfff5f5dc)
                        ),
                        enabled = !isTestingConnection
                    ) {
                        Text("Tester la connexion")
                    }

                    Button(
                        onClick = {
                            try {
                                val config = DatabaseConfig(
                                    url = url.text,
                                    user = user.text,
                                    password = password.text,
                                    driverClassName = "com.mysql.cj.jdbc.Driver"
                                )

                                // Sauvegarder la configuration
                                DatabaseConfig.save(config)

                                // Essayer de se connecter avec la nouvelle configuration
                                val newDatabase = try {
                                    Database.connect(
                                        url = config.url,
                                        user = config.user,
                                        password = config.password
                                    )
                                } catch (e: Exception) {
                                    null
                                }

                                // Notifier le parent
                                onConfigSaved(config, newDatabase)
                            } catch (e: Exception) {
                                connectionStatus = ConnectionStatus.Error(e.message ?: "Erreur lors de la sauvegarde")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF1dbc7c),
                            contentColor = Color(0xfff5f5dc)
                        )
                    ) {
                        Text("Sauvegarder et Connecter")
                    }
                }
            }
        }
    }
}

// États possibles de la connexion
sealed class ConnectionStatus {
    object Success : ConnectionStatus()
    data class Error(val message: String) : ConnectionStatus()
    object Testing : ConnectionStatus()
}