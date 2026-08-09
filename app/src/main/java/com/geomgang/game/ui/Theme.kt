package com.geomgang.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

/** 대장간 UI가 함께 쓰는 색. 검 스프라이트보다 앞서지 않는 낮은 채도의 금속·불빛 계열이다. */
val ForgeGold = Color(0xFFE7AD55)
val ForgeGoldSoft = Color(0xFFFFD990)
val ForgeSteel = Color(0xFFA9BBC8)
val ForgeViolet = Color(0xFFC29ADA)
val ForgeBackground = Color(0xFF0B0910)
val ForgeSurface = Color(0xFF17121D)
val ForgeSurfaceRaised = Color(0xFF211925)
val ForgeOutline = Color(0xFF514252)
val ForgeSuccess = Color(0xFF7ED6A2)
val ForgeWarning = Color(0xFFE8A45B)
val ForgeDanger = Color(0xFFFF7067)

private val ForgeColors = darkColorScheme(
    primary = ForgeGold,
    onPrimary = Color(0xFF271904),
    primaryContainer = Color(0xFF4C3217),
    onPrimaryContainer = ForgeGoldSoft,
    secondary = ForgeSteel,
    onSecondary = Color(0xFF10202A),
    secondaryContainer = Color(0xFF263641),
    onSecondaryContainer = Color(0xFFD7E7F2),
    tertiary = ForgeViolet,
    onTertiary = Color(0xFF2A1633),
    tertiaryContainer = Color(0xFF432950),
    onTertiaryContainer = Color(0xFFF0D5FA),
    background = ForgeBackground,
    onBackground = Color(0xFFF2EAF3),
    surface = ForgeSurface,
    onSurface = Color(0xFFF2EAF3),
    surfaceVariant = ForgeSurfaceRaised,
    onSurfaceVariant = Color(0xFFD6C9D8),
    outline = ForgeOutline,
    outlineVariant = Color(0xFF342A36),
    error = ForgeDanger,
    onError = Color(0xFF300503),
)

private val ForgeTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Black,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 27.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
)

private val ForgeShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun SwordForgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ForgeColors,
        typography = ForgeTypography,
        shapes = ForgeShapes,
        content = content,
    )
}

/** 모든 화면의 가장 아래에 깔리는 저채도 배경. 세부 화면도 같은 게임 안에 있도록 묶는다. */
@Composable
fun ForgeBackdrop(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF171020),
                        ForgeBackground,
                        Color(0xFF100B12),
                    ),
                ),
            ),
    ) {
        content()
    }
}
