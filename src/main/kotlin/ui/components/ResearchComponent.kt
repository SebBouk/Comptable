package org.example.comptable.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

/**
 * Composant de barre de recherche personnalisé
 *
 * @param onSearch Fonction appelée quand la recherche est déclenchée
 * @param onFilterChange Fonction appelée quand un filtre est sélectionné
 * @param placeholder Texte affiché quand la barre de recherche est vide
 * @param filters Liste des filtres disponibles
 * @param modifier Modifier à appliquer au composant
 */
@Composable
fun SearchBar(
    onSearch: (String) -> Unit,
    onFilterChange: (String) -> Unit = {},
    placeholder: String = "Rechercher...",
    filters: List<String> = emptyList(),
    currentQuery: String = "",
    currentFilter: String = "",
    modifier: Modifier = Modifier
) {
    var searchText by remember { mutableStateOf(TextFieldValue(currentQuery)) }
    var isSearchFocused by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf<String?>(null) }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(currentQuery) {
        if (currentQuery != searchText.text) {
            searchText = TextFieldValue(currentQuery)
        }
    }

    LaunchedEffect(currentFilter) {
        selectedFilter = if (currentFilter.isEmpty()) null else currentFilter
    }
    Column(modifier = modifier.padding(8.dp)) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            elevation = 4.dp,
            shape = RoundedCornerShape(28.dp),
            backgroundColor = Color(0xFFF7F7E1)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Rechercher",
                    tint = Color(0xFF1dbc7c)
                )

                Spacer(modifier = Modifier.width(8.dp))

                TextField(
                    value = searchText,
                    onValueChange = {
                        searchText = it
                        if (it.text.isNotEmpty()) {
                            onSearch(it.text)
                        }
                    },
                    placeholder = { Text(placeholder) },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            onSearch(searchText.text)
                            focusManager.clearFocus()
                        }
                    ),
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color(0xFF1dbc7c),
                        textColor = Color.DarkGray
                    ),
                    singleLine = true
                )

                AnimatedVisibility(
                    visible = searchText.text.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    IconButton(
                        onClick = {
                            searchText = TextFieldValue("")
                            onSearch("")
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Effacer",
                            tint = Color.Gray
                        )
                    }
                }

                if (filters.isNotEmpty()) {
                    IconButton(
                        onClick = { showFilters = !showFilters }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = "Filtres",
                            tint = if (selectedFilter != null) Color(0xFF1dbc7c) else Color.Gray
                        )
                    }
                }
            }
        }

        // Affichage des filtres
        AnimatedVisibility(visible = showFilters) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                elevation = 4.dp,
                backgroundColor = Color(0xFFF7F7E1)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        "Filtres",
                        style = MaterialTheme.typography.subtitle2,
                        modifier = Modifier.padding(8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filters.forEach { filter ->
                            val isSelected = filter == selectedFilter

                            Chip(
                                onClick = {
                                    selectedFilter = if (isSelected) null else filter
                                    onFilterChange(selectedFilter ?: "")
                                },
                                modifier = Modifier.border(
                                    width = 1.dp,
                                    color = if (isSelected) Color(0xFF1dbc7c) else Color.LightGray,
                                    shape = RoundedCornerShape(16.dp)
                                ),
                                backgroundColor = if (isSelected) Color(0xFF1dbc7c).copy(alpha = 0.1f) else Color.White
                            ) {
                                Text(
                                    text = filter,
                                    color = if (isSelected) Color(0xFF1dbc7c) else Color.DarkGray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Chip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        elevation = 0.dp,
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}