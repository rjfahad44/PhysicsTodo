package com.bitbytestudio.physicstodo.ui.models

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue

class TextParticle(
    val id: Int,
    val character: Char,
    initialX: Float,
    initialY: Float,
    val width: Float,
    val height: Float,
    initialReturnX: Float,
    initialReturnY: Float
) {
    var x by mutableFloatStateOf(initialX)
    var y by mutableFloatStateOf(initialY)
    var velocityX by mutableFloatStateOf(0f)
    var velocityY by mutableFloatStateOf(0f)
    var rotation by mutableFloatStateOf(0f)
    var angularVelocity by mutableFloatStateOf(0f)
    var returnX by mutableFloatStateOf(initialReturnX)
    var returnY by mutableFloatStateOf(initialReturnY)
    val isWhitespace: Boolean = character.isWhitespace()
    val radius: Float = (maxOf(width, height) * 0.45f).coerceIn(10f, 28f)
    fun getCenterX(): Float = x + width * 0.5f
    fun getCenterY(): Float = y - height * 0.35f
    fun setCenter(cx: Float, cy: Float) {
        x = cx - width * 0.5f
        y = cy + height * 0.35f
    }
}
