package com.elutto.app.model

import org.threeten.bp.LocalDateTime

data class ChannelEventDeclaration(
        val user: UserProfile,
        val declResponse: String,
        val declTm: LocalDateTime
)