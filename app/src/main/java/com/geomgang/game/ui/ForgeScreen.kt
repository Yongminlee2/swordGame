package com.geomgang.game.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
    onOpenShop: () -> Unit,
    onOpenCraft: () -> Unit,
    onOpenMenu: () -> Unit,
    onStartAuto: (Int) -> Unit,
    onStopAuto: () -> Unit,
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

    Column(
        modifier = Modifier
            .fillMaxSize()
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
                Text("기록", color = MaterialTheme.colorScheme.secondary)
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // 제한 시간 창이 열려 있으면 검이 있던 자리를 원이나 파편이 차지한다.
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

                    DestroyPhase.None -> SwordView(
                        sword = state.sword,
                        modifier = Modifier.size(210.dp, 210.dp),
                        shake = shake.value,
                        flash = flash.value,
                        flashColor = flashColor,
                    )
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = when (state.destroyPhase) {
                        is DestroyPhase.Prevent -> "지금 눌러야 한다"
                        is DestroyPhase.Salvage -> "파편이 흩어진다"
                        // 이름은 단계마다 다르다. 계열은 형태만 정하고 부제로 내려간다.
                        DestroyPhase.None -> state.sword?.let {
                            SwordNames.nameFor(it.level)
                        } ?: "검이 없다"
                    },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (state.destroyPhase == DestroyPhase.None && state.sword != null) {
                    Text(
                        text = "+${state.sword.level} · ${state.sword.family.displayName} 계열",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                    )
                }
                Spacer(Modifier.height(6.dp))
                ResultBanner(state.lastResult)
            }
        }

        InfoRow("골드", "%,d".format(state.gold))
        InfoRow("조각", "${state.shards}")
        InfoRow("방지권", "${state.preventTickets}장")

        if (state.sword != null) {
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            InfoRow("다음 성공률", "${state.successPercent}%")
            InfoRow("강화 비용", "%,d".format(state.upgradeCost))
            InfoRow("판매가", "%,d".format(state.sellPrice))

            if (!state.awaitingDestroyChoice) {
                Spacer(Modifier.height(10.dp))
                // 아이템은 한 번 쓰면 토글이 내려간다. 매번 다시 켜야 한다.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = state.useBlessing,
                        onClick = onToggleBlessing,
                        enabled = state.blessingScrolls > 0 && !state.busy,
                        label = { Text("축복서 ${state.blessingScrolls}") },
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = state.useLuckCharm,
                        onClick = onToggleLuckCharm,
                        enabled = state.luckCharms > 0 && !state.busy,
                        label = { Text("행운부적 ${state.luckCharms}") },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

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
                    text = "사냥터  ·  공격력 %,d".format(state.attackPower),
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(10.dp))
            AutoForgeBar(state, onStartAuto, onStopAuto)
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onOpenShop,
                    enabled = !state.busy && !state.autoForging,
                    modifier = Modifier.weight(1f),
                ) { Text("상점") }
                OutlinedButton(
                    onClick = onOpenCraft,
                    enabled = !state.busy && !state.autoForging,
                    modifier = Modifier.weight(1f),
                ) { Text("조합소  ${state.shards}") }
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
