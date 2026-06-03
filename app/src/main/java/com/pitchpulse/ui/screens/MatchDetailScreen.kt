package com.pitchpulse.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.pitchpulse.ui.theme.AppAccentMuted
import com.pitchpulse.ui.theme.AppBackground
import com.pitchpulse.ui.theme.TextPrimary
import com.pitchpulse.ui.theme.TextSecondary
import com.pitchpulse.ui.theme.TextMuted
import com.pitchpulse.ui.viewmodel.MatchDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailScreen(
    viewModel: MatchDetailViewModel,
    onBack: () -> Unit,
    onTeamClick: (Int) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
                        // 1. Unified Scoreboard Card
                        ScoreboardHeaderCard(
                            match = state.match,
                            onTeamClick = onTeamClick
                        )

                        // 2. Custom Tabs Row
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = AppAccent,
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier
                                        .tabIndicatorOffset(tabPositions[selectedTab])
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)),
                                    color = AppAccent
                                )
                            },
                            divider = {
                                HorizontalDivider(color = Color(0xFF252E38))
                            }
                        ) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    text = { 
                                        Text(
                                            text = title,
                                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedTab == index) AppAccent else TextSecondary
                                        ) 
                                    }
                                )
                            }
                        }

                        // 3. Tab Content Section
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
private fun ScoreboardHeaderCard(
    match: com.pitchpulse.data.model.Match,
    onTeamClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.SpacingLarge, vertical = Dimens.SpacingMedium),
        shape = RoundedCornerShape(Dimens.RadiusLarge),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (match.isLive) AppAccent else Color(0xFF252E38)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.SpacingLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Competition & Date
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = match.competition.uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = AppAccent
                )
                if (match.date.isNotEmpty()) {
                    Text(
                        text = "  •  ${match.date}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(Dimens.SpacingLarge))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home Team
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTeamClick(match.homeTeamId) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(Dimens.LogoSizeLarge)
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = CircleShape
                            )
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = match.homeTeamLogo,
                            contentDescription = match.homeTeam,
                            modifier = Modifier.size(Dimens.LogoSizeMedium)
                        )
                    }
                    Spacer(modifier = Modifier.height(Dimens.SpacingMedium))
                    Text(
                        text = match.homeTeam,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                
                // Score & Status
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = Dimens.SpacingMedium)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (match.homeScore != null && match.awayScore != null) {
                            Text(
                                text = match.homeScore.toString(),
                                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = TextPrimary
                            )
                            Text(
                                text = " - ",
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AppAccent
                                ),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            Text(
                                text = match.awayScore.toString(),
                                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = TextPrimary
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(Dimens.RadiusSmall)
                                    )
                                    .padding(horizontal = Dimens.SpacingLarge, vertical = Dimens.SpacingMedium)
                            ) {
                                Text(
                                    text = "VS",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(Dimens.SpacingSmall))
                    
                    // Status Badge
                    if (match.isLive) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .background(
                                    color = Color(0xFF1B3A2C),
                                    shape = RoundedCornerShape(100.dp)
                                )
                                .padding(horizontal = Dimens.SpacingMedium, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(AppAccent, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = match.time,
                                color = AppAccent,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    } else {
                        Text(
                            text = match.time,
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(100.dp))
                                .padding(horizontal = Dimens.SpacingMedium, vertical = 4.dp)
                        )
                    }
                }
                
                // Away Team
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTeamClick(match.awayTeamId) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(Dimens.LogoSizeLarge)
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = CircleShape
                            )
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = match.awayTeamLogo,
                            contentDescription = match.awayTeam,
                            modifier = Modifier.size(Dimens.LogoSizeMedium)
                        )
                    }
                    Spacer(modifier = Modifier.height(Dimens.SpacingMedium))
                    Text(
                        text = match.awayTeam,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = Dimens.SpacingMedium),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingLarge)
    ) {
        item {
            MatchEventsTimeline(match = match)
        }
    }
}

