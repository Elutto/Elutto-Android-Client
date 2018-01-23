package com.elutto.app.model

import com.google.gson.annotations.SerializedName
import org.threeten.bp.LocalDateTime

data class ApiChannelEventSummary(
    @SerializedName("event_id") val eventId: String,
    @SerializedName("author_user_id") val authorUserId: String,
    @SerializedName("accepted_count") val acceptedCount: Int,
    @SerializedName("denied_count") val deniedCount: Int,
    @SerializedName("max_allowed_users") val maxAllowedUsers: Int,
    @SerializedName("min_required_users") val minRequiredUsers: Int,
    @SerializedName("title") val title: String,
    @SerializedName("rsvp_expiry_tm") val rsvpExpiryTm: LocalDateTime,
    @SerializedName("rsvp_start_tm") val rsvpStartTm: LocalDateTime,
    @SerializedName("declarations") val declarations: List<ApiChannelEventDeclaration>,
    @SerializedName("underlying_event_location") val underlyingEventLocation: String?,
    @SerializedName("underlying_event_start_tm") val underlyingEventStartTm: LocalDateTime?,
    @SerializedName("event_icon") val icon: String?
)
