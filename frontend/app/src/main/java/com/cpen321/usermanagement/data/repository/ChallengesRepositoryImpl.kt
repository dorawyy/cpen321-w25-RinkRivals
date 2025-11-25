package com.cpen321.usermanagement.data.repository

import android.util.Log
import com.cpen321.usermanagement.data.remote.api.ChallengesInterface
import com.cpen321.usermanagement.data.remote.dto.Challenge
import com.cpen321.usermanagement.data.remote.dto.CreateChallengeRequest
import com.cpen321.usermanagement.utils.JsonUtils.parseErrorMessage
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ChallengesRepositoryImpl @Inject constructor(
    private val challengeInterface: ChallengesInterface,
) : ChallengesRepository {

    companion object {
        private const val TAG = "ChallengeRepositoryImpl"
    }


    override suspend fun getChallenges(): Result<Map<String, List<Challenge>>> {
        return try {
            val response = challengeInterface.getChallenges("")
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorBodyString = response.errorBody()?.string()
                val errorMessage =
                    parseErrorMessage(errorBodyString, "Failed to fetch challenges.")
                Log.e(TAG, "Failed to get challenges: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Network timeout while getting challenges", e)
            Result.failure(e)
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Network connection failed while getting challenges", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            Log.e(TAG, "IO error while getting challenges", e)
            Result.failure(e)
        } catch (e: retrofit2.HttpException) {
            Log.e(TAG, "HTTP error while getting challenges: ${e.code()}", e)
            Result.failure(e)
        }
    }

    override suspend fun getChallenge(challengeId: String): Result<Challenge> {
        return try {
            val response = challengeInterface.getChallenge("", challengeId = challengeId)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorBodyString = response.errorBody()?.string()
                val errorMessage =
                    parseErrorMessage(errorBodyString, "Failed to fetch challenges.")
                Log.e(TAG, "Failed to get challenges: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Network timeout while getting challenges", e)
            Result.failure(e)
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Network connection failed while getting challenges", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            Log.e(TAG, "IO error while getting challenges", e)
            Result.failure(e)
        } catch (e: retrofit2.HttpException) {
            Log.e(TAG, "HTTP error while getting challenges: ${e.code()}", e)
            Result.failure(e)
        }
    }



    override suspend fun createChallenge(challengeRequest: CreateChallengeRequest): Result<Challenge> {
        return try {
            // API response from backend!!
            Log.d(TAG, "Creating challenge with request: $challengeRequest")
            val response = challengeInterface.createChallenge("", challengeRequest)


            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorBodyString = response.errorBody()?.string()
                val errorMessage =
                    parseErrorMessage(errorBodyString, "Failed to fetch challenges.")
                Log.e(TAG, "Failed to get challenges: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Network timeout while getting challenges", e)
            Result.failure(e)
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Network connection failed while getting challenges", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            Log.e(TAG, "IO error while getting challenges", e)
            Result.failure(e)
        } catch (e: retrofit2.HttpException) {
            Log.e(TAG, "HTTP error while getting challenges: ${e.code()}", e)
            Result.failure(e)
        }
    }


    override suspend fun updateChallenge(challenge: Challenge): Result<Challenge> {
        return try {
            val response = challengeInterface.updateChallenge("", challenge.id, challenge)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorBodyString = response.errorBody()?.string()
                val errorMessage =
                    parseErrorMessage(errorBodyString, "Failed to update challenge.")
                Log.e(TAG, "Failed to update challenge: $errorMessage")
                Result.failure(Exception(errorMessage))
            }

        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Network timeout while updating challenge", e)
            Result.failure(e)
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Network connection failed while updating challenge", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            Log.e(TAG, "IO error while updating challenge", e)
            Result.failure(e)
        } catch (e: retrofit2.HttpException) {
            Log.e(TAG, "HTTP error while updating challenge: ${e.code()}", e)
            Result.failure(e)
        }

    }

    override suspend fun deleteChallenge(challengeId: String): Result<Unit> {
        return try {
            val response = challengeInterface.deleteChallenge("", challengeId)

            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorBodyString = response.errorBody()?.string()
                val errorMessage =
                    parseErrorMessage(errorBodyString, "Failed to delete challenge.")
                Log.e(TAG, "Failed to delete challenge: $errorMessage")
                Result.failure(Exception(errorMessage))
            }

        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Network timeout while deleting challenge", e)
            Result.failure(e)
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Network connection failed while deleting challenge", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            Log.e(TAG, "IO error while deleting challenge", e)
            Result.failure(e)
        } catch (e: retrofit2.HttpException) {
            Log.e(TAG, "HTTP error while deleting challenge: ${e.code()}", e)
            Result.failure(e)
        }
    }

    override suspend fun joinChallenge(challengeId: String, ticketId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Joining challenge with ticket: $ticketId")
            val requestBody = mapOf("ticketId" to ticketId) // ✅ Create proper JSON object
            val response = challengeInterface.joinChallenge("", challengeId, requestBody)

            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorBodyString = response.errorBody()?.string()
                val errorMessage =
                    parseErrorMessage(errorBodyString, "Failed to join challenge.")
                Log.e(TAG, "Failed to join challenge: $errorMessage")
                Result.failure(Exception(errorMessage))
            }

        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Network timeout while joining challenge", e)
            Result.failure(e)
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Network connection failed while joining challenge", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            Log.e(TAG, "IO error while joining challenge", e)
            Result.failure(e)
        } catch (e: retrofit2.HttpException) {
            Log.e(TAG, "HTTP error while joining challenge: ${e.code()}", e)
            Result.failure(e)
        }
    }

    override suspend fun leaveChallenge(challengeId: String): Result<Unit> {
        return try {
            val response = challengeInterface.leaveChallenge("", challengeId)

            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorBodyString = response.errorBody()?.string()
                val errorMessage =
                    parseErrorMessage(errorBodyString, "Failed to leave challenge.")
                Log.e(TAG, "Failed to leave challenge: $errorMessage")
                Result.failure(Exception(errorMessage))
            }

        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Network timeout while leaving challenge", e)
            Result.failure(e)
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Network connection failed while leaving challenge", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            Log.e(TAG, "IO error while leaving challenge", e)
            Result.failure(e)
        } catch (e: retrofit2.HttpException) {
            Log.e(TAG, "HTTP error while leaving challenge: ${e.code()}", e)
            Result.failure(e)
        }
    }

    override suspend fun declineInvitation(challengeId: String): Result<Unit> {
        return try {
            val response = challengeInterface.declineInvitation("", challengeId)

            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorBodyString = response.errorBody()?.string()
                val errorMessage =
                    parseErrorMessage(errorBodyString, "Failed to decline invitation.")
                Log.e(TAG, "Failed to decline invitation: $errorMessage")
                Result.failure(Exception(errorMessage))
            }

        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Network timeout while declining invitation", e)
            Result.failure(e)
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Network connection failed while declining invitation", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            Log.e(TAG, "IO error while declining invitation", e)
            Result.failure(e)
        } catch (e: retrofit2.HttpException) {
            Log.e(TAG, "HTTP error while declining invitation: ${e.code()}", e)
            Result.failure(e)
        }
    }



    override suspend fun isTicketUsedInChallenge(ticketId: String): Result<Boolean> {
        return try {
            // This is a simple client-side check.
            // A dedicated backend endpoint could be an even better solution.
            val challengesResult = getChallenges()

            challengesResult.map { challengesMap ->
                // 1. `challengesMap.values` gets all the lists:
                //    [ [pending challenges], [active challenges], [live challenges]... ]
                // 2. `.flatten()` merges them into one single list:
                //    [ ChallengeA, ChallengeB, ChallengeC, ChallengeD... ]
                // 3. `.any()` now safely checks every single challenge in that combined list.
                val isFound = challengesMap.values.flatten().any { challenge ->
                    challenge.ticketIds.containsValue(ticketId)
                }

                if (isFound) {
                    Log.d(TAG, "!!! Ticket FOUND in a challenge.")
                } else {
                    Log.d(TAG, "--- Ticket '$ticketId' was NOT found in any challenge.")
                }

                isFound // Return the boolean result
            }
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Network timeout while checking ticket usage", e)
            Result.failure(e)
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Network connection failed while checking ticket usage", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            Log.e(TAG, "IO error while checking ticket usage", e)
            Result.failure(e)
        } catch (e: retrofit2.HttpException) {
            Log.e(TAG, "HTTP error while checking ticket usage: ${e.code()}", e)
            Result.failure(e)
        }
    }


}
