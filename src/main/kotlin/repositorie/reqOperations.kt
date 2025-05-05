package org.example.comptable.repositorie

import androidx.compose.foundation.background
import org.example.comptable.ktorm.Operations
import org.ktorm.database.Database
import org.ktorm.dsl.*
import java.math.BigDecimal
import java.time.LocalDateTime
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import org.example.comptable.ktorm.*
import org.ktorm.dsl.insert
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class OperationEntity(
    val IdOperation: Int,
    val CommentaireOperation: String,
    val PrixOperation: BigDecimal,
    val NatureOperation: Boolean,
    val DateOperation: LocalDateTime,
    val IdCompte: Int,
    val IdCategorie: Int
)

fun fetchOperationsForAccount(database: Database, accountId: Int): List<OperationEntity> {
    println("Fetching operations for account ID: $accountId")
    val operations = database
        .from(Operations)
        .select()
        .where { Operations.IdCompte eq accountId }
        .map { row ->
            OperationEntity(
                IdOperation = row[Operations.IdOperation]!!,
                CommentaireOperation = row[Operations.CommentaireOperation]!!,
                PrixOperation = row[Operations.PrixOperation]!!,
                NatureOperation = row[Operations.NatureOperation]!!,
                DateOperation = row[Operations.DateOperation]!!,
                IdCompte = row[Operations.IdCompte]!!,
                IdCategorie = row[Operations.IdCategorie]!!
            )
        }
    println("Operations fetched: $operations") // Ajoutez ce log
    return operations
}


