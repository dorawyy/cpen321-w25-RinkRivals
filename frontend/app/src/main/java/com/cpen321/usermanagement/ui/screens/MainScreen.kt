package com.cpen321.usermanagement.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cpen321.usermanagement.R
import com.cpen321.usermanagement.data.remote.dto.Game
import com.cpen321.usermanagement.data.local.preferences.NhlDataManager
import com.cpen321.usermanagement.ui.components.BingoTicketCard
import com.cpen321.usermanagement.ui.components.MessageSnackbar
import com.cpen321.usermanagement.ui.components.MessageSnackbarState
import com.cpen321.usermanagement.ui.viewmodels.MainUiState
import com.cpen321.usermanagement.ui.viewmodels.MainViewModel



@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    onTicketClick: () -> Unit,
) {
    val uiState by mainViewModel.uiState.collectAsState()
    val nhlDataState by mainViewModel.nhlDataState.collectAsState()
    val snackBarHostState = remember { SnackbarHostState() }

    // Side effects
    LaunchedEffect(Unit) {
        mainViewModel.clearSuccessMessage()
        mainViewModel.loadGameSchedule()
        if (uiState.user == null) {
            mainViewModel.loadProfile()
        }
    }

    MainContent(
        uiState = uiState,
        snackBarHostState = snackBarHostState,
        onTicketClick = onTicketClick,
        onSuccessMessageShown = mainViewModel::clearSuccessMessage,
        upcomingGames = nhlDataState.gameSchedule?.flatMap { it.games }?.take(15) ?: emptyList(),
        nhlDataManager = mainViewModel.nhlDataManager
    )
}

@Composable
private fun MainContent(
    uiState: MainUiState,
    snackBarHostState: SnackbarHostState,
    onTicketClick: () -> Unit,
    onSuccessMessageShown: () -> Unit,
    upcomingGames: List<Game>,
    nhlDataManager: NhlDataManager,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            MainTopBar()
        },
        snackbarHost = {
            MainSnackbarHost(
                hostState = snackBarHostState,
                successMessage = uiState.successMessage,
                onSuccessMessageShown = onSuccessMessageShown
            )
        }
    ) { paddingValues ->
        MainBody(
                paddingValues = paddingValues,
                uiState = uiState,
                upcomingGames = upcomingGames,
                onTicketClick = onTicketClick,
                nhlDataManager = nhlDataManager
            )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopBar(
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier,
        title = {
            AppTitle()
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun AppTitle(
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Medium,
        modifier = modifier
    )
}

@Composable
private fun MainSnackbarHost(
    hostState: SnackbarHostState,
    successMessage: String?,
    onSuccessMessageShown: () -> Unit,
    modifier: Modifier = Modifier
) {
    MessageSnackbar(
        hostState = hostState,
        messageState = MessageSnackbarState(
            successMessage = successMessage,
            errorMessage = null,
            onSuccessMessageShown = onSuccessMessageShown,
            onErrorMessageShown = { }
        ),
        modifier = modifier
    )
}

@Composable
private fun MainBody(
    paddingValues: PaddingValues,
    uiState: MainUiState,
    upcomingGames: List<Game>,
    onTicketClick: () -> Unit,
    nhlDataManager: NhlDataManager,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HeroBanner(uiState = uiState)
        }
        
        // If user has created tickets, show a list of their live tickets.
        // Otherwise show the prominent "Create Your First Bingo Ticket" card.
        if (!uiState.userTickets.isNullOrEmpty()) {
            val liveTickets = uiState.userTickets
                .filter {
                    val state = it.game.gameState ?: "OFF"
                    val u = state.uppercase()
                    u == "LIVE" || u == "CRIT" || u == "FUT"
                }


            if (liveTickets.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.your_live_upcoming_tickets),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(liveTickets) { ticket ->
                    BingoTicketCard(
                        ticket = ticket,
                        nhlDataManager = nhlDataManager,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        } else if (uiState.userTickets.isEmpty() && !uiState.isLoadingTickets) {
            item {
                CreateFirstTicketCard(onClick = onTicketClick)
            }
        }
        
        // Upcoming/Live Games Section
        item {
            Text(
                text = stringResource(R.string.live_upcoming_games),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        
        items(upcomingGames) { game ->
            com.cpen321.usermanagement.ui.components.GameCard(
                game = game,
                nhlDataManager = nhlDataManager,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HeroBanner(
    uiState: MainUiState,
    modifier: Modifier = Modifier
) {
    val userName = uiState.user?.name ?: "Guest"
    val gradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF0F172A),
            Color(0xFF1E40AF),
            Color(0xFF0B8BD9)
        )
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .background(gradient)
                .fillMaxSize()
        ) {
            // Animated Puck on the left
            AnimatedPuck(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 24.dp)
            )

            // Welcome Text in the center
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 100.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.welcome_back, userName),
                    color = Color(0xFFCCF2FF),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.predict_hockey),
                    color = Color(0xFFE0F2FE),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun AnimatedPuck(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "puck rotation")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = LinearEasing)
        ),
        label = "rotation"
    )

    Canvas(modifier = modifier.size(70.dp)) {
        rotate(rotation) {
            // Outer black circle (puck)
            drawCircle(
                color = Color.Black,
                radius = size.minDimension / 2
            )
            // Inner gray circle (puck detail)
            drawCircle(
                color = Color.DarkGray,
                radius = size.minDimension / 2.6f,
                center = Offset(size.width * 0.35f, size.height * 0.35f)
            )
        }
    }
}

@Composable
private fun CreateFirstTicketCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.create_first_ticket),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.start_predicting),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.create_ticket))
            }
        }
    }
}

