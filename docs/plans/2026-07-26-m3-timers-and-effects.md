# M3: 방지권 · 줍기 · 강화 연출 구현 계획


**Goal:** 손맛을 완성한다. 파괴가 나면 작은 원이 뜨고 2.5초 안에 눌러야 검을 살리며, 놓치면 파편이 흩어져 3초 안에 주워야 한다. 강화 결과가 흔들림과 번쩍임으로 몸에 전달된다.

**Architecture:** 제한 시간 값은 `:core`의 `Timing`에 상수로 두고, 카운트다운 진행은 `ForgeViewModel`이 코루틴으로 돌린다. 화면은 남은 시간을 비율로 받아 그리기만 한다. 타이머 로직을 ViewModel에 두는 덕분에 가상 시간으로 JVM 테스트가 가능하다.

**Tech Stack:** M2와 동일 + `kotlinx-coroutines-test`

**참조:** [스펙](../specs/2026-07-26-sword-enhance-game-design.md) · [M2 계획](2026-07-26-m2-forge-screen-and-save.md)

## Global Constraints

M1·M2의 제약이 그대로 유효하다. 여기에 더해:

- **제한 시간 값은 `:core`에만 둔다.** UI나 ViewModel에 2500·3000을 직접 쓰지 않는다.
- **카운트다운은 ViewModel이 돌린다.** Compose의 `LaunchedEffect`로 시간을 재지 않는다. 화면이 시간을 재면 테스트할 수 없고, 화면 재구성 때마다 타이머가 흔들린다.
- **놓친 것과 쓴 것을 통계에 정확히 남긴다.** 방지권을 놓치면 `onPreventMissed`, 줍기를 놓치면 `onSalvageMissed`.
- **파괴 대기 상태는 계속 저장되어 있어야 한다.** 창이 떠 있는 동안 앱이 죽으면 다음 실행에서 파괴가 확정되는 M2 동작을 깨뜨리지 않는다.
- 브랜치는 `m3-timers-and-effects`. 완료 후 `main` 병합·푸시.
- 커밋 메시지 한국어, `Co-Authored-By` 없음.

---

## File Structure

```
core/src/main/kotlin/com/geomgang/core/
└── Timing.kt                    제한 시간 상수 (신규)

app/src/main/java/com/geomgang/game/
├── DestroyPhase.kt              파괴 후 어느 단계인지 (신규)
├── ForgeUiState.kt              destroyPhase 추가
├── ForgeViewModel.kt            카운트다운 코루틴
└── ui/
    ├── ForgeScreen.kt           단계별 UI 분기
    ├── PreventRing.kt           줄어드는 원 (신규)
    ├── SalvageShards.kt         흩어진 파편 (신규)
    └── SwordView.kt             흔들림·번쩍임 인자 추가

app/src/test/java/com/geomgang/game/
└── ForgeViewModelTimerTest.kt   가상 시간 타이머 검증 (신규)
```

---

### Task 1: 제한 시간과 카운트다운

**Files:**
- Create: `core/src/main/kotlin/com/geomgang/core/Timing.kt`
- Create: `app/src/main/java/com/geomgang/game/DestroyPhase.kt`
- Modify: `app/src/main/java/com/geomgang/game/ForgeUiState.kt`, `ForgeViewModel.kt`
- Modify: `app/build.gradle.kts` (테스트 의존성)
- Test: `app/src/test/java/com/geomgang/game/ForgeViewModelTimerTest.kt`

**Interfaces:**
- Produces:
  - `object Timing` — `PREVENT_WINDOW_MILLIS = 2_500L`, `SALVAGE_WINDOW_MILLIS = 3_000L`, `TICK_MILLIS = 50L`
  - `sealed interface DestroyPhase` — `None`, `Prevent(remainingMillis, totalMillis)`, `Salvage(remainingMillis, totalMillis)`; 각각 `val progress: Float`
  - `ForgeUiState.destroyPhase: DestroyPhase`
  - `ForgeViewModel`은 파괴 직후 자동으로 방지권 창을 연다. 방지권이 없으면 곧장 줍기 창으로 간다.

**상태 흐름**

```
파괴 판정
  ├ 방지권 있음 → Prevent(2.5초)
  │     ├ 탭    → applyPrevent → None
  │     └ 만료  → onPreventMissed → Salvage(3초)
  └ 방지권 없음 → Salvage(3초)
        ├ 탭   → applySalvage → None
        └ 만료 → onSalvageMissed → confirmDestroy → None
```

- [ ] **Step 1: 테스트 의존성을 추가한다**

`gradle/libs.versions.toml`의 `[versions]`에 `coroutines = "1.8.1"`,
`[libraries]`에 `kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }`.

