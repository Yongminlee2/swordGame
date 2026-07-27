# M18 강화석 + 단계별 재료 요구 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 고단계 강화의 화폐를 골드에서 재료로 옮긴다 — +13부터 검, +16부터 강화석이 필수가 되고, 강화석을 사냥·보스·분해·교환으로 모은다.

**Architecture:** 요구량 계산은 `:core` 새 객체 `ForgeCost`(순수 함수). `ForgeEngine.canAttempt`가 이를 물어보고, ViewModel 이 재료 소모를 집행한다. 기존 `MaterialBoost`(선택적 성공률 재료)는 **필수분 위에 얹는 추가분**으로 역할이 바뀐다.

**Tech Stack:** Kotlin, kotlinx-serialization(새 필드 기본값 = 세이브 호환), JUnit.

## Global Constraints

- AGP 9.2.1 내장 Kotlin — `org.jetbrains.kotlin.android` 플러그인을 **절대 추가하지 않는다**
- `gradle.properties`의 `-Dfile.encoding=MS949`·`org.gradle.java.home`을 건드리지 않는다
- `:core`는 순수 Kotlin — 안드로이드 의존성 금지
- 커밋 메시지·문서에 AI 언급 금지, UI 문구는 한국어
- 새 저장 필드는 전부 기본값 — 옛 세이브 무손실
- 사냥 ViewModel 테스트 규칙: 상태 캡처 → `leaveHunt()` → 단언 (단언 먼저 하면 실패가 행으로 둔갑)
- 요구량 표 (스펙 §1 그대로):
  - +0~+12: 골드만
  - +13~+15: 골드 + 검 1자루
  - +16~+20: 골드 + 검 2자루 + 강화석 `3 + (목표단계 - 16)`
  - +21~: 골드 + 검 3자루 + 강화석 `8 + (목표단계 - 20)`, 상한 40

---

### Task 1: ForgeCost 도메인

**Files:**
- Create: `core/src/main/kotlin/com/geomgang/core/ForgeCost.kt`
- Test: `core/src/test/kotlin/com/geomgang/core/ForgeCostTest.kt`

**Interfaces:**
- Consumes: `Economy.upgradeCost(currentLevel)`, `GameState`
- Produces:
  - `data class ForgeRequirement(val gold: Long, val swords: Int, val stones: Int)`
  - `ForgeCost.SWORD_BAND_START = 13`, `ForgeCost.STONE_BAND_START = 16`, `ForgeCost.MAX_STONES = 40`
  - `ForgeCost.requirementFor(currentLevel: Int): ForgeRequirement`
  - `ForgeCost.canPay(state: GameState, extraSwords: Int = 0): Boolean`
  - `ForgeCost.missingText(state: GameState): String?` — 못 내는 이유 한 줄(화면이 쓴다), 낼 수 있으면 null

- [ ] **Step 1: 실패하는 테스트 작성**

