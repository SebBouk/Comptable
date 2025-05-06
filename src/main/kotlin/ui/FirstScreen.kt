package org.example.comptable.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.comptable.config.DatabaseConfig
import org.example.comptable.routing.Routes
import org.example.comptable.ui.components.DatabaseConfigDialog

@Composable
@Preview
fun FirstScreen(onNavigateTo: (Routes) -> Unit) {
    var showDatabaseConfigDialog by remember { mutableStateOf(false) }
    var currentDatabaseConfig by remember { mutableStateOf(DatabaseConfig.load()) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    // Contenu principal avec positionnement amélioré
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Structure verticale principale
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo au centre
            Image(
                painter = painterResource("Comptable.png"),
                contentDescription = "Logo de l'application Comptable",
                modifier = Modifier.size(250.dp)
            )

            // Bouton Commencer - SOUS le logo et le texte
            Button(
                onClick = { onNavigateTo(Routes.LOGIN) },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF1dbc7c),
                    contentColor = Color(0xfff5f5dc)
                ),
                modifier = Modifier.padding(top = 32.dp)
            ) {
                Text("Commencer", fontSize = 20.sp)
            }
        }

        // Le bouton de configuration reste en bas à gauche
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Button(
                onClick = { showDatabaseConfigDialog = true },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF1dbc7c),
                    contentColor = Color(0xfff5f5dc)
                )
            ) {
                Text("Configuration BDD")
            }
        }
    }

    // Le reste du code pour le dialogue reste inchangé
    if (showDatabaseConfigDialog) {
        DatabaseConfigDialog(
            currentConfig = currentDatabaseConfig,
            onConfigSaved = { newConfig, newDb ->
                currentDatabaseConfig = newConfig
                newDb?.let {
                    snackbarMessage = "Configuration enregistrée. Redémarrez l'application pour appliquer les changements."
                }
                showDatabaseConfigDialog = false
            },
            onDismiss = {
                showDatabaseConfigDialog = false
            }
        )
    }

    // Afficher le message snackbar si nécessaire
    snackbarMessage?.let {
        AlertDialog(
            onDismissRequest = { snackbarMessage = null },
            backgroundColor = Color(0xFFf7f7e1),
            title = { Text("Information") },
            text = { Text(it) },
            confirmButton = {
                Button(
                    onClick = { snackbarMessage = null },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF1dbc7c),
                        contentColor = Color(0xfff5f5dc)
                    )
                ) {
                    Text("OK")
                }
            }
        )
    }
}