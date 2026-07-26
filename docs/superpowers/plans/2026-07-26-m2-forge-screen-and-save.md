# M2: 강화 화면과 저장 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 일반 모드 하나를 실제로 플레이할 수 있게 만든다. 검을 강화하고, 성공·유지·하락·파괴를 보고, 앱을 껐다 켜도 진행이 남는다.

**Architecture:** `:app` Android 모듈을 추가하고 Compose로 강화 화면 하나를 만든다. 저장은 `:core`에 `SaveStore`로 넣는다 — `java.io.File`만 쓰므로 안드로이드 의존성이 없고, 원자적 쓰기와 손상 복구를 JVM 테스트로 검증할 수 있다. `ForgeViewModel`이 도메인 상태를 들고 UI 이벤트를 엔진에 넘기며, 연출 중 입력 잠금을 단독으로 책임진다.

**Tech Stack:** Kotlin 2.4.0, AGP 9.2.1, Jetpack Compose, kotlinx-serialization, minSdk 26 / targetSdk 36

**참조:** [스펙](../specs/2026-07-26-sword-enhance-game-design.md) · [M1 계획](2026-07-26-m1-core-domain-engine.md)

## Global Constraints

M1의 제약이 그대로 유효하다. 여기에 더해:

- **`:core`는 여전히 안드로이드를 모른다.** `SaveStore`도 `java.io.File`만 쓴다. `android.*`/`androidx.*` import 금지.
- **`gradle.properties`의 `-Dfile.encoding=MS949`를 지우지 않는다.** 지우면 테스트 워커가 죽는다. 이유는 그 파일 주석에 있다.
- **화면 방향은 세로 고정**, 네트워크 권한 없음.
- **M2 범위는 일반 모드 하나뿐이다.** 모드 선택·상점·조합소·도감은 M4 이후다. 만들지 않는다.
- **검은 도형 플레이스홀더**로 그린다. 벡터 아트는 M5다.
- 커밋 메시지는 한국어, `Co-Authored-By` 트레일러 없음, 저작자는 `Yongminlee2 <dydals5678@gmail.com>`.
- 작업 브랜치는 `m2-forge-screen`. 완료 후 `main`에 병합·푸시한다.

### 알려진 위험

**Compose 의존성이 로컬 Gradle 캐시에 없다.** AGP 9.2.1과 `androidx.activity`·`androidx.lifecycle`은 있지만 `androidx.compose.*`와 Compose 컴파일러 플러그인은 받아야 한다. 첫 빌드에 네트워크가 필요하고 수 분 걸릴 수 있다. Task 1이 이 위험을 먼저 소진하도록 배치했다.

**AGP 9는 Kotlin을 내장한다.** `org.jetbrains.kotlin.android`를 함께 적용하면 충돌할 수 있다. Task 1에서 실제로 빌드해 보고 어느 조합이 통하는지 확정한다. 계획서가 아니라 빌드 결과가 답이다.

---

## File Structure

```
SwordForge/
├── settings.gradle.kts              :app 추가
├── gradle/libs.versions.toml        AGP·Compose·serialization 추가
├── core/
│   ├── build.gradle.kts             kotlinx-serialization 플러그인 추가
│   └── src/
│       ├── main/kotlin/com/geomgang/core/
│       │   ├── (M1 파일들)          @Serializable 애노테이션 추가
│       │   └── SaveStore.kt         원자적 쓰기·백업 폴백·손상 복구
│       └── test/kotlin/com/geomgang/core/
│           └── SaveStoreTest.kt
└── app/
    ├── build.gradle.kts
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/geomgang/game/
        │   ├── MainActivity.kt          Compose 진입점
        │   ├── ForgeViewModel.kt        상태 보유·입력 잠금·자동 저장
        │   ├── ForgeUiState.kt          화면이 그릴 것만 담은 상태
        │   └── ui/
        │       ├── Theme.kt             색·타이포
        │       ├── ForgeScreen.kt       화면 조립
        │       ├── SwordView.kt         검 플레이스홀더 도형
        │       └── ForgeResultBanner.kt 결과 표시
        └── res/…
```

**`SaveStore`가 `:core`에 있는 이유** — 스펙의 모듈 그림은 저장을 `:app`에 뒀지만, `java.io.File`은 안드로이드에서도 그대로 동작한다. `:core`에 두면 원자적 쓰기와 손상 복구를 JVM 테스트로 빠르게 검증할 수 있다. 안드로이드 계측 테스트로는 이 검증이 훨씬 번거롭다. 제약("`:core`는 안드로이드를 모른다")은 그대로 지켜진다.

---

### Task 1: `:app` 모듈과 Compose 스캐폴딩

**목표:** 빈 Compose 화면이 뜨는 APK를 만든다. 위험한 의존성 문제를 여기서 다 겪는다.

**Files:**
- Modify: `settings.gradle.kts`, `gradle/libs.versions.toml`
- Create: `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/geomgang/game/MainActivity.kt`
- Create: `app/src/main/java/com/geomgang/game/ui/Theme.kt`
- Create: `app/src/main/res/values/strings.xml`, `app/src/main/res/values/themes.xml`
- Create: `app/src/main/res/drawable/ic_launcher_bg.xml`, `ic_launcher_fg.xml`, `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`

**Interfaces:**
- Consumes: 없음
- Produces: `MainActivity`, `SwordForgeTheme(content: @Composable () -> Unit)`

- [ ] **Step 1: 버전 카탈로그에 Android/Compose를 추가한다**

`gradle/libs.versions.toml`에 다음을 더한다. 기존 `[versions]`의 `kotlin`·`junit`은 그대로 둔다.

