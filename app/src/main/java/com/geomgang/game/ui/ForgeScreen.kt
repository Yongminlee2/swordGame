package com.geomgang.game.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geomgang.core.Difficulty
import com.geomgang.core.ForgeResult
import com.geomgang.core.SwordNames
import com.geomgang.game.DestroyPhase
import com.geomgang.game.ForgeUiState

/**
 * 결과별 연출 길이(ms).
 *
 * 결과가 몸에 남을 만큼만, 진행을 막지 않을 만큼 짧게. 이 값이 커지면
 * 연타로 굴리는 맛이 사라지고 게임이 답답해진다.
 */
private const val SUCCESS_MILLIS = 350
private const val STAY_MILLIS = 250
private const val DROP_MILLIS = 400
private const val DESTROY_MILLIS = 300

/** 흔들림 진폭(px). 하락이 유지보다 크게 흔들려야 손해를 체감한다. */
private const val STAY_SHAKE = 12f
private const val DROP_SHAKE = 26f

@Composable
fun ForgeScreen(
    state: ForgeUiState,
    onForge: () -> Unit,
    onPrevent: () -> Unit,
    onSalvage: () -> Unit,
    onToggleBlessing: () -> Unit,
    onToggleLuckCharm: () -> Unit,
    onOpenHunt: () -> Unit,
    onOpenGauntlet: () -> Unit,
    onOpenStorage: () -> Unit,
    onOpenShop: () -> Unit,
    onOpenCraft: () -> Unit,
    onOpenCodex: () -> Unit,
    onOpenQuests: () -> Unit,
    onOpenMenu: () -> Unit,
    onStartAuto: (Int) -> Unit,
    onStopAuto: () -> Unit,
    onMaterialCount: (Int) -> Unit,
    onStarUp: () -> Unit,
    onAnimationEnd: () -> Unit,
) {
    val shake = remember { Animatable(0f) }
    val flash = remember { Animatable(0f) }
    var flashColor by remember { mutableStateOf(Color.White) }

    // 파괴는 사용자의 응답을 기다려야 하므로 잠금을 자동으로 풀지 않는다.
    LaunchedEffect(state.lastResult) {
        when (val result = state.lastResult) {
            null -> {
                shake.snapTo(0f)
                flash.snapTo(0f)
            }

            is ForgeResult.Success -> {
                flashColor = Color(0xFFFFF3D0)
                flash.flashOnce(SUCCESS_MILLIS)
                onAnimationEnd()
            }

            is ForgeResult.Stay -> {
                shake.shakeOnce(STAY_SHAKE, STAY_MILLIS)
                onAnimationEnd()
            }

            is ForgeResult.Drop -> {
                shake.shakeOnce(DROP_SHAKE, DROP_MILLIS)
                onAnimationEnd()
            }

            is ForgeResult.Destroyed -> {
                flashColor = Color(0xFFE05A5A)
                flash.flashOnce(DESTROY_MILLIS)
                // 여기서 onAnimationEnd 를 부르지 않는다.
                // 제한 시간 창이 열려 있고, 그 잠금은 ViewModel 이 푼다.
            }
        }
    }

    // 세로로 스크롤된다. 이 화면은 판이 갈수록 줄이 늘어나는데(요구량·스킬·재료·별·
    // 회랑·아이콘) 고정 높이로 두면 짧은 화면이나 큰 글꼴에서 아래가 잘려 나간다.
    // v1.3에서 실제로 검 이름이 그렇게 사라졌다.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(64.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "최고 +${state.bestLevel}",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                )
                state.progress.selectedTitle?.let {
                    Text(
                        text = it.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            TextButton(onClick = onOpenMenu, enabled = !state.busy && !state.autoForging) {
                Text("🏅", fontSize = 22.sp)
            }
        }

        // 그림 영역. 스크롤되는 열 안에서는 weight 를 쓸 수 없으므로(높이가 무한대다)
        // 최소 높이로 자리를 잡는다. 이름·강화 단계는 이 밖에 둔다 — 안에 넣으면
        // 가운데 정렬 탓에 위아래로 잘린다.
        Box(
            modifier = Modifier
                .heightIn(min = 170.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            when (val phase = state.destroyPhase) {
                is DestroyPhase.Prevent -> PreventRing(
                    progress = phase.progress,
                    enabled = state.canPrevent,
                    onTap = onPrevent,
                )

                is DestroyPhase.Salvage -> SalvageShards(
                    progress = phase.progress,
                    onTap = onSalvage,
                )

                // 표시 크기는 고정이다. 가변으로 두면 주변 UI(별 강화 바 등)가 나타날
                // 때마다 검이 커졌다 작아졌다 해서 들쭉날쭉해 보인다.
                DestroyPhase.None -> SwordView(
                    sword = state.sword,
                    modifier = Modifier.size(140.dp),
                    shake = shake.value,
                    flash = flash.value,
                    flashColor = flashColor,
                )
            }
        }

        // 이름·강화 단계는 가변 영역 밖이라 어떤 화면에서도 잘리지 않는다.
        Text(
            text = when (state.destroyPhase) {
                is DestroyPhase.Prevent -> "지금 눌러야 한다"
                is DestroyPhase.Salvage -> "파편이 흩어진다"
                // 이름은 단계마다 다르다. 계열은 형태만 정하고 부제로 내려간다.
                // 고유검은 고유 이름을 금색으로.
                DestroyPhase.None -> state.sword?.let {
                    SwordNames.nameFor(it)
                } ?: "검이 없다"
            },
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = if (state.destroyPhase == DestroyPhase.None &&
                state.sword?.uniqueId != null
            ) {
                Color(0xFFFFD54A)
            } else {
                Color.Unspecified
            },
        )
        if (state.destroyPhase == DestroyPhase.None && state.sword != null) {
            Text(
                text = "+${state.sword.level} · ${state.sword.family.displayName} 계열" +
                    if (state.sword.stars > 0) "  ${"★".repeat(state.sword.stars)}" else "",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            )
            // 스킬은 +15부터 열린다. 강화 단계를 올릴 이유를 하나 더 보여 준다.
            val skill = com.geomgang.core.Skills.of(state.sword.family)
            val unlocked = com.geomgang.core.Skills.unlocked(state.sword)
            Text(
                text = if (unlocked) {
                    "⚡ ${skill.name} — ${skill.blurb}"
                } else {
                    "🔒 ${skill.name} — +${com.geomgang.core.Skills.MIN_LEVEL}부터"
                },
                fontSize = 11.sp,
                color = if (unlocked) {
                    Color(0xFF7FE8FF)
                } else {
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                },
            )
        }
        Spacer(Modifier.height(4.dp))
        ResultBanner(state.lastResult)
        Spacer(Modifier.height(6.dp))

        // 자원은 한 줄에 아이콘으로. 세 줄 라벨-값 표는 세로 공간을 먹어
        // 가운데 검 자리를 눌러 이름이 잘려 나갔다.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Stat("💰", compactGold(state.gold))
            Stat("💎", "${state.shards}")
            Stat("🪨", "${state.forgeStones}")
            Stat("🛡", "${state.preventTickets}")
        }

        if (state.sword != null) {
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Stat("🎯", "${state.successPercent}%", MaterialTheme.colorScheme.primary)
                Stat("🔥", compactGold(state.upgradeCost))
                Stat("🏷", compactGold(state.sellPrice))
            }

            if (!state.awaitingDestroyChoice) {
                Spacer(Modifier.height(8.dp))
                // 아이템은 한 번 쓰면 토글이 내려간다. 매번 다시 켜야 한다.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = state.useBlessing,
                        onClick = onToggleBlessing,
                        enabled = state.blessingScrolls > 0 && !state.busy,
                        label = { Text("📜 ${state.blessingScrolls}") },
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = state.useLuckCharm,
                        onClick = onToggleLuckCharm,
                        enabled = state.luckCharms > 0 && !state.busy,
                        label = { Text("🍀 ${state.luckCharms}") },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // 창이 열려 있으면 하단 버튼을 감춘다. 원과 파편을 눌러야 하기 때문이다.
        if (state.awaitingDestroyChoice) {
            Text(
                text = when (state.destroyPhase) {
                    is DestroyPhase.Prevent ->
                        if (state.canPrevent) "원을 눌러 검을 살려라" else "방지권이 없다"

                    else -> "파편을 눌러 조각을 회수한다"
                },
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(64.dp))
        } else {
            // 고단계는 골드가 아니라 재료가 화폐다. 무엇이 드는지 버튼 위에 먼저 알린다.
            if (state.requiredSwords > 0 || state.requiredStones > 0) {
                Text(
                    text = buildString {
                        append("필요  ")
                        if (state.requiredSwords > 0) append("🗡 ${state.requiredSwords}자루  ")
                        if (state.requiredStones > 0) append("🪨 ${state.requiredStones}")
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (state.canForge) {
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
                Spacer(Modifier.height(4.dp))
            }
            Button(
                onClick = onForge,
                enabled = state.canForge,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
            ) {
                Text("강 화", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            state.forgeBlockedReason?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(10.dp))
            MaterialBar(state, onMaterialCount)
            StarBar(state, onStarUp)
            Spacer(Modifier.height(10.dp))
            // 사냥이 강화 비용의 출처다. 강화 버튼 바로 아래에 둬서 왕복이 짧게 한다.
            Button(
                onClick = onOpenHunt,
                enabled = !state.busy && !state.autoForging && state.sword != null,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = Color(0xFF10222E),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(
                    text = "⚔ 사냥터  %,d".format(state.attackPower),
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(8.dp))
            // 무한 회랑 - 중반(화산 클리어)부터 열리는 엔드컨텐츠
            OutlinedButton(
                onClick = onOpenGauntlet,
                enabled = !state.busy && !state.autoForging &&
                    state.sword != null && state.gauntletUnlocked,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = when {
                        !state.gauntletUnlocked -> "🔒 무한 회랑"
                        state.gauntletBest > 0 -> "무한 회랑 · ${state.gauntletBest}층"
                        else -> "무한 회랑"
                    },
                    color = if (state.gauntletUnlocked) Color(0xFFC79BFF) else Color.Unspecified,
                )
            }
            Spacer(Modifier.height(10.dp))
            AutoForgeBar(state, onStartAuto, onStopAuto)
            Spacer(Modifier.height(10.dp))
            // 글자 버튼 다섯 개 대신 아이콘 줄 하나 - 화면의 글자를 줄인다
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val enabled = !state.busy && !state.autoForging
                IconEntry("🛒", "상점", enabled, Modifier.weight(1f), onOpenShop)
                IconEntry("⚗️", "조합소", enabled, Modifier.weight(1f), onOpenCraft)
                IconEntry(
                    icon = "🎒",
                    label = "${state.storage.size}/${state.storageCapacity}",
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenStorage,
                )
                IconEntry("📖", "도감", enabled, Modifier.weight(1f), onOpenCodex)
                IconEntry(
                    icon = "📜",
                    label = if (state.questClaimable) "퀘스트!" else "퀘스트",
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenQuests,
                    highlight = state.questClaimable,
                )
            }
        }
    }
}

/**
 * 자동강화 막대.
 *
 * 안전구간에서만 뜬다. 하락·파괴가 걸린 구간을 자동화하면 그 구간의 긴장이 사라져
 * 게임이 남지 않기 때문이다. 멈춤 조건 판정은 전부 도메인이 한다.
 */
/** 아이콘 + 값 한 쌍. 라벨 글자 없이 뜻이 통하는 것만 아이콘으로 쓴다. */
@Composable
private fun Stat(icon: String, value: String, color: Color = Color.Unspecified) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 15.sp)
        Spacer(Modifier.width(4.dp))
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

/**
 * 큰 수를 짧게. 26억을 자릿수로 다 쓰면 한 줄을 통째로 먹는다.
 *
 * 조(兆) 이상은 소수 첫째 자리까지만 보여 준다 — 정확한 값이 필요한 순간은
 * 강화 비용을 낼 수 있는지뿐이고, 그건 버튼 활성화가 알려 준다.
 */
private fun compactGold(value: Long): String = when {
    value >= 1_000_000_000_000L -> "%.1f조".format(value / 1_000_000_000_000.0)
    value >= 100_000_000L -> "%.1f억".format(value / 100_000_000.0)
    value >= 10_000L -> "%,d만".format(value / 10_000L)
    else -> "%,d".format(value)
}

/**
 * 아이콘 입구 하나. 큰 아이콘 + 아주 작은 라벨.
 *
 * 글자 버튼이 다섯 개 늘어서면 화면이 문장으로 뒤덮인다. 아이콘이 뜻을 말하고
 * 라벨은 확인용으로만 작게 붙인다.
 */
@Composable
private fun IconEntry(
    icon: String,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    highlight: Boolean = false,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (highlight) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 8.dp)
            .alpha(if (enabled) 1f else 0.4f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = icon, fontSize = 22.sp)
        Text(
            text = label,
            fontSize = 10.sp,
            maxLines = 1,
            color = if (highlight) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            },
        )
    }
}

