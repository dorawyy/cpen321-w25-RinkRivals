package com.cpen321.usermanagement.data.local.preferences

import com.cpen321.usermanagement.data.remote.dto.*
import kotlinx.coroutines.flow.StateFlow

interface NhlDataManager {
    val uiState: StateFlow<NhlDataState>

    suspend fun loadSchedule(): Result<List<GameDay>>
    fun getUpcomingGames(): List<Game>
    fun getGamesForChallenges(): List<Game>
    fun getGamesForTickets(): List<Game>
    suspend fun getEventsForGame(gameId: Long, count: Int = 40): List<EventCondition>
    fun getGameById(gameId: Long): Game?
    fun clearError()
    suspend fun refreshSchedule(): Result<List<GameDay>>
    suspend fun getBoxscore(gameId: Long): Result<Boxscore>
    suspend fun isFulfilled(event: EventCondition, boxscore: Boxscore): Boolean
    fun formatEventLabel(event: EventCondition): String
}
