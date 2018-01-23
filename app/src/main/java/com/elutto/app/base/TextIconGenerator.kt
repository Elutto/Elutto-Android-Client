package com.elutto.app.base

import android.graphics.drawable.Drawable
import com.amulyakhare.textdrawable.TextDrawable
import com.amulyakhare.textdrawable.util.ColorGenerator

// Takes care of generating colored icons with text.
class TextIconGenerator
{
    private val colors = ColorGenerator.MATERIAL // or use DEFAULT

    fun getIconRound(text: String, colorKey: String): Drawable {
        val drawable = TextDrawable.builder().round()
        val color = colors.getColor(colorKey)
        return drawable.build(text, color)
    }
}