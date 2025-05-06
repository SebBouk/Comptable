package org.example.comptable.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.unit.sp
import org.example.comptable.repositorie.OperationEntity
import java.text.NumberFormat
import java.util.*

@Composable
fun AnimatedChartOperations(
    data: List<OperationEntity>,
    filteredRows: List<List<String?>>, // Paramètre pour les lignes filtrées du tableau
    categoriesMap: Map<Int, String?>, // Carte des catégories pour la correspondance
    activeFilter: String,
    searchQuery: String
) {
    // Correspondre entre les lignes filtrées et les opérations complètes
    val filteredData = remember(data, filteredRows) {
        // Si filteredRows est vide mais que la recherche est active, on retourne une liste vide
        if (filteredRows.isEmpty() && (searchQuery.isNotEmpty() || activeFilter.isNotEmpty())) {
            emptyList()
        } else if (filteredRows.isEmpty() || (searchQuery.isEmpty() && activeFilter.isEmpty())) {
            // Si pas de filtre actif ou pas de lignes du tout, utiliser toutes les données
            data
        } else {
            // Faire correspondre les opérations avec les lignes filtrées
            data.filter { operation ->
                filteredRows.any { row ->
                    val operationType = if (operation.NatureOperation) "Entrée" else "Sortie"
                    val dateStr = operation.DateOperation.toString()
                    val categorieName = categoriesMap[operation.IdCategorie] ?: "Non définie"
                    val commentaire = operation.CommentaireOperation ?: ""

                    // Vérifier si cette opération correspond à une ligne filtrée
                    row[0] == dateStr &&
                            row[2] == operationType &&
                            row[3] == categorieName &&
                            row[4] == commentaire
                }
            }
        }
    }

    // Calcul du solde filtré
    val totalBalance = filteredData.fold(0.0) { acc, operation ->
        if (operation.NatureOperation) acc + operation.PrixOperation.toDouble()
        else acc - operation.PrixOperation.toDouble()
    }

    // Animation du solde
    val animatedBalance = animateFloatAsState(
        targetValue = totalBalance.toFloat(),
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing)
    )

    // Animation du nombre d'opérations
    val animatedSize = animateFloatAsState(
        targetValue = filteredData.size.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    // Création du graphique
    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val radius = minOf(canvasWidth, canvasHeight) / 2 * 1f
        val center = Offset(canvasWidth / 2, canvasHeight / 2)
        val strokeWidth = 30f

        // Dessin d'un arc de fond
        drawArc(
            color = Color.LightGray.copy(alpha = 0.2f),
            startAngle = -180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )

        // Seulement dessiner l'arc animé s'il y a des opérations
        if (filteredData.isNotEmpty()) {
            // Arc coloré qui représente le solde animé
            val referenceValue = 5000.0  // Valeur de référence pour l'angle maximum
            val ratio = (animatedBalance.value / referenceValue).coerceIn(-1.0, 1.0)
            val sweepAngle = (ratio * 180).toFloat()

            val arcColor = if (animatedBalance.value >= 0) Color(0xFF1dbc7c) else Color(0xFFb61431)

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

    // Solde au centre
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val formatter = NumberFormat.getCurrencyInstance(Locale.FRANCE)
        val soldeFormate = formatter.format(animatedBalance.value)
        val textColor = if (animatedBalance.value >= 0) Color(0xFF1dbc7c) else Color(0xFFb61431)

        Text(
            text = soldeFormate,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )

        Text(
            text = if (activeFilter.isEmpty() && searchQuery.isEmpty()) "Solde" else "Solde filtré",
            fontSize = 16.sp,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
        )

        // Afficher le nombre d'opérations
        Text(
            text = "${filteredData.size} opération(s)",
            fontSize = 14.sp,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
        )
    }
}