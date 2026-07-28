# 강화 성장 축과 전설검 등급 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 오래 한 것(도감·대장간·고유검·계열)이 강화 확률로 쌓이게 하고, 그 끝에 전설검이라는 도달점을 둔다.

**Architecture:** 네 출처가 각자 자기 몫만 계산해 내놓고 `ForgeBonuses` 한 곳이 더한다. 확률표는 계속 `RateTable.successRate` 단일 출처이며 인자 하나가 붙는다 — 담금질을 넣을 때와 같은 자리다. 전설검은 `WeaponFamily` 에 넣지 않고 **단계가 전설 여부를 정한다.**

**Tech Stack:** Kotlin 2.4 / AGP 9.2.1 / Gradle 9.4.1, `:core` 순수 Kotlin + JUnit4, `:app` Compose + Material3, kotlinx-serialization

## Global Constraints

- `:core` 는 순수 Kotlin이다. 안드로이드 의존성을 넣지 않는다.
- UI 문구와 주석은 한국어다.
- `org.jetbrains.kotlin.android` 플러그인을 **추가하지 않는다.** AGP 9.2.1 에 내장되어 있다.
- `gradle.properties` 의 `-Dfile.encoding=MS949` 와 `org.gradle.java.home` 을 건드리지 않는다.
- 커밋 메시지·README·문서에 AI/Claude 표기를 넣지 않는다.
- 새 세이브 필드는 전부 기본값을 가진다. 옛 세이브가 손실 없이 열려야 한다.
- 여러 줄 커밋 메시지는 스크래치패드 파일에 쓰고 `git commit -F <file>` 로 넣는다.
- 테스트는 프로젝트 루트(`C:\workAndroid\SwordForge`)에서 `.\gradlew.bat` 로 돌린다.
- **`BalanceSimulationTest` 와 `BossTempoTest` 를 손대지 않는다.** 새 세이브는 보너스가 전부 0이라 그대로 통과해야 한다. 깨지면 보너스가 시뮬레이터에 새어 들어갔다는 뜻이다.
- **전설검은 전투 배수를 건드리지 않는다.** 사냥은 쉬워지지 않는다.

---

### Task 1: ForgeBonus 뼈대와 확률표 연결

출처는 아직 없다. 전부 0인 상태로 배선만 만든다.

**Files:**
- Create: `core/src/main/kotlin/com/geomgang/core/ForgeBonus.kt`
- Modify: `core/src/main/kotlin/com/geomgang/core/RateTable.kt`
- Modify: `core/src/main/kotlin/com/geomgang/core/ForgeEngine.kt`
- Test: `core/src/test/kotlin/com/geomgang/core/ForgeBonusTest.kt`

**Interfaces:**
- Produces:
  - `data class ForgeBonus(successRate: Double, destroyGuard: Double)` — `plus`, `ForgeBonus.NONE`
  - `data class BonusSource(label: String, detail: String, bonus: ForgeBonus)`
  - `object ForgeBonuses` — `sourcesOf(state, progress): List<BonusSource>`, `of(state, progress): ForgeBonus`
  - `RateTable.successRate(difficulty, targetLevel, blessing, temperFails, bonus: Double)`
  - `ForgeEngine.attempt(state, items, rng, bonus: ForgeBonus)`

- [ ] **Step 1: 실패하는 테스트를 작성한다**

`core/src/test/kotlin/com/geomgang/core/ForgeBonusTest.kt`:

```kotlin
package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ForgeBonusTest {

    private val empty = ProgressState()
    private val fresh = GameState(Difficulty.ENDLESS)

    @Test
    fun `아무것도 없으면 보너스가 0이다`() {
        val bonus = ForgeBonuses.of(fresh, empty)
        assertEquals(0.0, bonus.successRate, 1e-9)
        assertEquals(0.0, bonus.destroyGuard, 1e-9)
    }

    @Test
    fun `더하면 항목별로 더해진다`() {
        val a = ForgeBonus(0.01, 0.02)
        val b = ForgeBonus(0.03, 0.04)
        assertEquals(ForgeBonus(0.04, 0.06), a + b)
    }

    @Test
    fun `출처 목록을 다 더하면 합계와 같다`() {
        val sources = ForgeBonuses.sourcesOf(fresh, empty)
        val summed = sources.fold(ForgeBonus.NONE) { acc, s -> acc + s.bonus }
        assertEquals(ForgeBonuses.of(fresh, empty), summed)
    }

    /** 보너스가 0이면 확률표가 예전과 똑같아야 한다. 시뮬레이터가 보는 상태다. */
    @Test
    fun `보너스 0이면 예전 확률 그대로다`() {
        assertEquals(
            RateTable.baseSuccessRate(45) * Difficulty.ENDLESS.multiplier,
            RateTable.successRate(Difficulty.ENDLESS, 45, bonus = 0.0),
            1e-9,
        )
    }

    @Test
    fun `보너스는 성공률에 더해진다`() {
        val plain = RateTable.successRate(Difficulty.ENDLESS, 45)
        val boosted = RateTable.successRate(Difficulty.ENDLESS, 45, bonus = 0.05)
        assertEquals(plain + 0.05, boosted, 1e-9)
    }

    @Test
    fun `보너스를 얹어도 최종 상한을 넘지 않는다`() {
        val rate = RateTable.successRate(Difficulty.ENDLESS, 1, blessing = true, bonus = 0.5)
        assertTrue("rate=$rate", rate <= RateTable.MAX_SUCCESS_RATE)
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

```bash
cd /c/workAndroid/SwordForge && ./gradlew.bat :core:test --tests "com.geomgang.core.ForgeBonusTest" --console=plain
```

Expected: FAIL — `Unresolved reference 'ForgeBonuses'`

- [ ] **Step 3: ForgeBonus 를 만든다**

`core/src/main/kotlin/com/geomgang/core/ForgeBonus.kt`:

```kotlin
package com.geomgang.core

/**
 * 강화에 얹히는 보너스.
 *
 * @property successRate 성공률에 더하는 몫. 0.012 는 1.2%p 다
 * @property destroyGuard 파괴 판정을 무효로 돌릴 확률
 */
data class ForgeBonus(
    val successRate: Double = 0.0,
    val destroyGuard: Double = 0.0,
) {
    operator fun plus(other: ForgeBonus): ForgeBonus = ForgeBonus(
        successRate = successRate + other.successRate,
        destroyGuard = destroyGuard + other.destroyGuard,
    )

    companion object {
        val NONE: ForgeBonus = ForgeBonus()
    }
}

/** 보너스 한 줄. 화면이 "왜 이 확률인지" 말해 주기 위한 것이다. */
data class BonusSource(
    val label: String,
    val detail: String,
    val bonus: ForgeBonus,
)

/**
 * 강화 보너스를 모으는 **유일한 곳**.
 *
 * 출처가 넷(도감·대장간·고유검·전설검)인데 여러 군데서 더하면 왜 이 확률이 나왔는지
 * 아무도 설명 못 하게 된다. 합산도 여기서만 하고, 화면이 출처별로 쪼개 보여 줄 수 있게
 * [sourcesOf] 로 낱개도 낸다.
 *
 * **시뮬레이터는 이 값을 쓰지 않는다.** 새 세이브는 네 출처가 전부 0이고, 그게
 * [com.geomgang.core.sim.BalanceSimulation] 이 검증하려는 "맨손 신규 플레이어" 다.
 */
object ForgeBonuses {

    fun sourcesOf(state: GameState, progress: ProgressState): List<BonusSource> = emptyList()

    fun of(state: GameState, progress: ProgressState): ForgeBonus =
        sourcesOf(state, progress).fold(ForgeBonus.NONE) { acc, s -> acc + s.bonus }
}
```

- [ ] **Step 4: 확률표와 판정 엔진에 연결한다**

`RateTable.successRate` 를 바꾼다:

```kotlin
    /**
     * 난이도 배수·담금질·축복서·보너스를 반영한 최종 성공률.
     *
     * 순서가 뜻을 만든다. 담금질은 누적된 몫이라 난이도 배수 **뒤**, 축복서는
     * "이번 판만 얹는 것" 이라 그 **위**, [bonus] 는 플레이어가 쌓아 온 몫이라
     * 마지막에 더한다.
     *
     * @param bonus [ForgeBonuses] 가 모은 성공률 가산. 0 이면 예전과 같은 값이다
     */
    fun successRate(
        difficulty: Difficulty,
        targetLevel: Int,
        blessing: Boolean = false,
        temperFails: Int = 0,
        bonus: Double = 0.0,
    ): Double {
        val scaled = baseSuccessRate(targetLevel) * difficulty.multiplier
        val tempered = Tempering.rateFor(scaled, targetLevel, temperFails)
        val boosted = if (blessing) tempered + BLESSING_BONUS else tempered
        return minOf(boosted + bonus, MAX_SUCCESS_RATE)
    }
```

`ForgeEngine.attempt` 의 `extraSuccessRate: Double = 0.0` 파라미터를 지우고
`bonus: ForgeBonus = ForgeBonus.NONE` 으로 바꾼다. (재료 강화가 사라져 `extraSuccessRate`
를 넘기는 곳이 이미 없다.) 성공률 계산을 바꾼다:

```kotlin
        val successRate = (
            RateTable.successRate(
                state.difficulty,
                targetLevel,
                items.blessing,
                fails,
                bonus.successRate,
            ) + UniqueSwords.forgeBonusOf(sword)
            ).coerceAtMost(RateTable.MAX_SUCCESS_RATE)
