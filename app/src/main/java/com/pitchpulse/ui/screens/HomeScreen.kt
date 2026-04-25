package com.pitchpulse.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.pitchpulse.ui.components.MatchCard
import com.pitchpulse.ui.state.MatchUiState
import com.pitchpulse.ui.theme.AppBackground
import com.pitchpulse.ui.theme.AppCard
import com.pitchpulse.ui.theme.TextPrimary
import com.pitchpulse.ui.theme.TextSecondary

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings

@Composable
fun HomeScreen(
    uiState: MatchUiState,
    onMatchClick: (Int) -> Unit,
    onTeamClick: (Int) -> Unit,
    onSettingsClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppBackground
    ) {
        when (uiState) {
            is MatchUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is MatchUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Home",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Real-time Football Scores",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                            IconButton(onClick = onSettingsClick) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = TextPrimary
                                )
                            }
                        }
                    }

                    // My Teams (Favorites)
                    if (uiState.favoriteTeams.isNotEmpty()) {
                        item {
                            Text(
                                text = "My Teams",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                items(uiState.favoriteTeams) { team ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.clickable { onTeamClick(team.teamId) }
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(64.dp),
                                            shape = CircleShape,
                                            color = AppCard
                                        ) {
                                            AsyncImage(
                                                model = team.logoUrl,
                                                contentDescription = team.name,
                                                modifier = Modifier.padding(12.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = team.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Next for Your Teams
                    if (uiState.favoriteUpcomingMatches.isNotEmpty()) {
                        item {
                            Text(
                                text = "Next for Your Teams",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }
                        items(uiState.favoriteUpcomingMatches) { match ->
                            MatchCard(
                                match = match,
                                onClick = { onMatchClick(match.id) }
                            )
                        }
                    }

                    // No Matches Fallback
                    if (uiState.favoriteTeams.isEmpty() && uiState.favoriteUpcomingMatches.isEmpty()) {
                        item {
                            Text(
                                text = "Follow teams in Search to see their upcoming matches here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 24.dp)
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