`app/build.gradle.kts`의 `dependencies`에:

```kotlin
testImplementation(libs.junit)
testImplementation(libs.kotlinx.coroutines.test)
```

- [ ] **Step 2: 실패하는 테스트를 작성한다**

`app/src/test/java/com/geomgang/game/ForgeViewModelTimerTest.kt`:

```kotlin
package com.geomgang.game

import com.geomgang.core.Difficulty
import com.geomgang.core.GameState
import com.geomgang.core.Inventory
import com.geomgang.core.SaveStore
import com.geomgang.core.Sword
import com.geomgang.core.Timing
import com.geomgang.core.WeaponFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class ForgeViewModelTimerTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** 반드시 실패하고 반드시 파괴되는 난수. 성공 판정 0.99, 파괴 판정 0.0. */
    private fun alwaysDestroy() = object : Random() {
        private var i = 0
        override fun nextBits(bitCount: Int): Int = 0
        override fun nextDouble(): Double = if (i++ % 2 == 0) 0.99 else 0.0
    }

    private fun vm(tickets: Int): ForgeViewModel {
        val store = SaveStore(tmp.root)
        store.saveGame(
            GameState(
                difficulty = Difficulty.NORMAL,
                gold = 1_000_000,
                sword = Sword(WeaponFamily.STRAIGHT, 19),
                inventory = Inventory(preventTickets = tickets),
                bestLevel = 19,
            ),
        )
        return ForgeViewModel(store, Difficulty.NORMAL, alwaysDestroy())
    }

    @Test
    fun `방지권이 있으면 파괴 직후 방지권 창이 열린다`() = runTest(dispatcher) {
        val vm = vm(tickets = 1)
        vm.forge()
        val phase = vm.ui.value.destroyPhase
        assertTrue("phase=$phase", phase is DestroyPhase.Prevent)
        assertEquals(Timing.PREVENT_WINDOW_MILLIS, (phase as DestroyPhase.Prevent).totalMillis)
    }

    @Test
    fun `방지권이 없으면 곧바로 줍기 창이 열린다`() = runTest(dispatcher) {
        val vm = vm(tickets = 0)
        vm.forge()
        assertTrue(vm.ui.value.destroyPhase is DestroyPhase.Salvage)
    }

    @Test
    fun `제한 시간 안에 누르면 검이 되살아난다`() = runTest(dispatcher) {
        val vm = vm(tickets = 1)
        vm.forge()
        advanceTimeBy(1_000)
        vm.usePrevent()
        assertEquals(DestroyPhase.None, vm.ui.value.destroyPhase)
        assertNotNull(vm.ui.value.sword)
        assertEquals(19, vm.ui.value.sword?.level)
        assertEquals(0, vm.ui.value.preventTickets)
    }

    @Test
    fun `제한 시간이 지나면 방지권 창이 닫히고 줍기 창으로 넘어간다`() = runTest(dispatcher) {
        val vm = vm(tickets = 1)
        vm.forge()
        advanceTimeBy(Timing.PREVENT_WINDOW_MILLIS + Timing.TICK_MILLIS)
        assertTrue(vm.ui.value.destroyPhase is DestroyPhase.Salvage)
        // 놓쳤을 뿐 방지권이 소모되지는 않는다
        assertEquals(1, vm.ui.value.preventTickets)
        assertNull(vm.ui.value.sword)
    }

    @Test
    fun `남은 시간이 줄어든다`() = runTest(dispatcher) {
        val vm = vm(tickets = 1)
        vm.forge()
        val first = (vm.ui.value.destroyPhase as DestroyPhase.Prevent).remainingMillis
        advanceTimeBy(500)
        val later = (vm.ui.value.destroyPhase as DestroyPhase.Prevent).remainingMillis
        assertTrue("$first -> $later", later < first)
    }

    @Test
    fun `줍기를 제한 시간 안에 누르면 조각을 얻는다`() = runTest(dispatcher) {
        val vm = vm(tickets = 0)
        vm.forge()
        advanceTimeBy(500)
        vm.salvage()
        assertEquals(DestroyPhase.None, vm.ui.value.destroyPhase)
        assertTrue("shards=${vm.ui.value.shards}", vm.ui.value.shards > 0)
    }

    @Test
    fun `줍기를 놓치면 아무것도 얻지 못하고 창이 닫힌다`() = runTest(dispatcher) {
        val vm = vm(tickets = 0)
        vm.forge()
        advanceTimeBy(Timing.SALVAGE_WINDOW_MILLIS + Timing.TICK_MILLIS)
        assertEquals(DestroyPhase.None, vm.ui.value.destroyPhase)
        assertEquals(0, vm.ui.value.shards)
        assertNull(vm.ui.value.sword)
    }

    @Test
    fun `창이 닫히면 강화를 다시 할 수 있다`() = runTest(dispatcher) {
        val vm = vm(tickets = 0)
        vm.forge()
        advanceTimeBy(Timing.SALVAGE_WINDOW_MILLIS + Timing.TICK_MILLIS)
        // 검이 없으니 강화는 못 하지만 잠금은 풀려 있어야 한다
        assertEquals(false, vm.ui.value.busy)
        assertTrue(vm.ui.value.canBuySword)
    }

    @Test
    fun `진행 비율은 0과 1 사이다`() = runTest(dispatcher) {
        val vm = vm(tickets = 1)
        vm.forge()
        repeat(10) {
            val p = vm.ui.value.destroyPhase.progress
            assertTrue("progress=$p", p in 0f..1f)
            advanceTimeBy(200)
        }
    }
}
```

