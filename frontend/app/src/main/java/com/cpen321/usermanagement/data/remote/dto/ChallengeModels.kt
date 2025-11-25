package com.cpen321.usermanagement.data.remote.dto

import com.google.gson.annotations.SerializedName
import java.util.Date

data class Challenge(
    val id: String,
    val title: String,
    val description: String,
    val ownerId: String,
    val gameId: String,
    val status: ChallengeStatus,
    val memberIds: List<String> = emptyList(),
    val memberNames: List<String> = emptyList(),
    val invitedUserIds: List<String> = emptyList(),
    val invitedUserNames: List<String> = emptyList(),
    val maxMembers: Int,
    val ticketIds: Map<String, String> = emptyMap(),
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val gameStartTime: Date? = null,
)
enum class ChallengeStatus {
    @SerializedName("pending")
    PENDING,

    @SerializedName("active")
    ACTIVE,

    @SerializedName("live")
    LIVE,

    @SerializedName("finished")
    FINISHED,

    @SerializedName("cancelled")
    CANCELLED
}

data class CreateChallengeRequest(
    val title: String,
    val description: String,
    val gameId: String,
    val invitedUserIds: List<String>? = null,
    val maxMembers: Int? = null,
    val gameStartTime: Date? = null,
    val ticketId: String? = null
)


