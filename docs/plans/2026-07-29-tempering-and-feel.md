# 담금질과 손맛 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 무한 구간의 실패가 다음 성공률을 올리게 하고(담금질), 축복서와 부적을 배타로 묶어 강화 한 판마다 결정이 생기게 하며, 성공·실패·파괴가 손끝에 다르게 오게 한다.

**Architecture:** 확률 규칙은 `RateTable` 단일 출처를 유지한다 — 담금질은 `Tempering` 이 식을 갖고 `RateTable.successRate` 가 난이도 배수 뒤·축복서 앞에서 불러 쓴다. 누적 상태(`temperLevel`/`temperFails`)는 `GameState` 에 살고 **`ForgeEngine.attempt` 안에서** 갱신되므로 시뮬레이터와 뷰모델이 같은 규칙을 공짜로 받는다. 진동은 `SoundEngine` 과 같은 모양의 `HapticEngine` 이며 소리를 부르는 자리마다 나란히 붙는다.

**Tech Stack:** Kotlin 2.4 / AGP 9.2.1 / Gradle 9.4.1, `:core` 순수 Kotlin + JUnit4, `:app` Compose + Material3, kotlinx-serialization

## Global Constraints

- `:core` 는 순수 Kotlin이다. 안드로이드 의존성을 넣지 않는다. 진동은 `:app` 에만 둔다.
- UI 문구와 주석은 한국어다.
- `org.jetbrains.kotlin.android` 플러그인을 **추가하지 않는다.** AGP 9.2.1 에 내장되어 있다.
- `gradle.properties` 의 `-Dfile.encoding=MS949` 와 `org.gradle.java.home` 을 건드리지 않는다.
- 커밋 메시지·README·문서에 AI/Claude 표기를 넣지 않는다.
- 새 세이브 필드는 전부 기본값을 가진다. 옛 세이브가 손실 없이 열려야 한다.
- 커밋 메시지는 여러 줄이면 스크래치패드 파일에 쓰고 `git commit -F <file>` 로 넣는다. PowerShell 5.1 은 여러 줄 한글 문자열을 망가뜨린다.
- 테스트 실행은 프로젝트 루트(`C:\workAndroid\SwordForge`)에서 `.\gradlew.bat` 로 한다.
- `BalanceSimulationTest`·`ForgeTempoTest`·`BossTempoTest` 는 **손대지 않는다.** 깨지면 유한 구간(+20 이하)에 손이 닿았다는 뜻이므로 되돌린다.

---

### Task 1: Tempering 도메인

담금질 식 하나만 담는다. 상태도 화면도 모른다.

**Files:**
- Create: `core/src/main/kotlin/com/geomgang/core/Tempering.kt`
- Test: `core/src/test/kotlin/com/geomgang/core/TemperingTest.kt`

**Interfaces:**
- Consumes: `RateTable.MAX_FINITE_LEVEL` (=20), `GameState`
- Produces:
  - `Tempering.MIN_LEVEL: Int` (=21), `Tempering.STEP_RATIO: Double` (=0.5), `Tempering.MAX_RATE: Double` (=0.50)
  - `Tempering.applies(targetLevel: Int): Boolean`
  - `Tempering.rateFor(baseRate: Double, targetLevel: Int, fails: Int): Double`
  - `Tempering.failsFor(state: GameState, targetLevel: Int): Int`

- [ ] **Step 1: 실패하는 테스트를 작성한다**

`core/src/test/kotlin/com/geomgang/core/TemperingTest.kt`:

```kotlin
package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TemperingTest {

    @Test
    fun `유한 구간에는 붙지 않고 무한 구간부터 붙는다`() {
        assertFalse(Tempering.applies(20))
        assertTrue(Tempering.applies(21))
        assertTrue(Tempering.applies(45))
    }

    @Test
    fun `실패가 없으면 기준값 그대로다`() {
        assertEquals(0.005, Tempering.rateFor(0.005, 45, 0), 1e-9)
    }

    /** 붙지 않는 구간에서는 실패가 쌓여 있어도 무시한다. */
    @Test
    fun `유한 구간은 실패가 쌓여도 오르지 않는다`() {
        assertEquals(0.40, Tempering.rateFor(0.40, 10, 50), 1e-9)
    }

    @Test
    fun `실패 두 번이면 기준의 두 배가 된다`() {
        // base + base * 0.5 * 2 = base * 2
        assertEquals(0.010, Tempering.rateFor(0.005, 45, 2), 1e-9)
    }

    @Test
    fun `실패 수에 대해 단조 증가한다`() {
        var previous = Tempering.rateFor(0.005, 45, 0)
        for (fails in 1..200) {
            val now = Tempering.rateFor(0.005, 45, fails)
            assertTrue("fails=$fails prev=$previous now=$now", now >= previous)
            previous = now
        }
    }

    @Test
    fun `상한을 넘지 않는다`() {
        assertEquals(Tempering.MAX_RATE, Tempering.rateFor(0.005, 45, 100_000), 1e-9)
    }

    /** 담금질은 올려 주기만 한다. 이미 상한보다 높은 기준값을 끌어내리면 안 된다. */
    @Test
    fun `상한보다 높은 기준값은 낮추지 않는다`() {
        assertEquals(0.80, Tempering.rateFor(0.80, 45, 0), 1e-9)
        assertEquals(0.80, Tempering.rateFor(0.80, 45, 10), 1e-9)
    }

    @Test
    fun `음수 실패 수는 0으로 다룬다`() {
        assertEquals(0.005, Tempering.rateFor(0.005, 45, -3), 1e-9)
    }

    @Test
    fun `같은 단계에 쌓인 실패만 세어 준다`() {
        val state = GameState(Difficulty.ENDLESS, temperLevel = 45, temperFails = 7)
        assertEquals(7, Tempering.failsFor(state, 45))
        assertEquals(0, Tempering.failsFor(state, 44))
        assertEquals(0, Tempering.failsFor(GameState(Difficulty.ENDLESS), 45))
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

```bash
cd /c/workAndroid/SwordForge && ./gradlew.bat :core:test --tests "com.geomgang.core.TemperingTest" --console=plain
```

Expected: FAIL — `Unresolved reference 'Tempering'` 그리고 `GameState` 에 `temperLevel`/`temperFails` 가 없다는 컴파일 오류.

- [ ] **Step 3: GameState 에 필드 둘을 더한다**

`core/src/main/kotlin/com/geomgang/core/Model.kt` 의 `GameState` 에서 `lastSeenMillis` 선언 **바로 위**에 넣는다:

```kotlin
    /**
     * 담금질이 쌓인 목표 단계. 단계가 바뀌면 누적을 버린다.
     *
     * 0 은 "쌓인 것 없음"이다 — +1 을 노리는 시도의 targetLevel 이 1 이므로
     * 0 과 겹치지 않는다.
     */
    val temperLevel: Int = 0,
    /** [temperLevel] 에서 실패한 횟수. 성공하면 0 으로 돌아간다. */
    val temperFails: Int = 0,
```

같은 파일 `GameState` 의 `init` 블록에 검증 한 줄을 더한다:

```kotlin
        require(temperFails >= 0) { "temperFails must be >= 0, was $temperFails" }
```

- [ ] **Step 4: Tempering 을 구현한다**

`core/src/main/kotlin/com/geomgang/core/Tempering.kt`:

```kotlin
package com.geomgang.core

/**
 * 담금질 — 무한 구간에서 실패가 다음 성공률을 올린다.
 *
 * 예전에는 [RateTable.ENDLESS_FLOOR] 0.5% 에 붙은 채 실패가 **아무것도 남기지 않았다.**
 * 200번을 실패해도 201번째가 여전히 0.5% 라면 그건 긴장이 아니라 대기다.
 *
 * 실패가 쌓이면 확률이 오르고, 성공하면 0 으로 돌아간다. 그래서 게이지가 차오를수록
 * "이번엔 되나?" 가 진짜 질문이 된다.
 */
object Tempering {

    /**
     * 담금질이 붙는 첫 단계.
     *
     * 유한 구간(+20 이하)은 확률이 이미 충분해 필요 없고, 건드리면
     * [com.geomgang.core.sim.BalanceSimulationTest] 가 잡아 둔 곡선이 무너진다.
     */
    const val MIN_LEVEL: Int = RateTable.MAX_FINITE_LEVEL + 1

