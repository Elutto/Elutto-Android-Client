package com.elutto.app.model

import com.google.gson.annotations.SerializedName
import org.threeten.bp.LocalDateTime

data class ApiChannelMessage(
    @SerializedName("msgid") val msgId: String,
    @SerializedName("athid") val authorId: String,
    @SerializedName("rcvtm") val receivedTm: LocalDateTime,
    @SerializedName("txt") val msgText: String?,
    @SerializedName("img") val msgImage: String?
)