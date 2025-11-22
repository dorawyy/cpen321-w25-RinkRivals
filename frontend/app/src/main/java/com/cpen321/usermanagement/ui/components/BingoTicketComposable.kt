package com.cpen321.usermanagement.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.cpen321.usermanagement.R
import com.cpen321.usermanagement.data.local.preferences.EventCondition
import com.cpen321.usermanagement.data.local.preferences.NhlDataManager
import com.cpen321.usermanagement.data.remote.dto.BingoTicket

/**
 * Represents the score of a bingo ticket.
 * This data class holds the breakdown of how a score is calculated.
 *
 * @param noCrossedOff Number of squares crossed off.
 * @param noRows Number of completed horizontal rows.
 * @param noColumns Number of completed vertical columns.
 * @param noCrosses Number of completed diagonal crosses.
 * @param total The total calculated score.
 */
data class BingoScore(
    val noCrossedOff: Int = 0,
    val noRows: Int = 0,
    val noColumns: Int = 0,
    val noCrosses: Int = 0,
    val total: Int = 0
)

/**
 * Computes the score for a bingo grid based on which squares are crossed off.
 * This is a client-side implementation that should mirror the backend scoring logic.
 *
 * - 1 point for each square crossed off.
 * - 3 points for each completed line (row, column, or diagonal).
 * - 10 bonus points for a full bingo (all 9 squares crossed off).
 *
 * @param crossedOff A list of booleans representing the 3x3 grid's state.
 * @return A [BingoScore] object with the detailed score breakdown.
 */
fun computeBingoScore(crossedOff: List<Boolean>): BingoScore {
    val arr = crossedOff.take(9) + List(maxOf(0, 9 - crossedOff.size)) { false }
    val noCrossedOff = arr.count { it }

    var noRows = 0
    var noColumns = 0
    var noCrosses = 0

    // Check for completed rows
    for (r in 0 until 3) {
        val start = r * 3
        if (arr[start] && arr[start + 1] && arr[start + 2]) noRows++
    }
    // Check for completed columns
    for (c in 0 until 3) {
        if (arr[c] && arr[c + 3] && arr[c + 6]) noColumns++
    }
    // Check for completed diagonals
    if (arr[0] && arr[4] && arr[8]) noCrosses++
    if (arr[2] && arr[4] && arr[6]) noCrosses++

    val perSquare = noCrossedOff * 1
    val perLine = (noRows + noColumns + noCrosses) * 3
    val bingoBonus = if (noCrossedOff == 9) 10 else 0
    val total = perSquare + perLine + bingoBonus

    return BingoScore(noCrossedOff, noRows, noColumns, noCrosses, total)
}

/**
 * A composable that displays a 3x3 bingo grid.
 * Each cell can display either a team logo or a text label for a game event.
 * The grid is responsive and fills the available width.
 *
 * @param events The list of [EventCondition]s that define the content of each grid cell.
 * @param crossedOff The list of booleans indicating whether each cell is crossed off.
 * @param nhlDataManager The data manager to format event labels and resolve team logos.
 * @param cellSize The target size for each cell. The actual size will adapt to the screen width.
 * @param onToggle A callback function invoked when a cell is clicked, allowing for interactive toggling. If null, the grid is read-only.
 */
