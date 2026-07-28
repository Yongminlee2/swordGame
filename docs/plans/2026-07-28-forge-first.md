# v1.7 「강화가 주인공」 구현 계획

**목표:** 강화 한 번을 굴리기 위한 준비 시간을 줄여, 사냥이 아니라 강화가 주 활동이 되게 한다.

**접근:** 공급을 늘리고 요구를 낮추는 것이 먼저다(1단계). 그것만으로 목표가 달성되는지를
`ForgeTempoTest` 로 먼저 확인한 뒤, 남는 골드를 재료로 바꾸는 길(2~3단계)과
장기 목표(4~5단계)를 얹는다. 순서를 뒤집으면 상점이 밸런스를 가리게 된다.

**스택:** Kotlin 2.4 / AGP 9.2.1, `:core` 순수 Kotlin + `:app` Compose

## 전역 제약

- `:core` 는 순수 Kotlin — 안드로이드 의존 금지
- 세이브에 붙는 새 필드는 **전부 기본값**을 가진다(옛 세이브 무손실)
- 성공률·파괴 확률·보스 제한시간은 **팔지 않는다**
- 난수를 쓰는 도메인 함수는 `Random` 이 아니라 **난수 값**을 받는다(결정적 테스트)
- 커밋·문서에 AI 언급 금지
- 화면 문구는 한국어

---

### Task 1: 공급·요구 재조정 + 템포 테스트

**파일**
- 수정: `core/.../ForgeCost.kt` (요구량 곡선, `MAX_STONES`)
- 수정: `core/.../Zones.kt` (`bossStones` 2배)
- 수정: `core/.../IdleRewards.kt` (`STONES_PER_HOUR` 1 → 3)
- 수정: `app/.../ForgeViewModel.kt` (`MOB_STONE_CHANCE` 0.05 → 0.15)
- 생성: `core/src/test/.../ForgeTempoTest.kt`
- 수정: `core/src/test/.../ForgeCostTest.kt` (기대값 갱신)

**인터페이스**
- 산출: `ForgeCost.requirementFor(level)` 의 `stones` 곡선이 바뀐다. 상한 `MAX_STONES = 15`.
- 산출: `ForgeCost.MOB_STONE_CHANCE` — ViewModel 에 있던 상수를 도메인으로 옮겨
  테스트가 공급을 계산할 수 있게 한다.

- [ ] **1-1 실패하는 템포 테스트를 쓴다**

```kotlin
class ForgeTempoTest {
    /** 구역 완주 한 번(잡몹 12 + 보스)으로 얻는 강화석 기대값. */
    private fun stonesPerRun(zone: Zone): Double =
        Zone.MONSTERS_BEFORE_BOSS * ForgeCost.MOB_STONE_CHANCE + zone.bossStones

    @Test
    fun `구역 하나를 돌면 그 단계 강화를 다섯 번은 굴릴 수 있다`() {
        for (zone in Zone.entries) {
            val need = ForgeCost.requirementFor(zone.recommendedLevel).stones
            if (need == 0) continue
            val runs = stonesPerRun(zone) / need
            assertTrue(
                "${zone.displayName}: 완주 1회로 ${"%.1f".format(runs)}번뿐",
                runs >= ForgeCost.RUNS_PER_ZONE_CLEAR,
            )
        }
    }
}
```

- [ ] **1-2 실패를 확인한다**

`.\gradlew.bat :core:test --tests "*ForgeTempoTest*"` → FAIL

- [ ] **1-3 요구 곡선과 상한을 고친다**

```kotlin
const val MAX_STONES = 15
const val RUNS_PER_ZONE_CLEAR = 5.0
const val MOB_STONE_CHANCE = 0.15

val stones = when {
    target < STONE_BAND_START -> 0
    target < ENDLESS_BAND_START -> 3 + (target - STONE_BAND_START) / 2
    else -> (5 + (target - 20) / 2).coerceAtMost(MAX_STONES)
}
```

- [ ] **1-4 보스 강화석을 2배로, 자리비움을 시간당 3개로 올린다**

- [ ] **1-5 ViewModel 이 `ForgeCost.MOB_STONE_CHANCE` 를 쓰게 바꾼다**

- [ ] **1-6 전체 테스트 → 통과 확인, 기대값이 바뀐 기존 테스트 갱신**

- [ ] **1-7 커밋** `강화 준비 시간 단축: 공급 확대 + 요구 완화 + 템포 테스트`

---

### Task 2: GoldShop 가격 뼈대 + 세이브 필드

**파일**
- 생성: `core/.../GoldShop.kt`
- 수정: `core/.../Model.kt` (`stonesBought`, `swordsBought`, `priceBandLevel`, `upgrades`)
- 수정: `core/.../Economy.kt` (고정 가격 상수를 `GoldShop` 으로 위임)
- 생성: `core/src/test/.../GoldShopTest.kt`

**인터페이스**
- 산출: `GoldShop.stonePrice(state): Long`, `materialSwordPrice(state): Long`,
  `itemPrice(state, item): Long`, `buyStone(state): GameState`,
  `rebase(state): GameState`, `canBuyStone(state): Boolean`

