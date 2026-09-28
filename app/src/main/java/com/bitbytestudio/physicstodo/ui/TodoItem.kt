package com.bitbytestudio.physicstodo.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bitbytestudio.physicstodo.ui.models.CharacterAnchor
import com.bitbytestudio.physicstodo.ui.models.TaskTextLayout

@Composable
fun TodoItem(
    todo: Todo,
    fontSize: androidx.compose.ui.unit.TextUnit,
    hideOriginalText: Boolean,
    onLayoutChanged: (TaskTextLayout) -> Unit,
    onToggle: () -> Unit
) {

    var textLayoutResult by remember {
        mutableStateOf<TextLayoutResult?>(null)
    }

    var textRootPosition by remember {
        mutableStateOf(Offset.Zero)
    }

    /*
     * Whenever text layout or position changes,
     * create exact character anchors.
     */
    fun publishLayout() {
        val result = textLayoutResult ?: return
        if (result.layoutInput.text.text.isEmpty()) {
            return
        }

        val anchors = buildList {
            for (index in result.layoutInput.text.text.indices) {

                val character = result.layoutInput.text.text[index]
                val rect = result.getBoundingBox(index)
                val line = result.getLineForOffset(index)
                val baseline = result.getLineBaseline(line)

                add(
                    CharacterAnchor(
                        character = character,
                        x = textRootPosition.x + rect.left,
                        baselineY = textRootPosition.y + baseline,
                        width = rect.width,
                        height = rect.height
                    )
                )
            }
        }

        onLayoutChanged(
            TaskTextLayout(
                taskId = todo.id,
                text = todo.text,
                characters = anchors
            )
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(16.dp)
            ),
        tonalElevation = 2.dp
    ) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onToggle()
            }
            .padding(
                horizontal = 16.dp, vertical = 14.dp
            ),
            verticalAlignment = Alignment.CenterVertically) {

            Checkbox(
                checked = todo.completed,
                onCheckedChange = {
                    onToggle()
                })

            Spacer(
                modifier = Modifier.padding(
                    horizontal = 6.dp
                )
            )

            /*
             * IMPORTANT
             *
             * We DO NOT remove this Text.
             *
             * We only change alpha to 0.
             *
             * This preserves its exact layout position.
             */
            Text(
                text = todo.text,
                modifier = Modifier
                    .weight(1f)
                    .alpha(
                        if (hideOriginalText) {
                            0f
                        } else {
                            1f
                        }
                    )
                    .onGloballyPositioned { coordinates ->
                        textRootPosition = coordinates.positionInRoot()
                        /*
                         * Position changed due to:
                         *
                         * - scrolling
                         * - recomposition
                         * - layout
                         *
                         * Update current exact location.
                         */
                        publishLayout()
                    },

                fontSize = fontSize,
                fontWeight = FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { result ->
                    textLayoutResult = result
                    /*
                     * onGloballyPositioned may already
                     * have a valid position.
                     */
                    publishLayout()
                })
        }
    }
}