```toml
agp = "9.2.1"
composeBom = "2024.09.03"
activityCompose = "1.9.2"
lifecycleViewmodelCompose = "2.8.6"
kotlinxSerialization = "1.7.3"
```

`[libraries]`에:

```toml
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleViewmodelCompose" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerialization" }
```

`[plugins]`에:

```toml
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

- [ ] **Step 2: 루트와 settings에 `:app`을 등록한다**

`settings.gradle.kts`의 `include(":core")`를 다음으로 바꾼다.

```kotlin
include(":core")
include(":app")
```

`build.gradle.kts`(루트):

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
```

- [ ] **Step 3: `app/build.gradle.kts`를 만든다**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.geomgang.game"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.geomgang.game"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.2.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":core"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
}
```

- [ ] **Step 4: 매니페스트와 리소스를 만든다**

`app/src/main/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="false"
        android:theme="@style/Theme.SwordForge">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:screenOrientation="portrait"
            android:theme="@style/Theme.SwordForge">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

`app/src/main/res/values/strings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">검 강화</string>
</resources>
```

`app/src/main/res/values/themes.xml` — Compose가 화면을 그리므로 시스템 테마는 최소로 둔다.

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.SwordForge" parent="android:Theme.Material.NoActionBar">
        <item name="android:statusBarColor">#0E0B14</item>
        <item name="android:navigationBarColor">#0E0B14</item>
    </style>
</resources>
```

`app/src/main/res/drawable/ic_launcher_bg.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#1A1426" android:pathData="M0,0h108v108h-108z" />
</vector>
```

`app/src/main/res/drawable/ic_launcher_fg.xml` — 단순한 검 실루엣.

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#C9D4E4"
        android:pathData="M54,24 L58,30 L58,64 L50,64 L50,30 Z" />
    <path android:fillColor="#8A6A3B"
        android:pathData="M42,64 L66,64 L66,69 L42,69 Z" />
    <path android:fillColor="#5A4A32"
        android:pathData="M51,69 L57,69 L57,84 L51,84 Z" />
</vector>
```

`app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_bg" />
    <foreground android:drawable="@drawable/ic_launcher_fg" />
</adaptive-icon>
```

- [ ] **Step 5: 테마와 진입점을 만든다**

`app/src/main/java/com/geomgang/game/ui/Theme.kt`:

```kotlin
package com.geomgang.game.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** 대장간의 어두운 화면. 강화 게임은 밝은 배경에서 긴장이 살지 않는다. */
private val ForgeColors = darkColorScheme(
    primary = Color(0xFFE0A458),
    onPrimary = Color(0xFF241704),
    secondary = Color(0xFF7FA5C4),
    background = Color(0xFF0E0B14),
    onBackground = Color(0xFFE6E1F0),
    surface = Color(0xFF1A1426),
    onSurface = Color(0xFFE6E1F0),
    error = Color(0xFFE05A5A),
)

@Composable
fun SwordForgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ForgeColors,
        typography = Typography(),
        content = content,
    )
}
```

`app/src/main/java/com/geomgang/game/MainActivity.kt`:

```kotlin
package com.geomgang.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import com.geomgang.game.ui.SwordForgeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SwordForgeTheme {
                Surface {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("검 강화")
                    }
                }
            }
        }
    }
}
```

`import androidx.compose.ui.Modifier`를 빠뜨리지 않는다.

- [ ] **Step 6: 빌드가 되는지 확인한다**

Run:

```bash
cd /c/workAndroid/SwordForge && ./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL, `app/build/outputs/apk/debug/app-debug.apk` 생성.

**실제로 겪은 것과 해결 (실행 후 기록)**

1. **`org.jetbrains.kotlin.android` 적용 실패.**
   `The 'org.jetbrains.kotlin.android' plugin is no longer required for Kotlin support since AGP 9.0`
   → 루트와 `:app` 양쪽에서 `kotlin-android`를 제거했다. AGP 9의 내장 Kotlin이 처리한다.
   `org.jetbrains.kotlin.plugin.compose`는 그대로 두어도 문제없이 동작한다.

2. **`SDK location not found`.**
   → `local.properties`가 없었다. WordChain의 파일을 복사했다
   (`sdk.dir=C\:\\Users\\사용자\\AppData\\Local\\Android\\Sdk`, UTF-8).
   직접 `printf`로 만들면 백슬래시 이스케이프가 먹혀 경로가 깨진다. 복사가 안전하다.
   이 파일은 `.gitignore`에 있어 커밋되지 않는다 — **다른 PC에서 클론하면 직접 만들어야 한다.**

Compose BOM 2024.09.03과 Kotlin 2.4.0 컴파일러는 충돌 없이 동작했다.
확정된 조합: **AGP 9.2.1 (내장 Kotlin) + `kotlin.plugin.compose` 2.4.0 + Compose BOM 2024.09.03**.

- [ ] **Step 7: `:core` 테스트가 여전히 통과하는지 확인한다**

Run:

```bash
cd /c/workAndroid/SwordForge && ./gradlew :core:test
```

Expected: 133건 통과. 모듈을 추가하면서 인코딩 설정이나 툴체인이 깨지지 않았는지 보는 것이다.

- [ ] **Step 8: 커밋한다**

```bash
cd /c/workAndroid/SwordForge
git add -A
git commit -m "M2-1: :app 안드로이드 모듈과 Compose 스캐폴딩

