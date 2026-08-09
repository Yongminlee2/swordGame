package com.geomgang.game.ui

import androidx.compose.foundation.shape.RoundedCornerShape
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

val ForgeCyan = Color(0xFF48D6D2)
val ForgeAmber = Color(0xFFF2B338)
val ForgeOrange = Color(0xFFFF8B2C)
val ForgeRed = Color(0xFFFF625F)
val ForgeGreen = Color(0xFF67D995)
val ForgeInk = Color(0xFF071016)
val ForgePanelColor = Color(0xE610181F)
val ForgeLine = Color(0xFF28343C)
val ForgeText = Color(0xFFF1F4F5)
val ForgeMuted = Color(0xFF89959D)

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
    surfaceVariant = Color(0xFF131D24),
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
    background = Color(0xFF100C08),
    onBackground = Color(0xFFF4EEE8),
    surface = Color(0xE617120E),
    onSurface = Color(0xFFF4EEE8),
    surfaceVariant = Color(0xFF211811),
    onSurfaceVariant = Color(0xFFA99C91),
    outline = Color(0xFF3A2D23),
    error = ForgeRed,
    onError = Color(0xFF240503),
)

private val ForgeShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(3.dp),
    large = RoundedCornerShape(4.dp),
    extraLarge = RoundedCornerShape(6.dp),
)

private val ForgeTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        letterSpacing = 0.2.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 24.sp,
        lineHeight = 30.sp,
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