    /**
     * 실패 한 번이 더해 주는 몫. **기준 성공률에 대한 비율**이다.
     *
     * 고정 %p 로 두면 +21(1.7%)에서는 미미하고 +45(0.5%)에서는 과하거나 그 반대가 된다.
     * 비율로 두면 어느 단계에서나 "실패 두 번이면 확률이 두 배" 라는 같은 감각이 나온다.
     */
    const val STEP_RATIO: Double = 0.5

    /**
     * 담금질만으로 넘을 수 없는 성공률 상한.
     *
     * 여기가 없으면 무한 구간이 결국 공짜가 되고, 끝없이 이어질 이유가 사라진다.
     */
    const val MAX_RATE: Double = 0.50

    fun applies(targetLevel: Int): Boolean = targetLevel >= MIN_LEVEL

    /**
     * 담금질을 반영한 성공률.
     *
     * 붙지 않는 구간이거나 쌓인 실패가 없으면 [baseRate] 를 그대로 돌려준다.
     * 담금질은 **올려 주기만 한다** — 기준값이 이미 [MAX_RATE] 보다 높아도 낮추지 않는다.
     */
    fun rateFor(baseRate: Double, targetLevel: Int, fails: Int): Double {
        if (!applies(targetLevel) || fails <= 0) return baseRate
        val raised = baseRate + baseRate * STEP_RATIO * fails
        return maxOf(baseRate, minOf(raised, MAX_RATE))
    }

    /** 이 목표 단계에 유효한 누적 실패 수. 다른 단계에 쌓인 것은 세지 않는다. */
    fun failsFor(state: GameState, targetLevel: Int): Int =
        if (state.temperLevel == targetLevel) state.temperFails else 0
}
```

- [ ] **Step 5: 테스트가 통과하는지 확인한다**

```bash
cd /c/workAndroid/SwordForge && ./gradlew.bat :core:test --tests "com.geomgang.core.TemperingTest" --console=plain
```

Expected: PASS

- [ ] **Step 6: 커밋한다**

```bash
cd /c/workAndroid/SwordForge && git add core/src/main/kotlin/com/geomgang/core/Tempering.kt core/src/main/kotlin/com/geomgang/core/Model.kt core/src/test/kotlin/com/geomgang/core/TemperingTest.kt && git commit -m "담금질 식과 누적 상태"
```

---

### Task 2: ForgeMarks 도메인

최근 판 결과를 한 글자씩 남기는 목록.

**Files:**
- Create: `core/src/main/kotlin/com/geomgang/core/ForgeMarks.kt`
- Modify: `core/src/main/kotlin/com/geomgang/core/Model.kt` (GameState 에 `recentMarks`)
- Test: `core/src/test/kotlin/com/geomgang/core/ForgeMarksTest.kt`

**Interfaces:**
- Consumes: `ForgeResult`
- Produces:
  - `enum class ForgeMark { UP, STAY, DOWN, BREAK }`
  - `ForgeMarks.KEEP: Int` (=12)
  - `ForgeMarks.of(result: ForgeResult): ForgeMark`
  - `ForgeMarks.push(marks: List<ForgeMark>, mark: ForgeMark): List<ForgeMark>`
  - `GameState.recentMarks: List<ForgeMark>`

- [ ] **Step 1: 실패하는 테스트를 작성한다**

`core/src/test/kotlin/com/geomgang/core/ForgeMarksTest.kt`:

```kotlin
package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Test

class ForgeMarksTest {

    private val state = GameState(Difficulty.ENDLESS)

    @Test
    fun `네 결과가 각각 다른 글자로 간다`() {
        assertEquals(ForgeMark.UP, ForgeMarks.of(ForgeResult.Success(state, 12)))
        assertEquals(ForgeMark.STAY, ForgeMarks.of(ForgeResult.Stay(state, 11)))
        assertEquals(ForgeMark.DOWN, ForgeMarks.of(ForgeResult.Drop(state, 10)))
        assertEquals(
            ForgeMark.BREAK,
            ForgeMarks.of(ForgeResult.Destroyed(state, lostLevel = 11, preventable = false)),
        )
    }

    @Test
    fun `방금 것이 마지막에 온다`() {
        val marks = ForgeMarks.push(listOf(ForgeMark.UP), ForgeMark.BREAK)
        assertEquals(listOf(ForgeMark.UP, ForgeMark.BREAK), marks)
    }

    @Test
    fun `빈 목록에도 넣을 수 있다`() {
        assertEquals(listOf(ForgeMark.UP), ForgeMarks.push(emptyList(), ForgeMark.UP))
    }

    @Test
    fun `보관 수를 넘으면 앞에서 버린다`() {
        var marks = emptyList<ForgeMark>()
        repeat(ForgeMarks.KEEP) { marks = ForgeMarks.push(marks, ForgeMark.STAY) }
        marks = ForgeMarks.push(marks, ForgeMark.UP)

        assertEquals(ForgeMarks.KEEP, marks.size)
        assertEquals(ForgeMark.UP, marks.last())
        assertEquals(ForgeMark.STAY, marks.first())
    }

