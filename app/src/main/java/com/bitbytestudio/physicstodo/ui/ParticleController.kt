package com.bitbytestudio.physicstodo.ui

import androidx.compose.runtime.mutableStateMapOf
import com.bitbytestudio.physicstodo.ui.models.ParticleAnimation
import com.bitbytestudio.physicstodo.ui.models.TaskTextLayout

class ParticleController {

    val animations =
        mutableStateMapOf<Long, ParticleAnimation>()

    fun startComplete(
        taskId: Long,
        text: String,
        layout: TaskTextLayout,
        screenWidth: Float,
        screenHeight: Float,
        floorPadding: Float,
        fontSizePx: Float
    ) {

        val existing =
            animations[taskId]

        if (existing != null) {

            existing.restartFloating()

            return
        }

        val animation =
            ParticleAnimation(
                taskId = taskId,
                text = text,
                layout = layout,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                floorPadding = floorPadding,
                fontSizePx = fontSizePx
            )

        animations[taskId] = animation
    }

    fun startReturn(
        taskId: Long,
        latestLayout: TaskTextLayout
    ) {

        animations[taskId]
            ?.startReturning(latestLayout)
    }

    fun remove(
        taskId: Long
    ) {

        animations.remove(taskId)
    }

    fun hasAnimation(
        taskId: Long
    ): Boolean {

        return animations.containsKey(taskId)
    }
}