```kotlin
package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ForgeCostTest {

    private fun state(
        level: Int,
        gold: Long = 1_000_000_000,
        stones: Int = 100,
        storage: Int = 10,
    ) = GameState(
        difficulty = Difficulty.ENDLESS,
        gold = gold,
        sword = Sword(WeaponFamily.STRAIGHT, level),
        storage = List(storage) { Sword(WeaponFamily.STRAIGHT, 1) },
        forgeStones = stones,
    )

    @Test
    fun `12단계까지는 골드만 든다`() {
        for (level in 0..11) {
            val req = ForgeCost.requirementFor(level)
            assertEquals("+$level", Economy.upgradeCost(level), req.gold)
            assertEquals("+$level 검", 0, req.swords)
            assertEquals("+$level 강화석", 0, req.stones)
        }
    }

    @Test
    fun `13단계부터 검 한 자루가 필수다`() {
        // currentLevel 12 -> 목표 13
        val req = ForgeCost.requirementFor(12)
        assertEquals(1, req.swords)
        assertEquals(0, req.stones)
    }

    @Test
    fun `16단계부터 검 두 자루와 강화석이 필수다`() {
        val req16 = ForgeCost.requirementFor(15) // 목표 16
        assertEquals(2, req16.swords)
        assertEquals(3, req16.stones)
        val req20 = ForgeCost.requirementFor(19) // 목표 20
        assertEquals(2, req20.swords)
        assertEquals(7, req20.stones)
    }

    @Test
    fun `무한 구간은 검 세 자루와 강화석이 단계마다 늘어난다`() {
        val req21 = ForgeCost.requirementFor(20) // 목표 21
        assertEquals(3, req21.swords)
        assertEquals(9, req21.stones)
        val req30 = ForgeCost.requirementFor(29) // 목표 30
        assertEquals(3, req30.swords)
        assertEquals(18, req30.stones)
    }

    @Test
    fun `강화석 요구는 상한을 넘지 않는다`() {
        val req = ForgeCost.requirementFor(200)
        assertEquals(ForgeCost.MAX_STONES, req.stones)
    }

    @Test
    fun `요구를 다 갖추면 낼 수 있다`() {
        assertTrue(ForgeCost.canPay(state(16)))
    }

    @Test
    fun `강화석이 모자라면 못 낸다`() {
        assertFalse(ForgeCost.canPay(state(16, stones = 0)))
        assertNotNull(ForgeCost.missingText(state(16, stones = 0)))
    }

    @Test
    fun `재료 검이 모자라면 못 낸다`() {
        assertFalse(ForgeCost.canPay(state(16, storage = 1)))
    }

    @Test
    fun `골드가 모자라면 못 낸다`() {
        assertFalse(ForgeCost.canPay(state(5, gold = 0)))
    }

    @Test
    fun `추가 재료를 요구하면 그만큼 더 필요하다`() {
        // 필수 2 + 추가 3 = 5자루가 있어야 한다
        assertTrue(ForgeCost.canPay(state(16, storage = 5), extraSwords = 3))
        assertFalse(ForgeCost.canPay(state(16, storage = 4), extraSwords = 3))
    }

    @Test
    fun `낼 수 있으면 사유가 없다`() {
        assertNull(ForgeCost.missingText(state(16)))
    }

    @Test
    fun `검이 없으면 사유를 알려 준다`() {
        val empty = GameState(difficulty = Difficulty.ENDLESS, gold = 1000)
        assertNotNull(ForgeCost.missingText(empty))
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :core:test --tests "com.geomgang.core.ForgeCostTest" --console=plain`
Expected: FAIL — `ForgeCost` 없음, `GameState.forgeStones` 없음 (컴파일 에러).

- [ ] **Step 3: GameState 에 강화석 필드 추가**

`core/.../Model.kt` 의 `GameState` 에서 `gauntletBest` 다음에 넣는다:

```kotlin
    /** 무한 회랑 최고 기록(깬 층). */
    val gauntletBest: Int = 0,
    /** 강화석. 고단계 강화만 먹는 자원이다 — 조각과 수요가 겹치지 않게 분리했다. */
    val forgeStones: Int = 0,
) {
```

`init` 블록에 검증을 더한다:

```kotlin
        require(forgeStones >= 0) { "forgeStones must be >= 0, was $forgeStones" }
```

- [ ] **Step 4: ForgeCost 구현**

