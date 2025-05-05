package org.example.comptable.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import org.example.comptable.repositorie.*
import org.example.comptable.routing.Router
import org.example.comptable.routing.Routes
import org.example.comptable.ui.components.*
import org.ktorm.database.Database

data class ComptesEntity(
    val IdCompte: Int,
    val NumeroCompte: String,
    val IdUser: Int,
    val IdEtablissement:Int,
    val IdType: Int,
)

@Composable
fun HomeScreen(database: Database, userId: Int, router: Router, userName: String, onNavigate: (Routes, Int?) -> Unit) {
    var showForm by remember { mutableStateOf(false) }
    var numeroCompte by remember { mutableStateOf("") }
    var selectedEtablissementId by remember { mutableStateOf<Int?>(null) }
    var selectedTypeId by remember { mutableStateOf<Int?>(null) }

    var refreshTrigger by remember { mutableStateOf(0) }

    var showAddCompteDialog by remember { mutableStateOf(false) }
    var showEditCompteDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }

    var selectedCompteIndex by remember { mutableStateOf(-1) }
    var selectedCompte by remember { mutableStateOf<ComptesEntity?>(null) }

    val comptes = fetchComptes(database, userId)
    println("Compte in UserID: $comptes")

    val accounts = org.example.comptable.ui.components.fetchComptesData(database, userId)

    val selectedAccount = remember { mutableStateOf<AccountData?>(null) }



    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,

    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Titre
            Text(
                text = "Mes comptes",
                style = MaterialTheme.typography.h4,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = "Bonjour, $userName",
                style = MaterialTheme.typography.h6,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            if (selectedAccount.value == null) {
                // Affichage de tous les comptes avec sélecteur de mois
                AccountsArcChart(
                    accounts = accounts,
                    onAccountClick = { account -> selectedAccount.value = account },
                )
            }
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(0.8f)
                    .fillMaxHeight(0.8f)
                    .background(Color(0xFFF5F5DC))
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFf7f7e1))
                        .padding(16.dp)
                        .weight(1f)
                ) {
                    ComptesTable(
                        database = database,
                        userId = userId,
                        refreshKey = refreshTrigger,
                        onNavigateToCompte = { accountId ->
                            println("Navigating to account ID (from HomeScreen): $accountId")
                            router.accountId = accountId
                            router.navigateTo(Routes.COMPTE)
                            onNavigate(Routes.COMPTE, accountId)
                        },
                        onEditCompte = { compteId ->
                            // Recherche du compte par ID au lieu de l'index
                            val compte = comptes.find { it.IdCompte == compteId }
                            if (compte != null) {
                                selectedCompte = compte
                                showEditCompteDialog = true
                            } else {
                                println("Compte avec ID $compteId non trouvé")
                            }
                        },
                        onDeleteCompte = { compteId ->
                            // Même approche pour la suppression
                            val compte = comptes.find { it.IdCompte == compteId }
                            if (compte != null) {
                                selectedCompte = compte
                                showDeleteConfirmationDialog = true
                            } else {
                                println("Compte avec ID $compteId non trouvé")
                            }
                        },
                        onSoldeClick = { compteId ->
                            // Trouver le compte dans la liste des comptes graphiques
                            val accountData = accounts.find { it.id == compteId }
                            if (accountData != null) {
                                // Sélectionner ce compte pour afficher son graphique
                                selectedAccount.value = accountData
                            } else {
                                println("Compte graphique avec ID $compteId non trouvé")
                            }
                        }
                    )
                    Button(
                        onClick = { showForm = true },
                        modifier = Modifier.padding(bottom = 16.dp).align(Alignment.BottomEnd),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF1dbc7c), // Couleur de fond du bouton
                            contentColor = Color(0xfff5f5dc)         // Couleur du texte (et icône)
                        )
                    ) {
                        Text("Créer un Nouveau Compte")
                    }

                    // Bouton pour afficher le formulaire de création de compte


                    // Formulaire de création de compte
                    if (showForm) {
                        CreateCompteForm(
                            database = database,
                            numeroCompte = numeroCompte,
                            onNumeroCompteChange = { numeroCompte = it },
                            selectedEtablissementId = selectedEtablissementId,
                            onEtablissementSelected = { selectedEtablissementId = it },
                            selectedTypeId = selectedTypeId,
                            onTypeSelected = { selectedTypeId = it },
                            onCreateCompte = {
                                if (selectedEtablissementId != null && selectedTypeId != null) {
                                    addCompte(
                                        database = database,
                                        numeroCompte = numeroCompte,
                                        idUser = userId,
                                        idEtablissement = selectedEtablissementId!!,
                                        idType = selectedTypeId!!
                                    )
                                    // Réinitialiser les champs du formulaire
                                    numeroCompte = ""
                                    selectedEtablissementId = null
                                    selectedTypeId = null

                                    // Incrémenter pour déclencher une actualisation
                                    refreshTrigger++

                                    showForm = false // Masquer le formulaire après la création
                                } else {
                                    println("Veuillez sélectionner un établissement et un type de compte.")
                                }
                            },
                            onCancel = { showForm = false }
                        )
                    }
                    if (showEditCompteDialog && selectedCompte != null) {
                        EditCompteDialog(
                            database = database,
                            compte = selectedCompte!!,
                            onDismiss = { showEditCompteDialog = false },
                            onEtablissementSelected = { selectedEtablissementId = it },
                            onTypeSelected = { selectedTypeId = it },
                            onCompteUpdated = {
                                refreshTrigger++
                                showEditCompteDialog = false
                            }
                        )
                    }

                    // Dialogue de confirmation de suppression
                    if (showDeleteConfirmationDialog && selectedCompte != null) {
                        AlertDialog(
                            onDismissRequest = { showDeleteConfirmationDialog = false },
                            backgroundColor = Color(0xFFfafaeb),
                            title = { Text("Confirmer la suppression") },
                            text = { Text("Êtes-vous sûr de vouloir supprimer ce compte ?") },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        deleteCompte(database, selectedCompte!!.IdCompte)
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
        }
    }
}

