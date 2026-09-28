package com.bitbytestudio.physicstodo.ui.models

import androidx.compose.runtime.Stable

enum class ParticlePhase {
    FLOATING,
    FALLING,
    RESTING,
    RETURNING
}

@Stable
data class CharacterAnchor(
    val character: Char,
    val x: Float,
    val baselineY: Float,
    val width: Float,
    val height: Float
)

@Stable
data class TaskTextLayout(
    val taskId: Long,
    val text: String,
    val characters: List<CharacterAnchor>
)
