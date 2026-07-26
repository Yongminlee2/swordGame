# M12 사냥 랜덤 이벤트 + 일일 퀘스트 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 사냥이 매번 다르게 흘러가게 만드는 랜덤 이벤트 8종과, 매일 켤 이유를 주는 일일 퀘스트(일 3 + 주간 1)를 넣는다.

**Architecture:** 판정·보상 수식은 `:core` 순수 함수(`HuntEvents`, `DailyQuests`), 시간·탭 상태는 ViewModel. 퀘스트 진행도는 기존 `Stats` 카운터의 **스냅샷 차분** — 배정 시점 값을 저장하고 현재값과의 차이가 진행도다. 날짜 키는 앱 계층이 계산해 문자열로 넘긴다(`:core`는 시계를 모른다).

**Tech Stack:** Kotlin, kotlinx-serialization(새 필드 전부 기본값 = 세이브 호환), JUnit.

## Global Constraints

- M11 계획의 Global Constraints 전부 그대로 (AGP 내장 Kotlin, MS949, `:core` 순수, AI 언급 금지, 한국어 UI)
- **난수 소비 순서는 계약**: `spawnNext` = ① nextInt(몬스터) ② nextDouble(희귀) ③ nextDouble(이벤트 발생) ④ 발생 시 nextDouble(이벤트 종류). 기존 사냥 테스트의 스크립트를 이 순서로 갱신한다
- 사냥 테스트 규칙: 상태 캡처 → `leaveHunt()` → 단언 (M11의 행 함정)
- 이벤트는 잡몹 스폰에만 붙는다(보스전 제외). 지속 버프(골든타임·상인) 중에는 새 이벤트를 굴리지 않는다

---

### Task 1: Stats 카운터 확장 + Progress 훅

**Files:**
- Modify: `core/src/main/kotlin/com/geomgang/core/Progress.kt`
- Test: `core/src/test/kotlin/com/geomgang/core/ProgressTest.kt`

**Interfaces:**
- Produces: `Stats`에 `monsterKills: Long = 0`, `bossKills: Long = 0`, `fusions: Long = 0`,
  `starAttempts: Long = 0`, `eventsSeen: Long = 0` 추가.
  `Progress.onMonsterKill(p, isBoss): ProgressState`, `Progress.onFusion(p)`,
  `Progress.onStarAttempt(p)`, `Progress.onEventSeen(p)`

- [ ] 실패하는 테스트 4건: 각 훅이 해당 카운터만 1 올린다 (보스 처치는 monsterKills 는 안 올리고 bossKills 만).
- [ ] 구현: 기존 `onSell`/`onBailout` 과 같은 꼴의 한 줄 copy 함수들.
- [ ] `./gradlew :core:test` 통과 → 커밋 `"통계 카운터 확장 - 처치·조합·별강화·이벤트"`

---

### Task 2: HuntEvents 도메인

**Files:**
- Create: `core/src/main/kotlin/com/geomgang/core/HuntEvents.kt`
- Test: `core/src/test/kotlin/com/geomgang/core/HuntEventsTest.kt`

**Interfaces:**
- Produces:

