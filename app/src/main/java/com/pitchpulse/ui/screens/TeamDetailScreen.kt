package com.pitchpulse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.pitchpulse.core.ui.Dimens
import com.pitchpulse.data.model.TeamDetails
import com.pitchpulse.ui.components.MatchRow
import com.pitchpulse.ui.state.MatchUiState
import com.pitchpulse.ui.theme.AppAccent
import com.pitchpulse.ui.theme.AppSurface
import com.pitchpulse.ui.theme.TextPrimary
import com.pitchpulse.ui.theme.TextSecondary
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.pitchpulse.ui.viewmodel.TeamDetailUiState
import com.pitchpulse.ui.viewmodel.TeamDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamDetailScreen(
    viewModel: TeamDetailViewModel,
    onBack: () -> Unit,
    onMatchClick: (Int) -> Unit,
    onTeamClick: (Int) -> Unit // Added for nested team navigation
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()

    val pullToRefreshState = rememberPullToRefreshState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Team Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = if (isFavorite) "Unfavorite" else "Favorite",
                            tint = if (isFavorite) AppAccent else TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppSurface,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary,
                    actionIconContentColor = TextPrimary
                )
            )
        },
        containerColor = AppSurface
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            state = pullToRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                    containerColor = AppSurface,
                    color = AppAccent
                )
            }
        ) {
            when (val state = uiState) {
                is TeamDetailUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = AppAccent
                    )
                }
                is TeamDetailUiState.Success -> {
                    TeamDetailContent(
                        state = state,
                        onMatchClick = onMatchClick,
                        onTeamClick = onTeamClick
                    )
                }
                is TeamDetailUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(Dimens.SpacingMedium))
                        Button(onClick = { viewModel.refresh() }) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamDetailContent(
    state: TeamDetailUiState.Success,
    onMatchClick: (Int) -> Unit,
    onTeamClick: (Int) -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Upcoming", "Recent Results")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Dimens.SpacingExtraLarge)
    ) {
        item {
            TeamHeader(team = state.team)
        }

        item {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = AppSurface,
                contentColor = AppAccent,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = AppAccent
                    )
                },
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(Dimens.SpacingMedium))
        }

        val fixtures = if (selectedTabIndex == 0) state.upcomingFixtures else state.pastFixtures

        if (fixtures.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.SpacingExtraLarge),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No fixtures found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                }
            }
        } else {
            items(fixtures) { match ->
                MatchRow(
                    match = match,
                    favoriteTeamIds = emptySet(), // No favoriting toggle here for simplicity
                    onFavoriteToggle = { _, _, _ -> },
                    onMatchClick = onMatchClick,
                    onTeamClick = onTeamClick,
                    modifier = Modifier.padding(horizontal = Dimens.SpacingSmall)
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = Dimens.SpacingLarge),
                    thickness = 0.5.dp,
                    color = Color.White.copy(alpha = 0.1f)
                )
            }
        }
    }
}

@Composable
private fun TeamHeader(team: TeamDetails) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
    ) {
        // Background Image / Gradient
        if (team.venueImage != null) {
            AsyncImage(
                model = team.venueImage,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.3f
            )
        }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, AppSurface),
                        startY = 0f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimens.SpacingLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AsyncImage(
                model = team.logo,
                contentDescription = team.name,
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(Dimens.RadiusMedium)),
                contentScale = ContentScale.Fit
            )
            
            Spacer(modifier = Modifier.height(Dimens.SpacingMedium))
            
            Text(
                text = team.name,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            if (team.venueName != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = Dimens.SpacingSmall)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${team.venueName}, ${team.venueCity ?: ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            if (team.founded != null) {
                Text(
                    text = "Est. ${team.founded}",
                    style = MaterialTheme.typography.labelMedium,
                    color = AppAccent.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
