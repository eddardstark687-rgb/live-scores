package com.pitchpulse.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pitchpulse.ui.components.DateRibbon
import com.pitchpulse.ui.components.MatchCard
import com.pitchpulse.ui.state.MatchUiState
import com.pitchpulse.ui.theme.AppBackground
import com.pitchpulse.ui.theme.TextPrimary
import com.pitchpulse.ui.theme.TextSecondary

@Composable
fun MatchesScreen(
    uiState: MatchUiState,
    onDateSelected: (String) -> Unit,
    onMatchClick: (Int) -> Unit,
    onTeamClick: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppBackground
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Matches",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Text(
                    text = "Daily Football Schedule",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            when (uiState) {
                is MatchUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is MatchUiState.Success -> {
                    // Date Selection Ribbon
                    DateRibbon(
                        dates = uiState.availableDates,
                        selectedDate = uiState.selectedDate,
                        onDateSelected = onDateSelected
                    )

                    // Matches List
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (uiState.dailyMatches.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 80.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No matches scheduled for this date.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                        } else {
                            items(uiState.dailyMatches) { match ->
                                MatchCard(
                                    match = match,
                                    onClick = { onMatchClick(match.id) }
                                )
                            }
                        }
                    }
                }
                is MatchUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = uiState.message, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