- AGP 9.2.1 + Compose BOM, minSdk 26 / targetSdk 36, 세로 고정
- 어두운 대장간 테마와 앱 아이콘(검 실루엣 벡터)
- 빈 화면이 뜨는 디버그 APK 빌드 확인
- :core 테스트 133건 그대로 통과"
```

---

### Task 2: 저장 계층

**Files:**
- Modify: `core/build.gradle.kts` (serialization 플러그인·의존성)
- Modify: `core/src/main/kotlin/com/geomgang/core/Model.kt`, `Progress.kt` (`@Serializable`)
- Create: `core/src/main/kotlin/com/geomgang/core/SaveStore.kt`
- Test: `core/src/test/kotlin/com/geomgang/core/SaveStoreTest.kt`

**Interfaces:**
- Consumes: `GameState`, `ProgressState`, `Difficulty` (M1)
- Produces:
  - `class SaveStore(private val dir: File)`
  - `fun loadGame(difficulty: Difficulty): GameState`
  - `fun saveGame(state: GameState)`
  - `fun loadProgress(): ProgressState`
  - `fun saveProgress(p: ProgressState)`
  - `fun resetGame(difficulty: Difficulty)` — 해당 모드 파일만 지운다. 도감·업적은 건드리지 않는다.

**직렬화 방침** — 도메인 클래스에 `@Serializable`을 직접 단다. 별도 DTO 계층을 두지 않는다.
`kotlinx-serialization`은 순수 Kotlin이라 `:core`의 "안드로이드를 모른다" 제약을 깨지 않는다.
모든 필드에 기본값이 있고 `ignoreUnknownKeys = true`로 읽으므로, 나중에 필드를 더해도
옛 세이브가 깨지지 않는다. 마이그레이션이 필요할 만큼 구조가 바뀌면 그때 DTO를 도입한다.

- [ ] **Step 1: 실패하는 테스트를 작성한다**

`core/src/test/kotlin/com/geomgang/core/SaveStoreTest.kt`:

```kotlin
package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SaveStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun store() = SaveStore(tmp.root)

    private fun sample() = GameState(
        difficulty = Difficulty.NORMAL,
        gold = 12_345,
        shards = 42,
        sword = Sword(WeaponFamily.DRAGON, 13),
        inventory = Inventory(preventTickets = 2, blessingScrolls = 1),
        bestLevel = 15,
    )

    @Test
    fun `저장한 적 없는 모드는 빈 상태로 시작한다`() {
        val fresh = store().loadGame(Difficulty.NORMAL)
        assertEquals(Difficulty.NORMAL, fresh.difficulty)
        assertEquals(0L, fresh.gold)
        assertNull(fresh.sword)
    }

    @Test
    fun `저장한 상태가 그대로 복원된다`() {
        val s = store()
        s.saveGame(sample())
        assertEquals(sample(), s.loadGame(Difficulty.NORMAL))
    }

    @Test
    fun `파괴 대기 상태도 저장되고 복원된다`() {
        val s = store()
        val pending = sample().copy(
            sword = null,
            pendingDestroy = PendingDestroy(WeaponFamily.HOLY, 17),
        )
        s.saveGame(pending)
        assertEquals(PendingDestroy(WeaponFamily.HOLY, 17), s.loadGame(Difficulty.NORMAL).pendingDestroy)
    }

    @Test
    fun `모드마다 파일이 분리된다`() {
        val s = store()
        s.saveGame(sample())
        s.saveGame(GameState(Difficulty.HARD, gold = 7))
        assertEquals(12_345L, s.loadGame(Difficulty.NORMAL).gold)
        assertEquals(7L, s.loadGame(Difficulty.HARD).gold)
    }

    @Test
    fun `진행도가 저장되고 복원된다`() {
        val s = store()
        val p = ProgressState(
            codex = setOf(CodexKey(WeaponFamily.TWIN, WeaponTier.RUNE, Difficulty.EASY)),
            achievements = setOf(Achievement.REACH_10),
            selectedTitle = Achievement.REACH_10,
            stats = Stats(attempts = 300, successes = 120, attemptsByLevel = mapOf(10 to 40L)),
        )
        s.saveProgress(p)
        assertEquals(p, s.loadProgress())
    }

    @Test
    fun `두 번째 저장이 백업 파일을 남긴다`() {
        val s = store()
        s.saveGame(sample())
        s.saveGame(sample().copy(gold = 999))
        assertTrue(File(tmp.root, "save_normal.json.bak").exists())
        assertEquals(999L, s.loadGame(Difficulty.NORMAL).gold)
    }

    @Test
    fun `본 파일이 깨지면 백업으로 복원한다`() {
        val s = store()
        s.saveGame(sample())
        s.saveGame(sample().copy(gold = 999))
        File(tmp.root, "save_normal.json").writeText("{ 이건 JSON 이 아니다")
        // 백업에는 첫 저장(12,345골드)이 들어 있다
        assertEquals(12_345L, s.loadGame(Difficulty.NORMAL).gold)
    }

    @Test
    fun `본 파일과 백업이 모두 깨지면 빈 상태로 시작한다`() {
        val s = store()
        s.saveGame(sample())
        s.saveGame(sample().copy(gold = 999))
        File(tmp.root, "save_normal.json").writeText("깨짐")
        File(tmp.root, "save_normal.json.bak").writeText("이것도 깨짐")
        val recovered = s.loadGame(Difficulty.NORMAL)
        assertEquals(0L, recovered.gold)
        assertEquals(Difficulty.NORMAL, recovered.difficulty)
    }

    @Test
    fun `쓰다 만 임시 파일이 남아 있어도 읽기에 지장이 없다`() {
        val s = store()
        s.saveGame(sample())
        File(tmp.root, "save_normal.json.tmp").writeText("쓰다 만 것")
        assertEquals(12_345L, s.loadGame(Difficulty.NORMAL).gold)
    }

    @Test
    fun `모드 초기화는 그 모드만 지운다`() {
        val s = store()
        s.saveGame(sample())
        s.saveGame(GameState(Difficulty.HARD, gold = 7))
        s.saveProgress(ProgressState(achievements = setOf(Achievement.REACH_10)))

        s.resetGame(Difficulty.NORMAL)

        assertEquals(0L, s.loadGame(Difficulty.NORMAL).gold)
        assertEquals(7L, s.loadGame(Difficulty.HARD).gold)
        // 도감·업적은 초기화의 영향을 받지 않는다. 이게 이 게임의 재도전 동력이다.
        assertTrue(Achievement.REACH_10 in s.loadProgress().achievements)
    }

    @Test
    fun `초기화는 백업 파일까지 지운다`() {
        val s = store()
        s.saveGame(sample())
        s.saveGame(sample().copy(gold = 999))
        s.resetGame(Difficulty.NORMAL)
        assertEquals(0L, s.loadGame(Difficulty.NORMAL).gold)
    }

    @Test
    fun `저장 디렉터리가 없으면 만들어 쓴다`() {
        val nested = File(tmp.root, "a/b/c")
        val s = SaveStore(nested)
        s.saveGame(sample())
        assertNotNull(s.loadGame(Difficulty.NORMAL).sword)
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

Run:

```bash
cd /c/workAndroid/SwordForge && ./gradlew :core:test --tests "com.geomgang.core.SaveStoreTest"
```

Expected: 컴파일 실패. `Unresolved reference: SaveStore`.

- [ ] **Step 3: `:core`에 직렬화를 붙인다**

`core/build.gradle.kts`의 `plugins` 블록에 `alias(libs.plugins.kotlin.serialization)`을 더하고,
`dependencies`에 `implementation(libs.kotlinx.serialization.json)`을 더한다.

`Model.kt`의 `Sword`·`Inventory`·`PendingDestroy`·`GameState`와
`Progress.kt`의 `CodexKey`·`Stats`·`ProgressState`에 `@Serializable`을 단다.
`kotlinx.serialization.Serializable` import가 필요하다.
enum(`Item`·`WeaponFamily`·`WeaponTier`·`Difficulty`·`Achievement`)은 이름으로 직렬화되므로
애노테이션이 없어도 되지만, 명시적으로 다는 편이 의도가 분명하다.

- [ ] **Step 4: `SaveStore.kt`를 구현한다**

```kotlin
package com.geomgang.core

import kotlinx.serialization.json.Json
import java.io.File

/**
 * 세이브 파일 저장소.
 *
 * `java.io.File`만 쓰므로 안드로이드 의존성이 없고 JVM 테스트로 검증된다.
 *
 * 자동 저장이 매 강화마다 일어나서 중단 타이밍이 많다. 그래서 쓰기를 세 걸음으로 나눈다.
 * 1. 임시 파일에 쓴다
 * 2. 기존 파일을 `.bak` 으로 옮긴다
 * 3. 임시 파일을 정식 이름으로 rename 한다
 *
 * 어느 걸음에서 죽어도 정식 파일이나 `.bak` 중 하나는 온전하다.
 *
 * 모드별 진행은 파일이 분리되어 있어 한 모드가 깨져도 나머지는 살아남는다.
 * 도감·업적·통계는 [PROGRESS_FILE] 한 곳에 모으고 모드 초기화의 영향을 받지 않는다.
 */
class SaveStore(private val dir: File) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun loadGame(difficulty: Difficulty): GameState =
        read(gameFile(difficulty)) { json.decodeFromString<GameState>(it) }
            ?: GameState(difficulty)

    fun saveGame(state: GameState) {
        write(gameFile(state.difficulty), json.encodeToString(state))
    }

    fun loadProgress(): ProgressState =
        read(File(dir, PROGRESS_FILE)) { json.decodeFromString<ProgressState>(it) }
            ?: ProgressState()

    fun saveProgress(p: ProgressState) {
        write(File(dir, PROGRESS_FILE), json.encodeToString(p))
    }

    /** 해당 모드의 진행만 지운다. 도감·업적·통계는 남는다. */
    fun resetGame(difficulty: Difficulty) {
        val file = gameFile(difficulty)
        file.delete()
        backupOf(file).delete()
        tempOf(file).delete()
    }

    private fun gameFile(difficulty: Difficulty) = File(dir, "save_${difficulty.id}.json")

    private fun backupOf(file: File) = File(file.parentFile, file.name + ".bak")

    private fun tempOf(file: File) = File(file.parentFile, file.name + ".tmp")

    /** 정식 파일을 먼저 읽고, 깨졌으면 백업으로 물러선다. 둘 다 실패하면 null. */
    private fun <T> read(file: File, parse: (String) -> T): T? =
        tryRead(file, parse) ?: tryRead(backupOf(file), parse)

    private fun <T> tryRead(file: File, parse: (String) -> T): T? = try {
        if (file.exists()) parse(file.readText()) else null
    } catch (e: Exception) {
        // 손상된 세이브는 예외가 아니라 "없음"으로 다룬다. 앱이 죽는 것보다 낫다.
        null
    }

    private fun write(file: File, text: String) {
        dir.mkdirs()
        val tmp = tempOf(file)
        tmp.writeText(text)
        if (file.exists()) {
            val bak = backupOf(file)
            bak.delete()
            file.renameTo(bak)
        }
        if (!tmp.renameTo(file)) {
            // 드물게 rename 이 실패하면 복사로 대체하고 임시 파일을 치운다
            file.writeText(text)
            tmp.delete()
        }
    }

    companion object {
        const val PROGRESS_FILE = "collection.json"
    }
}
```

- [ ] **Step 5: 테스트가 통과하는지 확인한다**

Run:

```bash
cd /c/workAndroid/SwordForge && ./gradlew :core:test
```

Expected: PASS, 145건 (기존 133 + 12).

`GameState`에 `@Serializable`을 달 때 `init` 블록의 `require`가 역직렬화에서도 돈다.
깨진 값이 들어오면 예외가 나고 `tryRead`가 null 로 처리하므로 의도한 동작이다.

- [ ] **Step 6: 커밋한다**

```bash
cd /c/workAndroid/SwordForge
git add -A
git commit -m "M2-2: 세이브 저장소 구현

- SaveStore: 임시 파일 쓰기 -> 백업 이동 -> rename 3단계 원자적 저장
- 본 파일이 깨지면 .bak 으로 폴백, 둘 다 깨지면 빈 상태로 시작
- 모드별 파일 분리로 한 모드가 깨져도 나머지는 무사
- 모드 초기화는 그 모드만 지우고 도감·업적·통계는 보존
- 도메인 클래스에 @Serializable, ignoreUnknownKeys 로 필드 추가에 견디게 함
- java.io.File 만 써서 :core 의 안드로이드 무의존 제약 유지, JVM 테스트로 검증
- 테스트 12건 추가"
```

---

### Task 3: ForgeViewModel

**Files:**
- Create: `app/src/main/java/com/geomgang/game/ForgeUiState.kt`
- Create: `app/src/main/java/com/geomgang/game/ForgeViewModel.kt`

**Interfaces:**
- Consumes: `:core` 전부, `SaveStore`
- Produces:
  - `data class ForgeUiState(...)` — 화면이 그릴 것만 담는다
  - `class ForgeViewModel(private val store: SaveStore, difficulty: Difficulty, private val rng: Random)` — `val ui: StateFlow<ForgeUiState>`, `fun forge()`, `fun usePrevent()`, `fun salvage()`, `fun sellSword()`, `fun buySword(family)`, `fun onAnimationFinished()`

**책임 경계**
- **입력 잠금은 여기서만 한다.** 연출이 재생 중이면 `forge()`가 아무것도 하지 않는다. 버튼마다 플래그를 흩뿌리지 않는다.
- **자동 저장은 상태가 바뀔 때마다** 한다.
- **파괴 대기 복원**: 생성 시 세이브에 `pendingDestroy`가 남아 있으면 `ForgeEngine.confirmDestroy`를 불러 파괴를 확정한다. 이걸 빼면 강제 종료로 파괴를 무효화할 수 있다.
- 방지권·줍기 제한 시간(2.5초·3초)은 **M3**에서 붙인다. M2에서는 시간 제한 없이 버튼으로 동작시킨다. 타이머와 연출을 한꺼번에 넣으면 어디가 틀렸는지 분간이 안 되기 때문이다.

- [ ] **Step 1: `ForgeUiState.kt`를 작성한다**

```kotlin
package com.geomgang.game

import com.geomgang.core.Difficulty
import com.geomgang.core.ForgeResult
import com.geomgang.core.Sword

/** 강화 화면이 그리는 데 필요한 것 전부. 도메인 상태를 화면 언어로 옮긴 것이다. */
data class ForgeUiState(
    val difficulty: Difficulty,
    val sword: Sword?,
    val gold: Long,
    val shards: Int,
    val preventTickets: Int,
    val bestLevel: Int,
    val upgradeCost: Long,
    val sellPrice: Long,
    val successPercent: Int,
    val canForge: Boolean,
    val canBuySword: Boolean,
    /** 마지막 강화 결과. 연출이 끝나면 null 로 돌아간다. */
    val lastResult: ForgeResult? = null,
    /** 파괴 판정이 나서 방지권/줍기 응답을 기다리는 중인지. */
    val awaitingDestroyChoice: Boolean = false,
    val canPrevent: Boolean = false,
    /** 연출 재생 중에는 입력을 받지 않는다. */
    val busy: Boolean = false,
)
```

- [ ] **Step 2: `ForgeViewModel.kt`를 작성한다**

```kotlin
package com.geomgang.game

import androidx.lifecycle.ViewModel
import com.geomgang.core.Difficulty
import com.geomgang.core.Economy
import com.geomgang.core.ForgeEngine
import com.geomgang.core.ForgeResult
import com.geomgang.core.GameState
import com.geomgang.core.Progress
import com.geomgang.core.ProgressState
import com.geomgang.core.RateTable
import com.geomgang.core.SaveStore
import com.geomgang.core.UsedItems
import com.geomgang.core.WeaponFamily
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random
import kotlin.math.roundToInt

/**
 * 강화 화면의 상태 보유자.
 *
 * 도메인은 순수 함수라 상태를 스스로 들고 있지 않는다. 그 역할이 여기다.
 * 연출 중 입력 잠금도 여기서만 한다 — 잠금 주체가 여럿이면 연타로 상태가 꼬인다.
 */
class ForgeViewModel(
    private val store: SaveStore,
    difficulty: Difficulty,
    private val rng: Random = Random.Default,
) : ViewModel() {

    private var game: GameState
    private var progress: ProgressState
    private var busy = false

    private val _ui: MutableStateFlow<ForgeUiState>
    val ui: StateFlow<ForgeUiState> get() = _ui.asStateFlow()

    init {
        progress = store.loadProgress()
        // 방지권 대기 중 강제 종료로 파괴를 무효화할 수 없게, 남아 있던 대기 상태는 확정 처리한다.
        val loaded = store.loadGame(difficulty)
        game = if (loaded.pendingDestroy != null) {
            val confirmed = ForgeEngine.confirmDestroy(loaded)
            progress = Progress.onPreventMissed(progress)
            store.saveGame(confirmed)
            store.saveProgress(progress)
            confirmed
        } else {
            loaded
        }
        game = Economy.applyBailoutIfNeeded(game).also {
            if (it !== game) {
                progress = Progress.onBailout(progress)
                store.saveGame(it)
            }
        }
        _ui = MutableStateFlow(render(null))
    }

    fun forge() {
        if (busy || !ForgeEngine.canAttempt(game, UsedItems.NONE)) return
        val sword = game.sword ?: return
        val targetLevel = sword.level + 1
        val cost = Economy.upgradeCost(sword.level)

        val result = ForgeEngine.attempt(game, UsedItems.NONE, rng)
        game = result.state
        progress = Progress.refresh(
            Progress.onAttempt(progress, game.difficulty, sword.family, targetLevel, cost, result),
        )
        busy = true
        persist()
        _ui.value = render(result)
    }

    /** 파괴된 검을 방지권으로 되살린다. */
    fun usePrevent() {
        if (!ForgeEngine.canPrevent(game)) return
        game = ForgeEngine.applyPrevent(game)
        progress = Progress.refresh(Progress.onPreventUsed(progress))
        finishDestroyChoice()
    }

    /** 파편을 주워 조각을 얻는다. */
    fun salvage() {
        val pending = game.pendingDestroy ?: return
        val before = game.shards
        game = ForgeEngine.applySalvage(game, rng)
        progress = Progress.refresh(Progress.onSalvage(progress, game.shards - before))
        finishDestroyChoice()
    }

    fun sellSword() {
        if (busy || !Economy.canSellSword(game)) return
        val price = Economy.sellPrice(game.sword?.level ?: 0)
        game = Economy.sellSword(game)
        progress = Progress.refresh(Progress.onSell(progress, price))
        applyBailout()
        persist()
        _ui.value = render(null)
    }

    fun buySword(family: WeaponFamily) {
        if (busy || !Economy.canBuySword(game)) return
        game = Economy.buySword(game, family)
        progress = Progress.registerSword(progress, game.difficulty, requireNotNull(game.sword))
        persist()
        _ui.value = render(null)
    }

    /** 결과 연출이 끝났다고 화면이 알려 준다. 입력 잠금을 푼다. */
    fun onAnimationFinished() {
        busy = false
        _ui.value = render(_ui.value.lastResult.takeIf { game.pendingDestroy != null })
    }

    private fun finishDestroyChoice() {
        busy = false
        applyBailout()
        persist()
        _ui.value = render(null)
    }

    private fun applyBailout() {
        val rescued = Economy.applyBailoutIfNeeded(game)
        if (rescued !== game) {
            game = rescued
            progress = Progress.refresh(Progress.onBailout(progress))
        }
    }

    private fun persist() {
        store.saveGame(game)
        store.saveProgress(progress)
    }

    private fun render(result: ForgeResult?): ForgeUiState {
        val sword = game.sword
        val level = sword?.level ?: 0
        val targetLevel = level + 1
        return ForgeUiState(
            difficulty = game.difficulty,
            sword = sword,
            gold = game.gold,
            shards = game.shards,
            preventTickets = game.inventory.preventTickets,
            bestLevel = game.bestLevel,
            upgradeCost = Economy.upgradeCost(level),
            sellPrice = Economy.sellPrice(level),
            successPercent =
                (RateTable.successRate(game.difficulty, targetLevel) * 100).roundToInt(),
            canForge = !busy && ForgeEngine.canAttempt(game, UsedItems.NONE),
            canBuySword = !busy && Economy.canBuySword(game),
            lastResult = result,
            awaitingDestroyChoice = game.pendingDestroy != null,
            canPrevent = ForgeEngine.canPrevent(game),
            busy = busy,
        )
    }
}
```

`ui.value.lastResult` 접근 때문에 `_ui`는 `init`에서 마지막에 초기화한다.

- [ ] **Step 3: 컴파일을 확인한다**

Run:

```bash
cd /c/workAndroid/SwordForge && ./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

`kotlinx-coroutines`가 필요하면 `app/build.gradle.kts`에 추가한다
(`androidx.lifecycle:lifecycle-viewmodel-compose`가 끌어오는 경우가 많다).

- [ ] **Step 4: 커밋한다**

```bash
cd /c/workAndroid/SwordForge
git add -A
git commit -m "M2-3: ForgeViewModel 구현

- 도메인 상태 보유와 UI 상태 변환, 매 변화마다 자동 저장
- 연출 중 입력 잠금을 ViewModel 단독 책임으로 둠 (연타 방지)
- 시작 시 남아 있는 파괴 대기 상태를 확정 처리해 강제 종료 악용 차단
- 파산 구제를 시작·판매·파괴 처리 뒤에 자동 적용
- 방지권/줍기 제한 시간은 M3 에서 붙인다. 여기서는 버튼으로만 동작"
```

---

### Task 4: 강화 화면

**Files:**
- Create: `app/src/main/java/com/geomgang/game/ui/SwordView.kt`
- Create: `app/src/main/java/com/geomgang/game/ui/ForgeScreen.kt`
- Modify: `app/src/main/java/com/geomgang/game/MainActivity.kt`

**Interfaces:**
- Consumes: `ForgeUiState`, `ForgeViewModel`
- Produces: `@Composable fun ForgeScreen(state: ForgeUiState, onForge: () -> Unit, onPrevent: () -> Unit, onSalvage: () -> Unit, onSell: () -> Unit, onBuy: () -> Unit, onAnimationEnd: () -> Unit)`, `@Composable fun SwordView(sword: Sword?, modifier: Modifier)`

**레이아웃** — 세로 한 손 조작. 위에서 아래로:

```
상단   난이도 · 최고 기록
중앙   검 그림 (플레이스홀더 도형) + 단계 배지
       결과 배너 (성공/유지/하락/파괴)
정보   골드 · 조각 · 방지권
       다음 단계 성공률 · 강화 비용 · 판매가
하단   [강화] 큰 버튼
       [판매] [새 검 구입]  또는  [방지권 사용] [줍기]
```

M5에서 벡터 아트로 갈아 끼울 때 `SwordView`만 교체하면 되도록 그림을 한 곳에 가둔다.

- [ ] **Step 1: `SwordView.kt`를 작성한다**

```kotlin
package com.geomgang.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.geomgang.core.Sword
import com.geomgang.core.WeaponCatalog

/**
 * 검 그림. M2 에서는 도형 플레이스홀더다.
 *
 * M5 에서 벡터 레이어 조립으로 교체할 때 이 파일만 갈아 끼우면 되도록,
 * 검을 그리는 지식을 여기 한 곳에 가둔다.
 */
@Composable
fun SwordView(sword: Sword?, modifier: Modifier = Modifier) {
    val tierIndex = sword?.let { WeaponCatalog.tierFor(it.level).ordinal } ?: 0
    val bladeColor = TIER_COLORS[tierIndex.coerceIn(TIER_COLORS.indices)]
    val hasAura = (sword?.level ?: 0) >= WeaponCatalog.AURA_MIN_LEVEL

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f

        if (sword == null) return@Canvas

        if (hasAura) {
            drawCircle(
                color = bladeColor.copy(alpha = 0.18f),
                radius = w * 0.38f,
                center = Offset(cx, h * 0.45f),
            )
        }

        // 날
        drawRect(
            color = bladeColor,
            topLeft = Offset(cx - w * 0.05f, h * 0.10f),
            size = Size(w * 0.10f, h * 0.55f),
        )
        // 코등이
        drawRect(
            color = Color(0xFF8A6A3B),
            topLeft = Offset(cx - w * 0.18f, h * 0.65f),
            size = Size(w * 0.36f, h * 0.05f),
        )
        // 손잡이
        drawRect(
            color = Color(0xFF5A4A32),
            topLeft = Offset(cx - w * 0.04f, h * 0.70f),
            size = Size(w * 0.08f, h * 0.20f),
        )
    }
}

