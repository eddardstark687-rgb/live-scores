package com.pitchpulse.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.pitchpulse.ui.components.*
import com.pitchpulse.ui.state.MatchUiState
import com.pitchpulse.ui.theme.AppBackground
import com.pitchpulse.ui.theme.AppCard
import com.pitchpulse.ui.theme.TextPrimary
import com.pitchpulse.ui.theme.TextSecondary
import com.pitchpulse.ui.viewmodel.HomeExtrasViewModel

@Composable
fun HomeScreen(
    uiState: MatchUiState,
    onMatchClick: (Int) -> Unit,
    onTeamClick: (Int) -> Unit,
    onSettingsClick: () -> Unit = {},
    homeExtrasViewModel: HomeExtrasViewModel = viewModel()
) {
    val extrasState by homeExtrasViewModel.uiState.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppBackground
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
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

            item {
                HomeSectionTitle("Leagues Today")
            }
            items(
                items = extrasState.leagueSummaries,
                key = { it.name }
            ) { summary ->
                LeagueTodayCard(summary = summary)
            }

            item {
                HomeSectionTitle("Football Quiz")
            }
            item {
                val quiz = extrasState.currentQuiz
                when {
                    extrasState.isLoadingContent -> HomeExtrasLoadingRow()
                    quiz != null -> FootballQuizCard(
                        quiz = quiz,
                        selectedOptionIndex = extrasState.selectedOptionIndex,
                        onOptionSelected = homeExtrasViewModel::onQuizOptionSelected,
                        onNextQuestion = homeExtrasViewModel::nextQuizQuestion
                    )
                    else -> Text(
                        text = extrasState.contentError ?: "Quiz unavailable right now.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            item {
                HomeSectionTitle("Quote & Fact")
            }
            item {
                if (extrasState.isLoadingContent) {
                    HomeExtrasLoadingRow()
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        extrasState.quote?.let { quote ->
                            FootballQuoteCard(quote = quote)
                        }
                        extrasState.fact?.let { fact ->
                            FootballFactCard(fact = fact)
                        }
                    }
                }
            }

            when (uiState) {
                is MatchUiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                is MatchUiState.Success -> {
                    if (uiState.favoriteTeams.isNotEmpty()) {
                        item {
                            HomeSectionTitle("My Teams")
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

                    if (uiState.favoriteUpcomingMatches.isNotEmpty()) {
                        item {
                            HomeSectionTitle("Next for Your Teams")
                        }
                        items(uiState.favoriteUpcomingMatches) { match ->
                            MatchCard(
                                match = match,
                                onClick = { onMatchClick(match.id) }
                            )
                        }
                    }

                    if (uiState.favoriteTeams.isEmpty() && uiState.favoriteUpcomingMatches.isEmpty()) {
                        item {
                            Text(
                                text = "Follow teams in Search to see their upcoming matches here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
                is MatchUiState.Error -> {
                    item {
                        Text(
                            text = uiState.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
