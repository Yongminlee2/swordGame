package com.geomgang.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 원작과 같은 5초. 실수로 지워지지 않게 하려는 장치이므로 짧게 만들지 않는다. */
const val RESET_HOLD_MILLIS: Long = 5_000

private const val STEP_MILLIS: Long = 50

/**
 * 5초 동안 누르고 있어야 발동하는 초기화 버튼.
 *
 * 누르는 동안 막대가 차오르고, 손을 떼면 즉시 취소된다.
 * 세이브를 지우는 조작이라 실수로 눌리면 안 되기 때문에 일반 버튼으로 만들지 않았다.
 */
@Composable
fun HoldToReset(
    label: String,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var progress by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF2A2340))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        val job = scope.launch {
                            var elapsed = 0L
                            while (elapsed < RESET_HOLD_MILLIS) {
                                delay(STEP_MILLIS)
                                elapsed += STEP_MILLIS
                                progress = elapsed.toFloat() / RESET_HOLD_MILLIS
                            }
                            onComplete()
                            progress = 0f
                        }
                        try {
                            awaitRelease()
                        } finally {
                            job.cancel()
                            progress = 0f
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        // 차오르는 막대
        Box(
            Modifier
                .fillMaxWidth(progress)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.55f)),
        )
        Text(
            text = if (progress > 0f) "계속 누르고 있으면 초기화된다" else label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
        )
    }
}
