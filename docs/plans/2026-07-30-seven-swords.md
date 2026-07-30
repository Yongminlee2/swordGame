# v2.1 「일곱 자루」 구현 계획

> 스펙: docs/specs/2026-07-30-seven-swords-design.md
> 각 작업 끝에 관련 테스트를 돌리고, 전체 통과 후 커밋한다.

## Task 1: `WeaponFamily.VISIBLE` + 노출 필터

- `core/.../Model.kt` WeaponFamily companion에 `VISIBLE: List<WeaponFamily>` =
  [STRAIGHT, CURVED, GREAT, RAPIER, DEMON, HOLY, DRAGON].
- `WeaponCatalog.ENTRIES` = VISIBLE에서 DRAGON 제외한 6계열×21 + 전설 30 = 156.
- `CodexOffer.STEP_BONUS` 0.001→0.002.
- 테스트: `SevenSwordsTest` — ENTRIES 156, 숨긴 계열 칸 없음, 보너스 만점 ≈3.0%p.

## Task 2: 조합 2자루 + 표 축소

- `Fusion`: MIN=MAX=2, 결과 단계 = (a+b)/2 내림, 같은 계열 보너스·다수결 삭제.
  표에 없으면 preview null·canFuse false.
- `FusionTable.ALL` = {직검+곡도=마검, 대검+세검=성검} 둘뿐.
- 세검 해금 `RAPIER_UNLOCK_FUSIONS` 3→1.
- 기존 FusionTable/VoidFusion/CraftMany 테스트 재작성.

## Task 3: 전설 = 용검

- `LegendForge.MATERIALS` = [DEMON, HOLY]. 나머지 로직 그대로.
- 전설 표기 "전설검"→"용검" (LegendPanel·도감 구획·FamilyForge.LEGEND blurb).
- `LegendForgeTest` 재료 2종으로 수정.

## Task 4: 고유검 6종 · 2자루 레시피 + 정수 표기

- RECIPES: dragon_fang·cleaver·lucky·bloom 제거(코드 잔류, VISIBLE 필터로 숨김이 아니라
  RECIPES 목록 자체를 6종으로 — 되살릴 때 주석 해제). origin 직검2, tempest 세검2,
  abyss_eater 마검+16 2, trinity 성검+10 2, glutton 마검2, phoenix 유지.
- `PER_UNIQUE` 0.003→0.005.
- CraftScreen에 조합법+고유검 레시피(정수 포함) 목록 컴포저블.

## Task 5: 판정 — 동시 아이템 + 확률 상향

- UsedItems 배타 제거(ForgeViewModel 토글·ForgeOdds·RateTable 경로 점검).
- RateTable 1~20 상향(예: 낙폭 완만화), BalanceSimulationTest 구간 재조정,
  BossTempo/ForgeTempo/TemperTempo 영향 확인.

## Task 6: 화면 — 퀘스트 숨김·아이콘 1줄·회랑 이동·자취 삭제

- ForgeScreen: 퀘스트 아이콘 제거, 한 줄 5개(상점·조합소·가방·단련·도감),
  MarkStrip·recentMarks 제거(UiState·VM 기록 중단).
- HuntScreen: 회랑 입장 버튼 추가, ForgeScreen의 회랑 버튼 제거.
- 도움말·업적·통계 화면 VISIBLE 필터.

## Task 7: 마무리

- 전체 테스트, README 갱신, 커밋·푸시, 실기기 설치.
