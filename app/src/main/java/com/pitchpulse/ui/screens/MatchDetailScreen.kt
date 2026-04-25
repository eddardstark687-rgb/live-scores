package com.pitchpulse.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.pitchpulse.core.ui.Dimens
import com.pitchpulse.ui.components.EmptyState
import com.pitchpulse.ui.state.MatchDetailUiState
import com.pitchpulse.ui.theme.AppAccent
import com.pitchpulse.ui.theme.TextPrimary
import com.pitchpulse.ui.viewmodel.MatchDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailScreen(
    viewModel: MatchDetailViewModel,
    onBack: () -> Unit,
    onTeamClick: (Int) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Lineups", "Stats")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Match Center") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = TextPrimary
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is MatchDetailUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = AppAccent
                    )
                }
                is MatchDetailUiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = AppAccent,
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    color = AppAccent
                                )
                            }
                        ) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    text = { 
                                        Text(
                                            text = title,
                                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                        ) 
                                    }
                                )
                            }
                        }

                        Box(modifier = Modifier.fillMaxSize().padding(Dimens.SpacingLarge)) {
                            AnimatedContent(
                                targetState = selectedTab,
                                transitionSpec = {
                                    if (targetState > initialState) {
                                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                                                slideOutHorizontally { width -> -width } + fadeOut()
                                    } else {
                                        slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                                slideOutHorizontally { width -> width } + fadeOut()
                                    }.using(
                                        SizeTransform(clip = false)
                                    )
                                },
                                label = "TabTransition"
                            ) { targetIndex ->
                                when (targetIndex) {
                                    0 -> OverviewTab(state.match, onTeamClick)
                                    1 -> LineupsTab(state.lineups)
                                    2 -> StatsTab(state.stats)
                                }
                            }
                        }
                    }
                }
                is MatchDetailUiState.Error -> {
                    Text(
                        text = state.message,
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewTab(
    match: com.pitchpulse.data.model.Match,
    onTeamClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (match.date.isNotEmpty()) "${match.competition} • ${match.date}" else match.competition,
            style = MaterialTheme.typography.labelLarge,
            color = AppAccent,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(Dimens.SpacingExtraLarge))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TeamDetail(match.homeTeam, match.homeTeamLogo) { onTeamClick(match.homeTeamId) }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (match.homeScore != null && match.awayScore != null) "${match.homeScore} - ${match.awayScore}" else "VS",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    ),
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = Dimens.SpacingMedium)
                )
                if (match.isLive) {
                    com.pitchpulse.ui.components.LiveBadge()
                } else {
                    Text(
                        text = match.time,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            TeamDetail(match.awayTeam, match.awayTeamLogo) { onTeamClick(match.awayTeamId) }
        }

        Spacer(modifier = Modifier.height(Dimens.SpacingExtraLarge))

        // Goal Scorers Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.SpacingLarge),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Home Scorers
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                match.events.filter { it.type == com.pitchpulse.data.model.EventType.GOAL && it.teamId == match.homeTeamId }
                    .forEach { event ->
                        Text(
                            text = "${event.player} ${event.minute}'",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary.copy(alpha = 0.8f)
                        )
                    }
            }

            Spacer(modifier = Modifier.width(Dimens.SpacingMedium))

            // Away Scorers
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                match.events.filter { it.type == com.pitchpulse.data.model.EventType.GOAL && it.teamId == match.awayTeamId }
                    .forEach { event ->
                        Text(
                            text = "${event.minute}' ${event.player}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary.copy(alpha = 0.8f)
                        )
                    }
            }
        }
    }
}

@Composable
private fun LineupsTab(lineups: List<com.pitchpulse.data.model.Lineup>) {
    if (lineups.isEmpty()) {
        EmptyState(
            title = "Lineups Not Available",
            subtitle = "Team formations and starting lineups will be available closer to kick-off.",
            icon = androidx.compose.material.icons.Icons.Default.Groups
        )
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            PitchVisualization(lineups)
            Spacer(modifier = Modifier.height(Dimens.SpacingExtraLarge))
        }

        items(lineups) { lineup ->
            Text(
                text = "${lineup.teamName} (${lineup.formation ?: ""})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppAccent,
                modifier = Modifier.padding(vertical = Dimens.SpacingMedium)
            )
            
            Text(
                text = "Starting XI",
                style = MaterialTheme.typography.labelMedium,
                color = TextPrimary.copy(alpha = 0.6f)
            )
            
            lineup.startXI.forEach { player ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "${player.number ?: ""} ${player.name}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = player.position ?: "", style = MaterialTheme.typography.bodySmall, color = TextPrimary.copy(alpha = 0.4f))
                }
            }
            
            Spacer(modifier = Modifier.height(Dimens.SpacingLarge))
        }
    }
}

