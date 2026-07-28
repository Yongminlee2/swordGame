# 담금질과 손맛 — 설계

2026-07-29

## 왜 하는가

이 게임의 축은 처음부터 하나였다. **강화 한 번 한 번에 손이 떨리는 맛.**
지금 그 떨림이 없다. 이유가 둘이다.

### 1. 누를 때 고를 게 없다

아이템 셋이 전부 "있으면 무조건 쓰는 것"이다.

| 아이템 | 효과 | 언제 쓰나 |
|---|---|---|
| 축복서 | 성공률 +10%p | 항상 |
| 행운부적 | 실패해도 하락·파괴 없음 | 항상 |
| 방지권 | 파괴 방지 | 항상 |

최선의 수가 늘 뻔하다. **선택이 없으면 손이 떨리지 않는다.** 떨림은 확률이 아니라
갈림길에서 나온다.

### 2. 무한 구간이 도박이 아니라 로또다

실기기 세이브(+44) 기준으로 성공률은 [RateTable.ENDLESS_FLOOR] 0.5%에 붙어 있고,
실패하면 [RateTable.destroyChance] 가 1.00 이라 반드시 파괴된다.
한 단계에 기댓값 200번, 강화석 3,000개다.

그리고 **실패가 아무것도 남기지 않는다.** 200번을 실패해도 201번째 확률은 여전히
0.5%다. 이건 긴장이 아니라 대기다.

## 목표

- 강화 한 판마다 **결정**이 생긴다
- **실패가 진전이 된다**
- 성공·실패·파괴가 손끝에 다르게 온다

## 하지 않는 것

- 계열별 특성과 도감 완성 보상 — 별도 작업이다. 이 문서는 강화 그 자체만 다룬다
- 아이템 가격 조정 — [Economy.canBuyItem] 의 주석이 적어 둔 가드레일을 다시 열지 않는다
- 사냥 탭마다 진동 — 연타라 손이 아프고 배터리만 먹는다. 처치와 치명타만 울린다
- 유한 모드(+20 이하) 확률 변경 — [BalanceSimulationTest] 가 잡아 둔 곡선은 그대로다

---

## A. 손맛 3종

### A-1. 진동

`app/src/main/java/com/geomgang/game/feel/HapticEngine.kt` 를 새로 만든다.
[SoundEngine] 과 같은 모양이다 — 생성자가 켜짐 여부를 읽는 람다를 받고,
실패해도 게임이 멈추지 않게 전부 `runCatching` 안에서 돈다.

```kotlin
class HapticEngine(private val vibrator: Vibrator?, private val enabled: () -> Boolean)
```

| 함수 | 손에 오는 것 | 파형(ms / 세기) |
|---|---|---|
| `forgeSuccess(level)` | 짧고 밝게 톡 | `[0,18]` / 140 |
| `forgeStay()` | 둔탁하게 한 번 | `[0,45]` / 90 |
| `forgeDrop()` | 아래로 미끄러지듯 | `[0,30,25,60]` / 150·0·70 |
| `forgeDestroy()` | 길게 쿵 | `[0,90,50,180]` / 255·0·200 |
| `newRecord()` | 톡·톡·톡 올라감 | `[0,20,40,20,40,45]` / 120·0·170·0·255 |
| `preventUsed()` | 살렸다 | `[0,25,20,25]` / 200·0·200 |
| `starUp()` / `starDown()` | 별 결과 | 성공/유지와 같은 계열 |
| `monsterDown()` / `critical()` | 처치·치명타 | 아주 짧게 |

- minSdk 26 이라 `VibrationEffect.createWaveform` 을 그대로 쓴다.
  API 31 이상은 `VibratorManager` 를 거쳐 얻는다.
- `AndroidManifest.xml` 에 `android.permission.VIBRATE` 를 넣는다. 런타임 요청이 없는 권한이다.
- [Settings] 에 `hapticsOn: Boolean = true` 를 더한다. 기본이 켜짐인 이유는 소리와 같다 —
  **손맛의 절반이 여기 있다.** 설정 화면에 토글을 놓는다.
- `MainActivity` 가 [SoundEngine] 을 만드는 자리에서 함께 만든다.
- 부르는 자리는 `ForgeViewModel` 이 소리를 부르는 자리와 **정확히 같다.**
  두 곳이 어긋나면 소리와 진동이 따로 논다.

### A-2. 최근 기록 줄

`core/src/main/kotlin/com/geomgang/core/ForgeMarks.kt`

```kotlin
/** 강화 한 판의 결과를 한 글자로 줄인 것. */
enum class ForgeMark { UP, STAY, DOWN, BREAK }

object ForgeMarks {
    /** 화면에 남기는 판 수. */
    const val KEEP = 12

    fun of(result: ForgeResult): ForgeMark
    /** 새 결과를 뒤에 붙이고 [KEEP] 을 넘으면 앞에서 버린다. */
    fun push(marks: List<ForgeMark>, mark: ForgeMark): List<ForgeMark>
}
```

