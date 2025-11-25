package com.cpen321.usermanagement.data.remote.dto

import com.cpen321.usermanagement.data.local.preferences.EventCondition

// Represents one bingo ticket
data class BingoTicket(
    val _id: String,
    val userId: String,
    val name: String,
    val game: Game,
    val events: List<EventCondition>,
    val crossedOff: List<Boolean> = List(9) { false },
    val score: BingoTicketScore? = null
)

data class BingoTicketScore(
    val noCrossedOff: Int = 0,
    val noRows: Int = 0,
    val noColumns: Int = 0,
    val noCrosses: Int = 0,
    val total: Int = 0
)

// UI state for the TicketsScreen and CreateBingoTicketScreen
data class TicketsUiState(
    val allTickets: List<BingoTicket> = emptyList(),
    val isLoadingTickets: Boolean = false,
    val isCreating: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,

    val isLoadingGames: Boolean = false,
    val availableGames: List<Game> = emptyList(),
    val expandedSections: Set<String> = setOf("LIVE") // Added this line
)