@Composable
private fun AutoForgeBar(
    state: ForgeUiState,
    onStartAuto: (Int) -> Unit,
    onStopAuto: () -> Unit,
) {
    if (state.autoForging) {
        Button(
            onClick = onStopAuto,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("자동강화 중지") }
        return
    }

    if (!state.canAutoForge) return

    Column(Modifier.fillMaxWidth()) {
        Text(
            text = "자동강화 (안전구간까지만)",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val current = state.sword?.level ?: 0
            (current + 1..AUTO_FORGE_TARGET_MAX).forEach { target ->
                OutlinedButton(
                    onClick = { onStartAuto(target) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 6.dp),
                ) { Text("+$target", fontSize = 13.sp) }
            }
        }
    }
}

/** 자동강화 목표로 고를 수 있는 최대 단계. 안전구간의 끝이다. */
private const val AUTO_FORGE_TARGET_MAX = 5

/**
 * 재료 강화 막대.
 *
 * 어느 검을 태울지 고르게 하지 않는다. 개수만 정하면 보관함의 **낮은 단계부터** 집는다.
 * 태울 것은 늘 잡템이고, 고르는 화면을 하나 더 두면 강화 리듬이 끊긴다.
 */
@Composable
private fun MaterialBar(state: ForgeUiState, onMaterialCount: (Int) -> Unit) {
    if (state.sword == null || state.maxMaterials == 0) return

    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "재료 강화 (낮은 검부터 태운다)",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
            if (state.materialCount > 0) {
                Text(
                    text = "성공률 +${state.materialBonusPercent}%p",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            (0..state.maxMaterials).forEach { count ->
                FilterChip(
                    selected = state.materialCount == count,
                    onClick = { onMaterialCount(count) },
                    enabled = !state.busy && !state.autoForging,
                    label = {
                        Text(if (count == 0) "안 씀" else "${count}개", fontSize = 12.sp)
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}

/**
 * 별 강화(특수강화) 막대.
 *
 * 강화 단계와 별개의 계층이다. **실패해도 검이 부서지지 않고 별만 하나 줄어든다** —
 * 그래야 두 계층의 긴장이 겹치지 않는다.
 */
@Composable
private fun StarBar(state: ForgeUiState, onStarUp: () -> Unit) {
    val star = state.star ?: return

    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "특수강화  " + "★".repeat(star.stars) + "☆".repeat(star.maxStars - star.stars),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD24A),
            )
            if (star.attackBonusPercent > 0) {
                Text(
                    text = "공격력 +${star.attackBonusPercent}%",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        star.lastUp?.let {
            Text(
                text = if (it) "별이 하나 올랐다" else "실패 — 별 하나를 잃었다 (검은 무사하다)",
                fontSize = 12.sp,
                color = if (it) Color(0xFF7FD48A) else Color(0xFFE0906A),
            )
        }
        Spacer(Modifier.height(4.dp))
        if (star.stars >= star.maxStars) {
            Text(
                "더 올릴 별이 없다",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            )
        } else {
            OutlinedButton(
                onClick = onStarUp,
                enabled = !state.busy && !state.autoForging && star.affordable,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "별 올리기  ${star.successPercent}%  ·  조각 ${star.shardCost} · " +
                        " %,d골드".format(star.goldCost),
                    fontSize = 13.sp,
                )
            }
            if (!star.affordable) {
                Text(
                    "조각이나 골드가 모자라다",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun ResultBanner(result: ForgeResult?) {
    val (text, color) = when (result) {
        is ForgeResult.Success -> "성공!  +${result.newLevel}" to Color(0xFF7FD48A)
        is ForgeResult.Stay -> "실패 — 단계 유지" to Color(0xFFD4C87F)
        is ForgeResult.Drop -> "하락…  +${result.newLevel}" to Color(0xFFD49A5A)
        is ForgeResult.Destroyed -> "파괴!!" to Color(0xFFE05A5A)
        null -> "" to Color.Transparent
    }
    Text(text = text, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        Text(value, fontWeight = FontWeight.Medium)
    }
}

/** 좌우로 몇 번 흔들리다 제자리로 돌아온다. */
private suspend fun Animatable<Float, *>.shakeOnce(amplitude: Float, durationMillis: Int) {
    snapTo(0f)
    animateTo(
        targetValue = 0f,
        animationSpec = keyframes {
            this.durationMillis = durationMillis
            0f at 0
            amplitude at durationMillis / 6
            -amplitude at durationMillis * 2 / 6
            amplitude * 0.6f at durationMillis * 3 / 6
            -amplitude * 0.35f at durationMillis * 4 / 6
            amplitude * 0.15f at durationMillis * 5 / 6
            0f at durationMillis
        },
    )
}

/** 확 밝아졌다가 가라앉는다. */
private suspend fun Animatable<Float, *>.flashOnce(durationMillis: Int) {
    snapTo(0f)
    animateTo(
        targetValue = 0f,
        animationSpec = keyframes {
            this.durationMillis = durationMillis
            0f at 0
            1f at durationMillis / 5
            0f at durationMillis
        },
    )
}

private fun Difficulty.displayLabel(): String = when (this) {
    Difficulty.EASY -> "쉬움"
    Difficulty.NORMAL -> "일반"
    Difficulty.HARD -> "지옥"
    Difficulty.ENDLESS -> "무한"
}
