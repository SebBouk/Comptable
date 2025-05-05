package org.example.comptable.ui


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.comptable.repositorie.fetchOperationsForAccount
import org.ktorm.database.Database
import org.example.comptable.repositorie.AddOperationDialog
import java.time.format.DateTimeFormatter
import org.example.comptable.repositorie.*
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*


@Composable
fun CompteScreen(database: Database, accountId: Int) {
    println("CompteScreen called with account ID: $accountId")

    var refreshTrigger by remember { mutableStateOf(0) }

    var showAddOperationDialog by remember { mutableStateOf(false) }
    var showEditOperationDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }

    // État pour stocker l'opération en cours d'édition ou de suppression
    var selectedOperationIndex by remember { mutableStateOf(-1) }
    var selectedOperation by remember { mutableStateOf<org.example.comptable.repositorie.OperationEntity?>(null) }

    // Récupérer les opérations du compte
    val operations = fetchOperationsForAccount(database, accountId)
    println("Operations in CompteScreen: $operations")

    val categoriesMap = remember(refreshTrigger) {
        fetchCategories(database).associate { it.first to it.second }
    }

    // Calcul du solde total
    val solde = operations.fold(0.0) { acc, operation ->
        if (operation.NatureOperation) { // si entrée (true)
            acc + operation.PrixOperation.toDouble()
        } else { // si sortie (false)
            acc - operation.PrixOperation.toDouble()
        }
    }


    val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(0.8f)
                .fillMaxHeight(0.8f)
                .background(Color(0xFFF5F5DC))
        ) {
            Column(
                modifier = Modifier.background(Color(0xFFf7f7e1)).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Opérations du Compte",
                    style = MaterialTheme.typography.h5,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(200.dp)
                        .padding(8.dp)
                ) {
                    // Définir la couleur en fonction du solde
                    val circleColor = if (solde >= 0) Color(0xFF1dbc7c) else Color(0xFFb61431)
                    val textColor = if (solde >= 0) Color(0xFF1dbc7c) else Color(0xFFb61431)

                    // Formatage du solde
                    val formatter = NumberFormat.getCurrencyInstance(Locale.FRANCE)
                    val soldeFormate = formatter.format(solde)

                    // Récupérer les informations du compte
                    val compte = org.example.comptable.ui.components.fetchComptesData(database, accountId)

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val radius = minOf(canvasWidth, canvasHeight) / 2 * 1f
                        val center = Offset(canvasWidth / 2, canvasHeight / 2)
                        val strokeWidth = 30f

                        // Dessin d'un arc au lieu d'un cercle complet
                        // L'arc commence à -150 degrés et couvre 300 degrés
                        drawArc(
                            color = Color.LightGray.copy(alpha = 0.2f),
                            startAngle = -180f,
                            sweepAngle = 180f,
                            useCenter = false,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                            style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        )

                        // Arc coloré qui représente le solde
                        // Utilise un angle proportionnel au solde par rapport à une valeur de référence (par exemple, 5000€)
                        // Pour un solde négatif, l'angle est négatif, sinon positif
                        val referenceValue = 5000.0  // Valeur de référence pour l'angle maximum
                        val ratio = (solde / referenceValue).coerceIn(-1.0, 1.0)  // Limiter entre -1 et 1
                        val sweepAngle = (ratio * 180).toFloat()  // Angle proportionnel au ratio

                        drawArc(
                            color = circleColor,
                            startAngle = -180f,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                            style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        )
                    }

                    // Texte au centre de l'arc
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Affichage du numéro de compte

                        // Affichage du solde
                        Text(
                            text = soldeFormate,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )

                        // Affichage du texte "Solde"
                        Text(
                            text = "Solde",
                            fontSize = 16.sp,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                // Création des colonnes et des lignes pour le tableau dynamique
                val columns = listOf("Date", "Montant", "Type", "Categorie", "Description")
                val rows = operations.map { operation ->
                    val operationType = if (operation.NatureOperation) "Entrée" else "Sortie"
                    val montantFormatted = String.format("%.2f €", operation.PrixOperation)
                    val dateFormatted = operation.DateOperation.format(dateTimeFormatter)
                    val categorieName = categoriesMap[operation.IdCategorie] ?: "Non définie"

                    listOf(
                        operation.DateOperation.toString(),
                        montantFormatted,
                        operationType,
                        categorieName,
                        operation.CommentaireOperation ?: ""
                    )
                }

                // Utilisation du composant DynamicTable
                if (rows.isNotEmpty()) {
                    DynamicTable(
                        columns = columns,
                        rows = rows,
                        sortableColumns = setOf(0,1,2,3),
                        dateColumns = setOf(0),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        columnWidths = listOf(0.2f, 0.15f, 0.15f,0.15f, 0.25f, 0.1f),
                        customCellAlignment = { colIndex ->
                            when (colIndex) {
                                0 -> Alignment.Center // Date
                                1 -> Alignment.Center // Montant
                                2 -> Alignment.Center // Type
                                3 -> Alignment.Center //Categorie
                                else -> Alignment.CenterStart // Description
                            }
                        },
                        customCellColorsByValue = { value, colIndex ->
                            when (colIndex) {
                                2 -> { // Colonne "Type"
                                    when (value) {
                                        "Entrée" -> Color(0xFF1dbc7c)
                                        "Sortie" -> Color(0xFFb61431)
                                        else -> null
                                    }
                                }
                                1 -> { // Colonne "Montant"
                                    // On ne connaît pas le type ici, donc on doit passer par un autre moyen
                                    null // À compléter
                                }
                                else -> null
                            }
                        },
                        alternateRowColors = true,
                        showEditDeleteActions = true,
                        onEditRow = { rowIndex ->
                            selectedOperationIndex = rowIndex
                            selectedOperation = operations[rowIndex]
                            showEditOperationDialog = true
                        },
                        onDeleteRow = { rowIndex ->
                            selectedOperationIndex = rowIndex
                            selectedOperation = operations[rowIndex]
                            showDeleteConfirmationDialog = true
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Aucune opération pour ce compte")
                    }
                }

                // Bouton pour ajouter une nouvelle opération
                Button(
                    onClick = { showAddOperationDialog = true },
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF1dbc7c), // Couleur de fond du bouton
                        contentColor = Color(0xfff5f5dc)         // Couleur du texte (et icône)
                    )
                ) {
                    Text("Ajouter une opération")
                }
            }
        }

        // Afficher le dialogue d'ajout d'opération si demandé
        if (showAddOperationDialog) {
            AddOperationDialog(
                database = database,
                accountId = accountId,
                onDismiss = { showAddOperationDialog = false },
                onOperationAdded = {
                    // Incrémenter le trigger pour forcer une actualisation
                    refreshTrigger++
                }
            )
        }
        if (showEditOperationDialog && selectedOperation != null) {
            EditOperationDialog(
                database = database,
                operation = selectedOperation!!,
                onDismiss = { showEditOperationDialog = false },
                onOperationUpdated = {
                    refreshTrigger++
                    showEditOperationDialog = false
                }
            )
        }

        // Dialogue de confirmation de suppression
        if (showDeleteConfirmationDialog && selectedOperation != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmationDialog = false },
                backgroundColor = Color(0xFFfafaeb),
                title = { Text("Confirmer la suppression") },
                text = { Text("Êtes-vous sûr de vouloir supprimer cette opération ?") },
                confirmButton = {
                    Button(
                        onClick = {
                            deleteOperation(database, selectedOperation!!.IdOperation)
                            refreshTrigger++
                            showDeleteConfirmationDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error)
                    ) {
                        Text("Supprimer", color = Color.White)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showDeleteConfirmationDialog = false },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF1dbc7c), // Couleur de fond du bouton
                            contentColor = Color(0xfff5f5dc)         // Couleur du texte (et icône)
                        )) {
                        Text("Annuler")
                    }
                }
            )
        }
    }
}

