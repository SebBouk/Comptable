package org.example.comptable.ui


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.example.comptable.repositorie.fetchOperationsForAccount
import org.ktorm.database.Database
import org.example.comptable.repositorie.*
import org.example.comptable.ui.components.AnimatedChartOperations
import org.example.comptable.ui.components.SearchBar
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter


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

    val numeroCompte = getNumeroCompte(database, accountId)
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

    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf("") }

    // Création des colonnes pour le tableau dynamique
    val columns = listOf("Date", "Montant", "Type", "Categorie", "Description")

    // Création des lignes originales pour le tableau dynamique
    val originalRows = operations.map { operation ->
        val operationType = if (operation.NatureOperation) "Entrée" else "Sortie"
        val montantFormatted = String.format("%.2f €", operation.PrixOperation)
        val dateFormatted = operation.DateOperation.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        val categorieName = categoriesMap[operation.IdCategorie] ?: "Non définie"

        listOf(
            operation.DateOperation.toString(),
            montantFormatted,
            operationType,
            categorieName,
            operation.CommentaireOperation ?: ""
        )
    }

    // Filtrage des lignes en fonction de la recherche et du filtre actif
    val filteredRows = remember(searchQuery, activeFilter, originalRows) {
        if (searchQuery.isEmpty() && activeFilter.isEmpty()) {
            originalRows
        } else {
            originalRows.filter { row ->
                // Logique de filtrage basée sur la recherche
                val matchesSearch = if (searchQuery.isEmpty()) {
                    true // Pas de filtre de recherche
                } else {
                    // Chercher dans toutes les colonnes
                    row.any { cell ->
                        cell?.contains(searchQuery, ignoreCase = true) == true
                    }
                }

                // Logique de filtrage basée sur le filtre actif
                val matchesFilter = when (activeFilter) {
                    "Date" -> row[0]?.contains(searchQuery, ignoreCase = true) == true
                    "Montant" -> row[1]?.contains(searchQuery, ignoreCase = true) == true
                    "Type" -> row[2]?.contains(searchQuery, ignoreCase = true) == true
                    "Categorie" -> row[3]?.contains(searchQuery, ignoreCase = true) == true
                    "Description" -> row[4]?.contains(searchQuery, ignoreCase = true) == true
                    else -> true // Pas de filtre spécifique
                }

                matchesSearch && matchesFilter
            }
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
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.9f)
                .background(Color(0xFFF5F5DC))
        ) {
            Column(
                modifier = Modifier.background(Color(0xFFf7f7e1)).padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Opérations du Compte $numeroCompte",
                    style = MaterialTheme.typography.h5,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Box contenant le graphique animé
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(200.dp)
                        .padding(8.dp)
                ) {
                    AnimatedChartOperations(
                        data = operations,
                        filteredRows =filteredRows,
                        categoriesMap = categoriesMap,
                        activeFilter = activeFilter,
                        searchQuery = searchQuery
                    )
                }

                // Barre de recherche pour les opérations
                SearchBar(
                    onSearch = { query ->
                        searchQuery = query
                    },
                    onFilterChange = { filter ->
                        activeFilter = filter
                    },
                    placeholder = "Rechercher une opération...",
                    filters = listOf("Date", "Montant", "Type", "Categorie", "Description"),
                    modifier = Modifier.fillMaxWidth()
                )

                // Tableau des opérations
                if (operations.isNotEmpty()) {
                    if (filteredRows.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Aucune opération ne correspond à votre recherche",
                                style = MaterialTheme.typography.body1,
                                color = Color.Gray
                            )
                        }
                    } else {
                        DynamicTable(
                            columns = columns,
                            rows = filteredRows,
                            sortableColumns = setOf(0, 1, 2, 3),
                            dateColumns = setOf(0),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            columnWidths = listOf(0.2f, 0.15f, 0.15f, 0.15f, 0.25f, 0.1f),
                            customCellAlignment = { colIndex ->
                                when (colIndex) {
                                    0 -> Alignment.Center // Date
                                    1 -> Alignment.Center // Montant
                                    2 -> Alignment.Center // Type
                                    3 -> Alignment.Center // Categorie
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
                                    else -> null
                                }
                            },
                            alternateRowColors = true,
                            showEditDeleteActions = true,
                            onEditRow = { rowIndex ->
                                // Trouver l'index dans les opérations originales
                                val operationIndex = operations.indexOfFirst { operation ->
                                    val operationType = if (operation.NatureOperation) "Entrée" else "Sortie"
                                    val dateStr = operation.DateOperation.toString()
                                    val commentaire = operation.CommentaireOperation ?: ""
                                    val categorieName = categoriesMap[operation.IdCategorie] ?: "Non définie"

                                    filteredRows[rowIndex][0] == dateStr &&
                                            filteredRows[rowIndex][2] == operationType &&
                                            filteredRows[rowIndex][3] == categorieName &&
                                            filteredRows[rowIndex][4] == commentaire
                                }
                                if (operationIndex != -1) {
                                    selectedOperationIndex = operationIndex
                                    selectedOperation = operations[operationIndex]
                                    showEditOperationDialog = true
                                }
                            },
                            onDeleteRow = { rowIndex ->
                                // Trouver l'index dans les opérations originales
                                val operationIndex = operations.indexOfFirst { operation ->
                                    val operationType = if (operation.NatureOperation) "Entrée" else "Sortie"
                                    val dateStr = operation.DateOperation.toString()
                                    val commentaire = operation.CommentaireOperation ?: ""
                                    val categorieName = categoriesMap[operation.IdCategorie] ?: "Non définie"

                                    filteredRows[rowIndex][0] == dateStr &&
                                            filteredRows[rowIndex][2] == operationType &&
                                            filteredRows[rowIndex][3] == categorieName &&
                                            filteredRows[rowIndex][4] == commentaire
                                }
                                if (operationIndex != -1) {
                                    selectedOperationIndex = operationIndex
                                    selectedOperation = operations[operationIndex]
                                    showDeleteConfirmationDialog = true
                                }
                            }
                        )
                    }
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
                        .padding(top = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF1dbc7c),
                        contentColor = Color(0xfff5f5dc)
                    )
                ) {
                    Text("Ajouter une opération")
                }
            }
        }

        // Dialogue d'ajout d'opération
        if (showAddOperationDialog) {
            AddOperationDialog(
                database = database,
                accountId = accountId,
                onDismiss = { showAddOperationDialog = false },
                onOperationAdded = {
                    refreshTrigger++
                    showAddOperationDialog = false
                }
            )
        }

        // Dialogue d'édition d'opération
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
                    OutlinedButton(
                        onClick = { showDeleteConfirmationDialog = false },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF1dbc7c),
                            contentColor = Color(0xfff5f5dc)
                        )
                    ) {
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
                        focusedLabelColor = Color(0xff1dbc7c)
                    )
                )

                // Sélection de la date
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF1dbc7c),
                        contentColor = Color(0xfff5f5dc)
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
                        backgroundColor = Color(0xFF1dbc7c),
                        contentColor = Color(0xfff5f5dc)
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Heure: ${selectedTime.format(timeFormatter)}")
                        Icon(
                            imageVector = Icons.Default.DateRange,
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
                        backgroundColor = Color(0xFF1dbc7c),
                        contentColor = Color(0xfff5f5dc)
                    )
                ) {
                    Text(
                        text = categories.find { it.first == selectedCategorieId }?.second ?: "Sélectionner une catégorie"
                    )
                }

                DropdownMenu(
                    expanded = expandedCategorie,
                    onDismissRequest = { expandedCategorie = false },
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFfafaeb))
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
                        focusedLabelColor = Color(0xff1dbc7c)
                    )
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
                    backgroundColor = Color(0xFF1dbc7c),
                    contentColor = Color(0xfff5f5dc)
                )
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFFb61431),
                    contentColor = Color(0xfff5f5dc)
                )
            ) {
                Text("Annuler")
            }
        }
    )

    // Dialogue d'ajout de catégorie
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
                        cursorColor = Color(0xff1dbc7c)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCategorieName.isNotBlank()) {
                            val newId = addCategorie(database, newCategorieName)
                            selectedCategorieId = newId
                            refreshTrigger++
                            newCategorieName = ""
                            showAddCategorieDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF1dbc7c),
                        contentColor = Color(0xfff5f5dc)
                    )
                ) {
                    Text("Ajouter")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showAddCategorieDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFFb61431),
                        contentColor = Color(0xfff5f5dc)
                    )
                ) {
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

    // Dialogue de confirmation de suppression de catégorie
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
                            refreshTrigger++
                        }
                        showDeleteCategorieDialog = false
                        categorieToDeleteId = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFFb61431),
                        contentColor = Color(0xfff5f5dc)
                    )
                ) {
                    Text("Supprimer", color = Color.White)
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showDeleteCategorieDialog = false
                        categorieToDeleteId = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF1dbc7c),
                        contentColor = Color(0xfff5f5dc)
                    )
                ) {
                    Text("Annuler")
                }
            }
        )
    }
}