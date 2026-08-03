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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
    onAutoPreventChange: (Boolean) -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        ScreenHeader(title = "설정", onBack = onBack)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("효과음", fontWeight = FontWeight.Medium)
                        Text(
                            text = "강화 성공·실패·파괴, 방지권, 줍기, 사냥 타격에 소리가 붙는다. " +
                                "성공음은 단계가 높을수록 높은 음이 난다.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                    Switch(checked = settings.soundOn, onCheckedChange = onSoundChange)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("진동", fontWeight = FontWeight.Medium)
                        Text(
                            text = "성공은 짧게, 실패는 둔탁하게, 파괴는 길게 울린다. " +
                                "사냥 탭마다는 울리지 않는다 — 연타라 손이 아프다.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                    Switch(checked = settings.hapticsOn, onCheckedChange = onHapticsChange)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("방지권 자동 사용", fontWeight = FontWeight.Medium)
                        Text(
                            text = "파괴되면 ${Timing.DESTROY_WINDOW_MILLIS / 1000.0}초 창을 " +
                                "열지 않고 바로 방지권을 쓴다",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                    Switch(checked = settings.autoPrevent, onCheckedChange = onAutoPreventChange)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "기본값은 꺼짐이다. 파괴 순간 손이 먼저 나가는 그 긴장이 " +
                        "이 게임의 핵심이라, 편의를 원하는 사람만 켜게 두었다.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("진행 초기화", fontWeight = FontWeight.Medium)
                Text(
                    text = "검·골드·조각·아이템·사냥 진행을 지운다. " +
                        "도감·업적·통계·설정은 지워지지 않는다.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                Spacer(Modifier.height(10.dp))
                HoldToReset(label = "5초 길게 눌러 초기화", onComplete = onReset)
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("리소스 라이선스", fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "검 아이콘",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "The Humble Sword Pack · CC BY 4.0\n" +
                        "제작: The Wise Hedgehog\n" +
                        "https://opengameart.org/content/the-humble-sword-pack\n" +
                        "https://creativecommons.org/licenses/by/4.0/\n\n" +
                        "16비트 픽셀아트 검 30종 × 낡음 3단계. 스프라이트시트에서 " +
                        "필요한 칸만 잘라 그린다.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "몬스터 아이콘",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    // CC0 은 표기 의무가 없지만 출처를 남기는 것이 이 프로젝트의 방침이다.
                    text = "Dungeon Crawl 32x32 tiles · CC0 (퍼블릭 도메인)\n" +
                        "제작: Dungeon Crawl Stone Soup 팀 외 다수\n" +
                        "https://opengameart.org/content/dungeon-crawl-32x32-tiles\n\n" +
                        "몬스터 60종 + 보스 12종 + 펫 12종을 선별해 스프라이트시트로 합쳤다.\n" +
                        "검 그림도 같은 팩의 무기 타일이다 — 계열 14 × 강화 단계 21 = 294칸에\n" +
                        "무한 구간 전설 20칸, 고유검 10칸을 더해 배치했다.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "그 외",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "타사 게임의 그래픽·사운드를 사용하지 않았으며, 변형해 사용하지도 않았다.\n" +
                        "비트맵 이미지 파일이 하나도 없다. 전부 벡터다.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
    }
}