[GameState] 에 `recentMarks: List<ForgeMark> = emptyList()` 를 더한다.
기본값이 있으므로 옛 세이브가 그대로 열린다.

화면은 강화 버튼 아래 한 줄이다. 왼쪽이 오래된 것, 오른쪽이 방금 것이다.

| 결과 | 글자 | 색 |
|---|---|---|
| 성공 | `●` | 초록 |
| 유지 | `·` | 회색 |
| 하락 | `▽` | 주황 |
| 파괴 | `✕` | 빨강 |

지금은 결과가 한 번 뜨고 사라져서 **"세 판째 말아먹는 중"이라는 이야기가 안 남는다.**
연속 실패 횟수는 이미 통계로 세고 있는데 화면에 없다.

### A-3. 신기록

`runAttempt` 가 시도 전 `bestLevel` 을 들고 있다가 성공 후 넘어섰는지 본다.
넘어섰고 도달 단계가 `MIN_RECORD_LEVEL`(10) 이상이면 신기록이다.

10단계 문턱을 두는 이유: 새 세이브는 처음 열 판이 전부 신기록이라 연출이 금방 값을 잃는다.
같은 문턱을 [StarForce.MIN_LEVEL] 도 쓰고 있어 규칙이 한 벌로 읽힌다.

[ForgeUiState] 에 `isRecord: Boolean` 을 더하고, 연출은 금색 「최고 기록!」 한 줄과
`HapticEngine.newRecord()` 와 새 팡파르 `SoundEngine.newRecord()` 다.

---

## B. 담금질

### 규칙

**+21 부터** 실패할 때마다 다음 성공률이 오른다. 성공하면 0으로 돌아간다.

`core/src/main/kotlin/com/geomgang/core/Tempering.kt`

```kotlin
object Tempering {
    /** 담금질이 붙는 첫 단계. 유한 구간은 확률이 충분해 필요 없다. */
    const val MIN_LEVEL = RateTable.MAX_FINITE_LEVEL + 1   // 21

    /** 실패 한 번이 더해 주는 몫. 기준 성공률에 대한 비율이다. */
    const val STEP_RATIO = 0.5

    /** 담금질만으로 넘을 수 없는 성공률 상한. */
    const val MAX_RATE = 0.50

    fun applies(targetLevel: Int): Boolean = targetLevel >= MIN_LEVEL

    /** 담금질을 반영한 성공률. 붙지 않는 구간이면 [baseRate] 그대로다. */
    fun rateFor(baseRate: Double, targetLevel: Int, fails: Int): Double
}
```

`rateFor` 는 `min(base + base * STEP_RATIO * fails, MAX_RATE)` 이며, 붙지 않는 구간이거나
`fails <= 0` 이면 `baseRate` 를 그대로 돌려준다. `baseRate` 가 이미 [MAX_RATE] 보다 높으면
낮추지 않는다 — 담금질은 올려 주기만 한다.

가산분을 **기준 성공률에 비례**시키는 이유: 고정 %p 로 두면 +21(1.7%)에서는 미미하고
+45(0.5%)에서는 과하거나 그 반대가 된다. 비율로 두면 어느 단계에서나 "실패 두 번이면
확률이 두 배" 라는 같은 감각이 나온다.

상한 50%: 담금질만으로 공짜가 되면 안 된다. 무한 구간이 끝없이 이어지는 이유가
사라진다.

### 확률표에 넣는 자리

[RateTable.successRate] 하나가 계속 단일 출처여야 한다. 담금질은 **난이도 배수 뒤,
축복서 앞**에 들어간다.

```kotlin
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

축복서를 담금질 **뒤**에 두는 이유: 축복서는 "이번 판만 얹는 것" 이라는 성격이라
누적분 위에 얹혀야 말이 된다. 최종 상한은 [MAX_SUCCESS_RATE] 0.98 그대로다.

`temperFails` 의 기본값이 0 이므로 이 인자를 모르는 기존 호출부는 그대로 돈다.

### 상태

[GameState] 에 둘을 더한다.

```kotlin
/** 담금질이 쌓인 목표 단계. 단계가 바뀌면 누적을 버린다. */
val temperLevel: Int = 0,
/** 그 단계에서 실패한 횟수. */
val temperFails: Int = 0,
```

`runAttempt` 의 처리 순서:

1. 이번 목표 단계가 `temperLevel` 과 다르면 `temperFails = 0`, `temperLevel = 목표` 로 맞춘다
2. 판정한다
3. 성공이면 `temperFails = 0`, 실패면 `temperFails + 1`

검이 부서져 새 검을 사면 목표 단계가 달라지므로 1번에서 저절로 초기화된다.
방지권으로 살아남으면 단계가 그대로라 누적이 이어진다 — **방지권이 담금질을 지킨다**는
뜻이 생긴다.

### 화면

확률 줄 아래에 게이지 한 줄.

```
담금질  ████████░░░░░░░░
        47회 · 0.5% → 12.3%
