package com.geomgang.game.ui

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val ForgeCyan = Color(0xFF45D1CF)
val ForgeAmber = Color(0xFFF4B53B)
val ForgeOrange = Color(0xFFFF982E)
val ForgeRed = Color(0xFFFF4C4C)
val ForgeGreen = Color(0xFF67D995)
val ForgeInk = Color(0xFF050C12)
val ForgePanelColor = Color(0xD9121A21)
val ForgeLine = Color(0xFF303A43)
val ForgeText = Color(0xFFF1F1F3)
val ForgeMuted = Color(0xFF92929A)

private val DeepColors = darkColorScheme(
    primary = ForgeCyan,
    onPrimary = Color(0xFF041516),
    primaryContainer = Color(0xFF12383A),
    onPrimaryContainer = Color(0xFFB9FFFC),
    secondary = ForgeAmber,
    onSecondary = Color(0xFF1D1402),
    background = ForgeInk,
    onBackground = ForgeText,
    surface = ForgePanelColor,
    onSurface = ForgeText,
    surfaceVariant = Color(0xFF141C23),
    onSurfaceVariant = ForgeMuted,
    outline = ForgeLine,
    error = ForgeRed,
    onError = Color(0xFF240503),
)

private val EarlyColors = darkColorScheme(
    primary = ForgeAmber,
    onPrimary = Color(0xFF201300),
    primaryContainer = Color(0xFF443016),
    onPrimaryContainer = Color(0xFFFFE2A8),
    secondary = ForgeOrange,
    onSecondary = Color(0xFF221002),
    background = Color(0xFF0D0A07),
    onBackground = Color(0xFFF4EEE8),
    surface = Color(0xD918120D),
    onSurface = Color(0xFFF4EEE8),
    surfaceVariant = Color(0xFF211811),
    onSurfaceVariant = Color(0xFFA99C91),
    outline = Color(0xFF3A2D23),
    error = ForgeRed,
    onError = Color(0xFF240503),
)

private val ForgeShapes = Shapes(
    extraSmall = CutCornerShape(2.dp),
    small = CutCornerShape(3.dp),
    medium = CutCornerShape(4.dp),
    large = CutCornerShape(5.dp),
    extraLarge = CutCornerShape(6.dp),
)

private val ForgeTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        letterSpacing = 0.2.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.4).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 23.sp,
    ),
)

val LocalForgeDeep = staticCompositionLocalOf { false }

@Composable
fun SwordForgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EarlyColors,
        typography = ForgeTypography,
        shapes = ForgeShapes,
        content = content,
    )
}

/** 게임 진행 국면과 모든 메뉴의 색을 같은 경계로 전환한다. */
@Composable
fun ForgeSeasonTheme(deep: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalForgeDeep provides deep) {
        MaterialTheme(
            colorScheme = if (deep) DeepColors else EarlyColors,
            typography = ForgeTypography,
            shapes = ForgeShapes,
            content = content,
        )
    }
}
