package com.geomgang.game.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geomgang.core.Settings
import com.geomgang.core.Timing

/**
 * 설정.
 *
 * 데이터 초기화는 이 화면 아래쪽에 있다. 두 군데 두지 않는다.
 */
@Composable
fun SettingsScreen(
    settings: Settings,
    deepUnlocked: Boolean,
    onAutoPreventChange: (Boolean) -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
) {
    ScrollableForgeScreen(title = "설정", onBack = onBack) {
        ForgePanel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("효과음", fontWeight = FontWeight.Medium)
                        Text(
                            text = if (deepUnlocked) {
                                "강화 결과·복구·조각·사냥 타격 효과음. " +
                                    "단계가 높을수록 성공음도 높아진다."
                            } else {
                                "강화 결과·복구·조각 효과음. " +
                                    "단계가 높을수록 성공음도 높아진다."
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                    PixelToggle(checked = settings.soundOn, onCheckedChange = onSoundChange)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        ForgePanel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("진동", fontWeight = FontWeight.Medium)
                        Text(
                            text = if (deepUnlocked) {
                                "강화 결과마다 다른 진동. 사냥 연타에는 진동이 없다."
                            } else {
                                "강화 결과마다 다른 진동으로 성공과 실패를 구분한다."
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                    PixelToggle(checked = settings.hapticsOn, onCheckedChange = onHapticsChange)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        ForgePanel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("방지권 자동 사용", fontWeight = FontWeight.Medium)
                        Text(
                            text = "파괴 직후 사용 가능한 방지권을 즉시 쓴다",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                    PixelToggle(checked = settings.autoPrevent, onCheckedChange = onAutoPreventChange)
                }
                Spacer(Modifier.height(6.dp))
            }
        }

        Spacer(Modifier.height(12.dp))

        ForgePanel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("진행 초기화", fontWeight = FontWeight.Medium)
                Text(
                    text = if (deepUnlocked) {
                        "현재 판의 검·재화·아이템·사냥 진행 삭제. " +
                            "도감·업적·통계·설정은 유지된다."
                    } else {
                        "현재 판의 검·재화·아이템 삭제. " +
                            "도감·업적·통계·설정은 유지된다."
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                Spacer(Modifier.height(10.dp))
                HoldToReset(label = "5초 길게 눌러 초기화", onComplete = onReset)
            }
        }

        Spacer(Modifier.height(12.dp))

        ForgePanel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("리소스 라이선스", fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "프로젝트 전용 일러스트",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "강화검 7계열 +0~+20 · 전설 +21~+50\n" +
                        "사냥 몬스터 24구역 144종 · 전투 효과 16종\n" +
                        "대장간 배경과 UI 픽셀 아이콘",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "CC0 보조 스프라이트",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    // CC0 은 표기 의무가 없지만 출처를 남기는 것이 이 프로젝트의 방침이다.
                    text = "Dungeon Crawl 32x32 tiles · CC0\n" +
                        "제작: Dungeon Crawl Stone Soup 팀 외 다수\n" +
                        "https://opengameart.org/content/dungeon-crawl-32x32-tiles\n\n" +
                        "고유검과 펫 등 일부 보조 스프라이트에 사용.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "사운드와 기타",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "효과음은 앱에서 실시간 합성한다.\n" +
                        "타사 상용 게임의 그래픽·사운드는 사용하지 않았다.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
    }
}
