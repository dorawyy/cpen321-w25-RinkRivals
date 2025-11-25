package com.cpen321.usermanagement.fakes

import com.cpen321.usermanagement.data.local.preferences.ComparisonType
import com.cpen321.usermanagement.data.local.preferences.EventCategory
import com.cpen321.usermanagement.data.local.preferences.EventCondition
import com.cpen321.usermanagement.data.local.preferences.NhlDataManager
import com.cpen321.usermanagement.data.local.preferences.NhlDataState
import com.cpen321.usermanagement.data.remote.dto.Boxscore
import com.cpen321.usermanagement.data.remote.dto.Game
import com.cpen321.usermanagement.data.remote.dto.GameDay
import com.cpen321.usermanagement.data.remote.dto.GoalieStats
import com.cpen321.usermanagement.data.remote.dto.Name
import com.cpen321.usermanagement.data.remote.dto.NameWrapper
import com.cpen321.usermanagement.data.remote.dto.PeriodDescriptor
import com.cpen321.usermanagement.data.remote.dto.PlayerByGameStats
import com.cpen321.usermanagement.data.remote.dto.PlayerStats
import com.cpen321.usermanagement.data.remote.dto.Team
import com.cpen321.usermanagement.data.remote.dto.TeamInfo
import com.cpen321.usermanagement.data.remote.dto.TeamPlayers
import com.cpen321.usermanagement.data.remote.dto.TvBroadcast
import com.cpen321.usermanagement.data.remote.dto.Venue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeNhlDataManager : NhlDataManager {

    override val uiState: StateFlow<NhlDataState> = MutableStateFlow(NhlDataState())
    // ---- Fake Game Data ----
    private val fakeGame = Game(
        id = 1L,
        season = 20242025,
        gameType = 2,
        venue = Venue(default = "Rogers Arena"),
        neutralSite = false,
        startTimeUTC = "2025-03-10T19:00:00Z",
        easternUTCOffset = "-05:00",
        venueUTCOffset = "-08:00",
        venueTimezone = "America/Vancouver",
        gameState = "FUT",
        gameScheduleState = "OK",
        tvBroadcasts = listOf(
            TvBroadcast(
                id = 1L,
                market = "National",
                countryCode = "CA",
                network = "Sportsnet",
                sequenceNumber = 1
            )
        ),
        awayTeam = Team(
            id = 15,
            commonName = Name(default = "Maple Leafs"),
            placeName = Name(default = "Toronto"),
            placeNameWithPreposition = Name(default = "Toronto"),
            abbrev = "TOR",
            logo = "",
            darkLogo = "",
            radioLink = "",
            odds = null
        ),
        homeTeam = Team(
            id = 10,
            commonName = Name(default = "Canucks"),
            placeName = Name(default = "Vancouver"),
            placeNameWithPreposition = Name(default = "Vancouver"),
            abbrev = "VAN",
            logo = "",
            darkLogo = "",
            radioLink = "",
            odds = null
        ),
        periodDescriptor = PeriodDescriptor(
            number = 0,
            periodType = "REG",
            maxRegulationPeriods = 3
        ),
        ticketsLink = null,
        ticketsLinkFr = null,
        gameCenterLink = null
    )

    private val fakeGameDay = GameDay(
        date = "2025-03-10",
        dayAbbrev = "Mon",
        numberOfGames = 1,
        datePromo = emptyList(),
        games = listOf(fakeGame)
    )

    private val fakeEvents = listOf(
        EventCondition(
            id = "E1",
            category = EventCategory.FORWARD,
            subject = "goals",
            comparison = ComparisonType.GREATER_THAN,
            threshold = 2,
            playerName = "J.T. Miller",
            teamAbbrev = "VAN"
        ),
        EventCondition(
            id = "E2",
            category = EventCategory.TEAM,
            subject = "goals",
            comparison = ComparisonType.GREATER_THAN,
            threshold = 3,
            teamAbbrev = "VAN"
        ),
        EventCondition(
            id = "E3",
            category = EventCategory.GOALIE,
            subject = "saves",
            comparison = ComparisonType.GREATER_THAN,
            threshold = 25,
            playerName = "Thatcher Demko",
            teamAbbrev = "VAN"
        )
    )

    private val fakeBoxscore = Boxscore(
        id = 1L,
        season = 20242025,
        gameType = 2,
        gameDate = "2025-03-10",
        gameState = "FUT",
        limitedScoring = false,
        periodDescriptor = PeriodDescriptor(
            number = 0,
            periodType = "REG",
            maxRegulationPeriods = 3
        ),
        awayTeam = TeamInfo(
            id = 15,
            abbrev = "TOR",
            score = 0,
            sog = 0,
            commonName = NameWrapper(default = "Maple Leafs"),
            placeName = NameWrapper(default = "Toronto"),
            logo = "",
            darkLogo = ""
        ),
        homeTeam = TeamInfo(
            id = 10,
            abbrev = "VAN",
            score = 0,
            sog = 0,
            commonName = NameWrapper(default = "Canucks"),
            placeName = NameWrapper(default = "Vancouver"),
            logo = "",
            darkLogo = ""
        ),
        playerByGameStats = PlayerByGameStats(
            awayTeam = TeamPlayers(
                forwards = emptyList<PlayerStats>(),
                defense = emptyList<PlayerStats>(),
                goalies = emptyList<GoalieStats>()
            ),
            homeTeam = TeamPlayers(
                forwards = emptyList<PlayerStats>(),
                defense = emptyList<PlayerStats>(),
                goalies = emptyList<GoalieStats>()
            )
        )
    )

    // ---- Interface Implementations ----
    override suspend fun loadSchedule(): Result<List<GameDay>> {
        return Result.success(listOf(fakeGameDay))
    }

    override fun getUpcomingGames(): List<Game> {
        return listOf(fakeGame)
    }

    override fun getGamesForChallenges(): List<Game> {
        return listOf(fakeGame)
    }

    override fun getGamesForTickets(): List<Game> {
        return listOf(fakeGame)
    }

    override suspend fun getEventsForGame(gameId: Long, count: Int): List<EventCondition> {
        return fakeEvents.take(count)
    }

    override fun getGameById(gameId: Long): Game? {
        return if (gameId == fakeGame.id) fakeGame else null
    }

    override fun clearError() {
        // No-op (no errors tracked in fake)
    }

    override suspend fun refreshSchedule(): Result<List<GameDay>> {
        return Result.success(listOf(fakeGameDay))
    }

    override suspend fun getBoxscore(gameId: Long): Result<Boxscore> {
        return Result.success(fakeBoxscore)
    }

    override suspend fun isFulfilled(event: EventCondition, boxscore: Boxscore): Boolean {
        return true
    }

    override fun formatEventLabel(event: EventCondition): String {
        val subjectName = when {
            !event.playerName.isNullOrEmpty() -> event.playerName
            !event.teamAbbrev.isNullOrEmpty() && event.category in listOf(EventCategory.TEAM, EventCategory.PENALTY) -> event.teamAbbrev
            else -> "Player"
        }

        val statName = when (event.subject) {
            "goals" -> if (event.category == EventCategory.TEAM) "scores total goals" else "scores goals"
            "assists" -> "assists on a goal"
            "hits" -> "delivers hits"
            "sog" -> if (event.category == EventCategory.TEAM) "total shots" else "shots on goal"
            "blockedShots" -> "blocks shots"
            "saves" -> "makes saves"
            "penaltyMinutes" -> "takes penalty minutes"
            else -> event.subject ?: "unknown stat"
        }

        val comparison = when (event.comparison) {
            ComparisonType.GREATER_THAN -> "${event.threshold}+"
            ComparisonType.LESS_THAN -> "< ${event.threshold}"
        }

        return "$subjectName $statName ($comparison)"
    }
}