@Composable
fun AddOperationDialog(
    database: Database,
    accountId: Int,
    onDismiss: () -> Unit,
    onOperationAdded: () -> Unit
) {
    var refreshTrigger by remember { mutableStateOf(0) }
    // États pour les champs du formulaire
    var montant by remember { mutableStateOf("") }
    var commentaire by remember { mutableStateOf("") }
    var isEntree by remember { mutableStateOf(true) }
    var selectedCategorieId by remember { mutableStateOf<Int?>(null) }
    var showAddCategorieDialog by remember { mutableStateOf(false) }
    var newCategorieName by remember { mutableStateOf("") }
    var expandedCategorie by remember { mutableStateOf(false) }

    // États pour la date et l'heure
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedTime by remember { mutableStateOf(LocalTime.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Formatage de la date et de l'heure pour l'affichage
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    var categorieToDeleteId by remember { mutableStateOf<Int?>(null) }
    var showDeleteCategorieDialog by remember { mutableStateOf(false) }

    // Récupérer la liste des catégories
    var categories = remember(refreshTrigger) { fetchCategories(database) }

    Dialog(onDismissRequest = onDismiss) {

            Column(
                modifier = Modifier
                    .background(Color(0xFFfafaeb))
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()) // Permet de faire défiler si le contenu est trop grand
            ) {
                Text(
                    text = "Ajouter une opération",
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Champ pour le montant
                TextField(
                    value = montant,
                    onValueChange = {
                        // Accepter uniquement les chiffres et le point décimal
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                            montant = it
                        }
                    },
                    label = { Text("Montant (€)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF1dbc7c),
                        focusedLabelColor = Color(0xff1dbc7c))
                )

                // Sélection du type d'opération (entrée/sortie)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Type d'opération: ", modifier = Modifier.padding(end = 8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = isEntree,
                            onClick = { isEntree = true },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFF1dbc7c)
                            )
                        )
                        Text("Entrée", modifier = Modifier.padding(start = 4.dp, end = 16.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = !isEntree,
                            onClick = { isEntree = false },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFF1dbc7c)
                            )
                        )
                        Text("Sortie", modifier = Modifier.padding(start = 4.dp))
                    }
                }

                // Sélection de la date
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

                // Sélection de la catégorie
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text("Catégorie", style = MaterialTheme.typography.body1)

                    OutlinedButton(
                        onClick = { expandedCategorie = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF1dbc7c), // Couleur de fond du bouton
                            contentColor = Color(0xfff5f5dc)         // Couleur du texte (et icône)
                        )

                    ) {
                        Text(
                            text = selectedCategorieId?.let { id ->
                                categories.find { it.first == id }?.second ?: "Sélectionner"
                            } ?: "Sélectionner une catégorie",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                    }

                    DropdownMenu(
                        expanded = expandedCategorie,
                        onDismissRequest = { expandedCategorie = false },
                        modifier = Modifier.fillMaxWidth(0.9f).background(Color(0xFFfafaeb)),
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

                        // Option pour ajouter une nouvelle catégorie
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
                }

                // Champ pour le commentaire
                TextField(
                    value = commentaire,
                    onValueChange = { commentaire = it },
                    label = { Text("Commentaire (optionnel)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF1dbc7c),
                        focusedLabelColor = Color(0xff1dbc7c))
                )

                // Boutons d'action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFFb61431), // Couleur de fond du bouton
                            contentColor = Color(0xfff5f5dc)         // Couleur du texte (et icône)
                        )
                    ) {
                        Text("Annuler")
                    }
                    Button(
                        onClick = {
                            if (montant.isNotBlank() && selectedCategorieId != null) {
                                try {
                                    // Combiner date et heure en un LocalDateTime
                                    val dateTime = LocalDateTime.of(selectedDate, selectedTime)

                                    // Ajouter l'opération à la base de données
                                    addOperation(
                                        database = database,
                                        commentaire = commentaire,
                                        prix = BigDecimal(montant),
                                        isEntree = isEntree,
                                        date = dateTime,
                                        idCompte = accountId,
                                        idCategorie = selectedCategorieId!!
                                    )
                                    // Notifier que l'opération a été ajoutée
                                    onOperationAdded()
                                    // Fermer le dialogue
                                    onDismiss()
                                } catch (e: Exception) {
                                    println("Erreur lors de l'ajout de l'opération: ${e.message}")
                                }
                            } else {
                                println("Veuillez remplir tous les champs obligatoires.")
                            }
                        },
                        enabled = montant.isNotBlank() && selectedCategorieId != null,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF1dbc7c), // Couleur de fond du bouton
                            contentColor = Color(0xfff5f5dc)         // Couleur du texte (et icône)
                        )
                    ) {
                        Text("Ajouter")
                    }
                }
            }

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

    // Dialogue pour ajouter une nouvelle catégorie
    if (showAddCategorieDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategorieDialog = false },
            title = { Text("Ajouter une catégorie") },
            backgroundColor = Color(0xFFfafaeb),
            text = {
                TextField(
                    value = newCategorieName,
                    onValueChange = { newCategorieName = it },
                    label = { Text("Nom de la catégorie") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF1dbc7c),
                        focusedLabelColor = Color(0xff1dbc7c))
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
    if (showDeleteCategorieDialog && categorieToDeleteId != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteCategorieDialog = false
                categorieToDeleteId = null
            },
            title = { Text("Confirmer la suppression") },
            backgroundColor = Color(0xFFfafaeb),
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
                        backgroundColor = Color(0xFF1dbc7c), // Couleur de fond du bouton
                        contentColor = Color(0xfff5f5dc)         // Couleur du texte (et icône)
                    )                ) {
                    Text("Supprimer", color = Color.White)
                }
            },
            dismissButton = {
                Button(onClick = {
                    showDeleteCategorieDialog = false
                    categorieToDeleteId = null
                },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFFb61431), // Couleur de fond du bouton
                        contentColor = Color(0xfff5f5dc)         // Couleur du texte (et icône)
                    )) {
                    Text("Annuler")
                }
            }
        )
    }
}

