package com.cpen321.usermanagement.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cpen321.usermanagement.R
import com.cpen321.usermanagement.data.local.preferences.EventCondition
import com.cpen321.usermanagement.data.local.preferences.NhlDataManager
import com.cpen321.usermanagement.data.remote.dto.Game
import com.cpen321.usermanagement.ui.components.Icon
import com.cpen321.usermanagement.ui.components.TeamMatchup
import com.cpen321.usermanagement.ui.viewmodels.AuthViewModelContract
import com.cpen321.usermanagement.ui.viewmodels.TicketsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBingoTicketScreen(
    ticketsViewModel: TicketsViewModel,
    authViewModel: AuthViewModelContract,
    nhlDataManager: NhlDataManager,
    onBackClick: () -> Unit,
    onTicketCreated: () -> Unit,
    initialGameId: String? = null // optional preselected game id passed from navigation
) {
    val uiState by ticketsViewModel.uiState.collectAsState()
    val authState by authViewModel.uiState.collectAsState()
    val userId = authState.user?._id ?: ""

    var ticketName by remember { mutableStateOf("") }  // Ticket name state
    var selectedGame by remember { mutableStateOf<Game?>(null) }

    var availableEvents by remember { mutableStateOf<List<EventCondition>>(emptyList()) }
    var selectedEvents by remember { mutableStateOf(List<EventCondition?>(9) { null }) }

    var showEventPickerForIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        ticketsViewModel.loadUpcomingGames()
    }

    // If an initialGameId was provided via navigation, select that game once games are loaded.
    LaunchedEffect(uiState.availableGames, initialGameId) {
        if (initialGameId != null && selectedGame == null) {
            val match = uiState.availableGames.firstOrNull { it.id.toString() == initialGameId }
            if (match != null) {
                selectedGame = match
                // load events for the selected game
                ticketsViewModel.getEventsForGame(match.id) { events ->
                    // populate available events and clear any previously selected events
                    availableEvents = events
                    selectedEvents = List(9) { null }
                }
            }
        }
    }

    // Mirror the CreateChallenge screen layout: TopAppBar with top-right Create action and
    // scrollable content made of selection cards + bingo grid. Keep UI behavior.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text(text = stringResource(R.string.create_bingo_ticket), fontWeight = FontWeight.Bold)
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(name = R.drawable.ic_arrow_back)
                }
            },
            actions = {
                TextButton(
                    onClick = {
                        if (userId.isNotBlank() && selectedGame != null) {
                            ticketsViewModel.createTicket(
                                userId = userId,
                                name = ticketName,
                                game = selectedGame!!,
                                events = selectedEvents.filterNotNull()
                            )
                            onTicketCreated()
                        }
                    },
                    enabled = !uiState.isCreating && userId.isNotBlank() && ticketName.isNotBlank() && selectedEvents.none { it == null }
                ) {
                    Text(stringResource(R.string.create))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Ticket name
            OutlinedTextField(
                value = ticketName,
                onValueChange = { ticketName = it },
                label = { Text(stringResource(R.string.ticket_name)) },
                modifier = Modifier.fillMaxWidth()
            )

            // Game dropdown and loading handling
            if (uiState.isLoadingGames) {
                CircularProgressIndicator()
            } else if (uiState.availableGames.isEmpty()) {
                Text(stringResource(R.string.no_upcoming_games))
            } else {
                GameDropdown(
                    games = uiState.availableGames,
                    selectedGame = selectedGame,
                    onGameSelected = { game ->
                        selectedGame = game
                        ticketsViewModel.getEventsForGame(game.id) { events ->
                            availableEvents = events
                        }
                        selectedEvents = List(9) { null }
                    }
                )
            }

            // Bingo grid
            if (selectedGame != null) {
                Text(stringResource(R.string.fill_bingo_ticket), style = MaterialTheme.typography.titleMedium)
                BingoGrid(
                    selectedEvents = selectedEvents,
                    onSquareClick = { index -> showEventPickerForIndex = index },
                    onRemoveEvent = { index ->
                        selectedEvents = selectedEvents.toMutableList().also { it[index] = null }
                    },
                    nhlDataManager = nhlDataManager
                )
            }
        }

        // Event Picker Dialog
        showEventPickerForIndex?.let { index ->
            EventPickerDialog(
                allEvents = availableEvents,
                selectedEvents = selectedEvents.filterNotNull(),
                onDismiss = { showEventPickerForIndex = null },
                onEventSelected = { chosen ->
                    selectedEvents = selectedEvents.toMutableList().also { it[index] = chosen }
                    showEventPickerForIndex = null
                },
                nhlDataManager = nhlDataManager
            )
        }
    }
}

@Composable
private fun GameDropdown(
    games: List<Game>,
    selectedGame: Game?,
    onGameSelected: (Game) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedTextField(
            value = selectedGame?.let { "${it.awayTeam.abbrev} vs ${it.homeTeam.abbrev}" } ?: "",
            onValueChange = {},
            label = { Text(stringResource(R.string.select_game)) },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            enabled = false,
            readOnly = true
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            games.forEach { game ->
                DropdownMenuItem(
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            TeamMatchup(
                                awayTeamLogoUrl = game.awayTeam.logo,
                                awayTeamAbbrev = game.awayTeam.abbrev,
                                homeTeamLogoUrl = game.homeTeam.logo,
                                homeTeamAbbrev = game.homeTeam.abbrev,
                                logoSize = 32.dp,
                                showAbbrevs = true,
                                abbrevFontSize = 11.sp,
                                abbrevColor = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatDateTime(game.startTimeUTC),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onGameSelected(game)
                        expanded = false
                    }
                )

            }
        }
    }
}

@Composable
private fun BingoGrid(
    selectedEvents: List<EventCondition?>,
    onSquareClick: (Int) -> Unit,
    onRemoveEvent: (Int) -> Unit,
    nhlDataManager: NhlDataManager
) {
    Column {
        for (row in 0 until 3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                for (col in 0 until 3) {
                    val index = row * 3 + col
                    BingoSquare(
                        eventText = selectedEvents[index]?.let {
                            nhlDataManager.formatEventLabel(it)
                        } ?: "",
                        onClick = { onSquareClick(index) },
                        onRemove = { onRemoveEvent(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BingoSquare(
    eventText: String,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(4.dp)
            .size(100.dp)
            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (eventText.isBlank()) {
            Text(stringResource(R.string.tap_to_add), style = MaterialTheme.typography.bodyMedium)
        } else {
            Box(Modifier.fillMaxSize()) {
                Text(
                    text = eventText,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(6.dp)
                )
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(20.dp)
                ) {
                    Icon(name = R.drawable.ic_delete_forever)
                }
            }
        }
    }
}

@Composable
private fun EventPickerDialog(
    allEvents: List<EventCondition>,
    selectedEvents: List<EventCondition>,
    onDismiss: () -> Unit,
    onEventSelected: (EventCondition) -> Unit,
    nhlDataManager: NhlDataManager
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text("Select an Event") },
        text = {
            LazyColumn {
                val available = allEvents.filterNot { it in selectedEvents }
                items(available.size) { index ->
                    val event = available[index]
                    Text(
                        text = nhlDataManager.formatEventLabel(event),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEventSelected(event) }
                            .padding(8.dp)
                    )
                }
            }
        }
    )
}