- [ ] **2-1 테스트를 먼저 쓴다** — 누진(둘째가 첫째보다 비싸다), 리셋(최고 단계가
  오르면 첫 가격으로 돌아온다), 아이템 하한(초반 값이 지금과 같다)
- [ ] **2-2 실패 확인**
- [ ] **2-3 `GoldShop` 과 세이브 필드를 만든다**

```kotlin
object GoldShop {
    const val GROWTH = 1.18
    const val STONE_MULT = 0.8
    const val SWORD_MULT = 0.5

    fun stonePrice(state: GameState): Long =
        price(state, STONE_MULT, state.stonesBought)

    private fun price(state: GameState, mult: Double, bought: Int): Long {
        val base = Economy.upgradeCost(state.bestLevel) * mult
        return (base * GROWTH.pow(bought.toDouble())).roundToLong().coerceAtLeast(1)
    }

    /** 최고 단계가 오르면 누진을 푼다. 부르는 곳은 강화 성공 한 군데뿐이다. */
    fun rebase(state: GameState): GameState =
        if (state.bestLevel > state.priceBandLevel) {
            state.copy(stonesBought = 0, swordsBought = 0, priceBandLevel = state.bestLevel)
        } else {
            state
        }
}
```

- [ ] **2-4 통과 확인 → 커밋** `골드로 재료를 사는 길(누진 가격 + 단계별 리셋)`

---

### Task 3: 상점 탭 4개 + 재료 탭 + ViewModel 배선

**파일**
- 수정: `app/.../ui/ShopScreen.kt` (탭 구조)
- 수정: `app/.../ForgeViewModel.kt` (`buyStone()`, `rebase` 호출, 새 UI 필드)
- 수정: `app/.../ForgeUiState.kt`
- 수정: `app/.../MainActivity.kt`
- 생성: `app/src/test/.../ForgeViewModelShopTest.kt`

- [ ] **3-1 ViewModel 테스트** — 강화석을 사면 개수가 늘고 골드가 줄며 다음 가격이 오른다
- [ ] **3-2 배선**: `runAttempt` 의 성공 처리 뒤에 `game = GoldShop.rebase(game)`
- [ ] **3-3 상점 탭 UI** — 검 / 아이템 / 재료 / 대장간, 재료 탭에 다음 가격과
  "한 단계 올리면 값이 되돌아온다" 안내
- [ ] **3-4 테스트·빌드 → 커밋** `상점 탭 4개 + 강화석 구매`

---

### Task 4: 대장간 영구 업그레이드

**파일**
- 생성: `core/.../Upgrades.kt`
- 생성: `core/src/test/.../UpgradesTest.kt`
- 수정: 효과를 읽는 곳 — `Storage.CAPACITY`(가방), 사냥 골드·조각, 강화석 확률, 자리비움 상한
- 수정: `app/.../ui/ShopScreen.kt` (대장간 탭)

**인터페이스**
- 산출: `Upgrades.levelOf(state, kind): Int`, `Upgrades.price(state, kind): Long`,
  `Upgrades.buy(state, kind): GameState`, `Upgrades.MAX_LEVEL = 5`
- 산출: `Upgrades.bagSlots(state)`, `goldMult(state)`, `shardMult(state)`,
  `stoneChanceBonus(state)`, `idleHours(state)`

- [ ] **4-1 테스트** — 상한 5, 가격 3배씩, 효과 값
- [ ] **4-2 구현 + 효과를 쓰는 곳 배선**
- [ ] **4-3 대장간 탭 UI**
- [ ] **4-4 커밋** `대장간 영구 업그레이드 5종`

---

### Task 5: 수상한 상자

**파일**
- 생성: `core/.../MysteryBox.kt` (난수 **값**을 받는 순수 함수)
- 생성: `core/src/test/.../MysteryBoxTest.kt`
- 수정: `app/.../ui/ShopScreen.kt` (아이템 탭), `ForgeViewModel.kt`

- [ ] **5-1 테스트** — 확률 합이 1.0, 경계값(0.0/0.39/0.4/0.99)별 결과가 고정
- [ ] **5-2 구현 + 배선 + UI**
- [ ] **5-3 커밋** `수상한 상자`

---

### Task 6: 강화 화면 상단 지갑 줄

**파일**
- 수정: `app/.../ui/ForgeScreen.kt`

- [ ] **6-1 검 그림 위, 제목 줄 바로 아래에 `WalletBar` 를 올린다**
- [ ] **6-2 중간에 있던 자원 두 줄 중 지갑과 겹치는 것(골드·조각·강화석·방지권)을 지운다.
      성공률·비용·판매가 줄은 남긴다 — 그건 지갑이 아니라 이번 강화의 정보다**
- [ ] **6-3 빌드 → 설치 → 커밋** `강화 화면 상단 지갑 줄`

---

## 마무리

- 전체 테스트 통과 확인
- `README.md` 개발 일지에 v1.7 절 추가
- APK 빌드 → 기기 설치