// Composant pour sélectionner une date
@Composable
fun DatePickerDialog(
    onDismissRequest: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    initialDate: LocalDate = LocalDate.now()
) {
    var selectedYear by remember { mutableStateOf(initialDate.year) }
    var selectedMonth by remember { mutableStateOf(initialDate.monthValue) }
    var selectedDay by remember { mutableStateOf(initialDate.dayOfMonth) }

    var yearText by remember { mutableStateOf(selectedYear.toString()) }
    var monthText by remember { mutableStateOf(selectedMonth.toString()) }
    var dayText by remember { mutableStateOf(selectedDay.toString()) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Sélectionner une date") },
        backgroundColor = Color(0xFFfafaeb),
        text = {
            Column {
                // Sélection de l'année
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Année:", modifier = Modifier.width(80.dp))
                    OutlinedTextField(
                        value = yearText,
                        onValueChange = {input ->
                            yearText = input // Toujours mettre à jour le texte affiché

                            // Ne mettre à jour la valeur que si c'est un nombre valide
                            val year = input.toIntOrNull()
                            if (year != null && year in 1900..2100) {
                                selectedYear = year
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFF1dbc7c),
                            focusedLabelColor = Color(0xff1dbc7c))
                    )
                }

                // Sélection du mois
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Mois:", modifier = Modifier.width(80.dp))
                    OutlinedTextField(
                        value = monthText,
                        onValueChange = {input ->
                            monthText = input
                            val month = input.toIntOrNull()
                            if (month != null && month in 1..12) {
                                selectedMonth = month
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFF1dbc7c),
                            focusedLabelColor = Color(0xff1dbc7c))
                    )
                }

                // Sélection du jour
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Jour:", modifier = Modifier.width(80.dp))
                    OutlinedTextField(
                        value = dayText,
                        onValueChange = {input ->
                            dayText = input
                            val day = input.toIntOrNull()
                            if (day != null && day in 1..31) { // Validation simplifiée
                                selectedDay = day
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFF1dbc7c),
                            focusedLabelColor = Color(0xff1dbc7c))
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    try {
                        val date = LocalDate.of(selectedYear, selectedMonth, selectedDay)
                        onDateSelected(date)
                    } catch (e: Exception) {
                        // Gestion des dates invalides (par exemple, 31 février)
                        println("Date invalide: ${e.message}")
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF1dbc7c), // Couleur de fond du bouton
                    contentColor = Color(0xfff5f5dc)         // Couleur du texte (et icône)
                )
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            Button(onClick = onDismissRequest,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFFb61431), // Couleur de fond du bouton
                    contentColor = Color(0xfff5f5dc)         // Couleur du texte (et icône)
                )) {
                Text("Annuler")
            }
        }
    )
}

// Composant pour sélectionner une heure
@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    onTimeSelected: (LocalTime) -> Unit,
    initialTime: LocalTime = LocalTime.now()
) {
    var selectedHour by remember { mutableStateOf(initialTime.hour) }
    var selectedMinute by remember { mutableStateOf(initialTime.minute) }

    var hourText by remember { mutableStateOf(selectedHour.toString()) }
    var minuteText by remember { mutableStateOf(selectedMinute.toString()) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Sélectionner une heure") },
        backgroundColor = Color(0xFFfafaeb),
        text = {
            Column {
                // Sélection de l'heure
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Heure:", modifier = Modifier.width(80.dp))
                    OutlinedTextField(
                        value = hourText,
                        onValueChange = {input->
                            hourText = input
                            val hour = input.toIntOrNull()
                            if (hour != null && hour in 0..23) {
                                selectedHour = hour
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFF1dbc7c),
                            focusedLabelColor = Color(0xff1dbc7c))
                    )
                }

                // Sélection des minutes
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Minute:", modifier = Modifier.width(80.dp))
                    OutlinedTextField(
                        value = minuteText,
                        onValueChange = {input->
                            minuteText =input
                            val minute = input.toIntOrNull()
                            if (minute != null && minute in 0..59) {
                                selectedMinute = minute
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFF1dbc7c),
                            focusedLabelColor = Color(0xff1dbc7c))
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    try {
                        val time = LocalTime.of(selectedHour, selectedMinute)
                        onTimeSelected(time)
                    } catch (e: Exception) {
                        println("Heure invalide: ${e.message}")
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF1dbc7c), // Couleur de fond du bouton
                    contentColor = Color(0xfff5f5dc)         // Couleur du texte (et icône)
                )
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            Button(onClick = onDismissRequest,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFFb61431), // Couleur de fond du bouton
                    contentColor = Color(0xfff5f5dc)         // Couleur du texte (et icône)
                )) {
                Text("Annuler")
            }
        }
    )
}

