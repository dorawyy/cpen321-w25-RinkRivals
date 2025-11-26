package com.cpen321.usermanagement.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.cpen321.usermanagement.R
import com.cpen321.usermanagement.data.remote.dto.BingoTicket
import com.cpen321.usermanagement.data.remote.dto.CreateChallengeRequest
import com.cpen321.usermanagement.data.remote.dto.Game
import com.cpen321.usermanagement.ui.components.TeamMatchup
import com.cpen321.usermanagement.ui.viewmodels.ChallengesViewModel
import com.cpen321.usermanagement.ui.viewmodels.Friend
import com.cpen321.usermanagement.utils.FormatUtils.formatDateTime



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChallengeScreen(
    challengesViewModel: ChallengesViewModel,
    onBackClick: () -> Unit,
    onChallengeCreated: () -> Unit
) {
    // Collect UI state from ViewModel
    val uiState by challengesViewModel.uiState.collectAsState()
    
    // Load NHL games when screen opens
    LaunchedEffect(Unit) {
        challengesViewModel.loadProfile()
        challengesViewModel.loadUpcomingGames()

    }
    
    // Load friends once we have the user ID
    LaunchedEffect(uiState.user?._id) {
        uiState.user?._id?.let { userId ->
            challengesViewModel.loadFriends(userId)
            challengesViewModel.loadAvailableTickets(userId)
        }
    }
    
    // State variables
    var selectedGame by remember { mutableStateOf<Game?>(null) }
    var selectedTicket by remember { mutableStateOf<BingoTicket?>(null) }
    var challengeTitle by remember { mutableStateOf("") }
    var challengeDescription by remember { mutableStateOf("") }
    var maxMembers by remember { mutableStateOf("10") }
    var selectedFriends by remember { mutableStateOf<Set<Friend>>(emptySet()) }
    
    // Dialog states
    var showGamePicker by remember { mutableStateOf(false) }
    var showTicketPicker by remember { mutableStateOf(false) }
    var showFriendsPicker by remember { mutableStateOf(false) }
    
    // Get games from ViewModel
    val availableGames = uiState.availableGames
    val availableTickets = uiState.availableTicketsForJoining
    Log.d("CreateChallengeScreen", "Available games: $availableGames")
    Log.d("CreateChallengeScreen", "Available tickets: $availableTickets")


    // Filter tickets based on selected game
    val gameTickets = availableTickets?.filter { it.game.id.toString() == selectedGame?.id.toString() }

    
    // Use friends from ViewModel state instead of dummy data
    val availableFriends = uiState.allFriends ?: emptyList()
    

    
    // Update challenge title when game changes
    LaunchedEffect(selectedGame) {
        if (selectedGame != null && challengeTitle.isEmpty()) {
            challengeTitle = "${selectedGame!!.awayTeam.abbrev} vs ${selectedGame!!.homeTeam.abbrev}"
        }
    }
    
    // Validation
    val canCreateChallenge = selectedGame != null && 
                           selectedTicket != null && 
                           challengeTitle.isNotBlank() &&
                           maxMembers.toIntOrNull() != null &&
                           maxMembers.toInt() >= 2

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = { 
                Text(
                    text = "Create Challenge",
                    fontWeight = FontWeight.Bold
                ) 
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            actions = {
                TextButton(
                    onClick = {
                        if (canCreateChallenge) {
                            // Create challenge request
                            val challengeRequest = CreateChallengeRequest(
                                title = challengeTitle,
                                description = challengeDescription.ifBlank { "Join my hockey prediction challenge!" },
                                gameId = selectedGame!!.id.toString(),
                                invitedUserIds = selectedFriends.map { it.id },
                                maxMembers = maxMembers.toIntOrNull(),
                                gameStartTime = null, // Will be handled by backend from gameId
                                ticketId = selectedTicket?._id // Include owner's selected ticket
                            )
                            
                            // Call ViewModel to create challenge
                            challengesViewModel.createChallenge(challengeRequest)
                            onChallengeCreated()
                        }
                    },
                    enabled = canCreateChallenge
                ) {
                    Text("Create")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        // Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Game Selection Card
            SelectionCard(
                title = "Select Game",
                iconPainter = androidx.compose.ui.res.painterResource(id = R.drawable.hockey_outlined_icon),
                selectedText = selectedGame?.let { "${it.awayTeam.abbrev} vs ${it.homeTeam.abbrev}" } ?: "Choose a game",
                onClick = { showGamePicker = true },
                isSelected = selectedGame != null
            )
            
            // Ticket Selection Card  
            SelectionCard(
                title = "Select Bingo Ticket",
                iconPainter = androidx.compose.ui.res.painterResource(id = R.drawable.bingo_ticket),
                selectedText = selectedTicket?.name ?: if (selectedGame != null) "Choose your ticket" else "Select a game first",
                onClick = { if (selectedGame != null) showTicketPicker = true },
                isSelected = selectedTicket != null,
                enabled = selectedGame != null
            )
            
            // Challenge Details Card
            ChallengeDetailsCard(
                title = challengeTitle,
                onTitleChange = { challengeTitle = it },
                description = challengeDescription,
                onDescriptionChange = { challengeDescription = it },
                maxMembers = maxMembers,
                onMaxMembersChange = { maxMembers = it }
            )
            
            // Friends Selection Card
            FriendsSelectionCard(
                selectedFriends = selectedFriends,
                isLoading = uiState.isLoadingFriends,
                onClick = { showFriendsPicker = true }
            )
            
            // Create Button (Alternative to top bar)
            if (!canCreateChallenge) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "Please complete all required fields:\n• Select a game\n• Select a bingo ticket\n• Enter challenge title\n• Set max members (≥2)",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
    
    // Dialogs
    if (showGamePicker) {
        GamePickerDialog(
            games = availableGames,
            selectedGame = selectedGame,
            onGameSelected = { game ->
                selectedGame = game
                selectedTicket = null // Reset ticket when game changes
                showGamePicker = false
            },
            onDismiss = { showGamePicker = false }
        )
    }
    
    if (showTicketPicker) {
        TicketPickerDialog(
            tickets = gameTickets,
            selectedTicket = selectedTicket,
            onTicketSelected = { ticket ->
                selectedTicket = ticket
                showTicketPicker = false
            },
            onDismiss = { showTicketPicker = false }
        )
    }
    
    if (showFriendsPicker) {
        FriendsPickerDialog(
            friends = availableFriends,
            selectedFriends = selectedFriends,
            onFriendsSelected = { friends ->
                selectedFriends = friends
                showFriendsPicker = false
            },
            onDismiss = { showFriendsPicker = false }
        )
    }
}

@Composable
private fun SelectionCard(
    title: String,
    iconPainter: Painter,
    selectedText: String,
    onClick: () -> Unit,
    isSelected: Boolean,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                painter = iconPainter,
                contentDescription = stringResource(R.string.challenges),
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = selectedText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = if (isSelected) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = if (isSelected) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ChallengeDetailsCard(
    title: String,
    onTitleChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    maxMembers: String,
    onMaxMembersChange: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Challenge Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text("Challenge Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text("Description (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                placeholder = { Text("Add a description for your challenge...") }
            )

            OutlinedTextField(
                value = maxMembers,
                onValueChange = onMaxMembersChange,
                label = { Text("Max Members") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = { Text("Minimum 2 members") }
            )
        }
    }
}

@Composable
private fun FriendsSelectionCard(
    selectedFriends: Set<Friend>,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLoading) { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Invite Friends",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isLoading) {
                    Text(
                        text = "Loading friends...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = if (selectedFriends.isEmpty()) 
                            "No friends selected" 
                        else 
                            "${selectedFriends.size} friend${if (selectedFriends.size != 1) "s" else ""} selected",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (selectedFriends.isNotEmpty()) {
                        Text(
                            text = selectedFriends.take(3).joinToString(", ") { it.name } +
                                   if (selectedFriends.size > 3) " and ${selectedFriends.size - 3} more" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GamePickerDialog(
    games: List<Game>?,
    selectedGame: Game?,
    onGameSelected: (Game) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Select Game",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                if (games.isNullOrEmpty()) {
                    Text(
                        text = "No games available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(games.orEmpty()) { game ->
                        GameItem(
                            game = game,
                            isSelected = game.id == selectedGame?.id,
                            onClick = { onGameSelected(game) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GameItem(
    game: Game,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
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
                logoSize = 36.dp,
                showAbbrevs = true,
                abbrevFontSize = 11.sp,
                abbrevColor = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatDateTime(game.startTimeUTC),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun TicketPickerDialog(
    tickets: List<BingoTicket>?,
    selectedTicket: BingoTicket?,
    onTicketSelected: (BingoTicket) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Select Bingo Ticket",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                if (tickets.orEmpty().isEmpty()) {
                    Text(
                        text = "No tickets available for this game",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val sorted = tickets.orEmpty().sortedByDescending { it.score?.total ?: 0 }
                        items(sorted) { ticket ->
                            TicketItem(
                                ticket = ticket,
                                isSelected = ticket._id == selectedTicket?._id,
                                onClick = { onTicketSelected(ticket) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TicketItem(
    ticket: BingoTicket,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Text(
            text = ticket.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
private fun FriendsPickerDialog(
    friends: List<Friend>,
    selectedFriends: Set<Friend>,
    onFriendsSelected: (Set<Friend>) -> Unit,
    onDismiss: () -> Unit
) {
    var tempSelectedFriends by remember { mutableStateOf(selectedFriends) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Friends",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = { 
                            onFriendsSelected(tempSelectedFriends)
                            onDismiss()
                        }
                    ) {
                        Text("Done")
                    }
                }
                
                LazyColumn(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(friends) { friend ->
                        FriendItem(
                            friend = friend,
                            isSelected = friend in tempSelectedFriends,
                            onToggle = { 
                                tempSelectedFriends = if (friend in tempSelectedFriends) {
                                    tempSelectedFriends - friend
                                } else {
                                    tempSelectedFriends + friend
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendItem(
    friend: Friend,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = friend.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}