@Composable
fun BingoGrid(
    events: List<EventCondition>,
    crossedOff: List<Boolean>,
    nhlDataManager: NhlDataManager,
    cellSize: Dp = 80.dp,
    onToggle: ((index: Int, newValue: Boolean) -> Unit)? = null
) {
    // The grid fills the available width, and each cell is a square that takes 1/3 of the width.
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in 0 until 3) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (col in 0 until 3) {
                    val index = row * 3 + col
                    val isCrossed = crossedOff.getOrNull(index) ?: false
                    val event = events.getOrNull(index)

                    val label = remember(event) { mutableStateOf("") }
                    LaunchedEffect(event) {
                        if (event != null) label.value = nhlDataManager.formatEventLabel(event)
                    }

                    val teamAbbrev = event?.teamAbbrev
                    val logoUrl = remember(teamAbbrev) {
                        teamAbbrev?.let { abbrev ->
                            nhlDataManager.uiState.value.gameSchedule
                                ?.flatMap { it.games }
                                ?.flatMap { listOf(it.homeTeam, it.awayTeam) }
                                ?.find { it.abbrev == abbrev }
                                ?.logo
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                            .alpha(0.79f)
                            .background(
                                if (isCrossed) Color.Green.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(8.dp)
                            )
                            // Make the cell clickable only if onToggle is provided
                            .let { m -> if (onToggle != null) m.clickable { onToggle(index, !isCrossed) } else m },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!logoUrl.isNullOrBlank()) {
                            TeamLogo(
                                logoUrl = logoUrl,
                                teamAbbrev = label.value,
                                size = (cellSize.value / 2).dp,
                                showAbbrev = true,
                                abbrevFontSize = 13.sp
                            )
                        } else {
                            Text(
                                text = label.value,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center, // Center the text within the cell
                                modifier = Modifier.padding(3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * A card composable that displays a summary of a bingo ticket.
 * It features a header with score, title, and delete button, and a body with the bingo grid.
 * Faint team logos are displayed as the card's background.
 *
 * @param ticket The [BingoTicket] data to display.
 * @param nhlDataManager The data manager for resolving logos and formatting grid labels.
 * @param onDelete An optional callback to handle the deletion of the ticket. If provided, a delete icon is shown.
 * @param onClick An optional callback for when the card is clicked.
 * @param modifier A [Modifier] for this composable.
 */
@Composable
fun BingoTicketCard(
    ticket: BingoTicket,
    nhlDataManager: NhlDataManager,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val score = ticket.score?.let { BingoScore(it.noCrossedOff, it.noRows, it.noColumns, it.noCrosses, it.total) }

    // Resolve team logos, falling back to the schedule data if the ticket's game object doesn't have them.
    val awayAbbrev = ticket.game.awayTeam.abbrev
    val homeAbbrev = ticket.game.homeTeam.abbrev

    val awayLogoUrl = remember(awayAbbrev) {
        ticket.game.awayTeam.logo.takeUnless { it.isNullOrBlank() }
            ?: nhlDataManager.uiState.value.gameSchedule
                ?.flatMap { it.games }
                ?.flatMap { listOf(it.homeTeam, it.awayTeam) }
                ?.find { it.abbrev == awayAbbrev }
                ?.logo
            ?: ""
    }

    val homeLogoUrl = remember(homeAbbrev) {
        ticket.game.homeTeam.logo.takeUnless { it.isNullOrBlank() }
            ?: nhlDataManager.uiState.value.gameSchedule
                ?.flatMap { it.games }
                ?.flatMap { listOf(it.homeTeam, it.awayTeam) }
                ?.find { it.abbrev == homeAbbrev }
                ?.logo
            ?: ""
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable { onClick() } else it },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Faint background logos for atmosphere
            if (awayLogoUrl.isNotBlank()) {
                TeamLogo(
                    logoUrl = awayLogoUrl,
                    teamAbbrev = awayAbbrev,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = (-40).dp)
                        .size(320.dp)
                        .alpha(0.35f),
                    size = 320.dp,
                    showAbbrev = false
                )
            }

            if (homeLogoUrl.isNotBlank()) {
                TeamLogo(
                    logoUrl = homeLogoUrl,
                    teamAbbrev = homeAbbrev,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = 40.dp)
                        .size(320.dp)
                        .alpha(0.35f),
                    size = 320.dp,
                    showAbbrev = false
                )
            }

            Column(modifier = Modifier.padding(12.dp)) {
                // Header section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Left: Score
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${score?.total ?: stringResource(R.string.n_a)}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Middle: Title (truncated)
                    Text(
                        text = ticket.name,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Right: Delete button
                    if (onDelete != null) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete_ticket),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        // Add a spacer to maintain layout consistency when delete is not available
                        Spacer(modifier = Modifier.width(48.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Body section: The bingo grid
                BingoGrid(events = ticket.events, crossedOff = ticket.crossedOff, nhlDataManager = nhlDataManager)
            }
        }
    }
}

/**
 * A detailed, interactive view of a bingo ticket.
 * This composable allows the user to tap on grid cells to toggle their "crossed off" state
 * and see the score update in real-time. It's intended for a detail screen.
 *
 * @param ticket The [BingoTicket] to display and interact with.
 * @param nhlDataManager The data manager for resolving UI elements in the grid.
 */
@Composable
fun BingoTicketDetailInteractive(
    ticket: BingoTicket,
    nhlDataManager: NhlDataManager,
) {
    // State for the interactive crossed-off list
    var crossed by remember { mutableStateOf(ticket.crossedOff.toMutableList()) }
    // The score is recomposed whenever the 'crossed' state changes
    val score = computeBingoScore(crossed)

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
        Text(ticket.name, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        // Interactive Bingo Grid
        BingoGrid(
            events = ticket.events,
            crossedOff = crossed,
            nhlDataManager = nhlDataManager,
            cellSize = 100.dp
        ) { idx, newValue ->
            // Update the state when a cell is toggled
            crossed = crossed.toMutableList().also { it[idx] = newValue }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Live score breakdown section
        Text(stringResource(R.string.breakdown), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.fields_crossed, score.noCrossedOff))
        Text(stringResource(R.string.rows_columns_diagonals, score.noRows, score.noColumns, score.noCrosses))
        Text(stringResource(R.string.bonus_bingo, if (score.noCrossedOff == 9) 10 else 0))
        Text(stringResource(R.string.total, score.total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(12.dp))

        // Action buttons
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { crossed = ticket.crossedOff.toMutableList() },
                // The button is disabled if there are no changes to reset
                enabled = crossed != ticket.crossedOff
            ) {
                Text(stringResource(R.string.reset))
            }
        }
    }
}