package com.cpen321.usermanagement.ui.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cpen321.usermanagement.data.remote.dto.User
import com.cpen321.usermanagement.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    // Loading states
    val isLoadingProfile: Boolean = false,
    val isSavingProfile: Boolean = false,
    val isLoadingPhoto: Boolean = false,
    val isDeletingProfile: Boolean = false,


    // Data states
    val user: User? = null,
    val isProfileDeleted: Boolean = false,


    // Message states
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ProfileViewModel"
    }

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingProfile = true, 
                errorMessage = null,
                isProfileDeleted = false // Reset deletion flag when loading profile
            )

            val profileResult = profileRepository.getProfile()

            val user = profileResult.getOrNull()

            _uiState.value = _uiState.value.copy(
                isLoadingProfile = false,
                user = user
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

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    fun resetDeletionState() {
        _uiState.value = _uiState.value.copy(isProfileDeleted = false)
    }

    fun setLoadingPhoto(isLoading: Boolean) {
        _uiState.value = _uiState.value.copy(isLoadingPhoto = isLoading)
    }

    fun uploadProfilePicture(pictureUri: Uri) {
        viewModelScope.launch {
            val currentUser = _uiState.value.user ?: return@launch
            // log user
            Log.d(TAG, "currentUser: $currentUser")
            val updatedUser = currentUser.copy(profilePicture = pictureUri.toString())
            updateProfile(updatedUser.name, updatedUser.bio, pictureUri)
            _uiState.value = _uiState.value.copy(isLoadingPhoto = false, user= updatedUser, successMessage = "Profile picture updated successfully!")
        }
    }



    fun updateProfile(name: String? = null, bio: String? = null, pictureUri: Uri? = null, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.value =
                _uiState.value.copy(
                    isSavingProfile = true,
                    errorMessage = null,
                    successMessage = null
                )

            val updateResult = profileRepository.updateProfile(name?: _uiState.value.user?.name, bio?: _uiState.value.user?.bio, profilePicture = pictureUri?.toString())
            if (updateResult.isSuccess) {
                val updatedUser = updateResult.getOrNull()!!
                _uiState.value = _uiState.value.copy(
                    isSavingProfile = false,
                    user = updatedUser,
                    successMessage = "Profile updated successfully!"
                )
                onSuccess()
            } else {
                val error = updateResult.exceptionOrNull()
                Log.e(TAG, "Failed to update profile", error)
                val errorMessage = error?.message ?: "Failed to update profile"
                _uiState.value = _uiState.value.copy(
                    isSavingProfile = false,
                    errorMessage = errorMessage
                )
            }
        }
    }

    fun deleteProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeletingProfile = true, errorMessage = null)

            val deleteResult = profileRepository.deleteProfile()

            if (deleteResult.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isDeletingProfile = false,
                    successMessage = "Profile deleted successfully!",
                    isProfileDeleted = true // Add this flag to trigger navigation
                )
            } else {
                val error = deleteResult.exceptionOrNull()
                Log.e(TAG, "Failed to delete profile", error)
                val errorMessage = error?.message ?: "Failed to delete profile"
                _uiState.value = _uiState.value.copy(
                    isDeletingProfile = false,
                    errorMessage = errorMessage
                )
            }

        }
    }
}