- [ ] **Step 3: 실패를 확인한다**

```bash
cd /c/workAndroid/SwordForge && ./gradlew :app:testDebugUnitTest
```

Expected: 컴파일 실패 (`Timing`, `DestroyPhase` 없음).

- [ ] **Step 4: `Timing.kt`를 만든다**

```kotlin
package com.geomgang.core

/**
 * 반응 시간 규칙.
 *
 * 게임 규칙이지 UI 설정이 아니다. 이 값이 바뀌면 난이도가 바뀐다.
 * 그래서 화면이 아니라 여기에 둔다.
 */
object Timing {

    /** 파괴 직후 방지권을 쓸 수 있는 시간. 이 짧음이 이 게임의 핵심 긴장이다. */
    const val PREVENT_WINDOW_MILLIS: Long = 2_500

    /** 파괴가 확정된 뒤 파편을 주울 수 있는 시간. */
    const val SALVAGE_WINDOW_MILLIS: Long = 3_000

    /** 카운트다운 갱신 간격. */
    const val TICK_MILLIS: Long = 50
}
```

- [ ] **Step 5: `DestroyPhase.kt`를 만든다**

```kotlin
package com.geomgang.game

/** 파괴 판정 이후 지금 무엇을 기다리는 중인지. */
sealed interface DestroyPhase {

    /** 남은 시간 비율. 1.0 에서 시작해 0.0 으로 줄어든다. */
    val progress: Float

    data object None : DestroyPhase {
        override val progress: Float get() = 0f
    }

    /** 방지권을 쓸 수 있는 창이 열려 있다. */
    data class Prevent(val remainingMillis: Long, val totalMillis: Long) : DestroyPhase {
        override val progress: Float get() = (remainingMillis.toFloat() / totalMillis).coerceIn(0f, 1f)
    }

    /** 파편을 주울 수 있는 창이 열려 있다. */
    data class Salvage(val remainingMillis: Long, val totalMillis: Long) : DestroyPhase {
        override val progress: Float get() = (remainingMillis.toFloat() / totalMillis).coerceIn(0f, 1f)
    }
}
```

- [ ] **Step 6: ViewModel에 카운트다운을 붙인다**

`ForgeUiState`에 `val destroyPhase: DestroyPhase = DestroyPhase.None`을 더하고,
`awaitingDestroyChoice`는 `destroyPhase != DestroyPhase.None`으로 대체한다(중복 상태를 두지 않는다).

`ForgeViewModel`에:
- `private var phase: DestroyPhase = DestroyPhase.None`
- `private var countdownJob: Job? = null`
- `forge()`가 `ForgeResult.Destroyed`를 받으면 `openDestroyWindow(result.preventable)` 호출
- `openDestroyWindow` → 방지권 있으면 `runWindow(Prevent)` 아니면 `runWindow(Salvage)`
- `runWindow`는 `viewModelScope.launch`로 `TICK_MILLIS`마다 남은 시간을 줄이며 `render()`
- 만료 시 Prevent → `onPreventMissed` 후 줍기 창, Salvage → `onSalvageMissed` + `confirmDestroy` + 창 닫기
- `usePrevent()`/`salvage()`는 `countdownJob?.cancel()` 후 처리
- `onCleared()`에서 `countdownJob?.cancel()`

`busy`는 창이 열려 있는 동안 true로 유지해 강화 버튼을 잠근다.

- [ ] **Step 7: 테스트가 통과하는지 확인한다**

```bash
cd /c/workAndroid/SwordForge && ./gradlew :app:testDebugUnitTest :core:test
```

Expected: `:app` 9건, `:core` 145건 통과.

- [ ] **Step 8: 커밋한다**

