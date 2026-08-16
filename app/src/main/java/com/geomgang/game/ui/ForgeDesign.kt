package com.geomgang.game.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Button as MaterialButton
import androidx.compose.material3.OutlinedButton as MaterialOutlinedButton
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geomgang.game.R

/**
 * 긴 보조 화면의 공통 골격.
 *
 * 제목과 뒤로 가기, 지갑은 항상 화면에 남고 실제 내용만 스크롤된다. 화면마다
 * 스크롤 위치가 달라도 나가는 길과 현재 시즌을 잃지 않게 한다.
 */
@Composable
fun ScrollableForgeScreen(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    wallet: Wallet? = null,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 14.dp, end = 14.dp, top = 12.dp),
    ) {
        ScreenHeader(title = title, onBack = onBack, wallet = wallet)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 12.dp),
            horizontalAlignment = horizontalAlignment,
            content = content,
        )
    }
}

/** 기준 시안의 어두운 격자와 불씨를 모든 게임 화면의 공통 무대로 사용한다. */
@Composable
fun ForgeBackdrop(content: @Composable BoxScope.() -> Unit) {
    val deep = LocalForgeDeep.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Image(
            bitmap = ImageBitmap.imageResource(
                if (deep) R.drawable.forge_background_season2
                else R.drawable.forge_background_season1,
            ),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = if (deep) 0.94f else 0.88f,
            filterQuality = FilterQuality.None,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.08f)),
        )
        // Android 15의 edge-to-edge 환경에서도 시스템 뒤로·홈 버튼 위로 UI가
        // 내려가지 않게 모든 화면에 같은 하단 안전 영역을 적용한다.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(bottom = 6.dp),
            content = content,
        )
    }
}

/** 생성한 16비트 UI 자산을 흐림 없이 표시한다. */
@Composable
fun PixelIcon(
    @DrawableRes resource: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
) {
    Image(
        bitmap = ImageBitmap.imageResource(resource),
        contentDescription = contentDescription,
        modifier = modifier.alpha(alpha),
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.None,
    )
}

/** 기준 시안의 모서리가 한 픽셀 잘린 정보 패널. */
@Composable
fun ForgePanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = CutCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(content = content)
    }
}

@Composable
fun ForgeSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PixelIcon(
                resource = R.drawable.ui_pixel_bolt,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = title,
                modifier = Modifier.padding(start = 5.dp),
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.2.sp,
            )
        }
        subtitle?.let {
            Text(
                text = it,
                modifier = Modifier.padding(top = 2.dp),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun SeasonStamp(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Text(
        text = LocalForgeSeason.current.let { season ->
            if (compact) "${season.roman} · ${season.shortName}" else {
                "${season.roman} ${season.displayName}"
            }
        },
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary,
        fontSize = if (compact) 10.sp else 13.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 0.4.sp,
    )
}

@Composable
fun ThinRule(
    modifier: Modifier = Modifier,
    thickness: Dp = 1.dp,
    color: Color = MaterialTheme.colorScheme.outline,
) {
    Spacer(
        modifier = modifier
            .height(thickness)
            .background(color),
    )
}

/** 앱 팔레트와 잘린 모서리를 공유하는 공통 진행 막대. */
@Composable
fun PixelProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 7.dp,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val shape = CutCornerShape(2.dp)
    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape),
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxSize()
                .background(color),
        )
    }
}

/** 설정 화면용 픽셀 토글. 48dp 터치 영역 안에 각진 스위치를 둔다. */
@Composable
fun PixelToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val trackShape = CutCornerShape(4.dp)
    Box(
        modifier = modifier
            .size(width = 58.dp, height = 48.dp)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(52.dp)
                .height(28.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline, trackShape)
                .background(
                    if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                    else MaterialTheme.colorScheme.surfaceVariant,
                    trackShape,
                )
                .padding(4.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                    .size(18.dp)
                    .background(
                        if (checked) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        CutCornerShape(2.dp),
                    ),
            )
        }
    }
}

/** 기준 시안의 황금색 픽셀 베벨 강화 버튼. */
@Composable
fun PixelActionButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    feedback: Float = 0f,
    feedbackColor: Color = Color.White,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = CutCornerShape(6.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val amount = feedback.coerceIn(0f, 1f)
    val normalBackground = if (enabled) ForgeAmber else MaterialTheme.colorScheme.surfaceVariant
    val normalBorder = if (enabled) Color(0xFFFFCF52) else MaterialTheme.colorScheme.outline
    val pressedBackground = if (pressed && enabled) Color(0xFFD58A17) else normalBackground
    val background = lerp(pressedBackground, feedbackColor, amount * 0.44f)
    val border = lerp(normalBorder, feedbackColor, amount * 0.72f)
    val topBevel = if (pressed) Color(0xFFC77B13) else Color(0xFFFFD76B)
    val bottomBevel = if (pressed) Color(0xFFFFD76B) else Color(0xFFC77B13)
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = if (pressed && enabled) 0.985f else 1f
                scaleY = if (pressed && enabled) 0.92f else 1f
                translationY = if (pressed && enabled) 2.dp.toPx() else 0f
            }
            .semantics { role = Role.Button }
            .border(2.dp, border, shape)
            .background(background, shape)
            .clip(shape)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        // 판정 직후 버튼 전체가 한 번 번쩍인다. 버튼은 계속 활성 상태라 연타를 막지 않는다.
        if (amount > 0f) {
            Spacer(
                Modifier
                    .fillMaxSize()
                    .background(feedbackColor.copy(alpha = amount * 0.18f), shape),
            )
        }
        Spacer(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(2.dp)
                .background(if (enabled) lerp(topBevel, feedbackColor, amount) else border),
        )
        Spacer(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(3.dp)
                .background(if (enabled) lerp(bottomBevel, feedbackColor, amount) else border),
        )
        content()
    }
}

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CutCornerShape(4.dp),
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    MaterialButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )
}

@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CutCornerShape(4.dp),
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    MaterialOutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )
}
