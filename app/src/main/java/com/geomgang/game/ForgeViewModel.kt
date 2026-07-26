package com.geomgang.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geomgang.core.Difficulty
import com.geomgang.core.Economy
import com.geomgang.core.ForgeEngine
import com.geomgang.core.ForgeResult
import com.geomgang.core.GameState
import com.geomgang.core.Progress
import com.geomgang.core.ProgressState
import com.geomgang.core.RateTable
import com.geomgang.core.SaveStore
import com.geomgang.core.Timing
import com.geomgang.core.UsedItems
import com.geomgang.core.WeaponFamily
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * 강화 화면의 상태 보유자.
 *
 * 도메인은 순수 함수라 상태를 스스로 들고 있지 않는다. 그 역할이 여기다.
 * 연출 중 입력 잠금도 여기서만 한다 — 잠금 주체가 여럿이면 연타로 상태가 꼬인다.
 *
 * 프로퍼티 초기화 순서에 의미가 있다. [progress] → [game] → [busy] → [_ui] 순으로 선언해야
 * 마지막의 `render()` 가 앞의 셋을 모두 읽을 수 있다.
 */
class ForgeViewModel(
    private val store: SaveStore,
    difficulty: Difficulty,
    private val rng: Random = Random.Default,
) : ViewModel() {

    private var progress: ProgressState = store.loadProgress()

    private var game: GameState = loadAndRepair(difficulty)

    private var busy = false

    private var phase: DestroyPhase = DestroyPhase.None

    private var countdownJob: Job? = null

    private val _ui = MutableStateFlow(render(null))
    val ui: StateFlow<ForgeUiState> = _ui.asStateFlow()

    /**
     * 세이브를 불러오면서 두 가지를 손본다.
     *
     * 1. 파괴 대기 상태가 남아 있으면 확정 처리한다. 방지권 원이 떠 있는 동안 앱을 죽여
     *    파괴를 무효화하는 악용을 막는 지점이다. 기회를 놓친 것으로 통계에 남긴다.
     * 2. 아무것도 할 수 없는 상태면 파산 구제를 적용한다.
     */
    private fun loadAndRepair(difficulty: Difficulty): GameState {
        var loaded = store.loadGame(difficulty)

        if (loaded.pendingDestroy != null) {
            loaded = ForgeEngine.confirmDestroy(loaded)
            progress = Progress.refresh(Progress.onPreventMissed(progress))
            store.saveGame(loaded)
            store.saveProgress(progress)
        }

        val rescued = Economy.applyBailoutIfNeeded(loaded)
        if (rescued !== loaded) {
            progress = Progress.refresh(Progress.onBailout(progress))
            store.saveGame(rescued)
            store.saveProgress(progress)
            return rescued
        }
        return loaded
    }

    fun forge() {
        if (busy || !ForgeEngine.canAttempt(game, UsedItems.NONE)) return
        val sword = game.sword ?: return
        val targetLevel = sword.level + 1
        val cost = Economy.upgradeCost(sword.level)

        val result = ForgeEngine.attempt(game, UsedItems.NONE, rng)
        game = result.state
        progress = Progress.refresh(
            Progress.onAttempt(progress, game.difficulty, sword.family, targetLevel, cost, result),
        )
        busy = true
        persist()
        _ui.value = render(result)

        if (result is ForgeResult.Destroyed) openDestroyWindow()
    }

    /**
     * 파괴 직후 제한 시간 창을 연다.
     *
     * 방지권이 있으면 먼저 되살릴 기회를 주고, 없거나 놓치면 줍기로 넘어간다.
     */
    private fun openDestroyWindow() {
        if (ForgeEngine.canPrevent(game)) {
            runWindow(Timing.PREVENT_WINDOW_MILLIS) { remaining, total ->
                DestroyPhase.Prevent(remaining, total)
            }
        } else {
            openSalvageWindow()
        }
    }

    private fun openSalvageWindow() {
        runWindow(Timing.SALVAGE_WINDOW_MILLIS) { remaining, total ->
            DestroyPhase.Salvage(remaining, total)
        }
    }

    /**
     * 제한 시간을 재며 [make] 로 만든 단계를 화면에 흘려보낸다. 만료되면 [onWindowExpired].
     *
     * 시간을 화면이 아니라 여기서 재는 이유: 화면이 재구성될 때마다 타이머가 흔들리면 안 되고,
     * 가상 시간으로 테스트할 수 있어야 하기 때문이다.
     */
    private fun runWindow(
        totalMillis: Long,
        make: (remaining: Long, total: Long) -> DestroyPhase,
    ) {
        countdownJob?.cancel()
        busy = true

        // 창은 코루틴 스케줄을 기다리지 않고 즉시 연다.
        // 한 프레임이라도 늦게 뜨면 그만큼 반응 시간을 빼앗는 셈이다.
        phase = make(totalMillis, totalMillis)
        _ui.value = render(null)

        countdownJob = viewModelScope.launch {
            var remaining = totalMillis
            while (remaining > 0) {
                delay(Timing.TICK_MILLIS)
                remaining -= Timing.TICK_MILLIS
                phase = make(remaining.coerceAtLeast(0), totalMillis)
                _ui.value = render(null)
            }
            onWindowExpired()
        }
    }

    private fun onWindowExpired() {
        when (phase) {
            is DestroyPhase.Prevent -> {
                // 놓쳤을 뿐이므로 방지권은 소모하지 않는다. 기회만 사라진다.
                progress = Progress.refresh(Progress.onPreventMissed(progress))
                persist()
                openSalvageWindow()
            }

            is DestroyPhase.Salvage -> {
                progress = Progress.refresh(Progress.onSalvageMissed(progress))
                game = ForgeEngine.confirmDestroy(game)
                closeDestroyWindow()
            }

            DestroyPhase.None -> Unit
        }
    }

    /** 파괴된 검을 방지권으로 되살린다. */
    fun usePrevent() {
        if (!ForgeEngine.canPrevent(game)) return
        countdownJob?.cancel()
        game = ForgeEngine.applyPrevent(game)
        progress = Progress.refresh(Progress.onPreventUsed(progress))
        closeDestroyWindow()
    }

    /** 파편을 주워 조각을 얻는다. */
    fun salvage() {
        if (game.pendingDestroy == null) return
        countdownJob?.cancel()
        val before = game.shards
        game = ForgeEngine.applySalvage(game, rng)
        progress = Progress.refresh(Progress.onSalvage(progress, game.shards - before))
        closeDestroyWindow()
    }

    fun sellSword() {
        if (busy || !Economy.canSellSword(game)) return
        val price = Economy.sellPrice(game.sword?.level ?: 0)
        game = Economy.sellSword(game)
        progress = Progress.refresh(Progress.onSell(progress, price))
        applyBailout()
        persist()
        _ui.value = render(null)
    }

    fun buySword(family: WeaponFamily) {
        if (busy || !Economy.canBuySword(game)) return
        game = Economy.buySword(game, family)
        game.sword?.let { progress = Progress.registerSword(progress, game.difficulty, it) }
        persist()
        _ui.value = render(null)
    }

    /** 결과 연출이 끝났다고 화면이 알려 준다. 입력 잠금을 푼다. */
    fun onAnimationFinished() {
        if (!busy) return
        busy = false
        _ui.value = render(null)
    }

    private fun closeDestroyWindow() {
        countdownJob = null
        phase = DestroyPhase.None
        busy = false
        applyBailout()
        persist()
        _ui.value = render(null)
    }

    override fun onCleared() {
        countdownJob?.cancel()
        super.onCleared()
    }

    private fun applyBailout() {
        val rescued = Economy.applyBailoutIfNeeded(game)
        if (rescued !== game) {
            game = rescued
            progress = Progress.refresh(Progress.onBailout(progress))
        }
    }

    private fun persist() {
        store.saveGame(game)
        store.saveProgress(progress)
    }

    private fun render(result: ForgeResult?): ForgeUiState {
        val sword = game.sword
        val level = sword?.level ?: 0
        val targetLevel = level + 1
        return ForgeUiState(
            difficulty = game.difficulty,
            sword = sword,
            gold = game.gold,
            shards = game.shards,
            preventTickets = game.inventory.preventTickets,
            bestLevel = game.bestLevel,
            upgradeCost = Economy.upgradeCost(level),
            sellPrice = Economy.sellPrice(level),
            successPercent =
                (RateTable.successRate(game.difficulty, targetLevel) * 100).roundToInt(),
            canForge = !busy && ForgeEngine.canAttempt(game, UsedItems.NONE),
            canBuySword = !busy && Economy.canBuySword(game),
            lastResult = result,
            destroyPhase = phase,
            canPrevent = ForgeEngine.canPrevent(game),
            busy = busy,
        )
    }
}
