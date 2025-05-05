package org.example.comptable.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.example.comptable.routing.Routes

@Composable
@Preview
fun FirstScreen(onNavigateTo: (Routes) -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource("Comptable.png"),
                contentDescription = "Logo de l'application Comptable",
                modifier = Modifier.size(400.dp).padding(bottom = 32.dp)
            )
            Button(
                onClick = {
                    onNavigateTo(Routes.LOGIN)
                },
                modifier = Modifier.padding(16.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF1dbc7c), // Couleur de fond du bouton
                    contentColor = Color(0xfff5f5dc)         // Couleur du texte (et icône)
                )
            ) {
                Text(text = "Connexion")
            }
        }
    }
}