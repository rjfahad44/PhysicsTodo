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
    initialReturnY: Float,

    val targetX: Float,
    val targetY: Float
) {

    var x by mutableFloatStateOf(initialX)

    var y by mutableFloatStateOf(initialY)

    var velocityX by mutableFloatStateOf(0f)

    var velocityY by mutableFloatStateOf(0f)

    var rotation by mutableFloatStateOf(0f)

    var angularVelocity by mutableFloatStateOf(0f)

    var returnX by mutableFloatStateOf(initialReturnX)

    var returnY by mutableFloatStateOf(initialReturnY)
}