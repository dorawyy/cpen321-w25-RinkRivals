package com.cpen321.usermanagement.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cpen321.usermanagement.data.local.preferences.NhlDataManager
import com.cpen321.usermanagement.data.remote.dto.BingoTicket
import com.cpen321.usermanagement.data.remote.dto.Challenge
import com.cpen321.usermanagement.data.remote.dto.CreateChallengeRequest
import com.cpen321.usermanagement.data.remote.dto.Game
import com.cpen321.usermanagement.data.remote.dto.User
import com.cpen321.usermanagement.data.repository.ChallengesRepository
import com.cpen321.usermanagement.data.repository.FriendsRepository
import com.cpen321.usermanagement.data.repository.ProfileRepository
import com.cpen321.usermanagement.data.repository.TicketsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChallengesUiState(

    // loading states
    val isLoadingChallenges: Boolean = false,
    val isLoadingChallenge: Boolean = false,
    val isLoadingProfile: Boolean = false,
    val isLoadingFriends: Boolean = false,
    val isLoadingBingoTickets: Boolean = false,
    val isLoadingGames: Boolean = false,
    val isDeletingChallenge: Boolean = false,
    val isUpdatingChallenge: Boolean = false,
    val isJoiningChallenge: Boolean = false,
    val isLeavingChallenge: Boolean = false,


    //data states
    val user: User? = null,
    val allFriends: List<Friend>? = null,
    val availableGames: List<Game>? = emptyList(),
    val availableTicketsForJoining: List<BingoTicket>? = emptyList(),
    val challengeTickets: List<BingoTicket>? = emptyList(),
    val allChallenges: Map<String, List<Challenge>>? = null, // Map of challenge status to list of challenges
    val allPendingChallenges: List<Challenge>? = null,
    val allActiveChallenges: List<Challenge>? = null,
    val allLiveChallenges: List<Challenge>? = null,
    val allFinishedChallenges: List<Challenge>? = null,
    val allCancelledChallenges: List<Challenge>? = null,
    val selectedChallenge: Challenge? = null,

    // message states
    val errorMessage: String? = null,
    val successMessage: String? = null,
)