@Composable
private fun MatchEventsTimeline(
    match: com.pitchpulse.data.model.Match,
    modifier: Modifier = Modifier
) {
    val events = remember(match.events) {
        match.events.sortedBy { it.minute }
    }

    if (events.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = Dimens.SpacingHuge),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(Dimens.SpacingMedium))
                Text(
                    text = "No key events recorded yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.SpacingMedium)
    ) {
        Text(
            text = "MATCH TIMELINE",
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp),
            color = AppAccent,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = Dimens.SpacingLarge)
        )

        events.forEachIndexed { index, event ->
            val isHome = event.teamId == match.homeTeamId
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Side (Home)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = Dimens.SpacingMedium),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    if (isHome) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = event.player,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(Dimens.SpacingSmall))
                            EventIcon(event.type)
                        }
                    }
                }

                // Middle Column (Timeline Connector & Time Badge)
                Column(
                    modifier = Modifier
                        .width(54.dp)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Line above marker
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .width(2.dp)
                            .background(
                                if (index == 0) Color.Transparent else Color(0xFF252E38)
                            )
                    )

                    // Minute Badge
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = CircleShape
                            )
                            .border(
                                width = 1.5.dp,
                                color = if (event.type == com.pitchpulse.data.model.EventType.GOAL) AppAccent else Color(0xFF252E38),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${event.minute}'",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (event.type == com.pitchpulse.data.model.EventType.GOAL) AppAccent else TextSecondary
                        )
                    }

                    // Line below marker
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .width(2.dp)
                            .background(
                                if (index == events.lastIndex) Color.Transparent else Color(0xFF252E38)
                            )
                    )
                }

                // Right Side (Away)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = Dimens.SpacingMedium),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (!isHome) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            EventIcon(event.type)
                            Spacer(modifier = Modifier.width(Dimens.SpacingSmall))
                            Text(
                                text = event.player,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventIcon(type: com.pitchpulse.data.model.EventType) {
    when (type) {
        com.pitchpulse.data.model.EventType.GOAL -> {
            Text("⚽", fontSize = 14.sp)
        }
        com.pitchpulse.data.model.EventType.YELLOW_CARD -> {
            Box(
                modifier = Modifier
                    .size(width = 10.dp, height = 14.dp)
                    .background(Color(0xFFFFD54F), RoundedCornerShape(2.dp))
            )
        }
        com.pitchpulse.data.model.EventType.RED_CARD -> {
            Box(
                modifier = Modifier
                    .size(width = 10.dp, height = 14.dp)
                    .background(Color(0xFFE53935), RoundedCornerShape(2.dp))
            )
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingLarge)
    ) {
        item {
            PitchVisualization(lineups)
        }

        items(lineups.size) { index ->
            val lineup = lineups[index]
            val isHome = index == 0
            TeamSquadCard(lineup = lineup, isHome = isHome)
        }
    }
}

@Composable
private fun PitchVisualization(lineups: List<com.pitchpulse.data.model.Lineup>) {
    if (lineups.isEmpty()) return

    val coachPitchColor = Color(0xFF0D3310)
    val lineColor = Color(0xFF245928)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(380.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(Dimens.RadiusMedium))
            .background(coachPitchColor)
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Outer boundary
            drawRect(color = lineColor, style = Stroke(width = 1.5.dp.toPx()))

            // Center line
            drawLine(
                color = lineColor,
                start = Offset(0f, h / 2),
                end = Offset(w, h / 2),
                strokeWidth = 1.5.dp.toPx()
            )

            // Center circle
            drawCircle(
                color = lineColor,
                center = Offset(w / 2, h / 2),
                radius = 35.dp.toPx(),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Penalty areas
            // Top (Away)
            drawRect(
                color = lineColor,
                topLeft = Offset(w * 0.2f, 0f),
                size = Size(w * 0.6f, h * 0.15f),
                style = Stroke(width = 1.5.dp.toPx())
            )
            // Bottom (Home)
            drawRect(
                color = lineColor,
                topLeft = Offset(w * 0.2f, h * 0.85f),
                size = Size(w * 0.6f, h * 0.15f),
                style = Stroke(width = 1.5.dp.toPx())
            )
            
            // Goal areas
            // Top
            drawRect(
                color = lineColor,
                topLeft = Offset(w * 0.35f, 0f),
                size = Size(w * 0.3f, h * 0.05f),
                style = Stroke(width = 1.5.dp.toPx())
            )
            // Bottom
            drawRect(
                color = lineColor,
                topLeft = Offset(w * 0.35f, h * 0.95f),
                size = Size(w * 0.3f, h * 0.05f),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        // Home Team (Bottom half)
        lineups.firstOrNull()?.let { homeLineup ->
            TeamOnPitch(homeLineup, isHome = true)
        }

        // Away Team (Top half)
        if (lineups.size > 1) {
            TeamOnPitch(lineups[1], isHome = false)
        }
    }
}

private data class ParsedPlayer(
    val player: com.pitchpulse.data.model.LineupPlayer,
    val row: Int,
    val col: Int
)

@Composable
private fun BoxScope.TeamOnPitch(
    lineup: com.pitchpulse.data.model.Lineup,
    isHome: Boolean
) {
    val parsedPlayers = remember(lineup.startXI) {
        lineup.startXI.mapNotNull { player ->
            val grid = player.grid ?: return@mapNotNull null
            val parts = grid.split(":")
            if (parts.size < 2) return@mapNotNull null
            val row = parts[0].toIntOrNull() ?: return@mapNotNull null
            val col = parts[1].toIntOrNull() ?: return@mapNotNull null
            ParsedPlayer(player, row, col)
        }
    }

    val playersByRow = remember(parsedPlayers) {
        parsedPlayers.groupBy { it.row }
    }

    playersByRow.forEach { (row, rowPlayers) ->
        val sortedRowPlayers = remember(rowPlayers) {
            rowPlayers.sortedBy { it.col }
        }
        val n = sortedRowPlayers.size
        
        sortedRowPlayers.forEachIndexed { colIndex, parsed ->
            val xPos = (colIndex + 1).toFloat() / (n + 1).toFloat()
            val yPos = if (isHome) {
                0.90f - (row - 1) * 0.09f
            } else {
                0.10f + (row - 1) * 0.09f
            }

            PlayerOnPitch(
                number = parsed.player.number?.toString() ?: "",
                name = parsed.player.name.split(" ").last(),
                isHome = isHome,
                biasX = xPos,
                biasY = yPos
            )
        }
    }
}

@Composable
private fun PlayerOnPitch(
    number: String,
    name: String,
    isHome: Boolean,
    biasX: Float,
    biasY: Float
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val posX = maxWidth * biasX
        val posY = maxHeight * biasY

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(x = posX - 24.dp, y = posY - 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isHome) AppAccent else Color.White)
                    .border(1.dp, AppBackground, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = name,
                fontSize = 9.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier
                    .background(AppBackground, RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun TeamSquadCard(
    lineup: com.pitchpulse.data.model.Lineup,
    isHome: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.SpacingMedium),
        shape = RoundedCornerShape(Dimens.RadiusMedium),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = Color(0xFF252E38)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.SpacingLarge)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = lineup.teamLogo,
                        contentDescription = lineup.teamName,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(Dimens.SpacingMedium))
                Column {
                    Text(
                        text = lineup.teamName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (lineup.formation != null) {
                        Text(
                            text = "Formation: ${lineup.formation}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingLarge))

            Text(
                text = "STARTING XI",
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                color = TextSecondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = Dimens.SpacingSmall)
            )

            lineup.startXI.forEach { player ->
                PlayerRow(player = player, badgeColor = if (isHome) AppAccent else Color.White)
            }

            if (lineup.substitutes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Dimens.SpacingLarge))
                
                Text(
                    text = "SUBSTITUTES",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = Dimens.SpacingSmall)
                )

                lineup.substitutes.forEach { player ->
                    PlayerRow(
                        player = player,
                        badgeColor = if (isHome) AppAccentMuted else Color(0xFFB0BEC5)
                    )
                }
            }

            if (!lineup.coachName.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(Dimens.SpacingLarge))
                HorizontalDivider(color = Color(0xFF252E38))
                Spacer(modifier = Modifier.height(Dimens.SpacingMedium))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = "Coach",
                        tint = AppAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(Dimens.SpacingMedium))
                    Text(
                        text = "Coach: ${lineup.coachName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerRow(
    player: com.pitchpulse.data.model.LineupPlayer,
    badgeColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(badgeColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = player.number?.toString() ?: "-",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
            Spacer(modifier = Modifier.width(Dimens.SpacingMedium))
            Text(
                text = player.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
        Text(
            text = player.position ?: "",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)
    ) {
        if (featuredStats.isNotEmpty()) {
            item {
                Text(
                    text = "MATCH ANALYTICS",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp),
                    color = AppAccent,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = Dimens.SpacingMedium)
                )
            }
            items(featuredStats) { (home, away) ->
                StatRow(type = home.type, homeValue = home.value, awayValue = away.value, isFeatured = true)
            }
        }

        if (remainingStats.isNotEmpty()) {
            item {
                Text(
                    text = "GENERAL STATS",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp),
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = Dimens.SpacingLarge, bottom = Dimens.SpacingMedium)
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.SpacingSmall)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(Dimens.RadiusSmall)
            )
            .padding(horizontal = Dimens.SpacingLarge, vertical = Dimens.SpacingMedium)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = homeValue,
                style = if (isFeatured) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isFeatured) AppAccent else TextPrimary
            )
            Text(
                text = type.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                color = TextSecondary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = awayValue,
                style = if (isFeatured) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isFeatured) Color.White else TextPrimary
            )
        }
        
        Spacer(modifier = Modifier.height(Dimens.SpacingSmall))

        CustomComparativeProgress(
            homeValue = homeValue,
            awayValue = awayValue,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CustomComparativeProgress(
    homeValue: String,
    awayValue: String,
    modifier: Modifier = Modifier
) {
    val homeVal = homeValue.replace("%", "").toFloatOrNull() ?: 0f
    val awayVal = awayValue.replace("%", "").toFloatOrNull() ?: 0f
    val total = homeVal + awayVal
    
    val homeRatio = if (total == 0f) 0.5f else homeVal / total
    val awayRatio = if (total == 0f) 0.5f else awayVal / total

    val trackColor = Color(0xFF252E38)
    val homeColor = AppAccent
    val awayColor = Color.White

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(trackColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            
            // Home segment
            val homeWidth = w * homeRatio
            drawRect(
                color = homeColor,
                topLeft = Offset(0f, 0f),
                size = Size(homeWidth, h)
            )

            // Away segment
            val awayWidth = w * awayRatio
            drawRect(
                color = awayColor,
                topLeft = Offset(w - awayWidth, 0f),
                size = Size(awayWidth, h)
            )

            // Center separator line (using solid background color to keep zero transparency strategy)
            drawLine(
                color = AppBackground,
                start = Offset(w / 2, 0f),
                end = Offset(w / 2, h),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}