/** 티어 순서대로 색이 화려해진다. WeaponTier 선언 순서와 짝을 이룬다. */
private val TIER_COLORS = listOf(
    Color(0xFF8A8A8A), // 녹슨 검
    Color(0xFFB8C0C8), // 강철검
    Color(0xFFD8DEE8), // 은장검
    Color(0xFF9AD7E0), // 룬검
    Color(0xFFE08A4A), // 화염검
    Color(0xFFEBD75A), // 뇌전검
    Color(0xFFF5F0C8), // 여명의 성검
    Color(0xFF9A5AD0), // 흑룡참
    Color(0xFFE05A8A), // 용린참
    Color(0xFF5A5AD0), // 심연검
    Color(0xFFFFFFFF), // 이름 없는 검
)
```

- [ ] **Step 2: `ForgeScreen.kt`를 작성한다**

화면 조립이다. 아래 구조를 따르되 여백·크기는 보기 좋게 조정해도 된다.

```kotlin
package com.geomgang.game.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geomgang.core.ForgeResult
import com.geomgang.game.ForgeUiState
import kotlinx.coroutines.delay

@Composable
fun ForgeScreen(
    state: ForgeUiState,
    onForge: () -> Unit,
    onPrevent: () -> Unit,
    onSalvage: () -> Unit,
    onSell: () -> Unit,
    onBuy: () -> Unit,
    onAnimationEnd: () -> Unit,
) {
    // 결과 배너를 잠깐 보여 준 뒤 입력 잠금을 푼다. M3 에서 진짜 연출로 대체한다.
    LaunchedEffect(state.lastResult) {
        if (state.lastResult != null && !state.awaitingDestroyChoice) {
            delay(450)
            onAnimationEnd()
        } else if (state.lastResult != null) {
            onAnimationEnd()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "${state.difficulty.displayLabel()} · 최고 +${state.bestLevel}",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SwordView(state.sword, Modifier.size(160.dp, 220.dp))
                Spacer(Modifier.height(8.dp))
                Text(
                    text = state.sword?.let { "+${it.level} ${it.family.displayName}" } ?: "검 없음",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                ResultBanner(state.lastResult)
            }
        }

        InfoRow("골드", "%,d".format(state.gold))
        InfoRow("조각", "${state.shards}")
        InfoRow("방지권", "${state.preventTickets}장")
        Divider(Modifier.padding(vertical = 8.dp))
        if (state.sword != null) {
            InfoRow("다음 성공률", "${state.successPercent}%")
            InfoRow("강화 비용", "%,d".format(state.upgradeCost))
            InfoRow("판매가", "%,d".format(state.sellPrice))
        }

        Spacer(Modifier.height(16.dp))

        if (state.awaitingDestroyChoice) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onPrevent, enabled = state.canPrevent, modifier = Modifier.weight(1f)) {
                    Text("방지권 사용")
                }
                Button(onClick = onSalvage, modifier = Modifier.weight(1f)) {
                    Text("줍기")
                }
            }
        } else {
            Button(
                onClick = onForge,
                enabled = state.canForge,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
            ) {
                Text("강 화", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onSell,
                    enabled = !state.busy && state.sword != null,
                    modifier = Modifier.weight(1f),
                ) { Text("판매") }
                OutlinedButton(
                    onClick = onBuy,
                    enabled = state.canBuySword,
                    modifier = Modifier.weight(1f),
                ) { Text("새 검 구입") }
            }
        }
    }
}

