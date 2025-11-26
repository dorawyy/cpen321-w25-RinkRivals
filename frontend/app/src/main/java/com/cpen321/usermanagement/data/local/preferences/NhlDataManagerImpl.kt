package com.cpen321.usermanagement.data.local.preferences

import android.util.Log
import com.cpen321.usermanagement.data.remote.dto.Boxscore
import com.cpen321.usermanagement.data.remote.dto.Game
import com.cpen321.usermanagement.data.remote.dto.GameDay
import com.cpen321.usermanagement.data.remote.dto.GoalieStats
import com.cpen321.usermanagement.data.remote.dto.PlayerStats
import com.cpen321.usermanagement.data.repository.NHLRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * UI state for NHL data management
 */
data class NhlDataState(
    val isLoading: Boolean = false,
    val gameSchedule: List<GameDay>? = null,
    val errorMessage: String? = null
)

enum class EventCategory { FORWARD, DEFENSE, GOALIE, TEAM, PENALTY }
enum class ComparisonType { GREATER_THAN, LESS_THAN }

data class EventCondition(
    val id: String,
    val category: EventCategory,
    val subject: String,
    val comparison: ComparisonType,
    val threshold: Int,
    val playerId: Long? = null,
    val playerName: String? = null,
    val teamAbbrev: String? = null,
)

/**
 * Shared manager for NHL schedule data across the app
 * Maintains a single source of truth for game schedule
 */
