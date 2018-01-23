package com.elutto.app.model

import com.google.gson.annotations.SerializedName
import org.threeten.bp.LocalDateTime

data class ApiChannelEventDeclaration(
    @SerializedName("decl_resp") val declResponse: String,
    @SerializedName("decl_tm") val declTime: LocalDateTime,
    @SerializedName("user_id") val userId: String
)