@Composable
private fun ResultBanner(result: ForgeResult?) {
    val (text, color) = when (result) {
        is ForgeResult.Success -> "성공! +${result.newLevel}" to Color(0xFF7FD48A)
        is ForgeResult.Stay -> "실패 — 단계 유지" to Color(0xFFD4C87F)
        is ForgeResult.Drop -> "하락… +${result.newLevel}" to Color(0xFFD49A5A)
        is ForgeResult.Destroyed -> "파괴!!" to Color(0xFFE05A5A)
        null -> "" to Color.Transparent
    }
    Text(text = text, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        Text(value, fontWeight = FontWeight.Medium)
    }
}
```

`Difficulty.displayLabel()`은 `:core`에 없다. `ForgeScreen.kt` 안에 확장 함수로 둔다.

```kotlin
private fun com.geomgang.core.Difficulty.displayLabel(): String = when (this) {
    com.geomgang.core.Difficulty.EASY -> "쉬움"
    com.geomgang.core.Difficulty.NORMAL -> "일반"
    com.geomgang.core.Difficulty.HARD -> "지옥"
    com.geomgang.core.Difficulty.ENDLESS -> "무한"
}
```

- [ ] **Step 3: `MainActivity`를 화면에 연결한다**

`SaveStore(filesDir)`를 만들고 `ForgeViewModel`을 직접 생성한다.
DI 프레임워크를 도입하지 않는다 — 모듈이 둘뿐이고 의존성이 하나다.

```kotlin
package com.geomgang.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.geomgang.core.Difficulty
import com.geomgang.core.SaveStore
import com.geomgang.core.WeaponFamily
import com.geomgang.game.ui.ForgeScreen
import com.geomgang.game.ui.SwordForgeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm = remember { ForgeViewModel(SaveStore(filesDir), Difficulty.NORMAL) }
            val state by vm.ui.collectAsStateWithLifecycle()
            SwordForgeTheme {
                Surface {
                    ForgeScreen(
                        state = state,
                        onForge = vm::forge,
                        onPrevent = vm::usePrevent,
                        onSalvage = vm::salvage,
                        onSell = vm::sellSword,
                        onBuy = { vm.buySword(WeaponFamily.STRAIGHT) },
                        onAnimationEnd = vm::onAnimationFinished,
                    )
                }
            }
        }
    }
}
```

`collectAsStateWithLifecycle`은 `androidx.lifecycle:lifecycle-runtime-compose`가 필요하다.
없으면 의존성을 추가하거나 `collectAsState()`로 바꾼다.

- [ ] **Step 4: APK를 빌드한다**

Run:

```bash
cd /c/workAndroid/SwordForge && ./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: 커밋한다**

