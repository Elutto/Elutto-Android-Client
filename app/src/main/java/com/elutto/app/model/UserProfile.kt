package com.elutto.app.model

data class UserProfile(
    val userId: String,
    val username: String,
    val firstName: String,
    val lastName: String,
    val middleName: String?,
    val avatar: String?,
    val circle: String?
)