@Composable
private fun StatsTab(stats: com.pitchpulse.data.model.MatchStatistics?) {
    if (stats == null || stats.homeStats.isEmpty()) {
        EmptyState(
            title = "Stats Not Available",
            subtitle = "Detailed match statistics will appear here once the match begins.",
            icon = androidx.compose.material.icons.Icons.Default.BarChart
        )
        return
    }

    val priorityStats = listOf("Ball Possession", "Total Shots", "Fouls")
    val featuredStats = stats.homeStats.zip(stats.awayStats).filter { it.first.type in priorityStats }
    val remainingStats = stats.homeStats.zip(stats.awayStats).filter { it.first.type !in priorityStats }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (featuredStats.isNotEmpty()) {
            item {
                Text(
                    text = "Match Analytics",
                    style = MaterialTheme.typography.titleSmall,
                    color = AppAccent,
                    modifier = Modifier.padding(bottom = Dimens.SpacingMedium)
                )
            }
            items(featuredStats) { (home, away) ->
                StatRow(type = home.type, homeValue = home.value, awayValue = away.value, isFeatured = true)
            }
            item { Spacer(modifier = Modifier.height(Dimens.SpacingLarge)) }
        }

        if (remainingStats.isNotEmpty()) {
            item {
                Text(
                    text = "General Stats",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = Dimens.SpacingMedium)
                )
            }
            items(remainingStats) { (home, away) ->
                StatRow(type = home.type, homeValue = home.value, awayValue = away.value, isFeatured = false)
            }
        }
    }
}

@Composable
private fun StatRow(type: String, homeValue: String, awayValue: String, isFeatured: Boolean) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = if (isFeatured) Dimens.SpacingMedium else Dimens.SpacingSmall)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = homeValue,
                style = if (isFeatured) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = type,
                style = MaterialTheme.typography.labelSmall,
                color = TextPrimary.copy(alpha = 0.6f)
            )
            Text(
                text = awayValue,
                style = if (isFeatured) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        LinearProgressIndicator(
            progress = {
                val homeVal = homeValue.replace("%", "").toFloatOrNull() ?: 0f
                val awayVal = awayValue.replace("%", "").toFloatOrNull() ?: 0f
                val total = homeVal + awayVal
                if (total == 0f) 0.5f else homeVal / total
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isFeatured) 6.dp else 3.dp)
                .padding(top = Dimens.SpacingSmall)
                .clip(CircleShape),
            color = AppAccent,
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        )
    }
}

@Composable
private fun PitchVisualization(lineups: List<com.pitchpulse.data.model.Lineup>) {
    if (lineups.isEmpty()) return

    val coachPitchColor = Color(0xFF1B5E20)
    val lineColor = Color.White.copy(alpha = 0.3f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(Dimens.RadiusMedium))
            .background(coachPitchColor)
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Outer boundary
            drawRect(color = lineColor, style = Stroke(width = 2.dp.toPx()))

            // Center line
            drawLine(
                color = lineColor,
                start = Offset(0f, h / 2),
                end = Offset(w, h / 2),
                strokeWidth = 2.dp.toPx()
            )

            // Center circle
            drawCircle(
                color = lineColor,
                center = Offset(w / 2, h / 2),
                radius = 40.dp.toPx(),
                style = Stroke(width = 2.dp.toPx())
            )

            // Penalty areas
            // Top
            drawRect(
                color = lineColor,
                topLeft = Offset(w * 0.2f, 0f),
                size = Size(w * 0.6f, h * 0.15f),
                style = Stroke(width = 2.dp.toPx())
            )
            // Bottom
            drawRect(
                color = lineColor,
                topLeft = Offset(w * 0.2f, h * 0.85f),
                size = Size(w * 0.6f, h * 0.15f),
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Home Team Players (Top half, but API grid is usually 1-5 from top)
        // Note: api-football grid is Y:X or similar. 1:1 is Goalkeeper.
        // We will simplify: Y coordinate is the row.
        lineups.firstOrNull()?.let { homeLineup ->
            TeamOnPitch(homeLineup.startXI, isHome = true)
        }
    }
}

@Composable
private fun BoxScope.TeamOnPitch(players: List<com.pitchpulse.data.model.LineupPlayer>, isHome: Boolean) {
    players.forEach { player ->
        val grid = player.grid ?: return@forEach
        val coords = grid.split(":")
        if (coords.size < 2) return@forEach

        val row = coords[0].toIntOrNull() ?: 0
        val col = coords[1].toIntOrNull() ?: 0

        // Map grid to % position
        // Rows: 1 (GK) to ~5 (FWD)
        // Cols: 1 to X
        val xPos = when (col) {
            1 -> 0.5f
            2 -> 0.3f
            3 -> 0.7f
            4 -> 0.15f
            5 -> 0.85f
            else -> 0.5f
        }
        
        // Adjust for home/away side
        val yPos = if (isHome) {
            (row - 1) * 0.18f + 0.05f
        } else {
            0.95f - (row - 1) * 0.18f
        }

        PlayerOnPitch(
            number = player.number?.toString() ?: "",
            name = player.name.split(" ").last(),
            modifier = Modifier.align(Alignment.TopCenter),
            biasX = xPos,
            biasY = yPos
        )
    }
}

@Composable
private fun PlayerOnPitch(number: String, name: String, modifier: Modifier, biasX: Float, biasY: Float) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val posX = maxWidth * biasX
        val posY = maxHeight * biasY

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(x = posX - 20.dp, y = posY - 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
            Text(
                text = name,
                fontSize = 8.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TeamDetail(
    name: String,
    logoUrl: String?,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(100.dp)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = logoUrl,
            contentDescription = name,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(Dimens.SpacingMedium))
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
