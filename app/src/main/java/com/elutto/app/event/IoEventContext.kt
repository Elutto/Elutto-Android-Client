package com.elutto.app.event

import org.threeten.bp.LocalDateTime

enum class IoEventContext(val kind: Int) {
    CHANNELS_SYNC(0x1)
}
