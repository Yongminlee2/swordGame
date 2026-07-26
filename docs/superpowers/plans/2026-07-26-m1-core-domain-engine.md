# M1: `:core` 도메인 엔진 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 안드로이드 의존성이 전혀 없는 순수 Kotlin 모듈 `:core`에 검 강화 게임의 규칙 전부(확률·경제·판정·진행도)를 구현하고, 밸런스 시뮬레이션으로 확률표를 1차 확정한다.

**Architecture:** `:core`는 `java-library` + Kotlin JVM 모듈이다. 모든 도메인 타입은 불변 data class이고, 상태를 바꾸는 함수는 새 상태를 반환하는 순수 함수다. 난수는 항상 파라미터로 주입해 테스트에서 시드나 스크립트로 고정할 수 있게 한다. UI·저장·안드로이드는 M2 이후에 붙으며 이 모듈은 그것들을 알지 못한다.

**Tech Stack:** Kotlin 2.4.0 (JVM), Gradle 9.4.1, JUnit 4.13.2, JDK 21 (Android Studio JBR)

Kotlin은 2.4.0을 쓴다. Gradle 9와 확실히 맞물리고, 로컬 Gradle 캐시에 이미 받아져 있어 첫 빌드가 네트워크에 의존하지 않는다.

**참조 스펙:** [2026-07-26-sword-enhance-game-design.md](../specs/2026-07-26-sword-enhance-game-design.md)

## Global Constraints

이 절의 제약은 모든 태스크의 요구사항에 암묵적으로 포함된다.

- **`:core`에 안드로이드 의존성을 넣지 않는다.** `android.*`, `androidx.*`, `kotlinx.coroutines` 어느 것도 import 하지 않는다. 모듈 플러그인은 `java-library` + `org.jetbrains.kotlin.jvm`만 쓴다.
- **난수는 반드시 파라미터로 주입한다.** `Random.Default`, `Math.random()`, `kotlin.random.Random.nextInt()` 같은 전역 난수 호출을 `:core` 프로덕션 코드에 쓰지 않는다.
- **모든 도메인 타입은 불변이다.** `var` 프로퍼티, 가변 컬렉션(`MutableList` 등)을 public API에 노출하지 않는다. 상태 변경은 `copy()`로 새 인스턴스를 만든다.
- **패키지는 `com.geomgang.core`** 이며 하위 패키지를 만들지 않는다. (테스트 전용 시뮬레이터만 `com.geomgang.core.sim`)
- **Kotlin JDK 타깃은 21**, `jvmToolchain(21)`로 고정한다.
- **골드는 `Long`, 조각·단계·아이템 수량은 `Int`** 로 통일한다. 골드는 무한 모드에서 `Int` 범위를 넘길 수 있다.
- **단계를 가리키는 파라미터 이름 규칙**: `targetLevel`은 "이번 시도로 도달하려는 단계"(현재 +7이면 8), `level`/`currentLevel`은 "현재 보유 단계"다. 확률표는 전부 `targetLevel` 기준이다.
- **테스트 프레임워크는 JUnit 4** (`org.junit.Test`, `org.junit.Assert.*`). JUnit 5나 kotlin-test를 도입하지 않는다.
- **부동소수 비교는 반드시 델타를 준다**: `assertEquals(expected, actual, 1e-9)`.
- **커밋 메시지는 한국어**로 쓰고, `Co-Authored-By` 트레일러를 넣지 않는다. 이 저장소의 커밋 저작자는 `Yongminlee2 <dydals5678@gmail.com>` 단독이다.
- **작업 디렉터리는 `C:\workAndroid\SwordForge`**. 셸은 Git Bash 기준이며 경로는 `/c/workAndroid/SwordForge`로 쓴다.

---

## File Structure

M1 종료 시점의 파일 구성이다.

```
SwordForge/
├── settings.gradle.kts              루트 설정, :core 만 포함
├── build.gradle.kts                 플러그인 선언(apply false)
├── gradle.properties                JVM 옵션, JDK 경로
├── gradle/libs.versions.toml        버전 카탈로그
├── gradle/wrapper/                  WordChain에서 복사
├── gradlew, gradlew.bat             WordChain에서 복사
└── core/
    ├── build.gradle.kts
    └── src/
        ├── main/kotlin/com/geomgang/core/
        │   ├── Difficulty.kt        난이도 4종과 배수·상한
        │   ├── RateTable.kt         확률표 전부. 게임 밸런스의 단일 출처
        │   ├── Model.kt             Item·WeaponFamily·Sword·Inventory·GameState 등 값 타입
        │   ├── WeaponCatalog.kt     티어 11종, 단계→티어 매핑, 도감 88엔트리
        │   ├── Recipes.kt           조합소 교환식
        │   ├── Economy.kt           강화비용·판매가·상점가·파산 구제
        │   ├── ForgeEngine.kt       강화 판정, 방지권, 줍기
        │   └── Progress.kt          도감·업적·칭호·통계 누적
        └── test/kotlin/com/geomgang/core/
            ├── ScriptedRandom.kt    난수를 대본대로 돌려주는 테스트 도구
            ├── RateTableTest.kt
            ├── ModelTest.kt
            ├── WeaponCatalogTest.kt
            ├── RecipesTest.kt
            ├── EconomyTest.kt
            ├── ForgeEngineTest.kt
            ├── ForgeRecoveryTest.kt
            ├── ProgressTest.kt
            └── sim/
                ├── BalanceSimulation.kt   자동 플레이 시뮬레이터
                └── BalanceSimulationTest.kt
```

**책임 분리 원칙**: `RateTable`은 확률만, `Economy`는 돈만, `ForgeEngine`은 그 둘을 조합한 판정만 안다. `ForgeEngine`에 확률 숫자를 직접 쓰지 않는다 — 밸런스를 고칠 때 고칠 곳이 한 군데여야 한다.

---

### Task 1: Gradle 스캐폴딩 + 난이도 + 확률표

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `core/build.gradle.kts`
- Copy: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties` (from `C:\workAndroid\WordChain`)
- Create: `core/src/main/kotlin/com/geomgang/core/Difficulty.kt`
- Create: `core/src/main/kotlin/com/geomgang/core/RateTable.kt`
- Test: `core/src/test/kotlin/com/geomgang/core/RateTableTest.kt`

**Interfaces:**
- Consumes: 없음 (첫 태스크)
- Produces:
  - `enum class Difficulty(val id: String, val multiplier: Double, val maxLevel: Int?)` — `EASY/NORMAL/HARD/ENDLESS`, `val isEndless: Boolean`, `companion object { fun fromId(id: String): Difficulty }`
  - `enum class FailureBand { STAY, DROP, DESTROY_OR_DROP }`
  - `object RateTable` — `baseSuccessRate(targetLevel: Int): Double`, `successRate(difficulty: Difficulty, targetLevel: Int, blessing: Boolean = false): Double`, `failureBand(targetLevel: Int): FailureBand`, `destroyChance(targetLevel: Int): Double`, 상수 `MAX_SUCCESS_RATE=0.98`, `BLESSING_BONUS=0.10`, `ENDLESS_DECAY=0.85`, `ENDLESS_FLOOR=0.005`, `MAX_FINITE_LEVEL=20`, `SAFE_BAND_END=5`, `DROP_BAND_END=12`

- [ ] **Step 1: Gradle 래퍼를 WordChain에서 복사한다**

래퍼 jar은 바이너리라 새로 만들 수 없다. 이미 검증된 것을 그대로 가져온다.

```bash
cd /c/workAndroid/SwordForge
mkdir -p gradle/wrapper
cp /c/workAndroid/WordChain/gradlew .
cp /c/workAndroid/WordChain/gradlew.bat .
cp /c/workAndroid/WordChain/gradle/wrapper/gradle-wrapper.jar gradle/wrapper/
cp /c/workAndroid/WordChain/gradle/wrapper/gradle-wrapper.properties gradle/wrapper/
chmod +x gradlew
ls -la gradlew gradle/wrapper/
```

- [ ] **Step 2: 빌드 파일 5개를 만든다**

`settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SwordForge"
include(":core")
```

`build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
}
```

`gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
org.gradle.configuration-cache=true
org.gradle.java.home=C:/Program Files/Android/Android Studio/jbr
kotlin.code.style=official
```

`gradle/libs.versions.toml`:

```toml
[versions]
kotlin = "2.4.0"
junit = "4.13.2"

[libraries]
junit = { group = "junit", name = "junit", version.ref = "junit" }

[plugins]
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
```

`core/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    testImplementation(libs.junit)
}

