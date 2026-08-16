package com.geomgang.game

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.geomgang.core.Zone
import com.geomgang.game.ui.MonsterSheet
import com.geomgang.game.ui.MonsterSprite
import com.geomgang.game.ui.SwordForgeTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** 모든 전투 몬스터의 원본 셀과 실제 표시 프레임이 잘리지 않는지 실기기에서 확인한다. */
class MonsterArtworkDeviceTest {

    @get:Rule
    val compose = createComposeRule()

    private val allNames = Zone.entries.flatMap { zone ->
        zone.monsters.map { it.name } + zone.bossName
    }

    @Test
    fun everySourceCellContainsTheWholeArtworkInsideItsBounds() {
        val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
        val options = BitmapFactory.Options().apply { inScaled = false }
        val sheets = hashMapOf<Int, android.graphics.Bitmap>()

        allNames.forEach { name ->
            val source = MonsterSheet.combatSourceOf(name)
            val bitmap = sheets.getOrPut(source.drawable) {
                BitmapFactory.decodeResource(resources, source.drawable, options)
            }
            var minX = MonsterSheet.COMBAT_CELL
            var minY = MonsterSheet.COMBAT_CELL
            var maxX = -1
            var maxY = -1
            repeat(MonsterSheet.COMBAT_CELL) { y ->
                repeat(MonsterSheet.COMBAT_CELL) { x ->
                    val alpha = bitmap.getPixel(source.offset.x + x, source.offset.y + y) ushr 24
                    if (alpha > 8) {
                        minX = minOf(minX, x)
                        minY = minOf(minY, y)
                        maxX = maxOf(maxX, x)
                        maxY = maxOf(maxY, y)
                    }
                }
            }
            assertTrue("$name 원본이 비어 있다", maxX >= minX && maxY >= minY)
            assertTrue(
                "$name 원본이 셀 경계에 닿는다: $minX,$minY..$maxX,$maxY",
                minX >= 8 && minY >= 8 &&
                    maxX < MonsterSheet.COMBAT_CELL - 8 &&
                    maxY < MonsterSheet.COMBAT_CELL - 8,
            )
        }
    }

    @Test
    fun everyMonsterRendersWithAVisibleFrameOnAllFourSides() {
        val current = mutableStateOf(allNames.first())
        val bosses = Zone.entries.map { it.bossName }.toSet()
        val frame = Color(0xFF010203)

        compose.setContent {
            SwordForgeTheme {
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .background(frame)
                        .testTag("monster-frame"),
                    contentAlignment = Alignment.Center,
                ) {
                    val name = current.value
                    MonsterSprite(
                        name = name,
                        hpRatio = 1f,
                        isBoss = name in bosses,
                        isRare = false,
                        enraged = false,
                        hitSeq = 0,
                    )
                }
            }
        }

        val expected = android.graphics.Color.rgb(1, 2, 3)
        allNames.forEach { name ->
            compose.runOnIdle { current.value = name }
            compose.waitForIdle()
            val bitmap = compose.onNodeWithTag("monster-frame")
                .captureToImage()
                .asAndroidBitmap()
            val edge = 3
            repeat(bitmap.width) { x ->
                repeat(edge) { inset ->
                    assertTrue("$name 위쪽이 잘렸다", bitmap.getPixel(x, inset) == expected)
                    assertTrue(
                        "$name 아래쪽이 잘렸다",
                        bitmap.getPixel(x, bitmap.height - 1 - inset) == expected,
                    )
                }
            }
            repeat(bitmap.height) { y ->
                repeat(edge) { inset ->
                    assertTrue("$name 왼쪽이 잘렸다", bitmap.getPixel(inset, y) == expected)
                    assertTrue(
                        "$name 오른쪽이 잘렸다",
                        bitmap.getPixel(bitmap.width - 1 - inset, y) == expected,
                    )
                }
            }
        }

        // 과거 균등 아틀라스 자르기로 실제 신체가 손실됐던 대상은 결과물도
        // 남겨 자동 경계 검사 뒤 사람이 전신을 직접 대조할 수 있게 한다.
        val visualRegressions = linkedMapOf(
            "석순 골렘" to "stalagmite-golem",
            "녹슨 감시기" to "rust-watcher",
            "무너진 갱도의 주인" to "mine-boss",
            "회랑의 주인" to "hall-master",
            "회랑 끝의 그림자" to "hall-boss",
            "천공을 걷는 자" to "sky-boss",
            "심연의 왕" to "abyss-king",
            "유리 거인" to "glass-giant",
            "사막을 걷는 유리왕" to "glass-king",
            "섬을 든 자" to "isle-boss",
            "뒤틀림의 근원" to "warped-root",
        )
        val outputDirectory = requireNotNull(
            InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),
        )
        visualRegressions.forEach { (name, filename) ->
            compose.runOnIdle { current.value = name }
            compose.waitForIdle()
            outputDirectory.resolve("monster-$filename.png").outputStream().use { stream ->
                compose.onNodeWithTag("monster-frame")
                    .captureToImage()
                    .asAndroidBitmap()
                    .compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
        }
    }
}
