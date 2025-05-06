package org.example.comptable.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ktorm.database.Database
import java.text.NumberFormat
import java.util.*

/**
 * Modèle de données pour représenter un compte
 */
data class AccountData(
    val id: Int,
    val name: String,
    val balance: Double,
    val color: Color,
    val isIncome: Boolean,
    val etablissement: String = "",
    val type: String = ""
)

fun List<AccountData>.sumOfBalances(): Double {
    var sum = 0.0
    for (account in this) {
        sum += account.balance
    }
    return sum
}

/**
 * Fonction pour convertir les données du repository en objets AccountData
 */
fun fetchComptesData(database: Database, userId: Int): List<AccountData> {
    try {
        // Récupérer les données depuis la fonction dans repositorie
        val (columns, rows) = org.example.comptable.repositorie.fetchComptesDataForChart(database, userId)

        println("Nombre de colonnes: ${columns.size}")
        println("Colonnes: $columns")
        println("Nombre de lignes: ${rows.size}")

        // Vérifier si rows est vide
        if (rows.isEmpty()) {
            println("Aucune donnée de compte récupérée")
            return emptyList()
        }

        // Convertir les lignes en AccountData
        return rows.mapIndexed { index, row ->
            println("Traitement de la ligne $index: $row")

            // Extraire l'ID et le nom
            val id = row[0]?.toIntOrNull() ?: -1
            val name = row[1] ?: "Compte inconnu"

            // Conversion du solde avec approche plus robuste
            val soldeStr = row[3] ?: "0,00 €"
            println("Conversion du solde pour le compte $id ($name): '$soldeStr'")

            val solde = try {
                // Méthode plus robuste pour extraire uniquement les chiffres, le signe et le point décimal
                val cleanedStr = soldeStr.let {
                    // Déterminer si le nombre est négatif
                    val isNegative = it.contains("-")

                    // Supprimer tous les caractères non numériques sauf la virgule/point
                    var cleaned = it.filter { char ->
                        char.isDigit() || char == ',' || char == '.'
                    }

                    // Remplacer la virgule par un point pour le format décimal
                    cleaned = cleaned.replace(',', '.')

                    // Ajouter le signe négatif si nécessaire
                    if (isNegative) "-$cleaned" else cleaned
                }

                println("Chaîne après nettoyage complet: '$cleanedStr'")

                // Conversion en nombre
                val parsedValue = cleanedStr.toDoubleOrNull() ?: 0.0
                println("Valeur du solde convertie: $parsedValue")
                parsedValue
            } catch (e: Exception) {
                println("ERREUR lors de la conversion du solde: ${e.message}")
                println("Trace complète: ${e.stackTraceToString()}")
                0.0
            }

            val isIncome = solde >= 0
            val color = if (isIncome) Color(0xFF1dbc7c) else Color(0xFFb61431)

            val account = AccountData(id, name, solde, color, isIncome)
            println("Compte créé: ID=$id, Nom=$name, Solde=$solde")
            account
        }
    } catch (e: Exception) {
        println("ERREUR CRITIQUE dans fetchComptesData: ${e.message}")
        e.printStackTrace()
        return emptyList()
    }
}

/**
 * Composant principal qui affiche un graphique en arc représentant tous les comptes
 */
@Composable
fun AccountsArcChart(
    accounts: List<AccountData>,
    onAccountClick: (AccountData) -> Unit = {}
) {
    // Calcul du solde total des comptes filtrés
    val totalBalance = accounts.sumOfBalances()

    // Animation du solde
    val animatedBalance = animateFloatAsState(
        targetValue = totalBalance.toFloat(),
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing)
    )

    // Déterminer la couleur en fonction du solde
    val arcColor = if (animatedBalance.value >= 0) Color(0xFF1dbc7c) else Color(0xFFb61431)
    val textColor = if (animatedBalance.value >= 0) Color(0xFF1dbc7c) else Color(0xFFb61431)

    // Formater le solde avec le symbole € et séparateur de milliers
    val formatter = NumberFormat.getCurrencyInstance(Locale.FRANCE)
    val soldeFormate = formatter.format(animatedBalance.value)

    // Affichage de l'arc avec le montant total
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(250.dp)
            .padding(16.dp)
            .clickable {
                // Si un seul compte est filtré, on le sélectionne
                if (accounts.size == 1) {
                    onAccountClick(accounts[0])
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val radius = minOf(canvasWidth, canvasHeight) / 2 * 1f
            val center = Offset(canvasWidth / 2, canvasHeight / 2)
            val strokeWidth = 30f

            // Arc de fond gris clair
            drawArc(
                color = Color.LightGray.copy(alpha = 0.2f),
                startAngle = -180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )

            // Arc coloré représentant le solde (seulement s'il y a des comptes)
            if (accounts.isNotEmpty()) {
                val referenceValue = 5000.0  // Valeur de référence pour l'angle maximum
                val ratio = (animatedBalance.value / referenceValue).coerceIn(-1.0, 1.0)  // Limiter entre -1 et 1
                val sweepAngle = (ratio * 180).toFloat()  // Angle proportionnel au ratio

                drawArc(
                    color = arcColor,
                    startAngle = -180f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
            }
        }

        // Texte au centre du cercle
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Affichage du solde
            Text(
                text = soldeFormate,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            // Affichage du texte "Solde total" ou "Solde filtré"
            Text(
                text = if (accounts.size == accounts.sumOfBalances().toInt()) "Solde total" else "Solde filtré",
                fontSize = 16.sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )

            // Afficher le nombre de comptes
            Text(
                text = "${accounts.size} compte(s)",
                fontSize = 14.sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}