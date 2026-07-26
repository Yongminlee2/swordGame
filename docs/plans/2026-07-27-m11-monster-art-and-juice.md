# M11 몬스터 아트 + 타격감 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 색 사각형뿐인 몬스터·보스 40종에 픽셀아트 스프라이트를 입히고, 데미지 팝업·치명타·처치 연출·구역 분위기로 사냥의 타격감을 만든다.

**Architecture:** 검 시트와 같은 구조 — PNG 시트 한 장 + 코드 좌표 매핑 + 보간 끈 서브렉트 렌더링. 치명타 판정은 `:core`의 순수 함수(난수 값 주입), 연출 상태(`hitSeq`)는 ViewModel이 세고 화면은 그리기만 한다.

**Tech Stack:** Kotlin, Jetpack Compose, `BitmapFactory(inScaled=false)`, `FilterQuality.None`, JUnit.

## Global Constraints

- AGP 9.2.1 내장 Kotlin — `org.jetbrains.kotlin.android` 플러그인을 **절대 추가하지 않는다**
- `gradle.properties`의 `-Dfile.encoding=MS949`·`org.gradle.java.home`을 건드리지 않는다 (한글 경로 워커 크래시 방지)
- `:core`는 순수 Kotlin — 안드로이드 의존성 금지
- 커밋 메시지·문서에 AI 언급 금지, UI 문구는 한국어
- 아트는 CC0/CC-BY만. 출처는 설정 → 라이선스 화면과 README에 기록
- 스프라이트 그리기: `inScaled=false` 로드 + `FilterQuality.None` (보간 켜면 픽셀아트가 뭉개진다)

---

### Task 1: 몬스터 스프라이트시트 조달·합성

**Files:**
- Create: `app/src/main/res/drawable/monster_sheet.png` (256×224, 32px 칸 8열×7행 = 56칸 중 40칸 사용)
- Create: `tools/make_monster_sheet.ps1` (재현 가능하게 커밋)

**Interfaces:**
- Produces: `monster_sheet.png` — 칸 번호 = `row * 8 + col`. 0~29 잡몹(구역 순서×3), 30~39 보스(구역 순서). 40~55 예약(M14 펫 10칸 + 여유)

- [ ] **Step 1: CC0/CC-BY 몬스터 팩 검색·라이선스 확인**

itch.io / OpenGameArt에서 "16x16 monster pack CC0", "32x32 creature pack" 등으로 검색.
조건: 서로 구분되는 생물 40종 이상, 16 또는 32px, 라이선스 명시(CC0 우선, CC-BY 허용).
**에셋 페이지의 라이선스 문구를 직접 확인**하고 URL·저작자·라이선스를 기록해 둔다.
후보가 여럿이면 지금 검 시트(16비트 톤)와 색감이 맞는 쪽을 고른다.

- [ ] **Step 2: 다운로드·압축 해제**

```powershell
$dl = "$env:TEMP\monster_pack"
New-Item -ItemType Directory -Force $dl
curl.exe -L -o "$dl\pack.zip" "<에셋 zip URL>"
Expand-Archive "$dl\pack.zip" -DestinationPath $dl -Force
```

- [ ] **Step 3: 40칸 선별표 작성**

구역 테마에 맞춰 잡몹 30 + 보스 10을 고른다. 선별 기준:
- 잡몹 3종은 같은 구역 안에서 실루엣이 서로 달라야 한다
- 보스는 잡몹보다 크거나 위압적인 그림
- 초원(들쥐·들개·성난 들개) → 쥐·개과, 숲(거미·멧돼지·파수병) → 벌레·짐승·식물류,
  동굴 → 박쥐·도마뱀·해골, 폐광 → 유령·기계, 늪지 → 거머리·개구리·마녀,
  화산 → 불정령·도마뱀·거인, 설원 → 늑대·골렘·기사, 용의 둥지 → 도적·새끼용·수호룡,
  심연 → 눈알·그림자·촉수, 무한 회랑 → 잔상·검사·군주

- [ ] **Step 4: 시트 합성 스크립트 작성·실행**

`tools/make_monster_sheet.ps1`:

```powershell
# 낱장 PNG 40장을 8열x7행 32px 시트로 합친다.
# 사용: .\make_monster_sheet.ps1 -ListFile cells.txt -OutFile ..\app\src\main\res\drawable\monster_sheet.png
# cells.txt: 한 줄에 낱장 파일 경로 하나, 순서 = 칸 번호(0부터).
param(
    [Parameter(Mandatory)][string]$ListFile,
    [Parameter(Mandatory)][string]$OutFile
)
Add-Type -AssemblyName System.Drawing
$cell = 32; $cols = 8; $rows = 7
$sheet = [System.Drawing.Bitmap]::new($cell * $cols, $cell * $rows)
$g = [System.Drawing.Graphics]::FromImage($sheet)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$i = 0
foreach ($line in Get-Content $ListFile) {
    if ([string]::IsNullOrWhiteSpace($line)) { continue }
    $src = [System.Drawing.Bitmap]::FromFile($line.Trim())
    $x = ($i % $cols) * $cell; $y = [math]::Floor($i / $cols) * $cell
    # 원본이 16px이면 2배 확대(니어리스트), 32px이면 그대로
    $g.DrawImage($src, (New-Object System.Drawing.Rectangle($x, $y, $cell, $cell)))
    $src.Dispose(); $i++
}
$g.Dispose()
$sheet.Save($OutFile, [System.Drawing.Imaging.ImageFormat]::Png)
$sheet.Dispose()
Write-Host "cells=$i -> $OutFile"
```

실행 후 확인: `cells=40`, 파일 크기가 수십 KB 이하.

- [ ] **Step 5: 결과 육안 확인**

Read 도구로 `monster_sheet.png`를 열어 40칸이 격자에 맞는지, 투명 배경인지 확인한다.
칸이 어긋나면 cells.txt 순서를 고쳐 재실행.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/res/drawable/monster_sheet.png tools/make_monster_sheet.ps1
git commit -m "몬스터 스프라이트시트 40칸 (잡몹 30 + 보스 10)"
```

---

### Task 2: 치명타 — `:core` Combat

**Files:**
- Modify: `core/src/main/kotlin/com/geomgang/core/Combat.kt`
- Test: `core/src/test/kotlin/com/geomgang/core/CombatTest.kt`

**Interfaces:**
- Consumes: 기존 `Combat.hit(sword, combo, isBoss): Hit`
- Produces: `Hit(damage, hits, crit: Boolean = false)`,
  `Combat.hit(sword, combo, isBoss, critRoll: Double = 1.0): Hit`,
  `Combat.CRIT_CHANCE = 0.05`, `Combat.CRIT_MULTIPLIER = 1.8`
  (critRoll 기본값 1.0 = 치명타 없음 → 기존 호출·테스트 전부 그대로 통과)

- [ ] **Step 1: 실패하는 테스트 작성** (`CombatTest.kt`에 추가)

```kotlin
@Test
fun `치명타 롤이 기준 미만이면 피해가 배수로 커진다`() {
    val sword = Sword(WeaponFamily.STRAIGHT, 10)
    val normal = Combat.hit(sword, combo = 0, isBoss = false, critRoll = 1.0)
    val crit = Combat.hit(sword, combo = 0, isBoss = false, critRoll = 0.0)
    assertFalse(normal.crit)
    assertTrue(crit.crit)
    assertEquals(
        (normal.damage * Combat.CRIT_MULTIPLIER).roundToLong(),
        crit.damage,
    )
}

@Test
fun `치명타 경계값 - 기준과 같으면 치명타가 아니다`() {
    val sword = Sword(WeaponFamily.STRAIGHT, 5)
    assertFalse(Combat.hit(sword, 0, false, critRoll = Combat.CRIT_CHANCE).crit)
    assertTrue(Combat.hit(sword, 0, false, critRoll = Combat.CRIT_CHANCE - 0.001).crit)
}

