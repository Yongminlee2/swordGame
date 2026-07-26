# M13 고유검 + 조합·회랑 전용 계열 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 숨겨진 레시피 조합으로 나오는 고유검 10종(고유 이름·패시브·도감 페이지)과 조합·회랑 전용 계열 2종(합검·허검)을 넣어 조합에 꿈을 만든다.

**Architecture:** 레시피 매칭·패시브 수식은 `:core` `UniqueSwords`. 패시브는 Combat·ForgeEngine·드롭·보상 계산이 `UniqueSwords.xxxOf(sword)` 를 물어보는 형태 — 검이 곧 상태이므로 별도 버프 저장이 없다. `Sword.uniqueId`, `GameState.essences` 는 기본값이라 세이브 호환.

## Global Constraints

- M11·M12 계획의 Global Constraints 전부 그대로
- `Sword(family, level, stars, uniqueId=null)` — 새 필드는 마지막에, 기본값 필수
- 고유검·특수 계열은 **상점 구매·드롭 불가**. 획득 경로는 조합(합검·고유검)과 회랑(허검, M15)뿐
- 허검의 회랑 해금은 M15에서 배선한다. M13에서는 계열·전투 특성·도감 칸까지만

---

### Task 1: 계열 2종 추가 — 합검(FUSED)·허검(VOID)

**Files:** `core/.../Model.kt`(WeaponFamily), `core/.../Combat.kt`(FamilyStyle), `core/.../Progress.kt`(unlockedFamilies 제외 규칙), 관련 테스트

**Interfaces:**
- `WeaponFamily.FUSED("fused","합검")`, `WeaponFamily.VOID("void","허검")`
- `FamilyStyle.OMNI(damage 0.9, hits 1, minTap 150, comboGain 0.02, bossBonus 1.25, shardBonus 1.25, burnRatio 0.0)` ← 합검: 두루 좋음
- `FamilyStyle.HOLLOW(damage 0.5, ..., maxHpRatio 0.015)` ← 허검: 한 방은 약하나 적 최대체력 1.5% 추가
- `FamilyStyle` 에 `maxHpRatio: Double = 0.0` 파라미터 추가(기존 12종은 기본값)
- `Combat.hit(sword, combo, isBoss, critRoll, targetMaxHp: Long = 0)` — 피해에 `maxHp * maxHpRatio` 가산
- `Progress.unlockedFamilies` 는 FUSED·VOID 를 **항상 제외** (드롭·상점 풀에서 빠짐).
  대신 `WeaponFamily.SPECIAL = setOf(FUSED, VOID)` 상수
- 도감: WeaponCatalog ENTRIES 14 × 11 = **154** (자동으로 따라오는지 테스트 수정)

- [ ] 테스트: 허검 피해가 targetMaxHp 에 비례해 커진다 / 합검 특성 (보스·조각 1.25) /
  unlockedFamilies 에 FUSED·VOID 없음 / 도감 154칸 / 스프라이트 스트라이드 7과 30 서로소 유지
- [ ] 구현·통과 → 커밋 `"합검·허검 계열 - 조합·회랑 전용, 도감 154칸"`

### Task 2: Sword.uniqueId + 정수(essence)

**Files:** `core/.../Model.kt`, `core/.../SaveStoreTest.kt`(호환 테스트 추가)

- `Sword`에 `val uniqueId: String? = null`
- `GameState`에 `val essences: Map<String, Int> = emptyMap()` (Zone id → 개수)
- 보스 처치 시 해당 구역 정수 +1 (ViewModel, Task 5)
- [ ] 테스트: uniqueId 없는 옛 JSON 이 그대로 열린다(SaveStore 왕복) → 커밋

### Task 3: UniqueSwords 도메인 — 레시피 10종 + 패시브

**Files:** Create `core/.../UniqueSwords.kt`, Test `core/.../UniqueSwordsTest.kt`

```kotlin
data class UniqueRecipe(
    val id: String, val name: String, val hint: String,
    val resultFamily: WeaponFamily,
    /** 재료 조건: (계열, 최소 단계, 자루 수) 목록. null 계열 = 아무 검. */
    val needs: List<Triple<WeaponFamily?, Int, Int>>,
    /** 구역 정수 요구: Zone id → 개수. */
    val essences: Map<String, Int> = emptyMap(),
)
object UniqueSwords {
    val RECIPES: List<UniqueRecipe> // 10종 - 스펙 표 그대로
    fun match(materials: List<Sword>, essences: Map<String, Int>): UniqueRecipe?
    fun of(sword: Sword?): UniqueRecipe?      // uniqueId → 레시피(정의)
    // 패시브 - 없으면 중립값
    fun bossBonusOf(sword: Sword?): Double     // 삼위일체 1.4
    fun shardMultOf(sword: Sword?): Double     // 탐식자 2.0
    fun burnMultOf(sword: Sword?): Double      // 용왕의 송곳니 3.0
    fun dropMultOf(sword: Sword?): Double      // 행운아 2.0
    fun critBonusOf(sword: Sword?): Double     // 절단자 +0.10
    fun tapIntervalMultOf(sword: Sword?): Double // 폭풍우 0.7
    fun maxHpRatioOf(sword: Sword?): Double    // 심연을 삼킨 검 +0.02
    fun goldMultOf(sword: Sword?): Double      // 개화 1.5
    fun forgeBonusOf(sword: Sword?): Double    // 시작의 검 +0.03
    fun canRevive(sword: Sword?): Boolean      // 불사조
    const val REVIVE_LEVEL_LOSS = 3
}
```

