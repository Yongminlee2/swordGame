package com.geomgang.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geomgang.core.Difficulty
import com.geomgang.core.Economy
import com.geomgang.core.ForgeEngine
import com.geomgang.core.ForgeResult
import com.geomgang.core.GameState
import com.geomgang.core.Item
import com.geomgang.core.Progress
import com.geomgang.core.ProgressState
import com.geomgang.core.RateTable
import com.geomgang.core.Achievement
import com.geomgang.core.AdventureState
import com.geomgang.core.Combat
import com.geomgang.core.Recipes
import com.geomgang.core.SaveStore
import com.geomgang.core.Settings
import com.geomgang.core.Timing
import com.geomgang.core.UsedItems
import com.geomgang.core.WeaponFamily
import com.geomgang.core.Zone
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

    private var settings: Settings = store.loadSettings()

    private var game: GameState = loadAndRepair(difficulty)

    private var busy = false

    private var phase: DestroyPhase = DestroyPhase.None

    private var countdownJob: Job? = null

    private var autoJob: Job? = null

    // --- 사냥 상태 ---
    // 지금 잡고 있는 대상만 메모리에 둔다. 저장되는 것은 구역과 잡은 수뿐이다.
    // 앱을 껐다 켜면 잡던 몬스터는 사라지고 그 구역 처음부터인데, 그게 자연스럽다.
    private var huntZone: Zone? = null
    private var targetHp: Long = 0
    private var targetMaxHp: Long = 0
    private var fightingBoss = false
    private var bossRemainingMillis: Long = 0
    private var combo = 0
    private var lastTapAt = 0L
    private var lastDamage = 0L
    private var lastHits = 0
    private var bossFailed = false
    private var zoneCleared = false
    private var huntJob: Job? = null

    /**
     * 마지막 강화 결과. 연출이 끝나거나 파괴 창이 닫힐 때까지 유지한다.
     *
     * 카운트다운이 매 틱마다 화면을 다시 그리는데, 여기서 결과를 들고 있지 않으면
     * "파괴!!" 배너가 첫 틱에 사라져 버린다.
     */
    private var lastResult: ForgeResult? = null

    /** 다음 강화에 쓸 아이템. 한 번 쓰면 [UsedItems.NONE] 으로 돌아간다. */
    private var pendingItems: UsedItems = UsedItems.NONE

    private val _ui = MutableStateFlow(render())
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
        if (busy || autoJob != null) return
        val result = runAttempt(pendingItems) ?: return
        busy = true
        lastResult = result
        _ui.value = render()

        if (result is ForgeResult.Destroyed) openDestroyWindow()
    }

    /**
     * 강화 한 번을 실행하고 상태·통계·저장까지 마친다.
     *
     * 잠금과 연출은 건드리지 않는다. 손으로 누르는 [forge] 와 자동강화 루프가 함께 쓰기 때문이다.
     * 자동강화는 안전구간에서만 돌아 파괴가 나지 않으므로 창 처리도 필요 없다.
     */
    private fun runAttempt(items: UsedItems): ForgeResult? {
        if (!ForgeEngine.canAttempt(game, items)) return null
        val sword = game.sword ?: return null
        val targetLevel = sword.level + 1
        val cost = Economy.upgradeCost(sword.level)

        val result = ForgeEngine.attempt(game, items, rng)
        // 아이템은 한 번 쓰면 내려간다. 켜 둔 채 잊고 연타하면 순식간에 녹는다.
        pendingItems = UsedItems.NONE
        game = result.state
        progress = Progress.refresh(
            Progress.onAttempt(progress, game.difficulty, sword.family, targetLevel, cost, result),
        )
        persist()
        return result
    }

    /**
     * 파괴 직후 제한 시간 창을 연다.
     *
     * 방지권이 있으면 먼저 되살릴 기회를 주고, 없거나 놓치면 줍기로 넘어간다.
     */
    private fun openDestroyWindow() {
        // 자동사용이 켜져 있으면 창을 열지 않고 즉시 되살린다.
        if (settings.autoPrevent && ForgeEngine.canPrevent(game)) {
            game = ForgeEngine.applyPrevent(game)
            progress = Progress.refresh(Progress.onPreventUsed(progress))
            closeDestroyWindow()
            return
        }
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
        _ui.value = render()

        countdownJob = viewModelScope.launch {
            var remaining = totalMillis
            while (remaining > 0) {
                delay(Timing.TICK_MILLIS)
                remaining -= Timing.TICK_MILLIS
                phase = make(remaining.coerceAtLeast(0), totalMillis)
                _ui.value = render()
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

    /**
     * 자동강화를 시작한다.
     *
     * 안전구간을 벗어나거나 골드가 부족하면 스스로 멈춘다. 판정 조건은
     * [ForgeEngine.canAutoForge] 가 갖고 있다 — 화면도 여기도 단계를 직접 비교하지 않는다.
     */
    fun startAutoForge(targetLevel: Int) {
        if (autoJob != null || busy) return
        autoJob = viewModelScope.launch {
            while (
                ForgeEngine.canAutoForge(game) &&
                (game.sword?.level ?: 0) < targetLevel
            ) {
                runAttempt(UsedItems.NONE)
                _ui.value = render()
                delay(AUTO_FORGE_INTERVAL_MILLIS)
            }
            autoJob = null
            _ui.value = render()
        }
        _ui.value = render()
    }

    fun stopAutoForge() {
        autoJob?.cancel()
        autoJob = null
        _ui.value = render()
    }

    fun setAutoPrevent(on: Boolean) {
        settings = settings.copy(autoPrevent = on)
        store.saveSettings(settings)
        _ui.value = render()
    }

    /** 달성한 업적의 칭호만 고를 수 있다. null 이면 해제. */
    fun selectTitle(achievement: Achievement?) {
        progress = Progress.selectTitle(progress, achievement)
        store.saveProgress(progress)
        _ui.value = render()
    }

    // ---------------- 사냥 ----------------

    /**
     * 구역에 들어간다.
     *
     * 검이 없으면 들어갈 수 없다. 강화로 부서지면 사냥도 못 하게 되므로
     * 파괴의 무게가 커지고 방지권의 값어치가 오른다.
     */
    fun enterZone(zone: Zone) {
        if (busy || autoJob != null) return
        if (game.sword == null) return
        if (!game.adventure.isUnlocked(zone)) return

        stopHuntLoop()
        huntZone = zone
        game = game.copy(adventure = game.adventure.copy(zoneId = zone.id))
        bossFailed = false
        zoneCleared = false
        spawnNext()
        startHuntLoop()
        persist()
        _ui.value = render()
    }

    /** 사냥터를 나온다. */
    fun leaveHunt() {
        stopHuntLoop()
        huntZone = null
        combo = 0
        _ui.value = render()
    }

    /**
     * 몬스터를 한 번 친다.
     *
     * 계열마다 최소 탭 간격이 달라서 대검은 느리고 세검은 빠르다.
     * 간격을 못 채운 탭은 조용히 무시한다 — 눌렸는데 아무 일도 안 나는 게
     * 잘못된 피해가 들어가는 것보다 낫다.
     */
    fun tapTarget() {
        val zone = huntZone ?: return
        val sword = game.sword ?: return
        if (targetHp <= 0) return

        val now = System.currentTimeMillis()
        if (now - lastTapAt < Combat.minTapMillis(sword)) return
        lastTapAt = now

        val hit = Combat.hit(sword, combo, fightingBoss)
        combo++
        lastDamage = hit.damage
        lastHits = hit.hits
        targetHp -= hit.damage

        if (targetHp <= 0) onTargetDown(zone)
        _ui.value = render()
    }

    private fun onTargetDown(zone: Zone) {
        val sword = game.sword
        targetHp = 0
        combo = 0

        if (fightingBoss) {
            val shards = Combat.shardReward(sword, zone.bossShards)
            game = game.copy(
                gold = game.gold + zone.bossGold,
                shards = game.shards + shards,
                adventure = game.adventure.copy(
                    killsInZone = 0,
                    clearedZoneIds = game.adventure.clearedZoneIds + zone.id,
                ),
            )
            progress = Progress.refresh(Progress.onSell(progress, zone.bossGold))
            zoneCleared = true
            stopHuntLoop()
        } else {
            val shards = Combat.shardReward(sword, zone.monsterShards)
            game = game.copy(
                gold = game.gold + zone.monsterGold,
                shards = game.shards + shards,
                adventure = game.adventure.copy(killsInZone = game.adventure.killsInZone + 1),
            )
            progress = Progress.refresh(Progress.onSell(progress, zone.monsterGold))
            if (!game.adventure.bossReady) spawnNext()
        }
        persist()
    }

    /** 보스에 도전한다. 잡몹을 정해진 수만큼 잡아야 열린다. */
    fun challengeBoss() {
        val zone = huntZone ?: return
        if (!game.adventure.bossReady || game.sword == null) return
        fightingBoss = true
        bossFailed = false
        targetMaxHp = zone.bossHp
        targetHp = zone.bossHp
        bossRemainingMillis = zone.bossSeconds * 1000L
        combo = 0
        startHuntLoop()
        _ui.value = render()
    }

    private fun spawnNext() {
        val zone = huntZone ?: return
        fightingBoss = false
        targetMaxHp = zone.monsterHp
        targetHp = zone.monsterHp
        bossRemainingMillis = 0
        lastDamage = 0
        lastHits = 0
    }

    /**
     * 사냥터에서 1초마다 도는 루프.
     *
     * 용검의 화상 피해를 넣고, 보스전이면 제한 시간을 깎는다.
     */
    private fun startHuntLoop() {
        stopHuntLoop()
        huntJob = viewModelScope.launch {
            while (huntZone != null) {
                delay(HUNT_TICK_MILLIS)
                val zone = huntZone ?: break

                if (targetHp > 0) {
                    val burn = Combat.burnPerSecond(game.sword)
                    if (burn > 0) {
                        targetHp -= burn
                        if (targetHp <= 0) onTargetDown(zone)
                    }
                }

                if (fightingBoss && targetHp > 0) {
                    bossRemainingMillis -= HUNT_TICK_MILLIS
                    if (bossRemainingMillis <= 0) {
                        // 시간 안에 못 죽였다. 보스는 도망가고 잡몹부터 다시 모아야 한다.
                        bossRemainingMillis = 0
                        bossFailed = true
                        fightingBoss = false
                        game = game.copy(adventure = game.adventure.copy(killsInZone = 0))
                        spawnNext()
                        persist()
                    }
                }
                _ui.value = render()
            }
        }
    }

    private fun stopHuntLoop() {
        huntJob?.cancel()
        huntJob = null
    }

    private fun renderHunt(): HuntUiState? {
        val zone = huntZone ?: return null
        return HuntUiState(
            zone = zone,
            targetName = if (fightingBoss) zone.bossName else zone.monsterName,
            targetHp = targetHp.coerceAtLeast(0),
            targetMaxHp = targetMaxHp.coerceAtLeast(1),
            isBoss = fightingBoss,
            bossRemainingMillis = bossRemainingMillis,
            killsInZone = game.adventure.killsInZone,
            killsNeeded = Zone.MONSTERS_BEFORE_BOSS,
            attackPower = Combat.attackPower(game.sword),
            combo = combo,
            lastDamage = lastDamage,
            lastHits = lastHits,
            bossFailed = bossFailed,
            zoneCleared = zoneCleared,
        )
    }

    /** 다음 강화에 축복서를 쓸지 켜고 끈다. 보유량이 없으면 켜지지 않는다. */
    fun toggleBlessing() {
        if (busy) return
        val next = !pendingItems.blessing
        if (next && game.inventory.blessingScrolls <= 0) return
        pendingItems = pendingItems.copy(blessing = next)
        _ui.value = render()
    }

    /** 다음 강화에 행운부적을 쓸지 켜고 끈다. 보유량이 없으면 켜지지 않는다. */
    fun toggleLuckCharm() {
        if (busy) return
        val next = !pendingItems.luckCharm
        if (next && game.inventory.luckCharms <= 0) return
        pendingItems = pendingItems.copy(luckCharm = next)
        _ui.value = render()
    }

    fun buyItem(item: Item) {
        if (busy || !Economy.canBuyItem(game, item)) return
        game = Economy.buyItem(game, item)
        persist()
        _ui.value = render()
    }

    /** 조합소 교환. 검을 주는 교환일 때만 [family] 가 쓰인다. */
    fun craft(recipeId: String, family: WeaponFamily) {
        if (busy) return
        val recipe = Recipes.ALL.firstOrNull { it.id == recipeId } ?: return
        if (!Recipes.canCraft(game, recipe)) return
        game = Recipes.craft(game, recipe, family)
        game.sword?.let { progress = Progress.registerSword(progress, game.difficulty, it) }
        persist()
        _ui.value = render()
    }

    fun sellSword() {
        if (busy || !Economy.canSellSword(game)) return
        val price = Economy.sellPrice(game.sword?.level ?: 0)
        game = Economy.sellSword(game)
        progress = Progress.refresh(Progress.onSell(progress, price))
        applyBailout()
        persist()
        _ui.value = render()
    }

    fun buySword(family: WeaponFamily) {
        if (busy || !Economy.canBuySword(game)) return
        game = Economy.buySword(game, family)
        game.sword?.let { progress = Progress.registerSword(progress, game.difficulty, it) }
        persist()
        _ui.value = render()
    }

    /** 결과 연출이 끝났다고 화면이 알려 준다. 입력 잠금을 푼다. */
    fun onAnimationFinished() {
        if (!busy) return
        busy = false
        lastResult = null
        _ui.value = render()
    }

    private fun closeDestroyWindow() {
        countdownJob = null
        phase = DestroyPhase.None
        busy = false
        lastResult = null
        applyBailout()
        persist()
        _ui.value = render()
    }

    override fun onCleared() {
        countdownJob?.cancel()
        autoJob?.cancel()
        huntJob?.cancel()
        super.onCleared()
    }

    /** 사냥 진행을 읽기 전용으로 노출한다. 구역 선택 화면이 쓴다. */
    fun adventure(): AdventureState = game.adventure

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

    private companion object {
        /** 자동강화 한 번과 다음 번 사이의 간격. 화면이 따라올 만큼은 둔다. */
        const val AUTO_FORGE_INTERVAL_MILLIS = 220L

        /** 사냥 루프 주기. 화상 피해와 보스 제한 시간을 이 간격으로 처리한다. */
        const val HUNT_TICK_MILLIS = 1_000L
    }

    private fun render(): ForgeUiState {
        val sword = game.sword
        val level = sword?.level ?: 0
        val targetLevel = level + 1
        return ForgeUiState(
            difficulty = game.difficulty,
            sword = sword,
            gold = game.gold,
            shards = game.shards,
            preventTickets = game.inventory.preventTickets,
            blessingScrolls = game.inventory.blessingScrolls,
            luckCharms = game.inventory.luckCharms,
            bestLevel = game.bestLevel,
            upgradeCost = Economy.upgradeCost(level),
            sellPrice = Economy.sellPrice(level),
            successPercent = (
                RateTable.successRate(game.difficulty, targetLevel, pendingItems.blessing) * 100
                ).roundToInt(),
            canForge = !busy && ForgeEngine.canAttempt(game, pendingItems),
            canBuySword = !busy && Economy.canBuySword(game),
            unlockedFamilies = Progress.unlockedFamilies(progress),
            useBlessing = pendingItems.blessing,
            useLuckCharm = pendingItems.luckCharm,
            progress = progress,
            settings = settings,
            autoForging = autoJob != null,
            canAutoForge = ForgeEngine.canAutoForge(game),
            hunt = renderHunt(),
            attackPower = Combat.attackPower(game.sword),
            lastResult = lastResult,
            destroyPhase = phase,
            canPrevent = ForgeEngine.canPrevent(game),
            busy = busy,
        )
    }
}