@Test
fun `치명타는 쌍검의 타격 수를 바꾸지 않는다`() {
    val sword = Sword(WeaponFamily.TWIN, 8)
    assertEquals(2, Combat.hit(sword, 0, false, critRoll = 0.0).hits)
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :core:test --tests "com.geomgang.core.CombatTest" --console=plain`
Expected: FAIL — `crit` 파라미터/필드 없음 컴파일 에러.

- [ ] **Step 3: 최소 구현**

`Hit`와 `Combat.hit` 수정:

```kotlin
/** 한 번 탭한 결과. */
data class Hit(
    val damage: Long,
    /** 화면에 몇 번 튀어야 하는지. 쌍검은 2다. */
    val hits: Int,
    /** 치명타였는지. 화면이 크고 노랗게 띄운다. */
    val crit: Boolean = false,
)
```

```kotlin
/** 치명타 확률과 배수. 판정은 [hit] 에 난수 값을 넣어서 한다. */
const val CRIT_CHANCE = 0.05
const val CRIT_MULTIPLIER = 1.8

/**
 * 한 번 탭했을 때 들어가는 피해.
 *
 * @param combo    지금까지 연속으로 몇 번 쳤는지
 * @param isBoss   보스를 치는 중인지
 * @param critRoll 치명타 판정용 난수(0~1). 기본 1.0 = 치명타 없음.
 *                 난수 대신 값을 받는 것은 테스트를 결정적으로 만들기 위해서다.
 */
fun hit(sword: Sword?, combo: Int, isBoss: Boolean, critRoll: Double = 1.0): Hit {
    if (sword == null) return Hit(0, 0)
    val style = FamilyStyle.of(sword.family)
    val comboBonus = (style.comboGain * combo).coerceAtMost(MAX_COMBO_BONUS)
    val bossBonus = if (isBoss) style.bossBonus else 1.0
    val crit = critRoll < CRIT_CHANCE
    val perHit = attackPower(sword) * (1.0 + comboBonus) * bossBonus / style.hits
    val base = perHit.roundToLong().coerceAtLeast(1) * style.hits
    return Hit(
        damage = if (crit) (base * CRIT_MULTIPLIER).roundToLong() else base,
        hits = style.hits,
        crit = crit,
    )
}
```

주의: 치명타 배수는 **합산 피해에** 곱한다(타격 수 유지). `canBeatBoss`는 critRoll 기본값이라 변화 없음.

- [ ] **Step 4: 통과 확인**

Run: `./gradlew :core:test --console=plain`
Expected: 전부 PASS (기존 치명타 무관 테스트 포함).

- [ ] **Step 5: Commit**

```bash
git add core/src/main/kotlin/com/geomgang/core/Combat.kt core/src/test/kotlin/com/geomgang/core/CombatTest.kt
git commit -m "치명타 5% 1.8배 - 판정은 난수 주입 순수 함수"
```

---

### Task 3: ViewModel 배선 — 치명타 롤·hitSeq·isRare

**Files:**
- Modify: `app/src/main/java/com/geomgang/game/HuntUiState.kt`
- Modify: `app/src/main/java/com/geomgang/game/ForgeViewModel.kt` (`tapTarget`, `spawnNext`, `renderHunt`)
- Test: `app/src/test/java/com/geomgang/game/ForgeViewModelHuntTest.kt` (새 파일, 기존 Hunt 테스트가 있으면 그 파일에 추가)

**Interfaces:**
- Consumes: Task 2의 `Combat.hit(..., critRoll)` / `Hit.crit`
- Produces: `HuntUiState`에 `lastCrit: Boolean`, `hitSeq: Long`, `isRare: Boolean`,
  `lastKillGold: Long` (방금 처치로 번 골드, 처치 직후가 아니면 0) 추가,
  계산 프로퍼티 `enraged: Boolean` (보스 & 남은 시간 25% 이하 & 생존).
  화면은 `hitSeq` 변화로 팝업을, `isRare`·`enraged`로 틴트를,
  `lastKillGold`로 "+골드" 텍스트를 그린다

- [ ] **Step 1: 실패하는 테스트 작성**

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class ForgeViewModelHuntTest {
    // 기존 Hunt/Timer 테스트와 같은 세팅 방식(StandardTestDispatcher + 가짜 SaveStore)을 쓴다.

    @Test
    fun `탭마다 hitSeq가 1씩 오른다`() = runTest {
        val vm = huntReadyViewModel() // 검 지급 + enterZone(Zone.MEADOW) 까지 끝낸 헬퍼
        val before = vm.ui.value.hunt!!.hitSeq
        vm.tapTarget()
        assertEquals(before + 1, vm.ui.value.hunt!!.hitSeq)
    }

    @Test
    fun `치명타 롤이 낮으면 lastCrit이 참이다`() = runTest {
        // ScriptedRandom 순서: spawnNext(몬스터 종류 nextInt, 희귀 nextDouble) → tap(치명타 nextDouble)
        val vm = huntReadyViewModel(rng = ScriptedRandom(ints = listOf(0), doubles = listOf(0.5, 0.0)))
        vm.tapTarget()
        assertTrue(vm.ui.value.hunt!!.lastCrit)
    }

    @Test
    fun `희귀 몬스터면 isRare가 참이다`() = runTest {
        val vm = huntReadyViewModel(rng = ScriptedRandom(ints = listOf(0), doubles = listOf(0.01, 1.0)))
        assertTrue(vm.ui.value.hunt!!.isRare)
    }
}
```

(`ScriptedRandom`이 `nextInt`를 지원하지 않으면 지원하도록 확장하되, 기존 계약 —
스크립트에 없는 호출은 예외 — 를 유지한다.)

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :app:testDebugUnitTest --tests "com.geomgang.game.ForgeViewModelHuntTest" --console=plain`
Expected: FAIL — `hitSeq`/`lastCrit`/`isRare` 없음.

- [ ] **Step 3: 구현**

`HuntUiState.kt`에 필드 추가:

```kotlin
    /** 마지막 타격이 치명타였는지. */
    val lastCrit: Boolean,
    /** 타격마다 1씩 오르는 일련번호. 화면이 팝업 애니메이션 트리거로 쓴다. */
    val hitSeq: Long,
    /** 지금 대상이 희귀 몬스터인지. 금색 틴트를 입힌다. */
    val isRare: Boolean,
```

계산 프로퍼티 추가:

```kotlin
    /** 보스가 급해졌는지 - 남은 시간 25% 이하. 붉은 틴트 + 흔들림. */
    val enraged: Boolean
        get() = isBoss && targetHp > 0 && bossTimeRatio <= 0.25f
```

`ForgeViewModel`:
- 필드 추가 `private var lastCrit = false`, `private var hitSeq = 0L`,
  `private var lastKillGold = 0L`
- `tapTarget()`: `val hit = Combat.hit(sword, combo, fightingBoss, rng.nextDouble())` 로 교체,
  `lastCrit = hit.crit; hitSeq++` 를 `lastDamage = hit.damage` 옆에 추가
- `onTargetDown()`: 잡몹 분기에서 `lastKillGold = gold`, 보스 분기에서
  `lastKillGold = zone.bossGold` 기록
- `spawnNext()`: `lastCrit = false` 추가 (hitSeq는 리셋하지 않는다 — 화면 키 충돌 방지.
  `lastKillGold`도 리셋하지 않는다 — 처치 직후 스폰되므로 화면이 아직 그리는 중이다.
  다음 처치 때 덮어써진다)
- `renderHunt()`: `lastCrit = lastCrit, hitSeq = hitSeq,
  isRare = rareTarget && !fightingBoss, lastKillGold = lastKillGold` 전달

- [ ] **Step 4: 통과 확인**

Run: `./gradlew :app:testDebugUnitTest --console=plain`
Expected: 전부 PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/geomgang/game/HuntUiState.kt app/src/main/java/com/geomgang/game/ForgeViewModel.kt app/src/test/java/com/geomgang/game/ForgeViewModelHuntTest.kt
git commit -m "사냥 타격 배선 - 치명타 롤, hitSeq, 희귀 표시"
```

---

### Task 4: 몬스터 시트 좌표 매핑

**Files:**
- Create: `app/src/main/java/com/geomgang/game/ui/MonsterSheet.kt`
- Test: `app/src/test/java/com/geomgang/game/MonsterSheetTest.kt`

**Interfaces:**
- Consumes: Task 1의 시트 칸 배치(0~29 잡몹, 30~39 보스)
- Produces: `MonsterSheet.CELL = 32`, `MonsterSheet.COLUMNS = 8`,
  `MonsterSheet.hasCell(name: String): Boolean`,
  `MonsterSheet.cellOf(name: String): Int` (몬스터·보스 이름 → 칸 번호),
  `MonsterSheet.offsetOf(cell: Int): IntOffset` (칸 번호 → 픽셀 좌표)

- [ ] **Step 1: 실패하는 테스트 작성**

```kotlin
class MonsterSheetTest {

    @Test
    fun `모든 구역의 잡몹과 보스 이름에 칸이 있다`() {
        for (zone in Zone.entries) {
            for (m in zone.monsters) {
                assertTrue("${zone.displayName}/${m.name} 누락", MonsterSheet.hasCell(m.name))
            }
            assertTrue("${zone.displayName} 보스 누락", MonsterSheet.hasCell(zone.bossName))
        }
    }

    @Test
    fun `칸 번호는 겹치지 않는다`() {
        val cells = Zone.entries.flatMap { z -> z.monsters.map { MonsterSheet.cellOf(it.name) } } +
            Zone.entries.map { MonsterSheet.cellOf(it.bossName) }
        assertEquals(cells.size, cells.toSet().size)
    }

    @Test
    fun `잡몹은 0-29, 보스는 30-39 칸을 쓴다`() {
        for (zone in Zone.entries) {
            for (m in zone.monsters) assertTrue(MonsterSheet.cellOf(m.name) in 0..29)
            assertTrue(MonsterSheet.cellOf(zone.bossName) in 30..39)
        }
    }

    @Test
    fun `좌표는 시트 범위 안이다`() {
        for (cell in 0 until 40) {
            val o = MonsterSheet.offsetOf(cell)
            assertTrue(o.x in 0 until MonsterSheet.CELL * MonsterSheet.COLUMNS)
            assertTrue(o.y >= 0)
        }
    }

    @Test
    fun `모르는 이름은 예외를 던진다`() {
        assertFailsWith<IllegalArgumentException> { MonsterSheet.cellOf("없는 몬스터") }
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :app:testDebugUnitTest --tests "com.geomgang.game.MonsterSheetTest" --console=plain`
Expected: FAIL — `MonsterSheet` 없음.

- [ ] **Step 3: 구현**

```kotlin
package com.geomgang.game.ui

import androidx.compose.ui.unit.IntOffset
import com.geomgang.core.Zone

/**
 * 몬스터 그림의 단일 출처.
 *
 * monster_sheet.png 는 32px 칸 8열 시트다. 0~29 잡몹(구역 순서×3), 30~39 보스(구역 순서).
 * 칸 배치를 바꾸면 여기와 tools/make_monster_sheet.ps1 의 cells.txt 를 함께 바꿔야 한다.
 *
 * 출처: <에셋 이름 — 저작자, 라이선스>. 표기는 설정 → 라이선스 화면에 있다.
 */
object MonsterSheet {

    const val CELL = 32
    const val COLUMNS = 8

    /** 구역 순서가 곧 칸 순서다 - 이름 목록을 Zone 에서 뽑아 표를 만든다. */
    private val cells: Map<String, Int> = buildMap {
        var mob = 0
        for (zone in Zone.entries) {
            for (m in zone.monsters) put(m.name, mob++)
        }
        var boss = 30
        for (zone in Zone.entries) put(zone.bossName, boss++)
    }

    fun hasCell(name: String): Boolean = name in cells

    fun cellOf(name: String): Int =
        requireNotNull(cells[name]) { "unknown monster: $name" }

    fun offsetOf(cell: Int): IntOffset =
        IntOffset((cell % COLUMNS) * CELL, (cell / COLUMNS) * CELL)
}
```

주의: 매핑을 손 표가 아니라 `Zone.entries` 순회로 만든다 — 구역이 늘면 저절로 따라온다.
대신 **시트의 칸 배치가 반드시 구역 순서와 일치**해야 한다 (Task 1의 선별표가 그 계약).

- [ ] **Step 4: 통과 확인**

Run: `./gradlew :app:testDebugUnitTest --tests "com.geomgang.game.MonsterSheetTest" --console=plain`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/geomgang/game/ui/MonsterSheet.kt app/src/test/java/com/geomgang/game/MonsterSheetTest.kt
git commit -m "몬스터 시트 좌표 매핑 - 구역 순서 규칙"
```

---

### Task 5: MonsterSprite 컴포저블 (틴트·흔들림·처치 연출)

**Files:**
- Create: `app/src/main/java/com/geomgang/game/ui/MonsterView.kt`
- Modify: `app/src/main/java/com/geomgang/game/ui/HuntScreen.kt` (`MonsterBlob` 삭제 → `MonsterSprite`)

**Interfaces:**
- Consumes: Task 3의 `HuntUiState.isRare/enraged/hitSeq`, Task 4의 `MonsterSheet`
- Produces: `MonsterSprite(name, hpRatio, isBoss, isRare, enraged, hitSeq, modifier)` —
  HuntScreen 과 (M15에서) GauntletScreen 이 쓴다

- [ ] **Step 1: 구현** (그리기 코드라 테스트는 없다 — 규칙은 Task 4가 지킨다)

```kotlin
package com.geomgang.game.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.geomgang.game.R

@Composable
private fun rememberMonsterSheet(): ImageBitmap {
    val context = LocalContext.current
    return remember {
        val options = BitmapFactory.Options().apply { inScaled = false }
        BitmapFactory.decodeResource(context.resources, R.drawable.monster_sheet, options)
            .asImageBitmap()
    }
}

/**
 * 몬스터 한 마리.
 *
 * - 체력이 줄면 쪼그라든다 (기존 MonsterBlob 의 감각 유지)
 * - 희귀 = 금색 틴트, 보스 발악 = 붉은 틴트
 * - hitSeq 가 바뀔 때마다 좌우로 짧게 흔들린다
 * - 체력 0 = 회색으로 바래며 쓰러진 표시
 */
@Composable
fun MonsterSprite(
    name: String,
    hpRatio: Float,
    isBoss: Boolean,
    isRare: Boolean,
    enraged: Boolean,
    hitSeq: Long,
    modifier: Modifier = Modifier,
) {
    val sheet = rememberMonsterSheet()
    val cell = MonsterSheet.cellOf(name)
    val src = MonsterSheet.offsetOf(cell)

    val shake = remember { Animatable(0f) }
    LaunchedEffect(hitSeq) {
        if (hitSeq == 0L) return@LaunchedEffect
        shake.snapTo(0f)
        shake.animateTo(0f, keyframes {
            durationMillis = 120
            6f at 30
            -5f at 70
            0f at 120
        })
    }

    val base: Dp = if (isBoss) 150.dp else 110.dp
    val side = base * (0.55f + hpRatio * 0.45f)
    val tint: Color? = when {
        hpRatio <= 0f -> Color(0xB3555555)
        enraged -> Color(0x66E05A5A)
        isRare -> Color(0x66FFD54A)
        else -> null
    }

    Canvas(modifier.size(side)) {
        val dst = IntSize(size.width.toInt(), size.height.toInt())
        drawImage(
            image = sheet,
            srcOffset = IntOffset(src.x, src.y),
            srcSize = IntSize(MonsterSheet.CELL, MonsterSheet.CELL),
            dstOffset = IntOffset(shake.value.toInt(), 0),
            dstSize = dst,
            filterQuality = FilterQuality.None,
        )
        if (tint != null) {
            drawImage(
                image = sheet,
                srcOffset = IntOffset(src.x, src.y),
                srcSize = IntSize(MonsterSheet.CELL, MonsterSheet.CELL),
                dstOffset = IntOffset(shake.value.toInt(), 0),
                dstSize = dst,
                filterQuality = FilterQuality.None,
                colorFilter = ColorFilter.tint(tint),
            )
        }
    }
}
```

- [ ] **Step 2: HuntScreen 교체**

`MonsterBlob(hunt.hpRatio, hunt.isBoss)` 호출을 다음으로 바꾸고 `MonsterBlob` 함수를 지운다:

```kotlin
MonsterSprite(
    name = hunt.rawTargetName,
    hpRatio = hunt.hpRatio,
    isBoss = hunt.isBoss,
    isRare = hunt.isRare,
    enraged = hunt.enraged,
    hitSeq = hunt.hitSeq,
)
```

주의: `targetName`은 희귀일 때 "희귀 " 접두어가 붙어 시트 매핑이 깨진다.
`HuntUiState`에 접두어 없는 이름 `rawTargetName: String`을 추가하고(renderHunt 에서
`targetKind?.name ?: ...` 원본을 전달), 표시는 기존 `targetName` 그대로 쓴다.
"쓰러짐" 문구는 스프라이트 아래 별도 Text 로 유지.

- [ ] **Step 3: 빌드 확인**

Run: `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest --console=plain`
Expected: 성공. (`rawTargetName` 추가로 HuntUiState 생성자를 쓰는 테스트가 깨지면 함께 고친다)

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/geomgang/game/ui/MonsterView.kt app/src/main/java/com/geomgang/game/ui/HuntScreen.kt app/src/main/java/com/geomgang/game/HuntUiState.kt app/src/main/java/com/geomgang/game/ForgeViewModel.kt
git commit -m "몬스터 스프라이트 렌더링 - 틴트·흔들림·처치 연출"
```

---

### Task 6: 데미지 팝업 + 구역 분위기

**Files:**
- Create: `app/src/main/java/com/geomgang/game/ui/ZoneTheme.kt`
- Modify: `app/src/main/java/com/geomgang/game/ui/HuntScreen.kt`

**Interfaces:**
- Consumes: `HuntUiState.hitSeq/lastDamage/lastCrit/lastHits`
- Produces: `zoneBrush(zone: Zone): Brush` — (M15 회랑 화면도 재사용)

- [ ] **Step 1: ZoneTheme 구현**

```kotlin
package com.geomgang.game.ui

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.geomgang.core.Zone

/** 구역마다 배경 분위기. 어두운 테마 위에 얹는 은은한 그라디언트다. */
fun zoneBrush(zone: Zone): Brush {
    val (top, bottom) = when (zone) {
        Zone.MEADOW -> Color(0xFF17251A) to Color(0xFF0E1410)
        Zone.FOREST -> Color(0xFF152417) to Color(0xFF0D130D)
        Zone.CAVE -> Color(0xFF1B1B22) to Color(0xFF101014)
        Zone.MINE -> Color(0xFF221D16) to Color(0xFF141110)
        Zone.SWAMP -> Color(0xFF18221E) to Color(0xFF0E1412)
        Zone.VOLCANO -> Color(0xFF261314) to Color(0xFF140D0D)
        Zone.SNOWFIELD -> Color(0xFF1A2026) to Color(0xFF10131A)
        Zone.DRAGON_NEST -> Color(0xFF221520) to Color(0xFF120D12)
        Zone.ABYSS -> Color(0xFF151226) to Color(0xFF0C0A16)
        Zone.ENDLESS_HALL -> Color(0xFF1F1A26) to Color(0xFF120F16)
    }
    return Brush.verticalGradient(listOf(top, bottom))
}
```

- [ ] **Step 2: 데미지 팝업 구현** (HuntScreen 안 private)

```kotlin
private data class DamagePop(val id: Long, val text: String, val crit: Boolean, val xJitter: Int)

@Composable
private fun DamagePopups(hunt: HuntUiState, modifier: Modifier = Modifier) {
    val pops = remember { mutableStateListOf<DamagePop>() }
    LaunchedEffect(hunt.hitSeq) {
        if (hunt.hitSeq == 0L || hunt.lastDamage <= 0) return@LaunchedEffect
        val text = if (hunt.lastHits > 1) {
            "-%,d ×${hunt.lastHits}".format(hunt.lastDamage / hunt.lastHits)
        } else {
            "-%,d".format(hunt.lastDamage)
        }
        pops += DamagePop(hunt.hitSeq, text, hunt.lastCrit, Random.nextInt(-40, 41))
        if (pops.size > 6) pops.removeAt(0)   // 연타 시 화면이 숫자로 뒤덮이지 않게
    }
    Box(modifier) {
        pops.forEach { pop ->
            key(pop.id) {
                PopText(pop) { pops.remove(pop) }
            }
        }
    }
}

@Composable
private fun PopText(pop: DamagePop, onDone: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(pop.id) {
        progress.animateTo(1f, tween(durationMillis = if (pop.crit) 900 else 650))
        onDone()
    }
    Text(
        text = if (pop.crit) "치명타! ${pop.text}" else pop.text,
        fontSize = if (pop.crit) 24.sp else 16.sp,
        fontWeight = FontWeight.Bold,
        color = if (pop.crit) Color(0xFFFFD54A) else Color(0xFFEEEEEE),
        modifier = Modifier
            .offset { IntOffset(pop.xJitter, (-90 * progress.value).toInt()) }
            .graphicsLayer { alpha = 1f - progress.value * progress.value },
    )
}
```

- [ ] **Step 3: HuntScreen 조립**

- 사냥 중 화면의 루트 `Column`을 `Box(Modifier.fillMaxSize().background(zoneBrush(hunt.zone)))`
  로 감싼다 (ZonePicker 는 그대로)
- 몬스터 `Box` 위에 `DamagePopups(hunt, Modifier.align(Alignment.Center))` 를 겹친다
  (몬스터 스프라이트보다 위 레이어)
- **처치 골드 텍스트**: `DamagePopups`의 `LaunchedEffect`에 처치 감지를 추가한다 —
  `hunt.targetHp <= 0 && hunt.lastKillGold > 0` 이면
  `pops += DamagePop(id = -hunt.hitSeq, text = "+%,d".format(hunt.lastKillGold),
  crit = true, xJitter = 0)` (음수 id 로 타격 팝업과 키 충돌 방지, 금색 크게 표시)
- import 정리: `mutableStateListOf`, `key`, `tween`, `graphicsLayer`, `offset`,
  `kotlin.random.Random`

- [ ] **Step 4: 빌드·테스트 확인**

Run: `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :core:test --console=plain`
Expected: 전부 성공.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/geomgang/game/ui/ZoneTheme.kt app/src/main/java/com/geomgang/game/ui/HuntScreen.kt
git commit -m "데미지 팝업·치명타 연출·구역별 배경 그라디언트"
```

---

### Task 7: 라이선스 표기 + 마무리

**Files:**
- Modify: `app/src/main/java/com/geomgang/game/ui/SettingsScreen.kt:128` (라이선스 항목)
- Modify: `app/src/main/java/com/geomgang/game/ui/MonsterSheet.kt` (헤더 주석의 출처 칸 채우기)
- Modify: `README.md` (개발 일지 v1.3.0-M11 절)

**Interfaces:**
- Consumes: Task 1에서 기록해 둔 에셋 이름·저작자·라이선스·URL

- [ ] **Step 1: 라이선스 화면에 항목 추가**

`SettingsScreen.kt`의 기존 문구를 확장:

```kotlin
text = "The Humble Sword Pack · CC BY 4.0\n" +
    // ...기존 줄 유지...
    "<몬스터 에셋 이름> · <라이선스>\n" +
    "<저작자> — <URL>",
```

(CC0이어도 표기한다 — 의무는 아니지만 출처를 남기는 것이 이 프로젝트의 방침이다.)

- [ ] **Step 2: 전체 테스트 + APK**

Run: `./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug --console=plain`
Expected: 전부 성공.

- [ ] **Step 3: 실기기 확인 항목** (폰 연결 시)

- 잡몹·보스가 그림으로 나오는가, 구역을 옮기면 그림이 바뀌는가
- 희귀가 금색인가, 보스 막판이 붉어지는가
- 탭마다 숫자가 튀는가, 치명타가 가끔 크게 노랗게 터지는가
- 구역마다 배경 색감이 다른가

- [ ] **Step 4: README 갱신 후 Commit·Push**

```bash
git add -A
git commit -m "M11 마무리 - 라이선스 표기, 개발 일지"
git push origin main
```