@HiltViewModel
class ChallengesViewModel @Inject constructor(
    private val challengesRepository: ChallengesRepository,
    private val profileRepository: ProfileRepository,
    private val friendsRepository: FriendsRepository,
    private val ticketsRepository: TicketsRepository,
    val nhlDataManager: NhlDataManager
) : ViewModel() {
    companion object {
        private const val TAG = "ChallengesViewModel"
    }

    private val _uiState = MutableStateFlow(ChallengesUiState())
    val uiState = _uiState.asStateFlow()

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingProfile = true, errorMessage = null)

            val profileResult = profileRepository.getProfile()

            val user = profileResult.getOrNull()



            _uiState.value = _uiState.value.copy(
                isLoadingProfile = false,
                user = user,
            )

            if (profileResult.isFailure) {
                val error = profileResult.exceptionOrNull()
                val errorMessage = error?.message ?: "Failed to load profile"
                Log.e(TAG, "Failed to load profile", error)

                _uiState.value = _uiState.value.copy(
                    isLoadingProfile = false,
                    errorMessage = errorMessage
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoadingProfile = false,
                    errorMessage = null
                )
            }
        }
    }

    fun loadFriends(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingFriends = true, errorMessage = null)

            val friendsResult = friendsRepository.getFriends(userId)
            val friends = friendsResult.getOrNull()

            _uiState.value = _uiState.value.copy(
                isLoadingFriends = false,
                allFriends = friends
            )

            if (friendsResult.isFailure) {
                val error = friendsResult.exceptionOrNull()
                val errorMessage = error?.message ?: "Failed to load friends"
                Log.e(TAG, "Failed to load friends", error)

                _uiState.value = _uiState.value.copy(
                    isLoadingFriends = false,
                    errorMessage = errorMessage
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoadingFriends = false,
                    errorMessage = null
                )
            }



        }
    }

    fun loadChallenges() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingChallenges = true, errorMessage = null)

            val challengesResult = challengesRepository.getChallenges()

            val challenges = challengesResult.getOrNull()

            _uiState.value = _uiState.value.copy(
                isLoadingChallenges = false,
                allChallenges = challenges,
                allPendingChallenges = challenges?.get("pending"),
                allActiveChallenges = challenges?.get("active"),
                allLiveChallenges = challenges?.get("live"),
                allFinishedChallenges = challenges?.get("finished"),
                allCancelledChallenges = challenges?.get("cancelled")
            )

            if (challengesResult.isFailure) {
                val error = challengesResult.exceptionOrNull()
                val errorMessage = error?.message ?: "Failed to load challenges"
                Log.e(TAG, "Failed to load profile", error)

                _uiState.value = _uiState.value.copy(
                    isLoadingChallenges = false,
                    errorMessage = errorMessage
                )
            }
        }
    }

    fun loadChallenge(challengeId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingChallenge = true, errorMessage = null)
            val challengeResult = challengesRepository.getChallenge(challengeId)
            val challenge = challengeResult.getOrNull()
            _uiState.value = _uiState.value.copy(
                isLoadingChallenge = false,
                selectedChallenge = challenge
            )
            if (challengeResult.isFailure) {
                val error = challengeResult.exceptionOrNull()
                val errorMessage = error?.message ?: "Failed to load challenge"
                Log.e(TAG, "Failed to load profile", error)

                _uiState.value = _uiState.value.copy(
                    isLoadingChallenges = false,
                    errorMessage = errorMessage
                )
            }


        }
    }

    fun createChallenge(challengeRequest: CreateChallengeRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingChallenges = true, errorMessage = null)

            Log.d(TAG, "Creating challenge with request: $challengeRequest")

            val challengesResult = challengesRepository.createChallenge(challengeRequest)

            _uiState.value = _uiState.value.copy(
                isLoadingChallenges = false,
            )
            loadChallenges()

            if (challengesResult.isFailure) {
                val error = challengesResult.exceptionOrNull()
                val errorMessage = error?.message ?: "Failed to load challenges"
                Log.e(TAG, "Failed to load profile", error)

                _uiState.value = _uiState.value.copy(
                    isLoadingChallenges = false,
                    errorMessage = errorMessage
                )
            }
        }
    }

    fun updateChallenge(challenge: Challenge) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdatingChallenge = true, errorMessage = null)

            val updatedChallengeResult = challengesRepository.updateChallenge(challenge)
            _uiState.value = _uiState.value.copy(
                isUpdatingChallenge = false,
            )
            loadChallenges()

            if (updatedChallengeResult.isFailure) {
                val error = updatedChallengeResult.exceptionOrNull()
                val errorMessage = error?.message ?: "Failed to update challenge"
                Log.e(TAG, "Failed to update challenge", error)

                _uiState.value = _uiState.value.copy(
                    isUpdatingChallenge = false,
                    errorMessage = errorMessage
                )
            }
        }
    }

    fun deleteChallenge(challengeId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeletingChallenge = true, errorMessage = null)

            val deleteResult = challengesRepository.deleteChallenge(challengeId)

            _uiState.value = _uiState.value.copy(
                isDeletingChallenge = false,
                selectedChallenge = null, // Clear the selected challenge
                successMessage = "Challenge deleted successfully!"
            )


            if (deleteResult.isFailure) {
                val error = deleteResult.exceptionOrNull()
                val errorMessage = error?.message ?: "Failed to delete challenge"
                Log.e(TAG, "Failed to delete challenge", error)
                _uiState.value = _uiState.value.copy(
                    isDeletingChallenge = false,
                    errorMessage = errorMessage
                )
            }
        }
    }

    fun joinChallenge(challengeId: String, ticketId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isJoiningChallenge = true, errorMessage = null)

            val joinResult = challengesRepository.joinChallenge(challengeId = challengeId, ticketId = ticketId)

            _uiState.value = _uiState.value.copy(
                isJoiningChallenge = false,
                successMessage = "Challenge joined successfully!"
            )


            if (joinResult.isFailure) {
                val error = joinResult.exceptionOrNull()
                val errorMessage = error?.message ?: "Failed to join challenge"
                Log.e(TAG, "Failed to join challenge", error)
                _uiState.value = _uiState.value.copy(
                    isJoiningChallenge = false,
                    errorMessage = errorMessage
                )
            }
        }
    }

    fun leaveChallenge(challengeId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLeavingChallenge = true, errorMessage = null)

            val joinResult = challengesRepository.leaveChallenge(challengeId = challengeId)

            _uiState.value = _uiState.value.copy(
                isLeavingChallenge = false,
                successMessage = "Challenge left successfully!"
            )


            if (joinResult.isFailure) {
                val error = joinResult.exceptionOrNull()
                val errorMessage = error?.message ?: "Failed to leave challenge"
                Log.e(TAG, "Failed to leave challenge", error)
                _uiState.value = _uiState.value.copy(
                    isLeavingChallenge = false,
                    errorMessage = errorMessage
                )
            }
        }
    }

    fun declineInvitation(challengeId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLeavingChallenge = true, errorMessage = null)

            val declineResult = challengesRepository.declineInvitation(challengeId = challengeId)

            _uiState.value = _uiState.value.copy(
                isLeavingChallenge = false,
                successMessage = "Invitation declined successfully!"
            )

            if (declineResult.isFailure) {
                val error = declineResult.exceptionOrNull()
                val errorMessage = error?.message ?: "Failed to decline invitation"
                Log.e(TAG, "Failed to decline invitation", error)
                _uiState.value = _uiState.value.copy(
                    isLeavingChallenge = false,
                    errorMessage = errorMessage
                )
            }
        }
    }

    /**
     * Load upcoming NHL games for challenge creation
     */
    fun loadUpcomingGames() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingGames = true, errorMessage = null)
            try {
                // Load NHL schedule if not already loaded
                nhlDataManager.loadSchedule()
                
                // Get upcoming games and update UI state
                val games = nhlDataManager.getGamesForChallenges()
                _uiState.value = _uiState.value.copy(
                    availableGames = games,
                    isLoadingGames = false
                )
                
                Log.d(TAG, "Loaded ${games.size} upcoming games for challenges")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading upcoming games", e)
                _uiState.value = _uiState.value.copy(
                    isLoadingGames = false,
                    errorMessage = "Failed to load upcoming games: ${e.message}"
                )
            }
        }
    }


    /**
     * Load available bingo tickets for a user
     */
    fun loadAvailableTickets(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingBingoTickets = true, errorMessage = null)

            val ticketsResult = ticketsRepository.getTickets(userId)
            val tickets = ticketsResult.getOrNull()

            _uiState.value = _uiState.value.copy(
                isLoadingBingoTickets = false,
                availableTicketsForJoining = tickets
            )

            if (ticketsResult.isFailure) {
                val error = ticketsResult.exceptionOrNull()
                val errorMessage = error?.message ?: "Failed to load friends"
                Log.e(TAG, "Failed to load friends", error)

                _uiState.value = _uiState.value.copy(
                    isLoadingBingoTickets = false,
                    errorMessage = errorMessage
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoadingBingoTickets = false,
                    errorMessage = null
                )
            }
        }
    }

    /**
     * Load a single ticket by ID and add it to challengeTickets
     */
    fun loadTicketById(ticketId: String) {
        viewModelScope.launch {
            val ticketResult = ticketsRepository.getTicketById(ticketId)
            val ticket = ticketResult.getOrNull()

            if (ticket != null) {
                // Update ticket if it exists, otherwise add it
                val currentTickets = _uiState.value.challengeTickets.orEmpty().toMutableList()
                val existingIndex = currentTickets.indexOfFirst { it._id == ticketId }
                
                if (existingIndex >= 0) {
                    // Update existing ticket
                    currentTickets[existingIndex] = ticket
                } else {
                    // Add new ticket
                    currentTickets.add(ticket)
                }
                
                _uiState.value = _uiState.value.copy(challengeTickets = currentTickets)
            } else {
                Log.w(TAG, "Failed to load ticket: $ticketId")
            }
        }
    }

}