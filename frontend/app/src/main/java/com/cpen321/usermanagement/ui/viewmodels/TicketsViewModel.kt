package com.cpen321.usermanagement.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cpen321.usermanagement.data.local.preferences.EventCondition
import com.cpen321.usermanagement.data.local.preferences.NhlDataManager
import com.cpen321.usermanagement.data.remote.dto.BingoTicket
import com.cpen321.usermanagement.data.remote.dto.Game
import com.cpen321.usermanagement.data.remote.dto.TicketsUiState
import com.cpen321.usermanagement.data.repository.ChallengesRepository
import com.cpen321.usermanagement.data.repository.TicketsRepository
import com.cpen321.usermanagement.ui.navigation.NavigationStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TicketsViewModel @Inject constructor(
    private val repository: TicketsRepository,
    private val challengesRepository: ChallengesRepository, // <-- Needed to check if ticket is used in a challenge
    //private val nhlDataManager: NhlDataManager,
    private val navigationStateManager: NavigationStateManager,
    val nhlDataManager: NhlDataManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(TicketsUiState())
    val uiState: StateFlow<TicketsUiState> = _uiState

    fun loadTickets(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingTickets = true)
            val result = repository.getTickets(userId)
            val tickets = result.getOrDefault(emptyList())

            // Immediately populate UI state with fetched tickets so subsequent
            // updateTicketFromBoxscore calls can find them in the UI state.
            _uiState.value = _uiState.value.copy(
                isLoadingTickets = false,
                allTickets = tickets
            )

            // For every ticket, asynchronously refresh its crossedOff state
            tickets.forEach { ticket ->
                updateTicketFromBoxscore(ticket._id)
            }

            // get the new updated tickets and append them to the UI state
            val updatedResult = repository.getTickets(userId)
            val updatedTickets = updatedResult.getOrDefault(emptyList())

            _uiState.value = _uiState.value.copy(
                isLoadingTickets = false,
                allTickets = updatedTickets
            )
        }
    }

    fun createTicket(userId: String, name: String, game: Game, events: List<EventCondition>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreating = true)

            val result = repository.createTicket(userId, name, game, events)

            result.onSuccess { newTicket ->
                _uiState.value = _uiState.value.copy(
                    allTickets = _uiState.value.allTickets + newTicket,
                    isCreating = false,
                    successMessage = "Ticket created successfully!"
                )
                navigationStateManager.navigateToTickets()
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isCreating = false,
                    error = e.message
                )
            }
        }
    }

    fun deleteTicket(ticketId: String) {
        viewModelScope.launch {
            val result = repository.deleteTicket(ticketId)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    allTickets = _uiState.value.allTickets.filterNot { it._id == ticketId }
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    fun loadUpcomingGames() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingGames = true, error = null)
            try {
                nhlDataManager.loadSchedule() // ensures NHL schedule is fetched
                val games = nhlDataManager.getGamesForTickets()
                _uiState.value = _uiState.value.copy(
                    availableGames = games,
                    isLoadingGames = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingGames = false,
                    error = "Failed to load upcoming games: ${e.message}"
                )
            }
        }
    }

    fun getEventsForGame(gameId: Long, onEventsLoaded: (List<EventCondition>) -> Unit) {
        viewModelScope.launch {
            try {
                val events = nhlDataManager.getEventsForGame(gameId)
                onEventsLoaded(events)
            } catch (e: Exception) {
                Log.e("TicketsViewModel", "Failed to get events for game $gameId", e)
                onEventsLoaded(emptyList())
            }
        }
    }

    fun checkIfTicketIsUsed(ticket: BingoTicket, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            challengesRepository.isTicketUsedInChallenge(ticket._id)
                .onSuccess { isUsed -> onResult(isUsed) }
                .onFailure {
                    // Handle the error appropriately
                    Log.e("TicketsViewModel", "Failed to check if ticket is used", it)
                    onResult(false) // Default to false on error
                }
        }
    }

    fun selectTicket(ticketId: String) {
        navigationStateManager.navigateToTicketDetail(ticketId)
    }

    fun toggleSection(gameState: String) {
        val currentExpanded = _uiState.value.expandedSections
        val newExpanded = if (currentExpanded.contains(gameState)) {
            currentExpanded - gameState
        } else {
            currentExpanded + gameState
        }
        _uiState.value = _uiState.value.copy(expandedSections = newExpanded)
    }


    fun updateTicketFromBoxscore(ticketId: String) {
        viewModelScope.launch {
            Log.d("TicketsViewModel", "Starting update for ticketId: $ticketId")
            val currentTickets = _uiState.value.allTickets.toMutableList()
            val index = currentTickets.indexOfFirst { it._id == ticketId }
            if (index == -1) {
                Log.w("TicketsViewModel", "Ticket with id $ticketId not found in current UI state.")
                return@launch
            }

            val ticket = currentTickets[index]
            Log.d("TicketsViewModel", "Found ticket: ${ticket.name} for game ${ticket.game.id}")

            val boxscoreResult = nhlDataManager.getBoxscore(ticket.game.id)
            if (boxscoreResult.isFailure) {
                Log.e("TicketsViewModel", "Failed to get boxscore for game ${ticket.game.id}: ${boxscoreResult.exceptionOrNull()?.message}")
                return@launch
            }

            val boxscore = boxscoreResult.getOrNull()
            if (boxscore == null) {
                Log.w("TicketsViewModel", "Boxscore is null for game ${ticket.game.id}. It might not have started.")
                return@launch
            }

            Log.d("TicketsViewModel", "Successfully fetched boxscore for game ${ticket.game.id}.")

            // Use the centralized logic from NhlDataManager
            val newCrossedOff = ticket.events.map { event ->
                val isFulfilled = nhlDataManager.isFulfilled(event, boxscore)
                Log.d("TicketsViewModel", "Checking event: $event -> Fulfilled: $isFulfilled")
                isFulfilled
            }

            val updatedTicket = ticket.copy(crossedOff = newCrossedOff)
            currentTickets[index] = updatedTicket

            _uiState.value = _uiState.value.copy(allTickets = currentTickets)

            // Sync changes to backend
            repository.updateCrossedOff(ticketId, newCrossedOff)
            Log.i("TicketsViewModel", "Update complete for ticketId: $ticketId. New crossedOff state: $newCrossedOff")
        }
    }
}
