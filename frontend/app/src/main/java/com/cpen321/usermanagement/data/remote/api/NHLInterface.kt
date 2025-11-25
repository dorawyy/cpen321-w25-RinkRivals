package com.cpen321.usermanagement.data.remote.api

import com.cpen321.usermanagement.data.remote.dto.Boxscore
import com.cpen321.usermanagement.data.remote.dto.ScheduleResponse
import com.cpen321.usermanagement.data.remote.dto.TeamRosterResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface NHLInterface {

    // Current schedule as of now
    @GET("v1/schedule/now")
    suspend fun getCurrentSchedule(): Response<ScheduleResponse>

    @GET("v1/roster/{team}/current")
    suspend fun getTeamRoster(@Path("team") teamCode: String): TeamRosterResponse

    @GET("v1/gamecenter/{gameId}/boxscore")
    suspend fun getBoxscore(@Path("gameId") gameId: Long): Response<Boxscore>

}