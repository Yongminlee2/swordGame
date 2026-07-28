package com.geomgang.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geomgang.core.Difficulty
import com.geomgang.core.Economy
import com.geomgang.core.ForgeCost
import com.geomgang.core.ForgeEngine
import com.geomgang.core.ForgeMarks
import com.geomgang.core.ForgeOdds
import com.geomgang.core.ForgeResult
import com.geomgang.core.Fusion
import com.geomgang.core.GauntletBuff
import com.geomgang.core.GauntletEngine
import com.geomgang.core.GauntletRun
import com.geomgang.core.GoldShop
import com.geomgang.core.MaterialBoost
import com.geomgang.core.StarForce
import com.geomgang.core.GameState
import com.geomgang.core.Item
import com.geomgang.core.MonsterKind
import com.geomgang.core.PetKind
import com.geomgang.core.Pets
import com.geomgang.core.Progress
import com.geomgang.core.ProgressState
import com.geomgang.core.RateTable
import com.geomgang.core.Tempering
import com.geomgang.core.Achievement
import com.geomgang.core.AdventureState
import com.geomgang.core.Combat
import com.geomgang.core.DailyQuests
import com.geomgang.core.HuntEvent
import com.geomgang.core.HuntEvents
import com.geomgang.core.HuntRetry
import com.geomgang.core.IdleReward
import com.geomgang.core.IdleRewards
import com.geomgang.core.QuestKind
import com.geomgang.core.Recipes
import com.geomgang.core.SaveStore
import com.geomgang.core.Settings
import com.geomgang.core.Skill
import com.geomgang.core.Skills
import com.geomgang.core.Storage
import com.geomgang.core.Sword
import com.geomgang.core.SwordDrop
import com.geomgang.core.Timing
import com.geomgang.core.UniqueSwords
import com.geomgang.core.UsedItems
import com.geomgang.core.WeaponFamily
import com.geomgang.core.Zone
import com.geomgang.game.feel.HapticEngine
import com.geomgang.game.sound.SoundEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.roundToLong
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
    /** 소리. 테스트에서는 넘기지 않으므로 아무 일도 하지 않는 기본값을 둔다. */
    private val sound: SoundEngine = SoundEngine { false },
    /** 진동. 소리와 같은 이유로 아무 일도 하지 않는 기본값을 둔다. */
    private val haptics: HapticEngine = HapticEngine(null) { false },
    /** 지금 시각. 자리비움 보상만 쓴다 — 테스트가 시계를 직접 쥐어야 해서 밖에서 넣는다. */
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private var progress: ProgressState = store.loadProgress()

    private var settings: Settings = store.loadSettings()

    /** 이번에 켜면서 받은 자리비움 보상. 화면이 알리고 나면 비운다. */
    private var idleReward: IdleReward? = null

    private var game: GameState = loadAndRepair(difficulty)

    private var busy = false

    private var phase: DestroyPhase = DestroyPhase.None

    private var countdownJob: Job? = null

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
    private var lastCrit = false

    /** 방금 터진 계열 스킬. 화면이 이름을 크게 띄운다. */
    private var lastSkill: Skill? = null
    private var hitSeq = 0L
    private var lastKillGold = 0L
    private var bossFailed = false
    private var zoneCleared = false

    /**
     * 이 구역에서 골드를 내고 재도전한 횟수. 낼 값이 매번 2배가 된다([HuntRetry]).
     *
     * 구역을 나가거나 보스를 잡으면 0으로 돌아간다 — 안 그러면 다음 구역까지
     * 값이 따라와서 "왜 처음부터 비싸지" 가 된다.
     */
    private var bossRetries = 0

    /** 방금 보스에게서 얻은 것. 승리 팝업이 읽고 나면 비운다. */
    private var bossReward: BossReward? = null
    private var huntJob: Job? = null
    private var targetKind: MonsterKind? = null

    /** 지금 대상이 희귀 몬스터인지. 체력은 조금, 보상은 크게 오른다. */
    private var rareTarget = false

    // --- 사냥 이벤트 ---
    // 판정·수식은 HuntEvents(:core), 시간은 여기서 센다.
    private var activeEvent: HuntEvent? = null
    private var eventRemainingMillis = 0L
    private var goldenRemainingMillis = 0L
    private var merchantRemainingMillis = 0L
    private var merchantOffer: Item? = null
    private var nuggetRemainingMillis = 0L
    private var nuggetsLeft = 0

    /** 방금 떨어진 검. 화면이 알린 뒤 비운다. */
    private var lastDrop: Sword? = null

    /** 방금 얻은 펫 알. 화면이 알린 뒤 비운다. */
    private var lastEgg: PetKind? = null

    /** 보관함이 꽉 차서 드롭을 놓쳤는지. */
    private var dropMissed = false

    /** 마지막 별 강화가 성공했는지. 화면이 알린 뒤 비운다. */
    private var lastStarUp: Boolean? = null

    /**
     * 마지막 강화 결과. 연출이 끝나거나 파괴 창이 닫힐 때까지 유지한다.
     *
     * 카운트다운이 매 틱마다 화면을 다시 그리는데, 여기서 결과를 들고 있지 않으면
     * "파괴!!" 배너가 첫 틱에 사라져 버린다.
     */
    private var lastResult: ForgeResult? = null

    /** 이번 성공이 최고 기록을 넘었는지. 연출이 끝나면 내려간다. */
    private var lastWasRecord: Boolean = false

    /** 다음 강화에 쓸 아이템. 한 번 쓰면 [UsedItems.NONE] 으로 돌아간다. */
    private var pendingItems: UsedItems = UsedItems.NONE

    private val _ui = MutableStateFlow(render())
    val ui: StateFlow<ForgeUiState> = _ui.asStateFlow()

    init {
        // 하루가 지나 있으면 첫 화면부터 새 퀘스트가 보여야 한다.
        refreshQuests()
        _ui.value = render()
    }

    /**
     * 세이브를 불러오면서 두 가지를 손본다.
     *
     * 1. 파괴 대기 상태가 남아 있으면 확정 처리한다. 방지권 원이 떠 있는 동안 앱을 죽여
     *    파괴를 무효화하는 악용을 막는 지점이다. 기회를 놓친 것으로 통계에 남긴다.
     * 2. 아무것도 할 수 없는 상태면 파산 구제를 적용한다.
     * 3. 자리를 비운 사이 쌓인 보상을 넣는다.
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
            loaded = rescued
        }

        idleReward = idleRewardFor(loaded)
        idleReward?.let { loaded = IdleRewards.apply(loaded, it) }

        // 여기서 곧바로 저장한다. 보상을 넣어 놓고 저장을 미루면 앱이 죽었을 때
        // 같은 시간을 두 번 받는다.
        loaded = loaded.copy(lastSeenMillis = now())
        store.saveGame(loaded)
        return loaded
    }

    /**
     * 자리를 비운 시간만큼의 보상. 받을 게 없으면 null.
     *
     * 시각이 남아 있지 않은 세이브(이 기능이 없던 시절 것)에는 주지 않는다 —
     * 얼마나 비웠는지 알 수 없는데 그걸 "오래 비웠다"로 읽으면 공짜 보상이 된다.
     */
    private fun idleRewardFor(state: GameState): IdleReward? {
        if (state.lastSeenMillis <= 0) return null
        val elapsedSeconds = (now() - state.lastSeenMillis) / 1000
        return IdleRewards.rewardFor(state, elapsedSeconds)
    }

    /** 자리비움 안내를 닫는다. 보상은 이미 들어가 있고 이 호출은 알림만 끈다. */
    fun dismissIdleReward() {
        if (idleReward == null) return
        idleReward = null
        _ui.value = render()
    }

    fun forge() {
        if (busy) return
        val result = runAttempt(pendingItems) ?: return
        busy = true
        lastResult = result
        _ui.value = render()

        // 소리와 진동은 늘 나란히 간다. 한쪽만 울리면 손과 귀가 다른 말을 한다.
        when (result) {
            is ForgeResult.Success ->
                if (lastWasRecord) {
                    sound.newRecord()
                    haptics.newRecord()
                } else {
                    sound.forgeSuccess(result.newLevel)
                    haptics.forgeSuccess(result.newLevel)
                }

            is ForgeResult.Stay -> {
                sound.forgeStay()
                haptics.forgeStay()
            }

            is ForgeResult.Drop -> {
                sound.forgeDrop()
                haptics.forgeDrop()
            }

            is ForgeResult.Destroyed -> {
                sound.forgeDestroy()
                haptics.forgeDestroy()
            }
        }

        if (result is ForgeResult.Destroyed) openDestroyWindow()
    }

    /**
     * 강화 한 번을 실행하고 상태·통계·저장까지 마친다.
     *
     * 잠금과 연출은 건드리지 않는다 — 그 둘은 [forge] 의 몫이다. 이 함수는
     * "한 번 굴린 결과"만 만들어 상태·통계·저장에 반영한다.
     */
    private fun runAttempt(items: UsedItems): ForgeResult? {
        if (!ForgeEngine.canAttempt(game, items)) return null
        val sword = game.sword ?: return null
        val targetLevel = sword.level + 1
        val cost = Economy.upgradeCost(sword.level)
        val req = ForgeCost.requirementFor(sword.level)

        // 강화석은 성패와 무관하게 소모된다. 판정 전에 먼저 뺀다.
        if (req.stones > 0) {
            game = game.copy(forgeStones = game.forgeStones - req.stones)
        }

        val bestBefore = game.bestLevel

        val result = ForgeEngine.attempt(game, items, rng)
        // 아이템은 한 번 쓰면 내려간다. 켜 둔 채 잊고 연타하면 순식간에 녹는다.
        pendingItems = UsedItems.NONE
        // 최고 단계가 올랐으면 상점 누진을 푼다. 리셋이 일어나는 유일한 지점이다.
        game = GoldShop.rebase(result.state)
        // 자취를 한 칸 민다. 결과가 뜨고 사라지면 연패가 이야기로 남지 않는다.
        game = game.copy(recentMarks = ForgeMarks.push(game.recentMarks, ForgeMarks.of(result)))
        lastWasRecord = result is ForgeResult.Success &&
            result.newLevel > bestBefore &&
            result.newLevel >= MIN_RECORD_LEVEL
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
        sound.preventUsed()
        haptics.preventUsed()
        closeDestroyWindow()
    }

    /** 파편을 주워 조각을 얻는다. */
    fun salvage() {
        if (game.pendingDestroy == null) return
        countdownJob?.cancel()
        val before = game.shards
        game = ForgeEngine.applySalvage(game, rng)
        progress = Progress.refresh(Progress.onSalvage(progress, game.shards - before))
        sound.salvage()
        closeDestroyWindow()
    }

    fun setAutoPrevent(on: Boolean) {
        settings = settings.copy(autoPrevent = on)
        store.saveSettings(settings)
        _ui.value = render()
    }

    fun setSoundOn(on: Boolean) {
        settings = settings.copy(soundOn = on)
        store.saveSettings(settings)
        _ui.value = render()
        if (on) sound.purchase()
    }

    fun setHapticsOn(on: Boolean) {
        settings = settings.copy(hapticsOn = on)
        store.saveSettings(settings)
        _ui.value = render()
        // 켜는 순간 한 번 울려 준다 - 무엇을 켰는지 손으로 확인된다.
        if (on) haptics.forgeSuccess(0)
    }

    /** 소리를 켤지 판단할 때 쓴다. 설정이 바뀌면 즉시 반영된다. */
    fun soundEnabled(): Boolean = settings.soundOn

    /** 진동을 울릴지 판단할 때 쓴다. 설정이 바뀌면 즉시 반영된다. */
    fun hapticsEnabled(): Boolean = settings.hapticsOn

    /** 이 모드의 진행을 지운다. 도감·업적·통계·설정은 남는다. */
    fun resetProgress() {
        stopHuntLoop()
        countdownJob?.cancel()
        val difficulty = game.difficulty
        store.resetGame(difficulty)
        // 갓 지운 세이브는 골드 0에 검도 없다. 그대로 두면 검을 살 수도, 강화할 수도 없어
        // 아무것도 못 하는 판이 된다. 새로 깐 앱과 똑같은 길([loadAndRepair])을 타게 해서
        // 파산 구제가 시작 자금을 채우게 한다.
        game = loadAndRepair(difficulty)
        huntZone = null
        phase = DestroyPhase.None
        lastResult = null
        busy = false
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
        if (busy) return
        if (game.sword == null) return
        if (!game.adventure.isUnlocked(zone)) return

        stopHuntLoop()
        huntZone = zone
        game = game.copy(adventure = game.adventure.copy(zoneId = zone.id))
        bossFailed = false
        zoneCleared = false
        // 구역이 바뀌면 재도전 값도 처음으로 돌아간다.
        bossRetries = 0
        bossReward = null
        refreshQuests() // 자정을 넘겨 계속 켜 둔 경우를 여기서 따라잡는다
        spawnNext()
        startHuntLoop()
        persist()
        _ui.value = render()
    }

    /** 사냥터를 나온다. 이벤트·버프는 사냥터에 두고 나온다. */
    fun leaveHunt() {
        stopHuntLoop()
        huntZone = null
        combo = 0
        bossRetries = 0
        bossReward = null
        activeEvent = null
        eventRemainingMillis = 0
        goldenRemainingMillis = 0
        merchantRemainingMillis = 0
        merchantOffer = null
        nuggetRemainingMillis = 0
        nuggetsLeft = 0
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

        // 펫 치명타 보너스는 롤 값에서 빼는 방식이다 - 문턱을 넓히는 것과 같다
        val critRoll = rng.nextDouble() - Pets.critBonusOf(game.pets)
        // 난수 소비 순서 계약: 치명타 -> 스킬
        // 펫 스킬 보너스도 롤 값에서 뺀다 - 발동 문턱을 넓히는 것과 같다
        val skillRoll = if (Skills.unlocked(sword)) {
            rng.nextDouble() - Pets.skillBonusOf(game.pets)
        } else {
            1.0
        }
        val hit = Combat.hit(sword, combo, fightingBoss, critRoll, targetMaxHp, skillRoll)
        combo++
        lastDamage = hit.damage
        lastHits = hit.hits
        lastCrit = hit.crit
        lastSkill = hit.skill
        hitSeq++
        targetHp -= hit.damage

        // 스킬 부가 효과. 화상 폭발은 즉시 피해로, 흡혈은 조각으로 들어온다.
        hit.skill?.let { skill ->
            if (skill.burnBurst) {
                targetHp -= Combat.burnPerSecond(sword) * Skills.BURN_BURST_MULT
            }
            if (skill.shardBonus > 0) {
                game = game.copy(shards = game.shards + skill.shardBonus)
            }
            progress = Progress.onSkill(progress)
        }

        if (fightingBoss) sound.bossHit(combo) else sound.hit(combo)

        if (targetHp <= 0) onTargetDown(zone)
        _ui.value = render()
    }

    private fun onTargetDown(zone: Zone) {
        val sword = game.sword
        targetHp = 0
        combo = 0

        if (fightingBoss) {
            val golden = if (goldenRemainingMillis > 0) HuntEvents.GOLDEN_MULT else 1.0
            val bossGold =
                (zone.bossGold * golden * UniqueSwords.goldMultOf(sword)).toLong()
            val shards = Combat.shardReward(sword, (zone.bossShards * golden).toInt())
            lastKillGold = bossGold
            game = game.copy(
                gold = game.gold + bossGold,
                shards = game.shards + shards,
                adventure = game.adventure.copy(
                    killsInZone = 0,
                    clearedZoneIds = game.adventure.clearedZoneIds + zone.id,
                ),
                // 보스는 자기 구역의 정수를 남긴다. 고유검 레시피의 재료다.
                essences = game.essences +
                    (zone.id to (game.essences[zone.id] ?: 0) + 1),
                // 강화석도 확정으로 준다 - 고단계 강화의 화폐라 보스가 주 공급원이다.
                forgeStones = game.forgeStones + zone.bossStones,
            )
            progress = Progress.refresh(
                Progress.onStones(
                    Progress.onZoneCleared(
                        Progress.onMonsterKill(Progress.onSell(progress, bossGold), isBoss = true),
                        zone.id,
                    ),
                    zone.bossStones,
                ),
            )
            rollDrop(zone, isBoss = true)
            // 보스는 낮은 확률로 자기 구역 펫의 알을 떨어뜨린다 (드롭 판정 뒤 난수 1개)
            val eggBefore = lastEgg
            if (rng.nextDouble() < Pets.EGG_DROP_CHANCE) {
                grantEgg(zone)
            }
            // 얻은 것을 한 덩어리로 모아 승리 팝업에 넘긴다. 지금까지는 전부 조용히
            // 들어가서 무엇을 벌었는지 알 수 없었다.
            bossReward = BossReward(
                gold = bossGold,
                shards = shards,
                stones = zone.bossStones,
                petName = if (lastEgg !== eggBefore) lastEgg?.displayName else null,
            )
            // 이 구역은 끝났다. 다음 구역에 재도전 값이 따라가면 안 된다.
            bossRetries = 0
            zoneCleared = true
            sound.zoneCleared()
            stopHuntLoop()
        } else {
            val kind = targetKind ?: zone.monsters.first()
            val bonus = if (rareTarget) Zone.RARE_REWARD else 1.0
            val eventMult = HuntEvents.rewardMultOf(activeEvent)
            val golden = if (goldenRemainingMillis > 0) HuntEvents.GOLDEN_MULT else 1.0
            val gold = (
                zone.goldOf(kind) * bonus * eventMult * golden *
                    UniqueSwords.goldMultOf(sword) * Pets.goldMultOf(game.pets)
                ).toLong()
            // 수상한 알: 낮은 확률로 진짜 펫 알, 아니면 조각 잭팟 (난수 1개)
            var eggShards = 0
            if (activeEvent == HuntEvent.STRANGE_EGG) {
                if (rng.nextDouble() < Pets.EGG_EVENT_CHANCE) {
                    grantEgg(zone)
                } else {
                    eggShards = HuntEvents.EGG_SHARDS
                }
            }
            val shards = (
                Combat.shardReward(sword, (kind.shards * bonus * golden).toInt()) *
                    Pets.shardMultOf(game.pets)
                ).toInt() + eggShards
            lastKillGold = gold
            game = game.copy(
                gold = game.gold + gold,
                shards = game.shards + shards,
                adventure = game.adventure.copy(killsInZone = game.adventure.killsInZone + 1),
            )
            progress = Progress.refresh(
                Progress.onMonsterKill(Progress.onSell(progress, gold), isBoss = false),
            )
            sound.monsterDown()
            haptics.monsterDown()
            // 미믹은 드롭 확정 + 보스급 단계 보정. "잡을까 말까"의 답이다.
            rollDrop(zone, isBoss = activeEvent == HuntEvent.MIMIC)
            // 강화석은 검 드롭 판정 뒤에 굴린다 (난수 소비 순서 계약)
            if (rng.nextDouble() < ForgeCost.MOB_STONE_CHANCE + Pets.stoneBonusOf(game.pets)) {
                game = game.copy(forgeStones = game.forgeStones + 1)
                progress = Progress.onStones(progress, 1)
            }
            activeEvent = null
            eventRemainingMillis = 0
            if (!game.adventure.bossReady) spawnNext()
        }
        persist()
    }

    /** 보스에 도전한다. 잡몹을 정해진 수만큼 잡아야 열린다. */
    fun challengeBoss() {
        val zone = huntZone ?: return
        if (!game.adventure.bossReady || game.sword == null) return
        startBossFight(zone)
    }

    /**
     * 골드를 내고 **즉시** 다시 도전한다.
     *
     * 잡몹을 다시 모으지 않는다 — 낮추려는 것은 재도전 문턱이지 5초의 긴장이 아니다.
     */
    fun retryBoss() {
        val zone = huntZone ?: return
        if (!bossFailed || game.sword == null) return
        if (!HuntRetry.canRetry(game.gold, zone, bossRetries)) return

        game = game.copy(gold = game.gold - HuntRetry.priceOf(zone, bossRetries))
        bossRetries++
        // 놓칠 때 잡몹 진행이 지워졌으므로 보스가 다시 나오도록 되돌린다.
        game = game.copy(
            adventure = game.adventure.copy(killsInZone = Zone.MONSTERS_BEFORE_BOSS),
        )
        persist()
        startBossFight(zone)
    }

    /**
     * 승리 팝업을 닫고 이 구역에 남는다. 잡몹부터 다시 모아 재료를 캔다.
     *
     * 보스를 잡을 때 `killsInZone` 이 이미 0이 되므로 여기서는 알림만 끄면 된다.
     */
    fun stayInZone() {
        if (!zoneCleared) return
        zoneCleared = false
        bossReward = null
        spawnNext()
        _ui.value = render()
    }

    /**
     * 다음 구역으로 곧바로 넘어간다.
     *
     * 예전에는 승리 팝업의 이 버튼이 [leaveHunt] 를 불러 사냥터 목록을 열었다.
     * "다음 구역으로" 라고 써 놓고 첫 화면으로 돌려보내면 방금 이긴 흐름이 거기서 끊긴다.
     *
     * 다음 구역이 없거나 아직 잠겨 있을 때만 목록으로 돌아간다 — 그때는 정말
     * 갈 곳이 없으므로 고를 화면을 보여 주는 편이 낫다.
     */
    fun nextZone() {
        if (!zoneCleared) return
        val current = huntZone ?: return
        zoneCleared = false
        bossReward = null
        val next = Zone.entries.getOrNull(current.ordinal + 1)
        if (next == null || !game.adventure.isUnlocked(next)) {
            leaveHunt()
            return
        }
        enterZone(next)
    }

    /** 패배 팝업을 닫고 사냥터를 나간다. 대가를 안 냈으므로 잡몹 진행은 이미 지워졌다. */
    fun giveUpBoss() {
        if (!bossFailed) return
        bossFailed = false
        leaveHunt()
    }

    private fun startBossFight(zone: Zone) {
        fightingBoss = true
        bossFailed = false
        targetMaxHp = zone.bossHp
        targetHp = zone.bossHp
        bossRemainingMillis = zone.bossSeconds * 1000L + Pets.bossTimeBonusMillis(game.pets)
        combo = 0
        startHuntLoop()
        _ui.value = render()
    }

    private fun spawnNext() {
        val zone = huntZone ?: return
        fightingBoss = false

        // 구역마다 몬스터가 여러 종류다. 한 종류만 계속 잡으면 금방 지루해진다.
        val kind = zone.monsterFor(rng.nextInt(1_000))
        rareTarget = rng.nextDouble() < Zone.RARE_CHANCE + Pets.rareBonusOf(game.pets)

        // 이벤트. 지속 효과(골든타임·상인·금덩이)가 살아 있으면 새로 굴리지 않는다 -
        // 변수가 겹치면 어느 것도 특별하지 않게 된다.
        // 난수 소비 순서 계약: ①몬스터 ②희귀 ③이벤트 발생 ④종류 (⑤상인이면 아이템)
        activeEvent = null
        eventRemainingMillis = 0
        val buffActive =
            goldenRemainingMillis > 0 || merchantRemainingMillis > 0 || nuggetsLeft > 0
        if (!buffActive) {
            val event = if (rng.nextDouble() < HuntEvents.CHANCE + Pets.eventBonusOf(game.pets)) {
                HuntEvents.pick(rng.nextDouble())
            } else {
                null
            }
            if (event != null) {
                progress = Progress.refresh(Progress.onEventSeen(progress))
                sound.eventAppear()
                when {
                    HuntEvents.isMonsterEvent(event) -> {
                        activeEvent = event
                        if (event == HuntEvent.TREASURE) {
                            eventRemainingMillis = HuntEvents.TREASURE_SECONDS * 1000L
                        }
                    }
                    event == HuntEvent.GOLDEN_TIME ->
                        goldenRemainingMillis = HuntEvents.GOLDEN_SECONDS * 1000L
                    event == HuntEvent.MERCHANT -> {
                        merchantRemainingMillis = HuntEvents.MERCHANT_SECONDS * 1000L
                        merchantOffer = Item.entries[rng.nextInt(Item.entries.size)]
                    }
                    event == HuntEvent.GOLD_NUGGET -> {
                        nuggetsLeft = 1
                        nuggetRemainingMillis = HuntEvents.NUGGET_SECONDS * 1000L
                    }
                    event == HuntEvent.METEOR_SHOWER -> {
                        nuggetsLeft = HuntEvents.METEOR_COUNT
                        nuggetRemainingMillis = HuntEvents.NUGGET_SECONDS * 1000L
                    }
                }
            }
        }

        val hpMult =
            (if (rareTarget) Zone.RARE_HP else 1.0) * HuntEvents.hpMultOf(activeEvent)

        targetKind = kind
        targetMaxHp = (zone.hpOf(kind) * hpMult).toLong().coerceAtLeast(1)
        targetHp = targetMaxHp
        bossRemainingMillis = 0
        lastDamage = 0
        lastHits = 0
        // hitSeq 는 리셋하지 않는다 - 화면이 팝업 키로 쓰므로 되돌리면 충돌한다.
        // lastKillGold 도 리셋하지 않는다 - 처치 직후 스폰되므로 화면이 아직 그리는 중이다.
        lastCrit = false
        lastSkill = null
    }

    // ---------------- 무한 회랑 ----------------

    private var gauntletRun: GauntletRun? = null
    private var gauntletJob: Job? = null
    private var gauntletTapAt = 0L

    /** 화산 보스를 잡아야 회랑이 열린다. 중반부터 열려야 컨텐츠로 산다. */
    fun gauntletUnlocked(): Boolean = Zone.VOLCANO.id in game.adventure.clearedZoneIds

    fun enterGauntlet() {
        if (busy || huntZone != null) return
        if (game.sword == null || !gauntletUnlocked()) return
        gauntletRun = GauntletEngine.start()
        gauntletJob?.cancel()
        gauntletJob = viewModelScope.launch {
            while (gauntletRun != null && gauntletRun?.over != true) {
                delay(HUNT_TICK_MILLIS)
                gauntletRun = gauntletRun?.let { GauntletEngine.tick(it, HUNT_TICK_MILLIS) }
                _ui.value = render()
            }
        }
        _ui.value = render()
    }

    fun tapGauntlet() {
        val run = gauntletRun ?: return
        if (run.over || run.choosing) return
        val sword = game.sword ?: return
        val now = System.currentTimeMillis()
        if (now - gauntletTapAt < Combat.minTapMillis(sword)) return
        gauntletTapAt = now

        val critBuff = if (GauntletBuff.CRIT in run.buffs) GauntletEngine.CRIT_BUFF else 0.0
        val critRoll = rng.nextDouble() - Pets.critBonusOf(game.pets) - critBuff
        // 펫 스킬 보너스도 롤 값에서 뺀다 - 발동 문턱을 넓히는 것과 같다
        val skillRoll = if (Skills.unlocked(sword)) {
            rng.nextDouble() - Pets.skillBonusOf(game.pets)
        } else {
            1.0
        }
        val hit = Combat.hit(sword, 0, run.isBossFloor, critRoll, run.monsterMaxHp, skillRoll)
        lastSkill = hit.skill
        var next = GauntletEngine.damage(run, hit.damage)

        if (next.choosing && next.choices.isEmpty()) {
            // 층을 깼다 - 갈림길을 굴리고 마일스톤(허검·정령 알·기록)을 적용한다
            next = next.copy(choices = GauntletEngine.rollChoices(next.floor, rng))
            game = GauntletEngine.applyMilestones(game, next.floor)
            progress = Progress.refresh(Progress.onGauntletFloor(progress, next.floor))
            // 보스 층은 체크포인트 - 보상 확정을 소리로 알린다
            if (run.isBossFloor) sound.gauntletCheckpoint() else sound.zoneCleared()
            persist()
        } else if (run.isBossFloor) {
            sound.bossHit(1)
        } else {
            sound.hit(1)
        }
        gauntletRun = next
        _ui.value = render()
    }

    fun chooseGauntlet(index: Int) {
        val run = gauntletRun ?: return
        if (!run.choosing) return
        gauntletRun = GauntletEngine.choose(run, index)
        sound.purchase()
        _ui.value = render()
    }

    /** 회랑을 나온다. 정산: 확정 전액 + 미확정 70% (회랑의 정령이 배수를 곱한다). */
    fun leaveGauntlet() {
        val run = gauntletRun ?: return
        val (gold, shards) = GauntletEngine.payout(run)
        val mult = Pets.gauntletMultOf(game.pets)
        val finalGold = (gold * mult).toLong()
        val finalShards = (shards * mult).toInt()
        game = game.copy(gold = game.gold + finalGold, shards = game.shards + finalShards)
        progress = Progress.refresh(Progress.onSell(progress, finalGold))
        gauntletJob?.cancel()
        gauntletJob = null
        gauntletRun = null
        refreshQuests()
        persist()
        _ui.value = render()
    }

    private fun renderGauntlet(): GauntletUiState? {
        val run = gauntletRun ?: return null
        return GauntletUiState(
            floor = run.floor,
            kills = run.killsInFloor,
            waveSize = run.waveSize,
            timeLeftMillis = run.timeLeftMillis,
            monsterHp = run.monsterHp.coerceAtLeast(0),
            monsterMaxHp = run.monsterMaxHp.coerceAtLeast(1),
            isBossFloor = run.isBossFloor,
            cursed = run.cursed,
            buffs = run.buffs,
            choosing = run.choosing,
            choices = run.choices,
            pendingGold = run.pendingGold,
            pendingShards = run.pendingShards,
            bankedGold = run.bankedGold,
            bankedShards = run.bankedShards,
            over = run.over,
            best = game.gauntletBest,
        )
    }

    /** 이 구역의 펫 알을 준다. */
    private fun grantEgg(zone: Zone) {
        val pet = PetKind.byZone(zone.id) ?: return
        game = game.copy(pets = Pets.addEgg(game.pets, pet.id))
        // 수집 기록은 전역에 남는다 - 모드를 초기화해도 모은 것은 남아야 한다.
        progress = Progress.refresh(Progress.onPetFound(progress, pet.id))
        lastEgg = pet
        sound.eggGet()
    }

    /** 펫 알 알림을 화면이 읽은 뒤 비운다. */
    fun clearEggNotice() {
        if (lastEgg == null) return
        lastEgg = null
        _ui.value = render()
    }

    /** 펫을 장착하거나(소유한 것만) 해제한다(null). */
    fun equipPet(id: String?) {
        if (busy) return
        game = game.copy(pets = Pets.equip(game.pets, id))
        persist()
        _ui.value = render()
    }

    /** 금덩이를 탭한다. 골든타임이면 그것도 2배다. */
    fun tapNugget() {
        val zone = huntZone ?: return
        if (nuggetsLeft <= 0) return
        val golden = if (goldenRemainingMillis > 0) HuntEvents.GOLDEN_MULT else 1.0
        val gold = (HuntEvents.nuggetGold(zone) * golden).toLong()
        game = game.copy(gold = game.gold + gold)
        progress = Progress.refresh(Progress.onSell(progress, gold))
        nuggetsLeft--
        nuggetRemainingMillis =
            if (nuggetsLeft > 0) HuntEvents.NUGGET_SECONDS * 1000L else 0
        sound.purchase()
        persist()
        _ui.value = render()
    }

    /** 떠돌이 상인에게서 할인가로 산다. 한 번 사면 상인은 떠난다. */
    fun buyMerchantOffer() {
        val offer = merchantOffer ?: return
        if (merchantRemainingMillis <= 0) return
        val price = (Economy.priceOf(offer) * (1.0 - HuntEvents.MERCHANT_DISCOUNT))
            .roundToLong()
        if (game.gold < price) return
        game = game.copy(
            gold = game.gold - price,
            inventory = game.inventory.plus(offer, 1),
        )
        merchantOffer = null
        merchantRemainingMillis = 0
        sound.purchase()
        persist()
        _ui.value = render()
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

                // 펫 자동 타격 (쿼카·아기 용)
                if (targetHp > 0) {
                    val petDps =
                        (Combat.attackPower(game.sword) * Pets.autoTapRatio(game.pets)).toLong()
                    if (petDps > 0) {
                        targetHp -= petDps
                        if (targetHp <= 0) onTargetDown(zone)
                    }
                }

                // --- 이벤트 타이머 ---
                if (goldenRemainingMillis > 0) {
                    goldenRemainingMillis =
                        (goldenRemainingMillis - HUNT_TICK_MILLIS).coerceAtLeast(0)
                }
                if (merchantRemainingMillis > 0) {
                    merchantRemainingMillis =
                        (merchantRemainingMillis - HUNT_TICK_MILLIS).coerceAtLeast(0)
                    if (merchantRemainingMillis <= 0) merchantOffer = null
                }
                if (nuggetsLeft > 0) {
                    nuggetRemainingMillis -= HUNT_TICK_MILLIS
                    if (nuggetRemainingMillis <= 0) {
                        // 못 누른 금덩이는 사라진다. 유성우면 다음 금덩이가 떨어진다.
                        nuggetsLeft--
                        nuggetRemainingMillis =
                            if (nuggetsLeft > 0) HuntEvents.NUGGET_SECONDS * 1000L else 0
                    }
                }
                if (activeEvent == HuntEvent.TREASURE && targetHp > 0) {
                    eventRemainingMillis -= HUNT_TICK_MILLIS
                    if (eventRemainingMillis <= 0) {
                        // 시간 안에 못 잡았다. 보물 몬스터는 도망간다.
                        activeEvent = null
                        eventRemainingMillis = 0
                        spawnNext()
                    }
                }

                if (fightingBoss && targetHp > 0) {
                    bossRemainingMillis -= HUNT_TICK_MILLIS
                    if (bossRemainingMillis <= 0) {
                        // 시간 안에 못 죽였다. 보스는 도망가고 잡몹부터 다시 모아야 한다.
                        bossRemainingMillis = 0
                        bossFailed = true
                        fightingBoss = false
                        sound.bossFailed()
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
        val rawName = when {
            fightingBoss -> zone.bossName
            else -> targetKind?.name ?: zone.monsters.first().name
        }
        return HuntUiState(
            zone = zone,
            targetName = if (rareTarget && !fightingBoss) "희귀 $rawName" else rawName,
            rawTargetName = rawName,
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
            lastCrit = lastCrit,
            lastSkill = lastSkill,
            hitSeq = hitSeq,
            isRare = rareTarget && !fightingBoss,
            lastKillGold = lastKillGold,
            bossFailed = bossFailed,
            zoneCleared = zoneCleared,
            retryPrice = HuntRetry.priceOf(zone, bossRetries),
            canRetry = !busy && HuntRetry.canRetry(game.gold, zone, bossRetries),
            bossReward = bossReward,
            event = activeEvent,
            eventRemainingMillis = eventRemainingMillis,
            goldenRemainingMillis = goldenRemainingMillis,
            merchantOffer = if (merchantRemainingMillis > 0) merchantOffer else null,
            merchantPrice = merchantOffer?.let {
                (Economy.priceOf(it) * (1.0 - HuntEvents.MERCHANT_DISCOUNT)).roundToLong()
            } ?: 0,
            nugget = nuggetsLeft > 0,
        )
    }

    /**
     * 다음 강화에 축복서를 쓸지 켜고 끈다. **켜면 부적이 내려간다.**
     *
     * 배타는 [UsedItems] 가 지킨다 — 여기서 각각 다루면 반드시 어긋난다.
     */
    fun toggleBlessing() {
        if (busy) return
        if (!pendingItems.blessing && game.inventory.blessingScrolls <= 0) return
        pendingItems = pendingItems.toggleBlessing()
        _ui.value = render()
    }

    /** 다음 강화에 행운부적을 쓸지 켜고 끈다. 켜면 축복서가 내려간다. */
    fun toggleLuckCharm() {
        if (busy) return
        if (!pendingItems.luckCharm && game.inventory.luckCharms <= 0) return
        pendingItems = pendingItems.toggleLuckCharm()
        _ui.value = render()
    }

    fun buyItem(item: Item) {
        if (busy || !Economy.canBuyItem(game, item)) return
        game = Economy.buyItem(game, item)
        sound.purchase()
        persist()
        _ui.value = render()
    }

    /**
     * 조각 교환.
     *
     * 검을 주는 교환이면 계열은 [Recipes.familyFor] 가 정한다 — 화면이 고르게
     * 하지 않는다. 계열끼리 성능이 같아서 고를 이유가 없었고, 지금은 도감이
     * 덜 찬 계열이 먼저 나온다.
     */
    fun craft(recipeId: String) {
        if (busy) return
        val recipe = Recipes.ALL.firstOrNull { it.id == recipeId } ?: return
        if (!Recipes.canCraft(game, recipe)) return
        val family = Recipes.familyFor(
            unlocked = Progress.unlockedFamilies(progress),
            incomplete = Progress.incompleteFamilies(progress),
            roll = rng.nextInt(WeaponFamily.entries.size),
        )
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

    /** 골드로 강화석을 산다. 살수록 비싸지고, 한 단계 올리면 값이 되돌아온다. */
    fun buyStone() {
        if (busy || !GoldShop.canBuyStone(game)) return
        game = GoldShop.buyStone(game)
        progress = Progress.refresh(Progress.onStones(progress, 1))
        persist()
        _ui.value = render()
    }

    /**
     * 검을 보관함으로 바로 산다. 손에 든 검은 그대로다.
     *
     * 재료 검을 모으려고 상점과 보관함을 오가며 넣었다 뺐다 하는 일을 없앤다.
     */
    fun buySwordToStorage(family: WeaponFamily) {
        if (busy || !Economy.canBuyToStorage(game)) return
        game = Economy.buyToStorage(game, family)
        progress = Progress.registerSword(progress, game.difficulty, Sword(family, 0))
        persist()
        _ui.value = render()
    }

    /** 결과 연출이 끝났다고 화면이 알려 준다. 입력 잠금을 푼다. */
    fun onAnimationFinished() {
        if (!busy) return
        busy = false
        lastResult = null
        lastWasRecord = false
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
        huntJob?.cancel()
        super.onCleared()
    }

    /** 사냥 진행을 읽기 전용으로 노출한다. 구역 선택 화면이 쓴다. */
    fun adventure(): AdventureState = game.adventure

    // ---------------- 보관함 ----------------

    /**
     * 사냥에서 검이 떨어지는지 굴린다.
     *
     * 보관함이 꽉 차 있으면 조용히 버리지 않고 [dropMissed] 로 알린다.
     * 모르는 사이에 손해를 보는 것이 가장 나쁘다.
     */
    private fun rollDrop(zone: Zone, isBoss: Boolean) {
        val drop = SwordDrop.roll(
            zone = zone,
            isRare = rareTarget,
            isBoss = isBoss,
            families = Progress.unlockedFamilies(progress),
            rng = rng,
            chanceMult = UniqueSwords.dropMultOf(game.sword) * Pets.dropMultOf(game.pets),
        ) ?: return

        if (Storage.isFull(game)) {
            dropMissed = true
            return
        }
        game = game.copy(storage = game.storage + drop)
        progress = Progress.refresh(Progress.registerSword(progress, game.difficulty, drop))
        lastDrop = drop
        dropMissed = false
        sound.purchase()
    }

    /** 드롭 알림을 화면이 읽은 뒤 비운다. */
    fun clearDropNotice() {
        if (lastDrop == null && !dropMissed) return
        lastDrop = null
        dropMissed = false
        _ui.value = render()
    }

    /** 들고 있는 검을 보관함에 넣는다. */
    fun storeSword() {
        if (busy || !Storage.canStore(game)) return
        game = Storage.store(game)
        persist()
        _ui.value = render()
    }

    /** 보관함의 검을 든다. 들고 있던 검은 보관함으로 들어간다. */
    fun equipFromStorage(index: Int) {
        if (busy) return
        if (index !in game.storage.indices || game.pendingDestroy != null) return
        game = Storage.equip(game, index)
        persist()
        _ui.value = render()
    }

    /** 보관함의 검을 판다. */
    fun sellFromStorage(index: Int) {
        if (busy || index !in game.storage.indices) return
        val price = Economy.sellPrice(game.storage[index].level)
        game = Storage.sell(game, index)
        progress = Progress.refresh(Progress.onSell(progress, price))
        sound.purchase()
        persist()
        _ui.value = render()
    }

    // ---------------- 조합 · 재료 · 별 ----------------

    /** 보관함의 검 여러 자루를 녹여 한 자루로 만든다. 레시피와 맞으면 고유검이다. */
    fun fuse(indices: List<Int>) {
        if (busy || !Fusion.canFuse(game, indices)) return
        game = Fusion.fuse(game, indices)
        game.storage.lastOrNull()?.let { result ->
            progress =
                Progress.refresh(Progress.registerSword(progress, game.difficulty, result))
            result.uniqueId?.let { uniqueId ->
                progress = Progress.onUniqueFound(progress, uniqueId)
                sound.uniqueBorn() // 발견은 사건이어야 한다
            }
        }
        progress = Progress.refresh(Progress.onFusion(progress))
        sound.zoneCleared()
        persist()
        _ui.value = render()
    }

    /** 별을 하나 올려 본다. 실패해도 검은 부서지지 않는다. */
    fun starUp() {
        if (busy || !StarForce.canAfford(game)) return
        val result = StarForce.attempt(game, rng)
        game = result.state
        lastStarUp = result is StarForce.Result.Up
        if (result is StarForce.Result.Up) haptics.starUp() else haptics.starDown()
        progress = Progress.refresh(
            Progress.onStars(
                Progress.onStarAttempt(progress),
                game.sword?.stars ?: 0,
            ),
        )
        if (result is StarForce.Result.Up) {
            sound.forgeSuccess(game.sword?.level ?: 0)
            game.sword?.let {
                progress = Progress.refresh(Progress.registerSword(progress, game.difficulty, it))
            }
        } else {
            sound.forgeStay()
        }
        persist()
        _ui.value = render()
    }

    // ---------------- 퀘스트 ----------------

    /**
     * 날짜가 바뀌었으면 퀘스트를 새로 뽑는다.
     *
     * 시계는 여기(앱 계층)가 읽고 :core 는 문자열 키만 받는다.
     * 별 강화는 +10부터 열리므로 그 전에는 풀에서 뺀다.
     */
    private fun refreshQuests() {
        val cal = java.util.Calendar.getInstance()
        val dateKey = "%04d%02d%02d".format(
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH) + 1,
            cal.get(java.util.Calendar.DAY_OF_MONTH),
        )
        val weekKey = "%04d-%02d".format(
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.WEEK_OF_YEAR),
        )
        val pool = QuestKind.entries.filter {
            it != QuestKind.STAR || progress.stats.bestLevelEver >= StarForce.MIN_LEVEL
        }
        val refreshed =
            DailyQuests.refresh(game.quests, progress.stats, dateKey, weekKey, rng, pool)
        if (refreshed != game.quests) {
            game = game.copy(quests = refreshed)
            persist()
        }
    }

    /** 완료한 퀘스트의 보상을 받는다. [index] 0..2 = 일일, -1 = 주간. */
    fun claimQuest(index: Int) {
        val quests = game.quests
        val target = if (index < 0) quests.weekly else quests.daily.getOrNull(index)
        if (target == null || target.claimed || !DailyQuests.isDone(target, progress.stats)) {
            return
        }
        game = DailyQuests.claim(game, progress.stats, index)
        // 주간은 조각에 더해 지금 사냥 중인 구역의 펫 알을 준다
        if (index < 0) {
            grantEgg(game.adventure.zone)
        }
        sound.purchase()
        persist()
        _ui.value = render()
    }

    /** 별 강화 결과 알림을 화면이 읽은 뒤 비운다. */
    fun clearStarNotice() {
        if (lastStarUp == null) return
        lastStarUp = null
        _ui.value = render()
    }

    private fun renderStar(): StarUiState? {
        val sword = game.sword ?: return null
        if (sword.level < StarForce.MIN_LEVEL) return null
        return StarUiState(
            stars = sword.stars,
            maxStars = StarForce.MAX_STARS,
            successPercent = (StarForce.successRate(sword) * 100).roundToInt(),
            shardCost = StarForce.shardCost(sword),
            goldCost = StarForce.goldCost(sword),
            affordable = StarForce.canAfford(game),
            attackBonusPercent =
                ((StarForce.attackMultiplier(sword) - 1.0) * 100).roundToInt(),
            lastUp = lastStarUp,
        )
    }

    /** 보관함의 검을 부숴 조각으로 바꾼다. */
    fun scrapFromStorage(index: Int) {
        if (busy || index !in game.storage.indices) return
        game = Storage.scrap(game, index)
        sound.salvage()
        persist()
        _ui.value = render()
    }

    private fun applyBailout() {
        val rescued = Economy.applyBailoutIfNeeded(game)
        if (rescued !== game) {
            game = rescued
            progress = Progress.refresh(Progress.onBailout(progress))
        }
    }

    private fun persist() {
        // 저장할 때마다 시각을 새로 찍는다. 이 값이 다음 실행의 자리비움 기준이다.
        game = game.copy(lastSeenMillis = now())
        store.saveGame(game)
        store.saveProgress(progress)
    }

    private companion object {
        /** 사냥 루프 주기. 화상 피해와 보스 제한 시간을 이 간격으로 처리한다. */
        const val HUNT_TICK_MILLIS = 1_000L

        /**
         * 이 단계 위부터 신기록을 축하한다.
         *
         * 새 세이브는 처음 열 판이 전부 신기록이라 문턱이 없으면 연출이 금방 값을 잃는다.
         * [com.geomgang.core.StarForce.MIN_LEVEL] 과 같은 값이라 규칙이 한 벌로 읽힌다.
         */
        const val MIN_RECORD_LEVEL = 10

        // 잡몹 강화석 확률은 ForgeCost.MOB_STONE_CHANCE 로 옮겼다 -
        // 공급과 요구가 같은 파일에 있어야 ForgeTempoTest 가 둘을 견줄 수 있다.
    }

    /**
     * 담금질 표시. 붙지 않는 구간이면 null 이다.
     *
     * 기준값과 지금 값을 함께 낸다 — 게이지 옆의 "0.5% → 12.3%" 가 실패가 무엇을
     * 남겼는지 말해 주는 유일한 자리다.
     */
    private fun temperUiFor(targetLevel: Int): TemperUi? {
        if (game.sword == null) return null
        if (!Tempering.applies(targetLevel)) return null

        val fails = Tempering.failsFor(game, targetLevel)
        val base = RateTable.successRate(game.difficulty, targetLevel)
        val now = RateTable.successRate(game.difficulty, targetLevel, temperFails = fails)
        return TemperUi(
            fails = fails,
            basePercent = base * 100,
            currentPercent = now * 100,
            ratio = (now / Tempering.MAX_RATE).coerceIn(0.0, 1.0).toFloat(),
        )
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
            // 성공률만이 아니라 하락·파괴까지 한 곳에서 낸다. 화면이 다시 계산하면
            // 규칙이 두 군데가 되고 반드시 어긋난다.
            odds = ForgeOdds.of(
                game.difficulty,
                targetLevel,
                pendingItems,
                Tempering.failsFor(game, targetLevel),
            ).percents(),
            temper = temperUiFor(targetLevel),
            recentMarks = game.recentMarks,
            isRecord = lastWasRecord,
            canForge = !busy && ForgeEngine.canAttempt(game, pendingItems),
            canBuySword = !busy && Economy.canBuySword(game),
            canBuyToStorage = !busy && Economy.canBuyToStorage(game),
            stonePrice = GoldShop.stonePrice(game),
            nextStonePrice = GoldShop.stonePrice(game.copy(stonesBought = game.stonesBought + 1)),
            canBuyStone = !busy && GoldShop.canBuyStone(game),
            itemPrices = Item.entries.associateWith { Economy.priceOf(it) },
            unlockedFamilies = Progress.unlockedFamilies(progress),
            useBlessing = pendingItems.blessing,
            useLuckCharm = pendingItems.luckCharm,
            storage = game.storage,
            storageCapacity = Storage.CAPACITY,
            lastDrop = lastDrop,
            dropMissed = dropMissed,
            forgeStones = game.forgeStones,
            requiredStones = game.sword?.let { ForgeCost.requirementFor(it.level).stones } ?: 0,
            forgeBlockedReason = if (busy) null else ForgeCost.missingText(game),
            star = renderStar(),
            progress = progress,
            settings = settings,
            idleReward = idleReward,
            hunt = renderHunt(),
            attackPower = Combat.attackPower(game.sword),
            essences = game.essences,
            pets = game.pets,
            lastEgg = lastEgg,
            gauntlet = renderGauntlet(),
            gauntletUnlocked = gauntletUnlocked(),
            gauntletBest = game.gauntletBest,
            quests = game.quests,
            questProgress = game.quests.daily.map {
                DailyQuests.progressOf(it, progress.stats)
            },
            weeklyProgress = game.quests.weekly?.let {
                DailyQuests.progressOf(it, progress.stats)
            } ?: 0,
            questClaimable = run {
                val dailyReady = game.quests.daily.any {
                    !it.claimed && DailyQuests.isDone(it, progress.stats)
                }
                val weeklyReady = game.quests.weekly?.let {
                    !it.claimed && DailyQuests.isDone(it, progress.stats)
                } ?: false
                dailyReady || weeklyReady
            },
            lastResult = lastResult,
            destroyPhase = phase,
            canPrevent = ForgeEngine.canPrevent(game),
            busy = busy,
        )
    }
}