```kotlin
package com.geomgang.core

/** 강화 한 번에 드는 것. */
data class ForgeRequirement(
    val gold: Long,
    /** 필수로 태워야 하는 보관함 검 자루 수. */
    val swords: Int,
    /** 필수 강화석. */
    val stones: Int,
)

/**
 * 강화 비용의 단일 출처.
 *
 * 초반은 골드만으로 굴러가고, 고단계로 갈수록 **재료가 화폐가 된다.**
 * 골드는 사냥으로 무한히 벌 수 있어서 그것만으로는 고단계가 "돈 쌓기"의 반복이 된다.
 * 재료를 필수로 만들면 사냥·보관함·분해가 강화의 전제 조건이 된다.
 *
 * 요구량은 **목표 단계**(현재 단계 + 1)를 기준으로 읽는다.
 */
object ForgeCost {

    /** 이 목표 단계부터 검이 필수다. */
    const val SWORD_BAND_START = 13

    /** 이 목표 단계부터 강화석도 필수다. */
    const val STONE_BAND_START = 16

    /** 무한 구간이 시작되는 목표 단계. */
    const val ENDLESS_BAND_START = 21

    /** 강화석 요구의 상한. 없으면 무한 구간에서 요구가 발산한다. */
    const val MAX_STONES = 40

    fun requirementFor(currentLevel: Int): ForgeRequirement {
        require(currentLevel >= 0) { "currentLevel must be >= 0, was $currentLevel" }
        val target = currentLevel + 1
        val swords = when {
            target < SWORD_BAND_START -> 0
            target < STONE_BAND_START -> 1
            target < ENDLESS_BAND_START -> 2
            else -> 3
        }
        val stones = when {
            target < STONE_BAND_START -> 0
            target < ENDLESS_BAND_START -> 3 + (target - STONE_BAND_START)
            else -> (8 + (target - 20)).coerceAtMost(MAX_STONES)
        }
        return ForgeRequirement(
            gold = Economy.upgradeCost(currentLevel),
            swords = swords,
            stones = stones,
        )
    }

    /**
     * 지금 상태로 요구를 낼 수 있는지.
     *
     * @param extraSwords 성공률 보너스로 더 태울 검. 필수분 위에 얹힌다.
     */
    fun canPay(state: GameState, extraSwords: Int = 0): Boolean {
        val sword = state.sword ?: return false
        val req = requirementFor(sword.level)
        if (state.gold < req.gold) return false
        if (state.storage.size < req.swords + extraSwords) return false
        if (state.forgeStones < req.stones) return false
        return true
    }

    /** 못 내는 이유 한 줄. 낼 수 있으면 null. 화면이 그대로 띄운다. */
    fun missingText(state: GameState): String? {
        val sword = state.sword ?: return "검이 없다"
        val req = requirementFor(sword.level)
        if (state.gold < req.gold) return "골드가 모자라다"
        if (state.storage.size < req.swords) {
            return "재료 검 ${req.swords}자루가 필요하다 (보관함 ${state.storage.size})"
        }
        if (state.forgeStones < req.stones) {
            return "강화석 ${req.stones}개가 필요하다 (보유 ${state.forgeStones})"
        }
        return null
    }
}
```

- [ ] **Step 5: 통과 확인**

Run: `./gradlew :core:test --console=plain`
Expected: 전부 PASS. (`GameState` 필드 추가로 깨지는 테스트가 있으면 함께 고친다 — 기본값이라 없어야 정상)

- [ ] **Step 6: Commit**

```bash
git add core/src/main/kotlin/com/geomgang/core/ForgeCost.kt core/src/main/kotlin/com/geomgang/core/Model.kt core/src/test/kotlin/com/geomgang/core/ForgeCostTest.kt
git commit -m "강화 비용 재설계 - 단계별 재료 요구와 강화석 필드"
```

---

### Task 2: ForgeEngine 이 재료를 본다

**Files:**
- Modify: `core/src/main/kotlin/com/geomgang/core/ForgeEngine.kt`
- Test: `core/src/test/kotlin/com/geomgang/core/ForgeEngineTest.kt`

**Interfaces:**
- Consumes: Task 1의 `ForgeCost.canPay(state, extraSwords)`
- Produces: `ForgeEngine.canAttempt(state, items, extraSwords: Int = 0): Boolean` — 골드 검사를 `ForgeCost.canPay` 로 교체. `ForgeEngine.attempt` 는 그대로(재료 소모는 ViewModel 이 집행)

- [ ] **Step 1: 실패하는 테스트 추가** (`ForgeEngineTest.kt`)

