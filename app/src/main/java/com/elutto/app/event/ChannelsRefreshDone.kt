package com.elutto.app.event

data class ChannelsRefreshDone(
        val dataModelUpdated: Boolean // true if the underlying store has been updated
)
