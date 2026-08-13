package com.geomgang.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geomgang.core.Skills
import com.geomgang.core.WeaponFamily
import com.geomgang.core.Zone
import com.geomgang.game.ui.BattleArenaBackdrop
import com.geomgang.game.ui.BossFailedDialog
import com.geomgang.game.ui.CombatFeedbackOverlay
import com.geomgang.game.ui.ForgeRed
import com.geomgang.game.ui.MonsterSprite
import com.geomgang.game.ui.PixelProgressBar
import com.geomgang.game.ui.SwordForgeTheme
import kotlinx.coroutines.delay

/** Debug-only deterministic capture screen; it never reads or writes player save data. */
class CombatPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SwordForgeTheme { CombatPreview() } }
    }
}

@Composable
private fun CombatPreview() {
    val zone = Zone.MEADOW
    val skill = Skills.of(WeaponFamily.DRAGON)
    val previewBoss = intentBoolean("boss", default = true)
    val showHit = intentBoolean("hit", default = true)
    val previewFailed = intentBoolean("failed", default = false)
    var failedVisible by remember { mutableStateOf(previewFailed) }
    var hitSeq by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        if (showHit) {
            delay(1_000)
            hitSeq = 41L
        }
    }
    val base = HuntUiState(
        zone = zone,
        targetName = if (previewBoss) zone.bossName else zone.monsters.first().name,
        rawTargetName = if (previewBoss) zone.bossName else zone.monsters.first().name,
        targetHp = if (previewBoss) zone.bossHp else zone.hpOf(zone.monsters.first()),
        targetMaxHp = if (previewBoss) zone.bossHp else zone.hpOf(zone.monsters.first()),
        isBoss = previewBoss,
        bossRemainingMillis = if (previewBoss) 5_000 else 0,
        killsInZone = 12,
        killsNeeded = 12,
        attackPower = 16_959,
        combo = 7,
        lastDamage = 23_584,
        lastHits = 1,
        lastCrit = false,
        lastSkill = skill,
        hitSeq = hitSeq,
        isRare = false,
        lastKillGold = 0,
        bossFailed = false,
        zoneCleared = false,
    )
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("초원 · 보스전", fontSize = 22.sp, fontWeight = FontWeight.Black)
        Text("용검 +20  ·  공격력 16,959", color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(CutCornerShape(5.dp)),
            contentAlignment = Alignment.Center,
        ) {
            BattleArenaBackdrop(Modifier.fillMaxSize(), danger = true)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(base.targetName, fontSize = 22.sp, fontWeight = FontWeight.Black, color = ForgeRed)
                Spacer(Modifier.height(22.dp))
                MonsterSprite(
                    name = base.rawTargetName,
                    hpRatio = 0.62f,
                    isBoss = previewBoss,
                    isRare = false,
                    enraged = false,
                    hitSeq = hitSeq,
                )
                Spacer(Modifier.height(16.dp))
                PixelProgressBar(0.62f, Modifier.fillMaxWidth(0.88f), 14.dp, ForgeRed)
                Text("136 / 220", fontSize = 12.sp)
            }
            CombatFeedbackOverlay(base)
        }
    }
    if (failedVisible) {
        BossFailedDialog(base.copy(bossFailed = true)) { failedVisible = false }
    }
}

@Composable
private fun intentBoolean(name: String, default: Boolean): Boolean {
    val activity = androidx.compose.ui.platform.LocalContext.current as CombatPreviewActivity
    return activity.intent.getStringExtra(name)?.toBooleanStrictOrNull() ?: default
}