```

붙지 않는 구간(+20 이하)에서는 아예 그리지 않는다. 게이지가 0 이면 회색으로 비워 둔다.
[ForgeOdds] 가 이미 네 확률의 단일 출처이므로 `temperFails` 를 받아 반영하고,
게이지에 필요한 「기준 → 지금」 두 값도 여기서 낸다.

---

## B-2. 축복서와 부적은 하나만

[UsedItems] 가 배타를 직접 지킨다. 화면이 두 토글을 각각 다루면 반드시 어긋난다.

```kotlin
data class UsedItems(val blessing: Boolean = false, val luckCharm: Boolean = false) {
    fun toggleBlessing(): UsedItems
    fun toggleLuckCharm(): UsedItems
}
```

한쪽을 켜면 다른 쪽이 꺼진다. 둘 다 끄는 것은 언제나 가능하다.

### 왜 이게 갈림길이 되는가

- **행운부적** — 실패해도 안 부서진다. **그런데 담금질은 오른다.**
  안전하게 게이지를 쌓는 수다
- **축복서** — 이번 판 확률만 +10%p. 게이지가 낮을 때 지르는 수다

부적으로 담금질을 쌓다가 게이지가 차면 맨몸으로 지른다. 부적이 열 장뿐이라
**언제 쓸지**가 매 판 선택이 된다. 지금은 "있으면 켠다" 뿐이라 선택이 아니었다.

부적 실패가 담금질을 올리는 것이 이 설계의 핵심이다. 올리지 않으면 부적은 그저
"손해 없는 굴림" 이 되어 다시 무뇌 선택으로 돌아간다.

---

## 저장 호환

새 필드는 넷이고 전부 기본값이 있다.

| 필드 | 있는 곳 | 기본값 |
|---|---|---|
| `recentMarks` | [GameState] | `emptyList()` |
| `temperLevel` | [GameState] | `0` |
| `temperFails` | [GameState] | `0` |
| `hapticsOn` | [Settings] | `true` |

`ignoreUnknownKeys = true` 와 함께라서 옛 세이브가 손실 없이 열린다.
[SaveStore] 에 이관 코드는 필요 없다 — 도감 때와 달리 **모양이 바뀌는 필드가 없다.**

---

## 테스트

밸런스를 느낌으로 두지 않는다.

### 새 테스트

- `TemperingTest`
  - `applies` 가 +20 에서 거짓, +21 에서 참
  - `rateFor` 가 실패 수에 대해 단조 증가
  - [Tempering.MAX_RATE] 를 넘지 않는다
  - `fails = 0` 이면 기준값 그대로
  - 기준값이 이미 상한보다 높으면 낮추지 않는다
- `ForgeMarksTest`
  - `push` 가 [ForgeMarks.KEEP] 에서 멈추고 앞에서 버린다
  - 방금 것이 마지막에 온다
  - `of` 가 네 결과를 각각 옮긴다
- `UsedItemsTest`
  - 한쪽을 켜면 다른 쪽이 꺼진다
  - 켠 것을 다시 누르면 둘 다 꺼진다
- `TemperTempoTest` — **이 작업의 핵심 가드레일**
  - +45 한 단계를 시뮬레이션으로 만 번 굴려 중앙값 시도 수를 잰다
  - 목표 구간 **15~45회**. 벗어나면 실패한다
  - 담금질 없는 같은 조건이 100회를 넘는지 함께 확인해, 이 장치가 실제로 뭘 바꿨는지
    숫자로 남긴다
- `ForgeViewModel` 배선
  - 실패하면 `temperFails` 가 오른다
  - **부적을 쓴 실패도** `temperFails` 가 오른다
  - 성공하면 0 으로 돌아간다
  - 목표 단계가 바뀌면 0 으로 돌아간다
  - 신기록일 때만 `isRecord` 가 참이고, +10 미만은 거짓이다

### 지키는 테스트

[BalanceSimulationTest] 는 유한 모드(+20 이하)만 잠그고 무한 구간은 「21을 넘긴다」는
**하한**만 본다. 담금질은 +21 위에서만 도니 이 테스트들은 그대로 통과해야 한다.
통과하지 못하면 유한 구간에 손이 닿았다는 뜻이므로 되돌린다.

[ForgeTempoTest]·[BossTempoTest] 도 건드리지 않는다.

---

## 순서

1. `Tempering` + `ForgeMarks` + `UsedItems` 배타 — 순수 도메인, 테스트와 함께
2. `RateTable.successRate` 에 담금질 연결, [ForgeOdds] 가 받아 내보내기
3. `GameState` 필드 넷과 `runAttempt` 배선, `TemperTempoTest` 로 구간 확정
4. `HapticEngine` + 매니페스트 권한 + 설정 토글
5. 화면 — 담금질 게이지, 기록 줄, 신기록 연출, 토글 배타 표시
6. 실기기 설치와 확인
