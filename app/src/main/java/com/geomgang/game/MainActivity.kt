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
import com.geomgang.game.ui.CraftScreen
import com.geomgang.game.ui.ForgeScreen
import com.geomgang.game.ui.ModeSelectScreen
import com.geomgang.game.ui.ModeSummary
import com.geomgang.game.ui.ShopScreen
import com.geomgang.game.ui.SwordForgeTheme

/** 강화 화면 위에 무엇이 올라와 있는지. */
private enum class Overlay { None, Shop, Craft }

/**
 * 화면이 넷이고 딥링크가 없어 내비게이션 라이브러리를 쓰지 않는다.
 * 모드를 고르면 그 모드의 ViewModel 이 만들어지고, 상점·조합소는 같은 인스턴스를 조작한다.
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
        if (overlay != Overlay.None) overlay = Overlay.None else difficulty = null
    }

    when (overlay) {
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

        Overlay.None -> ForgeScreen(
            state = state,
            onForge = vm::forge,
            onPrevent = vm::usePrevent,
            onSalvage = vm::salvage,
            onToggleBlessing = vm::toggleBlessing,
            onToggleLuckCharm = vm::toggleLuckCharm,
            onOpenShop = { overlay = Overlay.Shop },
            onOpenCraft = { overlay = Overlay.Craft },
            onExit = { difficulty = null },
            onAnimationEnd = vm::onAnimationFinished,
        )
    }
}