```bash
cd /c/workAndroid/SwordForge
git add -A
git commit -m "M2-4: 강화 화면 구현

- SwordView: 티어별 색과 15단계 이상 오라를 도형으로 표현 (M5 에서 벡터로 교체)
- ForgeScreen: 검·단계·결과 배너·자원·확률·비용·강화 버튼을 한 화면에
- 파괴 판정 시 방지권/줍기 선택 버튼으로 전환
- MainActivity 가 SaveStore(filesDir) 와 ViewModel 을 연결
- 검 그리는 지식을 SwordView 한 곳에 가둬 M5 교체 범위를 좁힘"
```

---

### Task 5: 실행 확인과 기록

**Files:**
- Modify: `README.md` (마일스톤 표, 개발 일지 v0.3.0)

- [ ] **Step 1: 전체 테스트를 돌린다**

```bash
cd /c/workAndroid/SwordForge && ./gradlew :core:test :app:assembleDebug
```

Expected: 테스트 145건 통과, APK 생성.

- [ ] **Step 2: 실기기 또는 에뮬레이터에서 확인한다**

연결된 기기가 있으면 설치해 실제로 눌러 본다.

```bash
cd /c/workAndroid/SwordForge && ./gradlew :app:installDebug
```

기기가 없으면 이 단계를 건너뛰고 **README에 "미확인"이라고 적는다.** 확인하지 않은 것을 확인했다고 쓰지 않는다.

