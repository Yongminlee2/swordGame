package com.geomgang.game.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geomgang.core.SwordNames
import com.geomgang.game.ForgeUiState
import com.geomgang.game.R

/**
 * 특수강화(별) 전용 화면.
 *
 * 강화 화면 한가운데 있던 것을 떼어 냈다. 메인은 **한 번의 강화**에 집중해야 하는데
 * 성격이 다른 두 번째 강화가 같은 자리에 있으면 눈이 갈라진다.
 *
 * 규칙 자체가 다르다는 것도 여기서 분명해진다 — 별은 **실패해도 검이 부서지지 않는다.**
 * 별 하나를 잃을 뿐이다.
 */
@Composable
fun StarScreen(
    state: ForgeUiState,
    onStarUp: () -> Unit,
    onBack: () -> Unit,
) {
    ScrollableForgeScreen(
        title = "별 강화",
        onBack = onBack,
        wallet = state.wallet(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val sword = state.sword
        val star = state.star
        if (sword == null || star == null) {
            ForgePanel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    LText("아직 별을 붙일 수 없다", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    LText(
                        text = if (sword == null) {
                            "검 없음"
                        } else {
                            "별 강화는 용검부터 열린다."
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }
            return@ScrollableForgeScreen
        }

        SwordView(sword = sword, modifier = Modifier.size(140.dp))
        LText(SwordNames.nameFor(sword), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        LText(
            text = "+${sword.level}",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
        )

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(star.maxStars) { index ->
                PixelIcon(
                    resource = R.drawable.ui_pixel_star,
                    contentDescription = null,
                    modifier = Modifier.size(25.dp),
                    alpha = if (index < star.stars) 1f else 0.18f,
                )
            }
        }
        if (star.attackBonusPercent > 0) {
            LText(
                text = "공격력 +${star.attackBonusPercent}%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        star.lastUp?.let {
            Spacer(Modifier.height(8.dp))
            LText(
                text = if (it) "별 +1" else "실패 · 별 -1 (검 유지)",
                fontSize = 13.sp,
                color = if (it) ForgeGreen else Color(0xFFE0906A),
            )
        }

        Spacer(Modifier.height(16.dp))

        if (star.stars >= star.maxStars) {
            LText(
                "더 올릴 별이 없다",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Stat(R.drawable.ui_pixel_target, "성공", "${star.successPercent}%", MaterialTheme.colorScheme.primary)
                Stat(R.drawable.ui_pixel_gem, "조각", "${star.shardCost}")
                Stat(R.drawable.ui_pixel_gold, "골드", compactGold(star.goldCost))
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onStarUp,
                enabled = !state.busy && star.affordable,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                LText("별 올리기", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            if (!star.affordable) {
                Spacer(Modifier.height(4.dp))
                LText(
                    "조각 또는 골드 부족",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        LText(
            text = "실패해도 검은 유지 · 별만 1단계 하락",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        )
    }
}
