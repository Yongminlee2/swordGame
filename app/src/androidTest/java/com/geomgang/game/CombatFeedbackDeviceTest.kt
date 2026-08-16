package com.geomgang.game

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.test.platform.app.InstrumentationRegistry
import com.geomgang.core.Skills
import com.geomgang.core.WeaponFamily
import com.geomgang.core.Zone
import com.geomgang.game.ui.CombatFeedbackOverlay
import com.geomgang.game.ui.MonsterSprite
import com.geomgang.game.ui.SwordForgeTheme
import org.junit.Rule
import org.junit.Test

/** 보스전에서도 타격 연출과 데미지가 사라지지 않는지 실기기에서 확인한다. */
class CombatFeedbackDeviceTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun bossHitShowsSkillAndDamage() {
        val bossHit = HuntUiState(
            zone = Zone.MEADOW,
            targetName = Zone.MEADOW.bossName,
            rawTargetName = Zone.MEADOW.bossName,
            targetHp = 100,
            targetMaxHp = Zone.MEADOW.bossHp,
            isBoss = true,
            bossRemainingMillis = 4_000,
            killsInZone = 10,
            killsNeeded = 10,
            attackPower = 12_345,
            combo = 1,
            lastDamage = 12_345,
            lastHits = 1,
            lastCrit = false,
            lastSkill = Skills.of(WeaponFamily.DRAGON),
            hitSeq = 1,
            isRare = false,
            lastKillGold = 0,
            bossFailed = false,
            zoneCleared = false,
        )

        compose.setContent {
            SwordForgeTheme {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    MonsterSprite(
                        name = bossHit.rawTargetName,
                        hpRatio = bossHit.hpRatio,
                        isBoss = true,
                        isRare = false,
                        enraged = false,
                        hitSeq = bossHit.hitSeq,
                    )
                    CombatFeedbackOverlay(bossHit)
                }
            }
        }

        // 외곽선과 본문을 두 번 그리므로 같은 텍스트 노드가 두 개씩 있다.
        compose.onAllNodesWithText("12,345").assertCountEquals(2)
        compose.onAllNodesWithText(Skills.of(WeaponFamily.DRAGON).name).assertCountEquals(2)

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val output = requireNotNull(context.getExternalFilesDir(null)).resolve("combat-feedback.png")
        output.outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test
    fun oneShotKillShowsDamageAndGoldAfterNextMonsterSpawned() {
        val oneShot = HuntUiState(
            zone = Zone.MEADOW,
            targetName = Zone.MEADOW.monsters.first().name,
            rawTargetName = Zone.MEADOW.monsters.first().name,
            targetHp = 16,
            targetMaxHp = 16,
            isBoss = false,
            bossRemainingMillis = 0,
            killsInZone = 1,
            killsNeeded = 10,
            attackPower = 12_345,
            combo = 0,
            lastDamage = 12_345,
            lastHits = 1,
            lastCrit = false,
            lastSkill = Skills.of(WeaponFamily.CURVED),
            hitSeq = 1,
            isRare = false,
            lastKillGold = 777,
            bossFailed = false,
            zoneCleared = false,
            lastHitKilled = true,
        )

        compose.setContent {
            SwordForgeTheme {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    // targetHp가 이미 다음 몬스터의 체력이어도 직전 처치 피드백은 보여야 한다.
                    CombatFeedbackOverlay(oneShot)
                }
            }
        }

        compose.onAllNodesWithText("12,345").assertCountEquals(2)
        compose.onAllNodesWithText("+777 GOLD").assertCountEquals(2)
        compose.onAllNodesWithText(Skills.of(WeaponFamily.CURVED).name).assertCountEquals(2)
    }
}
