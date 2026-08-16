package com.geomgang.game

import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.geomgang.core.Settings
import com.geomgang.game.i18n.GameLanguage
import com.geomgang.game.i18n.GameTranslator
import com.geomgang.game.ui.LocalGameTranslator
import com.geomgang.game.ui.SettingsScreen
import com.geomgang.game.ui.SwordForgeTheme
import org.junit.Rule
import org.junit.Test

/** 13개 언어가 실제 Android Compose 화면에서 모두 로드되고 그려지는지 확인한다. */
class LocalizationDeviceTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun everyLanguageRendersSettingsScreen() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val initialLanguage = GameLanguage.KOREAN
        val current = mutableStateOf(
            initialLanguage to GameTranslator.load(context.assets, initialLanguage),
        )

        compose.setContent {
            val (language, translator) = current.value
            CompositionLocalProvider(LocalGameTranslator provides translator) {
                SwordForgeTheme {
                    SettingsScreen(
                        settings = Settings(languageTag = language.tag),
                        deepUnlocked = true,
                        onAutoPreventChange = {},
                        onSoundChange = {},
                        onMusicChange = {},
                        onHapticsChange = {},
                        onLanguageChange = {},
                        backupBusy = false,
                        backupMessage = null,
                        backupError = false,
                        onRequestExportBackup = {},
                        onRequestImportBackup = {},
                        onReset = {},
                        onBack = {},
                    )
                }
            }
        }

        val outputDirectory = requireNotNull(context.getExternalFilesDir("localization-qa"))
        GameLanguage.selectable.forEach { language ->
            val translator = GameTranslator.load(context.assets, language)
            compose.runOnIdle { current.value = language to translator }
            compose.waitForIdle()

            listOf("설정", "언어", "언어 변경", "효과음").forEach { source ->
                compose.onNodeWithText(translator.translate(source)).assertIsDisplayed()
            }

            outputDirectory.resolve("settings-${language.assetCode}.png").outputStream().use {
                compose.onRoot().captureToImage().asAndroidBitmap()
                    .compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }

        val korean = GameTranslator.load(context.assets, GameLanguage.KOREAN)
        compose.runOnIdle { current.value = GameLanguage.KOREAN to korean }
        compose.onNodeWithText("언어 변경").performClick()
        GameLanguage.selectable.forEach { language ->
            val option = if (language == GameLanguage.KOREAN) {
                "✓  ${language.nativeName}"
            } else {
                "   ${language.nativeName}"
            }
            compose.onNodeWithText(
                option,
                useUnmergedTree = true,
            ).assertExists()
        }
    }
}