```

파괴 분기에서 **방지 굴림을 하나 더** 넣는다. `DESTROY_OR_DROP` 안의
`if (rng.nextDouble() < RateTable.destroyChance(targetLevel))` 바로 다음 줄에:

```kotlin
                    // 파괴가 정해진 뒤 한 번 더 굴린다. 방지권보다 먼저이고 소모품이 아니다.
                    // 순서를 여기 둔 이유: 파괴가 안 났으면 굴릴 이유가 없어서 난수가 낭비된다.
                    if (bonus.destroyGuard > 0 && rng.nextDouble() < bonus.destroyGuard) {
                        ForgeResult.Stay(failed, sword.level)
                    } else if (UniqueSwords.canRevive(sword)) {
```

기존 `if (UniqueSwords.canRevive(sword)) {` 를 위 `else if` 로 잇는다.

- [ ] **Step 5: 테스트가 통과하는지 확인한다**

```bash
cd /c/workAndroid/SwordForge && ./gradlew.bat :core:test :app:testDebugUnitTest --console=plain
```

Expected: PASS. `BalanceSimulationTest` 도 통과해야 한다 — 보너스가 0이라 난수 소비도 그대로다.

- [ ] **Step 6: 커밋한다**

```bash
cd /c/workAndroid/SwordForge && git add -A && git commit -m "강화 보너스를 모으는 자리 하나"
```

---

### Task 2: 도감 수집과 도감 보너스

**Files:**
- Create: `core/src/main/kotlin/com/geomgang/core/CodexOffer.kt`
- Modify: `core/src/main/kotlin/com/geomgang/core/ForgeBonus.kt`
- Modify: `core/src/main/kotlin/com/geomgang/core/Progress.kt` (자동 등록 제거)
- Test: `core/src/test/kotlin/com/geomgang/core/CodexOfferTest.kt`

**Interfaces:**
- Consumes: `ForgeBonuses.sourcesOf` (Task 1), `WeaponCatalog.slotFor`, `Progress.entriesOf`
- Produces:
  - `CodexOffer.canOffer(progress: ProgressState, sword: Sword): Boolean`
  - `CodexOffer.offer(progress: ProgressState, difficulty: Difficulty, sword: Sword): ProgressState`
  - `CodexOffer.bonusOf(progress: ProgressState): ForgeBonus`
  - `CodexOffer.slotOf(sword: Sword): CodexEntry`
  - `CodexOffer.SLOT_STEP = 10`, `CodexOffer.STEP_BONUS = 0.001`, `CodexOffer.MAX_BONUS`

- [ ] **Step 1: 실패하는 테스트를 작성한다**

`core/src/test/kotlin/com/geomgang/core/CodexOfferTest.kt`:

```kotlin
package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 도감은 저절로 채워지지 않는다. 검을 바쳐야 칸이 열린다.
 *
 * 예전에는 검을 얻으면 자동으로 올라서 도감이 "지나간 자취" 일 뿐이었다.
 * 이제 "계속 강화할까, 여기서 바칠까" 가 매번 선택이 된다.
 */
class CodexOfferTest {

    private val empty = ProgressState()
    private val sword = Sword(WeaponFamily.STRAIGHT, 7)

    @Test
    fun `빈 칸이면 바칠 수 있다`() {
        assertTrue(CodexOffer.canOffer(empty, sword))
    }

    @Test
    fun `바치면 그 칸이 열린다`() {
        val after = CodexOffer.offer(empty, Difficulty.ENDLESS, sword)
        assertTrue(CodexEntry(WeaponFamily.STRAIGHT, 7) in Progress.entriesOf(after))
    }

    /** 이미 찬 칸에 또 바치면 검만 사라진다. 헛되이 태우지 않게 막는다. */
    @Test
    fun `이미 찬 칸은 못 바친다`() {
        val after = CodexOffer.offer(empty, Difficulty.ENDLESS, sword)
        assertFalse(CodexOffer.canOffer(after, sword))
    }

    @Test
    fun `열 칸마다 보너스가 한 계단 오른다`() {
        var p = empty
        for (level in 0 until 10) {
            p = CodexOffer.offer(p, Difficulty.ENDLESS, Sword(WeaponFamily.STRAIGHT, level))
        }
        val bonus = CodexOffer.bonusOf(p)
        assertEquals(CodexOffer.STEP_BONUS, bonus.successRate, 1e-9)
        assertEquals(CodexOffer.STEP_BONUS, bonus.destroyGuard, 1e-9)
    }

    @Test
    fun `아홉 칸으로는 아직 오르지 않는다`() {
        var p = empty
        for (level in 0 until 9) {
            p = CodexOffer.offer(p, Difficulty.ENDLESS, Sword(WeaponFamily.STRAIGHT, level))
        }
        assertEquals(0.0, CodexOffer.bonusOf(p).successRate, 1e-9)
    }

    /** 324칸을 다 채워도 상한을 넘지 않는다. */
    @Test
    fun `다 채우면 상한에 닿는다`() {
        var p = empty
        for (entry in WeaponCatalog.ENTRIES) {
            val family = entry.family ?: WeaponFamily.STRAIGHT
            p = CodexOffer.offer(p, Difficulty.ENDLESS, Sword(family, entry.level))
        }
        assertEquals(CodexOffer.MAX_BONUS, CodexOffer.bonusOf(p).successRate, 1e-9)
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

```bash
cd /c/workAndroid/SwordForge && ./gradlew.bat :core:test --tests "com.geomgang.core.CodexOfferTest" --console=plain
```

Expected: FAIL — `Unresolved reference 'CodexOffer'`

- [ ] **Step 3: CodexOffer 를 만든다**

`core/src/main/kotlin/com/geomgang/core/CodexOffer.kt`:

```kotlin
package com.geomgang.core

/**
 * 도감에 검을 바친다.
 *
 * 예전에는 검을 얻으면 저절로 도감에 올랐다. 그래서 도감이 **지나간 자취**일 뿐
 * 플레이어가 뭘 한 게 아니었고, 아무 결정도 요구하지 않았다.
 *
 * 이제 바쳐야 열린다. 바친 검은 사라진다 — "계속 강화할까, 여기서 도감에 바칠까" 가
 * 매번 선택이 된다.
 */
object CodexOffer {

    /** 보너스가 한 계단 오르는 칸 수. */
    const val SLOT_STEP: Int = 10

    /** 한 계단이 주는 몫. 0.001 은 0.1%p 다. */
    const val STEP_BONUS: Double = 0.001

    /** 324칸을 다 채웠을 때의 몫. 32계단 × 0.1%p = 3.2%p. */
    val MAX_BONUS: Double = (WeaponCatalog.ENTRIES.size / SLOT_STEP) * STEP_BONUS

    /** 이 검이 여는 도감 칸. */
    fun slotOf(sword: Sword): CodexEntry =
        WeaponCatalog.slotFor(sword.family, sword.level)

    /** 아직 안 찬 칸이면 바칠 수 있다. 이미 찬 칸에 태우게 두지 않는다. */
    fun canOffer(progress: ProgressState, sword: Sword): Boolean =
        slotOf(sword) !in Progress.entriesOf(progress)

    /** 칸을 연다. 검을 없애는 것은 부르는 쪽(뷰모델)의 몫이다. */
    fun offer(progress: ProgressState, difficulty: Difficulty, sword: Sword): ProgressState =
        Progress.refresh(Progress.registerSword(progress, difficulty, sword))

    /** 채운 칸 수로 정해지는 보너스. 성공률과 파괴방지가 같은 크기다. */
    fun bonusOf(progress: ProgressState): ForgeBonus {
        val steps = Progress.entriesOf(progress).size / SLOT_STEP
        val value = (steps * STEP_BONUS).coerceAtMost(MAX_BONUS)
        return ForgeBonus(successRate = value, destroyGuard = value)
    }
}
```

`ForgeBonuses.sourcesOf` 를 채운다:

```kotlin
    fun sourcesOf(state: GameState, progress: ProgressState): List<BonusSource> = listOf(
        BonusSource(
            label = "도감",
            detail = "${Progress.entriesOf(progress).size} / ${WeaponCatalog.ENTRIES.size}",
            bonus = CodexOffer.bonusOf(progress),
        ),
    )
```

- [ ] **Step 4: 자동 등록을 끊는다**

`Progress.registerSword` 는 그대로 두되(바치기가 쓴다), **저절로 부르던 자리**를 없앤다.
`app/src/main/java/com/geomgang/game/ForgeViewModel.kt` 에서 `Progress.registerSword` 를
부르는 곳을 전부 찾아 지운다:

```bash
cd /c/workAndroid/SwordForge && grep -n "registerSword" app/src/main/java/com/geomgang/game/ForgeViewModel.kt
```

`craft`·`buySword`·`buyToStorage`·`fuse` 등에서 부르던 것을 지운다.
`Progress.onAttempt` 안에서도 부르고 있으면 그 호출만 지운다(통계 갱신은 남긴다).

- [ ] **Step 5: 테스트가 통과하는지 확인한다**

```bash
cd /c/workAndroid/SwordForge && ./gradlew.bat :core:test :app:testDebugUnitTest --console=plain
```

Expected: PASS. 도감 자동 등록을 검사하던 기존 테스트가 있으면 **바치기로 바꿔서** 고친다 —
테스트를 지우지 말고 새 규칙을 검사하게 만든다.

- [ ] **Step 6: 커밋한다**

```bash
cd /c/workAndroid/SwordForge && git add -A && git commit -m "도감은 바쳐야 열린다"
```

---

### Task 3: 대장간 스킬

**Files:**
- Create: `core/src/main/kotlin/com/geomgang/core/Smithy.kt`
- Modify: `core/src/main/kotlin/com/geomgang/core/Progress.kt` (`smithyLevel`)
- Modify: `core/src/main/kotlin/com/geomgang/core/ForgeBonus.kt`
- Test: `core/src/test/kotlin/com/geomgang/core/SmithyTest.kt`

**Interfaces:**
- Produces:
  - `ProgressState.smithyLevel: Int`
  - `Smithy.MAX_LEVEL = 15`, `Smithy.PER_LEVEL = 0.002`
  - `Smithy.priceOf(state: GameState, level: Int): Long`
  - `Smithy.canUpgrade(state, progress): Boolean`
  - `Smithy.bonusOf(progress): ForgeBonus`

- [ ] **Step 1: 실패하는 테스트를 작성한다**

`core/src/test/kotlin/com/geomgang/core/SmithyTest.kt`:

```kotlin
package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmithyTest {

    private fun rich(bestLevel: Int = 20) = GameState(
        difficulty = Difficulty.ENDLESS,
        gold = 1_000_000_000_000_000L,
        bestLevel = bestLevel,
    )

    @Test
    fun `레벨당 같은 몫이 오른다`() {
        assertEquals(0.0, Smithy.bonusOf(ProgressState()).successRate, 1e-9)
        assertEquals(
            Smithy.PER_LEVEL * 5,
            Smithy.bonusOf(ProgressState(smithyLevel = 5)).successRate,
            1e-9,
        )
    }

    @Test
    fun `상한을 넘지 않는다`() {
        val capped = ProgressState(smithyLevel = Smithy.MAX_LEVEL)
        assertEquals(Smithy.PER_LEVEL * Smithy.MAX_LEVEL, Smithy.bonusOf(capped).successRate, 1e-9)
        assertFalse(Smithy.canUpgrade(rich(), capped))
    }

    @Test
    fun `값은 레벨이 오를수록 비싸진다`() {
        val s = rich()
        assertTrue(Smithy.priceOf(s, 1) > Smithy.priceOf(s, 0))
        assertTrue(Smithy.priceOf(s, 10) > Smithy.priceOf(s, 5))
    }

    /** 고정값이면 초반엔 못 사고 후반엔 공짜가 된다. 지금 강화 비용에 연동한다. */
    @Test
    fun `값이 최고 단계를 따라 오른다`() {
        assertTrue(Smithy.priceOf(rich(40), 0) > Smithy.priceOf(rich(10), 0))
    }

    @Test
    fun `골드가 모자라면 못 올린다`() {
        val poor = GameState(Difficulty.ENDLESS, gold = 1, bestLevel = 20)
        assertFalse(Smithy.canUpgrade(poor, ProgressState()))
    }

    @Test
    fun `올리면 레벨이 하나 오르고 골드가 빠진다`() {
        val state = rich()
        val price = Smithy.priceOf(state, 0)
        val (nextState, nextProgress) = Smithy.upgrade(state, ProgressState())
        assertEquals(1, nextProgress.smithyLevel)
        assertEquals(state.gold - price, nextState.gold)
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

```bash
cd /c/workAndroid/SwordForge && ./gradlew.bat :core:test --tests "com.geomgang.core.SmithyTest" --console=plain
```

Expected: FAIL — `Unresolved reference 'Smithy'`

- [ ] **Step 3: Smithy 를 만들고 진행도에 레벨을 더한다**

`core/src/main/kotlin/com/geomgang/core/Smithy.kt`:

```kotlin
package com.geomgang.core

import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * 대장간 스킬.
 *
 * 골드로 올리는 영구 성장이다. 골드는 후반에 쌓이기만 하고 쓸 데가 없었는데,
 * 여기가 오래 묶일 곳이 된다.
 *
 * 값을 **지금 강화 비용에 연동**한다. 고정값으로 두면 초반엔 못 사고 후반엔 공짜가
 * 된다 — 강화석([GoldShop])이 같은 이유로 같은 방식을 쓴다.
 */
object Smithy {

    const val MAX_LEVEL: Int = 15

    /** 레벨 하나가 주는 몫. 0.002 는 0.2%p 다. 성공률과 파괴방지가 같은 크기다. */
    const val PER_LEVEL: Double = 0.002

    /** 첫 레벨 값 = 지금 강화 비용 × 이 값. */
    private const val BASE_MULT = 5.0

    /** 레벨마다 붙는 배수. */
    private const val GROWTH = 1.5

    /** [level] 에서 다음 레벨로 올리는 값. */
    fun priceOf(state: GameState, level: Int): Long {
        val base = Economy.upgradeCost(state.bestLevel) * BASE_MULT
        return (base * GROWTH.pow(level.toDouble())).roundToLong().coerceAtLeast(1)
    }

    fun canUpgrade(state: GameState, progress: ProgressState): Boolean {
        if (progress.smithyLevel >= MAX_LEVEL) return false
        return state.gold >= priceOf(state, progress.smithyLevel)
    }

    /** 한 레벨 올린다. 골드는 게임 상태에서, 레벨은 진행도에서 움직인다. */
    fun upgrade(state: GameState, progress: ProgressState): Pair<GameState, ProgressState> {
        check(canUpgrade(state, progress)) { "cannot upgrade smithy in this state" }
        val price = priceOf(state, progress.smithyLevel)
        return state.copy(gold = state.gold - price) to
            progress.copy(smithyLevel = progress.smithyLevel + 1)
    }

    fun bonusOf(progress: ProgressState): ForgeBonus {
        val value = PER_LEVEL * progress.smithyLevel.coerceIn(0, MAX_LEVEL)
        return ForgeBonus(successRate = value, destroyGuard = value)
    }
}
```

`ProgressState` 에 필드를 더한다 (`clearedZones` 아래):

```kotlin
    /**
     * 대장간 스킬 레벨. 골드로 올리는 영구 성장이다.
     *
     * 진행도에 두는 이유: 모드 초기화로 지워지면 아무도 초기화를 누르지 않는다.
     */
    val smithyLevel: Int = 0,
```

`ForgeBonuses.sourcesOf` 에 한 줄 더한다:

```kotlin
        BonusSource(
            label = "대장간",
            detail = "Lv ${progress.smithyLevel}",
            bonus = Smithy.bonusOf(progress),
        ),
```

- [ ] **Step 4: 테스트가 통과하는지 확인한다**

```bash
cd /c/workAndroid/SwordForge && ./gradlew.bat :core:test :app:testDebugUnitTest --console=plain
```

Expected: PASS

- [ ] **Step 5: 커밋한다**

```bash
cd /c/workAndroid/SwordForge && git add -A && git commit -m "대장간 스킬 - 골드로 올리는 영구 성장"
```

---

### Task 4: 고유검 보유 보너스

**Files:**
- Modify: `core/src/main/kotlin/com/geomgang/core/ForgeBonus.kt`
- Test: `core/src/test/kotlin/com/geomgang/core/UniqueBonusTest.kt`

**Interfaces:**
- Consumes: `progress.uniqueFound: Set<String>`, `UniqueSwords.RECIPES`
- Produces: `UniqueSwords.holdingBonus(progress): ForgeBonus`, `UniqueSwords.PER_UNIQUE = 0.003`

- [ ] **Step 1: 실패하는 테스트를 작성한다**

`core/src/test/kotlin/com/geomgang/core/UniqueBonusTest.kt`:

```kotlin
package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 고유검은 한 번 발견한 사실 자체가 보너스다. 들고 있지 않아도 된다.
 *
 * 발견은 이미 영구 기록(`uniqueFound`)이라 새 저장 필드가 필요 없다.
 */
class UniqueBonusTest {

    @Test
    fun `하나도 없으면 0이다`() {
        assertEquals(0.0, UniqueSwords.holdingBonus(ProgressState()).successRate, 1e-9)
    }

    @Test
    fun `한 종류당 같은 몫이 붙는다`() {
        val three = ProgressState(uniqueFound = UniqueSwords.RECIPES.take(3).map { it.id }.toSet())
        assertEquals(UniqueSwords.PER_UNIQUE * 3, UniqueSwords.holdingBonus(three).successRate, 1e-9)
    }

    @Test
    fun `전부 모으면 상한이다`() {
        val all = ProgressState(uniqueFound = UniqueSwords.RECIPES.map { it.id }.toSet())
        val expected = UniqueSwords.PER_UNIQUE * UniqueSwords.RECIPES.size
        assertEquals(expected, UniqueSwords.holdingBonus(all).successRate, 1e-9)
        assertEquals(expected, UniqueSwords.holdingBonus(all).destroyGuard, 1e-9)
    }

    /** 모르는 id 가 섞여 있어도 실제 고유검만 센다. */
    @Test
    fun `모르는 id 는 세지 않는다`() {
        val junk = ProgressState(uniqueFound = setOf("없는것", "이상한것"))
        assertEquals(0.0, UniqueSwords.holdingBonus(junk).successRate, 1e-9)
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

```bash
cd /c/workAndroid/SwordForge && ./gradlew.bat :core:test --tests "com.geomgang.core.UniqueBonusTest" --console=plain
```

Expected: FAIL — `Unresolved reference 'holdingBonus'`

- [ ] **Step 3: UniqueSwords 에 더한다**

`core/src/main/kotlin/com/geomgang/core/UniqueSwords.kt` 의 `object UniqueSwords` 안에 넣는다:

```kotlin
    /** 고유검 한 종류를 발견한 값. 0.003 은 0.3%p 다. */
    const val PER_UNIQUE: Double = 0.003

    /**
     * 발견한 고유검 수로 정해지는 보너스.
     *
     * 들고 있을 필요가 없다 — 한 번 만들어 봤다는 사실이 대장장이의 실력이다.
     * 모르는 id 는 세지 않는다(옛 세이브나 손댄 세이브 대비).
     */
    fun holdingBonus(progress: ProgressState): ForgeBonus {
        val known = progress.uniqueFound.count { byId(it) != null }
        val value = PER_UNIQUE * known
        return ForgeBonus(successRate = value, destroyGuard = value)
    }
```

`ForgeBonuses.sourcesOf` 에 한 줄 더한다:

```kotlin
        BonusSource(
            label = "고유검",
            detail = "${progress.uniqueFound.count { UniqueSwords.byId(it) != null }} / " +
                "${UniqueSwords.RECIPES.size}",
            bonus = UniqueSwords.holdingBonus(progress),
        ),
```

- [ ] **Step 4: 테스트가 통과하는지 확인한다**

```bash
cd /c/workAndroid/SwordForge && ./gradlew.bat :core:test :app:testDebugUnitTest --console=plain
```

Expected: PASS

- [ ] **Step 5: 커밋한다**

```bash
cd /c/workAndroid/SwordForge && git add -A && git commit -m "고유검은 발견한 것만으로 대장장이의 실력이 된다"
```

---

### Task 5: 계열별 강화 특성

**Files:**
- Create: `core/src/main/kotlin/com/geomgang/core/FamilyForge.kt`
- Modify: `core/src/main/kotlin/com/geomgang/core/ForgeBonus.kt`
- Test: `core/src/test/kotlin/com/geomgang/core/FamilyForgeTest.kt`

**Interfaces:**
- Produces:
  - `Sword.isLegend(): Boolean`
  - `enum class FamilyForge` — `successBonus`, `destroyGuard`, `temperMult`, `temperCapBonus`, `costMult`, `stoneRelief`, `salvageMult`, `blessingMult`, `codexPair`, `refundStones`, `blurb`
  - `FamilyForge.of(sword: Sword?): FamilyForge`

- [ ] **Step 1: 실패하는 테스트를 작성한다**

`core/src/test/kotlin/com/geomgang/core/FamilyForgeTest.kt`:

```kotlin
package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 계열마다 강화 특성이 다르다. 여기서 계열이 처음으로 성격을 갖는다.
 *
 * 전투 특성([FamilyStyle])과 한 enum 에 섞지 않는다 — 서로 다른 이유로 바뀌는 값이다.
 */
class FamilyForgeTest {

    @Test
    fun `단계가 전설 여부를 정한다`() {
        assertFalse(Sword(WeaponFamily.STRAIGHT, 20).isLegend())
        assertTrue(Sword(WeaponFamily.STRAIGHT, 21).isLegend())
    }

    /** 전설검은 계열을 무시한다. +21 위면 무엇이든 전설이다. */
    @Test
    fun `전설검은 계열을 무시한다`() {
        assertEquals(FamilyForge.LEGEND, FamilyForge.of(Sword(WeaponFamily.STRAIGHT, 30)))
        assertEquals(FamilyForge.LEGEND, FamilyForge.of(Sword(WeaponFamily.VOID, 30)))
    }

    @Test
    fun `계열마다 다른 특성을 갖는다`() {
        assertEquals(FamilyForge.STRAIGHT, FamilyForge.of(Sword(WeaponFamily.STRAIGHT, 5)))
        assertEquals(FamilyForge.RAPIER, FamilyForge.of(Sword(WeaponFamily.RAPIER, 5)))
    }

    @Test
    fun `검이 없으면 아무 특성도 없다`() {
        val none = FamilyForge.of(null)
        assertEquals(0.0, none.successBonus, 1e-9)
        assertEquals(1.0, none.temperMult, 1e-9)
        assertEquals(1.0, none.costMult, 1e-9)
    }

    /** 전설검이 모든 계열보다 성공률이 높아야 한다. 가장 어려운 길의 보상이다. */
    @Test
    fun `전설검이 가장 강하다`() {
        val others = FamilyForge.entries.filter { it != FamilyForge.LEGEND && it != FamilyForge.NONE }
        assertTrue(others.all { FamilyForge.LEGEND.successBonus >= it.successBonus })
        assertTrue(others.all { FamilyForge.LEGEND.destroyGuard >= it.destroyGuard })
    }

    @Test
    fun `세검은 담금질이 두 배로 쌓인다`() {
        assertEquals(2.0, FamilyForge.of(Sword(WeaponFamily.RAPIER, 5)).temperMult, 1e-9)
    }

    @Test
    fun `도끼검은 강화 비용이 싸다`() {
        assertTrue(FamilyForge.of(Sword(WeaponFamily.AXE, 5)).costMult < 1.0)
    }

    @Test
    fun `열네 계열이 모두 특성을 갖는다`() {
        for (family in WeaponFamily.entries) {
            val forge = FamilyForge.of(Sword(family, 5))
            assertTrue("$family", forge != FamilyForge.NONE)
            assertTrue("$family", forge.blurb.isNotBlank())
        }
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

```bash
cd /c/workAndroid/SwordForge && ./gradlew.bat :core:test --tests "com.geomgang.core.FamilyForgeTest" --console=plain
```

Expected: FAIL — `Unresolved reference 'FamilyForge'`

- [ ] **Step 3: FamilyForge 를 만든다**

`core/src/main/kotlin/com/geomgang/core/FamilyForge.kt`:

```kotlin
package com.geomgang.core

/**
 * 이 검이 전설검인지. **계열이 아니라 단계가 정한다.**
 *
 * 전설검을 [WeaponFamily] 에 넣지 않는 이유: 계열이 15종이 되면 도감 계열칸이
 * 294 에서 315 로 늘어나는데, +0 짜리 전설검은 존재할 수 없으므로 영원히 못 채우는
 * 칸 21 개가 생긴다.
 */
fun Sword.isLegend(): Boolean = level > RateTable.MAX_FINITE_LEVEL

/**
 * 계열별 강화 특성.
 *
 * [FamilyStyle] 이 **싸우는 방식**을 담듯 여기는 **벼리는 방식**을 담는다.
 * 한 enum 에 섞지 않는 이유: 전투 밸런스와 강화 밸런스는 서로 다른 이유로 바뀐다.
 *
 * 합산되지 않는다 — **든 검에만** 붙는다.
 *
 * @param successBonus   성공률 가산
 * @param destroyGuard   파괴 판정을 무효로 돌릴 확률
 * @param temperMult     담금질이 쌓이는 배수
 * @param temperCapBonus 담금질 상한 가산
 * @param costMult       강화 비용 배수
 * @param stoneRelief    강화석 요구를 줄이는 개수
 * @param salvageMult    파괴 후 줍는 조각 배수
 * @param blessingMult   축복서 효과 배수
 * @param codexPair      도감에 바칠 때 다음 단계 칸도 함께 여는지
 * @param refundStones   실패해도 강화석을 돌려받는지
 */
enum class FamilyForge(
    val successBonus: Double = 0.0,
    val destroyGuard: Double = 0.0,
    val temperMult: Double = 1.0,
    val temperCapBonus: Double = 0.0,
    val costMult: Double = 1.0,
    val stoneRelief: Int = 0,
    val salvageMult: Double = 1.0,
    val blessingMult: Double = 1.0,
    val codexPair: Boolean = false,
    val refundStones: Boolean = false,
    val blurb: String = "",
) {
    NONE(blurb = ""),

    STRAIGHT(successBonus = 0.005, blurb = "성공률이 조금 높다"),
    CURVED(temperMult = 1.5, blurb = "담금질이 1.5배로 쌓인다"),
    GREAT(destroyGuard = 0.03, blurb = "잘 부서지지 않는다"),
    RAPIER(temperMult = 2.0, blurb = "담금질이 두 배로 쌓인다"),
    TWIN(successBonus = 0.003, destroyGuard = 0.015, blurb = "성공률과 내구가 조금씩"),
    DEMON(salvageMult = 2.0, blurb = "부서져도 조각을 두 배로 줍는다"),
    HOLY(blessingMult = 1.5, blurb = "축복서가 더 잘 듣는다"),
    DRAGON(successBonus = 0.002, destroyGuard = 0.02, blurb = "단단하고 조금 잘 붙는다"),
    SCYTHE(codexPair = true, blurb = "도감에 바치면 다음 칸도 함께 열린다"),
    AXE(costMult = 0.8, blurb = "강화 비용이 싸다"),
    SPEAR(stoneRelief = 2, blurb = "강화석이 두 개 덜 든다"),
    SPIRIT(temperCapBonus = 0.10, blurb = "담금질 상한이 높다"),
    FUSED(
        successBonus = 0.0025,
        destroyGuard = 0.015,
        temperMult = 1.25,
        costMult = 0.9,
        salvageMult = 1.5,
        blessingMult = 1.25,
        blurb = "여러 계열의 벼림을 조금씩 전부",
    ),
    VOID(refundStones = true, blurb = "실패해도 강화석을 돌려받는다"),

    /** 가장 어려운 길의 보상. 어느 계열보다 강하다. */
    LEGEND(
        successBonus = 0.03,
        destroyGuard = 0.03,
        temperCapBonus = 0.20,
        blurb = "전설검. 벼림의 끝",
    ),
    ;

    companion object {
        /** 전설 여부를 먼저 본다. +21 위면 계열이 무엇이든 전설이다. */
        fun of(sword: Sword?): FamilyForge {
            if (sword == null) return NONE
            if (sword.isLegend()) return LEGEND
            return when (sword.family) {
                WeaponFamily.STRAIGHT -> STRAIGHT
                WeaponFamily.CURVED -> CURVED
                WeaponFamily.GREAT -> GREAT
                WeaponFamily.RAPIER -> RAPIER
                WeaponFamily.TWIN -> TWIN
                WeaponFamily.DEMON -> DEMON
                WeaponFamily.HOLY -> HOLY
                WeaponFamily.DRAGON -> DRAGON
                WeaponFamily.SCYTHE -> SCYTHE
                WeaponFamily.AXE -> AXE
                WeaponFamily.SPEAR -> SPEAR
                WeaponFamily.SPIRIT -> SPIRIT
                WeaponFamily.FUSED -> FUSED
                WeaponFamily.VOID -> VOID
            }
        }
    }
}
```

`ForgeBonuses.sourcesOf` 에 든 검 몫을 더한다:

```kotlin
        FamilyForge.of(state.sword).let { forge ->
            BonusSource(
                label = if (state.sword?.isLegend() == true) "전설검" else "계열",
                detail = forge.blurb,
                bonus = ForgeBonus(forge.successBonus, forge.destroyGuard),
            )
        },
```

- [ ] **Step 4: 테스트가 통과하는지 확인한다**

```bash
cd /c/workAndroid/SwordForge && ./gradlew.bat :core:test :app:testDebugUnitTest --console=plain
```

Expected: PASS

- [ ] **Step 5: 커밋한다**

```bash
cd /c/workAndroid/SwordForge && git add -A && git commit -m "계열마다 벼리는 방식이 다르다"
```

---

### Task 6: 계열 특성을 실제로 적용한다

Task 5 는 표만 만들었다. 여기서 강화 판정·비용·담금질에 물린다.

**Files:**
- Modify: `core/src/main/kotlin/com/geomgang/core/ForgeEngine.kt`
- Modify: `core/src/main/kotlin/com/geomgang/core/ForgeCost.kt`
- Modify: `core/src/main/kotlin/com/geomgang/core/Tempering.kt`
- Test: `core/src/test/kotlin/com/geomgang/core/FamilyForgeEffectTest.kt`

**Interfaces:**
- Consumes: `FamilyForge.of` (Task 5)
- Produces: `Tempering.rateFor(baseRate, targetLevel, fails, capBonus: Double)`, `ForgeCost.requirementFor(level, relief: Int)`

- [ ] **Step 1: 실패하는 테스트를 작성한다**

`core/src/test/kotlin/com/geomgang/core/FamilyForgeEffectTest.kt`:

```kotlin
package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** 계열 특성이 말로만 있지 않고 실제로 물리는지. */
class FamilyForgeEffectTest {

    private fun alwaysFail(): Random = object : Random() {
        override fun nextBits(bitCount: Int): Int =
            if (bitCount >= 32) -1 else (1 shl bitCount) - 1
    }

    private fun at(family: WeaponFamily, level: Int) = GameState(
        difficulty = Difficulty.ENDLESS,
        gold = 1_000_000_000_000_000L,
        sword = Sword(family, level),
        forgeStones = 999,
    )

    @Test
    fun `창검은 강화석이 두 개 덜 든다`() {
        val plain = ForgeCost.requirementFor(30).stones
        val spear = ForgeCost.requirementFor(30, relief = 2).stones
        assertEquals(plain - 2, spear)
    }

    @Test
    fun `강화석 요구는 0 아래로 내려가지 않는다`() {
        assertEquals(0, ForgeCost.requirementFor(0, relief = 5).stones)
    }

    @Test
    fun `정령검은 담금질 상한이 높다`() {
        val plain = Tempering.rateFor(0.005, 45, 100_000)
        val spirit = Tempering.rateFor(0.005, 45, 100_000, capBonus = 0.10)
        assertEquals(Tempering.MAX_RATE, plain, 1e-9)
        assertEquals(Tempering.MAX_RATE + 0.10, spirit, 1e-9)
    }

    @Test
    fun `세검은 실패 한 번에 담금질이 두 칸 쌓인다`() {
        val result = ForgeEngine.attempt(at(WeaponFamily.RAPIER, 30), UsedItems.NONE, alwaysFail())
        assertEquals(2, result.state.temperFails)
    }

    @Test
    fun `직검은 한 칸만 쌓인다`() {
        val result = ForgeEngine.attempt(at(WeaponFamily.STRAIGHT, 30), UsedItems.NONE, alwaysFail())
        assertEquals(1, result.state.temperFails)
    }

    @Test
    fun `도끼검은 강화 비용이 싸다`() {
        val axe = ForgeEngine.attempt(at(WeaponFamily.AXE, 10), UsedItems.NONE, alwaysFail())
        val plain = ForgeEngine.attempt(at(WeaponFamily.STRAIGHT, 10), UsedItems.NONE, alwaysFail())
        assertTrue("axe=${axe.state.gold} plain=${plain.state.gold}", axe.state.gold > plain.state.gold)
    }

    @Test
    fun `허검은 실패해도 강화석을 돌려받는다`() {
        val before = at(WeaponFamily.VOID, 30)
        val result = ForgeEngine.attempt(before, UsedItems.NONE, alwaysFail())
        assertEquals(before.forgeStones, result.state.forgeStones)
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

```bash
cd /c/workAndroid/SwordForge && ./gradlew.bat :core:test --tests "com.geomgang.core.FamilyForgeEffectTest" --console=plain
```

Expected: FAIL — `requirementFor` 에 `relief` 인자가 없다는 컴파일 오류

- [ ] **Step 3: 세 곳에 인자를 더한다**

`Tempering.rateFor` 에 상한 가산을 더한다:

```kotlin
    /**
     * 담금질을 반영한 성공률.
     *
     * @param capBonus 계열 특성이 올려 주는 상한 가산. 정령검·전설검이 쓴다
     */
    fun rateFor(baseRate: Double, targetLevel: Int, fails: Int, capBonus: Double = 0.0): Double {
        if (!applies(targetLevel) || fails <= 0) return baseRate
        val raised = baseRate + baseRate * STEP_RATIO * fails
        return maxOf(baseRate, minOf(raised, MAX_RATE + capBonus))
    }
```

`RateTable.successRate` 가 그 값을 넘기도록 인자를 더한다:

```kotlin
    fun successRate(
        difficulty: Difficulty,
        targetLevel: Int,
        blessing: Boolean = false,
        temperFails: Int = 0,
        bonus: Double = 0.0,
        temperCapBonus: Double = 0.0,
        blessingMult: Double = 1.0,
    ): Double {
        val scaled = baseSuccessRate(targetLevel) * difficulty.multiplier
        val tempered = Tempering.rateFor(scaled, targetLevel, temperFails, temperCapBonus)
        val boosted = if (blessing) tempered + BLESSING_BONUS * blessingMult else tempered
        return minOf(boosted + bonus, MAX_SUCCESS_RATE)
    }
```

`ForgeCost.requirementFor` 에 감면을 더한다:

```kotlin
    /** @param relief 계열 특성이 깎아 주는 강화석 수. 창검이 쓴다 */
    fun requirementFor(currentLevel: Int, relief: Int = 0): ForgeRequirement {
        // 기존 본문에서 stones 를 구한 뒤 마지막에 깎는다
        ...
        return ForgeRequirement(gold, (stones - relief).coerceAtLeast(0))
    }
```

`ForgeEngine.attempt` 에서 계열 특성을 읽어 전부 물린다. `val sword = requireNotNull(state.sword)`
바로 아래에 넣는다:

```kotlin
        val forge = FamilyForge.of(sword)
```

비용 계산을 바꾼다 (`paid` 를 만드는 곳):

```kotlin
        val cost = (Economy.upgradeCost(sword.level) * forge.costMult).roundToLong()
        val paid = state.copy(gold = state.gold - cost, inventory = inventory)
```

담금질 누적을 계열 배수만큼 올린다 (`failed` 를 만드는 곳):

```kotlin
        val step = forge.temperMult.roundToInt().coerceAtLeast(1)
        val failed = if (Tempering.applies(targetLevel)) {
            paid.copy(temperLevel = targetLevel, temperFails = fails + step)
        } else {
            paid
        }
```

성공률 계산에 계열 몫을 넘긴다:

```kotlin
        val successRate = (
            RateTable.successRate(
                state.difficulty,
                targetLevel,
                items.blessing,
                fails,
                bonus.successRate + forge.successBonus,
                forge.temperCapBonus,
                forge.blessingMult,
            ) + UniqueSwords.forgeBonusOf(sword)
            ).coerceAtMost(RateTable.MAX_SUCCESS_RATE)
```

파괴 방지 굴림에 계열 몫을 더한다:

```kotlin
                    val guard = bonus.destroyGuard + forge.destroyGuard
                    if (guard > 0 && rng.nextDouble() < guard) {
```

**허검의 강화석 환급**은 뷰모델이 맡는다 (강화석을 빼는 곳이 거기다). `ForgeViewModel.runAttempt`
에서 강화석을 뺀 뒤, 결과가 성공이 아니고 `forge.refundStones` 면 되돌린다.

`ForgeEngine.kt` 상단에 `import kotlin.math.roundToInt` 와 `import kotlin.math.roundToLong` 을 더한다.

- [ ] **Step 4: 뷰모델의 강화석·조각 처리를 맞춘다**

`ForgeViewModel.runAttempt` 에서 `ForgeCost.requirementFor(sword.level)` 을
`ForgeCost.requirementFor(sword.level, FamilyForge.of(sword).stoneRelief)` 로 바꾸고,
강화석을 뺀 뒤 결과를 보고 되돌린다:

```kotlin
        val forge = FamilyForge.of(sword)
        val req = ForgeCost.requirementFor(sword.level, forge.stoneRelief)
        if (req.stones > 0) {
            game = game.copy(forgeStones = game.forgeStones - req.stones)
        }
        ...
        // 허검은 실패해도 강화석을 돌려받는다. 성공했을 때는 정상 소모다.
        if (forge.refundStones && result !is ForgeResult.Success && req.stones > 0) {
            game = game.copy(forgeStones = game.forgeStones + req.stones)
        }
```

`salvage()` 에서 조각 배수를 물린다 — `ForgeEngine.applySalvage` 뒤에 계열 배수를 곱하는
대신, `pendingDestroy` 의 계열로 `FamilyForge` 를 찾아 회수량을 늘린다:

```kotlin
        val forge = FamilyForge.of(game.pendingDestroy?.let { Sword(it.family, it.level) })
        val before = game.shards
        game = ForgeEngine.applySalvage(game, rng)
        val gained = game.shards - before
        val extra = (gained * (forge.salvageMult - 1.0)).roundToLong()
        if (extra > 0) game = game.copy(shards = game.shards + extra.toInt())
```

`ForgeViewModel.kt` 상단에 `import com.geomgang.core.FamilyForge` 와
`import com.geomgang.core.isLegend` 를 더한다.

- [ ] **Step 5: 테스트가 통과하는지 확인한다**

```bash
cd /c/workAndroid/SwordForge && ./gradlew.bat :core:test :app:testDebugUnitTest --console=plain
```

Expected: PASS. `BalanceSimulationTest` 도 통과해야 한다 — 시뮬레이터는 직검(STRAIGHT)만
쓰는데 직검 특성은 성공률 +0.5%p 뿐이라 영향이 작다. **만약 깨지면 STRAIGHT 의
`successBonus` 를 0 으로 내린다** — 기본 계열은 기준이지 특전이 아니다.

- [ ] **Step 6: 커밋한다**

```bash
cd /c/workAndroid/SwordForge && git add -A && git commit -m "계열 특성을 판정에 물린다"
```

---

### Task 7: 허검을 조합으로 내린다

**Files:**
- Modify: `core/src/main/kotlin/com/geomgang/core/FusionTable.kt`
- Test: `core/src/test/kotlin/com/geomgang/core/FusionTableTest.kt` (기존 파일에 추가)

**Interfaces:**
- Produces: `FusionTable.resultFor(setOf(AXE, SPEAR)) == WeaponFamily.VOID`

- [ ] **Step 1: 실패하는 테스트를 작성한다**

기존 `FusionTableTest.kt` 에 더한다 (파일이 없으면 만든다):

```kotlin
    /**
     * 허검을 회랑 10층에서 내렸다.
     *
     * 강화 게임인데 회랑 진도에 강제로 묶이는 것이 전설검 재료로는 너무 높은 문턱이었다.
     * 회랑 보상은 남아 있어 길이 둘이 된다.
     */
    @Test
    fun `도끼검과 창검을 합치면 허검이 된다`() {
        assertEquals(
            WeaponFamily.VOID,
            FusionTable.resultFor(setOf(WeaponFamily.AXE, WeaponFamily.SPEAR)),
        )
    }

    /** 조합표에 있는 결과는 서로 겹치지 않아야 한다. */
    @Test
    fun `조합 결과가 겹치지 않는다`() {
        val results = FusionTable.ALL.map { it.result }
        assertEquals(results.size, results.toSet().size)
    }
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

```bash
cd /c/workAndroid/SwordForge && ./gradlew.bat :core:test --tests "com.geomgang.core.FusionTableTest" --console=plain
```

Expected: FAIL — `resultFor` 가 null 을 돌려준다

- [ ] **Step 3: 조합표에 한 줄 더한다**

`FusionTable.ALL` 의 마지막 항목 뒤에 넣는다:

```kotlin
        FusionEntry(
            setOf(WeaponFamily.AXE, WeaponFamily.SPEAR),
            WeaponFamily.VOID,
            "도끼검 + 창검",
        ),
```

`FusionTable` 의 KDoc 에 한 줄 더한다:

```
 * 허검은 무한 회랑 10층 돌파로도 얻지만 여기 조합으로도 얻는다. 강화 게임인데
 * 회랑 진도에 강제로 묶이면 전설검 재료로 쓸 수 없다.
```

- [ ] **Step 4: 테스트가 통과하는지 확인한다**

```bash
cd /c/workAndroid/SwordForge && ./gradlew.bat :core:test :app:testDebugUnitTest --console=plain
```

Expected: PASS

- [ ] **Step 5: 커밋한다**

```bash
cd /c/workAndroid/SwordForge && git add -A && git commit -m "허검을 도끼검과 창검 조합으로"
```

---

### Task 8: 전설검 등급

**Files:**
- Create: `core/src/main/kotlin/com/geomgang/core/LegendForge.kt`
- Modify: `core/src/main/kotlin/com/geomgang/core/Progress.kt` (`legendUnlocked`)
- Modify: `core/src/main/kotlin/com/geomgang/core/ForgeEngine.kt`
- Test: `core/src/test/kotlin/com/geomgang/core/LegendForgeTest.kt`

**Interfaces:**
- Produces:
  - `ProgressState.legendUnlocked: Boolean`
  - `LegendForge.MATERIALS: List<WeaponFamily>`, `LegendForge.MATERIAL_LEVEL = 20`, `LegendForge.RECRAFT_SHARDS = 500`
  - `LegendForge.missingFor(state): List<WeaponFamily>`
  - `LegendForge.canCraft(state, progress): Boolean`, `LegendForge.craft(state, progress): Pair<GameState, ProgressState>`
  - `LegendForge.canRecraft(state, progress): Boolean`, `LegendForge.recraft(state): GameState`
  - `LegendForge.canForge(sword): Boolean`

- [ ] **Step 1: 실패하는 테스트를 작성한다**

`core/src/test/kotlin/com/geomgang/core/LegendForgeTest.kt`:

```kotlin
package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LegendForgeTest {

    private fun alwaysFail(): Random = object : Random() {
        override fun nextBits(bitCount: Int): Int =
            if (bitCount >= 32) -1 else (1 shl bitCount) - 1
    }

    private fun withMaterials() = GameState(
        difficulty = Difficulty.ENDLESS,
        gold = 1_000_000_000_000_000L,
        storage = LegendForge.MATERIALS.map { Sword(it, LegendForge.MATERIAL_LEVEL) },
    )

    // --- 계열 상한 ---

    @Test
    fun `계열 검은 20에서 더 못 올린다`() {
        assertFalse(LegendForge.canForge(Sword(WeaponFamily.STRAIGHT, 20)))
        assertTrue(LegendForge.canForge(Sword(WeaponFamily.STRAIGHT, 19)))
    }

    @Test
    fun `전설검은 계속 올릴 수 있다`() {
        assertTrue(LegendForge.canForge(Sword(WeaponFamily.STRAIGHT, 21)))
        assertTrue(LegendForge.canForge(Sword(WeaponFamily.STRAIGHT, 44)))
    }

    // --- 조합 ---

    @Test
    fun `재료가 다 있으면 벼릴 수 있다`() {
        assertTrue(LegendForge.canCraft(withMaterials(), ProgressState()))
        assertTrue(LegendForge.missingFor(withMaterials()).isEmpty())
    }

    @Test
    fun `모자란 재료를 알려 준다`() {
        val two = GameState(
            difficulty = Difficulty.ENDLESS,
            storage = LegendForge.MATERIALS.take(2).map { Sword(it, 20) },
        )
        assertEquals(LegendForge.MATERIALS.drop(2), LegendForge.missingFor(two))
        assertFalse(LegendForge.canCraft(two, ProgressState()))
    }

    @Test
    fun `단계가 모자라면 재료로 안 쳐준다`() {
        val low = GameState(
            difficulty = Difficulty.ENDLESS,
            storage = LegendForge.MATERIALS.map { Sword(it, 19) },
        )
        assertFalse(LegendForge.canCraft(low, ProgressState()))
    }

    @Test
    fun `벼리면 재료가 사라지고 전설검이 손에 온다`() {
        val (state, _) = LegendForge.craft(withMaterials(), ProgressState())
        assertEquals(21, state.sword?.level)
        assertTrue(state.sword!!.isLegend())
        assertTrue(state.storage.isEmpty())
    }

    // --- 전설 해금 ---

    @Test
    fun `처음 벼려도 아직 해금은 아니다`() {
        val (_, progress) = LegendForge.craft(withMaterials(), ProgressState())
        assertFalse(progress.legendUnlocked)
    }

    /** 도감에 바쳐야 해금이 남는다. 검은 사라지지만 벽을 넘은 기록은 영구다. */
    @Test
    fun `전설검을 도감에 바치면 해금이 남는다`() {
        val progress = LegendForge.onOffered(ProgressState(), Sword(WeaponFamily.STRAIGHT, 30))
        assertTrue(progress.legendUnlocked)
    }

    @Test
    fun `계열 검을 바쳐도 해금되지 않는다`() {
        val progress = LegendForge.onOffered(ProgressState(), Sword(WeaponFamily.STRAIGHT, 10))
        assertFalse(progress.legendUnlocked)
    }

    @Test
    fun `해금되면 조각으로 다시 벼린다`() {
        val unlocked = ProgressState(legendUnlocked = true)
        val rich = GameState(Difficulty.ENDLESS, shards = LegendForge.RECRAFT_SHARDS)
        assertTrue(LegendForge.canRecraft(rich, unlocked))

        val after = LegendForge.recraft(rich)
        assertEquals(21, after.sword?.level)
        assertEquals(0, after.shards)
    }

    @Test
    fun `해금 전에는 조각으로 못 벼린다`() {
        val rich = GameState(Difficulty.ENDLESS, shards = 99_999)
        assertFalse(LegendForge.canRecraft(rich, ProgressState()))
    }

    // --- 파괴 ---

    /** 재료 넷을 다시 모으는 것은 몇 시간을 지우는 일이라 누를 엄두가 안 난다. */
    @Test
    fun `전설검은 파괴돼도 사라지지 않고 21로 돌아간다`() {
        val state = GameState(
            difficulty = Difficulty.ENDLESS,
            gold = 1_000_000_000_000_000L,
            sword = Sword(WeaponFamily.STRAIGHT, 44),
            forgeStones = 999,
        )
        var result = ForgeEngine.attempt(state, UsedItems.NONE, alwaysFail())
        // 파괴가 나올 때까지 굴린다
        var guard = 0
        while (result.state.sword?.level == 44 && guard++ < 200) {
            result = ForgeEngine.attempt(result.state, UsedItems.NONE, alwaysFail())
        }
        assertTrue("결과=$result", result.state.sword != null)
        assertEquals(21, result.state.sword?.level)
        assertEquals(null, result.state.pendingDestroy)
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

```bash
cd /c/workAndroid/SwordForge && ./gradlew.bat :core:test --tests "com.geomgang.core.LegendForgeTest" --console=plain
```

Expected: FAIL — `Unresolved reference 'LegendForge'`

- [ ] **Step 3: LegendForge 를 만든다**

`core/src/main/kotlin/com/geomgang/core/LegendForge.kt`:

```kotlin
package com.geomgang.core

/**
 * 전설검 등급.
 *
 * 계열은 +20 에서 끝난다. 그 위는 강화로 가지 않고 **조합으로만** 간다.
 * 확률표가 이미 [RateTable.MAX_FINITE_LEVEL] 20 이고 아트도 계열 +0~+20 / 전설 공용이라
 * 구조는 이미 여기에 맞춰져 있다.
 */
object LegendForge {

    /** 전설검이 시작되는 단계. */
    const val LEVEL: Int = RateTable.MAX_FINITE_LEVEL + 1

    /** 재료가 갖춰야 하는 단계. */
    const val MATERIAL_LEVEL: Int = RateTable.MAX_FINITE_LEVEL

    /** 해금 뒤 다시 벼리는 조각 값. */
    const val RECRAFT_SHARDS: Int = 500

    /**
     * 재료 넷.
     *
     * 조합 나무의 3층 두 갈래(용검·정령검)와 폭넓음(합검), 그리고 허검이다.
     * 성격이 다 달라서 넷을 모았다는 것은 **전부 통달했다**는 뜻이 된다.
     */
    val MATERIALS: List<WeaponFamily> = listOf(
        WeaponFamily.DRAGON,
        WeaponFamily.SPIRIT,
        WeaponFamily.FUSED,
        WeaponFamily.VOID,
    )

    /** 이 검을 더 강화할 수 있는지. 계열은 +20 에서 멈춘다. */
    fun canForge(sword: Sword): Boolean =
        sword.isLegend() || sword.level < MATERIAL_LEVEL

    /** 아직 없는 재료. 화면이 "무엇이 필요한지" 늘 보여 주기 위한 것이다. */
    fun missingFor(state: GameState): List<WeaponFamily> {
        val have = state.storage
            .filter { it.level >= MATERIAL_LEVEL && it.uniqueId == null }
            .map { it.family }
            .toMutableList()
        return MATERIALS.filter { family ->
            if (have.remove(family)) false else true
        }
    }

    fun canCraft(state: GameState, progress: ProgressState): Boolean =
        state.sword == null && state.pendingDestroy == null && missingFor(state).isEmpty()

    /** 재료 넷을 태우고 전설검을 손에 쥔다. */
    fun craft(state: GameState, progress: ProgressState): Pair<GameState, ProgressState> {
        check(canCraft(state, progress)) { "cannot craft a legend in this state" }
        val used = mutableListOf<WeaponFamily>().apply { addAll(MATERIALS) }
        val left = state.storage.filterNot { sword ->
            sword.level >= MATERIAL_LEVEL && sword.uniqueId == null && used.remove(sword.family)
        }
        val next = state.copy(
            sword = Sword(MATERIALS.first(), LEVEL),
            storage = left,
            bestLevel = maxOf(state.bestLevel, LEVEL),
        )
        return next to progress
    }

    /**
     * 해금 뒤 조각으로 다시 벼린다.
     *
     * **+21 의 벽은 게임에서 가장 높은 벽인데, 그 벽을 넘은 사람에게 다시 넘으라고 하면
     * 아무도 두 번째 도전을 하지 않는다.**
     */
    fun canRecraft(state: GameState, progress: ProgressState): Boolean =
        progress.legendUnlocked &&
            state.sword == null &&
            state.pendingDestroy == null &&
            state.shards >= RECRAFT_SHARDS

    fun recraft(state: GameState): GameState = state.copy(
        sword = Sword(MATERIALS.first(), LEVEL),
        shards = state.shards - RECRAFT_SHARDS,
        bestLevel = maxOf(state.bestLevel, LEVEL),
    )

    /** 도감에 바친 검이 전설검이면 해금이 남는다. */
    fun onOffered(progress: ProgressState, sword: Sword): ProgressState =
        if (sword.isLegend()) progress.copy(legendUnlocked = true) else progress
}
```

`ProgressState` 에 필드를 더한다:

```kotlin
    /**
     * 전설검을 한 번이라도 도감에 바쳤는지.
     *
     * 켜지면 조각으로 전설검을 다시 벼릴 수 있다. 진행도에 두어 모드 초기화로도
     * 지워지지 않게 한다 — 가장 높은 벽을 두 번 넘으라고 하면 안 된다.
     */
    val legendUnlocked: Boolean = false,
```

`ForgeEngine.attempt` 의 파괴 분기에서 전설검을 갈아 끼운다. `ForgeResult.Destroyed(...)`
를 만드는 자리를 바꾼다:

```kotlin
                    } else if (sword.isLegend()) {
                        // 전설검은 사라지지 않는다. 재료 넷을 다시 모으는 것은 몇 시간을
                        // 지우는 일이라 누를 엄두가 안 난다. 단계를 잃는 것으로 충분하다.
                        ForgeResult.Drop(
                            state = failed.copy(sword = sword.copy(level = LegendForge.LEVEL)),
                            newLevel = LegendForge.LEVEL,
                        )
                    } else {
                        ForgeResult.Destroyed(...)
                    }
```

그리고 `drop()` 이 전설검을 +21 아래로 못 내리게 한다:

```kotlin
    private fun drop(paid: GameState, sword: Sword): ForgeResult.Drop {
        // 전설검은 +21 아래로 내려가지 않는다. 그 아래는 계열의 영역이다.
        val floor = if (sword.isLegend()) LegendForge.LEVEL else 0
        val dropped = maxOf(floor, sword.level - 1)
        return ForgeResult.Drop(
            state = paid.copy(sword = sword.copy(level = dropped)),
            newLevel = dropped,
        )
    }
```

`ForgeEngine.canRoll` 에 계열 상한을 더한다 (`val max = state.difficulty.maxLevel` 위):

```kotlin
        // 계열은 +20 에서 끝난다. 그 위는 조합으로만 간다.
        if (!LegendForge.canForge(sword)) return false
```

- [ ] **Step 4: 테스트가 통과하는지 확인한다**

```bash
cd /c/workAndroid/SwordForge && ./gradlew.bat :core:test :app:testDebugUnitTest --console=plain
```

Expected: PASS.

**`BalanceSimulationTest` 가 깨질 수 있다** — 시뮬레이터는 +20 을 넘어 무한 구간까지
올라가는데 이제 계열 검은 +20 에서 막힌다. 무한 모드 테스트가 「21 이상을 만들어 낸다」를
보고 있으므로, 시뮬레이터가 **전설 조합 경로를 타도록** `BalanceSimulation.simulateRun`
에 한 단계를 더한다: 검이 +20 에 닿고 `LegendForge.canCraft` 가 참이면 조합한다.
시뮬레이터는 재료를 모으지 않으므로, 대신 **`legendUnlocked = true` 로 두고 조각 경로**
(`LegendForge.recraft`)를 쓰게 하는 편이 단순하다. 어느 쪽이든 주석으로 이유를 남긴다.

- [ ] **Step 5: 커밋한다**

```bash
cd /c/workAndroid/SwordForge && git add -A && git commit -m "전설검 등급 - 계열은 20에서 끝나고 벽은 한 번만 넘는다"
```

---

### Task 9: 뷰모델 배선

**Files:**
- Modify: `app/src/main/java/com/geomgang/game/ForgeViewModel.kt`
- Modify: `app/src/main/java/com/geomgang/game/ForgeUiState.kt`
- Test: `app/src/test/java/com/geomgang/game/ForgeViewModelGrowthTest.kt`

**Interfaces:**
- Consumes: 전 작업의 도메인 전부
- Produces: `ForgeUiState.bonusSources`, `.smithyLevel`, `.smithyPrice`, `.canUpgradeSmithy`, `.canOfferCodex`, `.legendMissing`, `.canCraftLegend`, `.canRecraftLegend`, `.legendUnlocked`
- Produces: `ForgeViewModel.offerToCodex()`, `.upgradeSmithy()`, `.craftLegend()`, `.recraftLegend()`

- [ ] **Step 1: 실패하는 테스트를 작성한다**

`app/src/test/java/com/geomgang/game/ForgeViewModelGrowthTest.kt`:

```kotlin
package com.geomgang.game

import com.geomgang.core.Difficulty
import com.geomgang.core.GameState
import com.geomgang.core.LegendForge
import com.geomgang.core.SaveStore
import com.geomgang.core.Smithy
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class ForgeViewModelGrowthTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun vm(state: GameState): ForgeViewModel {
        val store = SaveStore(tmp.root)
        store.saveGame(state)
        return ForgeViewModel(store, Difficulty.ENDLESS)
    }

    private fun rich(sword: Sword? = Sword(WeaponFamily.STRAIGHT, 5)) = GameState(
        difficulty = Difficulty.ENDLESS,
        gold = 1_000_000_000_000_000L,
        sword = sword,
        forgeStones = 9_999,
    )

    @Test
    fun `보너스 출처가 화면 상태로 온다`() = runTest(dispatcher) {
        val sources = vm(rich()).ui.value.bonusSources
        assertTrue(sources.any { it.label == "도감" })
        assertTrue(sources.any { it.label == "대장간" })
        assertTrue(sources.any { it.label == "고유검" })
    }

    @Test
    fun `도감에 바치면 검이 사라지고 칸이 열린다`() = runTest(dispatcher) {
        val vm = vm(rich())
        assertTrue(vm.ui.value.canOfferCodex)
        vm.offerToCodex()
        assertEquals(null, vm.ui.value.sword)
        assertFalse(vm.ui.value.canOfferCodex)
    }

    @Test
    fun `대장간을 올리면 레벨이 오르고 골드가 빠진다`() = runTest(dispatcher) {
        val vm = vm(rich())
        val before = vm.ui.value.gold
        vm.upgradeSmithy()
        assertEquals(1, vm.ui.value.smithyLevel)
        assertTrue(vm.ui.value.gold < before)
    }

    @Test
    fun `대장간은 상한에서 멈춘다`() = runTest(dispatcher) {
        val vm = vm(rich())
        repeat(Smithy.MAX_LEVEL + 3) { vm.upgradeSmithy() }
        assertEquals(Smithy.MAX_LEVEL, vm.ui.value.smithyLevel)
        assertFalse(vm.ui.value.canUpgradeSmithy)
    }

    @Test
    fun `20강에서는 강화가 막힌다`() = runTest(dispatcher) {
        assertFalse(vm(rich(Sword(WeaponFamily.STRAIGHT, 20))).ui.value.canForge)
    }

    @Test
    fun `재료가 다 있으면 전설검을 벼린다`() = runTest(dispatcher) {
        val vm = vm(
            rich(sword = null).copy(
                storage = LegendForge.MATERIALS.map { Sword(it, LegendForge.MATERIAL_LEVEL) },
            ),
        )
        assertTrue(vm.ui.value.canCraftLegend)
        vm.craftLegend()
        assertEquals(LegendForge.LEVEL, vm.ui.value.sword?.level)
    }

    @Test
    fun `모자란 재료가 화면에 나온다`() = runTest(dispatcher) {
        val vm = vm(rich(sword = null))
        assertEquals(LegendForge.MATERIALS, vm.ui.value.legendMissing)
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

```bash
cd /c/workAndroid/SwordForge && ./gradlew.bat :app:testDebugUnitTest --tests "com.geomgang.game.ForgeViewModelGrowthTest" --console=plain
```

Expected: FAIL — `bonusSources` 등이 없다는 컴파일 오류

- [ ] **Step 3: UI 상태에 필드를 더한다**

`ForgeUiState` 에 넣는다:

```kotlin
    /** 강화 보너스의 출처별 내역. 화면이 "왜 이 확률인지" 말해 준다. */
    val bonusSources: List<BonusSource> = emptyList(),
    /** 지금 든 검을 도감에 바칠 수 있는지. */
    val canOfferCodex: Boolean = false,
    val smithyLevel: Int = 0,
    val smithyPrice: Long = 0,
    val canUpgradeSmithy: Boolean = false,
    /** 전설검 재료 중 아직 없는 것. */
    val legendMissing: List<WeaponFamily> = emptyList(),
    val canCraftLegend: Boolean = false,
    val canRecraftLegend: Boolean = false,
    val legendUnlocked: Boolean = false,
```

`import com.geomgang.core.BonusSource` 를 더한다.

- [ ] **Step 4: 뷰모델에 동작을 더한다**

```kotlin
    /** 든 검을 도감에 바친다. 검은 사라지고 칸이 열린다. */
    fun offerToCodex() {
        if (busy) return
        val sword = game.sword ?: return
        if (!CodexOffer.canOffer(progress, sword)) return

        progress = CodexOffer.offer(progress, game.difficulty, sword)
        // 낫검은 다음 단계 칸도 함께 연다. 이미 차 있으면 한 칸만 연다.
        if (FamilyForge.of(sword).codexPair) {
            val next = Sword(sword.family, sword.level + 1)
            if (CodexOffer.canOffer(progress, next)) {
                progress = CodexOffer.offer(progress, game.difficulty, next)
            }
        }
        progress = LegendForge.onOffered(progress, sword)
        game = game.copy(sword = null)
        sound.purchase()
        persist()
        _ui.value = render()
    }

    fun upgradeSmithy() {
        if (busy || !Smithy.canUpgrade(game, progress)) return
        val (nextGame, nextProgress) = Smithy.upgrade(game, progress)
        game = nextGame
        progress = nextProgress
        sound.purchase()
        persist()
        _ui.value = render()
    }

    fun craftLegend() {
        if (busy || !LegendForge.canCraft(game, progress)) return
        val (nextGame, nextProgress) = LegendForge.craft(game, progress)
        game = nextGame
        progress = nextProgress
        sound.uniqueBorn()
        haptics.newRecord()
        persist()
        _ui.value = render()
    }

    fun recraftLegend() {
        if (busy || !LegendForge.canRecraft(game, progress)) return
        game = LegendForge.recraft(game)
        sound.uniqueBorn()
        persist()
        _ui.value = render()
    }
```

`runAttempt` 에서 보너스를 넘긴다 — `ForgeEngine.attempt(game, items, rng)` 를
`ForgeEngine.attempt(game, items, rng, ForgeBonuses.of(game, progress))` 로 바꾼다.

`render()` 에 새 필드를 채운다:

```kotlin
            bonusSources = ForgeBonuses.sourcesOf(game, progress),
            canOfferCodex = !busy && game.sword?.let { CodexOffer.canOffer(progress, it) } == true,
            smithyLevel = progress.smithyLevel,
            smithyPrice = Smithy.priceOf(game, progress.smithyLevel),
            canUpgradeSmithy = !busy && Smithy.canUpgrade(game, progress),
            legendMissing = LegendForge.missingFor(game),
            canCraftLegend = !busy && LegendForge.canCraft(game, progress),
            canRecraftLegend = !busy && LegendForge.canRecraft(game, progress),
            legendUnlocked = progress.legendUnlocked,
```

또한 `odds` 계산에 보너스를 반영한다:

```kotlin
            odds = ForgeOdds.of(
                game.difficulty,
                targetLevel,
                pendingItems,
                Tempering.failsFor(game, targetLevel),
                ForgeBonuses.of(game, progress).successRate + FamilyForge.of(game.sword).successBonus,
            ).percents(),
```

`ForgeOdds.of` 에 `bonus: Double = 0.0` 파라미터를 더하고 `RateTable.successRate` 에 넘긴다.

**옛 세이브 이관** — `loadAndRepair` 안에서 이미 +21 위면 해금을 켠다:

```kotlin
        // 이미 +21 위에 있다는 것은 벽을 넘었다는 뜻이다.
        if ((loaded.sword?.level ?: 0) > RateTable.MAX_FINITE_LEVEL && !progress.legendUnlocked) {
            progress = progress.copy(legendUnlocked = true)
            store.saveProgress(progress)
        }
```

- [ ] **Step 5: 테스트가 통과하는지 확인한다**

```bash
cd /c/workAndroid/SwordForge && ./gradlew.bat :core:test :app:testDebugUnitTest --console=plain
```

Expected: PASS

- [ ] **Step 6: 커밋한다**

```bash
cd /c/workAndroid/SwordForge && git add -A && git commit -m "성장 축과 전설검을 화면 상태까지"
```

---

### Task 10: 화면

**Files:**
- Modify: `app/src/main/java/com/geomgang/game/ui/ForgeScreen.kt`
- Modify: `app/src/main/java/com/geomgang/game/ui/CraftScreen.kt`
- Modify: `app/src/main/java/com/geomgang/game/ui/StorageScreen.kt`
- Modify: `app/src/main/java/com/geomgang/game/MainActivity.kt`

- [ ] **Step 1: 확률을 출처별로 쪼개 보여 준다**

`ForgeScreen` 의 담금질 게이지 아래에 넣는다:

```kotlin
            if (state.bonusSources.any { it.bonus.successRate > 0 }) {
                Spacer(Modifier.height(8.dp))
                BonusBreakdown(state.bonusSources)
            }
```

컴포저블을 더한다:

```kotlin
/**
 * 확률이 어디서 왔는지.
 *
 * 합계만 보여 주면 "왜 이 숫자인지" 알 수 없고, 그러면 도감이나 대장간을 올릴 이유가
 * 손에 잡히지 않는다.
 */
@Composable
private fun BonusBreakdown(sources: List<BonusSource>) {
    Column(Modifier.fillMaxWidth()) {
        sources.filter { it.bonus.successRate > 0 }.forEach { source ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${source.label}  ${source.detail}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                )
                Text(
                    text = "+%.1f%%p".format(source.bonus.successRate * 100),
                    fontSize = 11.sp,
                    color = Color(0xFF7FD48A),
                )
            }
        }
    }
}
```

- [ ] **Step 2: 계열 상한과 도감 바치기를 붙인다**

강화 버튼 자리에서 `state.canForge` 가 거짓이고 검이 +20 이면 버튼 대신 안내를 띄운다:

```kotlin
            val atFamilyCap = state.sword?.let { it.level >= LegendForge.MATERIAL_LEVEL && !it.isLegend() } == true
            if (atFamilyCap) {
                Text(
                    "여기가 계열의 끝",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE0A458),
                )
                Text(
                    "전설검은 조합소에서 벼린다",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                )
            } else {
                // 기존 강화 버튼
            }
```

도감 바치기 버튼을 특수강화 줄 위에 넣는다:

```kotlin
            if (state.canOfferCodex) {
                OutlinedButton(
                    onClick = onOfferCodex,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("📖 도감에 바치기 — 이 검은 사라진다") }
            }
```

`ForgeScreen` 에 `onOfferCodex: () -> Unit` 파라미터를 더하고 `MainActivity` 에서
`onOfferCodex = vm::offerToCodex` 를 넘긴다.

- [ ] **Step 3: 대장간을 강화 화면에 붙인다**

특수강화 줄 아래에 한 줄로 넣는다 (별도 화면을 만들지 않는다 — 버튼 하나면 된다):

```kotlin
            OutlinedButton(
                onClick = onUpgradeSmithy,
                enabled = state.canUpgradeSmithy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "🔨 대장간 Lv ${state.smithyLevel}/${Smithy.MAX_LEVEL}  ·  " +
                        compactGold(state.smithyPrice),
                    fontSize = 13.sp,
                )
            }
```

`onUpgradeSmithy: () -> Unit` 파라미터를 더하고 `MainActivity` 에서
`onUpgradeSmithy = vm::upgradeSmithy` 를 넘긴다.

- [ ] **Step 4: 조합소에 전설 칸을 넣는다**

`CraftScreen` 의 `FusionPanel` 위에 넣는다:

```kotlin
        LegendPanel(
            missing = state.legendMissing,
            canCraft = state.canCraftLegend,
            canRecraft = state.canRecraftLegend,
            unlocked = state.legendUnlocked,
            shards = state.shards,
            onCraft = onCraftLegend,
            onRecraft = onRecraftLegend,
        )
```

컴포저블을 더한다:

```kotlin
/**
 * 전설검 벼리기.
 *
 * 재료가 모자라도 **무엇이 필요한지 늘 보여 준다.** 목표가 보여야 모으고 싶어진다.
 */
@Composable
private fun LegendPanel(
    missing: List<WeaponFamily>,
    canCraft: Boolean,
    canRecraft: Boolean,
    unlocked: Boolean,
    shards: Int,
    onCraft: () -> Unit,
    onRecraft: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("전설검 벼리기", fontWeight = FontWeight.Bold, color = Color(0xFFFFD54A))
            Text(
                text = "계열은 +${LegendForge.MATERIAL_LEVEL} 에서 끝난다. 그 위는 여기서만 간다.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            )
            Spacer(Modifier.height(8.dp))
            LegendForge.MATERIALS.forEach { family ->
                val have = family !in missing
                Text(
                    text = "${if (have) "✓" else "✗"} ${family.displayName} +${LegendForge.MATERIAL_LEVEL}",
                    fontSize = 12.sp,
                    color = if (have) Color(0xFF7FD48A) else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    },
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onCraft, enabled = canCraft, modifier = Modifier.fillMaxWidth()) {
                Text("재료로 벼리기")
            }
            if (unlocked) {
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = onRecraft,
                    enabled = canRecraft,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("💎 ${LegendForge.RECRAFT_SHARDS} 로 다시 벼리기  (보유 $shards)")
                }
            }
        }
    }
}
```

`CraftScreen` 에 `onCraftLegend`·`onRecraftLegend` 파라미터를 더하고 `MainActivity` 에서
`vm::craftLegend`·`vm::recraftLegend` 를 넘긴다.

- [ ] **Step 5: 보관함에도 바치기를 넣는다**

`StorageScreen` 의 `StorageRow` 조작 줄에 「📖」 버튼을 더한다. 이미 찬 칸이면
`enabled = false` 로 둔다. `onOffer: (Int) -> Unit` 을 화면 파라미터로 받고
`MainActivity` 에서 `vm::offerFromStorage` 를 넘긴다. 뷰모델에 함수를 더한다:

```kotlin
    /** 보관함의 검을 도감에 바친다. */
    fun offerFromStorage(index: Int) {
        if (busy) return
        val sword = game.storage.getOrNull(index) ?: return
        if (!CodexOffer.canOffer(progress, sword)) return
        progress = LegendForge.onOffered(CodexOffer.offer(progress, game.difficulty, sword), sword)
        game = game.copy(storage = game.storage.filterIndexed { i, _ -> i != index })
        sound.purchase()
        persist()
        _ui.value = render()
    }
```

- [ ] **Step 6: 빌드한다**

```bash
cd /c/workAndroid/SwordForge && ./gradlew.bat :core:test :app:testDebugUnitTest :app:assembleDebug --console=plain
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 7: 커밋한다**

```bash
cd /c/workAndroid/SwordForge && git add -A && git commit -m "성장 축과 전설검 화면"
```

---

### Task 11: 실기기 확인과 마무리

- [ ] **Step 1: 전체 테스트와 빌드**

```bash
cd /c/workAndroid/SwordForge && ./gradlew.bat :core:test :app:testDebugUnitTest :app:assembleDebug --console=plain
```

- [ ] **Step 2: 폰에 설치한다**

```bash
"/c/Users/사용자/AppData/Local/Android/Sdk/platform-tools/adb.exe" -s R3CN50JXF9E install -r "C:/workAndroid/SwordForge/app/build/outputs/apk/debug/app-debug.apk"
```

앱을 실행하고 `adb logcat -d -s AndroidRuntime:E` 로 예외가 없는지 본다.
기기가 안 붙어 있으면 멈추고 사용자에게 연결을 요청한다.

- [ ] **Step 3: 눈으로 확인할 것**

- 강화 화면에 확률 내역이 출처별로 뜨는가
- 「도감에 바치기」를 누르면 검이 사라지고 도감 칸이 열리는가
- 대장간 버튼이 골드를 먹고 레벨이 오르는가
- +20 검에서 「여기가 계열의 끝」이 뜨고 강화 버튼이 없는가
- 조합소에 전설 칸이 뜨고 모자란 재료가 ✗ 로 보이는가
- 이미 +21 위인 세이브가 그대로 열리는가

- [ ] **Step 4: README 를 갱신한다**

`README.md` 에 절을 더한다 — 강화 보너스 네 출처와 상한, 도감 수집 방식 변경,
대장간 스킬, 계열별 강화 특성 표, 전설검 등급과 전설 해금.

- [ ] **Step 5: 커밋하고 푸시한다**

커밋 메시지를 스크래치패드 파일에 쓰고 `-F` 로 넣는다.

```bash
cd /c/workAndroid/SwordForge && git add -A && git push origin main
```
