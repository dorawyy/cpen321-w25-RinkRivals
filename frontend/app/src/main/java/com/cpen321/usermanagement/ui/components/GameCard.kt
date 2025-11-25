package com.cpen321.usermanagement.ui.components

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.cpen321.usermanagement.R
import com.cpen321.usermanagement.data.local.preferences.NhlDataManager
import com.cpen321.usermanagement.data.remote.dto.Boxscore
import com.cpen321.usermanagement.data.remote.dto.Game
import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.*

/**
 * Reusable Game card that displays matchup, score (from boxscore) and some quick stats.
 */
@Composable
fun GameCard(
    game: Game,
    nhlDataManager: NhlDataManager,
    modifier: Modifier = Modifier
) {
    // Fetch boxscore for this game (may be cached by NhlDataManager implementation)
    val boxscoreResult by produceState<Result<Boxscore>?>(initialValue = null, key1 = game.id) {
        value = nhlDataManager.getBoxscore(game.id)
    }
    val boxscore = boxscoreResult?.getOrNull()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Game state badge
            SmallGameStateBadge(gameState = game.gameState)

            Spacer(modifier = Modifier.height(12.dp))

            // Team matchup and scores
            TeamMatchup(
                awayTeamLogoUrl = game.awayTeam.logo,
                awayTeamAbbrev = game.awayTeam.abbrev,
                homeTeamLogoUrl = game.homeTeam.logo,
                homeTeamAbbrev = game.homeTeam.abbrev,
                modifier = Modifier.fillMaxWidth(),
                logoSize = 48.dp,
                showAbbrevs = true,
                abbrevFontSize = 14.sp,
                abbrevColor = MaterialTheme.colorScheme.onSurface,
                vsText = "@"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Score row (shows when boxscore available and game is not upcoming

            if (boxscore != null && game.gameState != "FUT" && game.gameState != "PRE") {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Text(
                        text = "${boxscore.awayTeam.score} - ${boxscore.homeTeam.score}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Quick stats (shots on goal)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Text(text = stringResource(R.string.sog_format, boxscore.awayTeam.sog, boxscore.homeTeam.sog), style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Game time and venue
            Text(
                text = formatGameTime(game.startTimeUTC),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Text(
                text = game.venue.default,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SmallGameStateBadge(
    gameState: String,
    modifier: Modifier = Modifier
) {
    // 1. Read all composable theme colors OUTSIDE the remember block.
    val errorContainerColor = MaterialTheme.colorScheme.errorContainer
    val onErrorContainerColor = MaterialTheme.colorScheme.onErrorContainer
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainerColor = MaterialTheme.colorScheme.onPrimaryContainer
    val secondaryContainerColor = MaterialTheme.colorScheme.secondaryContainer
    val onSecondaryContainerColor = MaterialTheme.colorScheme.onSecondaryContainer
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    // 2. Perform the non-composable `when` logic inside remember,
    //    using the local variables you just created.
    val (backgroundColor, textColor, displayText) = remember(gameState) {
        when (gameState.uppercase()) {
            "LIVE" -> Triple(
                errorContainerColor,
                onErrorContainerColor,
                "Live"
            )
            "CRIT" -> Triple(
                Color(0xFFFFA000), // Orange
                Color.White,       // Using white for better contrast on orange
                "Critical"
            )
            "PRE" -> Triple(
                Color(0xFFC8E6C9), // Light Green
                Color(0xFF1B5E20), // Dark Green
                "Pre-Game"
            )
            "FUT" -> Triple(
                primaryContainerColor, // purple
                onPrimaryContainerColor,
                "Upcoming"
            )
            "FINAL", "OFF" -> Triple(
                secondaryContainerColor,
                onSecondaryContainerColor,
                "Final"
            )
            else -> Triple(
                surfaceColor,
                onSurfaceVariantColor,
                gameState
            )
        }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Text(
            text = displayText,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

private fun formatGameTime(startTimeUTC: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = inputFormat.parse(startTimeUTC)

        val outputFormat = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())
        date?.let { outputFormat.format(it) } ?: startTimeUTC
    } catch (e: Exception) {
        Log.e("GameCard", "Error formatting game time: $e")
        startTimeUTC
    }
}