```kotlin
enum class HuntEvent(val id: String, val displayName: String, val weight: Int) {
    TREASURE("treasure", "보물 몬스터", 12),      // 5초 내 처치 = 골드 10배, 실패 = 도망
    GOLDEN_TIME("golden_time", "골든타임", 10),   // 30초간 골드·조각 2배
    MIMIC("mimic", "미믹", 12),                   // 체력 2배, 처치 시 검 드롭 확정(+1 보정)
    MERCHANT("merchant", "떠돌이 상인", 8),       // 20초간 아이템 1개 30% 할인
    ELITE("elite", "분노한 정예", 12),            // 체력 3배, 보상 5배
    GOLD_NUGGET("gold_nugget", "금덩이", 20),     // 3초 내 탭 = 즉시 골드
    METEOR_SHOWER("meteor_shower", "유성우", 1),  // 금덩이 5연속
    STRANGE_EGG("strange_egg", "수상한 알", 8),   // 처치 시 조각 잭팟 (M14에서 펫 알로 확장)
}

object HuntEvents {
    const val CHANCE = 0.08
    const val TREASURE_SECONDS = 5;  const val TREASURE_GOLD = 10.0
    const val GOLDEN_SECONDS = 30;   const val GOLDEN_MULT = 2.0
    const val MIMIC_HP = 2.0;        const val MIMIC_DROP_BONUS = 1
    const val MERCHANT_SECONDS = 20; const val MERCHANT_DISCOUNT = 0.3
    const val ELITE_HP = 3.0;        const val ELITE_REWARD = 5.0
    const val NUGGET_SECONDS = 3;    const val NUGGET_GOLD_FACTOR = 6.0
    const val METEOR_COUNT = 5
    const val EGG_SHARDS = 30

    /** chanceRoll 이 CHANCE 미만일 때만 pickRoll 로 가중치 추첨. 아니면 null. */
    fun roll(chanceRoll: Double, pickRoll: Double): HuntEvent?
    /** 0~1 롤을 가중치 누적 구간으로 변환. */
    fun pick(pickRoll: Double): HuntEvent
    /** 이벤트가 몬스터 자체를 바꾸는가(보물·미믹·정예·알). */
    fun isMonsterEvent(e: HuntEvent): Boolean
    fun hpMultOf(e: HuntEvent?): Double      // MIMIC 2.0, ELITE 3.0, 그 외 1.0
    fun rewardMultOf(e: HuntEvent?): Double  // TREASURE 10.0, ELITE 5.0, 그 외 1.0
    fun nuggetGold(zone: Zone): Long         // baseGold * 6, 최소 1
}
```

- [ ] 실패하는 테스트: 가중치 경계(0.0 → 첫 이벤트, 1.0 직전 → 마지막), `roll(0.08, x) == null`(경계), `roll(0.079..)` 은 non-null, hp·보상 배수표, `nuggetGold` 최소 1, `isMonsterEvent` 4종.
- [ ] 구현 후 `:core:test` 통과 → 커밋 `"사냥 랜덤 이벤트 8종 판정·보상 수식"`

---

### Task 3: DailyQuests 도메인 + GameState.quests

**Files:**
- Create: `core/src/main/kotlin/com/geomgang/core/DailyQuests.kt`
- Modify: `core/src/main/kotlin/com/geomgang/core/Model.kt` (GameState 에 `quests: QuestState = QuestState()`)
- Test: `core/src/test/kotlin/com/geomgang/core/DailyQuestsTest.kt`

**Interfaces:**

```kotlin
@Serializable
enum class QuestKind(val id: String, val label: String) {
    KILL("kill", "잡몹 처치"), BOSS("boss", "보스 처치"),
    FORGE("forge", "강화 시도"), FORGE_SUCCESS("forge_success", "강화 성공"),
    FUSE("fuse", "조합"), STAR("star", "별 강화"), EVENT("event", "이벤트 조우"),
}

@Serializable
data class QuestInstance(
    val kind: QuestKind, val target: Int, val baseline: Long, val claimed: Boolean = false)

@Serializable
data class QuestState(
    val dateKey: String = "", val weekKey: String = "",
    val daily: List<QuestInstance> = emptyList(), val weekly: QuestInstance? = null)

object DailyQuests {
    const val DAILY_COUNT = 3
    /** kind별 일일 목표량. KILL 60, BOSS 2, FORGE 15, FORGE_SUCCESS 8, FUSE 2, STAR 3, EVENT 3 */
    fun dailyTarget(kind: QuestKind): Int
    /** 주간은 KILL 300 고정. */
    fun weeklyQuest(stats: Stats): QuestInstance
    fun counterOf(kind: QuestKind, stats: Stats): Long   // kind → Stats 카운터
    fun progressOf(q: QuestInstance, stats: Stats): Int  // (현재-baseline).coerceIn(0, target)
    fun isDone(q, stats): Boolean
    /** 날짜가 바뀌었으면 새로 뽑는다. rng 로 서로 다른 kind 3개. 같은 날이면 그대로. */
    fun refresh(state: QuestState, stats: Stats, dateKey: String, weekKey: String, rng: Random): QuestState
    /** 보상. 골드는 목표 무게에 비례하는 고정식 - dailyGold(kind) = target * 40L * (kind 무게). */
    fun dailyGold(kind: QuestKind): Long   // KILL 1200, BOSS 1500, FORGE 1800, FORGE_SUCCESS 2000, FUSE 1600, STAR 1500, EVENT 1400
    const val DAILY_SHARDS = 15
    const val WEEKLY_SHARDS = 120        // 주간 보상 (+ M14에서 펫 알 추가)
    /** index(0..2 일일, -1 주간)를 수령 처리하고 보상이 든 새 GameState 를 돌려준다. */
    fun claim(game: GameState, stats: Stats, index: Int): GameState
}
```

