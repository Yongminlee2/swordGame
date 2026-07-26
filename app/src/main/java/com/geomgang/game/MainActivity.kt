package com.geomgang.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.geomgang.core.Difficulty
import com.geomgang.core.SaveStore
import com.geomgang.game.ui.AchievementScreen
import com.geomgang.game.ui.CodexScreen
import com.geomgang.game.ui.CraftScreen
import com.geomgang.game.ui.ForgeScreen
import com.geomgang.game.ui.HuntScreen
import com.geomgang.game.ui.ModeSelectScreen
import com.geomgang.game.ui.ModeSummary
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
 * 화면이 여럿이지만 딥링크가 없어 내비게이션 라이브러리를 쓰지 않는다.
 * 모드를 고르면 그 모드의 ViewModel 이 만들어지고, 나머지 화면은 같은 인스턴스를 조작한다.
 */
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
    var difficulty by remember { mutableStateOf<Difficulty?>(null) }
    // 모드를 초기화하면 요약을 다시 읽어야 한다.
    var summaryVersion by remember { mutableIntStateOf(0) }

    val current = difficulty
    if (current == null) {
        val summaries = remember(summaryVersion) {
            Difficulty.entries.map { d ->
                val game = store.loadGame(d)
                ModeSummary(
                    difficulty = d,
                    bestLevel = game.bestLevel,
                    gold = game.gold,
                    started = game.bestLevel > 0 || game.sword != null || game.gold > 0,
                )
            }
        }
        ModeSelectScreen(
            summaries = summaries,
            onEnter = { difficulty = it },
            onReset = {
                store.resetGame(it)
                summaryVersion++
            },
        )
        return
    }

    val vm = remember(current) { ForgeViewModel(store, current) }
    val state by vm.ui.collectAsStateWithLifecycle()
    var overlay by remember(current) { mutableStateOf(Overlay.None) }

    BackHandler(enabled = !state.busy) {
        when {
            state.autoForging -> vm.stopAutoForge()
            // 사냥 중이면 먼저 사냥터 목록으로, 거기서 한 번 더 누르면 강화 화면으로
            state.hunt != null -> vm.leaveHunt()
            overlay != Overlay.None -> overlay = overlay.parent()
            else -> difficulty = null
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
            onExit = { difficulty = null },
            onAnimationEnd = vm::onAnimationFinished,
        )
    }
}
