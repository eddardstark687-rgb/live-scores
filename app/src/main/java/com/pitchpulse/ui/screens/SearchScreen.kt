package com.pitchpulse.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pitchpulse.ui.theme.*
import com.pitchpulse.ui.viewmodel.SearchViewModel
import com.pitchpulse.ui.viewmodel.SearchUiState

@Composable
fun SearchScreen(
    viewModel: SearchViewModel = viewModel()
) {
    val searchText by viewModel.searchText.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Search",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Redesigned Search Bar
            OutlinedTextField(
                value = searchText,
                onValueChange = { viewModel.onSearchTextChange(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { 
                    Text(
                        "Search teams...", 
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    ) 
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                trailingIcon = {
                    if (searchText.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchTextChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = AppSurface,
                    unfocusedContainerColor = AppSurface,
                    focusedBorderColor = AppAccent,
                    unfocusedBorderColor = AppSurface
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Search Results
            Box(modifier = Modifier.weight(1f)) {
                when (val state = uiState) {
                    is SearchUiState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = AppAccent
                        )
                    }
                    is SearchUiState.Success -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.teams) { team ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = AppCard,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = team.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            team.country?.let {
                                                Text(
                                                    text = it,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextSecondary
                                                )
                                            }
                                        }
                                        
                                        val isFav = state.favoriteIds.contains(team.id)
                                        TextButton(
                                            onClick = { viewModel.toggleFavorite(team) }
                                        ) {
                                            Text(
                                                text = if (isFav) "Followed" else "Follow",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isFav) AppAccent else TextPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        // Empty/Initial state
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchText.isEmpty()) "Search for teams to follow them." else "No results found.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