레시피 표 (스펙 v1.3 §2 그대로): 삼위일체(성검×3 +10↑) · 탐식자(마검×2+아무 검×2) ·
용왕의 송곳니(용검×2 +15↑ + 용의 둥지 정수 3) · 행운아(곡도+세검+쌍검+창검) ·
불사조(성검+마검 +12↑ + 화산 정수 3) · 절단자(대검+도끼검 +14↑) · 폭풍우(세검×3 + 설원 정수 2) ·
심연을 삼킨 검(마검×3 +16↑ + 심연 정수 5) · 개화(정령검+아무 검×3) · 시작의 검(직검×4)

- [ ] 테스트: 각 레시피가 맞는 재료에 매칭 / 정수 부족이면 불발 / 아무 검 조건은 계열 무관 /
  구체 조건 레시피가 "아무 검" 레시피보다 우선(탐식자 재료가 개화로 새지 않게 정의 순서 = 우선순위) /
  패시브 수치 표 / 중립값 1.0·0.0·false
- [ ] 커밋 `"고유검 10종 - 숨은 레시피와 패시브"`

### Task 4: 조합·전투·강화에 패시브 배선

**Files:** `core/.../Fusion.kt`, `core/.../Combat.kt`, `core/.../ForgeEngine.kt`, `core/.../Storage.kt`(scrap 고유검 보너스는 없음), 테스트

- `Fusion.resultOf/fuse` 앞단에서 `UniqueSwords.match` — 맞으면 고유검
  `Sword(recipe.resultFamily, best.level, 0, recipe.id)` + 정수 차감.
  `canFuse` 는 **고유검을 재료로 거부**
- `Combat.attackPower` 변화 없음. `Combat.hit` 에 `UniqueSwords` 보정:
  bossBonus × bossBonusOf, crit 판정 `critRoll < CRIT_CHANCE + critBonusOf`,
  maxHpRatio 합산. `Combat.burnPerSecond` × burnMultOf.
  `Combat.minTapMillis` × tapIntervalMultOf. `Combat.shardReward` × shardMultOf
- `ForgeEngine.attempt` 의 successRate 에 `UniqueSwords.forgeBonusOf(state.sword)` 가산.
  파괴 판정 시 `canRevive` 면 파괴 대신 `level - REVIVE_LEVEL_LOSS` 로 생존하고
  **uniqueId 를 잃는다**(불사조는 한 번 타오르고 재가 된다) — `ForgeResult.Stay` 로 취급
- [ ] 테스트: 조합이 레시피와 맞으면 고유검 / 고유검 재료 거부 / 정수 차감 /
  불사조 파괴가 -3단계 생존 + uniqueId 소멸 / 시작의 검 성공률 가산 / 절단자 치명타 경계
- [ ] 커밋 `"고유검 패시브 배선 - 전투·강화·조합"`

### Task 5: ViewModel·UI

**Files:** `app/.../ForgeViewModel.kt`, `ForgeUiState.kt`, `ui/StorageScreen.kt`, `ui/CodexScreen.kt`, `ui/SwordView.kt`, `ui/ForgeScreen.kt`, `MainActivity.kt`

- 보스 처치 → `essences[zone.id] += 1`. 드롭률 × `dropMultOf`, 골드 × `goldMultOf`
- `tapTarget` 의 `Combat.hit(..., targetMaxHp = targetMaxHp)`
- 고유검 발견 시 `ProgressState`… 도감은 `uniqueFound: Set<String>` 추가(기본값) +
  `Progress.onUniqueFound`
- StorageScreen 조합 미리보기: 레시피 매칭이면 고유검 이름을 금색으로 + 정수 조건 표시
- SwordView: `uniqueId != null` 이면 금색 얇은 틴트 오버레이(고유검 전용 시트는 만들지 않는다)
- CodexScreen: 고유검 페이지 — 발견 = 이름·패시브, 미발견 = "???" + 힌트 한 줄
- ForgeScreen: 검 이름 옆 고유검이면 이름을 금색으로
- [ ] 테스트(ViewModel): 보스 처치가 정수를 준다 / 조합으로 고유검이 나오고 도감에 등록된다
- [ ] 커밋 `"고유검 UI - 도감 페이지·조합 미리보기·정수"`

### Task 6: 마무리

- [ ] 전체 테스트 + APK + README 일지(v1.3.0-M13) + 커밋·푸쉬
