package com.cpen321.usermanagement.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cpen321.usermanagement.data.remote.dto.PendingRequestData
import com.cpen321.usermanagement.data.repository.FriendsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class Friend(
    val id: String,
    val name: String,
    val profilePicture: String? = null
)

/**
 * Holds *all* the UI state for the friends screen, like your ProfileUiState does.
 */
data class FriendsUiState(
    val friends: List<Friend> = emptyList(),
    val pendingRequests: List<PendingRequestData> = emptyList(),
    val isLoading: Boolean = false,
    val statusMessage: String? = null
)

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val repository: FriendsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    /**
     * Send a friend request by code, and show a message for a few seconds after.
     */
    fun sendFriendRequest(friendCode: String) {
        viewModelScope.launch {
            if (friendCode.isBlank()) {
                showMessage("Friend code cannot be empty")
                return@launch
            }

            val result = repository.sendFriendRequest(friendCode)
            if (result.isSuccess) {
                showMessage("Friend request sent!")
            } else {
                showMessage("Failed to send request: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun acceptFriendRequest(requestId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.acceptFriendRequest(requestId)
            refreshFriends()
            _uiState.value = _uiState.value.copy(isLoading = false)
            showMessage("Friend request accepted!")
        }
    }

    fun rejectFriendRequest(requestId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.rejectFriendRequest(requestId)
            refreshFriends()
            _uiState.value = _uiState.value.copy(isLoading = false)
            showMessage("Friend request rejected")
        }
    }

    fun removeFriend(friendId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.removeFriend(friendId)
            refreshFriends()
            _uiState.value = _uiState.value.copy(isLoading = false)
            showMessage("Friend removed")
        }
    }

    fun refreshFriends() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val friendsResult = repository.getFriends(currentUserId!!)
            val pendingResult = repository.getPendingRequests()

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                friends = friendsResult.getOrNull() ?: _uiState.value.friends,
                pendingRequests = pendingResult.getOrNull() ?: _uiState.value.pendingRequests
            )
        }
    }

    private var currentUserId: String? = null

    fun setCurrentUser(userId: String) {
        currentUserId = userId
        refreshFriends()
    }

    /**
     * Helper to show a message for a few seconds and then clear it.
     */
    private fun showMessage(message: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(statusMessage = message)
            delay(3000) // Show for 3 seconds
            _uiState.value = _uiState.value.copy(statusMessage = null)
        }
    }
}