    /** 어떤 이유로 목록이 길어져 있어도 한 번 밀면 제자리로 돌아온다. */
    @Test
    fun `이미 넘쳐 있어도 보관 수로 잘린다`() {
        val tooMany = List(ForgeMarks.KEEP + 5) { ForgeMark.DOWN }
        val marks = ForgeMarks.push(tooMany, ForgeMark.UP)
        assertEquals(ForgeMarks.KEEP, marks.size)
        assertEquals(ForgeMark.UP, marks.last())
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

```bash
cd /c/workAndroid/SwordForge && ./gradlew.bat :core:test --tests "com.geomgang.core.ForgeMarksTest" --console=plain
```

Expected: FAIL — `Unresolved reference 'ForgeMarks'`

- [ ] **Step 3: ForgeMarks 를 만들고 GameState 에 목록을 더한다**

`core/src/main/kotlin/com/geomgang/core/ForgeMarks.kt`:

```kotlin
package com.geomgang.core

import kotlinx.serialization.Serializable

/** 강화 한 판의 결과를 한 글자로 줄인 것. */
@Serializable
enum class ForgeMark { UP, STAY, DOWN, BREAK }

/**
 * 최근 강화 결과의 자취.
 *
 * 결과는 한 번 뜨고 사라져서 **"세 판째 말아먹는 중"이라는 이야기가 남지 않았다.**
 * 연속 실패 횟수는 이미 통계로 세고 있는데 화면에 없었다.
 */
object ForgeMarks {

    /** 화면에 남기는 판 수. 한 줄에 들어가고 흐름이 읽히는 길이다. */
    const val KEEP: Int = 12

    fun of(result: ForgeResult): ForgeMark = when (result) {
        is ForgeResult.Success -> ForgeMark.UP
        is ForgeResult.Stay -> ForgeMark.STAY
        is ForgeResult.Drop -> ForgeMark.DOWN
        is ForgeResult.Destroyed -> ForgeMark.BREAK
    }

    /** 새 결과를 뒤에 붙이고 [KEEP] 을 넘으면 앞에서 버린다. */
    fun push(marks: List<ForgeMark>, mark: ForgeMark): List<ForgeMark> =
        (marks + mark).takeLast(KEEP)
}
```

`core/src/main/kotlin/com/geomgang/core/Model.kt` 의 `GameState` 에서 `temperFails` 선언 **바로 아래**에 넣는다:

```kotlin
    /**
     * 최근 강화 결과. 왼쪽이 오래된 것, 오른쪽이 방금 것이다.
     *
     * 세이브에 남기는 이유: 앱을 껐다 켰다고 자기 연패가 없던 일이 되면 안 된다.
     */
    val recentMarks: List<ForgeMark> = emptyList(),
```

- [ ] **Step 4: 테스트가 통과하는지 확인한다**

```bash
cd /c/workAndroid/SwordForge && ./gradlew.bat :core:test --tests "com.geomgang.core.ForgeMarksTest" --console=plain
```

Expected: PASS

- [ ] **Step 5: 커밋한다**

```bash
cd /c/workAndroid/SwordForge && git add core/src/main/kotlin/com/geomgang/core/ForgeMarks.kt core/src/main/kotlin/com/geomgang/core/Model.kt core/src/test/kotlin/com/geomgang/core/ForgeMarksTest.kt && git commit -m "최근 강화 결과 자취"
```

---

### Task 3: 축복서와 부적을 배타로

**Files:**
- Modify: `core/src/main/kotlin/com/geomgang/core/Model.kt` (`UsedItems`)
- Test: `core/src/test/kotlin/com/geomgang/core/UsedItemsTest.kt`

**Interfaces:**
- Produces: `UsedItems.toggleBlessing(): UsedItems`, `UsedItems.toggleLuckCharm(): UsedItems`

- [ ] **Step 1: 실패하는 테스트를 작성한다**

`core/src/test/kotlin/com/geomgang/core/UsedItemsTest.kt`:

```kotlin
package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 축복서와 부적은 함께 켜지지 않는다.
 *
 * 둘 다 쓸 수 있으면 "있으면 전부 켠다" 가 유일한 최선이 되어 선택이 사라진다.
 * 하나만 고르게 해야 **확률을 올릴까, 검을 지킬까** 가 매 판 갈림길이 된다.
 */
class UsedItemsTest {

    @Test
    fun `축복서를 켜면 부적이 꺼진다`() {
        val items = UsedItems(luckCharm = true).toggleBlessing()
        assertTrue(items.blessing)
        assertFalse(items.luckCharm)
    }

    @Test
    fun `부적을 켜면 축복서가 꺼진다`() {
        val items = UsedItems(blessing = true).toggleLuckCharm()
        assertTrue(items.luckCharm)
        assertFalse(items.blessing)
    }

    @Test
    fun `켠 것을 다시 누르면 둘 다 꺼진다`() {
        assertEquals(UsedItems.NONE, UsedItems(blessing = true).toggleBlessing())
        assertEquals(UsedItems.NONE, UsedItems(luckCharm = true).toggleLuckCharm())
    }

    @Test
    fun `아무것도 안 켠 상태에서 하나만 켜진다`() {
        assertEquals(UsedItems(blessing = true), UsedItems.NONE.toggleBlessing())
        assertEquals(UsedItems(luckCharm = true), UsedItems.NONE.toggleLuckCharm())
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

```bash
cd /c/workAndroid/SwordForge && ./gradlew.bat :core:test --tests "com.geomgang.core.UsedItemsTest" --console=plain
```

Expected: FAIL — `Unresolved reference 'toggleBlessing'`

- [ ] **Step 3: UsedItems 에 배타 토글을 넣는다**

`core/src/main/kotlin/com/geomgang/core/Model.kt` 의 `UsedItems` 를 통째로 바꾼다:

```kotlin
/**
 * 이번 강화 시도에 함께 사용할 아이템.
 *
 * 축복서와 부적은 **함께 켜지지 않는다.** 둘 다 쓸 수 있으면 "있으면 전부 켠다" 가
 * 유일한 최선이 되어 고를 것이 사라진다. 하나만 고르게 해야 갈림길이 생긴다.
 *
 * - 축복서 — 이번 판 확률만 올린다. 담금질이 얕을 때 지르는 수다
 * - 부적 — 실패해도 부서지지 않지만 **담금질은 오른다.** 안전하게 쌓는 수다
 *
 * 배타를 화면이 아니라 여기서 지키는 이유: 토글이 둘로 나뉘어 있으면 반드시 어긋난다.
 */
data class UsedItems(
    val blessing: Boolean = false,
    val luckCharm: Boolean = false,
) {
    /** 축복서를 켜고 끈다. 켜면 부적이 내려간다. */
    fun toggleBlessing(): UsedItems =
        if (blessing) NONE else UsedItems(blessing = true)

    /** 부적을 켜고 끈다. 켜면 축복서가 내려간다. */
    fun toggleLuckCharm(): UsedItems =
        if (luckCharm) NONE else UsedItems(luckCharm = true)

    companion object {
        val NONE: UsedItems = UsedItems()
    }
}
```

- [ ] **Step 4: 테스트가 통과하는지 확인한다**

```bash
cd /c/workAndroid/SwordForge && ./gradlew.bat :core:test --tests "com.geomgang.core.UsedItemsTest" --console=plain
```

Expected: PASS

- [ ] **Step 5: 커밋한다**

```bash
cd /c/workAndroid/SwordForge && git add core/src/main/kotlin/com/geomgang/core/Model.kt core/src/test/kotlin/com/geomgang/core/UsedItemsTest.kt && git commit -m "축복서와 부적을 배타로"
```

---

### Task 4: 확률표와 확률 표시에 담금질 연결

`RateTable.successRate` 가 계속 단일 출처여야 한다. 담금질은 **난이도 배수 뒤, 축복서 앞**이다.

**Files:**
- Modify: `core/src/main/kotlin/com/geomgang/core/RateTable.kt:64-73`
- Modify: `core/src/main/kotlin/com/geomgang/core/ForgeOdds.kt:51-88`
- Test: `core/src/test/kotlin/com/geomgang/core/TemperedRateTest.kt`

**Interfaces:**
- Consumes: `Tempering.rateFor` (Task 1)
- Produces:
  - `RateTable.successRate(difficulty, targetLevel, blessing = false, temperFails = 0): Double`
  - `ForgeOdds.of(difficulty, targetLevel, items = UsedItems.NONE, temperFails = 0): ForgeOdds`

- [ ] **Step 1: 실패하는 테스트를 작성한다**

`core/src/test/kotlin/com/geomgang/core/TemperedRateTest.kt`:

```kotlin
package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TemperedRateTest {

    @Test
    fun `담금질 인자를 안 주면 예전과 같은 값이다`() {
        assertEquals(
            RateTable.baseSuccessRate(45) * Difficulty.ENDLESS.multiplier,
            RateTable.successRate(Difficulty.ENDLESS, 45),
            1e-9,
        )
    }

    @Test
    fun `실패가 쌓이면 성공률이 오른다`() {
        val cold = RateTable.successRate(Difficulty.ENDLESS, 45, temperFails = 0)
        val warm = RateTable.successRate(Difficulty.ENDLESS, 45, temperFails = 20)
        assertTrue("cold=$cold warm=$warm", warm > cold)
    }

    /** 축복서는 "이번 판만 얹는 것" 이므로 누적분 위에 얹혀야 말이 된다. */
    @Test
    fun `축복서는 담금질 뒤에 더해진다`() {
        val tempered = RateTable.successRate(Difficulty.ENDLESS, 45, temperFails = 10)
        val withScroll =
            RateTable.successRate(Difficulty.ENDLESS, 45, blessing = true, temperFails = 10)
        assertEquals(tempered + RateTable.BLESSING_BONUS, withScroll, 1e-9)
    }

    @Test
    fun `최종 상한을 넘지 않는다`() {
        val rate = RateTable.successRate(
            Difficulty.ENDLESS,
            45,
            blessing = true,
            temperFails = 100_000,
        )
        assertTrue("rate=$rate", rate <= RateTable.MAX_SUCCESS_RATE)
    }

    @Test
    fun `유한 구간은 담금질을 받지 않는다`() {
        assertEquals(
            RateTable.successRate(Difficulty.NORMAL, 10),
            RateTable.successRate(Difficulty.NORMAL, 10, temperFails = 50),
            1e-9,
        )
    }

    @Test
    fun `확률 표시도 담금질을 반영한다`() {
        val cold = ForgeOdds.of(Difficulty.ENDLESS, 45, temperFails = 0)
        val warm = ForgeOdds.of(Difficulty.ENDLESS, 45, temperFails = 30)
        assertTrue("cold=${cold.success} warm=${warm.success}", warm.success > cold.success)
        // 성공이 오르면 파괴가 그만큼 줄어야 한다. 넷의 합은 언제나 1이다.
        assertTrue(warm.destroy < cold.destroy)
        assertEquals(1.0, warm.success + warm.stay + warm.drop + warm.destroy, 1e-9)
    }

    @Test
    fun `부적을 켜면 실패분이 전부 유지로 간다`() {
        val odds = ForgeOdds.of(
            Difficulty.ENDLESS,
            45,
            UsedItems(luckCharm = true),
            temperFails = 30,
        )
        assertEquals(0.0, odds.destroy, 1e-9)
        assertEquals(0.0, odds.drop, 1e-9)
        assertEquals(1.0 - odds.success, odds.stay, 1e-9)
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

```bash
cd /c/workAndroid/SwordForge && ./gradlew.bat :core:test --tests "com.geomgang.core.TemperedRateTest" --console=plain
```

Expected: FAIL — `successRate` 에 `temperFails` 인자가 없다는 컴파일 오류

- [ ] **Step 3: RateTable 과 ForgeOdds 에 인자를 더한다**

`core/src/main/kotlin/com/geomgang/core/RateTable.kt` 의 `successRate` 를 바꾼다:

```kotlin
    /**
     * 난이도 배수·담금질·축복서를 반영한 최종 성공률.
     *
     * 순서가 뜻을 만든다. 담금질은 누적된 몫이라 난이도 배수 **뒤**에 붙고,
     * 축복서는 "이번 판만 얹는 것" 이라 누적분 **위**에 얹힌다.
     *
     * @param temperFails 이 목표 단계에 쌓인 실패 수. 0 이면 예전과 같은 값이다.
     */
    fun successRate(
        difficulty: Difficulty,
        targetLevel: Int,
        blessing: Boolean = false,
        temperFails: Int = 0,
    ): Double {
        val scaled = baseSuccessRate(targetLevel) * difficulty.multiplier
        val tempered = Tempering.rateFor(scaled, targetLevel, temperFails)
        val boosted = if (blessing) tempered + BLESSING_BONUS else tempered
        return minOf(boosted, MAX_SUCCESS_RATE)
    }
```

`core/src/main/kotlin/com/geomgang/core/ForgeOdds.kt` 의 `of` 서명과 첫 줄을 바꾼다:

```kotlin
        /**
         * @param targetLevel 이번 시도로 **도달하려는** 단계 (현재 단계 + 1)
         * @param items 지금 켜 둔 아이템. 축복서는 성공률을 올리고,
         *   행운부적은 실패의 결과 자체를 없앤다.
         * @param temperFails 이 단계에 쌓인 담금질. 성공률을 올린다.
         */
        fun of(
            difficulty: Difficulty,
            targetLevel: Int,
            items: UsedItems = UsedItems.NONE,
            temperFails: Int = 0,
        ): ForgeOdds {
            val success =
                RateTable.successRate(difficulty, targetLevel, items.blessing, temperFails)
            val fail = 1.0 - success
```

나머지 본문은 그대로 둔다.

- [ ] **Step 4: 테스트가 통과하는지 확인한다**

```bash
cd /c/workAndroid/SwordForge && ./gradlew.bat :core:test --console=plain
```

Expected: PASS. `RateTableTest`·`ForgeOddsTest` 등 기존 테스트도 전부 통과해야 한다 — 새 인자에 기본값이 있으므로 옛 호출부는 그대로 돈다.

- [ ] **Step 5: 커밋한다**

```bash
cd /c/workAndroid/SwordForge && git add core/src/main/kotlin/com/geomgang/core/RateTable.kt core/src/main/kotlin/com/geomgang/core/ForgeOdds.kt core/src/test/kotlin/com/geomgang/core/TemperedRateTest.kt && git commit -m "확률표와 확률 표시에 담금질 연결"
```

---

### Task 5: 판정 엔진이 담금질을 쌓고 지운다

누적 갱신을 `ForgeEngine.attempt` 안에 둔다. 그래야 시뮬레이터와 뷰모델이 같은 규칙을 공짜로 받는다.

**Files:**
- Modify: `core/src/main/kotlin/com/geomgang/core/ForgeEngine.kt:81-155`
- Test: `core/src/test/kotlin/com/geomgang/core/ForgeEngineTemperTest.kt`

**Interfaces:**
- Consumes: `Tempering.failsFor` (Task 1), `RateTable.successRate(..., temperFails)` (Task 4)
- Produces: `ForgeResult.state` 의 `temperLevel`/`temperFails` 가 갱신되어 나온다

- [ ] **Step 1: 실패하는 테스트를 작성한다**

`core/src/test/kotlin/com/geomgang/core/ForgeEngineTemperTest.kt`:

```kotlin
package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** 무한 구간에서 담금질이 쌓이고 지워지는지. */
class ForgeEngineTemperTest {

    private fun endless(level: Int, temperLevel: Int = 0, temperFails: Int = 0) = GameState(
        difficulty = Difficulty.ENDLESS,
        gold = 1_000_000_000_000_000L,
        sword = Sword(WeaponFamily.STRAIGHT, level),
        forgeStones = 999,
        temperLevel = temperLevel,
        temperFails = temperFails,
    )

    /**
     * 항상 실패하는 난수.
     *
     * `nextDouble()` 은 `nextBits` 를 두 번 불러 만든다. 전부 1 로 채우면 1 에 가장
     * 가까운 값이 나와 어떤 성공률도 통과하지 못한다.
     */
    private fun alwaysFail(): Random = object : Random() {
        override fun nextBits(bitCount: Int): Int =
            if (bitCount >= 32) -1 else (1 shl bitCount) - 1
    }

    @Test
    fun `무한 구간에서 실패하면 담금질이 쌓인다`() {
        val result = ForgeEngine.attempt(endless(44), UsedItems.NONE, alwaysFail())
        assertEquals(45, result.state.temperLevel)
        assertEquals(1, result.state.temperFails)
    }

    @Test
    fun `쌓인 위에 또 실패하면 하나 더 쌓인다`() {
        val state = endless(44, temperLevel = 45, temperFails = 7)
        val result = ForgeEngine.attempt(state, UsedItems.NONE, alwaysFail())
        assertEquals(8, result.state.temperFails)
    }

    /** 부적 실패도 담금질을 올린다. 이게 없으면 부적은 그냥 손해 없는 굴림이 된다. */
    @Test
    fun `부적을 쓴 실패도 담금질을 올린다`() {
        val state = endless(44).copy(inventory = Inventory(luckCharms = 1))
        val result = ForgeEngine.attempt(state, UsedItems(luckCharm = true), alwaysFail())
        assertTrue("결과=$result", result is ForgeResult.Stay)
        assertEquals(1, result.state.temperFails)
    }

    @Test
    fun `성공하면 담금질이 지워진다`() {
        // 담금질을 크게 쌓아 두면 상한 50%까지 오른다. 성공이 나올 때까지 같은 상태로 굴린다.
        val state = endless(44, temperLevel = 45, temperFails = 10_000)
        val rng = Random(7)
        var success: ForgeResult.Success? = null
        for (i in 0 until 200) {
            val r = ForgeEngine.attempt(state, UsedItems.NONE, rng)
            if (r is ForgeResult.Success) {
                success = r
                break
            }
        }
        val done = requireNotNull(success) { "상한 50%인데 200번 안에 성공이 없다" }
        assertEquals(0, done.state.temperLevel)
        assertEquals(0, done.state.temperFails)
    }

    @Test
    fun `유한 구간에서는 쌓지 않는다`() {
        val state = GameState(
            difficulty = Difficulty.NORMAL,
            gold = 1_000_000L,
            sword = Sword(WeaponFamily.STRAIGHT, 10),
        )
        val result = ForgeEngine.attempt(state, UsedItems.NONE, alwaysFail())
        assertEquals(0, result.state.temperLevel)
        assertEquals(0, result.state.temperFails)
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

```bash
cd /c/workAndroid/SwordForge && ./gradlew.bat :core:test --tests "com.geomgang.core.ForgeEngineTemperTest" --console=plain
```

Expected: FAIL — `temperFails` 가 0 인 채로 나온다

- [ ] **Step 3: attempt 가 담금질을 읽고 쓰게 한다**

`core/src/main/kotlin/com/geomgang/core/ForgeEngine.kt` 의 `attempt` 안에서 `paid` 를 만든 **직후**에 넣는다:

```kotlin
        // 담금질은 이 목표 단계에 쌓인 것만 센다. 단계가 바뀌었으면 0 부터다.
        val fails = Tempering.failsFor(state, targetLevel)

        /**
         * 실패했을 때의 바탕 상태. 담금질이 한 칸 쌓여 있다.
         *
         * 부적을 써서 검이 무사한 실패도 여기를 지난다 - 그러지 않으면 부적이
         * "손해 없는 굴림" 이 되어 고를 이유가 사라진다.
         */
        val failed = paid.copy(
            temperLevel = if (Tempering.applies(targetLevel)) targetLevel else 0,
            temperFails = if (Tempering.applies(targetLevel)) fails + 1 else 0,
        )
```

같은 함수의 `successRate` 계산에 `fails` 를 넘긴다:

```kotlin
        val successRate = (
            RateTable.successRate(state.difficulty, targetLevel, items.blessing, fails) +
                extraSuccessRate + UniqueSwords.forgeBonusOf(sword)
            ).coerceAtMost(RateTable.MAX_SUCCESS_RATE)
```

성공 분기에서 누적을 지운다:

```kotlin
        if (rng.nextDouble() < successRate) {
            return ForgeResult.Success(
                state = paid.copy(
                    sword = sword.copy(level = targetLevel),
                    bestLevel = maxOf(paid.bestLevel, targetLevel),
                    // 성공하면 담금질은 처음으로 돌아간다.
                    temperLevel = 0,
                    temperFails = 0,
                ),
                newLevel = targetLevel,
            )
        }
```

이후 **모든 실패 분기**에서 `paid` 를 `failed` 로 바꾼다. 바꿀 자리가 다섯이다:

```kotlin
        if (items.luckCharm) {
            return ForgeResult.Stay(failed, sword.level)
        }

        return when (RateTable.failureBand(targetLevel)) {
            FailureBand.STAY -> ForgeResult.Stay(failed, sword.level)

            FailureBand.DROP -> drop(failed, sword)

            FailureBand.DESTROY_OR_DROP ->
                if (rng.nextDouble() < RateTable.destroyChance(targetLevel)) {
                    if (UniqueSwords.canRevive(sword)) {
                        val revived = sword.copy(
                            level = (sword.level - UniqueSwords.REVIVE_LEVEL_LOSS)
                                .coerceAtLeast(0),
                            uniqueId = null,
                        )
                        ForgeResult.Drop(
                            state = failed.copy(sword = revived),
                            newLevel = revived.level,
                        )
                    } else {
                        ForgeResult.Destroyed(
                            state = failed.copy(
                                sword = null,
                                pendingDestroy = PendingDestroy(sword.family, sword.level),
                            ),
                            lostLevel = sword.level,
                            preventable = failed.inventory.preventTickets > 0,
                        )
                    }
                } else {
                    drop(failed, sword)
                }
        }
```

- [ ] **Step 4: 테스트가 통과하는지 확인한다**

```bash
cd /c/workAndroid/SwordForge && ./gradlew.bat :core:test --console=plain
```

Expected: PASS. 기존 `ForgeEngineTest` 도 통과해야 한다 — 유한 구간에서는 두 필드가 0 그대로다.

- [ ] **Step 5: 커밋한다**

```bash
cd /c/workAndroid/SwordForge && git add core/src/main/kotlin/com/geomgang/core/ForgeEngine.kt core/src/test/kotlin/com/geomgang/core/ForgeEngineTemperTest.kt && git commit -m "판정 엔진이 담금질을 쌓고 지운다"
```

---

### Task 6: 담금질 가드레일 — TemperTempoTest

**이 작업의 핵심 방어선.** 밸런스를 느낌으로 두지 않는다.

**Files:**
- Create: `core/src/test/kotlin/com/geomgang/core/TemperTempoTest.kt`

**Interfaces:**
- Consumes: `ForgeEngine.attempt` (Task 5), `Tempering` (Task 1)

- [ ] **Step 1: 시뮬레이션 테스트를 작성한다**

`core/src/test/kotlin/com/geomgang/core/TemperTempoTest.kt`:

```kotlin
package com.geomgang.core

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.ln
import kotlin.random.Random

/**
 * 담금질이 무한 구간의 속도를 얼마나 바꿨는지 숫자로 못 박는다.
 *
 * 실기기 세이브가 +44 였다. 담금질 전에는 +45 한 단계에 기댓값 200번이었고
 * 실패가 아무것도 남기지 않았다. 이 테스트가 그 구간을 지킨다.
 */
class TemperTempoTest {

    private companion object {
        const val START_LEVEL = 44
        const val RUNS = 2_000

        /** 한 단계를 올리는 데 걸리는 시도 수의 목표 구간(중앙값). */
        const val MIN_MEDIAN = 15
        const val MAX_MEDIAN = 45
    }

    /** 검이 부서지면 방지권으로 되살린다. 재도전 문턱이 아니라 속도를 재는 시험이다. */
    private fun attemptsToAdvance(rng: Random): Int {
        var state = GameState(
            difficulty = Difficulty.ENDLESS,
            gold = 1_000_000_000_000_000L,
            sword = Sword(WeaponFamily.STRAIGHT, START_LEVEL),
            inventory = Inventory(preventTickets = 9_999),
            forgeStones = 9_999,
        )
        var attempts = 0
        while (attempts < 5_000) {
            // 골드와 방지권은 이 시험의 관심사가 아니다. 매번 채운다.
            state = state.copy(
                gold = 1_000_000_000_000_000L,
                inventory = state.inventory.copy(preventTickets = 9_999),
            )
            val result = ForgeEngine.attempt(state, UsedItems.NONE, rng)
            attempts++
            state = result.state
            if (result is ForgeResult.Success) return attempts
            if (result is ForgeResult.Destroyed) {
                state = ForgeEngine.applyPrevent(state)
            }
        }
        return attempts
    }

    @Test
    fun `담금질이 붙으면 한 단계가 목표 구간 안에서 끝난다`() {
        val rng = Random(20_260_729L)
        val counts = List(RUNS) { attemptsToAdvance(rng) }.sorted()
        val median = counts[counts.size / 2]

        assertTrue(
            "중앙값=$median (목표 $MIN_MEDIAN..$MAX_MEDIAN), p90=${counts[(counts.size * 90) / 100]}",
            median in MIN_MEDIAN..MAX_MEDIAN,
        )
    }

    /**
     * 담금질이 없었다면 얼마나 걸렸는지.
     *
     * 확률이 고정이면 시도 수는 기하분포이고 중앙값은 ln(0.5)/ln(1-p) 다.
     * 이 값이 크다는 것이 담금질을 넣은 이유다 - 숫자로 남겨 둔다.
     */
    @Test
    fun `담금질이 없으면 백 번을 넘긴다`() {
        val flat = RateTable.successRate(Difficulty.ENDLESS, START_LEVEL + 1, temperFails = 0)
        val median = ln(0.5) / ln(1.0 - flat)
        assertTrue("담금질 없는 중앙값=%.1f (p=%.4f)".format(median, flat), median > 100.0)
    }

    /** 상한이 있어야 무한 구간이 끝없이 이어질 이유가 남는다. */
    @Test
    fun `담금질만으로는 상한을 넘지 못한다`() {
        val rate = RateTable.successRate(Difficulty.ENDLESS, 45, temperFails = 1_000_000)
        assertTrue("rate=$rate", rate <= Tempering.MAX_RATE + 1e-9)
    }
}
```

- [ ] **Step 2: 테스트를 돌려 실제 숫자를 본다**

```bash
cd /c/workAndroid/SwordForge && ./gradlew.bat :core:test --tests "com.geomgang.core.TemperTempoTest" --console=plain
```

Expected: PASS. 실패하면 **테스트를 고치지 말고** `Tempering.STEP_RATIO` 를 조정한다 — 중앙값이 목표보다 크면 올리고, 작으면 내린다. 조정 후 `TemperingTest` 의 `실패 두 번이면 기준의 두 배가 된다` 도 함께 맞춘다.

- [ ] **Step 3: 전체 테스트로 회귀를 확인한다**

```bash
cd /c/workAndroid/SwordForge && ./gradlew.bat :core:test :app:testDebugUnitTest --console=plain
```

Expected: PASS. 특히 `BalanceSimulationTest`·`ForgeTempoTest`·`BossTempoTest` 가 통과해야 한다. 깨지면 유한 구간에 손이 닿았다는 뜻이므로 Task 5 를 되돌아본다.

- [ ] **Step 4: 커밋한다**

```bash
cd /c/workAndroid/SwordForge && git add core/src/test/kotlin/com/geomgang/core/TemperTempoTest.kt && git commit -m "담금질 속도 가드레일"
```

---

### Task 7: 뷰모델 배선

**Files:**
- Modify: `app/src/main/java/com/geomgang/game/ForgeViewModel.kt` (`runAttempt`, `toggleBlessing`, `toggleLuckCharm`, `render`)
- Modify: `app/src/main/java/com/geomgang/game/ForgeUiState.kt`
- Test: `app/src/test/java/com/geomgang/game/ForgeViewModelTemperTest.kt`

**Interfaces:**
- Consumes: `ForgeMarks.push`/`of` (Task 2), `UsedItems.toggleBlessing`/`toggleLuckCharm` (Task 3), `ForgeOdds.of(..., temperFails)` (Task 4)
- Produces: `ForgeUiState.temper: TemperUi?`, `ForgeUiState.recentMarks: List<ForgeMark>`, `ForgeUiState.isRecord: Boolean`

- [ ] **Step 1: 실패하는 테스트를 작성한다**

`app/src/test/java/com/geomgang/game/ForgeViewModelTemperTest.kt`:

```kotlin
package com.geomgang.game

import com.geomgang.core.Difficulty
import com.geomgang.core.GameState
import com.geomgang.core.Inventory
import com.geomgang.core.SaveStore
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.random.Random

/** 담금질·자취·신기록이 화면 상태까지 오는지. */
@OptIn(ExperimentalCoroutinesApi::class)
class ForgeViewModelTemperTest {

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
        inventory: Inventory = Inventory(),
        rng: Random = alwaysSucceed(),
    ): ForgeViewModel {
        val store = SaveStore(tmp.root)
        store.saveGame(
            GameState(
                difficulty = Difficulty.ENDLESS,
                gold = 1_000_000_000_000_000L,
                sword = Sword(WeaponFamily.STRAIGHT, level),
                inventory = inventory,
                forgeStones = 9_999,
                bestLevel = level,
            ),
        )
        return ForgeViewModel(store, Difficulty.ENDLESS, rng)
    }

    @Test
    fun `담금질이 붙지 않는 구간에서는 표시가 없다`() = runTest(dispatcher) {
        // 손에 든 검이 +10 이면 목표가 +11 이라 담금질이 붙지 않는다
        assertNull(vm(10).ui.value.temper)
    }

    @Test
    fun `무한 구간에서는 담금질 표시가 나온다`() = runTest(dispatcher) {
        val temper = requireNotNull(vm(30).ui.value.temper)
        assertEquals(0, temper.fails)
        assertTrue("ratio=${temper.ratio}", temper.ratio in 0f..1f)
    }

    @Test
    fun `강화하면 자취가 한 칸 늘어난다`() = runTest(dispatcher) {
        val vm = vm(3)
        val before = vm.ui.value.recentMarks.size
        vm.forge()
        assertEquals(before + 1, vm.ui.value.recentMarks.size)
    }

    @Test
    fun `축복서를 켜면 부적이 꺼진다`() = runTest(dispatcher) {
        val vm = vm(3, Inventory(blessingScrolls = 1, luckCharms = 1))
        vm.toggleLuckCharm()
        assertTrue(vm.ui.value.useLuckCharm)
        vm.toggleBlessing()
        assertTrue(vm.ui.value.useBlessing)
        assertFalse(vm.ui.value.useLuckCharm)
    }

    @Test
    fun `부적을 켜면 축복서가 꺼진다`() = runTest(dispatcher) {
        val vm = vm(3, Inventory(blessingScrolls = 1, luckCharms = 1))
        vm.toggleBlessing()
        vm.toggleLuckCharm()
        assertTrue(vm.ui.value.useLuckCharm)
        assertFalse(vm.ui.value.useBlessing)
    }

    @Test
    fun `낮은 단계 성공은 신기록으로 치지 않는다`() = runTest(dispatcher) {
        val vm = vm(0)
        vm.forge()
        assertFalse(vm.ui.value.isRecord)
    }

    @Test
    fun `문턱 위에서 최고를 넘기면 신기록이다`() = runTest(dispatcher) {
        // +12 에서 성공하면 +13 이고, 이는 이 세이브의 최고(+12)를 넘는다
        val vm = vm(12)
        vm.forge()
        assertTrue(vm.ui.value.isRecord)
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

```bash
cd /c/workAndroid/SwordForge && ./gradlew.bat :app:testDebugUnitTest --tests "com.geomgang.game.ForgeViewModelTemperTest" --console=plain
```

Expected: FAIL — `temper`·`recentMarks`·`isRecord` 가 없다는 컴파일 오류

- [ ] **Step 3: UI 상태에 세 가지를 더한다**

`app/src/main/java/com/geomgang/game/ForgeUiState.kt` 의 파일 맨 위(다른 최상위 데이터 클래스 옆)에 넣는다:

```kotlin
/**
 * 담금질 표시. 붙지 않는 구간이면 null 이다.
 *
 * @property fails 이 단계에 쌓인 실패 수
 * @property basePercent 담금질 없는 성공률(%)
 * @property currentPercent 담금질을 반영한 지금 성공률(%)
 * @property ratio 게이지 채움 정도. 0..1
 */
data class TemperUi(
    val fails: Int,
    val basePercent: Double,
    val currentPercent: Double,
    val ratio: Float,
)
```

`ForgeUiState` 안에 필드 셋을 더한다:

```kotlin
    /** 담금질. 무한 구간에서만 채워진다. */
    val temper: TemperUi? = null,
    /** 최근 강화 결과. 왼쪽이 오래된 것이다. */
    val recentMarks: List<ForgeMark> = emptyList(),
    /** 이번 성공이 최고 기록을 갈아치웠는지. */
    val isRecord: Boolean = false,
```

`ForgeUiState.kt` 상단에 `import com.geomgang.core.ForgeMark` 를 더한다.

- [ ] **Step 4: 뷰모델을 배선한다**

`ForgeViewModel` 의 필드 선언부(`private var lastResult` 근처)에 더한다:

```kotlin
    /** 이번 성공이 최고 기록을 넘었는지. 연출이 끝나면 내려간다. */
    private var lastWasRecord: Boolean = false
```

같은 클래스에 상수를 더한다 (`companion object` 가 없으면 파일 맨 아래에 만든다):

```kotlin
    private companion object {
        /**
         * 이 단계 위부터 신기록을 축하한다.
         *
         * 새 세이브는 처음 열 판이 전부 신기록이라 문턱이 없으면 연출이 금방 값을 잃는다.
         * [com.geomgang.core.StarForce.MIN_LEVEL] 과 같은 값이라 규칙이 한 벌로 읽힌다.
         */
        const val MIN_RECORD_LEVEL = 10
    }
```

`runAttempt` 를 고친다. `val result = ForgeEngine.attempt(game, items, rng)` **바로 위**에 기록 전 최고 단계를 잡아 둔다:

```kotlin
        val bestBefore = game.bestLevel
```

`game = GoldShop.rebase(result.state)` **바로 아래**에 두 줄을 더한다:

```kotlin
        // 자취를 한 칸 민다. 결과가 뜨고 사라지면 연패가 이야기로 남지 않는다.
        game = game.copy(recentMarks = ForgeMarks.push(game.recentMarks, ForgeMarks.of(result)))
        lastWasRecord = result is ForgeResult.Success &&
            result.newLevel > bestBefore &&
            result.newLevel >= MIN_RECORD_LEVEL
```

`toggleBlessing` 과 `toggleLuckCharm` 을 배타 토글로 바꾼다:

```kotlin
    /** 다음 강화에 축복서를 쓸지 켜고 끈다. 켜면 부적이 내려간다. */
    fun toggleBlessing() {
        if (busy) return
        if (!pendingItems.blessing && game.inventory.blessingScrolls <= 0) return
        pendingItems = pendingItems.toggleBlessing()
        _ui.value = render()
    }

    /** 다음 강화에 부적을 쓸지 켜고 끈다. 켜면 축복서가 내려간다. */
    fun toggleLuckCharm() {
        if (busy) return
        if (!pendingItems.luckCharm && game.inventory.luckCharms <= 0) return
        pendingItems = pendingItems.toggleLuckCharm()
        _ui.value = render()
    }
```

`render()` 에서 `odds = ForgeOdds.of(...)` 줄을 담금질을 넘기도록 바꾸고 세 필드를 채운다. `render()` 안에서 `targetLevel` 이 계산되는 자리 아래에 넣는다:

```kotlin
            odds = ForgeOdds.of(
                game.difficulty,
                targetLevel,
                pendingItems,
                Tempering.failsFor(game, targetLevel),
            ).percents(),
            temper = temperUiFor(targetLevel),
            recentMarks = game.recentMarks,
            isRecord = lastWasRecord,
```

같은 클래스에 도우미를 더한다:

```kotlin
    /** 담금질 표시. 붙지 않는 구간이면 null 이다. */
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
```

`onAnimationFinished` 안에서 `lastResult = null` 을 하는 자리에 `lastWasRecord = false` 를 함께 넣는다.

`ForgeViewModel.kt` 상단에 필요한 import 를 더한다:

```kotlin
import com.geomgang.core.ForgeMarks
import com.geomgang.core.RateTable
import com.geomgang.core.Tempering
```

- [ ] **Step 5: 테스트가 통과하는지 확인한다**

```bash
cd /c/workAndroid/SwordForge && ./gradlew.bat :core:test :app:testDebugUnitTest --console=plain
```

Expected: PASS

- [ ] **Step 6: 커밋한다**

```bash
cd /c/workAndroid/SwordForge && git add app/src/main/java/com/geomgang/game/ForgeViewModel.kt app/src/main/java/com/geomgang/game/ForgeUiState.kt app/src/test/java/com/geomgang/game/ForgeViewModelTemperTest.kt && git commit -m "담금질과 자취를 화면 상태까지"
```

---

### Task 8: 진동

**Files:**
- Create: `app/src/main/java/com/geomgang/game/feel/HapticEngine.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `core/src/main/kotlin/com/geomgang/core/Settings.kt`
- Modify: `app/src/main/java/com/geomgang/game/ForgeViewModel.kt`
- Modify: `app/src/main/java/com/geomgang/game/MainActivity.kt`
- Modify: `app/src/main/java/com/geomgang/game/ui/SettingsScreen.kt`

**Interfaces:**
- Produces:
  - `HapticEngine(vibrator: Vibrator?, enabled: () -> Boolean)` — `forgeSuccess(level: Int)`, `forgeStay()`, `forgeDrop()`, `forgeDestroy()`, `newRecord()`, `preventUsed()`, `starUp()`, `starDown()`, `monsterDown()`
  - `systemVibrator(context: Context): Vibrator?`
  - `Settings.hapticsOn: Boolean`
  - `ForgeViewModel.hapticsEnabled(): Boolean`, `ForgeViewModel.setHapticsOn(on: Boolean)`

- [ ] **Step 1: 설정에 항목을 더한다**

`core/src/main/kotlin/com/geomgang/core/Settings.kt` 의 `soundOn` 아래에 넣는다:

```kotlin
    /**
     * 진동. 기본값이 켜짐인 이유는 소리와 같다 - **손맛의 절반이 여기 있다.**
     *
     * 성공과 파괴가 손에 똑같이 오면 5초를 걸고 누른 그 순간이 밋밋해진다.
     */
    val hapticsOn: Boolean = true,
```

- [ ] **Step 2: 권한을 더한다**

`app/src/main/AndroidManifest.xml` 의 `<manifest>` 여는 태그 바로 아래에 넣는다:

```xml
    <!-- 진동은 런타임 요청이 없는 권한이다. 선언만 하면 된다. -->
    <uses-permission android:name="android.permission.VIBRATE" />
```

- [ ] **Step 3: HapticEngine 을 만든다**

`app/src/main/java/com/geomgang/game/feel/HapticEngine.kt`:

```kotlin
package com.geomgang.game.feel

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * 이 기기의 진동기. 없으면 null 이다.
 *
 * API 31 부터 [VibratorManager] 를 거쳐야 한다. 예전 방식은 남아 있지만 경고가 붙는다.
 */
fun systemVibrator(context: Context): Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
            ?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

/**
 * 손끝으로 결과를 알린다.
 *
 * 소리는 처음부터 있었는데 진동이 없었다. 폰 게임에서 손맛의 절반이 진동이라,
 * 성공도 파괴도 손에는 똑같이 아무것도 오지 않았다.
 *
 * [com.geomgang.game.sound.SoundEngine] 과 같은 모양이다 - 켜짐 여부를 그때그때 읽고,
 * 실패해도 게임이 멈추지 않는다. **진동은 있으면 좋은 것이지 필수가 아니다.**
 *
 * 사냥 탭마다는 울리지 않는다. 연타라 손이 아프고 배터리만 먹는다.
 */
class HapticEngine(
    private val vibrator: Vibrator?,
    private val enabled: () -> Boolean,
) {

    /** 강화 성공. 짧고 밝게 톡. */
    fun forgeSuccess(level: Int) {
        // 단계가 높을수록 아주 조금 길게 - 어렵게 얻은 한 칸이 더 오래 남는다.
        val length = 18L + (level.coerceIn(0, 40) / 10)
        play(longArrayOf(0, length), intArrayOf(0, 140))
    }

    /** 실패했지만 단계 유지. 둔탁하게 한 번. */
    fun forgeStay() = play(longArrayOf(0, 45), intArrayOf(0, 90))

    /** 하락. 아래로 미끄러지듯 둘. */
    fun forgeDrop() = play(longArrayOf(0, 30, 25, 60), intArrayOf(0, 150, 0, 70))

    /** 파괴. 가장 길고 세게. 이 게임에서 제일 아픈 순간이다. */
    fun forgeDestroy() = play(longArrayOf(0, 90, 50, 180), intArrayOf(0, 255, 0, 200))

    /** 최고 기록 경신. 톡·톡·톡 올라간다. */
    fun newRecord() =
        play(longArrayOf(0, 20, 40, 20, 40, 45), intArrayOf(0, 120, 0, 170, 0, 255))

    /** 방지권으로 살렸다. */
    fun preventUsed() = play(longArrayOf(0, 25, 20, 25), intArrayOf(0, 200, 0, 200))

    /** 별이 올랐다. */
    fun starUp() = play(longArrayOf(0, 16, 30, 24), intArrayOf(0, 130, 0, 180))

    /** 별을 잃었다. 검은 무사하므로 파괴보다 훨씬 약하게. */
    fun starDown() = play(longArrayOf(0, 40), intArrayOf(0, 110))

    /** 몬스터를 잡았다. 아주 짧게. */
    fun monsterDown() = play(longArrayOf(0, 12), intArrayOf(0, 100))

    private fun play(timings: LongArray, amplitudes: IntArray) {
        if (!enabled()) return
        val device = vibrator ?: return
        runCatching {
            if (!device.hasVibrator()) return
            device.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        }
    }
}
```

신기록 팡파르도 함께 만든다. `app/src/main/java/com/geomgang/game/sound/SoundEngine.kt` 의
`uniqueBorn()` 아래에 넣는다:

```kotlin
    /** 최고 기록 경신. 고유검보다 짧되 분명하게 올라가는 세 음. */
    fun newRecord() = play {
        tone(784.0, 0.14, decay = 10.0, gain = 0.32)
        at(0.11) { tone(988.0, 0.14, decay = 10.0, gain = 0.32) }
        at(0.22) { tone(1_318.0, 0.34, decay = 5.0, gain = 0.38) }
        at(0.22) { tone(1_568.0, 0.34, decay = 5.0, gain = 0.18) }
    }
```

- [ ] **Step 4: 뷰모델과 화면에 연결한다**

`ForgeViewModel` 생성자에 인자를 더한다. 기본값을 주어 기존 테스트가 그대로 컴파일되게 한다:

```kotlin
    private val haptics: HapticEngine = HapticEngine(null) { false },
```

`ForgeViewModel` 에 설정 함수를 더한다 (`setSoundOn` 바로 아래):

```kotlin
    fun setHapticsOn(on: Boolean) {
        settings = settings.copy(hapticsOn = on)
        store.saveSettings(settings)
        _ui.value = render()
        if (on) haptics.forgeSuccess(0)
    }

    /** 진동을 울릴지 판단할 때 쓴다. 설정이 바뀌면 즉시 반영된다. */
    fun hapticsEnabled(): Boolean = settings.hapticsOn
```

`forge()` 안의 `when (result)` 블록에서 소리 옆에 진동을 나란히 붙인다:

```kotlin
        when (result) {
            is ForgeResult.Success -> {
                // 신기록이면 소리와 진동을 둘 다 특별한 것으로 바꾼다.
                if (lastWasRecord) {
                    sound.newRecord()
                    haptics.newRecord()
                } else {
                    sound.forgeSuccess(result.newLevel)
                    haptics.forgeSuccess(result.newLevel)
                }
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
```

`sound.preventUsed()` 옆에 `haptics.preventUsed()`, `sound.monsterDown()` 옆에 `haptics.monsterDown()` 을 더한다. 별 강화 결과를 다루는 자리에서는 성공이면 `haptics.starUp()`, 실패면 `haptics.starDown()` 을 부른다.

`MainActivity.kt` 의 `App` 에서 진동기를 만들어 넘긴다:

```kotlin
    val context = LocalContext.current
    val vm = remember {
        lateinit var holder: ForgeViewModel
        val engine = SoundEngine { holder.soundEnabled() }
        val feel = HapticEngine(systemVibrator(context)) { holder.hapticsEnabled() }
        holder = ForgeViewModel(store, ONLY_MODE, sound = engine, haptics = feel)
        holder
    }
```

`MainActivity.kt` 에 import 를 더한다:

```kotlin
import androidx.compose.ui.platform.LocalContext
import com.geomgang.game.feel.HapticEngine
import com.geomgang.game.feel.systemVibrator
```

`SettingsScreen` 에 토글을 더한다. 소리 토글 바로 아래에 같은 모양으로 놓고, 파라미터 `onHapticsChange: (Boolean) -> Unit` 을 추가한다. `MainActivity` 의 `Overlay.Settings` 분기에서 `onHapticsChange = vm::setHapticsOn` 을 넘긴다.

- [ ] **Step 5: 빌드하고 테스트를 돌린다**

```bash
cd /c/workAndroid/SwordForge && ./gradlew.bat :core:test :app:testDebugUnitTest --console=plain
```

Expected: PASS

- [ ] **Step 6: 커밋한다**

```bash
cd /c/workAndroid/SwordForge && git add app/src/main/java/com/geomgang/game/feel/HapticEngine.kt app/src/main/java/com/geomgang/game/sound/SoundEngine.kt app/src/main/AndroidManifest.xml core/src/main/kotlin/com/geomgang/core/Settings.kt app/src/main/java/com/geomgang/game/ForgeViewModel.kt app/src/main/java/com/geomgang/game/MainActivity.kt app/src/main/java/com/geomgang/game/ui/SettingsScreen.kt && git commit -m "진동 - 성공 실패 파괴가 손에 다르게 온다"
```

---

### Task 9: 강화 화면

**Files:**
- Modify: `app/src/main/java/com/geomgang/game/ui/ForgeScreen.kt`

**Interfaces:**
- Consumes: `ForgeUiState.temper`/`recentMarks`/`isRecord` (Task 7)

- [ ] **Step 1: 담금질 게이지를 그린다**

`ForgeScreen.kt` 의 확률 줄(`Stat("🎯", "성공", ...)` 가 있는 `Row`) **바로 아래**에 넣는다:

```kotlin
            state.temper?.let { temper ->
                Spacer(Modifier.height(8.dp))
                TemperBar(temper)
            }
```

같은 파일 아래쪽에 컴포저블을 더한다:

```kotlin
/**
 * 담금질 게이지.
 *
 * 무한 구간에서만 나온다. 실패가 쌓인 만큼 차오르고, 성공하면 비워진다.
 * **실패가 눈에 보이는 무언가를 남기는 유일한 자리**라 화면에 있어야 한다.
 */
@Composable
private fun TemperBar(temper: TemperUi) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "담금질",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE0A458),
            )
            Text(
                text = "%d회 · %.1f%% → %.1f%%".format(
                    temper.fails,
                    temper.basePercent,
                    temper.currentPercent,
                ),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { temper.ratio },
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFE0A458),
        )
    }
}
```

- [ ] **Step 2: 자취 줄과 신기록 배지를 그린다**

강화 버튼 **바로 아래**에 넣는다:

```kotlin
            if (state.isRecord) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "★ 최고 기록!",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD54A),
                )
            }
            if (state.recentMarks.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                MarkStrip(state.recentMarks)
            }