@Composable
fun CreateCompteForm(
    database: Database,
    numeroCompte: String,
    onNumeroCompteChange: (String) -> Unit,
    selectedEtablissementId: Int?,
    onEtablissementSelected: (Int?) -> Unit,
    selectedTypeId: Int?,
    onTypeSelected: (Int?) -> Unit,
    onCreateCompte: () -> Unit,
    onCancel: () -> Unit
) {
    var refreshTrigger by remember { mutableStateOf(0) }
    var etablissements = remember(refreshTrigger) { fetchEtablissements(database) }
    var typesComptes = remember(refreshTrigger) { fetchTypesComptes(database) }

    var expandedEtablissement by remember { mutableStateOf(false) }
    var expandedType by remember { mutableStateOf(false) }

    // États pour la gestion des dialogues d'ajout
    var showAddEtablissementDialog by remember { mutableStateOf(false) }
    var showAddTypeDialog by remember { mutableStateOf(false) }
    var newEtablissementName by remember { mutableStateOf("") }
    var newTypeName by remember { mutableStateOf("") }

    var showDeleteEtablissementDialog by remember { mutableStateOf(false) }
    var showDeleteTypeDialog by remember { mutableStateOf(false) }
    var etablissementToDeleteId by remember { mutableStateOf<Int?>(null) }
    var typeToDeleteId by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onCancel,
        backgroundColor = Color(0xFFfafaeb),
        title = { Text("Créer un nouveau compte") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp)
            ) {
                TextField(
                    value = numeroCompte,
                    onValueChange = onNumeroCompteChange,
                    label = { Text("Numéro de Compte") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = TextFieldDefaults.textFieldColors(
                        focusedIndicatorColor = Color(0xFF1dbc7c),
                        focusedLabelColor = Color(0xFF1dbc7c)
                    )
                )

                // Établissement dropdown
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text("Établissement", style = MaterialTheme.typography.body1)

                    OutlinedButton(
                        onClick = { expandedEtablissement = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF1dbc7c),
                            contentColor = Color(0xfff5f5dc)
                        )
                    ) {
                        Text(
                            text = selectedEtablissementId?.let { id ->
                                etablissements.find { it.first == id }?.second ?: "Sélectionner"
                            } ?: "Sélectionner un établissement",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                    }

                    DropdownMenu(
                        expanded = expandedEtablissement,
                        onDismissRequest = { expandedEtablissement = false },
                        modifier = Modifier.fillMaxWidth(0.9f).background( Color(0xFFfafaeb))
                    ) {
                        etablissements.forEach { (id, name) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DropdownMenuItem(
                                    onClick = {
                                        onEtablissementSelected(id)
                                        expandedEtablissement = false
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = name ?: "")
                                }

                                IconButton(
                                    onClick = {
                                        // Afficher une confirmation avant de supprimer
                                        showDeleteEtablissementDialog = true
                                        etablissementToDeleteId = id
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

                        // Option pour ajouter un nouvel établissement
                        Divider()
                        DropdownMenuItem(
                            onClick = {
                                expandedEtablissement = false
                                showAddEtablissementDialog = true
                            }
                        ) {
                            Text(
                                text = "+ Ajouter un nouvel établissement",
                                color = Color(0xff1bdc7c)
                            )
                        }
                    }
                }

                // Type de compte dropdown
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text("Type de Compte", style = MaterialTheme.typography.body1)

                    OutlinedButton(
                        onClick = { expandedType = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF1dbc7c),
                            contentColor = Color(0xfff5f5dc)
                        )
                    ) {
                        Text(
                            text = selectedTypeId?.let { id ->
                                typesComptes.find { it.first == id }?.second ?: "Sélectionner"
                            } ?: "Sélectionner un type de compte",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                    }

                    DropdownMenu(
                        expanded = expandedType,
                        onDismissRequest = { expandedType = false },
                        modifier = Modifier.fillMaxWidth(0.9f).background( Color(0xFFfafaeb))
                    ) {
                        typesComptes.forEach { (id, name) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DropdownMenuItem(
                                    onClick = {
                                        onTypeSelected(id)
                                        expandedType = false
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = name ?: "")
                                }

                                IconButton(
                                    onClick = {
                                        // Afficher une confirmation avant de supprimer
                                        showDeleteTypeDialog = true
                                        typeToDeleteId = id
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

                        // Option pour ajouter un nouveau type de compte
                        Divider()
                        DropdownMenuItem(
                            onClick = {
                                expandedType = false
                                showAddTypeDialog = true
                            }
                        ) {
                            Text(
                                text = "+ Ajouter un nouveau type de compte",
                                color = Color(0xff1bdc7c)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onCreateCompte,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF1dbc7c),
                    contentColor = Color(0xfff5f5dc)
                )
            ) {
                Text("Créer")
            }
        },
        dismissButton = {
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFFb61431),
                    contentColor = Color(0xfff5f5dc)
                )
            ) {
                Text("Annuler")
            }
        }
    )

    // Dialog pour ajouter un nouvel établissement
    if (showAddEtablissementDialog) {
        AlertDialog(
            onDismissRequest = { showAddEtablissementDialog = false },
            backgroundColor = Color(0xFFfafaeb),
            title = { Text("Ajouter un établissement") },
            text = {
                TextField(
                    value = newEtablissementName,
                    onValueChange = { newEtablissementName = it },
                    label = { Text("Nom de l'établissement") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.textFieldColors(
                        focusedIndicatorColor = Color(0xFF1dbc7c),
                        focusedLabelColor = Color(0xFF1dbc7c)
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newEtablissementName.isNotBlank()) {
                            // Ajouter le nouvel établissement à la base de données
                            val newId = addEtablissement(database, newEtablissementName)
                            // Sélectionner le nouvel établissement
                            onEtablissementSelected(newId)
                            refreshTrigger++
                            // Réinitialiser et fermer le dialogue
                            newEtablissementName = ""
                            showAddEtablissementDialog = false
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
                    onClick = { showAddEtablissementDialog = false },
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

    if (showDeleteEtablissementDialog && etablissementToDeleteId != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteEtablissementDialog = false
                etablissementToDeleteId = null
            },
            backgroundColor = Color(0xFFfafaeb),
            title = { Text("Confirmer la suppression") },
            text = {
                Text(
                    "Êtes-vous sûr de vouloir supprimer cet établissement ? Cette action est irréversible." +
                            "\n\nNote: La suppression sera impossible si des comptes sont liés à cet établissement."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val result = deleteEtablissement(database, etablissementToDeleteId!!)
                        if (result) {
                            // Suppression réussie
                            refreshTrigger++
                        } else {
                            // Afficher un message d'erreur (idéalement avec un Snackbar)
                            // Vous pouvez ajouter un état pour gérer ces messages
                        }
                        showDeleteEtablissementDialog = false
                        etablissementToDeleteId = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFFb61431),
                        contentColor = Color(0xfff5f5dc)
                    )
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showDeleteEtablissementDialog = false
                        etablissementToDeleteId = null
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

    // Dialog pour ajouter un nouveau type de compte
    if (showAddTypeDialog) {
        AlertDialog(
            onDismissRequest = { showAddTypeDialog = false },
            title = { Text("Ajouter un type de compte") },
            backgroundColor = Color(0xFFfafaeb),
            text = {
                TextField(
                    value = newTypeName,
                    onValueChange = { newTypeName = it },
                    label = { Text("Nom du type de compte") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.textFieldColors(
                        focusedIndicatorColor = Color(0xFF1dbc7c),
                        focusedLabelColor = Color(0xFF1dbc7c),
                        cursorColor = Color(0xff1dbc7c)
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTypeName.isNotBlank()) {
                            // Ajouter le nouveau type à la base de données
                            val newId = addTypeCompte(database, newTypeName)
                            // Sélectionner le nouveau type
                            onTypeSelected(newId)
                            refreshTrigger++
                            // Réinitialiser et fermer le dialogue
                            newTypeName = ""
                            showAddTypeDialog = false
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
                    onClick = { showAddTypeDialog = false },
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

    if (showDeleteTypeDialog && typeToDeleteId != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteTypeDialog = false
                typeToDeleteId = null
            },
            backgroundColor = Color(0xFFfafaeb),
            title = { Text("Confirmer la suppression") },
            text = {
                Text(
                    "Êtes-vous sûr de vouloir supprimer ce type ? Cette action est irréversible." +
                            "\n\nNote: La suppression sera impossible si des comptes sont liés à ce type."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val result = deleteTypeCompte(database, typeToDeleteId!!)
                        if (result) {
                            // Suppression réussie
                            refreshTrigger++
                        } else {
                            // Afficher un message d'erreur (idéalement avec un Snackbar)
                            // Vous pouvez ajouter un état pour gérer ces messages
                        }
                        showDeleteTypeDialog = false
                        typeToDeleteId = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFFb61431),
                        contentColor = Color(0xfff5f5dc)
                    )
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showDeleteTypeDialog = false
                        typeToDeleteId = null
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


    @Composable
    fun EditCompteDialog(
        database: Database,
        compte: ComptesEntity,
        onDismiss: () -> Unit,
        onCompteUpdated: () -> Unit,
        onEtablissementSelected: (Int?) -> Unit,
        onTypeSelected: (Int?) -> Unit,
    ) {

        var refreshTrigger by remember { mutableStateOf(0) }

        var numero by remember { mutableStateOf(compte.NumeroCompte) }
        var selectedEtablissementId by remember { mutableStateOf(compte.IdEtablissement) }
        var selectedTypeId by remember { mutableStateOf(compte.IdType) }

        var etablissements = remember(refreshTrigger) { fetchEtablissements(database) }
        var type = remember(refreshTrigger) { fetchTypesComptes(database) }

        var showAddEtablissementDialog by remember { mutableStateOf(false) }
        var showAddTypeDialog by remember { mutableStateOf(false) }
        var newEtablissementName by remember { mutableStateOf("") }
        var newTypeName by remember { mutableStateOf("") }

        var showDeleteEtablissementDialog by remember { mutableStateOf(false) }
        var showDeleteTypeDialog by remember { mutableStateOf(false) }
        var etablissementToDeleteId by remember { mutableStateOf<Int?>(null) }
        var typeToDeleteId by remember { mutableStateOf<Int?>(null) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Modifier le compte") },
            backgroundColor = Color(0xFFfafaeb), // Définit la couleur de fond de toute la boîte de dialogue
            text = {
                Column(modifier = Modifier.padding(8.dp)) {
                    // Champ pour le montant
                    TextField(
                        value = numero,
                        onValueChange = {
                            if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                                numero = it
                            }
                        },
                        label = { Text("N° de compte") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFF1dbc7c),
                            focusedLabelColor = Color(0xff1dbc7c)
                        )
                    )


                    // Sélection d'etablissement
                    var expandedEtablissement by remember { mutableStateOf(false) }

                    Text("Etablissements", modifier = Modifier.padding(vertical = 4.dp))
                    OutlinedButton(
                        onClick = { expandedEtablissement = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF1dbc7c), // Couleur de fond du bouton
                            contentColor = Color(0xfff5f5dc)         // Couleur du texte (et icône)
                        )
                    ) {
                        Text(
                            text = etablissements.find { it.first == selectedEtablissementId }?.second
                                ?: "Sélectionner un établissement"
                        )
                    }

                    DropdownMenu(
                        expanded = expandedEtablissement,
                        onDismissRequest = { expandedEtablissement = false },
                        modifier = Modifier.fillMaxWidth().background( Color(0xFFfafaeb)),
                    ) {
                        etablissements.forEach { (id, name) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DropdownMenuItem(
                                    onClick = {
                                        selectedEtablissementId = id!!
                                        expandedEtablissement = false
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = name ?: "")
                                }

                                // Bouton de suppression
                                IconButton(
                                    onClick = {
                                        // Afficher une confirmation avant de supprimer
                                        showDeleteEtablissementDialog = true
                                        etablissementToDeleteId = id
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
                                expandedEtablissement = false
                                showAddEtablissementDialog = true
                            }
                        ) {
                            Text(
                                text = "+ Ajouter un nouvel établissement",
                                color = Color(0xff1bdc7c)
                            )
                        }
                    }
                    // Sélection de type
                    var expandedType by remember { mutableStateOf(false) }

                    Text("Type de compte", modifier = Modifier.padding(vertical = 4.dp))
                    OutlinedButton(
                        onClick = { expandedType = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF1dbc7c), // Couleur de fond du bouton
                            contentColor = Color(0xfff5f5dc)         // Couleur du texte (et icône)
                        )
                    ) {
                        Text(
                            text = type.find { it.first == selectedTypeId }?.second ?: "Sélectionner un type de compte"
                        )
                    }

                    DropdownMenu(
                        expanded = expandedType,
                        onDismissRequest = { expandedType = false },
                        modifier = Modifier.fillMaxWidth().background( Color(0xFFfafaeb))
                    ) {
                        type.forEach { (id, name) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DropdownMenuItem(
                                    onClick = {
                                        selectedTypeId = id!!
                                        expandedType = false
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = name ?: "")
                                }

                                IconButton(
                                    onClick = {
                                        // Afficher une confirmation avant de supprimer
                                        showDeleteTypeDialog = true
                                        typeToDeleteId = id
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
                                expandedType = false
                                showAddTypeDialog = true
                            }
                        ) {
                            Text(
                                text = "+ Ajouter un nouveau type de compte",
                                color = Color(0xff1bdc7c)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (numero.isNotBlank() && selectedEtablissementId > 0 && selectedTypeId > 0) {
                            val updatedCompte = compte.copy(
                                NumeroCompte = numero,
                                IdEtablissement = selectedEtablissementId,
                                IdType = selectedTypeId
                            )
                            updateCompte(database, updatedCompte)
                            onCompteUpdated()
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
        if (showAddEtablissementDialog) {
            AlertDialog(
                onDismissRequest = { showAddEtablissementDialog = false },
                backgroundColor = Color(0xFFfafaeb),
                title = { Text("Ajouter un établissement") },
                text = {
                    TextField(
                        value = newEtablissementName,
                        onValueChange = { newEtablissementName = it },
                        label = { Text("Nom de l'établissement") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.textFieldColors(
                            focusedIndicatorColor = Color(0xFF1dbc7c),
                            focusedLabelColor = Color(0xFF1dbc7c),
                            cursorColor = Color(0xff1dbc7c)
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newEtablissementName.isNotBlank()) {
                                // Ajouter le nouvel établissement à la base de données
                                val newId = addEtablissement(database, newEtablissementName)
                                // Sélectionner le nouvel établissement
                                selectedEtablissementId = newId
                                refreshTrigger++
                                // Réinitialiser et fermer le dialogue
                                newEtablissementName = ""
                                showAddEtablissementDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFFb61431),
                            contentColor = Color(0xfff5f5dc)
                        )
                    ) {
                        Text("Ajouter")
                    }
                },
                dismissButton = {
                    Button(onClick = { showAddEtablissementDialog = false },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF1dbc7c),
                            contentColor = Color(0xfff5f5dc)
                        )) {
                        Text("Annuler")
                    }
                }
            )
        }
        // Dialogue de confirmation de suppression
        if (showDeleteEtablissementDialog && etablissementToDeleteId != null) {
            AlertDialog(
                onDismissRequest = {
                    showDeleteEtablissementDialog = false
                    etablissementToDeleteId = null
                },
                backgroundColor = Color(0xFFfafaeb),
                title = { Text("Confirmer la suppression") },
                text = {
                    Text(
                        "Êtes-vous sûr de vouloir supprimer cet établissement ? Cette action est irréversible." +
                                "\n\nNote: La suppression sera impossible si des comptes sont liés à cet établissement."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val result = deleteEtablissement(database, etablissementToDeleteId!!)
                            if (result) {
                                // Suppression réussie
                                refreshTrigger++
                            } else {
                                // Afficher un message d'erreur (idéalement avec un Snackbar)
                                // Vous pouvez ajouter un état pour gérer ces messages
                            }
                            showDeleteEtablissementDialog = false
                            etablissementToDeleteId = null
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error)
                    ) {
                        Text("Supprimer", color = Color.White)
                    }
                },
                dismissButton = {
                    Button(onClick = {
                        showDeleteEtablissementDialog = false
                        etablissementToDeleteId = null
                    }) {
                        Text("Annuler")
                    }
                }
            )
        }

        // Dialog pour ajouter un nouveau type de compte
        if (showAddTypeDialog) {
            AlertDialog(
                onDismissRequest = { showAddTypeDialog = false },
                backgroundColor = Color(0xFFfafaeb),
                title = { Text("Ajouter un type de compte") },
                text = {
                    TextField(
                        value = newTypeName,
                        onValueChange = { newTypeName = it },
                        label = { Text("Nom du type de compte") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.textFieldColors(
                            focusedIndicatorColor = Color(0xFF1dbc7c),
                            focusedLabelColor = Color(0xFF1dbc7c),
                            cursorColor = Color(0xff1dbc7c)
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newTypeName.isNotBlank()) {
                                // Ajouter le nouveau type à la base de données
                                val newId = addTypeCompte(database, newTypeName)
                                // Sélectionner le nouveau type
                                selectedTypeId = newId
                                refreshTrigger++
                                // Réinitialiser et fermer le dialogue
                                newTypeName = ""
                                showAddTypeDialog = false
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
                    Button(onClick = { showAddTypeDialog = false },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFFb61431),
                            contentColor = Color(0xfff5f5dc)
                        )) {
                        Text("Annuler")
                    }
                }
            )
        }
        if (showDeleteTypeDialog && typeToDeleteId != null) {
            AlertDialog(
                onDismissRequest = {
                    showDeleteTypeDialog = false
                    typeToDeleteId = null
                },
                title = { Text("Confirmer la suppression") },
                backgroundColor = Color(0xFFfafaeb),
                text = {
                    Text(
                        "Êtes-vous sûr de vouloir supprimer ce type ? Cette action est irréversible." +
                                "\n\nNote: La suppression sera impossible si des comptes sont liés à ce type."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val result = deleteTypeCompte(database, typeToDeleteId!!)
                            if (result) {
                                // Suppression réussie
                                refreshTrigger++
                            } else {
                                // Afficher un message d'erreur (idéalement avec un Snackbar)
                                // Vous pouvez ajouter un état pour gérer ces messages
                            }
                            showDeleteTypeDialog = false
                            typeToDeleteId = null
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
                    Button(onClick = {
                        showDeleteTypeDialog = false
                        typeToDeleteId = null
                    },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF1dbc7c),
                            contentColor = Color(0xfff5f5dc)
                        )) {
                        Text("Annuler")
                    }
                }
            )
        }
    }