```bash
cd /c/workAndroid/SwordForge
git add -A
git commit -m "M3-1: 방지권·줍기 제한 시간과 카운트다운

- Timing: 방지권 2.5초, 줍기 3초, 틱 50ms 를 :core 상수로
- DestroyPhase: None / Prevent / Salvage 와 남은 시간 비율
- ViewModel 이 코루틴으로 카운트다운을 돌리고 만료를 처리
- 방지권을 놓치면 소모 없이 기회만 잃고 줍기 창으로 넘어감
- 가상 시간 단위 테스트 9건"
```

---

### Task 2: 방지권 원과 줍기 파편 UI

**Files:**
- Create: `app/src/main/java/com/geomgang/game/ui/PreventRing.kt`
- Create: `app/src/main/java/com/geomgang/game/ui/SalvageShards.kt`
- Modify: `app/src/main/java/com/geomgang/game/ui/ForgeScreen.kt`

**Interfaces:**
- `@Composable fun PreventRing(progress: Float, enabled: Boolean, onTap: () -> Unit, modifier: Modifier)`
- `@Composable fun SalvageShards(progress: Float, onTap: () -> Unit, modifier: Modifier)`

**설계 의도** — 원작의 "작은 원을 클릭" 감각을 살린다. 원은 **작아야** 하고, 남은 시간이
테두리 호가 줄어드는 것으로 보여야 한다. 크고 편한 버튼이면 긴장이 사라진다.

파편은 검이 있던 자리에서 바깥으로 흩어진 조각들로 그리고, 영역 아무 데나 누르면 주워진다.
줍기는 놓치면 손해일 뿐 파국은 아니므로 방지권보다 관대하게 둔다.

- [ ] **Step 1: `PreventRing.kt`를 작성한다**

`Canvas`로 배경 원 + 남은 시간 호(`drawArc`, `sweepAngle = 360f * progress`)를 그리고
가운데에 "살리기"를 쓴다. `clickable`로 탭을 받는다.
`progress`가 0.3 아래로 내려가면 색을 붉게 바꿔 다급함을 준다.

- [ ] **Step 2: `SalvageShards.kt`를 작성한다**

파편 6~8개를 고정된 각도로 배치하고, `progress`에 따라 중심에서 멀어지며 흐려지게 한다.
난수를 쓰지 않는다 — 재구성마다 위치가 바뀌면 누르기 어렵다.

- [ ] **Step 3: `ForgeScreen`에서 단계별로 분기한다**

`state.destroyPhase`가
- `Prevent` → 검 자리에 `PreventRing`
- `Salvage` → 검 자리에 `SalvageShards`
- `None` → 평소 화면

하단 버튼은 창이 열려 있으면 감춘다. 원과 파편을 눌러야 하기 때문이다.

- [ ] **Step 4: 빌드하고 커밋한다**

```bash
cd /c/workAndroid/SwordForge && ./gradlew :app:assembleDebug
```

---

### Task 3: 강화 연출

**Files:**
- Modify: `app/src/main/java/com/geomgang/game/ui/SwordView.kt`, `ForgeScreen.kt`

**연출 규칙** — 결과가 몸에 남을 만큼만, 진행을 막지 않을 만큼 짧게.

| 결과 | 연출 | 길이 |
|---|---|---|
| 성공 | 검이 위로 살짝 튀며 밝게 번쩍 | 350ms |
| 유지 | 좌우로 짧게 흔들림 | 250ms |
| 하락 | 좌우로 크게 흔들리며 어두워짐 | 400ms |
| 파괴 | 검이 사라지고 화면이 붉게 번쩍 | 300ms |

`animateFloatAsState`와 `Animatable`로 처리하고, 흔들림은 `graphicsLayer`의 `translationX`에 준다.

- [ ] **Step 1: `SwordView`에 `shake: Float`와 `flash: Float` 인자를 더한다**
- [ ] **Step 2: `ForgeScreen`이 `lastResult`에 따라 애니메이션을 구동한다**
- [ ] **Step 3: 빌드하고 커밋한다**

---

### Task 4: 마무리

- [ ] **Step 1: 전체 검증**

```bash
cd /c/workAndroid/SwordForge && ./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug
```

- [ ] **Step 2: README 개발 일지 v0.4.0 추가**

구현 내용, 추가 테스트, 실기기 확인 여부(기기가 없으면 미확인으로 적는다), 겪은 문제.

- [ ] **Step 3: main 병합·푸시**

---

## 완료 기준

- [ ] `:core` 145건, `:app` 9건 전부 통과
- [ ] APK 빌드
- [ ] 제한 시간 값이 `:core`의 `Timing`에만 있다
- [ ] 방지권을 놓치면 소모되지 않고 기회만 사라진다
- [ ] 창이 열려 있는 동안 강화 버튼이 잠긴다
- [ ] README v0.4.0 기록, `main` 푸시