@Singleton
class NhlDataManagerImpl @Inject constructor(
    private val nhlRepository: NHLRepository
) : NhlDataManager {
    companion object {
        private const val TAG = "NhlDataManager"
    }

    private val _uiState = MutableStateFlow(NhlDataState())
    override val uiState: StateFlow<NhlDataState> = _uiState.asStateFlow()

    /**
     * Load current NHL schedule
     */
    override suspend fun loadSchedule(): Result<List<GameDay>> {
        return try {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val result = nhlRepository.getCurrentSchedule()

            if (result.isSuccess) {
                val schedule = result.getOrNull() ?: emptyList()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    gameSchedule = schedule,
                    errorMessage = null
                )
                Log.d(TAG, "Schedule loaded successfully: ${schedule.size} game weeks")
            } else {
                val errorMessage = result.exceptionOrNull()?.message ?: "Failed to load schedule"
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = errorMessage
                )
                Log.e(TAG, "Failed to load schedule: $errorMessage")
            }

            result
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = e.message
            )
            Log.e(TAG, "Error loading schedule", e)
            Result.failure(e)
        }
    }

    /**
     * Get all upcoming games (future games only)
     */
    override fun getUpcomingGames(): List<Game> {
        return _uiState.value.gameSchedule?.flatMap { gameWeek ->
            gameWeek.games.filter { game ->
                game.gameState == "FUT" || game.gameState == "PRE" // Pre-game

            }
        } ?: emptyList()
    }

    /**
     * Get games for challenge creation (upcoming games only)
     */
    override fun getGamesForChallenges(): List<Game> {
        return getUpcomingGames().take(20) // Limit to next 20 games
    }

    /**
     * Get games for ticket creation
     */
    override fun getGamesForTickets(): List<Game> {
        return getUpcomingGames().take(20)
    }

    override suspend fun getEventsForGame(gameId: Long, count: Int): List<EventCondition> = coroutineScope {
        val game = getGameById(gameId) ?: return@coroutineScope emptyList()
        val random = Random(gameId)

        // --- QUOTAS ---
        val forwardQuota = (count * 0.45).toInt()
        val defenseQuota = (count * 0.45).toInt()
        val goalieTeamQuota = count - forwardQuota - defenseQuota

        val categoryQuotas = mutableMapOf(
            EventCategory.FORWARD to forwardQuota,
            EventCategory.DEFENSE to defenseQuota,
            EventCategory.TEAM to goalieTeamQuota / 2,
            EventCategory.GOALIE to goalieTeamQuota - (goalieTeamQuota / 2)
        )

        val teamQuotas = mutableMapOf(
            game.homeTeam.abbrev to count / 2,
            game.awayTeam.abbrev to count - (count / 2)
        )

        // --- LOAD ROSTERS ---
        val homeRosterDeferred = async { nhlRepository.getTeamRoster(game.homeTeam.abbrev).getOrNull() }
        val awayRosterDeferred = async { nhlRepository.getTeamRoster(game.awayTeam.abbrev).getOrNull() }

        val homeRoster = homeRosterDeferred.await()
        val awayRoster = awayRosterDeferred.await()

        val homePlayers = mapOf(
            EventCategory.FORWARD to (homeRoster?.forwards ?: emptyList()),
            EventCategory.DEFENSE to (homeRoster?.defensemen ?: emptyList()),
            EventCategory.GOALIE to (homeRoster?.goalies ?: emptyList())
        )

        val awayPlayers = mapOf(
            EventCategory.FORWARD to (awayRoster?.forwards ?: emptyList()),
            EventCategory.DEFENSE to (awayRoster?.defensemen ?: emptyList()),
            EventCategory.GOALIE to (awayRoster?.goalies ?: emptyList())
        )

        // --- SUBJECT POOLS ---
        val subjects = mapOf(
            EventCategory.FORWARD to listOf("goals", "assists", "hits", "sog", "blockedShots", "toi"),
            EventCategory.DEFENSE to listOf("goals", "assists", "hits", "sog", "blockedShots", "toi"),
            EventCategory.GOALIE to listOf("saves"),
            EventCategory.TEAM to listOf("goals", "sog", "penaltyMinutes")
        )

        val events = mutableListOf<EventCondition>()
        val generatedEventSignatures = mutableSetOf<String>()

        fun tryGenerateEvent(team: String, category: EventCategory): EventCondition? {
            val comparison = ComparisonType.GREATER_THAN

            return when (category) {
                EventCategory.FORWARD, EventCategory.DEFENSE, EventCategory.GOALIE -> {
                    val pool = if (team == game.homeTeam.abbrev) homePlayers[category] else awayPlayers[category]
                    if (pool.isNullOrEmpty()) return null

                    val player = pool.random(random)
                    val subject = subjects[category]!!.random(random)
                    val threshold = randomThresholdFor(subject, category, random)
                    val signature = "${category}_${team}_${player.id}_$subject"
                    // Prevent duplicate events
                    if (generatedEventSignatures.contains(signature)) return null

                    generatedEventSignatures.add(signature)
                    EventCondition(
                        id = "${category}_${team}_${player.id}_${events.size}",
                        category = category,
                        subject = subject,
                        comparison = comparison,
                        threshold = threshold,
                        playerId = player.id,
                        playerName = player.fullName,
                        teamAbbrev = team
                    )
                }

                EventCategory.TEAM, EventCategory.PENALTY -> {
                    val subject = subjects[EventCategory.TEAM]!!.random(random)
                    val threshold = randomThresholdFor(subject, EventCategory.TEAM, random)
                    val signature = "${category}_${team}_$subject"

                    if (generatedEventSignatures.contains(signature)) return null

                    generatedEventSignatures.add(signature)
                    EventCondition(
                        id = "${category}_${team}_${events.size}",
                        category = category,
                        subject = subject,
                        comparison = comparison,
                        threshold = threshold,
                        playerId = null,
                        playerName = null,
                        teamAbbrev = team
                    )
                }
            }
        }

        // --- MAIN GENERATION LOOP ---
        var attempts = 0
        while (events.size < count && attempts < count * 5) { // Safety break for weird conditions
            val availableCategories = categoryQuotas.filter { it.value > 0 }.keys.toList()
            val availableTeams = teamQuotas.filter { it.value > 0 }.keys.toList()

            if (availableCategories.isEmpty() || availableTeams.isEmpty()) break

            val category = availableCategories.random(random)
            val team = availableTeams.random(random)

            val event = tryGenerateEvent(team, category)

            if (event != null) {
                events += event
                categoryQuotas[category] = categoryQuotas[category]!! - 1
                teamQuotas[team] = teamQuotas[team]!! - 1
            }
            attempts++
        }

        return@coroutineScope events
    }

    /**
     * Get a specific game by ID
     */
    override fun getGameById(gameId: Long): Game? {
        return _uiState.value.gameSchedule?.flatMap { it.games }?.find { it.id == gameId }
    }

    /**
     * Clear error state
     */
    override fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * Refresh the schedule data
     */
    override suspend fun refreshSchedule(): Result<List<GameDay>> {
        return loadSchedule()
    }

    /**
     * Get boxscore for a specific game
     */
    override suspend fun getBoxscore(gameId: Long): Result<Boxscore> {
        return nhlRepository.getBoxscore(gameId)
    }

    /**
     * Return true if the given event condition is satisfied by the boxscore.
     * Handles player/goalie/team categories and subjects.
     */
    override suspend fun isFulfilled(event: EventCondition, boxscore: Boxscore): Boolean {
        // Get the stat value based on the event category and subject
        val value = getValueForField(boxscore, event)


        // Log debug info to help verify what's happening
        Log.d(
            "BingoCheck",
            "Checking event: ${event.subject}, " +
                    "player=${event.playerName ?: "N/A"}, " +
                    "team=${event.teamAbbrev ?: "N/A"}, " +
                    "value=$value, " +
                    "threshold=${event.threshold}, " +
                    "comparison=${event.comparison}"
        )

        // If value is missing, it's not fulfilled
        if (value == null) {
            Log.d("BingoCheck", "❌ Value not found for ${event.subject}")
            return false
        }

        // Compare the stat against the threshold
        val result = compareStat(value, event.threshold, event.comparison)

        Log.d("BingoCheck", "✅ Comparison result for ${event.subject} = $result")

        return result
    }

    override fun formatEventLabel(event: EventCondition): String {
        val subject = when {
            !event.playerName.isNullOrEmpty() -> event.playerName
            !event.teamAbbrev.isNullOrEmpty() && event.category in listOf(EventCategory.TEAM, EventCategory.PENALTY) -> event.teamAbbrev
            else -> "Player"
        }

        val statName = when (event.subject) {
            "goals" -> if (event.category == EventCategory.TEAM) {
                listOf("scores", "total").random() + " goals"
            } else {
                listOf("scores", "goals", "scores goals").random()
            }
            "assists" -> listOf("assists on a goal", "gets assists", "assists").random()
            "hits" -> listOf("delivers hits", "body checks", "hits").random()
            "sog" -> if (event.category == EventCategory.TEAM) {
                "total shots"
            } else {
                listOf("shots on goal", "shots").random()
            }
            "blockedShots" -> listOf("blocks shots", "shots blocked").random()
            "saves" -> listOf("makes saves", "saves", "shots saved").random()
            "penaltyMinutes" -> listOf("takes penalty minutes", "total penalty minutes", "penalty minutes").random()
            else -> event.subject ?: "unknown stat"
        }

        val comparison = when (event.comparison) {
            ComparisonType.GREATER_THAN -> "${event.threshold}+"
            ComparisonType.LESS_THAN -> "< ${event.threshold}"
        }

        return "$subject $statName ($comparison)"
    }

    /**
     * Get an integer value for the event's subject from the boxscore.
     * Accepts subject values like "goals", "sog", "saves", "penaltyMinutes", "toi".
     * Returns null if the requested stat isn't available for the given event.
     */
    private fun getValueForField(boxscore: Boxscore, eventCondition: EventCondition): Int? {
        val subject = eventCondition.subject.removePrefix("player.").removePrefix("team.").removePrefix("goalie.")

        // TEAM-level queries (sums, or team fields)
        if (eventCondition.category == EventCategory.TEAM || eventCondition.category == EventCategory.PENALTY) {
            val teamAbbrev = eventCondition.teamAbbrev ?: return null
            val isHome = boxscore.homeTeam.abbrev == teamAbbrev
            val teamInfo = if (isHome) boxscore.homeTeam else boxscore.awayTeam

            return when (subject) {
                "goals" -> teamInfo.score
                "sog" -> teamInfo.sog
                // No direct team PIM field: sum player pims
                "penaltyMinutes", "pim", "penalties" -> {
                    val stats = boxscore.playerByGameStats ?: return null
                    val teamPlayers = if (isHome) stats.homeTeam else stats.awayTeam
                    val allPlayers = teamPlayers.forwards + teamPlayers.defense
                    allPlayers.sumOf { it.pim ?: 0 }
                }
                else -> null
            }
        }

        // PLAYER-level (skaters) OR GOALIE-level: prefer lookup by playerId if present
        val playerId = eventCondition.playerId
        if (playerId != null) {
            // Try skaters first
            val skater = getSkaterById(boxscore, playerId)
            if (skater != null) {
                return getSkaterStatBySubject(skater, subject)
            }

            // Try goalie table if not found among skaters
            val goalie = getGoalieById(boxscore, playerId)
            if (goalie != null) {
                return getGoalieStatBySubject(goalie, subject)
            }

            return null
        }

        // If no playerId, try by playerName (case-insensitive)
        val playerName = eventCondition.playerName
        if (!playerName.isNullOrBlank()) {
            val skater = findPlayerByName(boxscore, playerName)
            if (skater != null) {
                return getSkaterStatBySubject(skater, subject)
            }
            val goalie = findGoalieByName(boxscore, playerName)
            if (goalie != null) {
                return getGoalieStatBySubject(goalie, subject)
            }
        }

        // Nothing found
        return null
    }

    /**
     * Get skater (non-goalie) stat by subject.
     */
    private fun getSkaterStatBySubject(player: PlayerStats, subject: String): Int? {
        return when (subject) {
            "goals" -> player.goals
            "assists" -> player.assists
            "points" -> player.points
            "hits" -> player.hits
            "sog" -> player.sog
            "blockedShots" -> player.blockedShots
            "pim" -> player.pim ?: 0
            "toi" -> parseToiToMinutes(player.toi)
            else -> null
        }
    }

    /**
     * Get goalie stat by subject.
     * Note: goalie stats naming in boxscore used "saves", "goalsAgainst", "savePctg" etc.
     */
    private fun getGoalieStatBySubject(goalie: GoalieStats, subject: String): Int? {
        return when (subject) {
            "saves" -> goalie.saves
            "goalsAgainst", "ga" -> goalie.goalsAgainst
            // Convert TOI string to minutes for goalie too
            "toi" -> parseToiToMinutes(goalie.toi)
            // If you want to support save percentage comparisons you'll need to adapt return type (Double)
            else -> null
        }
    }

    /**
     * Helper: find skater by numeric player id in both teams
     */
    private fun getSkaterById(boxscore: Boxscore, id: Long): PlayerStats? {
        val stats = boxscore.playerByGameStats ?: return null
        return (stats.homeTeam.forwards + stats.homeTeam.defense +
                stats.awayTeam.forwards + stats.awayTeam.defense)
            .firstOrNull { it.playerId == id }
    }

    /**
     * Helper: find goalie by numeric player id in both teams
     */
    private fun getGoalieById(boxscore: Boxscore, id: Long): GoalieStats? {
        val stats = boxscore.playerByGameStats ?: return null
        return (stats.homeTeam.goalies + stats.awayTeam.goalies)
            .firstOrNull { it.playerId == id }
    }

    /**
     * Find a skater by display name (case-insensitive). Uses PlayerStats.name.default
     */
    private fun findPlayerByName(boxscore: Boxscore, name: String): PlayerStats? {
        val stats = boxscore.playerByGameStats ?: return null
        val allPlayers = stats.awayTeam.forwards +
                stats.awayTeam.defense +
                stats.homeTeam.forwards +
                stats.homeTeam.defense

        return allPlayers.firstOrNull { it.name.default.equals(name, ignoreCase = true) }
    }

    /**
     * Find a goalie by display name (case-insensitive). Uses GoalieStats.name.default
     */
    private fun findGoalieByName(boxscore: Boxscore, name: String): GoalieStats? {
        val stats = boxscore.playerByGameStats ?: return null
        val allGoalies = stats.awayTeam.goalies +
                stats.homeTeam.goalies

        return allGoalies.firstOrNull { it.name.default.equals(name, ignoreCase = true) }
    }

    /**
     * Parse TOI "MM:SS" (or "H:MM:SS") into minutes (rounded down).
     * Returns 0 on parse failure.
     */
    private fun parseToiToMinutes(toi: String?): Int {
        if (toi.isNullOrBlank()) return 0
        // Some goalies may have "HH:MM:SS" while skaters usually "MM:SS"
        val parts = toi.split(":").mapNotNull { it.toIntOrNull() }
        if (parts.isEmpty()) return 0
        return when (parts.size) {
            2 -> { // MM:SS
                val minutes = parts[0]
                val seconds = parts[1]
                minutes + (seconds / 60)
            }
            3 -> { // H:MM:SS
                val hours = parts[0]
                val minutes = parts[1]
                val seconds = parts[2]
                hours * 60 + minutes + (seconds / 60)
            }
            else -> 0
        }
    }

    /**
     * Compare integer stat with threshold using ComparisonType.
     */
    private fun compareStat(value: Int, threshold: Int, comparison: ComparisonType): Boolean {
        return when (comparison) {
            ComparisonType.GREATER_THAN -> value >= threshold
            ComparisonType.LESS_THAN -> value < threshold
        }
    }

    /**
     * Produce a random, reasonable threshold for a given subject.
     * Tuned to typical hockey stat ranges.
     */
    private fun randomThresholdFor(
        subject: String,
        category: EventCategory,
        random: Random
    ): Int {

        val cleanSubject = subject.removePrefix("player.")
            .removePrefix("team.")
            .removePrefix("goalie.")

        return when (category) {

            EventCategory.FORWARD -> when (cleanSubject) {
                "goals" -> (1..2).random(random)
                "assists" -> (1..2).random(random)
                "hits" -> (2..4).random(random)
                "sog" -> (3..6).random(random)
                "blockedShots" -> (1..2).random(random)
                "toi" -> (18..23).random(random)
                else -> (1..3).random(random)
            }

            EventCategory.DEFENSE -> when (cleanSubject) {
                "goals" -> 1        // rare
                "assists" -> (1..2).random(random)
                "hits" -> (2..4).random(random)
                "sog" -> (1..3).random(random)
                "blockedShots" -> (2..6).random(random)
                "toi" -> (18..23).random(random)
                else -> (1..3).random(random)
            }

            EventCategory.GOALIE -> when (cleanSubject) {
                "saves" -> (26..33).random(random)
                else -> (1..3).random(random)
            }

            EventCategory.TEAM -> when (cleanSubject) {
                "goals" -> (2..5).random(random)
                "sog" -> (27..36).random(random)
                "penaltyMinutes", "penalties" -> (4..12).random(random)
                else -> (1..3).random(random)
            }

            else -> (88..99).random(random)
        }
    }
}
