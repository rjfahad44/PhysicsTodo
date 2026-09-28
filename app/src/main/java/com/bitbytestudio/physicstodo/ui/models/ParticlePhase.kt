package com.bitbytestudio.physicstodo.ui.models

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs
import kotlin.math.exp
import kotlin.random.Random

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

@Stable
class ParticleAnimation(
    val taskId: Long,
    val text: String,
    layout: TaskTextLayout,
    screenWidth: Float,
    screenHeight: Float,
    floorPadding: Float,
    fontSizePx: Float
) {

    companion object {

        private const val FLOAT_DURATION = 0.42f

        private const val REST_AFTER = 2.5f

        private const val GRAVITY = 1450f

        private const val TILT_FORCE_X = 260f

        private const val TILT_FORCE_Y = 120f

        private const val BOUNCE = 0.18f

        private const val FLOOR_FRICTION = 0.82f

        private const val SPRING = 135f

        private const val RETURN_DAMPING = 8.5f

        private const val ROTATION_SPRING = 20f

        private const val ROTATION_DAMPING = 7f
    }

    var phase by mutableStateOf(
        ParticlePhase.FLOATING
    )

    var elapsedSeconds by mutableStateOf(0f)

    val particles = mutableStateListOf<TextParticle>()

    init {

        val random = Random(taskId)

        val safeWidth = screenWidth.coerceAtLeast(1f)

        val floorY =
            (screenHeight - floorPadding - fontSizePx)
                .coerceAtLeast(fontSizePx)

        layout.characters.forEachIndexed { index, anchor ->

            val targetX = (
                24f +
                    random.nextFloat() *
                    (safeWidth - 48f)
                        .coerceAtLeast(1f)
            )

            val targetY =
                floorY -
                    random.nextFloat() *
                    (fontSizePx * 0.55f)

            val particle = TextParticle(
                id = index,
                character = anchor.character,

                initialX = anchor.x,
                initialY = anchor.baselineY,

                width = anchor.width.coerceAtLeast(1f),
                height = anchor.height.coerceAtLeast(fontSizePx * 0.7f),

                initialReturnX = anchor.x,
                initialReturnY = anchor.baselineY,

                targetX = targetX,
                targetY = targetY
            )

            /*
             * Initial breakup velocity.
             *
             * Character starts exactly from original text position,
             * then separates.
             */
            particle.velocityX =
                (random.nextFloat() - 0.5f) * 150f

            particle.velocityY =
                -40f -
                    random.nextFloat() * 100f

            particle.angularVelocity =
                (random.nextFloat() - 0.5f) * 5f

            particle.rotation =
                (random.nextFloat() - 0.5f) * 0.12f

            particles += particle
        }
    }

    fun restartFloating() {

        phase = ParticlePhase.FLOATING
        elapsedSeconds = 0f

        particles.forEach { particle ->

            particle.velocityX *= 0.25f

            particle.velocityY =
                -40f

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

    fun update(
        dt: Float,
        tiltX: Float,
        tiltY: Float,
        screenWidth: Float,
        screenHeight: Float,
        floorPadding: Float,
        fontSizePx: Float
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

                    phase =
                        ParticlePhase.FALLING
                }
            }

            ParticlePhase.FALLING -> {

                updateFalling(
                    dt = dt,
                    tiltX = tiltX,
                    tiltY = tiltY,
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                    floorPadding = floorPadding,
                    fontSizePx = fontSizePx
                )

                if (elapsedSeconds >= REST_AFTER) {

                    settleParticles()
                    phase = ParticlePhase.RESTING
                }
            }

            ParticlePhase.RESTING -> {
                // No physics needed.
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

            /*
             * Zero gravity phase.
             *
             * Only velocity + device movement.
             */
            particle.velocityX +=
                tiltX * TILT_FORCE_X * dt

            particle.velocityY +=
                tiltY * TILT_FORCE_Y * dt

            particle.velocityX *=
                exp(-1.7f * dt)

            particle.velocityY *=
                exp(-1.7f * dt)

            particle.x +=
                particle.velocityX * dt

            particle.y +=
                particle.velocityY * dt

            particle.rotation +=
                particle.angularVelocity * dt
        }
    }

    private fun updateFalling(
        dt: Float,
        tiltX: Float,
        tiltY: Float,
        screenWidth: Float,
        screenHeight: Float,
        floorPadding: Float,
        fontSizePx: Float
    ) {

        val floorY =
            (
                screenHeight -
                    floorPadding -
                    fontSizePx
                ).coerceAtLeast(fontSizePx)

        particles.forEach { particle ->

            /*
             * Gravity.
             */
            particle.velocityY +=
                GRAVITY * dt

            /*
             * Device tilt adds a little sideways force.
             */
            particle.velocityX +=
                tiltX * TILT_FORCE_X * dt

            particle.velocityY +=
                tiltY * TILT_FORCE_Y * dt

            particle.x +=
                particle.velocityX * dt

            particle.y +=
                particle.velocityY * dt

            particle.rotation +=
                particle.angularVelocity * dt

            /*
             * Left/right boundary.
             */
            if (particle.x < 8f) {

                particle.x = 8f

                particle.velocityX =
                    abs(particle.velocityX) * 0.35f
            }

            if (particle.x > screenWidth - 8f) {

                particle.x =
                    (screenWidth - 8f).coerceAtLeast(8f)

                particle.velocityX =
                    -abs(particle.velocityX) * 0.35f
            }

            /*
             * Floor collision.
             */
            if (particle.y >= floorY) {

                particle.y = floorY

                if (abs(particle.velocityY) > 35f) {

                    particle.velocityY =
                        -particle.velocityY * BOUNCE

                } else {

                    particle.velocityY = 0f
                }

                particle.velocityX *=
                    FLOOR_FRICTION

                particle.angularVelocity *=
                    0.82f
            }
        }
    }

    private fun settleParticles() {

        particles.forEach { particle ->

            particle.x =
                particle.targetX

            particle.y =
                particle.targetY

            particle.velocityX = 0f
            particle.velocityY = 0f
            particle.angularVelocity = 0f
        }
    }

    private fun updateReturning(
        dt: Float
    ) {

        particles.forEach { particle ->

            val dx =
                particle.returnX - particle.x

            val dy =
                particle.returnY - particle.y

            /*
             * Spring force.
             */
            particle.velocityX +=
                dx * SPRING * dt

            particle.velocityY +=
                dy * SPRING * dt

            /*
             * Frame-rate independent damping.
             */
            val damping =
                exp(-RETURN_DAMPING * dt)

            particle.velocityX *= damping
            particle.velocityY *= damping

            /*
             * Position.
             */
            particle.x +=
                particle.velocityX * dt

            particle.y +=
                particle.velocityY * dt

            /*
             * Rotation spring.
             */
            particle.angularVelocity +=
                -particle.rotation *
                    ROTATION_SPRING *
                    dt

            particle.angularVelocity *=
                exp(-ROTATION_DAMPING * dt)

            particle.rotation +=
                particle.angularVelocity * dt
        }
    }

    private fun isReturnFinished(): Boolean {

        return particles.all { particle ->

            val dx =
                particle.returnX - particle.x

            val dy =
                particle.returnY - particle.y

            val distanceSquared =
                dx * dx + dy * dy

            distanceSquared < 4f &&
                abs(particle.velocityX) < 8f &&
                abs(particle.velocityY) < 8f
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