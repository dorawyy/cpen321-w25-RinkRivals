package com.cpen321.usermanagement.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.cpen321.usermanagement.data.local.preferences.TokenManager
import com.cpen321.usermanagement.data.remote.api.ImageInterface
import com.cpen321.usermanagement.data.remote.api.RetrofitClient
import com.cpen321.usermanagement.data.remote.api.UserInterface
import com.cpen321.usermanagement.data.remote.dto.PublicProfileData
import com.cpen321.usermanagement.data.remote.dto.UpdateProfileRequest
import com.cpen321.usermanagement.data.remote.dto.User
import com.cpen321.usermanagement.utils.JsonUtils.parseErrorMessage
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userInterface: UserInterface,
    private val imageInterface: ImageInterface,
    private val tokenManager: TokenManager
) : ProfileRepository {

    companion object {
        private const val TAG = "ProfileRepositoryImpl"
    }

    override suspend fun getProfile(): Result<User> {
        return try {
            val response = userInterface.getProfile("") // Auth header is handled by interceptor
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!.user)
            } else {
                val errorBodyString = response.errorBody()?.string()
                val errorMessage =
                    parseErrorMessage(errorBodyString, "Failed to fetch user information.")
                Log.e(TAG, "Failed to get profile: $errorMessage")
                tokenManager.clearToken()
                RetrofitClient.setAuthToken(null)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Network timeout while getting profile", e)
            Result.failure(e)
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Network connection failed while getting profile", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            Log.e(TAG, "IO error while getting profile", e)
            Result.failure(e)
        } catch (e: retrofit2.HttpException) {
            Log.e(TAG, "HTTP error while getting profile: ${e.code()}", e)
            Result.failure(e)
        }
    }

    override suspend fun getUserInfoById(userId: String): Result<PublicProfileData> {
        return try {
            val response = userInterface.getUserInfoById("", userId) // Auth header is handled by interceptor
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorBodyString = response.errorBody()?.string()
                val errorMessage =
                    parseErrorMessage(errorBodyString, "Failed to fetch user information.")
                Log.e(TAG, "Failed to get profile info: $errorMessage")
                tokenManager.clearToken()
                RetrofitClient.setAuthToken(null)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Network timeout while getting profile info", e)
            Result.failure(e)
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Network connection failed while getting profile info", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            Log.e(TAG, "IO error while getting profile info", e)
            Result.failure(e)
        } catch (e: retrofit2.HttpException) {
            Log.e(TAG, "HTTP error while getting profile info: ${e.code()}", e)
            Result.failure(e)
        }
    }

    override suspend fun uploadImage(imageUri: Uri): Result<String> {
        return try {
            // Convert URI to File
            val file = uriToFile(imageUri) ?: return Result.failure(Exception("Failed to read image file"))
            
            // Create request body and multipart body part
            val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
            val multipartBody = MultipartBody.Part.createFormData("media", file.name, requestBody)
            
            // Upload image
            val response = imageInterface.uploadPicture("", multipartBody)
            
            // Clean up temporary file
            file.delete()
            
            if (response.isSuccessful && response.body()?.data != null) {
                val imagePath = response.body()!!.data!!.image
                Log.d(TAG, "Image uploaded successfully: $imagePath")
                Result.success(imagePath)
            } else {
                val errorBodyString = response.errorBody()?.string()
                val errorMessage = parseErrorMessage(errorBodyString, "Failed to upload image.")
                Log.e(TAG, "Failed to upload image: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Network timeout while uploading image", e)
            Result.failure(e)
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Network connection failed while uploading image", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            Log.e(TAG, "IO error while uploading image", e)
            Result.failure(e)
        } catch (e: retrofit2.HttpException) {
            Log.e(TAG, "HTTP error while uploading image: ${e.code()}", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error while uploading image", e)
            Result.failure(e)
        }
    }
    
    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile("upload_", ".jpg", context.cacheDir)
            
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            inputStream.close()
            
            tempFile
        } catch (e: Exception) {
            Log.e(TAG, "Error converting URI to file", e)
            null
        }
    }

    override suspend fun updateProfile(
        name: String?,
        bio: String?,
        profilePicture: String?,
    ): Result<User> {
        return try {
            // If profilePicture is a URI (starts with content:// or file://), upload it first
            val imageUrl = if (profilePicture != null && 
                (profilePicture.startsWith("content://") || profilePicture.startsWith("file://"))) {
                val uploadResult = uploadImage(Uri.parse(profilePicture))
                if (uploadResult.isSuccess) {
                    uploadResult.getOrNull()
                } else {
                    return Result.failure(uploadResult.exceptionOrNull() ?: Exception("Failed to upload image"))
                }
            } else {
                profilePicture
            }
            
            val updateRequest = UpdateProfileRequest(
                name = name, 
                bio = bio, 
                profilePicture = imageUrl
            )
            val response = userInterface.updateProfile("", updateRequest) // Auth header is handled by interceptor
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!.user)
            } else {
                val errorBodyString = response.errorBody()?.string()
                val errorMessage = parseErrorMessage(errorBodyString, "Failed to update profile.")
                Log.e(TAG, "Failed to update profile: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Network timeout while updating profile", e)
            Result.failure(e)
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Network connection failed while updating profile", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            Log.e(TAG, "IO error while updating profile", e)
            Result.failure(e)
        } catch (e: retrofit2.HttpException) {
            Log.e(TAG, "HTTP error while updating profile: ${e.code()}", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteProfile(): Result<Unit> {
        return try {
            val response = userInterface.deleteProfile("") // Auth header is handled by interceptor
            if (response.isSuccessful) {
                // Clear tokens after successful deletion since user no longer exists
                tokenManager.clearToken()
                RetrofitClient.setAuthToken(null)
                Result.success(Unit)
            } else {
                val errorBodyString = response.errorBody()?.string()
                val errorMessage = parseErrorMessage(errorBodyString, "Failed to delete profile.")
                Log.e(TAG, "Failed to delete profile: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Network timeout while deleting profile", e)
            Result.failure(e)
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Network connection failed while deleting profile", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            Log.e(TAG, "IO error while deleting profile", e)
            Result.failure(e)
        } catch (e: retrofit2.HttpException) {
            Log.e(TAG, "HTTP error while deleting profile: ${e.code()}", e)
            Result.failure(e)
        }
    }

}
