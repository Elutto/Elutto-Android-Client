package com.elutto.app.model

import com.google.gson.annotations.SerializedName
import org.threeten.bp.LocalDateTime

data class ApiChannelSummary(
        @SerializedName("channel_id") val channelId: String,
        @SerializedName("create_tm") val createTm: LocalDateTime,
        @SerializedName("events") val events: List<ApiChannelEventSummary>,
        @SerializedName("is_public") val isPublic: Boolean,
        @SerializedName("my_notify_settings") val myNotifySettings: List<String>,
        @SerializedName("my_privileges") val myChannelPrivileges: List<String>,
        @SerializedName("my_rank") val myChannelRank: Int,
        @SerializedName("participants") val participants: List<ApiChannelParticipantSummary>,
        @SerializedName("primary_user_id") val primaryUserId: String,
        @SerializedName("theme_color") val themeColor: String?,
        @SerializedName("theme_image") val themeImage: String?,
        @SerializedName("title") val title: String?,
        @SerializedName("msg_count") val msgCount: Int,
        @SerializedName("msg_recent") val recentMsg: List<ApiChannelMessage>
)