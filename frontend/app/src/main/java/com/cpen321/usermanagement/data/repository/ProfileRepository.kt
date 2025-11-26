package com.cpen321.usermanagement.data.repository

import android.net.Uri
import com.cpen321.usermanagement.data.remote.dto.PublicProfileData
import com.cpen321.usermanagement.data.remote.dto.User

interface ProfileRepository {
    suspend fun getProfile(): Result<User>

    suspend fun getUserInfoById(userId: String): Result<PublicProfileData>

    suspend fun uploadImage(imageUri: Uri): Result<String>

    suspend fun updateProfile(
        name: String? = null,
        bio: String? = null,
        profilePicture: String? = null
    ): Result<User>

    suspend fun deleteProfile(): Result<Unit>
}