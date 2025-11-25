package com.cpen321.usermanagement.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cpen321.usermanagement.R
import com.cpen321.usermanagement.data.remote.dto.BingoTicket
import com.cpen321.usermanagement.data.remote.dto.TicketsUiState
import com.cpen321.usermanagement.ui.components.BingoTicketCard
import com.cpen321.usermanagement.ui.viewmodels.AuthViewModelContract
import com.cpen321.usermanagement.ui.viewmodels.TicketsViewModel
import kotlinx.coroutines.launch

data class TicketsScreenActions(
    val onBackClick: () -> Unit,
    val onCreateTicketClick: () -> Unit
)

private data class TicketsScreenCallbacks(
    val onBackClick: () -> Unit,
    val onCreateTicketClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketsScreen(
    authViewModel: AuthViewModelContract,
    actions: TicketsScreenActions,
    ticketsViewModel: TicketsViewModel
) {
    val uiState by ticketsViewModel.uiState.collectAsState()
    val authState by authViewModel.uiState.collectAsState()
    val userId = authState.user?._id ?: ""

    val snackbarHostState = remember { SnackbarHostState() }

    // Side effects: clear messages and load tickets
    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            ticketsViewModel.clearSuccessMessage()
            ticketsViewModel.clearError()
            ticketsViewModel.loadTickets(userId)
        }
    }

    TicketsContent(
        uiState = uiState,
        ticketsViewModel = ticketsViewModel,
        callbacks = TicketsScreenCallbacks(
            onBackClick = actions.onBackClick,
            onCreateTicketClick = actions.onCreateTicketClick
        ),
        snackbarHostState = snackbarHostState
    )
}

