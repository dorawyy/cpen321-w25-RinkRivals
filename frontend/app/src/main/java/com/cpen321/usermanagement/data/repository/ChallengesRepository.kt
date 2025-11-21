package com.cpen321.usermanagement.data.repository

import com.cpen321.usermanagement.data.remote.dto.Challenge
import com.cpen321.usermanagement.data.remote.dto.CreateChallengeRequest

interface ChallengesRepository {

    suspend fun getChallenges(): Result<Map<String, List<Challenge>>>

    suspend fun getChallenge(challengeId: String): Result<Challenge>

    suspend fun createChallenge(challengeRequest: CreateChallengeRequest): Result<Challenge>

    suspend fun updateChallenge(challenge: Challenge): Result<Challenge>

    suspend fun deleteChallenge(challengeId: String): Result<Unit>

    suspend fun joinChallenge(challengeId: String, ticketId: String): Result<Unit>

    suspend fun leaveChallenge(challengeId: String): Result<Unit>

    suspend fun declineInvitation(challengeId: String): Result<Unit>

    suspend fun isTicketUsedInChallenge(ticketId: String): Result<Boolean>
}


