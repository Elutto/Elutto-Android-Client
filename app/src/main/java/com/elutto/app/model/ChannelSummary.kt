package com.elutto.app.model

import org.threeten.bp.LocalDateTime

data class ChannelSummary(
    val channelId: String,
    val createTm: LocalDateTime,
    val isPublic: Boolean,
    val myChannelNotifySettings: Int,
    val myChannelPrivileges: Int,
    val myChannelRank: Int,
    val primaryUser: UserProfile,
    val themeColor: String?,
    val themeImage: String?,
    val title: String?,
    val lastPeekTm: LocalDateTime?,
    val lastMessage: ChannelMessage?,
    val msgCount: Int,
    val participants: List<UserProfile>,
    val events: List<ChannelEvent>
)