- [ ] 실패하는 테스트: refresh 가 날짜 바뀌면 3개 새로 뽑고 kind 중복 없음 / 같은 날짜면 그대로 /
  주간은 weekKey 로만 갈림 / progressOf 는 baseline 차분·상한 / claim 은 완료 전 예외(check)·
  이중 수령 예외·보상 지급 / 미해금 컨텐츠 제외는 **뽑기 풀 인자**로 해결 —
  `refresh(..., pool: List<QuestKind> = QuestKind.entries)` 로 받아 STAR 미해금(+10 미만) 시 빼고 넘긴다.
- [ ] 구현 후 `:core:test` 통과 → 커밋 `"일일 퀘스트 - 카운터 차분 방식, 날짜 키는 밖에서"`

---

### Task 4: ViewModel 이벤트 배선

**Files:**
- Modify: `app/src/main/java/com/geomgang/game/ForgeViewModel.kt`
- Modify: `app/src/main/java/com/geomgang/game/HuntUiState.kt`
- Test: `app/src/test/java/com/geomgang/game/ForgeViewModelHuntTest.kt` (스크립트 순서 갱신 + 이벤트 4건 추가)

**Interfaces:**
- `HuntUiState` 추가: `event: HuntEvent?`(지금 몬스터/배너의 이벤트),
  `eventRemainingMillis: Long`, `goldenRemainingMillis: Long`,
  `nugget: Boolean`(금덩이 떠 있음), `merchantOffer: Item?`, `merchantPrice: Long`
- ViewModel 추가: `fun tapNugget()`, `fun buyMerchantOffer()`

**동작 규칙 (구현 순서대로):**
- `spawnNext()`: 희귀 롤 다음에 `HuntEvents.roll(rng.nextDouble(), if 발생 rng.nextDouble())`.
  골든타임·상인 진행 중이면 굴리지 않는다. 발생 시 `progress = Progress.onEventSeen(progress)`.
  - 몬스터 이벤트: `activeEvent` 지정, 체력에 `hpMultOf` 곱, TREASURE 는 `eventRemainingMillis = 5_000`
  - GOLDEN_TIME: `goldenRemainingMillis = 30_000` (몬스터는 평범하게 스폰)
  - MERCHANT: `merchantRemainingMillis = 20_000`, `merchantOffer = Item.entries[rng.nextInt(3)]`
  - GOLD_NUGGET/METEOR: `nuggetRemainingMillis = 3_000`, `nuggetsLeft = 1 또는 5`
- `onTargetDown()`: 보상 = 기존 × `rewardMultOf(activeEvent)` × (골든타임이면 2.0).
  MIMIC 은 `rollDrop` 을 확정 드롭으로(레벨 +1 보정), STRANGE_EGG 는 `shards += EGG_SHARDS`.
  처치 후 `activeEvent = null`.
- `startHuntLoop()` 틱: `eventRemainingMillis` 감소 — TREASURE 시간초과 = 도망(`spawnNext`),
  금덩이 시간초과 = 소멸(유성우면 다음 금덩이), 골든·상인 타이머 감소.
- `tapNugget()`: `gold += HuntEvents.nuggetGold(zone)` ×(골든 2배), 유성우면 다음 금덩이.
- `buyMerchantOffer()`: 가격 = `(Economy.itemPrice(offer) * 0.7).roundToLong()`, 1회 후 종료.

