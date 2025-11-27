package com.cpen321.usermanagement.data.repository

import com.cpen321.usermanagement.data.remote.api.FriendsInterface
import com.cpen321.usermanagement.data.remote.api.RetrofitClient
import com.cpen321.usermanagement.data.remote.dto.PendingRequestData
import com.cpen321.usermanagement.ui.viewmodels.Friend
import javax.inject.Inject

class FriendsRepositoryImpl @Inject constructor(
    private val api: FriendsInterface
) : FriendsRepository {
    private val authHeader get() = "Bearer ${RetrofitClient.getAuthToken() ?: ""}"

    override suspend fun getFriends(currentUserId: String): Result<List<Friend>> = try {
        val response = api.getFriends(authHeader)
        if (response.isSuccessful) {
            val data = response.body()?.data?.mapNotNull { friendEntry ->
                val sender = friendEntry.sender
                val receiver = friendEntry.receiver

                // Skip if receiver is null (deleted account)
                if (receiver == null) {
                    return@mapNotNull null
                }

                // Pick the other person in the friendship
                val friendUser = if (sender._id == currentUserId) receiver else sender

                Friend(friendUser._id, friendUser.name, friendUser.profilePicture)
            } ?: emptyList()

            Result.success(data)
        } else {
            Result.failure(Exception(response.errorBody()?.string()))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }


    override suspend fun getPendingRequests(): Result<List<PendingRequestData>> = try {
        val response = api.getPendingRequests(authHeader)
        if (response.isSuccessful) {
            Result.success(response.body()?.data ?: emptyList())
        } else {
            Result.failure(Exception(response.errorBody()?.string()))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }


    override suspend fun sendFriendRequest(friendCode: String): Result<Unit> = try {
        val response = api.sendFriendRequest(authHeader, mapOf("receiverCode" to friendCode))
        if (response.isSuccessful) Result.success(Unit)
        else Result.failure(Exception(response.errorBody()?.string()))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun acceptFriendRequest(requestId: String): Result<Unit> = try {
        val response = api.acceptFriendRequest(authHeader, mapOf("requestId" to requestId))
        if (response.isSuccessful) Result.success(Unit)
        else Result.failure(Exception(response.errorBody()?.string()))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun rejectFriendRequest(requestId: String): Result<Unit> = try {
        val response = api.rejectFriendRequest(authHeader, mapOf("requestId" to requestId))
        if (response.isSuccessful) Result.success(Unit)
        else Result.failure(Exception(response.errorBody()?.string()))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun removeFriend(friendId: String): Result<Unit> = try {
        val response = api.removeFriend(authHeader, friendId)
        if (response.isSuccessful) Result.success(Unit)
        else Result.failure(Exception(response.errorBody()?.string()))
    } catch (e: Exception) {
        Result.failure(e)
    }
}
