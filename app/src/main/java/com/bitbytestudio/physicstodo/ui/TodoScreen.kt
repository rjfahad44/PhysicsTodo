package com.bitbytestudio.physicstodo.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bitbytestudio.physicstodo.ui.models.ParticleAnimation
import com.bitbytestudio.physicstodo.ui.models.ParticlePhase
import com.bitbytestudio.physicstodo.ui.models.TaskTextLayout
import kotlinx.coroutines.isActive

@Composable
fun TodoScreen(
    viewModel: TodoViewModel = viewModel()
) {

    val todos by viewModel.todos.collectAsStateWithLifecycle()

    val density = LocalDensity.current

    val context = LocalContext.current

    val particleController = remember {
        ParticleController()
    }

    val motionSensor = remember {
        MotionSensor(context)
    }

    /*
     * taskId -> latest exact text layout
     */
    val textLayouts = remember {
        mutableStateMapOf<Long, TaskTextLayout>()
    }

    var rootSize by remember {
        mutableStateOf(IntSize.Zero)
    }

    val fontSize = 17.sp

    val fontSizePx = with(density) {
        fontSize.toPx()
    }

    val floorPadding = with(density) {
        42.dp.toPx()
    }

    /*
     * Start / stop accelerometer.
     */
    DisposableEffect(Unit) {

        motionSensor.start()

        onDispose {
            motionSensor.stop()
        }
    }

    /*
     * Keep latest values available inside
     * the single frame loop.
     */
    val latestRootSize by rememberUpdatedState(rootSize)

    val latestTiltX by rememberUpdatedState(motionSensor.tiltX)

    val latestTiltY by rememberUpdatedState(motionSensor.tiltY)

    /*
     * GLOBAL PHYSICS LOOP
     *
     * Important:
     * There is only ONE frame loop for all particles.
     */
    LaunchedEffect(Unit) {

        var lastFrameNanos = 0L

        while (isActive) {

            val now = withFrameNanos {
                it
            }

            if (lastFrameNanos == 0L) {

                lastFrameNanos = now

                continue
            }

            val dt = (now - lastFrameNanos) / 1_000_000_000f

            lastFrameNanos = now

            /*
             * Protect physics from huge dt
             * after app pause/background.
             */
            val safeDt = dt.coerceIn(
                0.001f, 0.032f
            )

            val size = latestRootSize

            if (size.width > 0 && size.height > 0) {

                particleController.updateAll(
                    dt = safeDt,
                    tiltX = latestTiltX,
                    tiltY = latestTiltY,
                    screenWidth = size.width.toFloat(),
                    screenHeight = size.height.toFloat(),
                    floorPadding = floorPadding,
                    fontSizePx = fontSizePx
                )

                val finishedAnimations =
                    particleController.animations.values.filter { animation ->
                        animation.phase == ParticlePhase.RETURNING &&
                            animation.isReturnFinished()
                    }

                finishedAnimations.forEach { animation ->

                    particleController.remove(
                        animation.taskId
                    )
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.background
            )
            .onGloballyPositioned { coordinates ->

                rootSize = coordinates.size
            }) {

        /*
         * REAL TODO LIST
         */
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),

            contentPadding = PaddingValues(
                    horizontal = 16.dp, vertical = 24.dp
                ),

            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            item {

                Text(
                    text = "My Tasks",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(16.dp)
                )
            }

            items(
                items = todos, key = { todo ->
                    todo.id
                }) { todo ->

                val hasParticleAnimation = particleController.hasAnimation(todo.id)

                TodoItem(
                    todo = todo,

                    fontSize = fontSize,

                    hideOriginalText = hasParticleAnimation,

                    onLayoutChanged = { layout ->

                        textLayouts[todo.id] = layout
                    },

                    onToggle = {

                        if (!todo.completed) {

                            /*
                             * COMPLETE
                             *
                             * First create the visual duplicate
                             * from the EXACT text layout.
                             */
                            val layout = textLayouts[todo.id]

                            if (layout != null && rootSize.width > 0 && rootSize.height > 0) {

                                particleController.startComplete(
                                        taskId = todo.id,

                                        text = todo.text,

                                        layout = layout,

                                        fontSizePx = fontSizePx
                                    )
                            }

                            /*
                             * Now update real Todo state.
                             */
                            viewModel.toggleTodo(
                                todo.id
                            )

                        } else {

                            /*
                             * UNDO
                             *
                             * Keep the real Text hidden.
                             *
                             * Move particles from bottom
                             * back to the CURRENT exact
                             * text position.
                             */
                            val layout = textLayouts[todo.id]

                            if (layout != null) {

                                particleController.startReturn(
                                        taskId = todo.id,

                                        latestLayout = layout
                                    )
                            }

                            /*
                             * Real text stays invisible because
                             * particle animation still exists.
                             */
                            viewModel.toggleTodo(
                                todo.id
                            )
                        }
                    })
            }
        }

        /*
         * GLOBAL PARTICLE LAYER
         *
         * This is above LazyColumn.
         *
         * Therefore particles are NOT children of
         * individual LazyColumn rows.
         */
        ParticleOverlay(
            animations = particleController.animations.values.toList()
        )
    }
}


@Composable
private fun ParticleOverlay(
    animations: List<ParticleAnimation>
) {

    if (animations.isEmpty()) {
        return
    }

    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {

        val paint = Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {

            isSubpixelText = true

            typeface = Typeface.create(
                Typeface.DEFAULT, Typeface.NORMAL
            )

            textSize = 17.sp.toPx()

            color = Color(
                0xFF202124
            ).toArgb()
        }

        animations.forEach { animation ->

            animation.particles.forEach { particle ->

                /*
                 * Spaces don't need to draw.
                 *
                 * But we KEEP the space particle,
                 * because its original position is
                 * still required for exact reconstruction.
                 */
                if (particle.character == ' ' || particle.character == '\n' || particle.character == '\t') {
                    return@forEach
                }

                /*
                 * Draw each character individually.
                 */
                drawContext.canvas.nativeCanvas.save()

                drawContext.canvas.nativeCanvas.rotate(
                        particle.rotation * 57.29578f,

                        particle.x,

                        particle.y
                    )

                drawContext.canvas.nativeCanvas.drawText(
                        particle.character.toString(),

                        particle.x,

                        particle.y,

                        paint
                    )

                drawContext.canvas.nativeCanvas.restore()
            }
        }
    }
}
