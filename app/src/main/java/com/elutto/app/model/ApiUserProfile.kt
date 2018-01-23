package com.elutto.app.model

import com.google.gson.annotations.SerializedName

data class ApiUserProfile(
    @SerializedName("uid") val userId: String,
    @SerializedName("un") val username: String,
    @SerializedName("fn") val firstName: String,
    @SerializedName("ln") val lastName: String,
    @SerializedName("mn") val middleName: String?,
    @SerializedName("gn") val gender: String,
    @SerializedName("av") val avatar: String?,
    @SerializedName("cr") val circle: String?
)