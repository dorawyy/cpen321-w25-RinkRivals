package com.cpen321.usermanagement.fakes

import com.cpen321.usermanagement.data.remote.dto.Challenge
import com.cpen321.usermanagement.data.remote.dto.ChallengeStatus
import com.cpen321.usermanagement.data.remote.dto.CreateChallengeRequest
import com.cpen321.usermanagement.data.repository.ChallengesRepository

class FakeChallengesRepository : ChallengesRepository {

    private val challenges = mutableListOf<Challenge>()

    override suspend fun getChallenges(): Result<Map<String, List<Challenge>>> {
        val groupedChallenges = challenges.groupBy { it.status.name.lowercase() }
        return Result.success(mapOf("my_challenges" to challenges, "invitations" to emptyList()))
    }

    override suspend fun getChallenge(challengeId: String): Result<Challenge> {
        val challenge = challenges.find { it.id == challengeId }
        return if (challenge != null) {
            Result.success(challenge)
        } else {
            Result.failure(Exception("Challenge not found"))
        }
    }

    override suspend fun createChallenge(challengeRequest: CreateChallengeRequest): Result<Challenge> {
        val newChallenge = Challenge(
            id = "challenge_${challenges.size + 1}",
            title = challengeRequest.title,
            description = challengeRequest.description,
            ownerId = "fake_user_id", // Assuming a fake owner
            gameId = challengeRequest.gameId,
            status = ChallengeStatus.PENDING,
            maxMembers = challengeRequest.maxMembers ?: 2, // Default or from request
            invitedUserIds = challengeRequest.invitedUserIds ?: emptyList()
        )
        challenges.add(newChallenge)
        return Result.success(newChallenge)
    }

    override suspend fun updateChallenge(challenge: Challenge): Result<Challenge> {
        val index = challenges.indexOfFirst { it.id == challenge.id }
        if (index != -1) {
            challenges[index] = challenge
            return Result.success(challenge)
        }
        return Result.failure(Exception("Challenge not found"))
    }

    override suspend fun deleteChallenge(challengeId: String): Result<Unit> {
        challenges.removeIf { it.id == challengeId }
        return Result.success(Unit)
    }

    override suspend fun joinChallenge(challengeId: String, ticketId: String): Result<Unit> {
        val index = challenges.indexOfFirst { it.id == challengeId }
        if (index != -1) {
            val oldChallenge = challenges[index]
            val updatedChallenge = oldChallenge.copy(
                ticketIds = oldChallenge.ticketIds + ("fake_user_id" to ticketId),
                status = ChallengeStatus.ACTIVE
            )
            challenges[index] = updatedChallenge
            return Result.success(Unit)
        }
        return Result.failure(Exception("Challenge not found"))
    }

    override suspend fun leaveChallenge(challengeId: String): Result<Unit> {
        // In a real scenario, you'd have a user ID to remove the specific user
        challenges.removeIf { it.id == challengeId }
        return Result.success(Unit)
    }

    override suspend fun declineInvitation(challengeId: String): Result<Unit> {
        val index = challenges.indexOfFirst { it.id == challengeId }
        if (index != -1) {
            // In a real scenario, you'd remove the user from invited list
            // For fake, we can just change status or remove
            val oldChallenge = challenges[index]
            val updatedChallenge = oldChallenge.copy(status = ChallengeStatus.CANCELLED) // Or some other logic
            challenges[index] = updatedChallenge
            return Result.success(Unit)
        }
        return Result.failure(Exception("Challenge not found"))
    }

    override suspend fun isTicketUsedInChallenge(ticketId: String): Result<Boolean> {
        return Result.success(challenges.any { it.ticketIds.containsValue(ticketId) })
    }
}
