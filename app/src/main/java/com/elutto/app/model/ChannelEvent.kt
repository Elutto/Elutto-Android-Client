package com.elutto.app.model

import com.google.gson.annotations.SerializedName
import org.threeten.bp.LocalDateTime

data class ChannelEvent(
    val eventId: String,
    val authorUser: UserProfile,
    val acceptedCount: Int,
    val deniedCount: Int,
    val maxAllowedUsers: Int,
    val minRequiredUsers: Int,
    val title: String,
    val rsvpExpiryTm: LocalDateTime,
    val rsvpStartTm: LocalDateTime,
    val declarations: List<ChannelEventDeclaration>,
    val underlyingEventLocation: String?,
    val underlyingEventStartTm: LocalDateTime?,
    val icon: String?)
