package com.elutto.app.model

import org.threeten.bp.LocalDateTime

data class ChannelMessage(
        val msgId: String,
        val author: UserProfile,
        val msgServerReceivedTm: LocalDateTime,
        val msgText: String?,
        val msgImage: String?
)