@Composable
fun EditOperationDialog(
    database: Database,
    operation: org.example.comptable.repositorie.OperationEntity,
    onDismiss: () -> Unit,
    onOperationUpdated: () -> Unit
) {
    var refreshTrigger by remember { mutableStateOf(0) }

    // Copie de l'opération pour l'édition
    var commentaire by remember { mutableStateOf(operation.CommentaireOperation) }
    var montant by remember { mutableStateOf(operation.PrixOperation.toString()) }
    var isEntree by remember { mutableStateOf(operation.NatureOperation) }
    var selectedCategorieId by remember { mutableStateOf(operation.IdCategorie) }

    var showAddCategorieDialog by remember { mutableStateOf(false) }
    var newCategorieName by remember { mutableStateOf("") }

    var selectedDate by remember { mutableStateOf(LocalDate.from(operation.DateOperation)) }
    var selectedTime by remember { mutableStateOf(LocalTime.from(operation.DateOperation)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    var categorieToDeleteId by remember { mutableStateOf<Int?>(null) }
    var showDeleteCategorieDialog by remember { mutableStateOf(false) }

    // Récupérer la liste des catégories
    val categories = remember(refreshTrigger) { fetchCategories(database) }

    AlertDialog(
        onDismissRequest = onDismiss,
        backgroundColor = Color(0xFFfafaeb),
        title = { Text("Modifier l'opération") },
        text = {
            Column(modifier = Modifier.padding(8.dp)) {
                // Champ pour le montant
                TextField(
                    value = montant,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                            montant = it
                        }
                    },
                    label = { Text("Montant (€)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF1dbc7c),
                        focusedLabelColor = Color(0xff1dbc7c))
                )
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF1dbc7c), // Couleur de fond du bouton
                        contentColor = Color(0xfff5f5dc)         // Couleur du texte (et icône)
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Date: ${selectedDate.format(dateFormatter)}")
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Sélectionner une date"
                        )
                    }
                }

                // Sélection de l'heure
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF1dbc7c), // Couleur de fond du bouton
                        contentColor = Color(0xfff5f5dc)         // Couleur du texte (et icône)
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Heure: ${selectedTime.format(timeFormatter)}")
                        Icon(
                            imageVector = Icons.Default.DateRange, // Utilisez une icône d'horloge si disponible
                            contentDescription = "Sélectionner une heure"
                        )
                    }
                }
                // Type d'opération (entrée/sortie)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Type : ")
                    RadioButton(
                        selected = isEntree,
                        onClick = { isEntree = true },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color(0xFF1dbc7c)
                        )
                    )
                    Text("Entrée", Modifier.padding(end = 16.dp))
                    RadioButton(
                        selected = !isEntree,
                        onClick = { isEntree = false },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color(0xFF1dbc7c)
                        )
                    )
                    Text("Sortie")
                }

                // Sélection de catégorie
                var expandedCategorie by remember { mutableStateOf(false) }

                Text("Catégorie", modifier = Modifier.padding(vertical = 4.dp))
                OutlinedButton(
                    onClick = { expandedCategorie = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF1dbc7c), // Couleur de fond du bouton
                        contentColor = Color(0xfff5f5dc)         // Couleur du texte (et icône)
                    )
                ) {
                    Text(
                        text = categories.find { it.first == selectedCategorieId }?.second ?: "Sélectionner une catégorie"
                    )
                }

                DropdownMenu(
                    expanded = expandedCategorie,
                    onDismissRequest = { expandedCategorie = false },
                    modifier = Modifier.fillMaxWidth().background( Color(0xFFfafaeb))
                ) {
                    categories.forEach { (id, name) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DropdownMenuItem(
                                onClick = {
                                    selectedCategorieId = id
                                    expandedCategorie = false
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = name ?: "")
                            }
                            IconButton(
                                onClick = {
                                    // Afficher une confirmation avant de supprimer
                                    showDeleteCategorieDialog = true
                                    categorieToDeleteId = id
                                },
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Supprimer",
                                    tint = MaterialTheme.colors.error
                                )
                            }
                        }
                    }
                    Divider()
                    DropdownMenuItem(
                        onClick = {
                            expandedCategorie = false
                            showAddCategorieDialog = true
                        }
                    ) {
                        Text(
                            text = "+ Ajouter une nouvelle catégorie",
                            color = Color(0xff1bdc7c)
                        )
                    }
                }

                // Champ pour le commentaire
                TextField(
                    value = commentaire,
                    onValueChange = { commentaire = it },
                    label = { Text("Commentaire") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF1dbc7c),
                        focusedLabelColor = Color(0xff1dbc7c))
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (montant.isNotBlank() && selectedCategorieId > 0) {
                        val updatedOperation = operation.copy(
                            CommentaireOperation = commentaire,
                            PrixOperation = BigDecimal(montant),
                            NatureOperation = isEntree,
                            IdCategorie = selectedCategorieId,
                            DateOperation = LocalDateTime.of(selectedDate, selectedTime)
                        )
                        updateOperation(database, updatedOperation)
                        onOperationUpdated()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF1dbc7c), // Couleur de fond du bouton
                    contentColor = Color(0xfff5f5dc)         // Couleur du texte (et icône)
                )
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFFb61431), // Couleur de fond du bouton
                    contentColor = Color(0xfff5f5dc)         // Couleur du texte (et icône)
                )) {
                Text("Annuler")
            }
        }
    )
    if (showAddCategorieDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategorieDialog = false },
            backgroundColor = Color(0xFFfafaeb),
            title = { Text("Ajouter une catégorie") },
            text = {
                TextField(
                    value = newCategorieName,
                    onValueChange = { newCategorieName = it },
                    label = { Text("Nom de la catégorie") },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF1dbc7c),
                        focusedLabelColor = Color(0xff1dbc7c),
                        cursorColor = Color(0xff1dbc7c) ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCategorieName.isNotBlank()) {
                            // Ajouter la nouvelle catégorie à la base de données
                            val newId = addCategorie(database, newCategorieName)
                            // Sélectionner la nouvelle catégorie
                            selectedCategorieId = newId
                            refreshTrigger++
                            // Réinitialiser et fermer le dialogue
                            newCategorieName = ""
                            showAddCategorieDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF1dbc7c), // Couleur de fond du bouton
                        contentColor = Color(0xfff5f5dc)         // Couleur du texte (et icône)
                    )
                ) {
                    Text("Ajouter")
                }
            },
            dismissButton = {
                Button(onClick = { showAddCategorieDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFFb61431), // Couleur de fond du bouton
                        contentColor = Color(0xfff5f5dc)         // Couleur du texte (et icône)
                    )) {
                    Text("Annuler")
                }
            }
        )
    }
    // Dialogue pour sélectionner la date
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            onDateSelected = {
                selectedDate = it
                showDatePicker = false
            },
            initialDate = selectedDate
        )
    }

    // Dialogue pour sélectionner l'heure
    if (showTimePicker) {
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            onTimeSelected = {
                selectedTime = it
                showTimePicker = false
            },
            initialTime = selectedTime
        )
    }
    if (showDeleteCategorieDialog && categorieToDeleteId != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteCategorieDialog = false
                categorieToDeleteId = null
            },
            backgroundColor = Color(0xFFfafaeb),
            title = { Text("Confirmer la suppression") },
            text = {
                Text(
                    "Êtes-vous sûr de vouloir supprimer cette categorie ? Cette action est irréversible." +
                            "\n\nNote: La suppression sera impossible si des comptes sont liés à cette categorie."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val result = deleteCategorie(database, categorieToDeleteId!!)
                        if (result) {
                            // Suppression réussie
                            refreshTrigger++
                        } else {
                            // Afficher un message d'erreur (idéalement avec un Snackbar)
                            // Vous pouvez ajouter un état pour gérer ces messages
                        }
                        showDeleteCategorieDialog = false
                        categorieToDeleteId = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFFb61431), // Couleur de fond du bouton
                        contentColor = Color(0xfff5f5dc)         // Couleur du texte (et icône)
                    )
                ) {
                    Text("Supprimer", color = Color.White)
                }
            },
            dismissButton = {
                Button(onClick = {
                    showDeleteCategorieDialog = false
                    categorieToDeleteId = null
                },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF1dbc7c), // Couleur de fond du bouton
                        contentColor = Color(0xfff5f5dc)         // Couleur du texte (et icône)
                    )) {
                    Text("Annuler")
                }
            }
        )
    }
}


