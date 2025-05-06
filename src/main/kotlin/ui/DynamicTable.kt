package org.example.comptable.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

enum class SortOrder {
    NONE, ASCENDING, DESCENDING
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DynamicTable(
    columns: List<String>,
    rows: List<List<String?>>,
    isEditable: (rowIndex: Int, colIndex: Int) -> Boolean = { _, _ -> false },
    onCellUpdate: (rowIndex: Int, colIndex: Int, newValue: String) -> Unit = { _, _, _ -> },
    customCellColors: (rowIndex: Int, colIndex: Int) -> Color? = { _, _ -> null },
    customCellColorsByValue: (value: String, colIndex: Int) -> Color? = { _, _ -> null },
    isClickable: (rowIndex: Int, colIndex: Int) -> Boolean = { _, _ -> false },
    onCellClick: (rowIndex: Int, colIndex: Int) -> Unit = { _, _ -> },
    // Options d'édition et de suppression
    showEditDeleteActions: Boolean = false,
    onEditRow: (rowIndex: Int) -> Unit = {},
    onDeleteRow: (rowIndex: Int) -> Unit = {},
    // Options de style et de mise en page
    modifier: Modifier = Modifier,
    columnWidths: List<Float> = emptyList(),
    alternateRowColors: Boolean = true,
    isRowClickable: (rowIndex: Int) -> Boolean = { false },
    onRowClick: (rowIndex: Int) -> Unit = {},
    customCellAlignment: (colIndex: Int) -> Alignment = { Alignment.CenterStart },
    maxHeight: Int = Int.MAX_VALUE,
    headerBackgroundColor: Color = Color(0xff1bdc7c),
    headerTextColor: Color = Color(0xfff5f5dc),
    rowBackgroundColor: (rowIndex: Int) -> Color = { if (alternateRowColors && it % 2 == 1) Color.LightGray.copy(alpha = 0.3f) else Color.Transparent },
    dateColumns: Set<Int> = emptySet(),
    // Options de tri
    sortableColumns: Set<Int> = emptySet(),  // Les indices des colonnes triables
    initialSortColumnIndex: Int? = null,     // Colonne triée initialement
    initialSortOrder: SortOrder = SortOrder.NONE // Ordre de tri initial
) {
    // Gardez en mémoire les cellules en cours d'édition
    var editingCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // État pour le tri
    var sortColumnIndex by remember { mutableStateOf(initialSortColumnIndex) }
    var sortOrder by remember { mutableStateOf(initialSortOrder) }

    // État de la liste pour la scrollbar
    val scrollState = rememberLazyListState()

    // Calculer les largeurs des colonnes avec une colonne supplémentaire pour les actions si nécessaire
    val totalColumns = if (showEditDeleteActions) columns.size + 1 else columns.size
    val actualColumnWidths = if (columnWidths.isEmpty()) {
        if (showEditDeleteActions) {
            // Pour la dernière colonne des actions
            val normalColumnWeight = 0.8f / (totalColumns - 1)
            List(totalColumns) { index -> if (index == totalColumns - 1) 0.2f else normalColumnWeight }
        } else {
            List(totalColumns) { 1f / totalColumns }
        }
    } else {
        if (showEditDeleteActions && columnWidths.size == columns.size) {
            // Ajouter la colonne pour les actions
            columnWidths + listOf(0.15f)
        } else {
            // Utiliser les largeurs fournies
            columnWidths
        }
    }

    // Fonction pour trier les lignes
    fun getSortedRows(): List<List<String?>> {
        if (sortColumnIndex == null || sortOrder == SortOrder.NONE) {
            return rows
        }

        return when (sortOrder) {
            SortOrder.ASCENDING -> rows.sortedBy { row ->
                row.getOrNull(sortColumnIndex!!)?.lowercase() ?: ""
            }
            SortOrder.DESCENDING -> rows.sortedByDescending { row ->
                row.getOrNull(sortColumnIndex!!)?.lowercase() ?: ""
            }
            else -> rows
        }
    }

    fun formatDateTime(dateTimeStr: String): String {
        return try {
            // Essai de différents formats
            if (dateTimeStr.contains("T") || dateTimeStr.contains(" ") && dateTimeStr.contains(":")) {
                // Format avec date et heure (ISO ou similaire)
                val hasT = dateTimeStr.contains("T")
                val delimiter = if (hasT) "T" else " "
                val parts = dateTimeStr.split(delimiter)

                if (parts.size >= 2) {
                    val datePart = parts[0]
                    val timePart = parts[1].split(".")[0] // Enlever les millisecondes si présentes

                    // Formatage de la date
                    val dateFormatted = try {
                        val dateParts = datePart.split("-", "/", ".")
                        if (dateParts.size == 3) {
                            if (dateParts[0].length == 4) {
                                // Format YYYY-MM-DD
                                "${dateParts[2]}/${dateParts[1]}/${dateParts[0]}"
                            } else {
                                // Format DD-MM-YYYY ou similaire
                                "${dateParts[0]}/${dateParts[1]}/${dateParts[2]}"
                            }
                        } else {
                            datePart
                        }
                    } catch (e: Exception) {
                        datePart
                    }

                    // Récupérer juste les heures et minutes (retirer les secondes)
                    val timePartWithoutSeconds = try {
                        val timeParts = timePart.split(":")
                        if (timeParts.size >= 2) {
                            "${timeParts[0]}:${timeParts[1]}"  // Format HH:MM
                        } else {
                            timePart
                        }
                    } catch (e: Exception) {
                        timePart
                    }

                    // Renvoyer la date et l'heure formatées sans les secondes
                    "$dateFormatted $timePartWithoutSeconds"
                } else {
                    dateTimeStr
                }
            } else {
                // Format date uniquement
                val parts = dateTimeStr.split("-", "/", ".")
                if (parts.size == 3) {
                    if (parts[0].length == 4) {
                        // Format YYYY-MM-DD
                        "${parts[2]}/${parts[1]}/${parts[0]}"
                    } else if (parts[2].length == 4) {
                        // Format DD-MM-YYYY ou MM-DD-YYYY
                        "${parts[0]}/${parts[1]}/${parts[2]}"
                    } else {
                        dateTimeStr
                    }
                } else {
                    dateTimeStr
                }
            }
        } catch (e: Exception) {
            dateTimeStr // En cas d'erreur, retourner la chaîne d'origine
        }
    }

    Column(modifier = modifier.heightIn(max = maxHeight.dp)) {
        // En-tête du tableau
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBackgroundColor)
                .border(BorderStroke(1.dp, Color.Gray))
                .padding(8.dp)
        ) {
            columns.forEachIndexed { index, column ->
                Box(
                    modifier = Modifier
                        .weight(actualColumnWidths[index])
                        .padding(horizontal = 4.dp)
                        .let {
                            if (index in sortableColumns) {
                                it.clickable {
                                    // Changer l'ordre de tri quand on clique sur l'en-tête
                                    if (sortColumnIndex == index) {
                                        sortOrder = when (sortOrder) {
                                            SortOrder.NONE -> SortOrder.ASCENDING
                                            SortOrder.ASCENDING -> SortOrder.DESCENDING
                                            SortOrder.DESCENDING -> SortOrder.NONE
                                        }
                                    } else {
                                        sortColumnIndex = index
                                        sortOrder = SortOrder.ASCENDING
                                    }
                                }
                            } else {
                                it
                            }
                        },
                    contentAlignment = customCellAlignment(index)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = if (customCellAlignment(index) == Alignment.Center)
                            Arrangement.Center else Arrangement.Start
                    ) {
                        Text(
                            text = column,
                            style = MaterialTheme.typography.subtitle2,
                            color = headerTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Afficher l'icône de tri si cette colonne est triée
                        if (index == sortColumnIndex && sortOrder != SortOrder.NONE) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (sortOrder == SortOrder.ASCENDING)
                                    Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (sortOrder == SortOrder.ASCENDING)
                                    "Tri croissant" else "Tri décroissant",
                                tint = headerTextColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Colonne pour les actions (si activée)
            if (showEditDeleteActions) {
                Box(
                    modifier = Modifier
                        .weight(actualColumnWidths.last())
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Actions",
                        style = MaterialTheme.typography.subtitle2,
                        color = headerTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Corps du tableau avec LazyColumn pour un défilement efficace et scrollbar
        Box(modifier = Modifier.weight(1f)) {
            val sortedRows = getSortedRows()

            LazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(sortedRows) { rowIndex, row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(rowBackgroundColor(rowIndex))
                            .border(BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f)))
                            .padding(8.dp)
                            .then(
                                if (isRowClickable(rowIndex)) {
                                    Modifier.clickable { onRowClick(rowIndex) }
                                } else {
                                    Modifier
                                }
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row.forEachIndexed { colIndex, cell ->
                            val cellText = cell ?: ""
                            val displayText = if (colIndex in dateColumns) formatDateTime(cellText) else cellText
                            val isCurrentlyEditing = editingCell == Pair(rowIndex, colIndex)

                            val textColor = customCellColors(rowIndex, colIndex)

                            Box(
                                modifier = Modifier
                                    .weight(actualColumnWidths[colIndex])
                                    .padding(horizontal = 4.dp),
                                contentAlignment = customCellAlignment(colIndex)
                            ) {
                                if (isCurrentlyEditing && isEditable(rowIndex, colIndex)) {
                                    // Mode édition
                                    var textFieldValue by remember { mutableStateOf(cellText) }

                                    BasicTextField(
                                        value = textFieldValue,
                                        onValueChange = { textFieldValue = it },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(4.dp)
                                            .focusRequester(focusRequester)
                                            .onPreviewKeyEvent { keyEvent ->
                                                // Gérer la touche Entrée pour confirmer l'édition
                                                if (keyEvent.key == Key.Enter && keyEvent.type == KeyEventType.KeyDown) {
                                                    onCellUpdate(rowIndex, colIndex, textFieldValue)
                                                    editingCell = null
                                                    focusManager.clearFocus()
                                                    true
                                                } else {
                                                    false
                                                }
                                            },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(
                                            onDone = {
                                                onCellUpdate(rowIndex, colIndex, textFieldValue)
                                                editingCell = null
                                                focusManager.clearFocus()
                                            }
                                        ),
                                        decorationBox = { innerTextField ->
                                            Box(
                                                modifier = Modifier
                                                    .border(BorderStroke(1.dp, Color.Blue))
                                                    .padding(4.dp)
                                            ) {
                                                innerTextField()
                                            }
                                        }
                                    )

                                    // Focus automatique sur le champ d'édition
                                    LaunchedEffect(Unit) {
                                        focusRequester.requestFocus()
                                    }

                                    // Gestionnaire pour sauvegarder les modifications quand on perd le focus
                                    DisposableEffect(Unit) {
                                        onDispose {
                                            onCellUpdate(rowIndex, colIndex, textFieldValue)
                                            editingCell = null
                                        }
                                    }
                                } else {
                                    // Mode affichage
                                    Text(
                                        text = displayText,
                                        style = MaterialTheme.typography.body2,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = if (customCellAlignment(colIndex) == Alignment.Center) TextAlign.Center else null,
                                        color = customCellColorsByValue(displayText, colIndex) ?: textColor ?: Color.Black,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .let {
                                                if (isEditable(rowIndex, colIndex)) {
                                                    it.clickable { editingCell = Pair(rowIndex, colIndex) }
                                                } else if (isClickable(rowIndex, colIndex)) {
                                                    it.clickable { onCellClick(rowIndex, colIndex) }
                                                } else {
                                                    it
                                                }
                                            }
                                    )
                                }
                            }
                        }

                        // Colonne pour les boutons d'action (si activée)
                        if (showEditDeleteActions) {
                            Row(
                                modifier = Modifier
                                    .weight(actualColumnWidths.last())
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Bouton Modifier
                                IconButton(
                                    onClick = { onEditRow(rowIndex) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Modifier",
                                        tint = Color(0xFF1dbc7c)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Bouton Supprimer
                                IconButton(
                                    onClick = { onDeleteRow(rowIndex) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Supprimer",
                                        tint = MaterialTheme.colors.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Ajouter la scrollbar verticale
            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                adapter = rememberScrollbarAdapter(scrollState)
            )
        }
    }
}