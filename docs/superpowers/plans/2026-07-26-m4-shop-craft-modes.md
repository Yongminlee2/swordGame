# M4: 상점 · 조합소 · 모드 4종 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 경제 루프를 닫는다. 검을 팔아 번 돈으로 아이템을 사고, 주운 조각을 바꾸고, 그 아이템을 실제로 강화에 쓴다. 모드 4종을 각각 독립 세이브로 오간다.

**Architecture:** 화면은 넷(모드 선택 · 강화 · 상점 · 조합소)이지만 상태는 하나다. 모드를 고르면 그 모드의 `ForgeViewModel`이 만들어지고, 상점·조합소는 같은 ViewModel을 조작한다. 내비게이션 라이브러리를 도입하지 않는다 — 화면이 넷이고 딥링크가 없어 `mutableStateOf`로 충분하다.

**참조:** [스펙](../specs/2026-07-26-sword-enhance-game-design.md) · [M3 계획](2026-07-26-m3-timers-and-effects.md)

## Global Constraints

M1~M3의 제약이 유효하다. 여기에 더해:

- **상태의 단일 출처는 `ForgeViewModel`이다.** 상점·조합소가 자기 상태를 따로 들지 않는다.
- **모드를 바꾸면 ViewModel을 새로 만든다.** 모드별 세이브가 완전히 분리되어야 한다.
- **모드 초기화는 도감·업적·통계를 건드리지 않는다.** `SaveStore.resetGame`이 이미 그렇게 동작하며, M1 테스트가 지키고 있다.
- **가격·교환식을 UI에 다시 쓰지 않는다.** `Economy`·`Recipes`에서 읽는다.
- 브랜치는 `m4-shop-craft-modes`.

---

### Task 1: ViewModel 확장 — 상점·조합소·아이템 사용

**Files:**
- Modify: `app/src/main/java/com/geomgang/game/ForgeUiState.kt`, `ForgeViewModel.kt`
- Test: `app/src/test/java/com/geomgang/game/ForgeViewModelEconomyTest.kt` (신규)

**Produces:**
- `ForgeUiState` 추가 필드: `blessingScrolls`, `luckCharms`, `unlockedFamilies: List<WeaponFamily>`, `useBlessing: Boolean`, `useLuckCharm: Boolean`
- `ForgeViewModel` 추가: `buyItem(item)`, `craft(recipeId, family)`, `toggleBlessing()`, `toggleLuckCharm()`
- `forge()`가 토글된 아이템을 실제로 사용

**중요** — M3까지 `forge()`는 `UsedItems.NONE`으로만 강화했다. 상점에서 축복서를 팔면서
쓸 방법이 없으면 경제 루프가 닫히지 않는다. 여기서 배선한다.
아이템 토글은 **한 번 쓰면 자동으로 꺼진다.** 켜 둔 채 잊고 연타하면 아이템이 순식간에 녹는다.

- [ ] **Step 1: 실패하는 테스트를 작성한다** — 아래 케이스를 담는다.

```
- 아이템을 사면 골드가 줄고 보유량이 는다
- 골드가 모자라면 사지지 않는다
- 조각을 바꾸면 조각이 줄고 아이템이 생긴다
- 조각으로 +5 검을 바꾸면 검이 생긴다
- 검이 있으면 검 교환이 막힌다
- 축복서 토글을 켜고 강화하면 축복서가 소모된다
- 축복서 토글은 한 번 쓰면 꺼진다
- 축복서가 없으면 토글이 켜지지 않는다
- 행운부적 토글도 같은 규칙을 따른다
- 처음에는 기본 계열 4종만 고를 수 있다
```

- [ ] **Step 2: 실패 확인** → **Step 3: 구현** → **Step 4: 통과 확인** → **Step 5: 커밋**

---

### Task 2: 상점 화면

**Files:** `app/src/main/java/com/geomgang/game/ui/ShopScreen.kt` (신규)

검 판매 · 기본 검 구매(계열 선택) · 아이템 3종 구매를 한 화면에.
가격은 `Economy`에서 읽고 화면에 숫자를 다시 쓰지 않는다.
살 수 없는 항목은 비활성으로 두되 **이유를 함께 보여 준다**(골드 부족 / 검을 이미 들고 있음).

- [ ] **Step 1: 작성** → **Step 2: 빌드** → **Step 3: 커밋**

---

### Task 3: 조합소 화면

**Files:** `app/src/main/java/com/geomgang/game/ui/CraftScreen.kt` (신규)

`Recipes.ALL`을 그대로 훑어 목록을 만든다. 교환식이 늘어도 화면은 그대로다.
검을 주는 교환은 계열 선택이 필요하다.

- [ ] **Step 1: 작성** → **Step 2: 빌드** → **Step 3: 커밋**

---

### Task 4: 모드 선택과 화면 전환

**Files:**
- `app/src/main/java/com/geomgang/game/ui/ModeSelectScreen.kt` (신규)
- `app/src/main/java/com/geomgang/game/ui/HoldToReset.kt` (신규)
- Modify: `MainActivity.kt`, `ForgeScreen.kt` (상점·조합소 가는 버튼)

**모드 선택 화면**
모드 4종 카드. 각 카드에 그 모드의 최고 기록과 보유 골드를 `SaveStore.loadGame(d)`로 읽어 보여 준다.

**5초 롱프레스 초기화** — 원작의 조작을 그대로 가져온다.
`pointerInput`으로 누른 시간을 재고 진행 막대를 채운다. 5초를 채우면 `resetGame(difficulty)`.
손을 떼면 즉시 취소된다. 실수로 지워지지 않게 하려는 장치이므로 **짧게 만들지 않는다.**

**화면 전환**
`Route` sealed interface(`ModeSelect` / `Forge` / `Shop` / `Craft`)와 `mutableStateOf`.
모드가 정해지면 `remember(difficulty) { ForgeViewModel(...) }`로 ViewModel을 만들고
상점·조합소가 같은 인스턴스를 쓴다.

- [ ] **Step 1~4: 작성 · 빌드 · 뒤로가기 처리 · 커밋**

---

### Task 5: 마무리

- [ ] 전체 검증 (`:core:test`, `:app:testDebugUnitTest`, `:app:assembleDebug`)
- [ ] README 개발 일지 v0.5.0
- [ ] main 병합·푸시

## 완료 기준

- [ ] 모드 4종을 오갈 수 있고 각각 세이브가 분리된다
- [ ] 5초 롱프레스로 모드가 초기화되고 도감·업적은 남는다
- [ ] 상점에서 산 축복서·행운부적을 강화에 실제로 쓸 수 있다
- [ ] 아이템 토글은 한 번 쓰면 꺼진다
- [ ] 가격·교환식이 UI에 중복 정의되어 있지 않다
- [ ] 테스트 전부 통과, README v0.5.0 기록, `main` 푸시