```

컴포저블을 더한다:

```kotlin
/**
 * 최근 강화 자취. 왼쪽이 오래된 것, 오른쪽이 방금 것이다.
 *
 * 결과가 한 번 뜨고 사라지면 "세 판째 말아먹는 중" 이라는 이야기가 남지 않는다.
 */
@Composable
private fun MarkStrip(marks: List<ForgeMark>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        marks.forEach { mark ->
            Text(
                text = when (mark) {
                    ForgeMark.UP -> "●"
                    ForgeMark.STAY -> "·"
                    ForgeMark.DOWN -> "▽"
                    ForgeMark.BREAK -> "✕"
                },
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 3.dp),
                color = when (mark) {
                    ForgeMark.UP -> Color(0xFF7FD48A)
                    ForgeMark.STAY -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    ForgeMark.DOWN -> Color(0xFFE0A060)
                    ForgeMark.BREAK -> MaterialTheme.colorScheme.error
                },
            )
        }
    }
}
```

- [ ] **Step 3: 아이템 칩에 배타를 알린다**

두 `FilterChip` 을 감싼 `Row` 바로 위에 안내 한 줄을 넣는다:

```kotlin
                Text(
                    text = "둘 중 하나만 걸 수 있다",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                )
```

칩 자체는 고치지 않는다 — 배타는 [UsedItems] 가 지키므로 한쪽을 켜면 다른 쪽 `selected` 가 저절로 내려간다.

`ForgeScreen.kt` 상단에 import 를 더한다:

```kotlin
import androidx.compose.material3.LinearProgressIndicator
import com.geomgang.core.ForgeMark
import com.geomgang.game.TemperUi
```

- [ ] **Step 4: 빌드한다**

```bash
cd /c/workAndroid/SwordForge && ./gradlew.bat :app:compileDebugKotlin --console=plain
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 커밋한다**

