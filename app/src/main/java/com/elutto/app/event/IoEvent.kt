package com.elutto.app.event

import org.threeten.bp.LocalDateTime

data class IoEvent(
    val timeOfEvent: LocalDateTime,
    val context: IoEventContext,
    val success: Boolean,
    val msg: String? = ""
)
