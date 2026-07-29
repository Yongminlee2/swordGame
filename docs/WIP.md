# 진행 중 — 강화 성장 축과 전설검 등급

이 문서는 **작업이 끝나면 지운다.** 지금 어디까지 됐고 다음에 무엇을 하면 되는지만 적는다.

## 읽을 것

| 문서 | 무엇 |
|---|---|
| [설계](specs/2026-07-29-forge-growth-and-legend-design.md) | 왜 이렇게 만드는지 |
| [계획](plans/2026-07-29-forge-growth-and-legend.md) | 작업 11개. **붙일 코드와 테스트가 그대로 적혀 있다** |

## 끝난 것

- [x] **1. ForgeBonus 합산 자리** — `core/.../ForgeBonus.kt`
  `RateTable.successRate(..., bonus)` 와 `ForgeEngine.attempt(..., bonus)` 에 연결.
  파괴 판정 뒤 `destroyGuard` 굴림이 하나 더 있다(방지권보다 먼저, 소모품 아님).
- [x] **2. 도감 수집** — `core/.../CodexOffer.kt`
  자동 등록을 끊었다. `Progress.onAttempt` 와 뷰모델의 `registerSword` 호출을 전부 지웠다.
  10칸마다 +0.1%p, 최대 +3.2%p.
- [x] **3. 대장간 스킬** — `core/.../Smithy.kt`, `ProgressState.smithyLevel`
  골드 15레벨, 레벨당 +0.2%p. 값은 `Economy.upgradeCost(bestLevel) × 5 × 1.5^level`.
- [x] **4. 고유검 보유** — `UniqueSwords.holdingBonus`, `PER_UNIQUE = 0.003`

**테스트 전부 통과.** 특히 `BalanceSimulationTest` 가 그대로 선다 — 새 세이브는 세 출처가
전부 0이라 시뮬레이터가 보는 "맨손 신규 플레이어" 가 바뀌지 않았다는 증거다.

## 남은 것

- [ ] **5. `FamilyForge`** — 계열 15종 강화 특성 표 (계획서 Task 5)
- [ ] **6. 특성을 판정에 물리기** — `ForgeEngine`·`ForgeCost`·`Tempering` (Task 6)
- [ ] **7. 허검 완화** — 도끼검 + 창검 조합 (Task 7)
- [ ] **8. 전설검 등급** — +20 상한, 조합, 파괴 시 +21 복귀, 전설 해금 (Task 8)
- [ ] **9. 뷰모델 배선** (Task 9)
- [ ] **10. 화면** — 확률 내역, 바치기 버튼, 대장간, 조합소 전설 칸 (Task 10)
- [ ] **11. 실기기 확인 + 이 문서 삭제** (Task 11)

## 지금 상태의 주의점

**아직 화면에 아무 버튼도 없다.** 도메인만 섰다. 지금 설치하면 **도감이 안 채워지는 것만**
체감된다 — 자동 등록을 끊었는데 바치기 버튼이 아직 없기 때문이다. **작업 10까지 가야
실제로 쓸 수 있다.**

## 이어서 할 때

```
docs/plans/2026-07-29-forge-growth-and-legend.md 의 Task 5부터 이어서 해줘
```

계획서의 각 작업에 붙일 코드가 그대로 적혀 있어 앞 대화 없이도 이어진다.

## 지켜야 할 선

- `BalanceSimulationTest` 와 `BossTempoTest` 를 손대지 않는다. 깨지면 보너스가
  시뮬레이터에 새어 들어갔다는 뜻이다.
- 전설검은 **전투 배수를 건드리지 않는다.** 사냥은 쉬워지지 않는다.
- 새 저장 필드는 전부 기본값을 가진다.