tasks.test {
    useJUnit()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
```

- [ ] **Step 3: 실패하는 테스트를 작성한다**

`core/src/test/kotlin/com/geomgang/core/RateTableTest.kt`:

```kotlin
package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RateTableTest {

    private val eps = 1e-9

    @Test
    fun `기준 성공률은 스펙 표와 일치한다`() {
        assertEquals(0.95, RateTable.baseSuccessRate(1), eps)
        assertEquals(0.75, RateTable.baseSuccessRate(5), eps)
        assertEquals(0.40, RateTable.baseSuccessRate(10), eps)
        assertEquals(0.15, RateTable.baseSuccessRate(15), eps)
        assertEquals(0.02, RateTable.baseSuccessRate(20), eps)
    }

    @Test
    fun `기준 성공률은 단계가 오를수록 단조 감소한다`() {
        for (level in 2..RateTable.MAX_FINITE_LEVEL) {
            val prev = RateTable.baseSuccessRate(level - 1)
            val cur = RateTable.baseSuccessRate(level)
            assertTrue("level=$level: $cur >= $prev", cur < prev)
        }
    }

    @Test
    fun `난이도 배수가 적용된다`() {
        // 일반은 기준 그대로
        assertEquals(0.40, RateTable.successRate(Difficulty.NORMAL, 10), eps)
        // 지옥은 0.75배
        assertEquals(0.30, RateTable.successRate(Difficulty.HARD, 10), eps)
        // 쉬움은 1.25배
        assertEquals(0.50, RateTable.successRate(Difficulty.EASY, 10), eps)
    }

    @Test
    fun `쉬움 모드 저단계는 98퍼센트 상한에 걸린다`() {
        // 0.95 * 1.25 = 1.1875 이므로 상한으로 잘린다
        assertEquals(0.98, RateTable.successRate(Difficulty.EASY, 1), eps)
    }

    @Test
    fun `축복서는 난이도 배수를 적용한 뒤에 더해진다`() {
        // 지옥 +10: 0.40 * 0.75 = 0.30, 여기에 +0.10
        assertEquals(0.40, RateTable.successRate(Difficulty.HARD, 10, blessing = true), eps)
        // 일반 +10: 0.40 + 0.10
        assertEquals(0.50, RateTable.successRate(Difficulty.NORMAL, 10, blessing = true), eps)
    }

    @Test
    fun `축복서를 써도 98퍼센트를 넘지 못한다`() {
        assertEquals(0.98, RateTable.successRate(Difficulty.NORMAL, 1, blessing = true), eps)
    }

    @Test
    fun `무한 모드는 21단계부터 직전의 85퍼센트로 감쇠한다`() {
        assertEquals(0.02 * 0.85, RateTable.successRate(Difficulty.ENDLESS, 21), eps)
        assertEquals(0.02 * 0.85 * 0.85, RateTable.successRate(Difficulty.ENDLESS, 22), eps)
    }

    @Test
    fun `무한 모드 성공률은 0점5퍼센트 아래로 내려가지 않는다`() {
        assertEquals(0.005, RateTable.successRate(Difficulty.ENDLESS, 100), eps)
        assertTrue(RateTable.successRate(Difficulty.ENDLESS, 60) >= RateTable.ENDLESS_FLOOR)
    }

    @Test
    fun `모든 난이도 모든 단계에서 성공률은 0과 1 사이다`() {
        for (difficulty in Difficulty.entries) {
            for (level in 1..RateTable.MAX_FINITE_LEVEL) {
                for (blessing in listOf(false, true)) {
                    val rate = RateTable.successRate(difficulty, level, blessing)
                    assertTrue(
                        "$difficulty level=$level blessing=$blessing rate=$rate",
                        rate in 0.0..1.0,
                    )
                }
            }
        }
    }

    @Test
    fun `실패 구간 경계가 스펙과 일치한다`() {
        assertEquals(FailureBand.STAY, RateTable.failureBand(1))
        assertEquals(FailureBand.STAY, RateTable.failureBand(5))
        assertEquals(FailureBand.DROP, RateTable.failureBand(6))
        assertEquals(FailureBand.DROP, RateTable.failureBand(12))
        assertEquals(FailureBand.DESTROY_OR_DROP, RateTable.failureBand(13))
        assertEquals(FailureBand.DESTROY_OR_DROP, RateTable.failureBand(20))
        assertEquals(FailureBand.DESTROY_OR_DROP, RateTable.failureBand(21))
    }

    @Test
    fun `파괴 확률이 스펙 표와 일치한다`() {
        assertEquals(0.00, RateTable.destroyChance(12), eps)
        assertEquals(0.40, RateTable.destroyChance(13), eps)
        assertEquals(0.40, RateTable.destroyChance(15), eps)
        assertEquals(0.60, RateTable.destroyChance(16), eps)
        assertEquals(0.60, RateTable.destroyChance(18), eps)
        assertEquals(0.80, RateTable.destroyChance(19), eps)
        assertEquals(0.80, RateTable.destroyChance(20), eps)
    }

    @Test
    fun `무한 구간 실패는 항상 파괴다`() {
        assertEquals(1.00, RateTable.destroyChance(21), eps)
        assertEquals(1.00, RateTable.destroyChance(50), eps)
    }

    @Test
    fun `난이도별 상한이 스펙과 일치한다`() {
        assertEquals(20, Difficulty.EASY.maxLevel)
        assertEquals(20, Difficulty.NORMAL.maxLevel)
        assertEquals(20, Difficulty.HARD.maxLevel)
        assertEquals(null, Difficulty.ENDLESS.maxLevel)
        assertTrue(Difficulty.ENDLESS.isEndless)
        assertTrue(!Difficulty.NORMAL.isEndless)
    }

    @Test
    fun `아이디로 난이도를 찾을 수 있다`() {
        assertEquals(Difficulty.HARD, Difficulty.fromId("hard"))
    }
}
```

- [ ] **Step 4: 테스트가 실패하는지 확인한다**

Run:

```bash
cd /c/workAndroid/SwordForge && ./gradlew :core:test
```

Expected: 컴파일 실패. `Unresolved reference: Difficulty`, `Unresolved reference: RateTable`.

- [ ] **Step 5: `Difficulty.kt`를 구현한다**

```kotlin
package com.geomgang.core

/**
 * 게임 모드. 각 모드는 세이브가 완전히 분리된 독립 진행이다.
 *
 * 확률표는 [RateTable]에 한 벌만 두고, 여기 [multiplier]를 곱해 난이도를 만든다.
 * 표를 여러 벌 두지 않는 이유는 밸런스를 고칠 곳을 한 군데로 유지하기 위해서다.
 *
 * @property multiplier 기준 성공률에 곱하는 배수
 * @property maxLevel   강화 상한. null 이면 상한 없음(무한 모드)
 */
enum class Difficulty(
    val id: String,
    val multiplier: Double,
    val maxLevel: Int?,
) {
    EASY("easy", 1.25, 20),
    NORMAL("normal", 1.00, 20),
    HARD("hard", 0.75, 20),
    ENDLESS("endless", 1.00, null),
    ;

    val isEndless: Boolean get() = maxLevel == null

    companion object {
        fun fromId(id: String): Difficulty =
            entries.firstOrNull { it.id == id }
                ?: throw IllegalArgumentException("unknown difficulty id: $id")
    }
}
```

- [ ] **Step 6: `RateTable.kt`를 구현한다**

```kotlin
package com.geomgang.core

import kotlin.math.pow

/** 강화 실패 시 어떤 처리를 받는 구간인지. */
enum class FailureBand {
    /** 단계 유지. */
    STAY,

    /** 1단계 하락. */
    DROP,

    /** [RateTable.destroyChance] 확률로 파괴, 나머지는 하락. */
    DESTROY_OR_DROP,
}

/**
 * 강화 확률표. 게임 밸런스의 단일 출처다.
 *
 * 모든 함수의 `targetLevel`은 "이번 시도로 도달하려는 단계"다.
 * +7 검을 +8로 올리려는 시도의 targetLevel 은 8 이다.
 */
object RateTable {

    /** 난이도 배수와 축복서를 모두 적용한 뒤에도 넘을 수 없는 성공률 상한. */
    const val MAX_SUCCESS_RATE: Double = 0.98

    /** 축복서 1장이 더해 주는 성공률(%p). 난이도 배수를 적용한 뒤에 가산한다. */
    const val BLESSING_BONUS: Double = 0.10

    /** 무한 모드 +21 이상에서 한 단계마다 곱해지는 감쇠 계수. */
    const val ENDLESS_DECAY: Double = 0.85

    /** 무한 모드 성공률의 하한. 이보다 낮아지지 않는다. */
    const val ENDLESS_FLOOR: Double = 0.005

    /** 유한 모드(쉬움·일반·지옥)의 최대 단계. */
    const val MAX_FINITE_LEVEL: Int = 20

    /** 실패해도 단계가 유지되는 마지막 단계. */
    const val SAFE_BAND_END: Int = 5

    /** 실패 시 1단계 하락하는 마지막 단계. 이 위는 파괴 가능 구간이다. */
    const val DROP_BAND_END: Int = 12

    /** 인덱스 = targetLevel(1..20). 0번 자리는 사용하지 않는 자리채움이다. */
    private val BASE = doubleArrayOf(
        0.00,
        0.95, 0.90, 0.85, 0.80, 0.75,
        0.68, 0.61, 0.54, 0.47, 0.40,
        0.34, 0.28, 0.23, 0.19, 0.15,
        0.12, 0.09, 0.06, 0.04, 0.02,
    )

    /** 난이도 보정 전 기준 성공률. 무한 구간(21+)은 감쇠식으로 계산한다. */
    fun baseSuccessRate(targetLevel: Int): Double {
        require(targetLevel >= 1) { "targetLevel must be >= 1, was $targetLevel" }
        if (targetLevel <= MAX_FINITE_LEVEL) return BASE[targetLevel]
        val steps = (targetLevel - MAX_FINITE_LEVEL).toDouble()
        val decayed = BASE[MAX_FINITE_LEVEL] * ENDLESS_DECAY.pow(steps)
        return maxOf(decayed, ENDLESS_FLOOR)
    }

    /** 난이도 배수와 축복서를 반영한 최종 성공률. */
    fun successRate(
        difficulty: Difficulty,
        targetLevel: Int,
        blessing: Boolean = false,
    ): Double {
        val scaled = baseSuccessRate(targetLevel) * difficulty.multiplier
        val boosted = if (blessing) scaled + BLESSING_BONUS else scaled
        return minOf(boosted, MAX_SUCCESS_RATE)
    }

    /** 실패했을 때 어떤 처리를 받는 구간인지. */
    fun failureBand(targetLevel: Int): FailureBand {
        require(targetLevel >= 1) { "targetLevel must be >= 1, was $targetLevel" }
        return when {
            targetLevel <= SAFE_BAND_END -> FailureBand.STAY
            targetLevel <= DROP_BAND_END -> FailureBand.DROP
            else -> FailureBand.DESTROY_OR_DROP
        }
    }

    /** 실패했을 때 파괴로 이어질 확률. 유지·하락 구간이면 0.0. */
    fun destroyChance(targetLevel: Int): Double {
        require(targetLevel >= 1) { "targetLevel must be >= 1, was $targetLevel" }
        return when {
            targetLevel <= DROP_BAND_END -> 0.00
            targetLevel <= 15 -> 0.40
            targetLevel <= 18 -> 0.60
            targetLevel <= MAX_FINITE_LEVEL -> 0.80
            else -> 1.00
        }
    }
}
```

- [ ] **Step 7: 테스트가 통과하는지 확인한다**

Run:

```bash
cd /c/workAndroid/SwordForge && ./gradlew :core:test
```

Expected: PASS, 13개 테스트 전부 통과.

- [ ] **Step 8: 커밋한다**

```bash
cd /c/workAndroid/SwordForge
git add -A
git commit -m "M1-1: Gradle 스캐폴딩과 확률표 구현

- :core 순수 Kotlin 모듈 구성 (java-library + kotlin jvm, JDK 21)
- Difficulty 난이도 4종: 배수와 상한만 갖고 확률표는 공유
- RateTable: 기준 성공률표, 난이도 배수, 축복서 가산, 98% 상한,
  무한 모드 85% 감쇠와 0.5% 하한, 실패 구간과 파괴 확률
- 테스트 13건: 스펙 표 일치, 단조 감소, 구간 경계, 상한 동작"
```

---

### Task 2: 도메인 값 타입

**Files:**
- Create: `core/src/main/kotlin/com/geomgang/core/Model.kt`
- Test: `core/src/test/kotlin/com/geomgang/core/ModelTest.kt`

**Interfaces:**
- Consumes: `Difficulty` (Task 1)
- Produces:
  - `enum class Item(val id: String, val displayName: String)` — `PREVENT_TICKET/BLESSING_SCROLL/LUCK_CHARM`
  - `enum class WeaponFamily(val id: String, val displayName: String)` — 8종, `companion object { val STARTERS: List<WeaponFamily>; fun fromId(id: String): WeaponFamily }`
  - `data class Sword(val family: WeaponFamily, val level: Int)`
  - `data class Inventory(preventTickets: Int = 0, blessingScrolls: Int = 0, luckCharms: Int = 0)` — `countOf(item: Item): Int`, `plus(item: Item, n: Int): Inventory`, `minus(item: Item, n: Int): Inventory`
  - `data class PendingDestroy(val family: WeaponFamily, val level: Int)`
  - `data class GameState(difficulty, gold: Long = 0, shards: Int = 0, sword: Sword? = null, inventory: Inventory = Inventory(), bestLevel: Int = 0, pendingDestroy: PendingDestroy? = null)`
  - `data class UsedItems(val blessing: Boolean = false, val luckCharm: Boolean = false)` — `companion object { val NONE: UsedItems }`

- [ ] **Step 1: 실패하는 테스트를 작성한다**

`core/src/test/kotlin/com/geomgang/core/ModelTest.kt`:

```kotlin
package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelTest {

    @Test
    fun `계열은 8종이고 기본 해금은 4종이다`() {
        assertEquals(8, WeaponFamily.entries.size)
        assertEquals(4, WeaponFamily.STARTERS.size)
        assertTrue(WeaponFamily.STRAIGHT in WeaponFamily.STARTERS)
        assertTrue(WeaponFamily.DRAGON !in WeaponFamily.STARTERS)
    }

    @Test
    fun `계열 아이디는 서로 겹치지 않는다`() {
        val ids = WeaponFamily.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `아이디로 계열을 찾을 수 있다`() {
        assertEquals(WeaponFamily.DEMON, WeaponFamily.fromId("demon"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `음수 단계 검은 만들 수 없다`() {
        Sword(WeaponFamily.STRAIGHT, -1)
    }

    @Test
    fun `인벤토리는 아이템별 수량을 반환한다`() {
        val inv = Inventory(preventTickets = 3, blessingScrolls = 1, luckCharms = 0)
        assertEquals(3, inv.countOf(Item.PREVENT_TICKET))
        assertEquals(1, inv.countOf(Item.BLESSING_SCROLL))
        assertEquals(0, inv.countOf(Item.LUCK_CHARM))
    }

    @Test
    fun `인벤토리 가감은 새 인스턴스를 만든다`() {
        val before = Inventory(preventTickets = 1)
        val after = before.plus(Item.PREVENT_TICKET, 2)
        assertEquals(1, before.preventTickets)
        assertEquals(3, after.preventTickets)
        assertEquals(1, after.minus(Item.PREVENT_TICKET, 2).preventTickets)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `보유량보다 많이 뺄 수 없다`() {
        Inventory(luckCharms = 1).minus(Item.LUCK_CHARM, 2)
    }

    @Test
    fun `새 게임 상태의 기본값`() {
        val state = GameState(Difficulty.NORMAL)
        assertEquals(0L, state.gold)
        assertEquals(0, state.shards)
        assertNull(state.sword)
        assertNull(state.pendingDestroy)
        assertEquals(0, state.bestLevel)
        assertEquals(Inventory(), state.inventory)
    }

    @Test
    fun `사용 아이템 기본값은 아무것도 쓰지 않음이다`() {
        assertEquals(false, UsedItems.NONE.blessing)
        assertEquals(false, UsedItems.NONE.luckCharm)
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

Run:

```bash
cd /c/workAndroid/SwordForge && ./gradlew :core:test --tests "com.geomgang.core.ModelTest"
```

Expected: 컴파일 실패. `Unresolved reference: WeaponFamily`.

- [ ] **Step 3: `Model.kt`를 구현한다**

```kotlin
package com.geomgang.core

/** 소비 아이템. */
enum class Item(val id: String, val displayName: String) {
    /** 파괴를 1회 무효화한다. 파괴 판정 직후 제한 시간 안에 눌러야 한다. */
    PREVENT_TICKET("prevent", "방지권"),

    /** 다음 1회 성공률을 [RateTable.BLESSING_BONUS] 만큼 올린다. */
    BLESSING_SCROLL("blessing", "축복서"),

    /** 다음 1회 실패해도 하락·파괴가 일어나지 않는다. */
    LUCK_CHARM("luck", "행운부적"),
}

/**
 * 무기 계열. 외형만 다르고 확률·경제에는 전혀 영향을 주지 않는다.
 * 해금 조건은 [Progress.unlockedFamilies]가 관리한다.
 */
enum class WeaponFamily(val id: String, val displayName: String) {
    STRAIGHT("straight", "직검"),
    CURVED("curved", "곡도"),
    GREAT("great", "대검"),
    RAPIER("rapier", "세검"),
    TWIN("twin", "쌍검"),
    DEMON("demon", "마검"),
    HOLY("holy", "성검"),
    DRAGON("dragon", "용검"),
    ;

    companion object {
        /** 업적 없이 처음부터 쓸 수 있는 계열. */
        val STARTERS: List<WeaponFamily> = listOf(STRAIGHT, CURVED, GREAT, RAPIER)

        fun fromId(id: String): WeaponFamily =
            entries.firstOrNull { it.id == id }
                ?: throw IllegalArgumentException("unknown family id: $id")
    }
}

/** 보유 중인 검 한 자루. */
data class Sword(val family: WeaponFamily, val level: Int) {
    init {
        require(level >= 0) { "level must be >= 0, was $level" }
    }
}

/** 소비 아이템 보유량. */
data class Inventory(
    val preventTickets: Int = 0,
    val blessingScrolls: Int = 0,
    val luckCharms: Int = 0,
) {
    init {
        require(preventTickets >= 0) { "preventTickets must be >= 0, was $preventTickets" }
        require(blessingScrolls >= 0) { "blessingScrolls must be >= 0, was $blessingScrolls" }
        require(luckCharms >= 0) { "luckCharms must be >= 0, was $luckCharms" }
    }

    fun countOf(item: Item): Int = when (item) {
        Item.PREVENT_TICKET -> preventTickets
        Item.BLESSING_SCROLL -> blessingScrolls
        Item.LUCK_CHARM -> luckCharms
    }

    fun plus(item: Item, n: Int): Inventory {
        require(n >= 0) { "n must be >= 0, was $n" }
        return when (item) {
            Item.PREVENT_TICKET -> copy(preventTickets = preventTickets + n)
            Item.BLESSING_SCROLL -> copy(blessingScrolls = blessingScrolls + n)
            Item.LUCK_CHARM -> copy(luckCharms = luckCharms + n)
        }
    }

    /** 보유량보다 많이 빼려 하면 init 블록의 require 가 걸려 예외가 난다. */
    fun minus(item: Item, n: Int): Inventory {
        require(n >= 0) { "n must be >= 0, was $n" }
        return when (item) {
            Item.PREVENT_TICKET -> copy(preventTickets = preventTickets - n)
            Item.BLESSING_SCROLL -> copy(blessingScrolls = blessingScrolls - n)
            Item.LUCK_CHARM -> copy(luckCharms = luckCharms - n)
        }
    }
}

/**
 * 파괴 판정이 났지만 방지권/줍기 응답을 아직 받지 못한 상태.
 *
 * 이 값이 세이브에 남아 있는 채로 앱이 다시 켜지면 파괴를 확정 처리한다.
 * 그렇게 하지 않으면 방지권 대기 중 강제 종료로 파괴를 무효화할 수 있다.
 */
data class PendingDestroy(val family: WeaponFamily, val level: Int)

/** 한 모드의 전체 진행 상태. */
data class GameState(
    val difficulty: Difficulty,
    val gold: Long = 0,
    val shards: Int = 0,
    val sword: Sword? = null,
    val inventory: Inventory = Inventory(),
    val bestLevel: Int = 0,
    val pendingDestroy: PendingDestroy? = null,
) {
    init {
        require(gold >= 0) { "gold must be >= 0, was $gold" }
        require(shards >= 0) { "shards must be >= 0, was $shards" }
        require(bestLevel >= 0) { "bestLevel must be >= 0, was $bestLevel" }
    }
}

/** 이번 강화 시도에 함께 사용할 아이템. */
data class UsedItems(
    val blessing: Boolean = false,
    val luckCharm: Boolean = false,
) {
    companion object {
        val NONE: UsedItems = UsedItems()
    }
}
```

- [ ] **Step 4: 테스트가 통과하는지 확인한다**

Run:

```bash
cd /c/workAndroid/SwordForge && ./gradlew :core:test
```

Expected: PASS. RateTableTest 13건 + ModelTest 9건.

- [ ] **Step 5: 커밋한다**

```bash
cd /c/workAndroid/SwordForge
git add -A
git commit -m "M1-2: 도메인 값 타입 구현

- Item 3종, WeaponFamily 8종(기본 해금 4종)
- Sword·Inventory·PendingDestroy·GameState·UsedItems
- 전부 불변 data class, 음수 방어는 init require 로 처리
- PendingDestroy 는 방지권 대기 중 강제 종료 악용을 막기 위한 상태"
```

---

### Task 3: 무기 카탈로그와 도감 엔트리

**Files:**
- Create: `core/src/main/kotlin/com/geomgang/core/WeaponCatalog.kt`
- Test: `core/src/test/kotlin/com/geomgang/core/WeaponCatalogTest.kt`

**Interfaces:**
- Consumes: `Difficulty` (Task 1), `WeaponFamily` (Task 2)
- Produces:
  - `enum class WeaponTier(val id: String, val displayName: String, val minLevel: Int, val maxLevel: Int, val endlessOnly: Boolean)` — 11종
  - `data class CodexEntry(val family: WeaponFamily, val tier: WeaponTier)`
  - `object WeaponCatalog` — `tierFor(level: Int): WeaponTier`, `difficultiesFor(tier: WeaponTier): List<Difficulty>`, `val ENTRIES: List<CodexEntry>` (88개), `const val AURA_MIN_LEVEL = 15`

- [ ] **Step 1: 실패하는 테스트를 작성한다**

`core/src/test/kotlin/com/geomgang/core/WeaponCatalogTest.kt`:

```kotlin
package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeaponCatalogTest {

    @Test
    fun `티어는 11종이고 무한 전용은 3종이다`() {
        assertEquals(11, WeaponTier.entries.size)
        assertEquals(3, WeaponTier.entries.count { it.endlessOnly })
    }

    @Test
    fun `단계별 티어 경계가 스펙과 일치한다`() {
        assertEquals(WeaponTier.RUSTY, WeaponCatalog.tierFor(0))
        assertEquals(WeaponTier.RUSTY, WeaponCatalog.tierFor(2))
        assertEquals(WeaponTier.STEEL, WeaponCatalog.tierFor(3))
        assertEquals(WeaponTier.STEEL, WeaponCatalog.tierFor(5))
        assertEquals(WeaponTier.SILVER, WeaponCatalog.tierFor(6))
        assertEquals(WeaponTier.RUNE, WeaponCatalog.tierFor(11))
        assertEquals(WeaponTier.FLAME, WeaponCatalog.tierFor(12))
        assertEquals(WeaponTier.THUNDER, WeaponCatalog.tierFor(15))
        assertEquals(WeaponTier.DAWN, WeaponCatalog.tierFor(17))
        assertEquals(WeaponTier.BLACK_DRAGON, WeaponCatalog.tierFor(19))
        assertEquals(WeaponTier.BLACK_DRAGON, WeaponCatalog.tierFor(20))
    }

    @Test
    fun `무한 구간 티어 경계가 스펙과 일치한다`() {
        assertEquals(WeaponTier.DRAGON_SCALE, WeaponCatalog.tierFor(21))
        assertEquals(WeaponTier.DRAGON_SCALE, WeaponCatalog.tierFor(25))
        assertEquals(WeaponTier.ABYSS, WeaponCatalog.tierFor(26))
        assertEquals(WeaponTier.ABYSS, WeaponCatalog.tierFor(30))
        assertEquals(WeaponTier.NAMELESS, WeaponCatalog.tierFor(31))
        assertEquals(WeaponTier.NAMELESS, WeaponCatalog.tierFor(9999))
    }

    @Test
    fun `0부터 40까지 어느 단계에도 대응 티어가 있다`() {
        for (level in 0..40) {
            WeaponCatalog.tierFor(level)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `음수 단계에는 티어가 없다`() {
        WeaponCatalog.tierFor(-1)
    }

    @Test
    fun `티어 구간은 빈틈도 겹침도 없다`() {
        val sorted = WeaponTier.entries.sortedBy { it.minLevel }
        assertEquals(0, sorted.first().minLevel)
        for (i in 1 until sorted.size) {
            assertEquals(
                "${sorted[i].id} 앞 구간과 이어지지 않는다",
                sorted[i - 1].maxLevel + 1,
                sorted[i].minLevel,
            )
        }
        assertEquals(Int.MAX_VALUE, sorted.last().maxLevel)
    }

    @Test
    fun `일반 티어는 네 모드 모두에서 얻을 수 있다`() {
        assertEquals(4, WeaponCatalog.difficultiesFor(WeaponTier.RUSTY).size)
        assertEquals(4, WeaponCatalog.difficultiesFor(WeaponTier.BLACK_DRAGON).size)
    }

    @Test
    fun `무한 전용 티어는 무한 모드에서만 얻을 수 있다`() {
        assertEquals(
            listOf(Difficulty.ENDLESS),
            WeaponCatalog.difficultiesFor(WeaponTier.ABYSS),
        )
    }

    @Test
    fun `도감 엔트리는 계열 8종 곱하기 티어 11종으로 88개다`() {
        assertEquals(88, WeaponCatalog.ENTRIES.size)
        assertEquals(88, WeaponCatalog.ENTRIES.toSet().size)
    }

    @Test
    fun `모든 계열이 모든 티어를 하나씩 갖는다`() {
        for (family in WeaponFamily.entries) {
            val tiers = WeaponCatalog.ENTRIES.filter { it.family == family }.map { it.tier }
            assertEquals(WeaponTier.entries.size, tiers.size)
            assertEquals(WeaponTier.entries.toSet(), tiers.toSet())
        }
    }

    @Test
    fun `오라는 15단계부터 그린다`() {
        assertEquals(15, WeaponCatalog.AURA_MIN_LEVEL)
        assertTrue(WeaponTier.THUNDER.minLevel == WeaponCatalog.AURA_MIN_LEVEL)
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

Run:

```bash
cd /c/workAndroid/SwordForge && ./gradlew :core:test --tests "com.geomgang.core.WeaponCatalogTest"
```

Expected: 컴파일 실패. `Unresolved reference: WeaponTier`.

- [ ] **Step 3: `WeaponCatalog.kt`를 구현한다**

```kotlin
package com.geomgang.core

/**
 * 강화 단계에 따라 결정되는 검의 외형 등급.
 *
 * 구간은 [minLevel]..[maxLevel] 이며 빈틈 없이 이어진다.
 * 마지막 티어의 [maxLevel]은 [Int.MAX_VALUE]로 열려 있다.
 */
enum class WeaponTier(
    val id: String,
    val displayName: String,
    val minLevel: Int,
    val maxLevel: Int,
    val endlessOnly: Boolean,
) {
    RUSTY("rusty", "녹슨 검", 0, 2, false),
    STEEL("steel", "강철검", 3, 5, false),
    SILVER("silver", "은장검", 6, 8, false),
    RUNE("rune", "룬검", 9, 11, false),
    FLAME("flame", "화염검", 12, 14, false),
    THUNDER("thunder", "뇌전검", 15, 16, false),
    DAWN("dawn", "여명의 성검", 17, 18, false),
    BLACK_DRAGON("black_dragon", "흑룡참", 19, 20, false),
    DRAGON_SCALE("dragon_scale", "용린참", 21, 25, true),
    ABYSS("abyss", "심연검", 26, 30, true),
    NAMELESS("nameless", "이름 없는 검", 31, Int.MAX_VALUE, true),
}

/** 도감의 칸 하나. 계열 × 티어 조합이다. */
data class CodexEntry(val family: WeaponFamily, val tier: WeaponTier)

/** 무기 외형 정의와 도감 구성. 확률·경제와는 무관하다. */
object WeaponCatalog {

    /** 이 단계부터 검에 오라 레이어를 그린다. */
    const val AURA_MIN_LEVEL: Int = 15

    /** 해당 강화 단계의 외형 티어. */
    fun tierFor(level: Int): WeaponTier {
        require(level >= 0) { "level must be >= 0, was $level" }
        return WeaponTier.entries.first { level >= it.minLevel && level <= it.maxLevel }
    }

    /** 이 티어를 획득할 수 있는 난이도들. */
    fun difficultiesFor(tier: WeaponTier): List<Difficulty> =
        if (tier.endlessOnly) listOf(Difficulty.ENDLESS) else Difficulty.entries.toList()

    /** 도감 전체 칸. 계열 8종 × 티어 11종 = 88칸. */
    val ENTRIES: List<CodexEntry> =
        WeaponFamily.entries.flatMap { family ->
            WeaponTier.entries.map { tier -> CodexEntry(family, tier) }
        }
}
```

- [ ] **Step 4: 테스트가 통과하는지 확인한다**

Run:

```bash
cd /c/workAndroid/SwordForge && ./gradlew :core:test
```

Expected: PASS.

- [ ] **Step 5: 커밋한다**

```bash
cd /c/workAndroid/SwordForge
git add -A
git commit -m "M1-3: 무기 티어와 도감 카탈로그 구현

- WeaponTier 11종(무한 전용 3종 포함), 구간이 0부터 빈틈없이 이어짐
- tierFor 단계→티어 매핑, difficultiesFor 획득 가능 난이도
- 도감 엔트리 88칸(계열 8 x 티어 11) 생성
- 구간 연속성을 테스트로 고정해 티어 추가 시 빈틈을 잡도록 함"
```

---

### Task 4: 조합소 교환식

**Files:**
- Create: `core/src/main/kotlin/com/geomgang/core/Recipes.kt`
- Test: `core/src/test/kotlin/com/geomgang/core/RecipesTest.kt`

**Interfaces:**
- Consumes: `Item`, `Sword`, `GameState`, `WeaponFamily` (Task 2)
- Produces:
  - `sealed interface RecipeReward` — `data class GrantItem(val item: Item, val count: Int)`, `data class GrantSword(val level: Int)`
  - `data class Recipe(val id: String, val displayName: String, val shardCost: Int, val reward: RecipeReward)`
  - `object Recipes` — `const val SWORD5_SHARD_COST = 120`, `val ALL: List<Recipe>`, `fun byId(id: String): Recipe`, `fun canCraft(state: GameState, recipe: Recipe): Boolean`, `fun craft(state: GameState, recipe: Recipe, family: WeaponFamily): GameState`

`craft`가 `family`를 받는 이유: `GrantSword` 보상은 새 검을 주므로 계열을 정해야 한다. `GrantItem` 보상일 때 `family`는 무시된다.

- [ ] **Step 1: 실패하는 테스트를 작성한다**

`core/src/test/kotlin/com/geomgang/core/RecipesTest.kt`:

```kotlin
package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipesTest {

    private fun state(shards: Int, sword: Sword? = null) =
        GameState(Difficulty.NORMAL, shards = shards, sword = sword)

    @Test
    fun `교환식은 5종이고 스펙 조각 가격과 일치한다`() {
        assertEquals(5, Recipes.ALL.size)
        assertEquals(10, Recipes.byId("prevent").shardCost)
        assertEquals(30, Recipes.byId("blessing").shardCost)
        assertEquals(60, Recipes.byId("luck").shardCost)
        assertEquals(120, Recipes.byId("sword5").shardCost)
        assertEquals(400, Recipes.byId("sword10").shardCost)
    }

    @Test
    fun `조각 5검 교환가는 파산 판정에서 쓰는 상수와 같다`() {
        assertEquals(Recipes.SWORD5_SHARD_COST, Recipes.byId("sword5").shardCost)
    }

    @Test
    fun `조각이 모자라면 교환할 수 없다`() {
        val recipe = Recipes.byId("prevent")
        assertFalse(Recipes.canCraft(state(shards = 9), recipe))
        assertTrue(Recipes.canCraft(state(shards = 10), recipe))
    }

    @Test
    fun `아이템 교환은 조각을 차감하고 아이템을 준다`() {
        val before = state(shards = 35)
        val after = Recipes.craft(before, Recipes.byId("blessing"), WeaponFamily.STRAIGHT)
        assertEquals(5, after.shards)
        assertEquals(1, after.inventory.blessingScrolls)
    }

    @Test
    fun `검 교환은 지정한 계열의 검을 준다`() {
        val before = state(shards = 130)
        val after = Recipes.craft(before, Recipes.byId("sword5"), WeaponFamily.DRAGON)
        assertEquals(10, after.shards)
        assertEquals(Sword(WeaponFamily.DRAGON, 5), after.sword)
    }

    @Test
    fun `검 교환으로 최고 기록이 갱신된다`() {
        val before = state(shards = 400)
        val after = Recipes.craft(before, Recipes.byId("sword10"), WeaponFamily.STRAIGHT)
        assertEquals(10, after.bestLevel)
    }

    @Test
    fun `이미 검이 있으면 검 교환을 할 수 없다`() {
        val holding = state(shards = 400, sword = Sword(WeaponFamily.STRAIGHT, 3))
        assertFalse(Recipes.canCraft(holding, Recipes.byId("sword5")))
        // 아이템 교환은 검을 들고 있어도 가능하다
        assertTrue(Recipes.canCraft(holding, Recipes.byId("prevent")))
    }

    @Test(expected = IllegalStateException::class)
    fun `조건을 만족하지 않으면 교환은 예외를 던진다`() {
        Recipes.craft(state(shards = 1), Recipes.byId("prevent"), WeaponFamily.STRAIGHT)
    }

    @Test
    fun `교환식 아이디는 서로 겹치지 않는다`() {
        val ids = Recipes.ALL.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

Run:

```bash
cd /c/workAndroid/SwordForge && ./gradlew :core:test --tests "com.geomgang.core.RecipesTest"
```

Expected: 컴파일 실패. `Unresolved reference: Recipes`.

- [ ] **Step 3: `Recipes.kt`를 구현한다**

```kotlin
package com.geomgang.core

/** 조합소 교환의 보상. */
sealed interface RecipeReward {
    data class GrantItem(val item: Item, val count: Int) : RecipeReward
    data class GrantSword(val level: Int) : RecipeReward
}

/** 조각으로 무언가를 바꾸는 교환식 하나. */
data class Recipe(
    val id: String,
    val displayName: String,
    val shardCost: Int,
    val reward: RecipeReward,
)

/**
 * 조합소.
 *
 * 조각은 골드와 분리된 화폐다. 골드가 바닥나도 주워 모은 조각으로 재기할 수 있어야
 * 파괴가 곧 게임 종료가 되지 않는다.
 */
object Recipes {

    /** +5 검 교환가. [Economy.needsBailout]이 파산 판정 기준으로 함께 쓴다. */
    const val SWORD5_SHARD_COST: Int = 120

    val ALL: List<Recipe> = listOf(
        Recipe("prevent", "방지권", 10, RecipeReward.GrantItem(Item.PREVENT_TICKET, 1)),
        Recipe("blessing", "축복서", 30, RecipeReward.GrantItem(Item.BLESSING_SCROLL, 1)),
        Recipe("luck", "행운부적", 60, RecipeReward.GrantItem(Item.LUCK_CHARM, 1)),
        Recipe("sword5", "+5 검", SWORD5_SHARD_COST, RecipeReward.GrantSword(5)),
        Recipe("sword10", "+10 검", 400, RecipeReward.GrantSword(10)),
    )

    fun byId(id: String): Recipe =
        ALL.firstOrNull { it.id == id }
            ?: throw IllegalArgumentException("unknown recipe id: $id")

    fun canCraft(state: GameState, recipe: Recipe): Boolean {
        if (state.shards < recipe.shardCost) return false
        // 검을 주는 교환은 빈손일 때만 가능하다. 들고 있는 검을 덮어쓰지 않는다.
        if (recipe.reward is RecipeReward.GrantSword && state.sword != null) return false
        return true
    }

    fun craft(state: GameState, recipe: Recipe, family: WeaponFamily): GameState {
        check(canCraft(state, recipe)) { "cannot craft ${recipe.id} in this state" }
        val paid = state.copy(shards = state.shards - recipe.shardCost)
        return when (val reward = recipe.reward) {
            is RecipeReward.GrantItem ->
                paid.copy(inventory = paid.inventory.plus(reward.item, reward.count))

            is RecipeReward.GrantSword ->
                paid.copy(
                    sword = Sword(family, reward.level),
                    bestLevel = maxOf(paid.bestLevel, reward.level),
                )
        }
    }
}
```

- [ ] **Step 4: 테스트가 통과하는지 확인한다**

Run:

```bash
cd /c/workAndroid/SwordForge && ./gradlew :core:test
```

Expected: PASS.

- [ ] **Step 5: 커밋한다**

```bash
cd /c/workAndroid/SwordForge
git add -A
git commit -m "M1-4: 조합소 교환식 구현

- 교환식 5종: 방지권 10 / 축복서 30 / 행운부적 60 / +5검 120 / +10검 400
- 검 보상 교환은 빈손일 때만 가능 (보유 검 덮어쓰기 방지)
- 검 교환 시 최고 기록 갱신
- SWORD5_SHARD_COST 를 상수로 노출해 파산 판정이 같은 값을 참조하도록 함"
```

---

### Task 5: 경제와 파산 구제

**Files:**
- Create: `core/src/main/kotlin/com/geomgang/core/Economy.kt`
- Test: `core/src/test/kotlin/com/geomgang/core/EconomyTest.kt`

**Interfaces:**
- Consumes: `Item`, `Sword`, `GameState`, `WeaponFamily` (Task 2), `Recipes.SWORD5_SHARD_COST` (Task 4)
- Produces:
  - `object Economy` — 상수 `BASE_SWORD_PRICE=100L`, `PREVENT_TICKET_PRICE=800L`, `BLESSING_SCROLL_PRICE=1200L`, `LUCK_CHARM_PRICE=2000L`, `BAILOUT_GOLD=300L`
  - `upgradeCost(currentLevel: Int): Long`, `sellPrice(level: Int): Long`, `priceOf(item: Item): Long`
  - `canBuyItem(state, item): Boolean`, `buyItem(state, item): GameState`
  - `canBuySword(state): Boolean`, `buySword(state, family): GameState`
  - `canSellSword(state): Boolean`, `sellSword(state): GameState`
  - `needsBailout(state): Boolean`, `applyBailoutIfNeeded(state): GameState`

- [ ] **Step 1: 실패하는 테스트를 작성한다**

`core/src/test/kotlin/com/geomgang/core/EconomyTest.kt`:

```kotlin
package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EconomyTest {

    private fun state(
        gold: Long = 0,
        shards: Int = 0,
        sword: Sword? = null,
        pending: PendingDestroy? = null,
    ) = GameState(
        Difficulty.NORMAL,
        gold = gold,
        shards = shards,
        sword = sword,
        pendingDestroy = pending,
    )

    @Test
    fun `강화 비용이 스펙 표와 일치한다`() {
        // round(30 * 1.45^currentLevel)
        assertEquals(30L, Economy.upgradeCost(0))
        assertEquals(133L, Economy.upgradeCost(4))
        assertEquals(850L, Economy.upgradeCost(9))
        assertEquals(5448L, Economy.upgradeCost(14))
        assertEquals(34923L, Economy.upgradeCost(19))
    }

    @Test
    fun `판매가가 스펙 표와 일치한다`() {
        // round(60 * 1.6^level)
        assertEquals(60L, Economy.sellPrice(0))
        assertEquals(96L, Economy.sellPrice(1))
        assertEquals(629L, Economy.sellPrice(5))
        assertEquals(6597L, Economy.sellPrice(10))
        assertEquals(69175L, Economy.sellPrice(15))
        assertEquals(725355L, Economy.sellPrice(20))
    }

    @Test
    fun `판매가는 비용보다 빠르게 증가한다`() {
        // 스펙의 핵심 불변식. 고단계 도전이 계산이 서는 도박이 되려면 이게 성립해야 한다.
        for (level in 1..20) {
            val priceRatio = Economy.sellPrice(level).toDouble() / Economy.sellPrice(level - 1)
            val costRatio = Economy.upgradeCost(level).toDouble() / Economy.upgradeCost(level - 1)
            assertTrue("level=$level price=$priceRatio cost=$costRatio", priceRatio > costRatio)
        }
    }

    @Test
    fun `무한 모드 고단계 판매가가 Long 범위에서 계산된다`() {
        assertTrue(Economy.sellPrice(40) > Economy.sellPrice(30))
        assertTrue(Economy.sellPrice(40) > 0)
    }

    @Test
    fun `상점 가격이 스펙과 일치한다`() {
        assertEquals(800L, Economy.priceOf(Item.PREVENT_TICKET))
        assertEquals(1200L, Economy.priceOf(Item.BLESSING_SCROLL))
        assertEquals(2000L, Economy.priceOf(Item.LUCK_CHARM))
        assertEquals(100L, Economy.BASE_SWORD_PRICE)
    }

    @Test
    fun `아이템 구매는 골드를 차감하고 아이템을 준다`() {
        val after = Economy.buyItem(state(gold = 1000), Item.PREVENT_TICKET)
        assertEquals(200L, after.gold)
        assertEquals(1, after.inventory.preventTickets)
    }

    @Test
    fun `골드가 모자라면 아이템을 살 수 없다`() {
        assertFalse(Economy.canBuyItem(state(gold = 799), Item.PREVENT_TICKET))
        assertTrue(Economy.canBuyItem(state(gold = 800), Item.PREVENT_TICKET))
    }

    @Test
    fun `기본 검 구매는 지정한 계열의 0단계 검을 준다`() {
        val after = Economy.buySword(state(gold = 300), WeaponFamily.CURVED)
        assertEquals(200L, after.gold)
        assertEquals(Sword(WeaponFamily.CURVED, 0), after.sword)
    }

    @Test
    fun `검을 들고 있으면 또 살 수 없다`() {
        val holding = state(gold = 999, sword = Sword(WeaponFamily.STRAIGHT, 1))
        assertFalse(Economy.canBuySword(holding))
    }

    @Test
    fun `검 판매는 골드를 주고 검을 없앤다`() {
        val before = state(gold = 0, sword = Sword(WeaponFamily.STRAIGHT, 10))
        val after = Economy.sellSword(before)
        assertEquals(6597L, after.gold)
        assertNull(after.sword)
    }

    @Test
    fun `검이 없으면 팔 수 없다`() {
        assertFalse(Economy.canSellSword(state()))
    }

    @Test
    fun `파산 판정은 세 조건이 모두 성립할 때만 참이다`() {
        // 검 없음 + 골드 100 미만 + 조각 120 미만
        assertTrue(Economy.needsBailout(state(gold = 99, shards = 119)))
        // 검이 있으면 아니다
        assertFalse(
            Economy.needsBailout(
                state(gold = 0, shards = 0, sword = Sword(WeaponFamily.STRAIGHT, 0)),
            ),
        )
        // 검을 살 골드가 있으면 아니다
        assertFalse(Economy.needsBailout(state(gold = 100, shards = 0)))
        // +5 검을 바꿀 조각이 있으면 아니다
        assertFalse(Economy.needsBailout(state(gold = 0, shards = 120)))
    }

    @Test
    fun `파괴 대기 중에는 파산 구제가 발동하지 않는다`() {
        val pending = PendingDestroy(WeaponFamily.STRAIGHT, 14)
        assertFalse(Economy.needsBailout(state(gold = 0, shards = 0, pending = pending)))
    }

    @Test
    fun `파산 구제는 골드를 300으로 채운다`() {
        val after = Economy.applyBailoutIfNeeded(state(gold = 12, shards = 3))
        assertEquals(300L, after.gold)
        // 검은 주지 않는다. 상점에서 원하는 계열을 직접 고르게 한다.
        assertNull(after.sword)
    }

    @Test
    fun `파산이 아니면 구제는 상태를 바꾸지 않는다`() {
        val rich = state(gold = 5000)
        assertEquals(rich, Economy.applyBailoutIfNeeded(rich))
    }

    @Test
    fun `구제 반복으로 골드를 불릴 수 없다`() {
        // 사고(100) 되파는(60) 순환은 한 바퀴에 40 손해이고 구제 상한이 300이므로
        // 몇 바퀴를 돌려도 골드가 300을 넘지 못한다.
        var s = state(gold = 0)
        repeat(50) {
            s = Economy.applyBailoutIfNeeded(s)
            if (Economy.canBuySword(s)) s = Economy.buySword(s, WeaponFamily.STRAIGHT)
            if (Economy.canSellSword(s)) s = Economy.sellSword(s)
        }
        assertTrue("gold=${s.gold}", s.gold <= Economy.BAILOUT_GOLD)
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

Run:

```bash
cd /c/workAndroid/SwordForge && ./gradlew :core:test --tests "com.geomgang.core.EconomyTest"
```

Expected: 컴파일 실패. `Unresolved reference: Economy`.

- [ ] **Step 3: `Economy.kt`를 구현한다**

```kotlin
package com.geomgang.core

import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * 돈의 흐름.
 *
 * 수입은 검 판매와 조각 조합, 지출은 강화 비용과 아이템 구매 두 갈래씩이다.
 * 판매가 지수가 비용 지수보다 크다는 것이 이 게임 경제의 핵심 불변식이며,
 * 그 덕분에 고단계 도전이 무모한 짓이 아니라 계산이 서는 도박이 된다.
 */
object Economy {

    /** 상점에서 파는 +0 검의 가격. */
    const val BASE_SWORD_PRICE: Long = 100

    const val PREVENT_TICKET_PRICE: Long = 800
    const val BLESSING_SCROLL_PRICE: Long = 1_200
    const val LUCK_CHARM_PRICE: Long = 2_000

    /** 파산 구제가 채워 주는 골드. */
    const val BAILOUT_GOLD: Long = 300

    private const val COST_BASE = 30.0
    private const val COST_GROWTH = 1.45
    private const val PRICE_BASE = 60.0
    private const val PRICE_GROWTH = 1.60

    /** [currentLevel] 검을 한 단계 올리는 데 드는 비용. */
    fun upgradeCost(currentLevel: Int): Long {
        require(currentLevel >= 0) { "currentLevel must be >= 0, was $currentLevel" }
        return (COST_BASE * COST_GROWTH.pow(currentLevel.toDouble())).roundToLong()
    }

    /** [level] 검을 팔았을 때 받는 골드. */
    fun sellPrice(level: Int): Long {
        require(level >= 0) { "level must be >= 0, was $level" }
        return (PRICE_BASE * PRICE_GROWTH.pow(level.toDouble())).roundToLong()
    }

    fun priceOf(item: Item): Long = when (item) {
        Item.PREVENT_TICKET -> PREVENT_TICKET_PRICE
        Item.BLESSING_SCROLL -> BLESSING_SCROLL_PRICE
        Item.LUCK_CHARM -> LUCK_CHARM_PRICE
    }

    fun canBuyItem(state: GameState, item: Item): Boolean =
        state.gold >= priceOf(item)

    fun buyItem(state: GameState, item: Item): GameState {
        check(canBuyItem(state, item)) { "not enough gold for $item" }
        return state.copy(
            gold = state.gold - priceOf(item),
            inventory = state.inventory.plus(item, 1),
        )
    }

    fun canBuySword(state: GameState): Boolean =
        state.sword == null && state.pendingDestroy == null && state.gold >= BASE_SWORD_PRICE

    fun buySword(state: GameState, family: WeaponFamily): GameState {
        check(canBuySword(state)) { "cannot buy a sword in this state" }
        return state.copy(
            gold = state.gold - BASE_SWORD_PRICE,
            sword = Sword(family, 0),
        )
    }

    fun canSellSword(state: GameState): Boolean =
        state.sword != null && state.pendingDestroy == null

    fun sellSword(state: GameState): GameState {
        val sword = state.sword
        check(canSellSword(state) && sword != null) { "no sword to sell" }
        return state.copy(gold = state.gold + sellPrice(sword.level), sword = null)
    }

    /**
     * 아무것도 할 수 없는 상태인지.
     *
     * 검이 없고, 검을 살 골드도 없고, +5 검으로 바꿀 조각도 없을 때만 참이다.
     * 파괴 연출이 진행 중(pendingDestroy)이면 아직 결과가 확정된 게 아니므로 판정하지 않는다.
     */
    fun needsBailout(state: GameState): Boolean =
        state.sword == null &&
            state.pendingDestroy == null &&
            state.gold < BASE_SWORD_PRICE &&
            state.shards < Recipes.SWORD5_SHARD_COST

    /**
     * 파산 상태면 골드를 [BAILOUT_GOLD]로 채운다. 검은 주지 않는다.
     *
     * 검이 아니라 골드를 주는 이유: 검만 주면 골드가 0이라 강화 비용조차 못 내고,
     * 골드로 주면 플레이어가 원하는 계열을 골라 살 수 있다.
     * 사고 되파는 순환은 한 바퀴에 40골드씩 손해라 이 장치로 골드를 벌 수는 없다.
     */
    fun applyBailoutIfNeeded(state: GameState): GameState =
        if (needsBailout(state)) state.copy(gold = BAILOUT_GOLD) else state
}
```

- [ ] **Step 4: 테스트가 통과하는지 확인한다**

Run:

```bash
cd /c/workAndroid/SwordForge && ./gradlew :core:test
```

Expected: PASS.

강화 비용·판매가 기대값이 어긋나면 `roundToLong()` 결과를 먼저 출력해 확인하고, **테스트 기대값이 아니라 스펙 표를 실제 계산값으로 맞춘다.** 공식이 진실이고 표는 그 표시다.

- [ ] **Step 5: 커밋한다**

```bash
cd /c/workAndroid/SwordForge
git add -A
git commit -m "M1-5: 경제와 파산 구제 구현

- 강화 비용 30*1.45^n, 판매가 60*1.6^n
- 판매가 지수 > 비용 지수 불변식을 테스트로 고정
- 상점: 기본 검 100, 방지권 800, 축복서 1200, 행운부적 2000
- 파산 구제: 검 없음 + 골드<100 + 조각<120 일 때만 골드를 300으로 채움
- 구제 반복으로 골드를 불릴 수 없음을 50회 순환 테스트로 검증"
```

---

### Task 6: 강화 판정 엔진

**Files:**
- Create: `core/src/main/kotlin/com/geomgang/core/ForgeEngine.kt`
- Create: `core/src/test/kotlin/com/geomgang/core/ScriptedRandom.kt`
- Test: `core/src/test/kotlin/com/geomgang/core/ForgeEngineTest.kt`

**Interfaces:**
- Consumes: `RateTable`, `FailureBand`, `Difficulty` (Task 1), `GameState`, `Sword`, `Inventory`, `Item`, `UsedItems`, `PendingDestroy` (Task 2), `Economy.upgradeCost` (Task 5)
- Produces:
  - `sealed interface ForgeResult { val state: GameState }` — `Success(state, newLevel: Int)`, `Stay(state, level: Int)`, `Drop(state, newLevel: Int)`, `Destroyed(state, lostLevel: Int, preventable: Boolean)`
  - `object ForgeEngine` — `const val AUTO_FORGE_MAX_LEVEL = 4`, `canAttempt(state: GameState, items: UsedItems): Boolean`, `canAutoForge(state: GameState): Boolean`, `attempt(state: GameState, items: UsedItems, rng: Random): ForgeResult`
- 테스트 도구: `class ScriptedRandom(vararg values: Double) : Random()`

`canAutoForge`가 `:core`에 있는 이유: 자동 강화를 안전구간으로 제한하는 것은 UI 편의가 아니라 게임 규칙이다. 화면 쪽에서 단계를 비교하게 두면 규칙이 UI로 샌다.

**난수 소비 순서 계약** — 테스트가 여기에 의존한다.
1. 첫 번째 `nextDouble()`: 성공 판정
2. 두 번째 `nextDouble()`: 파괴/하락 판정 (파괴 가능 구간에서 실패했을 때만 호출)

- [ ] **Step 1: 테스트용 난수 도구를 만든다**

`core/src/test/kotlin/com/geomgang/core/ScriptedRandom.kt`:

```kotlin
package com.geomgang.core

import kotlin.random.Random

/**
 * 정해 둔 값을 순서대로 돌려주는 테스트용 난수.
 *
 * 강화 판정처럼 분기가 확률로 갈리는 코드를 결정적으로 검증하기 위한 도구다.
 * [nextDouble] 외의 호출은 테스트가 의도치 않은 경로를 타고 있다는 뜻이므로 예외를 던진다.
 */
class ScriptedRandom(private vararg val values: Double) : Random() {

    private var index = 0

    /** 지금까지 소비한 값의 개수. */
    val consumed: Int get() = index

    override fun nextBits(bitCount: Int): Int =
        throw UnsupportedOperationException("ScriptedRandom only supports nextDouble()")

    override fun nextDouble(): Double {
        check(index < values.size) {
            "ScriptedRandom exhausted: ${values.size} values were scripted but more were requested"
        }
        return values[index++]
    }
}
```

- [ ] **Step 2: 실패하는 테스트를 작성한다**

`core/src/test/kotlin/com/geomgang/core/ForgeEngineTest.kt`:

```kotlin
package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ForgeEngineTest {

    private fun state(
        level: Int,
        gold: Long = 1_000_000,
        difficulty: Difficulty = Difficulty.NORMAL,
        inventory: Inventory = Inventory(),
        family: WeaponFamily = WeaponFamily.STRAIGHT,
    ) = GameState(
        difficulty = difficulty,
        gold = gold,
        sword = Sword(family, level),
        inventory = inventory,
        bestLevel = level,
    )

    // --- canAttempt ---

    @Test
    fun `검이 없으면 강화할 수 없다`() {
        val empty = GameState(Difficulty.NORMAL, gold = 1_000_000)
        assertFalse(ForgeEngine.canAttempt(empty, UsedItems.NONE))
    }

    @Test
    fun `골드가 비용보다 적으면 강화할 수 없다`() {
        assertFalse(ForgeEngine.canAttempt(state(level = 0, gold = 29), UsedItems.NONE))
        assertTrue(ForgeEngine.canAttempt(state(level = 0, gold = 30), UsedItems.NONE))
    }

    @Test
    fun `상한에 도달하면 더 강화할 수 없다`() {
        assertFalse(ForgeEngine.canAttempt(state(level = 20), UsedItems.NONE))
        // 무한 모드는 상한이 없다
        assertTrue(
            ForgeEngine.canAttempt(
                state(level = 20, difficulty = Difficulty.ENDLESS),
                UsedItems.NONE,
            ),
        )
    }

    @Test
    fun `없는 아이템은 사용 지정할 수 없다`() {
        val s = state(level = 0)
        assertFalse(ForgeEngine.canAttempt(s, UsedItems(blessing = true)))
        assertFalse(ForgeEngine.canAttempt(s, UsedItems(luckCharm = true)))
        val stocked = state(level = 0, inventory = Inventory(blessingScrolls = 1, luckCharms = 1))
        assertTrue(ForgeEngine.canAttempt(stocked, UsedItems(blessing = true, luckCharm = true)))
    }

    @Test
    fun `파괴 대기 중에는 강화할 수 없다`() {
        val pending = state(level = 5).copy(
            sword = null,
            pendingDestroy = PendingDestroy(WeaponFamily.STRAIGHT, 14),
        )
        assertFalse(ForgeEngine.canAttempt(pending, UsedItems.NONE))
    }

    // --- canAutoForge ---

    @Test
    fun `자동 강화는 안전구간에서만 허용된다`() {
        // +0~+4 에서 시도하면 목표가 +1~+5 라 실패해도 단계가 유지된다
        for (level in 0..4) {
            assertTrue("level=$level", ForgeEngine.canAutoForge(state(level = level)))
        }
        // +5 부터는 실패 시 하락하므로 자동화하지 않는다
        assertFalse(ForgeEngine.canAutoForge(state(level = 5)))
        assertFalse(ForgeEngine.canAutoForge(state(level = 12)))
    }

    @Test
    fun `자동 강화 한계는 안전구간의 끝과 맞물려 있다`() {
        // 이 둘이 어긋나면 자동 강화가 하락 구간을 건드리게 된다
        assertEquals(RateTable.SAFE_BAND_END - 1, ForgeEngine.AUTO_FORGE_MAX_LEVEL)
    }

    @Test
    fun `강화 자체가 불가능하면 자동 강화도 불가능하다`() {
        assertFalse(ForgeEngine.canAutoForge(state(level = 0, gold = 0)))
        assertFalse(ForgeEngine.canAutoForge(GameState(Difficulty.NORMAL, gold = 1_000_000)))
    }

    // --- 성공 ---

    @Test
    fun `성공하면 단계가 오르고 비용이 빠진다`() {
        val before = state(level = 3, gold = 1000)
        // 일반 +4 성공률 0.80, 난수 0.5 는 그 아래이므로 성공
        val result = ForgeEngine.attempt(before, UsedItems.NONE, ScriptedRandom(0.5))
        assertTrue(result is ForgeResult.Success)
        result as ForgeResult.Success
        assertEquals(4, result.newLevel)
        assertEquals(4, result.state.sword?.level)
        assertEquals(1000L - Economy.upgradeCost(3), result.state.gold)
    }

    @Test
    fun `성공하면 최고 기록이 갱신된다`() {
        val before = state(level = 3).copy(bestLevel = 3)
        val result = ForgeEngine.attempt(before, UsedItems.NONE, ScriptedRandom(0.1))
        assertEquals(4, result.state.bestLevel)
    }

    @Test
    fun `하락 후 재상승은 최고 기록을 낮추지 않는다`() {
        val before = state(level = 6).copy(bestLevel = 12)
        val result = ForgeEngine.attempt(before, UsedItems.NONE, ScriptedRandom(0.1))
        assertEquals(12, result.state.bestLevel)
    }

    // --- 실패: 구간별 ---

    @Test
    fun `안전구간 실패는 단계를 유지한다`() {
        val before = state(level = 4)
        // 일반 +5 성공률 0.75, 난수 0.9 는 실패
        val result = ForgeEngine.attempt(before, UsedItems.NONE, ScriptedRandom(0.9))
        assertTrue(result is ForgeResult.Stay)
        assertEquals(4, result.state.sword?.level)
    }

    @Test
    fun `하락구간 실패는 한 단계 떨어뜨린다`() {
        val before = state(level = 8)
        // 일반 +9 성공률 0.47, 난수 0.9 는 실패
        val result = ForgeEngine.attempt(before, UsedItems.NONE, ScriptedRandom(0.9))
        assertTrue(result is ForgeResult.Drop)
        result as ForgeResult.Drop
        assertEquals(7, result.newLevel)
        assertEquals(7, result.state.sword?.level)
    }

    @Test
    fun `파괴구간 실패는 두 번째 난수로 파괴와 하락이 갈린다`() {
        val before = state(level = 13)
        // 일반 +14 성공률 0.19 → 0.9 로 실패. 파괴 확률 0.40.
        val destroyed = ForgeEngine.attempt(before, UsedItems.NONE, ScriptedRandom(0.9, 0.1))
        assertTrue(destroyed is ForgeResult.Destroyed)

        val dropped = ForgeEngine.attempt(before, UsedItems.NONE, ScriptedRandom(0.9, 0.9))
        assertTrue(dropped is ForgeResult.Drop)
    }

    @Test
    fun `파괴되면 검이 사라지고 파괴 대기 상태가 남는다`() {
        val before = state(level = 13, family = WeaponFamily.DRAGON)
        val result = ForgeEngine.attempt(before, UsedItems.NONE, ScriptedRandom(0.9, 0.1))
        result as ForgeResult.Destroyed
        assertEquals(13, result.lostLevel)
        assertNull(result.state.sword)
        assertEquals(PendingDestroy(WeaponFamily.DRAGON, 13), result.state.pendingDestroy)
    }

    @Test
    fun `방지권이 있어야 되살릴 수 있는 파괴로 표시된다`() {
        val without = ForgeEngine.attempt(
            state(level = 13),
            UsedItems.NONE,
            ScriptedRandom(0.9, 0.1),
        ) as ForgeResult.Destroyed
        assertFalse(without.preventable)

        val with = ForgeEngine.attempt(
            state(level = 13, inventory = Inventory(preventTickets = 1)),
            UsedItems.NONE,
            ScriptedRandom(0.9, 0.1),
        ) as ForgeResult.Destroyed
        assertTrue(with.preventable)
    }

    @Test
    fun `무한 구간 실패는 두 번째 난수와 무관하게 파괴다`() {
        val before = state(level = 21, difficulty = Difficulty.ENDLESS)
        val result = ForgeEngine.attempt(before, UsedItems.NONE, ScriptedRandom(0.9, 0.99))
        assertTrue(result is ForgeResult.Destroyed)
    }

    // --- 아이템 ---

    @Test
    fun `축복서는 성공률을 올리고 소모된다`() {
        val before = state(level = 9, inventory = Inventory(blessingScrolls = 1))
        // 일반 +10 성공률 0.40, 축복서로 0.50. 난수 0.45 는 축복서가 있어야 성공한다.
        val result = ForgeEngine.attempt(before, UsedItems(blessing = true), ScriptedRandom(0.45))
        assertTrue(result is ForgeResult.Success)
        assertEquals(0, result.state.inventory.blessingScrolls)
    }

    @Test
    fun `축복서 없이 같은 난수면 실패한다`() {
        val before = state(level = 9)
        val result = ForgeEngine.attempt(before, UsedItems.NONE, ScriptedRandom(0.45))
        assertFalse(result is ForgeResult.Success)
    }

    @Test
    fun `행운부적은 파괴구간 실패를 유지로 바꾼다`() {
        val before = state(level = 19, inventory = Inventory(luckCharms = 1))
        // 파괴 확률 0.80 구간이지만 부적이 실패 자체의 결과를 무효화한다
        val result = ForgeEngine.attempt(before, UsedItems(luckCharm = true), ScriptedRandom(0.99))
        assertTrue(result is ForgeResult.Stay)
        assertEquals(19, result.state.sword?.level)
        assertEquals(0, result.state.inventory.luckCharms)
    }

    @Test
    fun `행운부적은 성공했을 때도 소모된다`() {
        val before = state(level = 3, inventory = Inventory(luckCharms = 1))
        val result = ForgeEngine.attempt(before, UsedItems(luckCharm = true), ScriptedRandom(0.1))
        assertTrue(result is ForgeResult.Success)
        assertEquals(0, result.state.inventory.luckCharms)
    }

    @Test
    fun `행운부적을 쓰면 파괴 판정 난수를 소비하지 않는다`() {
        val before = state(level = 19, inventory = Inventory(luckCharms = 1))
        val rng = ScriptedRandom(0.99)
        ForgeEngine.attempt(before, UsedItems(luckCharm = true), rng)
        assertEquals(1, rng.consumed)
    }

    // --- 방어 ---

    @Test(expected = IllegalStateException::class)
    fun `조건을 만족하지 않는 상태로 시도하면 예외가 난다`() {
        ForgeEngine.attempt(state(level = 0, gold = 0), UsedItems.NONE, ScriptedRandom(0.1))
    }

    @Test
    fun `0단계 하락구간은 존재하지 않으므로 단계가 음수로 가지 않는다`() {
        // +6 시도(현재 5)에서 실패하면 4로 떨어진다. 0 아래로는 어떤 경우에도 내려가지 않는다.
        var s = state(level = 5)
        repeat(10) {
            if (ForgeEngine.canAttempt(s, UsedItems.NONE)) {
                s = ForgeEngine.attempt(s, UsedItems.NONE, ScriptedRandom(0.99, 0.99)).state
            }
            assertTrue((s.sword?.level ?: 0) >= 0)
        }
    }
}
```

- [ ] **Step 3: 테스트가 실패하는지 확인한다**

Run:

```bash
cd /c/workAndroid/SwordForge && ./gradlew :core:test --tests "com.geomgang.core.ForgeEngineTest"
```

Expected: 컴파일 실패. `Unresolved reference: ForgeEngine`.

- [ ] **Step 4: `ForgeEngine.kt`를 구현한다**

```kotlin
package com.geomgang.core

import kotlin.random.Random

/**
 * 강화 1회의 결과.
 *
 * [state]는 결과가 이미 반영된 새 상태다. 비용 차감과 아이템 소모도 포함되어 있다.
 */
sealed interface ForgeResult {

    val state: GameState

    /** 단계가 올랐다. */
    data class Success(override val state: GameState, val newLevel: Int) : ForgeResult

    /** 실패했지만 단계는 그대로다. 안전구간이거나 행운부적을 썼을 때. */
    data class Stay(override val state: GameState, val level: Int) : ForgeResult

    /** 실패해서 한 단계 떨어졌다. */
    data class Drop(override val state: GameState, val newLevel: Int) : ForgeResult

    /**
     * 파괴됐다. [state]의 검은 이미 null 이고 `pendingDestroy`가 채워져 있다.
     *
     * @property preventable 방지권을 갖고 있어 되살릴 수 있는지
     */
    data class Destroyed(
        override val state: GameState,
        val lostLevel: Int,
        val preventable: Boolean,
    ) : ForgeResult
}

/**
 * 강화 판정.
 *
 * 확률 숫자는 [RateTable], 비용은 [Economy]에만 있다. 여기에는 규칙의 조합만 둔다.
 * 밸런스를 고칠 때 고칠 곳을 한 군데로 유지하기 위해서다.
 *
 * 난수 소비 순서는 테스트가 의존하는 계약이다.
 * 1. 성공 판정
 * 2. 파괴/하락 판정 (파괴 가능 구간에서 부적 없이 실패했을 때만)
 */
object ForgeEngine {

    /**
     * 자동 강화가 허용되는 최대 현재 단계.
     *
     * 이 단계에서 시도하면 목표가 안전구간의 끝([RateTable.SAFE_BAND_END])이라
     * 실패해도 단계가 유지된다. 하락·파괴가 걸린 구간을 자동화하면
     * 그 구간의 긴장이 사라지고 게임이 남지 않는다.
     */
    const val AUTO_FORGE_MAX_LEVEL: Int = RateTable.SAFE_BAND_END - 1

    fun canAttempt(state: GameState, items: UsedItems): Boolean {
        val sword = state.sword ?: return false
        if (state.pendingDestroy != null) return false

        val max = state.difficulty.maxLevel
        if (max != null && sword.level >= max) return false

        if (items.blessing && state.inventory.blessingScrolls <= 0) return false
        if (items.luckCharm && state.inventory.luckCharms <= 0) return false

        return state.gold >= Economy.upgradeCost(sword.level)
    }

    /** 자동 강화 루프가 한 번 더 돌아도 되는지. 안전구간을 벗어나면 멈춘다. */
    fun canAutoForge(state: GameState): Boolean {
        val sword = state.sword ?: return false
        if (sword.level > AUTO_FORGE_MAX_LEVEL) return false
        return canAttempt(state, UsedItems.NONE)
    }

    fun attempt(state: GameState, items: UsedItems, rng: Random): ForgeResult {
        check(canAttempt(state, items)) {
            "attempt() called on a state that fails canAttempt()"
        }
        val sword = requireNotNull(state.sword)
        val targetLevel = sword.level + 1

        var inventory = state.inventory
        if (items.blessing) inventory = inventory.minus(Item.BLESSING_SCROLL, 1)
        if (items.luckCharm) inventory = inventory.minus(Item.LUCK_CHARM, 1)

        val paid = state.copy(
            gold = state.gold - Economy.upgradeCost(sword.level),
            inventory = inventory,
        )

        val successRate = RateTable.successRate(state.difficulty, targetLevel, items.blessing)
        if (rng.nextDouble() < successRate) {
            return ForgeResult.Success(
                state = paid.copy(
                    sword = sword.copy(level = targetLevel),
                    bestLevel = maxOf(paid.bestLevel, targetLevel),
                ),
                newLevel = targetLevel,
            )
        }

        // 행운부적은 실패의 결과 자체를 무효화한다. 파괴 판정 난수도 소비하지 않는다.
        if (items.luckCharm) {
            return ForgeResult.Stay(paid, sword.level)
        }

        return when (RateTable.failureBand(targetLevel)) {
            FailureBand.STAY -> ForgeResult.Stay(paid, sword.level)

            FailureBand.DROP -> drop(paid, sword)

            FailureBand.DESTROY_OR_DROP ->
                if (rng.nextDouble() < RateTable.destroyChance(targetLevel)) {
                    ForgeResult.Destroyed(
                        state = paid.copy(
                            sword = null,
                            pendingDestroy = PendingDestroy(sword.family, sword.level),
                        ),
                        lostLevel = sword.level,
                        preventable = paid.inventory.preventTickets > 0,
                    )
                } else {
                    drop(paid, sword)
                }
        }
    }

    private fun drop(paid: GameState, sword: Sword): ForgeResult.Drop {
        val dropped = maxOf(0, sword.level - 1)
        return ForgeResult.Drop(
            state = paid.copy(sword = sword.copy(level = dropped)),
            newLevel = dropped,
        )
    }
}
```

- [ ] **Step 5: 테스트가 통과하는지 확인한다**

Run:

```bash
cd /c/workAndroid/SwordForge && ./gradlew :core:test
```

Expected: PASS.

- [ ] **Step 6: 커밋한다**

```bash
cd /c/workAndroid/SwordForge
git add -A
git commit -m "M1-6: 강화 판정 엔진 구현

- ForgeResult 4종: Success / Stay / Drop / Destroyed(preventable)
- canAttempt 로 사전 조건 분리, attempt 는 순수 함수이며 난수를 주입받음
- canAutoForge: 자동 강화를 안전구간(+0~+4)으로 제한하는 규칙을 도메인에 둠
- 난수 소비 순서를 계약으로 고정 (1: 성공 판정, 2: 파괴/하락 판정)
- 행운부적은 실패 결과를 무효화하고 파괴 판정 난수를 소비하지 않음
- ScriptedRandom 테스트 도구로 확률 분기를 결정적으로 검증"
```

---

### Task 7: 방지권과 줍기

**Files:**
- Modify: `core/src/main/kotlin/com/geomgang/core/ForgeEngine.kt` (함수 4개 추가)
- Test: `core/src/test/kotlin/com/geomgang/core/ForgeRecoveryTest.kt`

**Interfaces:**
- Consumes: `ForgeEngine`, `ForgeResult` (Task 6), `PendingDestroy`, `Sword`, `Item` (Task 2)
- Produces: `ForgeEngine`에 추가되는 함수들
  - `const val SALVAGE_MULTIPLIER = 2`
  - `fun canPrevent(state: GameState): Boolean`
  - `fun applyPrevent(state: GameState): GameState`
  - `fun salvageAmount(level: Int, rng: Random): Int`
  - `fun applySalvage(state: GameState, rng: Random): GameState`
  - `fun confirmDestroy(state: GameState): GameState`

- [ ] **Step 1: 실패하는 테스트를 작성한다**

`core/src/test/kotlin/com/geomgang/core/ForgeRecoveryTest.kt`:

```kotlin
package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ForgeRecoveryTest {

    private fun destroyed(
        level: Int = 14,
        tickets: Int = 1,
        family: WeaponFamily = WeaponFamily.STRAIGHT,
    ) = GameState(
        difficulty = Difficulty.NORMAL,
        gold = 1000,
        sword = null,
        inventory = Inventory(preventTickets = tickets),
        bestLevel = level,
        pendingDestroy = PendingDestroy(family, level),
    )

    // --- 방지권 ---

    @Test
    fun `방지권이 있고 파괴 대기 중이면 되살릴 수 있다`() {
        assertTrue(ForgeEngine.canPrevent(destroyed()))
    }

    @Test
    fun `방지권이 없으면 되살릴 수 없다`() {
        assertFalse(ForgeEngine.canPrevent(destroyed(tickets = 0)))
    }

    @Test
    fun `파괴 대기 상태가 아니면 되살릴 수 없다`() {
        val normal = GameState(Difficulty.NORMAL, sword = Sword(WeaponFamily.STRAIGHT, 3))
        assertFalse(ForgeEngine.canPrevent(normal))
    }

    @Test
    fun `되살리면 파괴 직전 단계와 계열이 그대로 복구된다`() {
        val after = ForgeEngine.applyPrevent(destroyed(level = 17, family = WeaponFamily.HOLY))
        assertEquals(Sword(WeaponFamily.HOLY, 17), after.sword)
        assertNull(after.pendingDestroy)
    }

    @Test
    fun `되살리면 방지권이 한 장 소모된다`() {
        val after = ForgeEngine.applyPrevent(destroyed(tickets = 3))
        assertEquals(2, after.inventory.preventTickets)
    }

    @Test(expected = IllegalStateException::class)
    fun `방지권 없이 되살리려 하면 예외가 난다`() {
        ForgeEngine.applyPrevent(destroyed(tickets = 0))
    }

    // --- 줍기 ---

    @Test
    fun `조각 회수량은 단계의 두 배에 0점7에서 1점3 배 흔들림이 붙는다`() {
        // level 10, jitter 최소(난수 0.0) → floor(10 * 2 * 0.7) = 14
        assertEquals(14, ForgeEngine.salvageAmount(10, ScriptedRandom(0.0)))
        // jitter 중앙(난수 0.5) → floor(10 * 2 * 1.0) = 20
        assertEquals(20, ForgeEngine.salvageAmount(10, ScriptedRandom(0.5)))
        // jitter 최대에 가깝게(난수 1.0 직전) → floor(10 * 2 * 1.3) = 26
        assertEquals(26, ForgeEngine.salvageAmount(10, ScriptedRandom(1.0)))
    }

    @Test
    fun `0단계 검을 잃어도 최소 한 조각은 나온다`() {
        assertEquals(1, ForgeEngine.salvageAmount(0, ScriptedRandom(0.0)))
    }

    @Test
    fun `줍기는 조각을 더하고 파괴 대기를 해제한다`() {
        val before = destroyed(level = 10)
        val after = ForgeEngine.applySalvage(before, ScriptedRandom(0.5))
        assertEquals(20, after.shards)
        assertNull(after.pendingDestroy)
        assertNull(after.sword)
    }

    @Test(expected = IllegalStateException::class)
    fun `파괴 대기가 아닐 때 줍기는 예외가 난다`() {
        val normal = GameState(Difficulty.NORMAL, sword = Sword(WeaponFamily.STRAIGHT, 3))
        ForgeEngine.applySalvage(normal, ScriptedRandom(0.5))
    }

    // --- 파괴 확정 ---

    @Test
    fun `파괴 확정은 대기 상태만 지우고 아무것도 주지 않는다`() {
        val before = destroyed(level = 14)
        val after = ForgeEngine.confirmDestroy(before)
        assertNull(after.pendingDestroy)
        assertNull(after.sword)
        assertEquals(0, after.shards)
        assertEquals(before.inventory, after.inventory)
    }

    @Test
    fun `대기 상태가 없으면 파괴 확정은 아무 변화도 없다`() {
        val normal = GameState(Difficulty.NORMAL, sword = Sword(WeaponFamily.STRAIGHT, 3))
        assertEquals(normal, ForgeEngine.confirmDestroy(normal))
    }

    @Test
    fun `앱 재시작 시나리오 - 대기 상태로 저장된 검은 확정 파괴된다`() {
        // 방지권 원이 떠 있는 동안 앱을 강제 종료한 상황을 재현한다.
        // 다시 켜면 confirmDestroy 가 호출되어 파괴가 없던 일이 되지 않는다.
        val savedMidDestroy = destroyed(level = 19, tickets = 5)
        val resumed = ForgeEngine.confirmDestroy(savedMidDestroy)
        assertNull(resumed.sword)
        assertNull(resumed.pendingDestroy)
        // 방지권도 소모되지 않는다. 기회를 그냥 잃는 것이다.
        assertEquals(5, resumed.inventory.preventTickets)
        assertFalse(ForgeEngine.canPrevent(resumed))
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

Run:

```bash
cd /c/workAndroid/SwordForge && ./gradlew :core:test --tests "com.geomgang.core.ForgeRecoveryTest"
```

Expected: 컴파일 실패. `Unresolved reference: canPrevent`.

- [ ] **Step 3: `ForgeEngine.kt`에 함수 4개를 추가한다**

`import kotlin.math.floor`를 파일 상단 import 목록에 추가하고, `object ForgeEngine` 안의 `drop` 함수 위에 다음을 넣는다.

```kotlin
    /** 조각 회수량 = 단계 × 이 값 × 흔들림. */
    const val SALVAGE_MULTIPLIER: Int = 2

    private const val SALVAGE_JITTER_MIN = 0.7
    private const val SALVAGE_JITTER_MAX = 1.3

    fun canPrevent(state: GameState): Boolean =
        state.pendingDestroy != null && state.inventory.preventTickets > 0

    /**
     * 방지권을 태워 파괴 직전 상태로 되돌린다.
     *
     * 제한 시간 안에 눌렀을 때만 호출된다. 시간을 넘겼으면 [confirmDestroy]를 부른다.
     */
    fun applyPrevent(state: GameState): GameState {
        val pending = checkNotNull(state.pendingDestroy) { "no pending destroy to prevent" }
        check(state.inventory.preventTickets > 0) { "no prevent ticket" }
        return state.copy(
            sword = Sword(pending.family, pending.level),
            inventory = state.inventory.minus(Item.PREVENT_TICKET, 1),
            pendingDestroy = null,
        )
    }

    /** 파괴된 검에서 나오는 조각 수. 최소 1개는 나온다. */
    fun salvageAmount(level: Int, rng: Random): Int {
        require(level >= 0) { "level must be >= 0, was $level" }
        val jitter = SALVAGE_JITTER_MIN + rng.nextDouble() * (SALVAGE_JITTER_MAX - SALVAGE_JITTER_MIN)
        val raw = floor(level * SALVAGE_MULTIPLIER * jitter).toInt()
        return maxOf(1, raw)
    }

    /** 파편을 주워 조각을 얻고 파괴를 마무리한다. */
    fun applySalvage(state: GameState, rng: Random): GameState {
        val pending = checkNotNull(state.pendingDestroy) { "no pending destroy to salvage" }
        return state.copy(
            shards = state.shards + salvageAmount(pending.level, rng),
            pendingDestroy = null,
        )
    }

    /**
     * 파괴를 확정한다. 아무것도 주지 않는다.
     *
     * 방지권·줍기 제한 시간을 넘겼을 때, 그리고 **파괴 대기 상태가 저장된 채로
     * 앱이 다시 켜졌을 때** 호출한다. 후자를 처리하지 않으면 방지권 대기 중
     * 강제 종료로 파괴를 무효화할 수 있다.
     */
    fun confirmDestroy(state: GameState): GameState =
        if (state.pendingDestroy == null) state else state.copy(pendingDestroy = null)
```

- [ ] **Step 4: 테스트가 통과하는지 확인한다**

Run:

```bash
cd /c/workAndroid/SwordForge && ./gradlew :core:test
```

Expected: PASS.

`salvageAmount` 기대값이 어긋나면 부동소수 오차 때문일 수 있다. `floor(10 * 2 * 1.3)`가 `25`로 나오면 `26` 대신 실제값으로 테스트를 맞추되, **0단계 최소 1개 보장은 반드시 유지한다.**

- [ ] **Step 5: 커밋한다**

```bash
cd /c/workAndroid/SwordForge
git add -A
git commit -m "M1-7: 방지권과 줍기 구현

- canPrevent/applyPrevent: 파괴 직전 단계와 계열을 복구, 방지권 1장 소모
- salvageAmount/applySalvage: 단계x2에 0.7~1.3배 흔들림, 최소 1개 보장
- confirmDestroy: 제한 시간 초과와 앱 재시작 시 파괴 확정
- 방지권 대기 중 강제 종료로 파괴를 무효화할 수 없음을 테스트로 고정"
```

---

### Task 8: 진행도 — 도감·업적·칭호·통계

**Files:**
- Create: `core/src/main/kotlin/com/geomgang/core/Progress.kt`
- Test: `core/src/test/kotlin/com/geomgang/core/ProgressTest.kt`

**Interfaces:**
- Consumes: `Difficulty` (Task 1), `WeaponFamily`, `Sword` (Task 2), `WeaponTier`, `WeaponCatalog` (Task 3), `ForgeResult` (Task 6)
- Produces:
  - `enum class Achievement(val id: String, val displayName: String, val title: String)` — 20종
  - `data class CodexKey(val family: WeaponFamily, val tier: WeaponTier, val difficulty: Difficulty)`
  - `data class Stats(...)` — 아래 구현 참조, `fun observedRate(level: Int): Double?`
  - `data class ProgressState(codex: Set<CodexKey>, achievements: Set<Achievement>, selectedTitle: Achievement?, stats: Stats)`
  - `object Progress` — `registerSword(p, difficulty, sword)`, `onAttempt(p, difficulty, family, targetLevel, cost, result)`, `onPreventUsed(p)`, `onPreventMissed(p)`, `onSalvage(p, shards)`, `onSalvageMissed(p)`, `onSell(p, gold)`, `onBailout(p)`, `refresh(p)`, `unlockedFamilies(p)`, `selectTitle(p, achievement)`

**설계 노트**: 업적 판정은 상태를 누적하며 즉석에서 하지 않고, `refresh(p)`가 현재 통계·도감을 보고 달성 조건을 통째로 다시 계산한다. 이렇게 해야 세이브를 불러왔을 때나 업적을 새로 추가했을 때 소급 적용이 자동으로 된다.

- [ ] **Step 1: 실패하는 테스트를 작성한다**

`core/src/test/kotlin/com/geomgang/core/ProgressTest.kt`:

```kotlin
package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressTest {

    private val empty = ProgressState()

    private fun forge(
        p: ProgressState,
        level: Int,
        result: ForgeResult,
        difficulty: Difficulty = Difficulty.NORMAL,
        family: WeaponFamily = WeaponFamily.STRAIGHT,
    ): ProgressState = Progress.onAttempt(
        p = p,
        difficulty = difficulty,
        family = family,
        targetLevel = level,
        cost = Economy.upgradeCost(level - 1),
        result = result,
    )

    private fun successAt(level: Int): ForgeResult.Success =
        ForgeResult.Success(
            state = GameState(Difficulty.NORMAL, sword = Sword(WeaponFamily.STRAIGHT, level)),
            newLevel = level,
        )

    private fun failAt(level: Int): ForgeResult.Stay =
        ForgeResult.Stay(
            state = GameState(Difficulty.NORMAL, sword = Sword(WeaponFamily.STRAIGHT, level - 1)),
            level = level - 1,
        )

    private fun destroyedAt(level: Int): ForgeResult.Destroyed =
        ForgeResult.Destroyed(
            state = GameState(
                Difficulty.NORMAL,
                pendingDestroy = PendingDestroy(WeaponFamily.STRAIGHT, level - 1),
            ),
            lostLevel = level - 1,
            preventable = false,
        )

    // --- 통계 ---

    @Test
    fun `강화 시도가 통계에 누적된다`() {
        var p = empty
        p = forge(p, 1, successAt(1))
        p = forge(p, 2, failAt(2))
        assertEquals(2L, p.stats.attempts)
        assertEquals(1L, p.stats.successes)
        assertEquals(1L, p.stats.stays)
    }

    @Test
    fun `단계별 시도와 성공이 따로 기록된다`() {
        var p = empty
        repeat(3) { p = forge(p, 10, failAt(10)) }
        p = forge(p, 10, successAt(10))
        assertEquals(4L, p.stats.attemptsByLevel[10])
        assertEquals(1L, p.stats.successesByLevel[10])
    }

    @Test
    fun `실제 성공률을 계산할 수 있다`() {
        var p = empty
        repeat(3) { p = forge(p, 10, failAt(10)) }
        p = forge(p, 10, successAt(10))
        assertEquals(0.25, p.stats.observedRate(10)!!, 1e-9)
    }

    @Test
    fun `시도한 적 없는 단계의 실제 성공률은 없다`() {
        assertNull(empty.stats.observedRate(7))
    }

    @Test
    fun `연속 실패 최대치가 기록되고 성공하면 초기화된다`() {
        var p = empty
        repeat(4) { p = forge(p, 8, failAt(8)) }
        assertEquals(4, p.stats.maxFailStreak)
        assertEquals(4, p.stats.currentFailStreak)
        p = forge(p, 8, successAt(8))
        assertEquals(0, p.stats.currentFailStreak)
        assertEquals(4, p.stats.maxFailStreak)
        repeat(2) { p = forge(p, 8, failAt(8)) }
        assertEquals(4, p.stats.maxFailStreak)
    }

    @Test
    fun `소비 골드가 누적된다`() {
        var p = empty
        p = forge(p, 1, successAt(1))
        assertEquals(Economy.upgradeCost(0), p.stats.goldSpent)
    }

    @Test
    fun `판매 수입이 누적된다`() {
        val p = Progress.onSell(empty, 6605)
        assertEquals(6605L, p.stats.goldEarned)
    }

    @Test
    fun `방지권 사용과 놓침이 따로 기록된다`() {
        var p = Progress.onPreventUsed(empty)
        p = Progress.onPreventMissed(p)
        p = Progress.onPreventMissed(p)
        assertEquals(1L, p.stats.preventUsed)
        assertEquals(2L, p.stats.preventMissed)
    }

    @Test
    fun `줍기 성공과 놓침이 따로 기록된다`() {
        var p = Progress.onSalvage(empty, 24)
        p = Progress.onSalvageMissed(p)
        assertEquals(1L, p.stats.salvageTaken)
        assertEquals(1L, p.stats.salvageMissed)
        assertEquals(24L, p.stats.shardsEarned)
    }

    // --- 도감 ---

    @Test
    fun `검을 얻으면 해당 계열과 티어가 도감에 등록된다`() {
        val p = Progress.registerSword(
            empty,
            Difficulty.HARD,
            Sword(WeaponFamily.DRAGON, 19),
        )
        assertTrue(
            CodexKey(WeaponFamily.DRAGON, WeaponTier.BLACK_DRAGON, Difficulty.HARD) in p.codex,
        )
    }

    @Test
    fun `강화에 성공하면 도달한 티어가 도감에 등록된다`() {
        val p = forge(empty, 12, successAt(12), family = WeaponFamily.HOLY)
        assertTrue(CodexKey(WeaponFamily.HOLY, WeaponTier.FLAME, Difficulty.NORMAL) in p.codex)
    }

    @Test
    fun `같은 칸을 다시 얻어도 도감 크기는 늘지 않는다`() {
        var p = forge(empty, 12, successAt(12))
        val size = p.codex.size
        p = forge(p, 12, successAt(12))
        assertEquals(size, p.codex.size)
    }

    @Test
    fun `실패했을 때는 도감에 등록되지 않는다`() {
        val p = forge(empty, 12, failAt(12))
        assertTrue(p.codex.isEmpty())
    }

    // --- 업적 ---

    @Test
    fun `도달형 업적이 최고 단계로 달성된다`() {
        var p = forge(empty, 10, successAt(10))
        p = Progress.refresh(p)
        assertTrue(Achievement.REACH_5 in p.achievements)
        assertTrue(Achievement.REACH_10 in p.achievements)
        assertFalse(Achievement.REACH_12 in p.achievements)
    }

    @Test
    fun `불운형 업적 - 10연속 실패`() {
        var p = empty
        repeat(10) { p = forge(p, 8, failAt(8)) }
        p = Progress.refresh(p)
        assertTrue(Achievement.FAIL_STREAK_10 in p.achievements)
    }

    @Test
    fun `불운형 업적 - 첫 강화에서 실패`() {
        var p = forge(empty, 1, failAt(1))
        p = Progress.refresh(p)
        assertTrue(Achievement.FIRST_FAIL in p.achievements)
    }

    @Test
    fun `첫 강화에 성공하면 그 업적은 안 달린다`() {
        var p = forge(empty, 1, successAt(1))
        p = Progress.refresh(p)
        assertFalse(Achievement.FIRST_FAIL in p.achievements)
    }

    @Test
    fun `불운형 업적 - 19에서 파괴`() {
        var p = forge(empty, 20, destroyedAt(20))
        p = Progress.refresh(p)
        assertTrue(Achievement.DESTROY_AT_19 in p.achievements)
    }

    @Test
    fun `누적형 업적 - 파괴 50회와 100회`() {
        var p = empty
        repeat(50) { p = forge(p, 14, destroyedAt(14)) }
        p = Progress.refresh(p)
        assertTrue(Achievement.DESTROY_50 in p.achievements)
        assertFalse(Achievement.DESTROY_100 in p.achievements)
        repeat(50) { p = forge(p, 14, destroyedAt(14)) }
        p = Progress.refresh(p)
        assertTrue(Achievement.DESTROY_100 in p.achievements)
    }

    @Test
    fun `업적은 20종이고 아이디가 겹치지 않는다`() {
        assertEquals(20, Achievement.entries.size)
        val ids = Achievement.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `모든 업적에 칭호가 붙어 있다`() {
        assertTrue(Achievement.entries.all { it.title.isNotBlank() })
    }

    @Test
    fun `한번 달성한 업적은 취소되지 않는다`() {
        var p = empty
        repeat(10) { p = forge(p, 8, failAt(8)) }
        p = Progress.refresh(p)
        p = forge(p, 8, successAt(8))
        p = Progress.refresh(p)
        // 연속 실패는 끊겼지만 업적은 남는다
        assertEquals(0, p.stats.currentFailStreak)
        assertTrue(Achievement.FAIL_STREAK_10 in p.achievements)
    }

    // --- 계열 해금 ---

    @Test
    fun `처음에는 기본 계열 4종만 열려 있다`() {
        assertEquals(WeaponFamily.STARTERS, Progress.unlockedFamilies(empty))
    }

    @Test
    fun `12단계를 달성하면 쌍검이 열린다`() {
        var p = forge(empty, 12, successAt(12))
        p = Progress.refresh(p)
        assertTrue(WeaponFamily.TWIN in Progress.unlockedFamilies(p))
        assertFalse(WeaponFamily.DRAGON in Progress.unlockedFamilies(p))
    }

    @Test
    fun `파괴 50회를 하면 마검이 열린다`() {
        var p = empty
        repeat(50) { p = forge(p, 14, destroyedAt(14)) }
        p = Progress.refresh(p)
        assertTrue(WeaponFamily.DEMON in Progress.unlockedFamilies(p))
    }

    @Test
    fun `18단계를 달성하면 용검이 열린다`() {
        var p = forge(empty, 18, successAt(18))
        p = Progress.refresh(p)
        assertTrue(WeaponFamily.DRAGON in Progress.unlockedFamilies(p))
    }

    // --- 칭호 ---

    @Test
    fun `달성한 업적의 칭호만 선택할 수 있다`() {
        var p = forge(empty, 5, successAt(5))
        p = Progress.refresh(p)
        p = Progress.selectTitle(p, Achievement.REACH_5)
        assertEquals(Achievement.REACH_5, p.selectedTitle)
    }

    @Test(expected = IllegalStateException::class)
    fun `달성하지 않은 칭호는 선택할 수 없다`() {
        Progress.selectTitle(empty, Achievement.REACH_20)
    }

    @Test
    fun `칭호를 해제할 수 있다`() {
        var p = forge(empty, 5, successAt(5))
        p = Progress.refresh(p)
        p = Progress.selectTitle(p, Achievement.REACH_5)
        p = Progress.selectTitle(p, null)
        assertNull(p.selectedTitle)
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

Run:

```bash
cd /c/workAndroid/SwordForge && ./gradlew :core:test --tests "com.geomgang.core.ProgressTest"
```

Expected: 컴파일 실패. `Unresolved reference: ProgressState`.

- [ ] **Step 3: `Progress.kt`를 구현한다**

```kotlin
package com.geomgang.core

/**
 * 업적. 각 업적은 해금되는 칭호를 하나씩 갖는다.
 *
 * 세 갈래로 나뉜다.
 * - 도달형: 특정 단계 달성
 * - 누적형: 횟수·수량 누적
 * - 불운형: 실패와 관련된 것들. 망했을 때도 얻는 게 있어야 재도전으로 이어진다.
 */
enum class Achievement(val id: String, val displayName: String, val title: String) {
    // 도달형
    REACH_5("reach_5", "+5 달성", "견습 대장장이"),
    REACH_10("reach_10", "+10 달성", "숙련 대장장이"),
    REACH_12("reach_12", "+12 달성", "쌍검의 계승자"),
    REACH_15("reach_15", "+15 달성", "빛을 담은 자"),
    REACH_18("reach_18", "+18 달성", "용을 벼린 자"),
    REACH_20("reach_20", "+20 달성", "흑룡참의 주인"),
    ENDLESS_25("endless_25", "무한 모드 +25 달성", "끝을 보는 자"),

    // 누적형
    ATTEMPTS_1000("attempts_1000", "강화 1,000회", "망치의 세월"),
    DESTROY_50("destroy_50", "파괴 50회", "마검의 부름"),
    DESTROY_100("destroy_100", "파괴 100회", "잿더미의 증인"),
    SHARDS_5000("shards_5000", "조각 5,000개 획득", "부지런한 넝마주이"),
    SALVAGE_10("salvage_10", "줍기 10회 성공", "알뜰한 손"),
    PREVENT_10("prevent_10", "방지권 10회 사용", "위기의 순간"),
    CODEX_HALF("codex_half", "도감 절반 수집", "수집가"),
    CODEX_FULL("codex_full", "도감 완성", "만검의 주인"),
    BAILOUT("bailout", "파산 구제 받기", "빈털터리"),

    // 불운형
    FAIL_STREAK_10("fail_streak_10", "10연속 실패", "불운의 화신"),
    PREVENT_MISS_3("prevent_miss_3", "방지권 3회 놓침", "굼뜬 손"),
    DESTROY_AT_19("destroy_at_19", "+19 검을 파괴", "한 끗 차이"),
    FIRST_FAIL("first_fail", "첫 강화에서 실패", "불길한 시작"),
}

/** 도감 한 칸의 획득 기록. 같은 계열·티어라도 난이도가 다르면 별개 칸이다. */
data class CodexKey(
    val family: WeaponFamily,
    val tier: WeaponTier,
    val difficulty: Difficulty,
)

/**
 * 누적 통계. 전 모드 합산이다.
 *
 * [attemptsByLevel]과 [successesByLevel]이 통계 화면의 "표기 확률 대 실제 확률" 비교표를 만든다.
 */
data class Stats(
    val attempts: Long = 0,
    val successes: Long = 0,
    val stays: Long = 0,
    val drops: Long = 0,
    val destroys: Long = 0,
    val attemptsByLevel: Map<Int, Long> = emptyMap(),
    val successesByLevel: Map<Int, Long> = emptyMap(),
    val currentFailStreak: Int = 0,
    val maxFailStreak: Int = 0,
    val highestDestroyedLevel: Int = -1,
    val goldSpent: Long = 0,
    val goldEarned: Long = 0,
    val shardsEarned: Long = 0,
    val preventUsed: Long = 0,
    val preventMissed: Long = 0,
    val salvageTaken: Long = 0,
    val salvageMissed: Long = 0,
    val bailouts: Long = 0,
    val bestLevelEver: Int = 0,
    val bestEndlessLevel: Int = 0,
) {
    /** 이 단계에서 실제로 관측된 성공률. 시도한 적이 없으면 null. */
    fun observedRate(targetLevel: Int): Double? {
        val tried = attemptsByLevel[targetLevel] ?: return null
        if (tried == 0L) return null
        val won = successesByLevel[targetLevel] ?: 0L
        return won.toDouble() / tried.toDouble()
    }
}

/**
 * 모드에 종속되지 않는 전역 진행도.
 *
 * 모드를 초기화해도 이 상태는 지우지 않는다. 수집물이 초기화로 날아가면
 * 아무도 초기화를 누르지 않게 되고, 그러면 재도전이라는 이 장르의 핵심 재미가 막힌다.
 */
data class ProgressState(
    val codex: Set<CodexKey> = emptySet(),
    val achievements: Set<Achievement> = emptySet(),
    val selectedTitle: Achievement? = null,
    val stats: Stats = Stats(),
)

/** 진행도 누적 규칙. */
object Progress {

    /** 계열별 해금 조건. null 이면 처음부터 열려 있다. */
    private val FAMILY_UNLOCK: Map<WeaponFamily, Achievement?> = mapOf(
        WeaponFamily.STRAIGHT to null,
        WeaponFamily.CURVED to null,
        WeaponFamily.GREAT to null,
        WeaponFamily.RAPIER to null,
        WeaponFamily.TWIN to Achievement.REACH_12,
        WeaponFamily.DEMON to Achievement.DESTROY_50,
        WeaponFamily.HOLY to Achievement.REACH_15,
        WeaponFamily.DRAGON to Achievement.REACH_18,
    )

    /** 검을 손에 넣었을 때 도감에 등록한다. 구매·조합·강화 성공 모두 여기를 지난다. */
    fun registerSword(p: ProgressState, difficulty: Difficulty, sword: Sword): ProgressState {
        val key = CodexKey(sword.family, WeaponCatalog.tierFor(sword.level), difficulty)
        return if (key in p.codex) p else p.copy(codex = p.codex + key)
    }

    /**
     * 강화 시도 한 번을 통계와 도감에 반영한다.
     *
     * @param targetLevel 이번 시도로 도달하려던 단계
     * @param cost        이번 시도에 쓴 골드
     */
    fun onAttempt(
        p: ProgressState,
        difficulty: Difficulty,
        family: WeaponFamily,
        targetLevel: Int,
        cost: Long,
        result: ForgeResult,
    ): ProgressState {
        val s = p.stats
        val succeeded = result is ForgeResult.Success

        val streak = if (succeeded) 0 else s.currentFailStreak + 1

        var stats = s.copy(
            attempts = s.attempts + 1,
            successes = s.successes + if (succeeded) 1 else 0,
            stays = s.stays + if (result is ForgeResult.Stay) 1 else 0,
            drops = s.drops + if (result is ForgeResult.Drop) 1 else 0,
            destroys = s.destroys + if (result is ForgeResult.Destroyed) 1 else 0,
            attemptsByLevel = s.attemptsByLevel.increment(targetLevel),
            successesByLevel =
                if (succeeded) s.successesByLevel.increment(targetLevel) else s.successesByLevel,
            currentFailStreak = streak,
            maxFailStreak = maxOf(s.maxFailStreak, streak),
            goldSpent = s.goldSpent + cost,
        )

        if (result is ForgeResult.Destroyed) {
            stats = stats.copy(
                highestDestroyedLevel = maxOf(stats.highestDestroyedLevel, result.lostLevel),
            )
        }

        if (succeeded) {
            stats = stats.copy(
                bestLevelEver = maxOf(stats.bestLevelEver, targetLevel),
                bestEndlessLevel =
                    if (difficulty == Difficulty.ENDLESS) {
                        maxOf(stats.bestEndlessLevel, targetLevel)
                    } else {
                        stats.bestEndlessLevel
                    },
            )
        }

        val withStats = p.copy(stats = stats)
        return if (succeeded) {
            registerSword(withStats, difficulty, Sword(family, targetLevel))
        } else {
            withStats
        }
    }

    fun onPreventUsed(p: ProgressState): ProgressState =
        p.copy(stats = p.stats.copy(preventUsed = p.stats.preventUsed + 1))

    fun onPreventMissed(p: ProgressState): ProgressState =
        p.copy(stats = p.stats.copy(preventMissed = p.stats.preventMissed + 1))

    fun onSalvage(p: ProgressState, shards: Int): ProgressState =
        p.copy(
            stats = p.stats.copy(
                salvageTaken = p.stats.salvageTaken + 1,
                shardsEarned = p.stats.shardsEarned + shards,
            ),
        )

    fun onSalvageMissed(p: ProgressState): ProgressState =
        p.copy(stats = p.stats.copy(salvageMissed = p.stats.salvageMissed + 1))

    fun onSell(p: ProgressState, gold: Long): ProgressState =
        p.copy(stats = p.stats.copy(goldEarned = p.stats.goldEarned + gold))

    fun onBailout(p: ProgressState): ProgressState =
        p.copy(stats = p.stats.copy(bailouts = p.stats.bailouts + 1))

    /**
     * 현재 통계·도감을 보고 달성 업적을 통째로 다시 계산한다.
     *
     * 즉석에서 하나씩 판정하지 않는 이유: 세이브를 불러왔을 때나 업적을 새로 추가했을 때
     * 소급 적용이 자동으로 되기 때문이다. 이미 달성한 업적은 절대 취소되지 않는다.
     */
    fun refresh(p: ProgressState): ProgressState {
        val s = p.stats
        val earned = buildSet {
            addAll(p.achievements)

            if (s.bestLevelEver >= 5) add(Achievement.REACH_5)
            if (s.bestLevelEver >= 10) add(Achievement.REACH_10)
            if (s.bestLevelEver >= 12) add(Achievement.REACH_12)
            if (s.bestLevelEver >= 15) add(Achievement.REACH_15)
            if (s.bestLevelEver >= 18) add(Achievement.REACH_18)
            if (s.bestLevelEver >= 20) add(Achievement.REACH_20)
            if (s.bestEndlessLevel >= 25) add(Achievement.ENDLESS_25)

            if (s.attempts >= 1_000) add(Achievement.ATTEMPTS_1000)
            if (s.destroys >= 50) add(Achievement.DESTROY_50)
            if (s.destroys >= 100) add(Achievement.DESTROY_100)
            if (s.shardsEarned >= 5_000) add(Achievement.SHARDS_5000)
            if (s.salvageTaken >= 10) add(Achievement.SALVAGE_10)
            if (s.preventUsed >= 10) add(Achievement.PREVENT_10)
            if (s.bailouts >= 1) add(Achievement.BAILOUT)

            val total = WeaponCatalog.ENTRIES.size
            if (p.codex.map { CodexEntry(it.family, it.tier) }.toSet().size >= total / 2) {
                add(Achievement.CODEX_HALF)
            }
            if (p.codex.map { CodexEntry(it.family, it.tier) }.toSet().size >= total) {
                add(Achievement.CODEX_FULL)
            }

            if (s.maxFailStreak >= 10) add(Achievement.FAIL_STREAK_10)
            if (s.preventMissed >= 3) add(Achievement.PREVENT_MISS_3)
            if (s.highestDestroyedLevel >= 19) add(Achievement.DESTROY_AT_19)
            if (s.attempts >= 1 && s.successes == 0L && s.attemptsByLevel[1] != null) {
                add(Achievement.FIRST_FAIL)
            }
        }
        return if (earned == p.achievements) p else p.copy(achievements = earned)
    }

    /** 지금 고를 수 있는 계열들. 항상 enum 선언 순서를 유지한다. */
    fun unlockedFamilies(p: ProgressState): List<WeaponFamily> =
        WeaponFamily.entries.filter { family ->
            val required = FAMILY_UNLOCK[family]
            required == null || required in p.achievements
        }

    /** 칭호를 고르거나(달성한 업적만) 해제한다(null). */
    fun selectTitle(p: ProgressState, achievement: Achievement?): ProgressState {
        if (achievement != null) {
            check(achievement in p.achievements) { "achievement not earned: ${achievement.id}" }
        }
        return p.copy(selectedTitle = achievement)
    }

    private fun Map<Int, Long>.increment(key: Int): Map<Int, Long> =
        this + (key to ((this[key] ?: 0L) + 1L))
}
```

`FIRST_FAIL` 판정 주의: "첫 강화에서 실패"는 **아직 한 번도 성공하지 못했고 +1 시도를 한 적이 있을 때** 성립한다. 성공 이력이 생기면 더는 새로 달리지 않지만, 이미 달성했다면 유지된다(`buildSet`이 기존 집합을 먼저 담기 때문).

- [ ] **Step 4: 테스트가 통과하는지 확인한다**

Run:

```bash
cd /c/workAndroid/SwordForge && ./gradlew :core:test
```

Expected: PASS.

- [ ] **Step 5: 커밋한다**

```bash
cd /c/workAndroid/SwordForge
git add -A
git commit -m "M1-8: 도감·업적·칭호·통계 구현

- Achievement 20종(도달형 7 / 누적형 9 / 불운형 4)과 칭호
- CodexKey(계열x티어x난이도) 기반 도감 등록
- Stats: 단계별 시도/성공을 따로 세어 표기 확률 대비 실제 확률 산출
- refresh() 가 통계를 보고 업적을 통째로 재계산해 소급 적용을 자동화
- 계열 해금을 업적에 연결 (쌍검 +12 / 마검 파괴50 / 성검 +15 / 용검 +18)"
```

---

### Task 9: 밸런스 시뮬레이션과 확률표 1차 확정

**Files:**
- Create: `core/src/test/kotlin/com/geomgang/core/sim/BalanceSimulation.kt`
- Test: `core/src/test/kotlin/com/geomgang/core/sim/BalanceSimulationTest.kt`
- Modify (튜닝 결과에 따라): `core/src/main/kotlin/com/geomgang/core/RateTable.kt`
- Modify: `README.md` (개발 일지에 M1 결과 추가)

**Interfaces:**
- Consumes: `:core` 공개 API 전부
- Produces: (테스트 전용)
  - `data class RunOutcome(val bestLevel: Int, val attempts: Int, val bailouts: Int, val reachedCap: Boolean)`
  - `data class SimReport(difficulty, runs, averageBestLevel, medianBestLevel, bankruptcyRate, capRate, averageAttempts)`
  - `object BalanceSimulation` — `fun simulateRun(difficulty: Difficulty, rng: Random, maxAttempts: Int): RunOutcome`, `fun run(difficulty: Difficulty, runs: Int, maxAttempts: Int, seed: Long): SimReport`

**시뮬레이션 정책** — 결과를 해석하려면 가상 플레이어가 무엇을 하는지 고정되어야 한다.
1. 검이 없으면: 파산 구제 판정 → 조각 120 이상이면 +5 검 교환, 아니면 골드로 기본 검 구매
2. 조각이 10 이상이고 방지권이 없으면 방지권으로 교환
3. 강화 비용을 낼 수 없으면 검을 판다 (검이 있을 때)
4. 그 외에는 항상 강화 시도 (아이템 미사용)
5. 파괴되면 방지권이 있으면 항상 사용, 없으면 항상 줍기 성공
6. 상한에 도달하면 종료

- [ ] **Step 1: 시뮬레이터를 작성한다**

`core/src/test/kotlin/com/geomgang/core/sim/BalanceSimulation.kt`:

```kotlin
package com.geomgang.core.sim

import com.geomgang.core.Difficulty
import com.geomgang.core.Economy
import com.geomgang.core.ForgeEngine
import com.geomgang.core.ForgeResult
import com.geomgang.core.GameState
import com.geomgang.core.Item
import com.geomgang.core.Recipes
import com.geomgang.core.UsedItems
import com.geomgang.core.WeaponFamily
import kotlin.random.Random

/** 한 판(가상 플레이어 1명)의 결과. */
data class RunOutcome(
    val bestLevel: Int,
    val attempts: Int,
    val bailouts: Int,
    val reachedCap: Boolean,
)

/** 여러 판을 돌린 요약. 확률표 튜닝의 판단 근거다. */
data class SimReport(
    val difficulty: Difficulty,
    val runs: Int,
    val averageBestLevel: Double,
    val medianBestLevel: Int,
    val bankruptcyRate: Double,
    val capRate: Double,
    val averageAttempts: Double,
) {
    override fun toString(): String = buildString {
        appendLine("[$difficulty] runs=$runs")
        appendLine("  평균 최고 단계 : %.2f".format(averageBestLevel))
        appendLine("  중앙값         : $medianBestLevel")
        appendLine("  파산 구제 발생율: %.1f%%".format(bankruptcyRate * 100))
        appendLine("  상한 도달률    : %.2f%%".format(capRate * 100))
        appendLine("  평균 시도 수   : %.1f".format(averageAttempts))
    }
}

/**
 * 자동 플레이 시뮬레이터.
 *
 * 밸런스를 감으로 조정하지 않기 위한 도구다. 확률표를 바꾸면 여기서 나오는 숫자가 바뀌고,
 * 그 숫자로 판단한다. 동시에 회귀 방지선 역할도 한다.
 */
object BalanceSimulation {

    private const val START_GOLD = 500L

    fun simulateRun(difficulty: Difficulty, rng: Random, maxAttempts: Int): RunOutcome {
        var state = GameState(difficulty, gold = START_GOLD)
        var attempts = 0
        var bailouts = 0
        val cap = difficulty.maxLevel

        while (attempts < maxAttempts) {
            // 1. 파산 구제
            if (Economy.needsBailout(state)) {
                state = Economy.applyBailoutIfNeeded(state)
                bailouts++
            }

            // 2. 검 확보
            if (state.sword == null) {
                val sword5 = Recipes.byId("sword5")
                state = when {
                    Recipes.canCraft(state, sword5) ->
                        Recipes.craft(state, sword5, WeaponFamily.STRAIGHT)

                    Economy.canBuySword(state) ->
                        Economy.buySword(state, WeaponFamily.STRAIGHT)

                    else -> break // 구제가 있으므로 여기 오면 안 되지만 방어한다
                }
                continue
            }

            // 3. 상한 도달
            if (cap != null && (state.sword?.level ?: 0) >= cap) break

            // 4. 방지권 비축
            val prevent = Recipes.byId("prevent")
            if (state.inventory.countOf(Item.PREVENT_TICKET) == 0 &&
                Recipes.canCraft(state, prevent)
            ) {
                state = Recipes.craft(state, prevent, WeaponFamily.STRAIGHT)
                continue
            }

            // 5. 비용을 못 내면 검을 판다
            if (!ForgeEngine.canAttempt(state, UsedItems.NONE)) {
                if (Economy.canSellSword(state)) {
                    state = Economy.sellSword(state)
                    continue
                }
                break
            }

            // 6. 강화
            val result = ForgeEngine.attempt(state, UsedItems.NONE, rng)
            attempts++
            state = result.state

            if (result is ForgeResult.Destroyed) {
                state = if (ForgeEngine.canPrevent(state)) {
                    ForgeEngine.applyPrevent(state)
                } else {
                    ForgeEngine.applySalvage(state, rng)
                }
            }
        }

        return RunOutcome(
            bestLevel = state.bestLevel,
            attempts = attempts,
            bailouts = bailouts,
            reachedCap = cap != null && state.bestLevel >= cap,
        )
    }

    fun run(difficulty: Difficulty, runs: Int, maxAttempts: Int, seed: Long): SimReport {
        val rng = Random(seed)
        val outcomes = List(runs) { simulateRun(difficulty, rng, maxAttempts) }
        val levels = outcomes.map { it.bestLevel }.sorted()
        return SimReport(
            difficulty = difficulty,
            runs = runs,
            averageBestLevel = levels.average(),
            medianBestLevel = levels[levels.size / 2],
            bankruptcyRate = outcomes.count { it.bailouts > 0 }.toDouble() / runs,
            capRate = outcomes.count { it.reachedCap }.toDouble() / runs,
            averageAttempts = outcomes.map { it.attempts }.average(),
        )
    }
}
```

- [ ] **Step 2: 리포트를 출력하는 테스트를 작성하고 실행한다**

`core/src/test/kotlin/com/geomgang/core/sim/BalanceSimulationTest.kt` (1차 — 리포트만):

```kotlin
package com.geomgang.core.sim

import com.geomgang.core.Difficulty
import org.junit.Test

class BalanceSimulationTest {

    /** 튜닝용. 실패하지 않고 숫자만 뽑는다. */
    @Test
    fun `밸런스 리포트를 출력한다`() {
        for (difficulty in Difficulty.entries) {
            val report = BalanceSimulation.run(
                difficulty = difficulty,
                runs = RUNS,
                maxAttempts = MAX_ATTEMPTS,
                seed = 20260726L,
            )
            println(report)
        }
    }

    companion object {
        const val RUNS = 100_000
        const val MAX_ATTEMPTS = 400
    }
}
```

Run:

```bash
cd /c/workAndroid/SwordForge && ./gradlew :core:test --tests "com.geomgang.core.sim.BalanceSimulationTest" -i
```

출력된 숫자를 기록한다. 실행이 60초를 넘으면 `RUNS`를 `20_000`으로 낮추고 그 값을 이후 단계에서도 그대로 쓴다.

- [ ] **Step 3: 튜닝 목표와 비교하고, 벗어나면 확률표를 조정한다**

목표 구간이다. 강화 게임은 "대부분 중간에서 막히고 가끔 뚫린다"가 재미의 형태이므로 다음을 기준으로 삼는다.

| 지표 | 일반 모드 목표 |
|---|---|
| 평균 최고 단계 | 9 ~ 14 |
| 상한(+20) 도달률 | 0% 초과 ~ 15% 미만 |
| 파산 구제 발생률 | 100% 미만 (구제 없이 끝나는 판이 존재해야 함) |
| 난이도 순서 | 지옥 < 일반 < 쉬움 (평균 최고 단계 기준) |

벗어났을 때의 조정 방향:

- **평균이 너무 높다** → `RateTable.BASE`의 중반 구간(+8~+13)을 0.02씩 내린다.
- **평균이 너무 낮다** → 같은 구간을 0.02씩 올린다. 저단계(+1~+5)는 건드리지 않는다. 초반이 답답해지면 게임을 시작조차 안 하게 된다.
- **상한 도달률이 0%다** → +18~+20 성공률을 0.01씩 올린다.
- **상한 도달률이 15%를 넘는다** → 같은 구간을 0.01씩 내린다.

조정할 때는 **`RateTable.BASE` 배열 한 곳만 고친다.** 다른 파일은 건드리지 않는다.
고친 뒤 Step 2를 다시 실행해 목표 구간에 들어올 때까지 반복한다.
표를 고쳤으면 `RateTableTest`의 "기준 성공률은 스펙 표와 일치한다" 테스트의 기대값도 함께 고친다.

- [ ] **Step 4: 목표를 만족하면 회귀 방지 단언을 추가한다**

`BalanceSimulationTest.kt`에 다음 테스트들을 추가한다. 구체적 수치가 아니라 **목표 구간**으로 단언하는 이유는, 확률표를 미세 조정할 때마다 테스트가 깨지지 않으면서도 밸런스가 크게 무너지면 반드시 걸리게 하기 위해서다.

```kotlin
    @Test
    fun `일반 모드 평균 최고 단계가 목표 구간 안에 있다`() {
        val report = normalReport()
        assertTrue(
            "평균 최고 단계=${report.averageBestLevel}",
            report.averageBestLevel in 9.0..14.0,
        )
    }

    @Test
    fun `상한 도달이 가능하되 흔하지는 않다`() {
        val report = normalReport()
        assertTrue("상한 도달률=${report.capRate}", report.capRate > 0.0)
        assertTrue("상한 도달률=${report.capRate}", report.capRate < 0.15)
    }

    @Test
    fun `난이도 순서가 평균 최고 단계로 드러난다`() {
        val easy = report(Difficulty.EASY)
        val normal = report(Difficulty.NORMAL)
        val hard = report(Difficulty.HARD)
        assertTrue(hard.averageBestLevel < normal.averageBestLevel)
        assertTrue(normal.averageBestLevel < easy.averageBestLevel)
    }

    @Test
    fun `구제 없이 끝나는 판이 존재한다`() {
        assertTrue(normalReport().bankruptcyRate < 1.0)
    }

    @Test
    fun `무한 모드는 상한 없이 21단계 이상을 만들어 낸다`() {
        val endless = report(Difficulty.ENDLESS)
        assertTrue("무한 평균=${endless.averageBestLevel}", endless.averageBestLevel > 0.0)
        assertEquals(0.0, endless.capRate, 1e-9)
    }

    private fun normalReport() = report(Difficulty.NORMAL)

    private fun report(difficulty: Difficulty) = BalanceSimulation.run(
        difficulty = difficulty,
        runs = RUNS,
        maxAttempts = MAX_ATTEMPTS,
        seed = 20260726L,
    )
```

`import org.junit.Assert.assertEquals`와 `import org.junit.Assert.assertTrue`를 파일 상단에 추가한다.

- [ ] **Step 5: 전체 테스트를 돌린다**

Run:

```bash
cd /c/workAndroid/SwordForge && ./gradlew :core:test
```

Expected: PASS. 8개 테스트 클래스 전부 통과.

- [ ] **Step 6: README 개발 일지에 M1 결과를 기록한다**

`README.md`의 마일스톤 표에서 M1 상태를 `완료`로 바꾸고, 개발 일지에 항목을 추가한다.
아래 `<실측값>` 자리는 Step 3에서 나온 실제 숫자로 채운다.

```markdown
### v0.2.0 — 2026-07-XX · M1 도메인 엔진

`:core` 순수 Kotlin 모듈에 게임 규칙 전부를 구현했다. UI는 아직 없다.

**구현**

| 파일 | 내용 |
|---|---|
| `RateTable` | 확률표, 난이도 배수, 무한 감쇠, 실패 구간, 파괴 확률 |
| `Model` | Item · WeaponFamily · Sword · Inventory · GameState · PendingDestroy |
| `WeaponCatalog` | 티어 11종, 단계→티어 매핑, 도감 88칸 |
| `Recipes` | 조합소 교환식 5종 |
| `Economy` | 강화 비용 · 판매가 · 상점 · 파산 구제 |
| `ForgeEngine` | 강화 판정, 방지권, 줍기, 파괴 확정 |
| `Progress` | 도감 · 업적 20종 · 칭호 · 통계 |

**밸런스 1차 확정 (10만 판 시뮬레이션)**

| 모드 | 평균 최고 단계 | 상한 도달률 | 평균 시도 수 |
|---|---|---|---|
| 쉬움 | `<실측값>` | `<실측값>` | `<실측값>` |
| 일반 | `<실측값>` | `<실측값>` | `<실측값>` |
| 지옥 | `<실측값>` | `<실측값>` | `<실측값>` |
| 무한 | `<실측값>` | — | `<실측값>` |

`<확률표를 조정했다면 무엇을 왜 바꿨는지 여기 적는다. 조정이 없었다면 "초안 수치가 목표 구간에 들어와 조정 없이 확정했다"고 적는다.>`

**다음**

M2 — Compose 강화 화면과 저장. 여기서 처음으로 플레이 가능한 형태가 된다.
```

- [ ] **Step 7: 커밋한다**

```bash
cd /c/workAndroid/SwordForge
git add -A
git commit -m "M1-9: 밸런스 시뮬레이션과 확률표 1차 확정

- 자동 플레이 시뮬레이터: 탐욕적 정책으로 한 판을 끝까지 돌림
- 10만 판 리포트로 평균 최고 단계/상한 도달률/파산율/평균 시도 수 측정
- 목표 구간을 회귀 방지 단언으로 고정 (평균 9~14, 상한 도달 0~15%)
- 난이도 순서(지옥<일반<쉬움)를 시뮬레이션 결과로 검증
- README 개발 일지에 M1 결과와 측정값 기록"
```

- [ ] **Step 8: GitHub에 푸시한다**

```bash
cd /c/workAndroid/SwordForge && git push origin main
```

---

## 완료 기준

M1은 다음이 모두 참일 때 끝난다.

- [ ] `./gradlew :core:test`가 전부 통과한다
- [ ] `:core`에 `android`/`androidx` import가 하나도 없다 (`grep -r "import android" core/src/main` 결과 없음)
- [ ] `:core` 프로덕션 코드에 전역 난수 호출이 없다 (`grep -rn "Random.Default\|Math.random()" core/src/main` 결과 없음)
- [ ] 밸런스 리포트가 목표 구간 안에 있고, 그 수치가 README 개발 일지에 기록되어 있다
- [ ] 커밋이 태스크 단위로 9개 올라가 있고 전부 `Yongminlee2` 단독 저작자다
- [ ] GitHub `main`에 푸시되어 있다