```kotlin
    @Test
    fun `16단계 목표는 강화석이 없으면 시도할 수 없다`() {
        val state = GameState(
            difficulty = Difficulty.ENDLESS,
            gold = 1_000_000_000,
            sword = Sword(WeaponFamily.STRAIGHT, 15),
            storage = List(3) { Sword(WeaponFamily.STRAIGHT, 1) },
            forgeStones = 0,
        )
        assertFalse(ForgeEngine.canAttempt(state, UsedItems.NONE))
    }

    @Test
    fun `16단계 목표는 재료가 갖춰지면 시도할 수 있다`() {
        val state = GameState(
            difficulty = Difficulty.ENDLESS,
            gold = 1_000_000_000,
            sword = Sword(WeaponFamily.STRAIGHT, 15),
            storage = List(3) { Sword(WeaponFamily.STRAIGHT, 1) },
            forgeStones = 50,
        )
        assertTrue(ForgeEngine.canAttempt(state, UsedItems.NONE))
    }

    @Test
    fun `추가 재료를 요구하면 보관함이 그만큼 있어야 한다`() {
        val state = GameState(
            difficulty = Difficulty.ENDLESS,
            gold = 1_000_000_000,
            sword = Sword(WeaponFamily.STRAIGHT, 15),
            storage = List(2) { Sword(WeaponFamily.STRAIGHT, 1) },
            forgeStones = 50,
        )
        assertTrue(ForgeEngine.canAttempt(state, UsedItems.NONE))
        assertFalse(ForgeEngine.canAttempt(state, UsedItems.NONE, extraSwords = 1))
    }

    @Test
    fun `저단계는 재료 없이도 시도할 수 있다`() {
        val state = GameState(
            difficulty = Difficulty.ENDLESS,
            gold = 1_000_000,
            sword = Sword(WeaponFamily.STRAIGHT, 3),
        )
        assertTrue(ForgeEngine.canAttempt(state, UsedItems.NONE))
    }
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :core:test --tests "com.geomgang.core.ForgeEngineTest" --console=plain`
Expected: FAIL — `extraSwords` 파라미터 없음.

- [ ] **Step 3: 구현**

`ForgeEngine.canAttempt` 를 이렇게 바꾼다 (골드 직접 비교를 `ForgeCost` 로 넘긴다):

```kotlin
    /**
     * @param extraSwords 성공률 보너스로 더 태울 검 수. 필수 재료 위에 얹힌다.
     */
    fun canAttempt(state: GameState, items: UsedItems, extraSwords: Int = 0): Boolean {
        val sword = state.sword ?: return false
        if (state.pendingDestroy != null) return false

        val max = state.difficulty.maxLevel
        if (max != null && sword.level >= max) return false

        if (items.blessing && state.inventory.blessingScrolls <= 0) return false
        if (items.luckCharm && state.inventory.luckCharms <= 0) return false

        // 골드·재료 검·강화석 요구는 ForgeCost 가 단일 출처다.
        return ForgeCost.canPay(state, extraSwords)
    }
```

`canAutoForge` 는 그대로 둔다 — 자동강화는 안전구간(+5 이하)에서만 돌아 재료 구간에 닿지 않는다.

- [ ] **Step 4: 통과 확인**

Run: `./gradlew :core:test --console=plain`
Expected: 전부 PASS.

- [ ] **Step 5: Commit**

```bash
git add core/src/main/kotlin/com/geomgang/core/ForgeEngine.kt core/src/test/kotlin/com/geomgang/core/ForgeEngineTest.kt
git commit -m "강화 판정이 재료·강화석 요구를 본다"
```

---

### Task 3: 강화석 획득 경로

**Files:**
- Modify: `core/src/main/kotlin/com/geomgang/core/Storage.kt` (분해 보상)
- Modify: `core/src/main/kotlin/com/geomgang/core/Zones.kt` (보스 강화석)
- Modify: `core/src/main/kotlin/com/geomgang/core/Recipes.kt` (교환식)
- Test: `core/src/test/kotlin/com/geomgang/core/ForgeStoneSourceTest.kt` (새 파일)

**Interfaces:**
- Produces:
  - `Storage.scrapStones(sword: Sword): Int` — 분해 시 나오는 강화석 (`1 + level / 8`, 상한 3)
  - `Storage.scrap(state, index)` 가 `forgeStones` 도 올린다
  - `Zone.bossStones: Int` — 구역별 보스 강화석 (기존 생성자 뒤에 붙이는 새 파라미터)
  - `Recipes.STONE_SHARD_COST = 20`, `RecipeReward.GrantStone(count: Int)`, 교환식 `"stone"`
  - `HuntEvents` 는 건드리지 않는다 — 잡몹 5% 강화석은 ViewModel 이 굴린다(Task 4)

- [ ] **Step 1: 실패하는 테스트 작성**

