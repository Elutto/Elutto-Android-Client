package com.elutto.app.model

import com.google.gson.annotations.SerializedName
import org.threeten.bp.LocalDateTime

data class ApiChannelParticipantSummary(
    @SerializedName("join_tm") val channelJoinTm: LocalDateTime,
    @SerializedName("privileges") val channelPrivileges: List<String>,
    @SerializedName("user_id") val userId: Long,
    @SerializedName("user_rank") val userRank: Int
)
