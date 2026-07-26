package com.geomgang.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.geomgang.core.Difficulty
import com.geomgang.core.SaveStore
import com.geomgang.game.sound.SoundEngine
import com.geomgang.game.ui.AchievementScreen
import com.geomgang.game.ui.CodexScreen
import com.geomgang.game.ui.CraftScreen
import com.geomgang.game.ui.ForgeScreen
import com.geomgang.game.ui.HuntScreen
import com.geomgang.game.ui.RecordsMenuScreen
import com.geomgang.game.ui.SettingsScreen
import com.geomgang.game.ui.ShopScreen
import com.geomgang.game.ui.StatsScreen
import com.geomgang.game.ui.SwordForgeTheme

/** 강화 화면 위에 무엇이 올라와 있는지. */
private enum class Overlay { None, Hunt, Shop, Craft, Records, Codex, Achievements, Stats, Settings }

/** 뒤로 가면 어디로 돌아가는지. 기록 하위 화면들은 메뉴로 돌아간다. */
private fun Overlay.parent(): Overlay = when (this) {
    Overlay.Codex, Overlay.Achievements, Overlay.Stats, Overlay.Settings -> Overlay.Records
    else -> Overlay.None
}

/**
 * 모드는 하나다.
 *
 * 쉬움·일반·지옥을 없애고 상한 없는 무한 모드만 남겼다. 덕분에 모드 선택 화면이
 * 사라져 앱을 켜면 곧바로 강화 화면이다 — "시작까지 길다"는 문제가 같이 풀렸다.
 *
 * [Difficulty] 는 지우지 않았다. 확률표에 배수를 곱하는 장치는 그대로 쓸모가 있고
 * 테스트가 그 계산을 지키고 있다. 다만 게임이 쓰는 것은 [Difficulty.ENDLESS] 하나뿐이다.
 */
private val ONLY_MODE = Difficulty.ENDLESS

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SwordForgeTheme {
                Surface {
                    App(SaveStore(filesDir))
                }
            }
        }
    }
}

@Composable
private fun App(store: SaveStore) {
    // 소리를 켤지는 ViewModel 의 설정을 그때그때 읽는다. 설정을 바꾸면 즉시 반영된다.
    val vm = remember {
        lateinit var holder: ForgeViewModel
        val engine = SoundEngine { holder.soundEnabled() }
        holder = ForgeViewModel(store, ONLY_MODE, sound = engine)
        holder
    }
    val state by vm.ui.collectAsStateWithLifecycle()
    var overlay by remember { mutableStateOf(Overlay.None) }

    BackHandler(enabled = !state.busy) {
        when {
            state.autoForging -> vm.stopAutoForge()
            // 사냥 중이면 먼저 사냥터 목록으로, 거기서 한 번 더 누르면 강화 화면으로
            state.hunt != null -> vm.leaveHunt()
            overlay != Overlay.None -> overlay = overlay.parent()
            else -> Unit
        }
    }

    when (overlay) {
        Overlay.Hunt -> HuntScreen(
            state = state,
            adventure = vm.adventure(),
            onEnterZone = vm::enterZone,
            onTap = vm::tapTarget,
            onChallengeBoss = vm::challengeBoss,
            onLeave = vm::leaveHunt,
            onBack = {
                vm.leaveHunt()
                overlay = Overlay.None
            },
        )

        Overlay.Shop -> ShopScreen(
            state = state,
            onBuySword = vm::buySword,
            onSellSword = vm::sellSword,
            onBuyItem = vm::buyItem,
            onBack = { overlay = Overlay.None },
        )

        Overlay.Craft -> CraftScreen(
            state = state,
            onCraft = vm::craft,
            onBack = { overlay = Overlay.None },
        )

        Overlay.Records -> RecordsMenuScreen(
            progress = state.progress,
            onOpenCodex = { overlay = Overlay.Codex },
            onOpenAchievements = { overlay = Overlay.Achievements },
            onOpenStats = { overlay = Overlay.Stats },
            onOpenSettings = { overlay = Overlay.Settings },
            onBack = { overlay = Overlay.None },
        )

        Overlay.Codex -> CodexScreen(
            progress = state.progress,
            onBack = { overlay = Overlay.Records },
        )

        Overlay.Achievements -> AchievementScreen(
            progress = state.progress,
            onSelectTitle = vm::selectTitle,
            onBack = { overlay = Overlay.Records },
        )

        Overlay.Stats -> StatsScreen(
            difficulty = state.difficulty,
            progress = state.progress,
            onBack = { overlay = Overlay.Records },
        )

        Overlay.Settings -> SettingsScreen(
            settings = state.settings,
            onAutoPreventChange = vm::setAutoPrevent,
            onSoundChange = vm::setSoundOn,
            onReset = vm::resetProgress,
            onBack = { overlay = Overlay.Records },
        )

        Overlay.None -> ForgeScreen(
            state = state,
            onForge = vm::forge,
            onPrevent = vm::usePrevent,
            onSalvage = vm::salvage,
            onToggleBlessing = vm::toggleBlessing,
            onToggleLuckCharm = vm::toggleLuckCharm,
            onOpenHunt = { overlay = Overlay.Hunt },
            onOpenShop = { overlay = Overlay.Shop },
            onOpenCraft = { overlay = Overlay.Craft },
            onOpenMenu = { overlay = Overlay.Records },
            onStartAuto = vm::startAutoForge,
            onStopAuto = vm::stopAutoForge,
            onAnimationEnd = vm::onAnimationFinished,
        )
    }
}