```bash
cd /c/workAndroid/SwordForge && git add app/src/main/java/com/geomgang/game/ui/ForgeScreen.kt && git commit -m "담금질 게이지와 자취 줄"
```

---

### Task 10: 실기기 확인과 마무리

**Files:**
- Modify: `README.md`

- [ ] **Step 1: 전체 테스트와 빌드**

```bash
cd /c/workAndroid/SwordForge && ./gradlew.bat :core:test :app:testDebugUnitTest :app:assembleDebug --console=plain
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 폰에 설치하고 크래시가 없는지 본다**

```bash
"/c/Users/사용자/AppData/Local/Android/Sdk/platform-tools/adb.exe" -s R3CN50JXF9E install -r "C:/workAndroid/SwordForge/app/build/outputs/apk/debug/app-debug.apk"
```

이어서 앱을 실행하고 `adb logcat -d -s AndroidRuntime:E` 로 예외가 없는지 본다. 기기가 안 붙어 있으면 여기서 멈추고 사용자에게 연결을 요청한다.

- [ ] **Step 3: 눈으로 확인할 것**

- 강화 화면에 담금질 게이지가 나오는가 (무한 구간에서만)
- 실패할 때마다 게이지가 차고 「n회 · a% → b%」 숫자가 오르는가
- 성공하면 게이지가 비워지는가
- 축복서를 켜면 부적이 내려가는가
- 강화 버튼 아래 자취 줄이 쌓이는가
- 성공·실패·파괴가 손에 **다르게** 오는가
- 설정에서 진동을 끄면 조용해지는가

- [ ] **Step 4: README 를 갱신한다**

`README.md` 의 기능 목록에 담금질·배타 아이템·진동을 한 줄씩 더한다. 개발 이력 절이 있으면 이번 작업을 날짜와 함께 덧붙인다.

- [ ] **Step 5: 커밋하고 푸시한다**

커밋 메시지를 스크래치패드 파일에 쓰고 `-F` 로 넣는다 (PowerShell 5.1 이 여러 줄 한글을 망가뜨린다).

```bash
cd /c/workAndroid/SwordForge && git add -A && git commit -F "$SCRATCHPAD/tempering_done.txt" && git push origin main
```