@Composable
private fun TicketsContent(
    modifier: Modifier = Modifier,
    uiState: TicketsUiState,
    ticketsViewModel: TicketsViewModel,
    callbacks: TicketsScreenCallbacks,
    snackbarHostState: SnackbarHostState
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TicketsTopBar()
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        TicketsBody(
            paddingValues = paddingValues,
            allTickets = uiState.allTickets,
            isLoading = uiState.isLoadingTickets,
            ticketsViewModel = ticketsViewModel,
            onCreateTicketClick = callbacks.onCreateTicketClick,
            snackbarHostState = snackbarHostState
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketsTopBar(
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = stringResource(R.string.bingo_tickets),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}


@Composable
private fun TicketsBody(
    paddingValues: PaddingValues,
    allTickets: List<BingoTicket>,
    isLoading: Boolean,
    ticketsViewModel: TicketsViewModel,
    onCreateTicketClick: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState
) {

    // For each ticket, find its full game data from the NhlDataManager.
    // If the game is not found (finished games), keep the original ticket
    val enrichedTickets = remember(allTickets, ticketsViewModel.nhlDataManager.getGamesForTickets()) {
        allTickets.map { ticket ->
            // Look up the game using the gameId stored on the ticket
            val fullGame = ticketsViewModel.nhlDataManager.getGameById(ticket.game.id)
            if (fullGame != null) {
                // Create a new BingoTicket instance with the full game data
                ticket.copy(game = fullGame)
            } else {
                // Game not found (likely finished), keep original ticket
                ticket
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator()
            }
            allTickets.isEmpty() -> {
                EmptyState(
                    modifier = Modifier.align(Alignment.Center),
                    message = stringResource(R.string.you_have_no_tickets),
                    actionText = stringResource(R.string.create_one_now)
                ) {
                    onCreateTicketClick()
                }
            }
            else -> {
                // The LazyColumn is now inside the body, just like in ChallengesScreen
                TicketsList(
                    modifier = Modifier.fillMaxSize(),
                    allTickets = enrichedTickets,
                    ticketsViewModel = ticketsViewModel,
                    snackbarHostState = snackbarHostState
                )
            }
        }
        // The button is placed in the Box to overlay the list
        if (!isLoading) {
            AddTicketButton(
                onClick = onCreateTicketClick,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            )
        }
    }
}


@Composable
fun TicketsList(
    modifier: Modifier = Modifier,
    allTickets: List<BingoTicket> = emptyList(),
    ticketsViewModel: TicketsViewModel,
    snackbarHostState: SnackbarHostState
) {
    // print all ticket game states
    println("All tickets: $allTickets")
    println("Ticket game states: ${allTickets.map { it.game.gameState }}")

    val gameStateOrder = listOf("CRIT", "LIVE", "PRE", "FUT", "FINAL", "OFF")

    // Group tickets by gameState, treating null/empty gameState as "OFF"
    val groupedTickets = allTickets.groupBy { 
        it.game.gameState?.takeIf { state -> state.isNotBlank() } ?: "OFF"
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp), // Space for FAB
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // This is the correct pattern from ChallengesScreen
        gameStateOrder.forEach { gameState ->
            val ticketsInState = groupedTickets[gameState].orEmpty()
            if (ticketsInState.isNotEmpty()) {
                item {
                    CollapsibleTicketsSection(
                        gameState = gameState,
                        tickets = ticketsInState,
                        ticketsViewModel = ticketsViewModel,
                        snackbarHostState = snackbarHostState
                    )
                }
            }
        }
    }
}

// Create this NEW self-contained composable, replacing the old CollapsibleSectionHeader
@Composable
private fun CollapsibleTicketsSection(
    gameState: String,
    tickets: List<BingoTicket>,
    ticketsViewModel: TicketsViewModel,snackbarHostState: SnackbarHostState
) {
    // State is managed internally, just like in ChallengesScreen
    var isExpanded by remember { mutableStateOf(gameState.uppercase() == "LIVE") }
    val scope = rememberCoroutineScope()

    // 1. More descriptive names for the UI
    val sectionTitle = when (gameState.uppercase()) {
        "LIVE" -> stringResource(R.string.live)
        "CRIT" -> stringResource(R.string.critical)
        "PRE" -> stringResource(R.string.pre_game)
        "FUT" -> stringResource(R.string.upcoming)
        "FINAL" -> stringResource(R.string.game_final)
        "OFF" -> stringResource(R.string.game_final)
        else -> gameState
    }

    // 2. Clearer color scheme
    val statusColor = when (gameState.uppercase()) {
        "LIVE" -> MaterialTheme.colorScheme.errorContainer
        "CRIT" -> Color(0xFFFFA000)  // Orange
        "PRE" -> Color(0xFFC8E6C9) // Light Green
        "FUT" -> MaterialTheme.colorScheme.primaryContainer // purple
        "FINAL" -> MaterialTheme.colorScheme.secondaryContainer
        "OFF" -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    // Text color that works well
    val onStatusColor = when (gameState.uppercase()) {
        "LIVE" -> MaterialTheme.colorScheme.onErrorContainer
        "PRE" -> Color(0xFF1B5E20) // Dark Green
        "FUT" -> MaterialTheme.colorScheme.onPrimaryContainer
        "FINAL" -> MaterialTheme.colorScheme.onSecondaryContainer
        "OFF" -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }



    // The entire section is a single Column
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = statusColor,
            ) {
                Text(
                    text = sectionTitle, // Use the new descriptive title
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = onStatusColor,
                    fontWeight = FontWeight.Bold
                )
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand"
            )
        }

        // AnimatedVisibility controls the list of tickets
        AnimatedVisibility(visible = isExpanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tickets.forEach { ticket ->
                    // Your BingoTicketCard composable goes here
                    BingoTicketCard(
                        ticket = ticket,
                        nhlDataManager = ticketsViewModel.nhlDataManager,
                        onClick = { ticketsViewModel.selectTicket(ticket._id) },
                        onDelete = {
                            ticketsViewModel.checkIfTicketIsUsed(ticket) { isUsed ->
                                if (isUsed) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "This ticket is in an active challenge and cannot be deleted.",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                } else {
                                    ticketsViewModel.deleteTicket(ticket._id)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}


@Composable
private fun EmptyState(
    modifier: Modifier = Modifier,
    message: String,
    actionText: String,
    onActionClick: () -> Unit
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onActionClick) {
            Text(text = actionText)
        }
    }
}

@Composable
private fun AddTicketButton(
    onClick: () -> Unit,    modifier: Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(56.dp), // Set a fixed height for a larger touch target
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
        contentPadding = PaddingValues(
            start = 24.dp,
            top = 16.dp,
            end = 24.dp,
            bottom = 16.dp
        ) // Increase padding around the content
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize)
        )
        Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
        Text(
            text = "New Bingo Ticket",
            fontSize = 16.sp, // Increase the font size
            fontWeight = FontWeight.Medium
        )
    }
}