확인할 것:
1. 앱이 켜지고 "검 없음"이 보인다
2. 새 검 구입 → +0 검이 생기고 골드가 100 줄어든다
3. 강화를 눌러 성공·유지·하락이 배너로 보인다
4. 앱을 완전히 종료했다가 다시 켜면 단계·골드가 그대로다
5. 고단계에서 파괴가 나면 방지권/줍기 버튼으로 바뀐다

- [ ] **Step 3: README를 갱신한다**

마일스톤 표의 M2를 `완료`로 바꾸고 개발 일지에 `v0.3.0` 항목을 추가한다.
구현 내용, 추가된 테스트, 실기기 확인 여부(또는 미확인 사유), 겪은 문제와 해결을 적는다.
M1 항목과 같은 밀도로 쓴다.

- [ ] **Step 4: 커밋하고 main에 병합한다**

```bash
cd /c/workAndroid/SwordForge
git add -A
git commit -m "M2-5: 실행 확인과 개발 일지 갱신"
git checkout main
git merge --no-ff m2-forge-screen -m "M2 강화 화면과 저장 완료"
./gradlew :core:test
git push origin main
git branch -d m2-forge-screen
```

---

## 완료 기준

- [ ] `./gradlew :core:test`가 145건 전부 통과한다
- [ ] `./gradlew :app:assembleDebug`가 APK를 만든다
- [ ] `:core`에 `android`/`androidx` import가 없다
- [ ] 앱을 껐다 켜도 진행 상황이 남는다 (실기기 확인 또는 미확인 명시)
- [ ] 파괴 대기 상태로 앱을 죽였다 켜면 파괴가 확정된다
- [ ] README 개발 일지에 v0.3.0이 기록되어 있다
- [ ] `main`에 병합되어 GitHub에 푸시되어 있다