// Fonction pour ajouter une opération à la base de données
fun addOperation(
    database: Database,
    commentaire: String,
    prix: BigDecimal,
    isEntree: Boolean,
    date: LocalDateTime,
    idCompte: Int,
    idCategorie: Int
): Int {
    return database.insert(Operations) {
        set(it.CommentaireOperation, commentaire)
        set(it.PrixOperation, prix)
        set(it.NatureOperation, isEntree)
        set(it.DateOperation, date)
        set(it.IdCompte, idCompte)
        set(it.IdCategorie, idCategorie)
    }
}

// Fonction pour récupérer toutes les catégories
fun fetchCategories(database: Database): List<Pair<Int, String?>> {
    return database.from(org.example.comptable.ktorm.Categories)
        .select()
        .map { row ->
            val id = row[org.example.comptable.ktorm.Categories.IdCategorie]!!
            val nom = row[org.example.comptable.ktorm.Categories.NomCategorie]
            Pair(id, nom)
        }
}

// Fonction pour ajouter une nouvelle catégorie
fun addCategorie(database: Database, nomCategorie: String): Int {
    return database.insert(org.example.comptable.ktorm.Categories) {
        set(it.NomCategorie, nomCategorie)
    }
}

fun updateOperation(database: Database, operation: OperationEntity) {
    database.update(Operations) {
        set(it.CommentaireOperation, operation.CommentaireOperation)
        set(it.PrixOperation, operation.PrixOperation)
        set(it.NatureOperation, operation.NatureOperation)
        set(it.IdCategorie, operation.IdCategorie)
        set(it.DateOperation, operation.DateOperation)
        where {
            it.IdOperation eq operation.IdOperation
        }
    }
    println("Opération mise à jour avec succès : ID ${operation.IdOperation}")
}

// Supprimer une opération
fun deleteOperation(database: Database, operationId: Int) {
    val deletedRows = database.delete(Operations) {
        it.IdOperation eq operationId
    }
    if (deletedRows > 0) {
        println("Opération supprimée avec succès : ID $operationId")
    } else {
        println("Aucune opération trouvée avec l'ID $operationId")
    }
}

// Récupérer une opération par son ID
fun getOperationById(database: Database, operationId: Int): OperationEntity? {
    return database
        .from(Operations)
        .select()
        .where { Operations.IdOperation eq operationId }
        .map { row ->
            OperationEntity(
                IdOperation = row[Operations.IdOperation]!!,
                CommentaireOperation = row[Operations.CommentaireOperation]!!,
                PrixOperation = row[Operations.PrixOperation]!!,
                NatureOperation = row[Operations.NatureOperation]!!,
                DateOperation = row[Operations.DateOperation]!!,
                IdCompte = row[Operations.IdCompte]!!,
                IdCategorie = row[Operations.IdCategorie]!!
            )
        }
        .firstOrNull()
}

fun deleteCategorie(database: Database, idCategorie: Int): Boolean {
    // Vérifier si le type est utilisé par des comptes
    val comptesUsingCategorie = database.useConnection { conn ->
        val sql = "SELECT COUNT(*) FROM Operations WHERE IdCategorie = ?"
        val stmt = conn.prepareStatement(sql)
        stmt.setInt(1, idCategorie)
        val rs = stmt.executeQuery()
        if (rs.next()) rs.getInt(1) else 0
    }

    // Ne pas supprimer si le type est utilisé
    if (comptesUsingCategorie > 0) {
        return false
    }

    // Supprimer le type
    return database.useConnection { conn ->
        val sql = "DELETE FROM Categories WHERE IdCategorie = ?"
        val stmt = conn.prepareStatement(sql)
        stmt.setInt(1, idCategorie)
        stmt.executeUpdate() > 0
    }
}