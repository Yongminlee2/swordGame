package com.geomgang.game.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geomgang.core.Skill
import com.geomgang.game.HuntUiState
import com.geomgang.game.R
import kotlinx.coroutines.delay

private data class CombatHit(
    val seq: Long,
    val damage: Long,
    val hits: Int,
    val critical: Boolean,
    val skill: Skill?,
    val killGold: Long,
)

@DrawableRes
private fun effectDrawable(skillId: String?, critical: Boolean): Int = when (skillId) {
    "flash" -> R.drawable.combat_fx_flash
    "moonfall" -> R.drawable.combat_fx_moonfall
    "collapse" -> R.drawable.combat_fx_collapse
    "flurry" -> R.drawable.combat_fx_flurry
    "twinmoon" -> R.drawable.combat_fx_twinmoon
    "drain" -> R.drawable.combat_fx_drain
    "judgment" -> R.drawable.combat_fx_judgment
    "dragonbreath" -> R.drawable.combat_fx_dragonbreath
    "reap" -> R.drawable.combat_fx_reap
    "crush" -> R.drawable.combat_fx_crush
    "pierce" -> R.drawable.combat_fx_pierce
    "spiritburst" -> R.drawable.combat_fx_spiritburst
    "allthings" -> R.drawable.combat_fx_allthings
    "voidcall" -> R.drawable.combat_fx_voidcall
    else -> if (critical) R.drawable.combat_fx_critical else R.drawable.combat_fx_normal
}

/** Illustrated attack VFX, ornate skill plate, and outlined damage from the selected mock. */
@Composable
fun CombatFeedbackOverlay(hunt: HuntUiState, modifier: Modifier = Modifier) {
    // 보스 대기 화면의 잔상은 HuntScreen 쪽에서 막고, 실제 보스전은
    // 일반 전투와 같이 타격 이펙트·데미지·스킬명을 보여 준다.
    if (hunt.bossFailed || hunt.zoneCleared) return
    val incoming = if (hunt.hitSeq == 0L || hunt.lastDamage <= 0L) null else CombatHit(
            seq = hunt.hitSeq,
            damage = hunt.lastDamage,
            hits = hunt.lastHits,
            critical = hunt.lastCrit,
            skill = hunt.lastSkill,
            killGold = if (hunt.lastHitKilled) hunt.lastKillGold else 0L,
        )
    var visible by remember(hunt.hitSeq) { mutableStateOf(incoming != null) }
    LaunchedEffect(hunt.hitSeq) {
        if (incoming == null) return@LaunchedEffect
        visible = true
        delay(if (incoming.skill != null) 1_050L else 720L)
        visible = false
    }
    incoming?.takeIf { visible }?.let { snapshot ->
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(effectDrawable(snapshot.skill?.id, snapshot.critical)),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(if (snapshot.skill != null) 292.dp else 224.dp)
                    .offset(y = (-12).dp),
            )
            if (snapshot.skill != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 54.dp)
                        .size(width = 260.dp, height = 86.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.combat_skill_banner),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds,
                    )
                    OutlinedCombatText(
                        text = snapshot.skill.name,
                        fill = Color(0xFFFFE4B1),
                        stroke = Color(0xFF290508),
                        size = 22,
                    )
                }
            }
            if (snapshot.critical && snapshot.skill == null) {
                OutlinedCombatText(
                    text = "치명타!",
                    fill = Color(0xFFFFE174),
                    stroke = Color(0xFF5B0905),
                    size = 18,
                    modifier = Modifier.offset(y = (-10).dp),
                )
            }
            val damageText = buildString {
                append("%,d".format(snapshot.damage))
                if (snapshot.hits > 1) append("  ×${snapshot.hits}")
            }
            OutlinedCombatText(
                text = damageText,
                fill = if (snapshot.skill != null || snapshot.critical) Color(0xFFFF8A28) else Color.White,
                stroke = Color(0xFF621105),
                size = if (snapshot.skill != null || snapshot.critical) 30 else 23,
                modifier = Modifier.offset(y = 34.dp),
            )
            if (snapshot.killGold > 0L) {
                OutlinedCombatText(
                    text = "+%,d GOLD".format(snapshot.killGold),
                    fill = ForgeAmber,
                    stroke = Color(0xFF382508),
                    size = 16,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = (-54).dp),
                )
            }
        }
    }
}

@Composable
private fun OutlinedCombatText(
    text: String,
    fill: Color,
    stroke: Color,
    size: Int,
    modifier: Modifier = Modifier,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(
            text = text,
            color = stroke,
            fontSize = size.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            style = MaterialTheme.typography.bodyLarge.merge(
                TextStyle(drawStyle = Stroke(width = if (size >= 24) 7f else 5f)),
            ),
        )
        Text(
            text = text,
            color = fill,
            fontSize = size.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
    }
}