```kotlin
package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ForgeStoneSourceTest {

    @Test
    fun `분해하면 강화석도 나온다`() {
        val state = GameState(
            difficulty = Difficulty.ENDLESS,
            storage = listOf(Sword(WeaponFamily.STRAIGHT, 16)),
        )
        val after = Storage.scrap(state, 0)
        assertEquals(Storage.scrapStones(Sword(WeaponFamily.STRAIGHT, 16)), after.forgeStones)
        assertTrue(after.shards > 0)
    }

    @Test
    fun `단계가 높은 검이 강화석을 더 준다 - 상한 3`() {
        assertEquals(1, Storage.scrapStones(Sword(WeaponFamily.STRAIGHT, 0)))
        assertEquals(2, Storage.scrapStones(Sword(WeaponFamily.STRAIGHT, 8)))
        assertEquals(3, Storage.scrapStones(Sword(WeaponFamily.STRAIGHT, 16)))
        assertEquals(3, Storage.scrapStones(Sword(WeaponFamily.STRAIGHT, 60)))
    }

    @Test
    fun `모든 구역 보스가 강화석을 준다`() {
        for (zone in Zone.entries) {
            assertTrue("${zone.displayName} 보스 강화석", zone.bossStones >= 3)
        }
    }

    @Test
    fun `깊은 구역 보스가 더 많이 준다`() {
        assertTrue(Zone.ABYSS.bossStones > Zone.MEADOW.bossStones)
    }

    @Test
    fun `조각을 강화석으로 바꿀 수 있다`() {
        val recipe = Recipes.byId("stone")
        assertEquals(Recipes.STONE_SHARD_COST, recipe.shardCost)
        val state = GameState(difficulty = Difficulty.ENDLESS, shards = 100)
        val after = Recipes.craft(state, recipe, WeaponFamily.STRAIGHT)
        assertEquals(1, after.forgeStones)
        assertEquals(100 - Recipes.STONE_SHARD_COST, after.shards)
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :core:test --tests "com.geomgang.core.ForgeStoneSourceTest" --console=plain`
Expected: FAIL — `scrapStones`·`bossStones`·`"stone"` 없음.

- [ ] **Step 3: Storage 분해 보상 구현**

`Storage` 객체에 추가하고 `scrap` 을 고친다:

```kotlin
    /** 분해로 나오는 강화석. 단계가 높을수록 많지만 상한이 있다. */
    fun scrapStones(sword: Sword): Int = (1 + sword.level / 8).coerceAtMost(3)
```

`scrap(state, index)` 안에서 조각을 더하는 곳에 강화석도 더한다:

```kotlin
        return state.copy(
            shards = state.shards + scrapShards(sword),
            forgeStones = state.forgeStones + scrapStones(sword),
            storage = state.storage.filterIndexed { i, _ -> i != index },
        )
```

(실제 `scrap` 본문의 형태에 맞춰 `forgeStones` 한 줄만 얹는다.)

- [ ] **Step 4: Zone 보스 강화석 구현**

`Zone` enum 생성자 마지막에 파라미터를 추가한다:

```kotlin
    val bossShards: Int,
    /** 보스가 확정으로 주는 강화석. 구역이 깊을수록 많다. */
    val bossStones: Int,
```

각 구역 정의의 끝에 값을 붙인다 (초원부터 순서대로 `3, 3, 4, 4, 5, 5, 6, 6, 7, 8`):

```kotlin
        "들개 우두머리", 420, 5, 900, 6, 3,
```

주의: 이 파일의 보스 인자는 `bossName, bossHp, bossSeconds, bossGold, bossShards` 순서다.
`bossSeconds` 는 M21에서 5로 바꾼다 — **여기서는 건드리지 않는다.**

- [ ] **Step 5: 조합소 교환식 구현**

`Recipes.kt` 에 보상 종류와 교환식을 추가한다:

```kotlin
sealed interface RecipeReward {
    data class GrantItem(val item: Item, val count: Int) : RecipeReward
    data class GrantSword(val level: Int) : RecipeReward
    data class GrantStone(val count: Int) : RecipeReward
}
```

```kotlin
    /** 조각을 강화석으로 바꾸는 값. 강화석은 고단계 강화의 화폐다. */
    const val STONE_SHARD_COST: Int = 20
```

`ALL` 목록 맨 앞(가장 자주 쓸 교환)에 넣는다:

```kotlin
        Recipe("stone", "강화석", STONE_SHARD_COST, RecipeReward.GrantStone(1)),
```

`craft` 의 `when (recipe.reward)` 분기에 추가한다:

```kotlin
            is RecipeReward.GrantStone ->
                paid.copy(forgeStones = paid.forgeStones + recipe.reward.count)
```

`canCraft` 는 고칠 것이 없다 — 강화석 교환에는 빈손 조건이 없다.

- [ ] **Step 6: 통과 확인**

Run: `./gradlew :core:test --console=plain`
Expected: 전부 PASS. (`Zone` 생성자 변경으로 `ZonesTest` 가 깨지면 새 인자를 반영해 고친다)

- [ ] **Step 7: Commit**

```bash
git add core/
git commit -m "강화석 획득 경로 - 분해·보스·조합소 교환"
```

---

### Task 4: ViewModel 배선

**Files:**
- Modify: `app/src/main/java/com/geomgang/game/ForgeViewModel.kt`
- Modify: `app/src/main/java/com/geomgang/game/ForgeUiState.kt`
- Test: `app/src/test/java/com/geomgang/game/ForgeViewModelStoneTest.kt` (새 파일)

**Interfaces:**
- Consumes: Task 1~3의 `ForgeCost`, `Storage.scrapStones`, `Zone.bossStones`
- Produces: `ForgeUiState` 에 `forgeStones: Int`, `requiredSwords: Int`, `requiredStones: Int`, `forgeBlockedReason: String?` 추가

**동작 규칙:**
- `runAttempt` 가 **필수 재료를 먼저 태운다**: `ForgeCost.requirementFor(level).swords + materialCount` 자루를 낮은 단계부터 집어 소모하고, 강화석도 차감한다. 성공률 보너스는 **추가분(materialCount)에만** 붙는다
- 잡몹 처치 시 `rng.nextDouble() < 0.05` 면 강화석 1개 (난수 소비 순서: 검 드롭 판정 **뒤**)
- 보스 처치 시 `forgeStones += zone.bossStones`
- `materialCount` 상한은 `MaterialBoost.MAX_MATERIALS` 와 "필수분을 뺀 보관함 여유" 중 작은 값

- [ ] **Step 1: 실패하는 테스트 작성**

```kotlin
package com.geomgang.game

import com.geomgang.core.Difficulty
import com.geomgang.core.ForgeCost
import com.geomgang.core.GameState
import com.geomgang.core.SaveStore
import com.geomgang.core.Storage
import com.geomgang.core.Sword
import com.geomgang.core.WeaponFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class ForgeViewModelStoneTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** 항상 성공하는 난수. */
    private fun alwaysSucceed() = object : Random() {
        override fun nextBits(bitCount: Int): Int = 0
        override fun nextDouble(): Double = 0.0
    }

    private fun vm(
        level: Int,
        stones: Int,
        storage: Int,
        rng: Random = alwaysSucceed(),
    ): ForgeViewModel {
        val store = SaveStore(tmp.root)
        store.saveGame(
            GameState(
                difficulty = Difficulty.ENDLESS,
                gold = 1_000_000_000,
                sword = Sword(WeaponFamily.STRAIGHT, level),
                storage = List(storage) { Sword(WeaponFamily.STRAIGHT, 1) },
                forgeStones = stones,
            ),
        )
        return ForgeViewModel(store, Difficulty.ENDLESS, rng)
    }

    @Test
    fun `강화석이 없으면 고단계 강화 버튼이 잠긴다`() {
        val ui = vm(level = 15, stones = 0, storage = 5).ui.value
        assertFalse(ui.canForge)
        assertNotNull(ui.forgeBlockedReason)
    }

    @Test
    fun `요구량이 화면에 실린다`() {
        val ui = vm(level = 15, stones = 50, storage = 5).ui.value
        val req = ForgeCost.requirementFor(15)
        assertEquals(req.swords, ui.requiredSwords)
        assertEquals(req.stones, ui.requiredStones)
        assertEquals(50, ui.forgeStones)
    }

    @Test
    fun `강화하면 필수 재료와 강화석이 빠진다`() = runTest(dispatcher) {
        val v = vm(level = 15, stones = 50, storage = 5)
        val req = ForgeCost.requirementFor(15)
        v.forge()
        val ui = v.ui.value
        assertEquals(50 - req.stones, ui.forgeStones)
        assertEquals(5 - req.swords, ui.storage.size)
    }

    @Test
    fun `저단계 강화는 재료를 태우지 않는다`() = runTest(dispatcher) {
        val v = vm(level = 3, stones = 10, storage = 5)
        v.forge()
        val ui = v.ui.value
        assertEquals(10, ui.forgeStones)
        assertEquals(5, ui.storage.size)
    }

    @Test
    fun `분해하면 강화석이 들어온다`() = runTest(dispatcher) {
        val v = vm(level = 3, stones = 0, storage = 2)
        v.scrapFromStorage(0)
        assertTrue(v.ui.value.forgeStones > 0)
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :app:testDebugUnitTest --tests "com.geomgang.game.ForgeViewModelStoneTest" --console=plain`
Expected: FAIL — `forgeStones`·`requiredSwords` 등 UI 필드 없음.

