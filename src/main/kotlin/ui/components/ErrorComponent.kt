package org.example.comptable.ui.components

import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object ErrorHandler {
    // Un état mutable pour stocker les erreurs
    private val _currentError = mutableStateOf<Throwable?>(null)
    val currentError: State<Throwable?> = _currentError

    // Une fonction pour définir l'erreur actuelle
    fun setError(error: Throwable) {
        println("Erreur capturée: ${error.message}")
        error.printStackTrace()
        _currentError.value = error
    }

    // Une fonction pour effacer l'erreur
    fun clearError() {
        _currentError.value = null
    }

    // Une fonction pour exécuter du code en toute sécurité
    fun <T> runSafely(block: () -> T, defaultValue: T): T {
        return try {
            block()
        } catch (e: Exception) {
            setError(e)
            defaultValue
        }
    }
}

// 2. Créez un composant pour afficher les erreurs
@Composable
fun ErrorDialog(onDismiss: () -> Unit) {
    ErrorHandler.currentError.value?.let { error ->
        AlertDialog(
            onDismissRequest = onDismiss,
            backgroundColor = Color(0xFFfafaeb),
            title = { Text("Une erreur est survenue") },
            text = {
                Column {
                    Text("Une erreur est survenue dans l'application :")
                    Text(
                        text = error.message ?: "Erreur inconnue",
                        color = Color(0xFFb61431),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onDismiss,
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