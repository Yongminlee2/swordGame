import java.util.Properties

// AGP 9.0 부터 Kotlin 지원이 내장이라 org.jetbrains.kotlin.android 를 적용하면 안 된다.
// 적용하면 "no longer required for Kotlin support since AGP 9.0" 오류로 빌드가 죽는다.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

/**
 * 서명 키는 저장소에 넣지 않는다.
 *
 * 프로젝트 루트에 keystore.properties 가 있을 때만 릴리스를 서명한다.
 * 공개된 디버그 키로 릴리스를 서명하면 같은 키를 가진 개조 APK가 업데이트될 수 있으므로
 * 보안 기능을 넣은 뒤에는 안전한 대체 경로가 아니다. 만드는 법은 README 에 있다.
 */
val keystoreProps = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val hasReleaseKey = keystoreProps.getProperty("storeFile") != null

android {
    namespace = "com.geomgang.game"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.geomgang.game"
        minSdk = 26
        targetSdk = 36
        versionCode = 4
        versionName = "1.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseKey) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isDebuggable = false
            isJniDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasReleaseKey) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        // 디버그 APK 롤백 뒤의 평문 세이브 재이전은 DEBUG 빌드에서만 허용한다.
        buildConfig = true
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
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.serialization.json)

    // 광고. user-messaging-platform 은 유럽 이용자 동의를 받는 구글 인증 도구로,
    // 이것 없이 유럽에 광고를 내보내면 정책 위반이다.
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