- [ ] 기존 사냥 테스트 6건의 doubles 스크립트를 새 소비 순서로 갱신 (기본값 1.0 = 이벤트 없음이라
  대부분 그대로 통과 — 치명타 테스트만 `[0.5, 1.0, 0.0]` 처럼 이벤트 롤 자리를 끼운다)
- [ ] 새 테스트 4건: ELITE 는 체력 3배 / TREASURE 시간초과에 도망(가상 시간 5초) /
  골든타임 중 보상 2배 / 금덩이 탭에 골드 증가. 전부 캡처→leaveHunt→단언.
- [ ] `:app:testDebugUnitTest` 통과 → 커밋 `"사냥 이벤트 배선 - 상태 기계는 ViewModel, 수식은 core"`

---

### Task 5: ViewModel 퀘스트 배선

**Files:**
- Modify: `app/src/main/java/com/geomgang/game/ForgeViewModel.kt`, `ForgeUiState.kt`
- Test: `app/src/test/java/com/geomgang/game/ForgeViewModelQuestTest.kt`

**Interfaces:**
- `ForgeUiState` 추가: `quests: QuestState`, `questProgress: List<Int>`(일일 3개 진행도),
  `weeklyProgress: Int`, `questClaimable: Boolean`(수령 가능한 것이 하나라도)
- ViewModel: `fun claimQuest(index: Int)` (-1 = 주간), `private fun refreshQuests()` —
  init·`enterZone`·`claimQuest` 에서 호출. 날짜 키는 `java.util.Calendar` 로
  `yyyyMMdd` / 주간 키는 `yyyy-w주차` 문자열. STAR 는 `bestLevel >= 10` 일 때만 풀에 넣는다.
- 처치·조합·별강화 지점에 Task 1 훅 호출 추가:
  `onTargetDown` → `Progress.onMonsterKill(progress, isBoss)`,
  `fuse()` → `onFusion`, `starUp()` → `onStarAttempt`.

- [ ] 테스트: 첫 로드에 일일 3개가 배정된다 / 처치가 KILL 진행도를 올린다 /
  완료 후 claim 으로 골드·조각이 들어오고 claimed 표시 / 이중 수령 불가.
- [ ] `:app:testDebugUnitTest` 통과 → 커밋 `"일일 퀘스트 배선 - 카운터 훅과 수령"`

---

### Task 6: UI — 이벤트 연출 + 퀘스트 화면

**Files:**
- Modify: `app/src/main/java/com/geomgang/game/ui/HuntScreen.kt`
- Create: `app/src/main/java/com/geomgang/game/ui/QuestScreen.kt`
- Modify: `app/src/main/java/com/geomgang/game/ui/ForgeScreen.kt`, `MainActivity.kt`

**구현 내용:**
- HuntScreen: 이벤트 배너(이벤트명 + 남은 초, 금색 테두리) / 보물·정예·미믹·알은 몬스터 이름 옆
  표식(⚡·💰 대신 텍스트 「보물」「정예」「미믹」「알」 배지) / 골든타임 중 상단에 "골드·조각 2배" 띠 /
  금덩이는 화면 하단에 큰 노란 버튼("금덩이! 탭!")으로 — `onTapNugget` / 상인 배너에 구매 버튼
- QuestScreen: 일일 3줄 + 주간 1줄. 각 줄 = 이름 · 진행도 바 · `progress/target` · 수령 버튼
- ForgeScreen: 도감 버튼 옆에 "퀘스트" 버튼, `questClaimable` 이면 「!」 배지
- MainActivity: `Overlay.Quests` 추가, ForgeScreen/RecordsMenu 양쪽 진입
- [ ] `:app:assembleDebug` 성공 → 커밋 `"이벤트 연출·퀘스트 화면"`

---

### Task 7: 마무리

- [ ] 전체 테스트 + APK. README 개발 일지 v1.3.0-M12 절(이벤트 표·퀘스트 규칙·테스트 수).
- [ ] 커밋 `"M12 마무리 - 개발 일지"` → push. 폰 연결돼 있으면 설치.
