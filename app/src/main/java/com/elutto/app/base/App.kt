package com.elutto.app.base

import android.app.Application
import android.support.text.emoji.EmojiCompat
import android.support.text.emoji.bundled.BundledEmojiCompatConfig



class App : Application() {
    companion object {
        lateinit var elutto: Elutto
            private set
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize Emoji support
        val emojiConfig = BundledEmojiCompatConfig(this)
        EmojiCompat.init(emojiConfig)

        elutto = Elutto(this)
    }
}