- [ ] **Step 3: ForgeUiState 확장**

```kotlin
    /** 강화석 보유량. 고단계 강화의 화폐다. */
    val forgeStones: Int = 0,
    /** 다음 강화에 필수인 재료 검 자루 수. */
    val requiredSwords: Int = 0,
    /** 다음 강화에 필수인 강화석. */
    val requiredStones: Int = 0,
    /** 강화할 수 없는 이유 한 줄. 가능하면 null. */
    val forgeBlockedReason: String? = null,
```

- [ ] **Step 4: ViewModel 배선**

`render()` 에 추가한다 (`canForge` 는 그대로 `ForgeEngine.canAttempt` 를 쓰되 추가 재료를 넘긴다):

```kotlin
            canForge = !busy && ForgeEngine.canAttempt(game, pendingItems, materialCount),
            forgeStones = game.forgeStones,
            requiredSwords = game.sword?.let { ForgeCost.requirementFor(it.level).swords } ?: 0,
            requiredStones = game.sword?.let { ForgeCost.requirementFor(it.level).stones } ?: 0,
            forgeBlockedReason = if (busy) null else ForgeCost.missingText(game),
```

`runAttempt(items)` 의 재료 처리를 이렇게 바꾼다 (기존 `materialIndices()` 블록을 대체):

```kotlin
    private fun runAttempt(items: UsedItems): ForgeResult? {
        if (!ForgeEngine.canAttempt(game, items, materialCount)) return null
        val sword = game.sword ?: return null
        val targetLevel = sword.level + 1
        val cost = Economy.upgradeCost(sword.level)
        val req = ForgeCost.requirementFor(sword.level)

        // 필수 재료 + 추가 재료를 낮은 단계부터 집는다. 성공률 보너스는 추가분에만 붙는다 -
        // 필수분은 입장료이므로 보너스까지 주면 고단계가 쉬워진다.
        val burnCount = req.swords + materialCount
        val burnIndices = game.storage
            .withIndex()
            .sortedBy { it.value.level }
            .take(burnCount)
            .map { it.index }
        val bonusIndices = burnIndices.drop(req.swords)
        val materialBonus = MaterialBoost.bonusFor(bonusIndices.map { game.storage[it] })

        if (burnIndices.isNotEmpty()) {
            game = MaterialBoost.consume(game, burnIndices)
            materialCount = 0
        }
        if (req.stones > 0) {
            game = game.copy(forgeStones = game.forgeStones - req.stones)
        }
```

(이 뒤의 판정·통계·저장 코드는 기존 그대로 둔다. `materialBonus` 를 `extraSuccessRate` 로 넘기는 부분도 그대로.)

`setMaterialCount` 의 상한을 필수분을 뺀 여유로 바꾼다:

```kotlin
    /** 다음 강화에 태울 **추가** 재료 수를 정한다. 필수분과 별개다. */
    fun setMaterialCount(count: Int) {
        if (busy || autoJob != null) return
        val required = game.sword?.let { ForgeCost.requirementFor(it.level).swords } ?: 0
        val spare = (game.storage.size - required).coerceAtLeast(0)
        materialCount = count.coerceIn(0, minOf(MaterialBoost.MAX_MATERIALS, spare))
        _ui.value = render()
    }
```

