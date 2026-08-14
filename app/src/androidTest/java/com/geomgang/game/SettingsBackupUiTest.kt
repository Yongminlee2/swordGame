package com.geomgang.game

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class SettingsBackupUiTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun exportAndImportButtonsOpenPasswordDialogs() {
        compose.waitForIdle()
        val confirms = compose.onAllNodesWithText("확인")
        if (confirms.fetchSemanticsNodes().isNotEmpty()) {
            confirms[0].performClick()
        }

        compose.onNodeWithContentDescription("기록 메뉴").performClick()
        compose.onNodeWithText("설정").performClick()

        compose.onNodeWithText("내보내기").performClick()
        compose.onNodeWithText("암호화 백업 만들기").assertIsDisplayed()
        compose.onNodeWithText("취소").performClick()

        compose.onNodeWithText("가져오기").performClick()
        compose.onNodeWithText("암호화 백업 불러오기").assertIsDisplayed()
        compose.onNodeWithText("취소").performClick()
    }
}
