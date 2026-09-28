package com.bitbytestudio.physicstodo.ui.models

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.exp
import kotlin.random.Random

@Stable
class ParticleAnimation(
    val taskId: Long,
    val text: String,
    layout: TaskTextLayout,
    fontSizePx: Float
) {

    companion object {

        private const val FLOAT_DURATION = 0.12f

        private const val TILT_FORCE_X = 1400f

        private const val TILT_FORCE_Y = 500f
    }

    var phase by mutableStateOf(
        ParticlePhase.FLOATING
    )

    var elapsedSeconds by mutableStateOf(0f)

    val particles = mutableStateListOf<TextParticle>()

    init {

        val random = Random(taskId)

        layout.characters.forEachIndexed { index, anchor ->

            val particle = TextParticle(
                id = index,
                character = anchor.character,

                initialX = anchor.x,
                initialY = anchor.baselineY,

                width = anchor.width.coerceAtLeast(1f),
                height = anchor.height.coerceAtLeast(fontSizePx * 0.7f),

                initialReturnX = anchor.x,
                initialReturnY = anchor.baselineY
            )

            /*
             * Initial breakup velocity.
             * Characters start exactly from original text position,
             * then separate slightly before falling.
             */
            particle.velocityX =
                (random.nextFloat() - 0.5f) * 100f

            particle.velocityY =
                -20f - random.nextFloat() * 60f

            particle.angularVelocity =
                (random.nextFloat() - 0.5f) * 3f

            particle.rotation =
                (random.nextFloat() - 0.5f) * 0.08f

            particles += particle
        }
    }

    fun restartFloating() {

        phase = ParticlePhase.FLOATING
        elapsedSeconds = 0f

        particles.forEach { particle ->

            particle.velocityX *= 0.25f

            particle.velocityY = -20f

            particle.angularVelocity *= 0.4f
        }
    }

    fun startReturning(
        latestLayout: TaskTextLayout
    ) {

        val anchors = latestLayout.characters

        particles.forEachIndexed { index, particle ->

            val anchor = anchors.getOrNull(index)

            if (anchor != null) {

                particle.returnX = anchor.x
                particle.returnY = anchor.baselineY
            }
        }

        phase = ParticlePhase.RETURNING

        elapsedSeconds = 0f
    }

    fun updateSingleTask(
        dt: Float,
        tiltX: Float,
        tiltY: Float
    ) {

        if (particles.isEmpty()) {
            return
        }

        elapsedSeconds += dt

        when (phase) {

            ParticlePhase.FLOATING -> {

                updateFloating(
                    dt = dt,
                    tiltX = tiltX,
                    tiltY = tiltY
                )

                if (elapsedSeconds >= FLOAT_DURATION) {

                    phase = ParticlePhase.FALLING
                }
            }

            ParticlePhase.FALLING, ParticlePhase.RESTING -> {
                // Multi-particle physics (gravity, collisions, sand heap stacking)
                // are driven globally in ParticleController.updateAll.
            }

            ParticlePhase.RETURNING -> {

                updateReturning(dt)

                if (isReturnFinished()) {

                    snapToReturnPositions()
                }
            }
        }
    }

    private fun updateFloating(
        dt: Float,
        tiltX: Float,
        tiltY: Float
    ) {

        particles.forEach { particle ->

            particle.velocityX +=
                tiltX * TILT_FORCE_X * dt

            particle.velocityY +=
                tiltY * TILT_FORCE_Y * dt

            particle.velocityX *=
                exp(-2.0f * dt)

            particle.velocityY *=
                exp(-2.0f * dt)

            particle.x +=
                particle.velocityX * dt

            particle.y +=
                particle.velocityY * dt

            particle.rotation +=
                particle.angularVelocity * dt
        }
    }

    private fun updateReturning(
        dt: Float
    ) {

        // Direct exponential interpolation towards return position.
        // Travels strictly along the straight line from bottom to row without any upward overshoot!
        val speed = 15.0f

        val factor =
            (1f - exp(-speed * dt)).coerceIn(
                0f, 1f
            )

        particles.forEach { particle ->

            particle.x +=
                (particle.returnX - particle.x) * factor

            particle.y +=
                (particle.returnY - particle.y) * factor

            particle.rotation +=
                (0f - particle.rotation) * factor

            particle.velocityX = 0f
            particle.velocityY = 0f
            particle.angularVelocity = 0f
        }
    }

    fun isReturnFinished(): Boolean {

        return particles.all { particle ->

            val dx =
                particle.returnX - particle.x

            val dy =
                particle.returnY - particle.y

            (dx * dx + dy * dy) < 2f
        }
    }

    private fun snapToReturnPositions() {

        particles.forEach { particle ->

            particle.x =
                particle.returnX

            particle.y =
                particle.returnY

            particle.velocityX = 0f
            particle.velocityY = 0f
            particle.rotation = 0f
            particle.angularVelocity = 0f
        }
    }
}