`maxMaterials` 도 같은 여유를 쓴다:

```kotlin
            maxMaterials = run {
                val required = game.sword?.let { ForgeCost.requirementFor(it.level).swords } ?: 0
                minOf(MaterialBoost.MAX_MATERIALS, (game.storage.size - required).coerceAtLeast(0))
            },
```

잡몹·보스 강화석을 `onTargetDown` 에 넣는다. 잡몹 분기의 `rollDrop(...)` **뒤**:

```kotlin
            // 강화석은 검 드롭 판정 뒤에 굴린다 (난수 소비 순서 계약)
            if (rng.nextDouble() < MOB_STONE_CHANCE) {
                game = game.copy(forgeStones = game.forgeStones + 1)
            }
```

보스 분기의 `game = game.copy(...)` 에 `forgeStones` 를 더한다:

```kotlin
                forgeStones = game.forgeStones + zone.bossStones,
```

companion 에 상수를 추가한다:

```kotlin
        /** 잡몹이 강화석을 떨어뜨릴 확률. */
        const val MOB_STONE_CHANCE = 0.05
```

- [ ] **Step 5: 통과 확인**

Run: `./gradlew :app:testDebugUnitTest --console=plain`
Expected: 전부 PASS. 기존 사냥 테스트의 난수 스크립트가 강화석 롤로 어긋나면
`QueueRandom` 기본값(1.0 = 강화석 없음)이 흡수하므로 대개 그대로 통과한다.

- [ ] **Step 6: Commit**

```bash
git add app/
git commit -m "강화석 배선 - 필수 재료 소모, 획득 경로, 화면 상태"
```

---

### Task 5: 강화 화면 표시 + 마무리

**Files:**
- Modify: `app/src/main/java/com/geomgang/game/ui/ForgeScreen.kt`
- Modify: `app/src/main/java/com/geomgang/game/ui/CraftScreen.kt` (강화석 교환 표시)
- Modify: `README.md`

**구현 내용:**

- 자원 줄에 강화석을 넣는다. 기존 `Stat("💎", ...)` 옆:

```kotlin
            Stat("🪨", "${state.forgeStones}")
```

- 강화 버튼 위에 요구량을 한 줄로 보여 준다 (요구가 있을 때만):

```kotlin
            if (state.requiredSwords > 0 || state.requiredStones > 0) {
                Text(
                    text = buildString {
                        append("필요: ")
                        if (state.requiredSwords > 0) append("🗡${state.requiredSwords} ")
                        if (state.requiredStones > 0) append("🪨${state.requiredStones}")
                    },
                    fontSize = 12.sp,
                    color = if (state.canForge) {
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
            }
```

- 못 하는 이유를 강화 버튼 아래에 띄운다 (기존 `Reason(...)` 패턴이 있으면 그것을 쓴다):

```kotlin
            state.forgeBlockedReason?.let {
                Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
            }
```

- `CraftScreen` 은 `Recipes.ALL` 을 순회하므로 강화석 교환이 자동으로 나온다.
  보상 문구를 만드는 `when` 에 `GrantStone` 분기를 추가한다:

```kotlin
        is RecipeReward.GrantStone -> "강화석 ${reward.count}개"
```

- [ ] **Step 1: 빌드·테스트 확인**

Run: `./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug --console=plain`
Expected: 전부 성공.

- [ ] **Step 2: 실기기 확인**

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

확인 항목: 저단계(+3)에서는 요구 줄이 안 보이고, +13 이상에서 🗡·🪨 요구가 뜨는지 /
강화석이 없으면 버튼이 잠기고 사유가 보이는지 / 분해·보스에서 강화석이 들어오는지 /
조합소에 "강화석" 교환이 있는지.

- [ ] **Step 3: README 개발 일지 + Commit**

`README.md` 에 `### v1.4.0-M18 — 2026-07-27 · 강화석과 재료 강화` 절을 추가한다.
내용: 요구량 표, 강화석 획득 경로, "성공률 보너스는 추가분에만" 규칙과 그 이유.

```bash
git add -A
git commit -m "M18 마무리 - 강화 화면 요구량 표시, 개발 일지"
git push origin main
```
