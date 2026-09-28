package com.bitbytestudio.physicstodo.ui

import androidx.compose.runtime.mutableStateMapOf
import com.bitbytestudio.physicstodo.ui.models.ParticleAnimation
import com.bitbytestudio.physicstodo.ui.models.ParticlePhase
import com.bitbytestudio.physicstodo.ui.models.TaskTextLayout
import com.bitbytestudio.physicstodo.ui.models.TextParticle
import kotlin.math.abs
import kotlin.math.sqrt

class ParticleController {

    val animations =
        mutableStateMapOf<Long, ParticleAnimation>()

    fun startComplete(
        taskId: Long,
        text: String,
        layout: TaskTextLayout,
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

    /**
     * Updates physics for all animations together so that particles from different tasks
     * interact, collide, and stack into a realistic sand heap / sand pillar on the floor.
     */
    fun updateAll(
        dt: Float,
        tiltX: Float,
        tiltY: Float,
        screenWidth: Float,
        screenHeight: Float,
        floorPadding: Float,
        fontSizePx: Float
    ) {

        val animationList = animations.values.toList()

        if (animationList.isEmpty()) {
            return
        }

        // 1. Update individual animation phases (Floating, Returning)
        animationList.forEach { animation ->

            animation.updateSingleTask(dt, tiltX, tiltY)
        }

        // 2. Gather active falling or resting non-whitespace particles
        val physicsParticles = mutableListOf<TextParticle>()

        animationList.forEach { animation ->

            if (
                animation.phase == ParticlePhase.FALLING ||
                animation.phase == ParticlePhase.RESTING
            ) {

                animation.particles.forEach { particle ->

                    if (!particle.isWhitespace) {

                        physicsParticles.add(particle)
                    }
                }
            }
        }

        if (physicsParticles.isEmpty()) {
            return
        }

        // Quick check: if ALL particles are completely stationary (vx=0, vy=0, w=0) AND tilt is near 0,
        // skip physics calculation to save battery & CPU resources
        val isAnyMoving = physicsParticles.any { p ->
            p.velocityX != 0f || p.velocityY != 0f || p.angularVelocity != 0f
        }

        val tiltMagSq = tiltX * tiltX + tiltY * tiltY

        if (!isAnyMoving && tiltMagSq < 0.08f) {
            return
        }

        val baseGravity = 2200f

        val gx = tiltX * 1400f

        val gy = baseGravity + tiltY * 500f

        val floorY =
            (
                screenHeight -
                    floorPadding -
                    fontSizePx * 0.2f
                ).coerceAtLeast(fontSizePx)

        // Substep simulation (8 substeps) for smooth, high-accuracy rigid body sand physics
        val subSteps = 8

        val subDt = dt / subSteps

        for (step in 0 until subSteps) {

            // Step 1: Apply gravity, tilt, and air resistance damping
            for (i in physicsParticles.indices) {

                val p = physicsParticles[i]

                p.velocityX += gx * subDt

                p.velocityY += gy * subDt

                // Smooth air drag
                p.velocityX *= (1f - 1.2f * subDt)

                p.velocityY *= (1f - 1.2f * subDt)

                p.x += p.velocityX * subDt

                p.y += p.velocityY * subDt

                p.rotation += p.angularVelocity * subDt

                // Angular friction
                p.angularVelocity *= (1f - 12.0f * subDt)
            }

            // Step 2: Particle-particle collisions & sand heap stacking
            val count = physicsParticles.size

            for (i in 0 until count) {

                val p1 = physicsParticles[i]

                val c1x = p1.getCenterX()

                val c1y = p1.getCenterY()

                val r1 = p1.radius

                for (j in i + 1 until count) {

                    val p2 = physicsParticles[j]

                    val c2x = p2.getCenterX()

                    val c2y = p2.getCenterY()

                    val r2 = p2.radius

                    var dx = c2x - c1x

                    var dy = c2y - c1y

                    val distSq = dx * dx + dy * dy

                    val minDist = r1 + r2

                    if (distSq < minDist * minDist) {

                        var dist = sqrt(distSq)

                        if (dist < 0.001f) {

                            dx = if ((p1.id + p2.id) % 2 == 0) 0.1f else -0.1f

                            dy = -0.1f

                            dist = sqrt(dx * dx + dy * dy)
                        }

                        val overlap = minDist - dist

                        var nx = dx / dist

                        var ny = dy / dist

                        // Slide off shoulders horizontally if vertically stacked
                        if (
                            abs(nx) < 0.3f &&
                            (abs(p1.velocityY) > 10f || abs(p2.velocityY) > 10f)
                        ) {

                            val slipSign = if (p1.id % 2 == 0) 1f else -1f

                            nx += slipSign * 0.2f

                            val len = sqrt(nx * nx + ny * ny)

                            nx /= len

                            ny /= len
                        }

                        // Gentle Baumgarte position separation (prevents spring explosions / jitter)
                        val separationFactor = 0.45f

                        val sep = overlap * separationFactor

                        p1.setCenter(
                            c1x - nx * sep,
                            c1y - ny * sep
                        )

                        p2.setCenter(
                            c2x + nx * sep,
                            c2y + ny * sep
                        )

                        // Velocity impulse resolution
                        val rvx = p2.velocityX - p1.velocityX

                        val rvy = p2.velocityY - p1.velocityY

                        val velAlongNormal = rvx * nx + rvy * ny

                        if (velAlongNormal < 0f) {

                            // Restitution e = 0.12f (inelastic sand bounce)
                            val e = 0.12f

                            val impulse = -(1f + e) * velAlongNormal * 0.5f

                            val impX = impulse * nx

                            val impY = impulse * ny

                            p1.velocityX -= impX

                            p1.velocityY -= impY

                            p2.velocityX += impX

                            p2.velocityY += impY

                            // Tangential friction
                            val tx = -ny

                            val ty = nx

                            val velAlongTangent = rvx * tx + rvy * ty

                            val frictionImpulse = -velAlongTangent * 0.35f

                            p1.velocityX -= frictionImpulse * tx

                            p1.velocityY -= frictionImpulse * ty

                            p2.velocityX += frictionImpulse * tx

                            p2.velocityY += frictionImpulse * ty

                            p1.angularVelocity -= frictionImpulse * 0.004f

                            p2.angularVelocity += frictionImpulse * 0.004f

                            p1.angularVelocity = p1.angularVelocity.coerceIn(-3f, 3f)

                            p2.angularVelocity = p2.angularVelocity.coerceIn(-3f, 3f)
                        }
                    }
                }
            }

            // Step 3: Floor & Wall boundary constraints
            for (i in physicsParticles.indices) {

                val p = physicsParticles[i]

                // Floor constraint
                if (p.y > floorY) {

                    p.y = floorY

                    if (p.velocityY > 0f) {

                        if (p.velocityY > 25f) {

                            p.velocityY = -p.velocityY * 0.15f

                        } else {

                            p.velocityY = 0f
                        }
                    }

                    // Strong floor friction
                    p.velocityX *= (1f - 25f * subDt)

                    p.angularVelocity *= (1f - 25f * subDt)
                }

                // Left wall constraint
                if (p.x < 8f) {

                    p.x = 8f

                    if (p.velocityX < 0f) {

                        p.velocityX = -p.velocityX * 0.2f
                    }
                }

                // Right wall constraint
                if (p.x > screenWidth - p.width - 8f) {

                    p.x = (screenWidth - p.width - 8f).coerceAtLeast(8f)

                    if (p.velocityX > 0f) {

                        p.velocityX = -p.velocityX * 0.2f
                    }
                }

                // Step 4: Natural Settling & Smooth Motion Decay
                val speedSq = p.velocityX * p.velocityX + p.velocityY * p.velocityY

                val angSpeed = abs(p.angularVelocity)

                if (speedSq < 300f && angSpeed < 1.0f) {

                    // Exponential smooth decay when motion is slow near bottom/stack
                    p.velocityX *= (1f - 20f * subDt)

                    p.velocityY *= (1f - 20f * subDt)

                    p.angularVelocity *= (1f - 20f * subDt)

                    p.rotation *= (1f - 10f * subDt)

                    // Complete zero-snap when speed is negligible (< 0.5 px/s)
                    if (speedSq < 0.25f && angSpeed < 0.05f) {

                        p.velocityX = 0f

                        p.velocityY = 0f

                        p.angularVelocity = 0f
                    }
                }
            }
        }

        // Update animation phase to RESTING if all particles in that task are static
        animationList.forEach { animation ->

            if (animation.phase == ParticlePhase.FALLING) {

                val nonWhitespace = animation.particles.filter { !it.isWhitespace }

                val allStatic = nonWhitespace.all { p ->

                    p.velocityX == 0f && p.velocityY == 0f && p.angularVelocity == 0f
                }

                if (allStatic && nonWhitespace.isNotEmpty()) {

                    animation.phase = ParticlePhase.RESTING
                }
            }
        }
    }
}
