package com.cpen321.usermanagement.ui.screens

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cpen321.usermanagement.R
import com.cpen321.usermanagement.data.local.preferences.SocketEventListener
import com.cpen321.usermanagement.data.remote.dto.Challenge
import com.cpen321.usermanagement.data.remote.dto.ChallengeStatus
import com.cpen321.usermanagement.ui.viewmodels.ChallengesUiState
import com.cpen321.usermanagement.ui.viewmodels.ChallengesViewModel

private data class ChallengesScreenCallbacks(
    val onBackClick: () -> Unit,
    val onAddChallengeClick: () -> Unit,
    val onChallengeClick: (challengeId: String) -> Unit
)

data class ChallengesScreenActions(
    val onBackClick: () -> Unit,
    val onAddChallengeClick: () -> Unit,
    val onChallengeClick: (challengeId: String) -> Unit
)

private const val TAG = "ChallengesScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengesScreen(
    challengesViewModel: ChallengesViewModel,
    socketEventListener: SocketEventListener,
    actions: ChallengesScreenActions
) {
    val uiState by challengesViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        challengesViewModel.clearSuccessMessage()
        challengesViewModel.clearError()
        challengesViewModel.loadProfile()
        challengesViewModel.loadChallenges()
    }

    val socketEventListeners = remember(socketEventListener) {
        listOf(
            socketEventListener.challengeInvitations,
            socketEventListener.challengeUpdated,
            socketEventListener.challengeCreated,
            socketEventListener.challengeDeleted,
            socketEventListener.userJoinedChallenge,
            socketEventListener.userLeftChallenge,
            socketEventListener.invitationDeclined
        )
    }

    socketEventListeners.forEach { eventFlow ->
        LaunchedEffect(eventFlow) {
            eventFlow.collect { event ->
                Log.d(TAG, "Socket event received: ${event.message}")
                challengesViewModel.loadChallenges()
            }
        }
    }

    ChallengesContent(
        uiState = uiState,
        callbacks = ChallengesScreenCallbacks(
            onBackClick = actions.onBackClick,
            onAddChallengeClick = actions.onAddChallengeClick,
            onChallengeClick = actions.onChallengeClick
        )
    )
}

@Composable
private fun ChallengesContent(
    modifier: Modifier = Modifier,
    uiState: ChallengesUiState,
    callbacks: ChallengesScreenCallbacks
) {
    Scaffold(
        modifier = modifier,
        topBar = { ChallengesTopBar() },
        content = {
            ChallengesBody(
                paddingValues = it,
                uiState = uiState,
                callbacks = callbacks
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChallengesTopBar(
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = { Text(stringResource(R.string.challenges)) },
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun ChallengesBody(
    paddingValues: PaddingValues,
    uiState: ChallengesUiState,
    callbacks: ChallengesScreenCallbacks
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        val challenges = uiState.allChallenges

        when {
            uiState.isLoadingChallenges -> CircularProgressIndicator()
            uiState.errorMessage != null -> Text(
                text = uiState.errorMessage,
                color = Color.Red,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
            challenges != null -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    challenges.forEach { (challengeType, challengeList) ->
                        val filteredList = challengeList.filter { it.status != ChallengeStatus.CANCELLED }
                        if (filteredList.isNotEmpty()) {
                            item {
                                CollapsibleChallengeSection(challengeType, filteredList, callbacks)
                            }
                        }
                    }
                }
            }
            else -> Text(stringResource(R.string.welcome_to_challenges))
        }

        AddChallengeButton(
            onClick = callbacks.onAddChallengeClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .width(200.dp)
                .height(60.dp)
        )
    }
}

@Composable
private fun CollapsibleChallengeSection(
    challengeType: String,
    challengeList: List<Challenge>,
    callbacks: ChallengesScreenCallbacks
) {
    var isExpanded by remember { mutableStateOf(challengeType.lowercase() != "finished") }

    val statusColor = when (challengeType.uppercase()) {
        "LIVE" -> MaterialTheme.colorScheme.errorContainer
        "ACTIVE" -> Color(0xFFC8E6C9) // Light Green
        "PENDING" -> MaterialTheme.colorScheme.primaryContainer
        "FINISHED" -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val onStatusColor = when (challengeType.uppercase()) {
        "LIVE" -> MaterialTheme.colorScheme.onErrorContainer
        "ACTIVE" -> Color(0xFF1B5E20) // Dark Green
        "PENDING" -> MaterialTheme.colorScheme.onPrimaryContainer
        "FINISHED" -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

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
                    text = challengeType.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = onStatusColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column {
                if (challengeList.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_challenges_available, challengeType),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                } else {
                    challengeList.forEach { challenge ->
                        ChallengeItem(
                            challenge = challenge,
                            onClick = { callbacks.onChallengeClick(challenge.id) }
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun ChallengeItem(challenge: Challenge, onClick: () -> Unit = {}) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = challenge.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = challenge.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = stringResource(id = R.string.members),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (challenge.memberNames.isNotEmpty()) {
                        challenge.memberNames.forEach { memberName ->
                            Text(
                                text = "• $memberName",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(id = R.string.no_members_yet),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                modifier = Modifier.padding(start = 16.dp),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun AddChallengeButton(
    onClick: () -> Unit,
    modifier: Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
    ) {
        Text(stringResource(R.string.new_challenge))
    }
}
