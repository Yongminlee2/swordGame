package com.geomgang.game

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.geomgang.game.ui.ForgeRed
import com.geomgang.game.ui.PixelActionButton
import com.geomgang.game.ui.SwordForgeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

/** 강화 버튼의 결과 번쩍임이 실제로 그려지고, 그동안에도 입력을 받는지 확인한다. */
class PixelActionButtonDeviceTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun feedbackChangesButtonAndDoesNotLockClicks() {
        var feedback by mutableFloatStateOf(0f)
        var clicks = 0
        compose.setContent {
            SwordForgeTheme {
                PixelActionButton(
                    onClick = { clicks++ },
                    enabled = true,
                    feedback = feedback,
                    feedbackColor = ForgeRed,
                    modifier = Modifier
                        .width(320.dp)
                        .height(64.dp)
                        .testTag(TAG),
                ) {
                    Text("강화하기", color = Color.Black)
                }
            }
        }

        val node = compose.onNodeWithTag(TAG)
        val normal = node.captureToImage().toPixelMap()
        val sampleX = normal.width / 4
        val sampleY = normal.height / 2

        compose.runOnIdle { feedback = 1f }
        val flashed = node.captureToImage().toPixelMap()
        assertNotEquals(normal[sampleX, sampleY], flashed[sampleX, sampleY])

        node.performClick()
        node.performClick()
        compose.runOnIdle { assertEquals(2, clicks) }
    }

    private companion object {
        const val TAG = "forge-action"
    }
}
