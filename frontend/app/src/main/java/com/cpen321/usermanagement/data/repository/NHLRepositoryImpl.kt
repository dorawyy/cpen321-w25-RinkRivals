package com.cpen321.usermanagement.data.repository

import android.util.Log
import com.cpen321.usermanagement.data.remote.api.NHLInterface
import com.cpen321.usermanagement.data.remote.dto.Boxscore
import com.cpen321.usermanagement.data.remote.dto.GameDay
import com.cpen321.usermanagement.data.remote.dto.TeamRosterResponse
import com.cpen321.usermanagement.utils.JsonUtils.parseErrorMessage
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class NHLRepositoryImpl @Inject constructor(
    private val nhlInterface: NHLInterface
): NHLRepository {

    companion object {
        private const val TAG = "NHLRepositoryImpl"
    }

    override suspend fun getCurrentSchedule(): Result<List<GameDay>> {
        return try {
            val response = nhlInterface.getCurrentSchedule()


            Log.d("RAW", response.toString())
            Log.d("response", "${response.body()}")


            if (response.isSuccessful && response.body()?.gameWeek != null) {
                Result.success(response.body()!!.gameWeek)
            } else {
                val errorBodyString = response.errorBody()?.string()
                val errorMessage = parseErrorMessage(errorBodyString, "Failed to fetch NHL Schedule.")
                Log.e(TAG, "Failed to get NHL Schedule: $errorBodyString")


                Result.failure(Exception(errorMessage))
            }

        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Network timeout while getting NHL Schedule", e)
            Result.failure(e)
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Network connection failed while getting NHL Schedule", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            Log.e(TAG, "IO error while getting NHL Schedule", e)
            Result.failure(e)
        } catch (e: retrofit2.HttpException) {
            Log.e(TAG, "HTTP error while getting NHL Schedule: ${e.code()}", e)
            Result.failure(e)
        }
    }

    override suspend fun getTeamRoster(teamCode: String): Result<TeamRosterResponse> {
        return try {
            val response = nhlInterface.getTeamRoster(teamCode)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getBoxscore(gameId: Long): Result<Boxscore> {
        return try {
            val response = nhlInterface.getBoxscore(gameId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to load boxscore for $gameId"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}
